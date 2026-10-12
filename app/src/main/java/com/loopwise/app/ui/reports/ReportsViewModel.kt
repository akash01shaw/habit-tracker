@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package com.loopwise.app.ui.reports

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.loopwise.app.data.export.CsvFileWriter
import com.loopwise.app.data.repository.AuthRepository
import com.loopwise.app.data.repository.HabitRepository
import com.loopwise.app.domain.model.HabitType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
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
    private val csvFileWriter: CsvFileWriter,
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

    private val _isExporting = MutableStateFlow(false)
    val isExporting: StateFlow<Boolean> = _isExporting

    private val _exportError = MutableStateFlow<String?>(null)
    val exportError: StateFlow<String?> = _exportError

    /** One-shot: the screen reads this to launch the share sheet, then calls [consumeExportUri]. */
    private val _exportUri = MutableStateFlow<Uri?>(null)
    val exportUri: StateFlow<Uri?> = _exportUri

    fun selectRange(newRange: ReportRange) {
        range.value = newRange
    }

    fun selectSort(newSort: ReportSort) {
        sort.value = newSort
    }

    fun exportCsv() {
        val userId = authRepository.currentUserIdOrNull()
        if (userId == null) {
            _exportError.value = "You need to be signed in to export."
            return
        }
        _isExporting.value = true
        _exportError.value = null
        viewModelScope.launch {
            runCatching {
                val habits = habitRepository.observeHabits(userId).first()
                val logs = habitRepository.getAllLogsForExport(userId)
                val csv = buildHabitLogsCsv(habits, logs)
                csvFileWriter.write(csv)
            }.onSuccess { uri ->
                _isExporting.value = false
                _exportUri.value = uri
            }.onFailure {
                _isExporting.value = false
                _exportError.value = it.message ?: "Couldn't export your data."
            }
        }
    }

    fun consumeExportUri() {
        _exportUri.value = null
    }
}
