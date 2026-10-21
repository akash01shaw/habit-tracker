package com.loopwise.app.ui.taskedit

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.loopwise.app.data.repository.AuthRepository
import com.loopwise.app.data.repository.TaskRepository
import com.loopwise.app.domain.model.Task
import com.loopwise.app.notifications.ReminderScheduler
import com.loopwise.app.ui.theme.BuildPalette
import com.loopwise.app.ui.theme.DayPeriod
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.util.UUID

data class CreateTaskFormState(
    val taskId: String? = null,
    val ownerId: String? = null,
    val createdAt: Instant? = null,
    val completedAt: Instant? = null,
    val originalReminderTime: Instant? = null,
    val title: String = "",
    val color: Long = BuildPalette.Blue.toArgbLong(),
    val dueDate: LocalDate = LocalDate.now(),
    val dueTime: LocalTime? = null,
    val priorityFlag: Boolean = false,
    val note: String = "",
    val reminderEnabled: Boolean = false,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val isDeleting: Boolean = false,
    val error: String? = null,
    val saved: Boolean = false,
    val deleted: Boolean = false,
) {
    val isEditing: Boolean get() = taskId != null
    val canEnableReminder: Boolean get() = dueTime != null
    val availableColors: List<Long> get() = BuildPalette.all.map { it.toArgbLong() }
}

private fun Color.toArgbLong(): Long = toArgb().toLong() and 0xFFFFFFFFL

class CreateTaskViewModel(
    private val authRepository: AuthRepository,
    private val taskRepository: TaskRepository,
    private val reminderScheduler: ReminderScheduler,
) : ViewModel() {

    private val _formState = MutableStateFlow(CreateTaskFormState())
    val formState: StateFlow<CreateTaskFormState> = _formState.asStateFlow()

    private var loadedTaskId: String? = null

    /** Called once from the screen when opened in edit mode. A no-op if already loaded for this id. */
    fun loadForEdit(taskId: String) {
        if (loadedTaskId == taskId) return
        loadedTaskId = taskId
        _formState.value = _formState.value.copy(isLoading = true)
        viewModelScope.launch {
            val task = taskRepository.getTask(taskId)
            if (task == null) {
                _formState.value = _formState.value.copy(isLoading = false, error = "Couldn't find this task.")
                return@launch
            }
            _formState.value = CreateTaskFormState(
                taskId = task.id,
                ownerId = task.ownerId,
                createdAt = task.createdAt,
                completedAt = task.completedAt,
                originalReminderTime = task.reminderTime,
                title = task.title,
                color = task.color,
                dueDate = task.dueDate ?: LocalDate.now(),
                dueTime = task.dueTime,
                priorityFlag = task.priorityFlag,
                note = task.note.orEmpty(),
                reminderEnabled = task.reminderTime != null,
            )
        }
    }

    /**
     * Called once, only for a brand-new (not-yet-saved) task, so it starts out matching the
     * current time-of-day theme's "to-do" accent instead of always defaulting to blue — the
     * same reasoning as CreateEditHabitViewModel.seedThemeColorForNewHabit.
     */
    fun seedThemeColorForNewTask(period: DayPeriod) {
        _formState.value = _formState.value.copy(color = period.accentTodo.toArgbLong())
    }

    fun onTitleChange(value: String) {
        _formState.value = _formState.value.copy(title = value, error = null)
    }

    fun onColorChange(color: Long) {
        _formState.value = _formState.value.copy(color = color)
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
        val ownerId = state.ownerId ?: authRepository.currentUserIdOrNull()
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
            id = state.taskId ?: UUID.randomUUID().toString(),
            ownerId = ownerId,
            title = state.title.trim(),
            color = state.color,
            dueDate = state.dueDate,
            dueTime = state.dueTime,
            priorityFlag = state.priorityFlag,
            note = state.note.trim().ifBlank { null },
            reminderTime = reminderTime,
            completedAt = state.completedAt,
            createdAt = state.createdAt ?: Instant.now(),
        )

        _formState.value = state.copy(isSaving = true, error = null)
        viewModelScope.launch {
            runCatching { taskRepository.upsertTask(task) }
                .onSuccess {
                    // A reschedule overwrites the existing alarm for this task id automatically;
                    // only an explicit cancel is needed when the reminder was removed outright.
                    if (reminderTime != null) {
                        reminderScheduler.scheduleTaskReminder(task.id, task.title, reminderTime)
                    } else if (state.originalReminderTime != null) {
                        reminderScheduler.cancelTaskReminder(task.id)
                    }
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

    fun delete() {
        val state = _formState.value
        val taskId = state.taskId ?: return
        _formState.value = state.copy(isDeleting = true, error = null)
        viewModelScope.launch {
            runCatching { taskRepository.deleteTask(taskId) }
                .onSuccess {
                    if (state.originalReminderTime != null) reminderScheduler.cancelTaskReminder(taskId)
                    _formState.value = _formState.value.copy(isDeleting = false, deleted = true)
                }
                .onFailure {
                    _formState.value = _formState.value.copy(
                        isDeleting = false,
                        error = it.message ?: "Couldn't delete this task.",
                    )
                }
        }
    }
}
