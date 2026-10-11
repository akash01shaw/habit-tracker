package com.loopwise.app.ui.reports

import com.loopwise.app.domain.model.Habit
import com.loopwise.app.domain.model.HabitLog
import com.loopwise.app.domain.model.HabitType
import com.loopwise.app.ui.today.describe
import com.loopwise.app.ui.today.isScheduledOn
import java.time.LocalDate

enum class ReportRange(val label: String, val days: Int) {
    WEEK("Week", 7),
    MONTH("Month", 30),
    YEAR("Year", 365),
}

/** One cell in the heatmap strip, oldest first. [intensity] is 0f (not done) to 1f (done). */
data class HeatmapDay(val date: LocalDate, val intensity: Float, val isScheduled: Boolean)

data class HabitReport(
    val habit: Habit,
    val completionPercent: Int,
    val streak: Int,
    val heatmap: List<HeatmapDay>,
)

/**
 * Pulls the reporting math out of the ViewModel so it's plain, unit-testable logic:
 * completion % only counts days the habit was actually scheduled on (spec §2.7 —
 * a weekday-only habit shouldn't be penalized for days it was never due), and streak
 * counts consecutive scheduled days ending "today" with no gap.
 */
fun buildHabitReport(habit: Habit, logs: List<HabitLog>, today: LocalDate, range: ReportRange): HabitReport {
    val logsByDate = logs.associateBy { it.date }
    val startDate = today.minusDays((range.days - 1).toLong())
    val dates = generateSequence(startDate) { it.plusDays(1) }.takeWhile { !it.isAfter(today) }.toList()

    val heatmap = dates.map { date ->
        val log = logsByDate[date]
        val scheduled = habit.isScheduledOn(date)
        val intensity = dayIntensity(habit, log)
        HeatmapDay(date = date, intensity = intensity, isScheduled = scheduled)
    }

    val scheduledDays = heatmap.filter { it.isScheduled }
    val completionPercent = if (scheduledDays.isEmpty()) {
        0
    } else {
        val doneCount = scheduledDays.count { it.intensity >= 1f }
        ((doneCount * 100.0) / scheduledDays.size).toInt()
    }

    var streak = 0
    for (day in heatmap.asReversed()) {
        if (!day.isScheduled) continue
        if (day.intensity >= 1f) streak++ else break
    }

    return HabitReport(habit = habit, completionPercent = completionPercent, streak = streak, heatmap = heatmap)
}

/** e.g. "8 cups · Every day" or "Every day" for a boolean habit — mirrors the reference screenshots. */
fun habitTargetDescription(habit: Habit): String {
    val frequencyText = habit.frequency.describe()
    if (habit.type != HabitType.COUNTER && habit.type != HabitType.DURATION) return frequencyText

    val target = habit.targetValue
    val targetText = when {
        target == null -> "-"
        target == target.toLong().toDouble() -> target.toLong().toString()
        else -> target.toString()
    }
    val unit = if (habit.type == HabitType.DURATION) "min" else habit.unit.orEmpty()
    return "$targetText $unit · $frequencyText".replace("  ", " ").trim()
}

private fun dayIntensity(habit: Habit, log: HabitLog?): Float {
    if (log == null) return 0f
    if (log.isDone) return 1f
    val target = habit.targetValue
    if (target == null || target <= 0.0) return 0f
    return (log.value / target).toFloat().coerceIn(0f, 1f)
}
