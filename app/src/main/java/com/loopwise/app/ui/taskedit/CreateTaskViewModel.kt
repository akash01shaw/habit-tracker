package com.loopwise.app.ui.taskedit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.loopwise.app.data.repository.AuthRepository
import com.loopwise.app.data.repository.TaskRepository
import com.loopwise.app.domain.model.Task
import com.loopwise.app.notifications.ReminderScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.util.UUID

data class CreateTaskFormState(
    val title: String = "",
    val dueDate: LocalDate = LocalDate.now(),
    val dueTime: LocalTime? = null,
    val priorityFlag: Boolean = false,
    val note: String = "",
    val reminderEnabled: Boolean = false,
    val isSaving: Boolean = false,
    val error: String? = null,
    val saved: Boolean = false,
) {
    val canEnableReminder: Boolean get() = dueTime != null
}

class CreateTaskViewModel(
    private val authRepository: AuthRepository,
    private val taskRepository: TaskRepository,
    private val reminderScheduler: ReminderScheduler,
) : ViewModel() {

    private val _formState = MutableStateFlow(CreateTaskFormState())
    val formState: StateFlow<CreateTaskFormState> = _formState.asStateFlow()

    fun onTitleChange(value: String) {
        _formState.value = _formState.value.copy(title = value, error = null)
    }

    fun onDueDateChange(date: LocalDate) {
        _formState.value = _formState.value.copy(dueDate = date)
    }

    fun onDueTimeChange(time: LocalTime?) {
        val state = _formState.value
        _formState.value = state.copy(dueTime = time, reminderEnabled = state.reminderEnabled && time != null)
    }

    fun onPriorityToggle() {
        _formState.value = _formState.value.copy(priorityFlag = !_formState.value.priorityFlag)
    }

    fun onNoteChange(value: String) {
        _formState.value = _formState.value.copy(note = value)
    }

    fun onReminderToggle(enabled: Boolean) {
        if (enabled && _formState.value.dueTime == null) return
        _formState.value = _formState.value.copy(reminderEnabled = enabled)
    }

    fun save() {
        val state = _formState.value
        val ownerId = authRepository.currentUserIdOrNull()
        if (ownerId == null) {
            _formState.value = state.copy(error = "You need to be signed in.")
            return
        }
        if (state.title.isBlank()) {
            _formState.value = state.copy(error = "Give this task a title.")
            return
        }

        val reminderTime = if (state.reminderEnabled && state.dueTime != null) {
            state.dueDate.atTime(state.dueTime).atZone(ZoneId.systemDefault()).toInstant()
        } else {
            null
        }

        val task = Task(
            id = UUID.randomUUID().toString(),
            ownerId = ownerId,
            title = state.title.trim(),
            dueDate = state.dueDate,
            dueTime = state.dueTime,
            priorityFlag = state.priorityFlag,
            note = state.note.trim().ifBlank { null },
            reminderTime = reminderTime,
        )

        _formState.value = state.copy(isSaving = true, error = null)
        viewModelScope.launch {
            runCatching { taskRepository.upsertTask(task) }
                .onSuccess {
                    reminderTime?.let { reminderScheduler.scheduleTaskReminder(task.id, task.title, it) }
                    _formState.value = _formState.value.copy(isSaving = false, saved = true)
                }
                .onFailure {
                    _formState.value = _formState.value.copy(
                        isSaving = false,
                        error = it.message ?: "Couldn't save this task.",
                    )
                }
        }
    }
}
