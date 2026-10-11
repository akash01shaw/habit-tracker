package com.loopwise.app.ui.timer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.loopwise.app.data.repository.AuthRepository
import com.loopwise.app.data.repository.HabitRepository
import com.loopwise.app.data.repository.TimerRepository
import com.loopwise.app.domain.model.Habit
import com.loopwise.app.domain.model.HabitType
import com.loopwise.app.domain.model.TimerEvent
import com.loopwise.app.domain.model.TimerEventType
import com.loopwise.app.domain.model.TimerMode
import com.loopwise.app.domain.model.TimerSession
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

enum class TimerRunState { IDLE, RUNNING, PAUSED }

data class TimerUiState(
    val mode: TimerMode = TimerMode.STOPWATCH,
    val runState: TimerRunState = TimerRunState.IDLE,
    val targetMinutes: Int = 30,
    val elapsedSeconds: Long = 0,
    val linkedHabitId: String? = null,
    val habits: List<Habit> = emptyList(),
    val events: List<TimerEvent> = emptyList(),
    val isSignedIn: Boolean = true,
) {
    val linkedHabitName: String? get() = habits.firstOrNull { it.id == linkedHabitId }?.name

    /** What the ring/number should show: counts up for stopwatch, down for countdown. */
    val displaySeconds: Long
        get() = if (mode == TimerMode.COUNTDOWN) (targetMinutes * 60L - elapsedSeconds).coerceAtLeast(0) else elapsedSeconds

    val progressFraction: Float
        get() {
            val targetTotal = targetMinutes * 60L
            if (mode != TimerMode.COUNTDOWN || targetTotal <= 0) return 0f
            return (elapsedSeconds.toFloat() / targetTotal.toFloat()).coerceIn(0f, 1f)
        }
}

/**
 * Deliberately a single mutable state machine rather than a reactive pipeline — a
 * running timer is inherently imperative (user presses buttons, a coroutine ticks),
 * unlike the rest of the app's mostly-reactive-from-the-database screens. Habits for
 * the Focus picker are the one piece that's still a live DB subscription.
 */
class TimerViewModel(
    private val authRepository: AuthRepository,
    private val habitRepository: HabitRepository,
    private val timerRepository: TimerRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(TimerUiState())
    val state: StateFlow<TimerUiState> = _state.asStateFlow()

    private var tickJob: Job? = null
    private var sessionId: String? = null

    init {
        viewModelScope.launch {
            authRepository.currentUser.collect { user ->
                _state.value = _state.value.copy(isSignedIn = user != null)
                if (user != null) observeHabits(user.id)
            }
        }
    }

    private fun observeHabits(userId: String) {
        viewModelScope.launch {
            habitRepository.observeHabits(userId).collect { habits ->
                _state.value = _state.value.copy(habits = habits.filter { it.type != HabitType.AVOIDANCE })
            }
        }
    }

    fun selectMode(mode: TimerMode) {
        if (_state.value.runState != TimerRunState.IDLE) return
        _state.value = _state.value.copy(mode = mode)
    }

    fun setTargetMinutes(minutes: Int) {
        if (_state.value.runState != TimerRunState.IDLE) return
        _state.value = _state.value.copy(targetMinutes = minutes.coerceIn(1, 480))
    }

    fun selectHabit(habitId: String?) {
        if (_state.value.runState != TimerRunState.IDLE) return
        _state.value = _state.value.copy(linkedHabitId = habitId)
    }

    fun start() {
        if (_state.value.runState != TimerRunState.IDLE) return
        sessionId = UUID.randomUUID().toString()
        recordEvent(TimerEventType.START)
        _state.value = _state.value.copy(runState = TimerRunState.RUNNING, elapsedSeconds = 0)
        startTicking()
    }

    fun pause() {
        if (_state.value.runState != TimerRunState.RUNNING) return
        tickJob?.cancel()
        recordEvent(TimerEventType.PAUSE)
        _state.value = _state.value.copy(runState = TimerRunState.PAUSED)
        persist()
    }

    fun resume() {
        if (_state.value.runState != TimerRunState.PAUSED) return
        recordEvent(TimerEventType.RESUME)
        _state.value = _state.value.copy(runState = TimerRunState.RUNNING)
        startTicking()
    }

    fun stop() {
        if (_state.value.runState == TimerRunState.IDLE) return
        tickJob?.cancel()
        recordEvent(TimerEventType.STOP)
        val finished = _state.value
        persist()
        feedLinkedHabit(finished)
        sessionId = null
        _state.value = TimerUiState(habits = finished.habits, isSignedIn = finished.isSignedIn)
    }

    private fun startTicking() {
        tickJob = viewModelScope.launch {
            while (true) {
                delay(1_000)
                val newElapsed = _state.value.elapsedSeconds + 1
                _state.value = _state.value.copy(elapsedSeconds = newElapsed)
                val current = _state.value
                if (current.mode == TimerMode.COUNTDOWN && newElapsed >= current.targetMinutes * 60L) {
                    stop()
                    break
                }
            }
        }
    }

    private fun recordEvent(type: TimerEventType) {
        _state.value = _state.value.copy(events = _state.value.events + TimerEvent(type, Instant.now()))
    }

    private fun persist() {
        val ownerId = authRepository.currentUserIdOrNull() ?: return
        val id = sessionId ?: return
        val state = _state.value
        val session = TimerSession(
            id = id,
            ownerId = ownerId,
            habitId = state.linkedHabitId,
            mode = state.mode,
            targetSeconds = if (state.mode == TimerMode.COUNTDOWN) state.targetMinutes * 60 else null,
            events = state.events,
        )
        viewModelScope.launch { timerRepository.upsertSession(session) }
    }

    /** Spec §2.6: time logged in a session attached to a habit feeds that habit's progress. */
    private fun feedLinkedHabit(finished: TimerUiState) {
        val habit = finished.habits.firstOrNull { it.id == finished.linkedHabitId } ?: return
        if (habit.type != HabitType.DURATION) return
        val userId = authRepository.currentUserIdOrNull() ?: return
        val minutesLogged = finished.elapsedSeconds / 60.0
        if (minutesLogged <= 0) return

        viewModelScope.launch {
            val today = LocalDate.now()
            val todaysLogs = habitRepository.observeLogsForDate(userId, today).first()
            val currentValue = todaysLogs.firstOrNull { it.habitId == habit.id }?.value ?: 0.0
            val newValue = currentValue + minutesLogged
            val target = habit.targetValue ?: Double.MAX_VALUE
            habitRepository.logProgress(habit.id, userId, today, newValue, newValue >= target)
        }
    }
}
