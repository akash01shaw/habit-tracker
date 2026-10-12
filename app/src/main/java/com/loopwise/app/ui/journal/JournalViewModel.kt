@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package com.loopwise.app.ui.journal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.loopwise.app.data.repository.AuthRepository
import com.loopwise.app.data.repository.HabitRepository
import com.loopwise.app.data.repository.JournalRepository
import com.loopwise.app.domain.model.ChecklistItem
import com.loopwise.app.domain.model.Habit
import com.loopwise.app.domain.model.JournalEntry
import com.loopwise.app.domain.model.JournalEntryType
import com.loopwise.app.domain.model.decodeChecklistItems
import com.loopwise.app.domain.model.encodeChecklistItems
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

data class JournalFilterState(val habitId: String? = null, val type: JournalEntryType? = null)

data class JournalUiState(
    val entries: List<JournalEntry> = emptyList(),
    val habits: List<Habit> = emptyList(),
    val filter: JournalFilterState = JournalFilterState(),
    val isSignedIn: Boolean = true,
)

data class NewEntryFormState(
    val type: JournalEntryType = JournalEntryType.TEXT,
    val content: String = "",
    val moodValue: Int = 50,
    val checklistItems: List<ChecklistItem> = emptyList(),
    val checklistDraft: String = "",
    val linkedHabitId: String? = null,
    val isSaving: Boolean = false,
    val error: String? = null,
    val saved: Boolean = false,
)

class JournalViewModel(
    private val authRepository: AuthRepository,
    private val journalRepository: JournalRepository,
    private val habitRepository: HabitRepository,
) : ViewModel() {

    private val filter = MutableStateFlow(JournalFilterState())

    val uiState: StateFlow<JournalUiState> =
        combine(authRepository.currentUser, filter) { user, f -> user to f }
            .flatMapLatest { (user, f) ->
                if (user == null) {
                    flowOf(JournalUiState(isSignedIn = false))
                } else {
                    combine(
                        journalRepository.observeEntries(user.id, f.habitId, f.type),
                        habitRepository.observeHabits(user.id),
                    ) { entries, habits ->
                        JournalUiState(entries = entries, habits = habits, filter = f, isSignedIn = true)
                    }
                }
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), JournalUiState())

    fun setHabitFilter(habitId: String?) {
        filter.value = filter.value.copy(habitId = habitId)
    }

    fun setTypeFilter(type: JournalEntryType?) {
        filter.value = filter.value.copy(type = type)
    }

    fun toggleChecklistItem(entry: JournalEntry, index: Int) {
        val items = decodeChecklistItems(entry.content).toMutableList()
        if (index !in items.indices) return
        items[index] = items[index].copy(checked = !items[index].checked)
        val updated = entry.copy(content = encodeChecklistItems(items))
        viewModelScope.launch { runCatching { journalRepository.upsertEntry(updated) } }
    }

    private val _formState = MutableStateFlow(NewEntryFormState())
    val formState: StateFlow<NewEntryFormState> = _formState.asStateFlow()

    fun resetForm() {
        _formState.value = NewEntryFormState()
    }

    fun onTypeChange(type: JournalEntryType) {
        _formState.value = _formState.value.copy(type = type, error = null)
    }

    fun onContentChange(text: String) {
        _formState.value = _formState.value.copy(content = text, error = null)
    }

    fun onMoodChange(value: Int) {
        _formState.value = _formState.value.copy(moodValue = value)
    }

    fun onLinkedHabitChange(habitId: String?) {
        _formState.value = _formState.value.copy(linkedHabitId = habitId)
    }

    fun onChecklistDraftChange(text: String) {
        _formState.value = _formState.value.copy(checklistDraft = text, error = null)
    }

    fun addChecklistItem() {
        val state = _formState.value
        val text = state.checklistDraft.trim()
        if (text.isEmpty()) return
        _formState.value = state.copy(
            checklistItems = state.checklistItems + ChecklistItem(text = text),
            checklistDraft = "",
        )
    }

    fun removeChecklistItem(index: Int) {
        val state = _formState.value
        if (index !in state.checklistItems.indices) return
        _formState.value = state.copy(checklistItems = state.checklistItems.filterIndexed { i, _ -> i != index })
    }

    fun saveEntry() {
        val ownerId = authRepository.currentUserIdOrNull()
        val state = _formState.value
        if (ownerId == null) {
            _formState.value = state.copy(error = "You need to be signed in.")
            return
        }
        if (state.type == JournalEntryType.TEXT && state.content.isBlank()) {
            _formState.value = state.copy(error = "Write something first.")
            return
        }
        if (state.type == JournalEntryType.CHECKLIST && state.checklistItems.isEmpty()) {
            _formState.value = state.copy(error = "Add at least one item.")
            return
        }

        val entry = JournalEntry(
            id = UUID.randomUUID().toString(),
            ownerId = ownerId,
            linkedHabitId = state.linkedHabitId,
            type = state.type,
            content = when (state.type) {
                JournalEntryType.CHECKLIST -> encodeChecklistItems(state.checklistItems)
                else -> state.content.trim().ifBlank { null }
            },
            moodValue = if (state.type == JournalEntryType.MOOD) state.moodValue else null,
        )

        _formState.value = state.copy(isSaving = true, error = null)
        viewModelScope.launch {
            runCatching { journalRepository.upsertEntry(entry) }
                .onSuccess { _formState.value = NewEntryFormState(saved = true) }
                .onFailure {
                    _formState.value = _formState.value.copy(
                        isSaving = false,
                        error = it.message ?: "Couldn't save this entry.",
                    )
                }
        }
    }
}
