package com.loopwise.app.ui.habitedit

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.loopwise.app.data.health.HealthConnectManager
import com.loopwise.app.data.repository.AuthRepository
import com.loopwise.app.data.repository.HabitRepository
import com.loopwise.app.domain.model.Frequency
import com.loopwise.app.domain.model.FrequencyKind
import com.loopwise.app.domain.model.Habit
import com.loopwise.app.domain.model.HabitType
import com.loopwise.app.domain.model.HealthSyncSource
import com.loopwise.app.notifications.ReminderScheduler
import com.loopwise.app.ui.theme.BuildPalette
import com.loopwise.app.ui.theme.DayPeriod
import com.loopwise.app.ui.theme.QuitPalette
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalTime
import java.util.UUID

data class CreateHabitFormState(
    val habitId: String? = null,
    val ownerId: String? = null,
    val createdAt: Instant? = null,
    val originalReminders: List<LocalTime> = emptyList(),
    val name: String = "",
    val type: HabitType = HabitType.BOOLEAN,
    val color: Long = BuildPalette.Blue.toArgbLong(),
    val frequencyKind: FrequencyKind = FrequencyKind.DAILY,
    val selectedWeekdays: Set<Int> = emptySet(),
    val perPeriodCount: Int = 1,
    val targetValue: String = "",
    val unit: String = "",
    val healthSyncSource: HealthSyncSource? = null,
    val reminders: List<LocalTime> = emptyList(),
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val isDeleting: Boolean = false,
    val error: String? = null,
    val saved: Boolean = false,
    val deleted: Boolean = false,
) {
    val isEditing: Boolean get() = habitId != null

    val availableColors: List<Long>
        get() = (if (type == HabitType.AVOIDANCE) QuitPalette.all else BuildPalette.all).map { it.toArgbLong() }

    val needsTarget: Boolean get() = type == HabitType.COUNTER || type == HabitType.DURATION
}

private fun Color.toArgbLong(): Long = toArgb().toLong() and 0xFFFFFFFFL

class CreateEditHabitViewModel(
    private val authRepository: AuthRepository,
    private val habitRepository: HabitRepository,
    private val reminderScheduler: ReminderScheduler,
    private val healthConnectManager: HealthConnectManager,
) : ViewModel() {

    /** The permission set the Screen's ActivityResultLauncher requests — sourced from one place. */
    val healthPermissions: Set<String> get() = healthConnectManager.permissions

    private val _formState = MutableStateFlow(CreateHabitFormState())
    val formState: StateFlow<CreateHabitFormState> = _formState.asStateFlow()

    private var loadedHabitId: String? = null

    // Replaces the flat BuildPalette.Blue/QuitPalette.first() reset below once a brand-new
    // habit's form has been seeded with the current time-of-day theme's colors (see
    // seedThemeColorForNewHabit). Edit mode never calls that, so these just keep today's
    // old behavior for an in-progress edit that switches habit type.
    private var themeBuildColor: Long = BuildPalette.Blue.toArgbLong()
    private var themeQuitColor: Long = QuitPalette.Charcoal.toArgbLong()

    /**
     * Called once, only for a brand-new (not-yet-saved) habit, so it starts out matching the
     * current time-of-day theme — vivid [DayPeriod.accentHabit] for build habits, the same
     * muted [DayPeriod.accentQuit] the "Habits I'm Quitting" section already uses for
     * avoidance habits — instead of always defaulting to blue regardless of time of day.
     */
    fun seedThemeColorForNewHabit(period: DayPeriod) {
        themeBuildColor = period.accentHabit.toArgbLong()
        themeQuitColor = period.accentQuit.toArgbLong()
        val current = _formState.value
        _formState.value = current.copy(
            color = if (current.type == HabitType.AVOIDANCE) themeQuitColor else themeBuildColor,
        )
    }

    /** Called once from the screen when opened in edit mode. A no-op if already loaded for this id. */
    fun loadForEdit(habitId: String) {
        if (loadedHabitId == habitId) return
        loadedHabitId = habitId
        _formState.value = _formState.value.copy(isLoading = true)
        viewModelScope.launch {
            val habit = habitRepository.getHabit(habitId)
            if (habit == null) {
                _formState.value = _formState.value.copy(isLoading = false, error = "Couldn't find this habit.")
                return@launch
            }
            _formState.value = CreateHabitFormState(
                habitId = habit.id,
                ownerId = habit.ownerId,
                createdAt = habit.createdAt,
                originalReminders = habit.reminders,
                name = habit.name,
                type = habit.type,
                color = habit.color,
                frequencyKind = habit.frequency.kind,
                selectedWeekdays = habit.frequency.days?.toSet() ?: emptySet(),
                perPeriodCount = habit.frequency.count ?: 1,
                targetValue = habit.targetValue?.let { if (it == it.toLong().toDouble()) it.toLong().toString() else it.toString() } ?: "",
                unit = habit.unit.orEmpty(),
                healthSyncSource = HealthSyncSource.fromWire(habit.healthSyncSource),
                reminders = habit.reminders,
            )
        }
    }

    fun onNameChange(value: String) {
        _formState.value = _formState.value.copy(name = value, error = null)
    }

    fun onTypeChange(type: HabitType) {
        val needsTarget = type == HabitType.COUNTER || type == HabitType.DURATION
        _formState.value = _formState.value.copy(
            type = type,
            color = if (type == HabitType.AVOIDANCE) themeQuitColor else themeBuildColor,
            healthSyncSource = _formState.value.healthSyncSource.takeIf { needsTarget },
        )
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

    fun onHealthSyncSourceChange(source: HealthSyncSource?) {
        _formState.value = _formState.value.copy(healthSyncSource = source)
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
        val ownerId = state.ownerId ?: authRepository.currentUserIdOrNull()
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
            id = state.habitId ?: UUID.randomUUID().toString(),
            ownerId = ownerId,
            name = state.name.trim(),
            color = state.color,
            type = state.type,
            frequency = frequency,
            targetValue = target,
            unit = state.unit.trim().ifBlank { null },
            healthSyncSource = state.healthSyncSource?.wireValue,
            reminders = state.reminders,
            createdAt = state.createdAt ?: Instant.now(),
        )

        _formState.value = state.copy(isSaving = true, error = null)
        viewModelScope.launch {
            runCatching { habitRepository.upsertHabit(habit) }
                .onSuccess {
                    // Original reminder times no longer present get cancelled rather than left
                    // dangling as orphaned alarms; everything currently set gets (re)scheduled.
                    state.originalReminders.filterNot { it in habit.reminders }
                        .forEach { reminderScheduler.cancelHabitReminder(habit.id, it) }
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

    fun delete() {
        val state = _formState.value
        val habitId = state.habitId ?: return
        _formState.value = state.copy(isDeleting = true, error = null)
        viewModelScope.launch {
            runCatching { habitRepository.deleteHabit(habitId) }
                .onSuccess {
                    state.reminders.forEach { reminderScheduler.cancelHabitReminder(habitId, it) }
                    _formState.value = _formState.value.copy(isDeleting = false, deleted = true)
                }
                .onFailure {
                    _formState.value = _formState.value.copy(
                        isDeleting = false,
                        error = it.message ?: "Couldn't delete this habit.",
                    )
                }
        }
    }
}
