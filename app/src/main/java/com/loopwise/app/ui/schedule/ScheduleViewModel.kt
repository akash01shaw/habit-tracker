@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package com.loopwise.app.ui.schedule

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.loopwise.app.data.repository.AuthRepository
import com.loopwise.app.data.repository.HabitRepository
import com.loopwise.app.data.repository.TaskRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import java.time.DayOfWeek
import java.time.LocalDate

enum class ScheduleViewMode { AGENDA, GRID }

data class ScheduleUiState(
    val weekStart: LocalDate = currentWeekStart(),
    val blocks: List<ScheduleBlock> = emptyList(),
    val viewMode: ScheduleViewMode = ScheduleViewMode.AGENDA,
    val isSignedIn: Boolean = true,
) {
    val weekDates: List<LocalDate> get() = (0..6).map { weekStart.plusDays(it.toLong()) }
}

fun currentWeekStart(): LocalDate = LocalDate.now().with(DayOfWeek.MONDAY).minusDays(1) // Sunday start, matches DateStrip

class ScheduleViewModel(
    private val authRepository: AuthRepository,
    private val habitRepository: HabitRepository,
    private val taskRepository: TaskRepository,
) : ViewModel() {

    private val weekStart = MutableStateFlow(currentWeekStart())
    private val viewMode = MutableStateFlow(ScheduleViewMode.AGENDA)

    val uiState: StateFlow<ScheduleUiState> =
        combine(authRepository.currentUser, weekStart, viewMode) { user, ws, mode -> Triple(user, ws, mode) }
            .flatMapLatest { (user, ws, mode) ->
                if (user == null) {
                    flowOf(ScheduleUiState(isSignedIn = false))
                } else {
                    val dates = (0..6).map { ws.plusDays(it.toLong()) }
                    combine(
                        habitRepository.observeHabits(user.id),
                        combine(dates.map { date -> taskRepository.observeTasksForDate(user.id, date) }) { perDay ->
                            perDay.toList().flatten()
                        },
                    ) { habits, tasks ->
                        ScheduleUiState(
                            weekStart = ws,
                            blocks = buildScheduleBlocks(habits, tasks, dates),
                            viewMode = mode,
                            isSignedIn = true,
                        )
                    }
                }
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ScheduleUiState())

    fun previousWeek() {
        weekStart.value = weekStart.value.minusWeeks(1)
    }

    fun nextWeek() {
        weekStart.value = weekStart.value.plusWeeks(1)
    }

    fun goToCurrentWeek() {
        weekStart.value = currentWeekStart()
    }

    fun setViewMode(mode: ScheduleViewMode) {
        viewMode.value = mode
    }
}
