@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package com.loopwise.app.ui.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.loopwise.app.data.health.HealthConnectManager
import com.loopwise.app.data.repository.AuthRepository
import com.loopwise.app.data.repository.HabitRepository
import com.loopwise.app.data.repository.TaskRepository
import com.loopwise.app.domain.model.Habit
import com.loopwise.app.domain.model.HabitLog
import com.loopwise.app.domain.model.HabitType
import com.loopwise.app.domain.model.HealthSyncSource
import com.loopwise.app.domain.model.Task
import com.loopwise.app.notifications.ReminderScheduler
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

data class HabitWithProgress(val habit: Habit, val log: HabitLog?, val streak: Int = 0) {
    val currentValue: Double get() = log?.value ?: 0.0
    val isDone: Boolean get() = log?.isDone ?: false
    val isSkipped: Boolean get() = log?.isSkipped ?: false
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
    private val reminderScheduler: ReminderScheduler,
    private val healthConnectManager: HealthConnectManager,
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
            val today = LocalDate.now()
            combine(
                habitRepository.observeHabits(user.id),
                habitRepository.observeLogsForDate(user.id, date),
                taskRepository.observeTasksForDate(user.id, date),
                habitRepository.observeLatestLogPerHabit(user.id),
                habitRepository.observeLogsInRangeForUser(user.id, today.minusDays(400), today),
            ) { habits, logs, tasks, latestLogs, recentLogs ->
                val logsByHabit = logs.associateBy { it.habitId }
                val latestLogByHabit = latestLogs.associateBy { it.habitId }
                val recentLogsByHabit = recentLogs.groupBy { it.habitId }
                TodayUiState(
                    selectedDate = date,
                    tasks = tasks,
                    habits = habits
                        .filter { it.type != HabitType.AVOIDANCE && it.isScheduledOn(date) }
                        .map { habit ->
                            HabitWithProgress(
                                habit = habit,
                                log = logsByHabit[habit.id],
                                streak = computeCurrentStreak(habit, recentLogsByHabit[habit.id].orEmpty(), today),
                            )
                        },
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
            syncHealthData()
        }
    }

    /**
     * Pulls today's value for every Health Connect-linked habit and logs it, same as a
     * manual tap would. Only applies to "today" — Health Connect's live reading has no
     * meaningful answer for a past day in the date strip. No-ops quietly if Health
     * Connect isn't installed or permission hasn't been granted (it's an optional
     * auto-fill, not a requirement). Also what the sync icon's tap triggers manually.
     */
    fun syncHealthData() {
        if (selectedDate.value != LocalDate.now()) return
        val userId = authRepository.currentUserIdOrNull() ?: return
        viewModelScope.launch {
            if (!healthConnectManager.hasAllPermissions()) return@launch
            uiState.value.habits.forEach { item ->
                val source = HealthSyncSource.fromWire(item.habit.healthSyncSource) ?: return@forEach
                val value = healthConnectManager.readValueForToday(source) ?: return@forEach
                val target = item.habit.targetValue ?: Double.MAX_VALUE
                habitRepository.logProgress(
                    habitId = item.habit.id,
                    userId = userId,
                    date = selectedDate.value,
                    value = value,
                    isDone = value >= target,
                )
            }
        }
    }

    fun signOut() {
        viewModelScope.launch { authRepository.signOut() }
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

    /** Skip today without it counting as a miss — doesn't break the streak or count against completion%. */
    fun skipHabit(item: HabitWithProgress) {
        val userId = authRepository.currentUserIdOrNull() ?: return
        viewModelScope.launch {
            habitRepository.logProgress(
                habitId = item.habit.id,
                userId = userId,
                date = uiState.value.selectedDate,
                value = item.currentValue,
                isDone = false,
                isSkipped = true,
            )
        }
    }

    fun unskipHabit(item: HabitWithProgress) {
        val userId = authRepository.currentUserIdOrNull() ?: return
        viewModelScope.launch {
            habitRepository.logProgress(
                habitId = item.habit.id,
                userId = userId,
                date = uiState.value.selectedDate,
                value = item.currentValue,
                isDone = false,
                isSkipped = false,
            )
        }
    }

    fun deleteHabit(habit: Habit) {
        viewModelScope.launch {
            habitRepository.deleteHabit(habit.id)
            habit.reminders.forEach { reminderScheduler.cancelHabitReminder(habit.id, it) }
        }
    }

    fun deleteTask(task: Task) {
        viewModelScope.launch {
            taskRepository.deleteTask(task.id)
            if (task.reminderTime != null) reminderScheduler.cancelTaskReminder(task.id)
        }
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
