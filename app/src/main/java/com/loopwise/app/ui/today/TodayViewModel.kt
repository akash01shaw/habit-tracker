@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package com.loopwise.app.ui.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.loopwise.app.data.repository.AuthRepository
import com.loopwise.app.data.repository.HabitRepository
import com.loopwise.app.data.repository.TaskRepository
import com.loopwise.app.domain.model.Habit
import com.loopwise.app.domain.model.HabitLog
import com.loopwise.app.domain.model.HabitType
import com.loopwise.app.domain.model.Task
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate

data class HabitWithProgress(val habit: Habit, val log: HabitLog?) {
    val currentValue: Double get() = log?.value ?: 0.0
    val isDone: Boolean get() = log?.isDone ?: false
}

/** [since] is the last reset/slip time — the streak clock counts up from here. */
data class AvoidanceHabitState(val habit: Habit, val since: Instant)

data class TodayUiState(
    val selectedDate: LocalDate = LocalDate.now(),
    val tasks: List<Task> = emptyList(),
    val habits: List<HabitWithProgress> = emptyList(),
    val avoidanceHabits: List<AvoidanceHabitState> = emptyList(),
    val isSignedIn: Boolean = true,
)

class TodayViewModel(
    private val authRepository: AuthRepository,
    private val habitRepository: HabitRepository,
    private val taskRepository: TaskRepository,
) : ViewModel() {

    private val selectedDate = MutableStateFlow(LocalDate.now())
    private var hasRefreshedForUser: String? = null

    init {
        viewModelScope.launch {
            authRepository.currentUser.collect { user ->
                if (user != null && hasRefreshedForUser != user.id) {
                    hasRefreshedForUser = user.id
                    refresh()
                }
            }
        }
    }

    val uiState: StateFlow<TodayUiState> = combine(authRepository.currentUser, selectedDate) { user, date ->
        user to date
    }.flatMapLatest { (user, date) ->
        if (user == null) {
            flowOf(TodayUiState(isSignedIn = false))
        } else {
            combine(
                habitRepository.observeHabits(user.id),
                habitRepository.observeLogsForDate(user.id, date),
                taskRepository.observeTasksForDate(user.id, date),
                habitRepository.observeLatestLogPerHabit(user.id),
            ) { habits, logs, tasks, latestLogs ->
                val logsByHabit = logs.associateBy { it.habitId }
                val latestLogByHabit = latestLogs.associateBy { it.habitId }
                TodayUiState(
                    selectedDate = date,
                    tasks = tasks,
                    habits = habits
                        .filter { it.type != HabitType.AVOIDANCE && it.isScheduledOn(date) }
                        .map { habit -> HabitWithProgress(habit, logsByHabit[habit.id]) },
                    avoidanceHabits = habits
                        .filter { it.type == HabitType.AVOIDANCE }
                        .map { habit ->
                            AvoidanceHabitState(
                                habit = habit,
                                since = latestLogByHabit[habit.id]?.loggedAt ?: habit.createdAt,
                            )
                        },
                    isSignedIn = true,
                )
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TodayUiState())

    fun selectDate(date: LocalDate) {
        selectedDate.value = date
        refresh()
    }

    fun refresh() {
        val userId = authRepository.currentUserIdOrNull() ?: return
        viewModelScope.launch {
            habitRepository.refreshFromRemote(userId)
            taskRepository.refreshFromRemote(userId, selectedDate.value)
            habitRepository.syncPending()
            taskRepository.syncPending()
        }
    }

    fun toggleTask(task: Task) {
        viewModelScope.launch { taskRepository.setCompleted(task, !task.isCompleted) }
    }

    fun toggleBooleanHabit(item: HabitWithProgress) {
        val userId = authRepository.currentUserIdOrNull() ?: return
        val newDone = !item.isDone
        viewModelScope.launch {
            habitRepository.logProgress(
                habitId = item.habit.id,
                userId = userId,
                date = uiState.value.selectedDate,
                value = if (newDone) 1.0 else 0.0,
                isDone = newDone,
            )
        }
    }

    fun incrementCounterHabit(item: HabitWithProgress, delta: Double) {
        val userId = authRepository.currentUserIdOrNull() ?: return
        val newValue = (item.currentValue + delta).coerceAtLeast(0.0)
        val target = item.habit.targetValue ?: Double.MAX_VALUE
        viewModelScope.launch {
            habitRepository.logProgress(
                habitId = item.habit.id,
                userId = userId,
                date = uiState.value.selectedDate,
                value = newValue,
                isDone = newValue >= target,
            )
        }
    }

    fun logDurationMinutes(item: HabitWithProgress, minutes: Double) {
        incrementCounterHabit(item, minutes)
    }

    /** "I slipped" — resets the streak clock and logs the relapse timestamp (spec §2.3). */
    fun resetAvoidanceStreak(item: AvoidanceHabitState) {
        val userId = authRepository.currentUserIdOrNull() ?: return
        viewModelScope.launch {
            habitRepository.logProgress(
                habitId = item.habit.id,
                userId = userId,
                date = LocalDate.now(),
                value = 0.0,
                isDone = false,
            )
        }
    }

    companion object {
        fun defaultIncrement(habit: Habit): Double = when (habit.type) {
            HabitType.COUNTER -> 1.0
            HabitType.DURATION -> 5.0
            else -> 1.0
        }
    }
}
