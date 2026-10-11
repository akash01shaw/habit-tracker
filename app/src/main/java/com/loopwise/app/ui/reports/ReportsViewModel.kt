@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package com.loopwise.app.ui.reports

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.loopwise.app.data.repository.AuthRepository
import com.loopwise.app.data.repository.HabitRepository
import com.loopwise.app.domain.model.HabitType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate

enum class ReportSort { COMPLETION, STREAK }

data class ReportsUiState(
    val range: ReportRange = ReportRange.WEEK,
    val sort: ReportSort = ReportSort.COMPLETION,
    val reports: List<HabitReport> = emptyList(),
    val isSignedIn: Boolean = true,
)

class ReportsViewModel(
    private val authRepository: AuthRepository,
    private val habitRepository: HabitRepository,
) : ViewModel() {

    private val range = MutableStateFlow(ReportRange.WEEK)
    private val sort = MutableStateFlow(ReportSort.COMPLETION)

    val uiState: StateFlow<ReportsUiState> =
        combine(authRepository.currentUser, range, sort) { user, r, s -> Triple(user, r, s) }
            .flatMapLatest { (user, r, s) ->
                if (user == null) {
                    flowOf(ReportsUiState(isSignedIn = false))
                } else {
                    val today = LocalDate.now()
                    val from = today.minusDays((r.days - 1).toLong())
                    habitRepository.observeHabits(user.id).flatMapLatest { habits ->
                        // Avoidance habits use a continuous streak, not daily completion —
                        // reporting for them is a separate concern, not built in this pass.
                        val reportable = habits.filter { it.type != HabitType.AVOIDANCE }
                        if (reportable.isEmpty()) {
                            flowOf(ReportsUiState(range = r, sort = s, reports = emptyList(), isSignedIn = true))
                        } else {
                            combine(
                                reportable.map { habit ->
                                    habitRepository.observeLogsInRange(habit.id, user.id, from, today)
                                        .map { logs -> buildHabitReport(habit, logs, today, r) }
                                },
                            ) { reportsArray ->
                                val sorted = when (s) {
                                    ReportSort.COMPLETION -> reportsArray.sortedByDescending { it.completionPercent }
                                    ReportSort.STREAK -> reportsArray.sortedByDescending { it.streak }
                                }
                                ReportsUiState(range = r, sort = s, reports = sorted, isSignedIn = true)
                            }
                        }
                    }
                }
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ReportsUiState())

    fun selectRange(newRange: ReportRange) {
        range.value = newRange
    }

    fun selectSort(newSort: ReportSort) {
        sort.value = newSort
    }
}
