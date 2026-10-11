package com.loopwise.app.ui.today

import com.loopwise.app.domain.model.Frequency
import com.loopwise.app.domain.model.FrequencyKind
import com.loopwise.app.domain.model.Habit
import java.time.LocalDate

/**
 * Whether a habit should appear on the Today list for [date]. DAILY and WEEKDAYS are
 * exact per-day rules; PER_WEEK/PER_MONTH ("X times per week/month") don't pin a habit
 * to specific days, so for the MVP we surface them every day and let the target/count
 * on the habit itself communicate the goal — exact scheduling logic is a later refinement.
 */
fun Habit.isScheduledOn(date: LocalDate): Boolean = when (frequency.kind) {
    FrequencyKind.DAILY -> true
    FrequencyKind.WEEKDAYS -> frequency.days.orEmpty().contains(date.dayOfWeek.value)
    FrequencyKind.PER_WEEK, FrequencyKind.PER_MONTH -> true
}

fun Frequency.describe(): String = when (kind) {
    FrequencyKind.DAILY -> "Every day"
    FrequencyKind.WEEKDAYS -> "Select days"
    FrequencyKind.PER_WEEK -> "${count ?: 1}x per week"
    FrequencyKind.PER_MONTH -> "${count ?: 1}x per month"
}
