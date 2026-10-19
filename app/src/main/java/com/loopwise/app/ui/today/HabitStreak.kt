package com.loopwise.app.ui.today

import com.loopwise.app.domain.model.Habit
import com.loopwise.app.domain.model.HabitLog
import java.time.LocalDate

/**
 * Consecutive scheduled days, counting backward from [today], with no miss — mirrors
 * the streak math in HabitReportMath's heatmap, but standalone so the Today screen can
 * show it without pulling in the Reports range machinery. Stops at [lookbackDays] so a
 * very old habit doesn't force scanning its entire history every recomposition.
 */
fun computeCurrentStreak(
    habit: Habit,
    logs: List<HabitLog>,
    today: LocalDate,
    lookbackDays: Int = 400,
): Int {
    val logsByDate = logs.associateBy { it.date }
    var streak = 0
    var date = today
    repeat(lookbackDays) {
        if (habit.isScheduledOn(date)) {
            val log = logsByDate[date]
            when {
                log?.isSkipped == true -> Unit // explicitly skipped — neither counts nor breaks the streak
                isLogComplete(habit, log) -> streak++
                else -> return streak
            }
        }
        date = date.minusDays(1)
    }
    return streak
}

fun isLogComplete(habit: Habit, log: HabitLog?): Boolean {
    if (log == null) return false
    if (log.isDone) return true
    val target = habit.targetValue ?: return false
    if (target <= 0.0) return false
    return log.value >= target
}
