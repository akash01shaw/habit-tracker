package com.loopwise.app.ui.habitedit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.loopwise.app.data.repository.AuthRepository
import com.loopwise.app.data.repository.HabitRepository
import com.loopwise.app.domain.model.Frequency
import com.loopwise.app.domain.model.FrequencyKind
import com.loopwise.app.domain.model.Habit
import com.loopwise.app.domain.model.HabitType
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.loopwise.app.notifications.ReminderScheduler
import com.loopwise.app.ui.theme.BuildPalette
import com.loopwise.app.ui.theme.QuitPalette
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalTime
import java.util.UUID

data class CreateHabitFormState(
    val name: String = "",
    val type: HabitType = HabitType.BOOLEAN,
    val color: Long = BuildPalette.Blue.toArgbLong(),
    val frequencyKind: FrequencyKind = FrequencyKind.DAILY,
    val selectedWeekdays: Set<Int> = emptySet(),
    val perPeriodCount: Int = 1,
    val targetValue: String = "",
    val unit: String = "",
    val reminders: List<LocalTime> = emptyList(),
    val isSaving: Boolean = false,
    val error: String? = null,
    val saved: Boolean = false,
) {
    val availableColors: List<Long>
        get() = (if (type == HabitType.AVOIDANCE) QuitPalette.all else BuildPalette.all).map { it.toArgbLong() }

    val needsTarget: Boolean get() = type == HabitType.COUNTER || type == HabitType.DURATION
}

private fun Color.toArgbLong(): Long = toArgb().toLong() and 0xFFFFFFFFL

class CreateEditHabitViewModel(
    private val authRepository: AuthRepository,
    private val habitRepository: HabitRepository,
    private val reminderScheduler: ReminderScheduler,
) : ViewModel() {

    private val _formState = MutableStateFlow(CreateHabitFormState())
    val formState: StateFlow<CreateHabitFormState> = _formState.asStateFlow()

    fun onNameChange(value: String) {
        _formState.value = _formState.value.copy(name = value, error = null)
    }

    fun onTypeChange(type: HabitType) {
        val palette = if (type == HabitType.AVOIDANCE) QuitPalette.all else BuildPalette.all
        _formState.value = _formState.value.copy(type = type, color = palette.first().toArgbLong())
    }

    fun onColorChange(color: Long) {
        _formState.value = _formState.value.copy(color = color)
    }

    fun onFrequencyKindChange(kind: FrequencyKind) {
        _formState.value = _formState.value.copy(frequencyKind = kind)
    }

    fun onWeekdayToggle(dayOfWeek: Int) {
        val current = _formState.value.selectedWeekdays
        _formState.value = _formState.value.copy(
            selectedWeekdays = if (dayOfWeek in current) current - dayOfWeek else current + dayOfWeek,
        )
    }

    fun onPerPeriodCountChange(count: Int) {
        _formState.value = _formState.value.copy(perPeriodCount = count.coerceAtLeast(1))
    }

    fun onTargetValueChange(value: String) {
        _formState.value = _formState.value.copy(targetValue = value, error = null)
    }

    fun onUnitChange(value: String) {
        _formState.value = _formState.value.copy(unit = value)
    }

    fun addReminder(time: LocalTime) {
        val current = _formState.value.reminders
        if (time in current) return
        _formState.value = _formState.value.copy(reminders = (current + time).sorted())
    }

    fun removeReminder(time: LocalTime) {
        _formState.value = _formState.value.copy(reminders = _formState.value.reminders - time)
    }

    fun save() {
        val state = _formState.value
        val ownerId = authRepository.currentUserIdOrNull()
        if (ownerId == null) {
            _formState.value = state.copy(error = "You need to be signed in.")
            return
        }
        if (state.name.isBlank()) {
            _formState.value = state.copy(error = "Give this habit a name.")
            return
        }
        val target = state.targetValue.toDoubleOrNull()
        if (state.needsTarget && target == null) {
            _formState.value = state.copy(error = "Enter a target value.")
            return
        }

        val frequency = Frequency(
            kind = state.frequencyKind,
            days = state.selectedWeekdays.takeIf { state.frequencyKind == FrequencyKind.WEEKDAYS }?.toList(),
            count = state.perPeriodCount.takeIf {
                state.frequencyKind == FrequencyKind.PER_WEEK || state.frequencyKind == FrequencyKind.PER_MONTH
            },
        )

        val habit = Habit(
            id = UUID.randomUUID().toString(),
            ownerId = ownerId,
            name = state.name.trim(),
            color = state.color,
            type = state.type,
            frequency = frequency,
            targetValue = target,
            unit = state.unit.trim().ifBlank { null },
            reminders = state.reminders,
        )

        _formState.value = state.copy(isSaving = true, error = null)
        viewModelScope.launch {
            runCatching { habitRepository.upsertHabit(habit) }
                .onSuccess {
                    habit.reminders.forEach { time ->
                        reminderScheduler.scheduleHabitReminder(habit.id, habit.name, time)
                    }
                    _formState.value = _formState.value.copy(isSaving = false, saved = true)
                }
                .onFailure {
                    _formState.value = _formState.value.copy(
                        isSaving = false,
                        error = it.message ?: "Couldn't save this habit.",
                    )
                }
        }
    }
}
