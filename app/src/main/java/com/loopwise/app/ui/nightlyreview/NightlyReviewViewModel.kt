package com.loopwise.app.ui.nightlyreview

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.loopwise.app.data.repository.AuthRepository
import com.loopwise.app.data.repository.JournalRepository
import com.loopwise.app.data.repository.TaskRepository
import com.loopwise.app.domain.model.JournalEntry
import com.loopwise.app.domain.model.JournalEntryType
import com.loopwise.app.domain.model.Task
import com.loopwise.app.notifications.NightlyRitualScheduler
import com.loopwise.app.notifications.NightlyRitualState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.UUID

data class NightlyReviewUiState(
    val isSignedIn: Boolean = true,
    val todayIncompleteTasks: List<Task> = emptyList(),
    val tomorrowTasks: List<Task> = emptyList(),
    val journalText: String = "",
    val newTaskTitle: String = "",
    val isSavingJournal: Boolean = false,
    val journalSaved: Boolean = false,
    val finished: Boolean = false,
)

/**
 * Backs the nightly ritual screen: finish (or see) today's leftover to-dos, jot a quick
 * journal entry for today, and queue up tomorrow's to-dos — then "Done for tonight" marks
 * the ritual complete (so the 10pm notification won't re-nag) and dismisses it if it's
 * currently showing. Deliberately not gated on actually filling in the journal/tasks —
 * there's no way to enforce that server-side, so this stays an honest nudge rather than a
 * fake-strict gate.
 */
class NightlyReviewViewModel(
    private val authRepository: AuthRepository,
    private val taskRepository: TaskRepository,
    private val journalRepository: JournalRepository,
    private val ritualScheduler: NightlyRitualScheduler,
    private val ritualState: NightlyRitualState,
) : ViewModel() {

    private val journalText = MutableStateFlow("")
    private val newTaskTitle = MutableStateFlow("")
    private val isSavingJournal = MutableStateFlow(false)
    private val journalSaved = MutableStateFlow(false)
    private val finished = MutableStateFlow(false)

    val uiState: StateFlow<NightlyReviewUiState> = authRepository.currentUser
        .flatMapLatest { user ->
            if (user == null) {
                flowOf(NightlyReviewUiState(isSignedIn = false))
            } else {
                val today = LocalDate.now()
                val tomorrow = today.plusDays(1)
                val taskLists = combine(
                    taskRepository.observeTasksForDate(user.id, today),
                    taskRepository.observeTasksForDate(user.id, tomorrow),
                ) { todayTasks, tomorrowTasks -> TaskLists(todayTasks, tomorrowTasks) }
                val formBits = combine(
                    journalText,
                    newTaskTitle,
                    isSavingJournal,
                    journalSaved,
                    finished,
                ) { journal, draft, saving, saved, done -> FormBits(journal, draft, saving, saved, done) }

                taskLists.combine(formBits) { lists, form ->
                    NightlyReviewUiState(
                        isSignedIn = true,
                        todayIncompleteTasks = lists.today.filterNot { it.isCompleted },
                        tomorrowTasks = lists.tomorrow,
                        journalText = form.journal,
                        newTaskTitle = form.newTaskTitle,
                        isSavingJournal = form.isSavingJournal,
                        journalSaved = form.journalSaved,
                        finished = form.finished,
                    )
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), NightlyReviewUiState())

    private data class TaskLists(val today: List<Task>, val tomorrow: List<Task>)
    private data class FormBits(
        val journal: String,
        val newTaskTitle: String,
        val isSavingJournal: Boolean,
        val journalSaved: Boolean,
        val finished: Boolean,
    )

    fun onJournalTextChange(value: String) {
        journalSaved.value = false
        journalText.value = value
    }

    fun onNewTaskTitleChange(value: String) {
        newTaskTitle.value = value
    }

    fun toggleTask(task: Task) {
        viewModelScope.launch { taskRepository.setCompleted(task, !task.isCompleted) }
    }

    fun saveJournalEntry() {
        val text = journalText.value.trim()
        if (text.isEmpty()) return
        val ownerId = authRepository.currentUserIdOrNull() ?: return
        isSavingJournal.value = true
        viewModelScope.launch {
            runCatching {
                journalRepository.upsertEntry(
                    JournalEntry(
                        id = UUID.randomUUID().toString(),
                        ownerId = ownerId,
                        type = JournalEntryType.TEXT,
                        content = text,
                    ),
                )
            }
            isSavingJournal.value = false
            journalSaved.value = true
        }
    }

    fun addTomorrowTask() {
        val title = newTaskTitle.value.trim()
        if (title.isEmpty()) return
        val ownerId = authRepository.currentUserIdOrNull() ?: return
        viewModelScope.launch {
            runCatching {
                taskRepository.upsertTask(
                    Task(
                        id = UUID.randomUUID().toString(),
                        ownerId = ownerId,
                        title = title,
                        dueDate = LocalDate.now().plusDays(1),
                    ),
                )
            }
            newTaskTitle.value = ""
        }
    }

    fun finishForTonight() {
        ritualState.markCompletedToday()
        ritualScheduler.dismissForTonight()
        finished.value = true
    }
}
