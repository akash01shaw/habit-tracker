package com.loopwise.app.ui.reports

import com.loopwise.app.domain.model.Frequency
import com.loopwise.app.domain.model.FrequencyKind
import com.loopwise.app.domain.model.Habit
import com.loopwise.app.domain.model.HabitLog
import com.loopwise.app.domain.model.HabitType
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.util.UUID

class HabitReportMathTest {

    private val today = LocalDate.of(2026, 9, 13) // a Sunday

    private fun dailyHabit(target: Double? = null) = Habit(
        id = "h1",
        ownerId = "u1",
        name = "Drink water",
        color = 0xFF3B82F6,
        type = if (target != null) HabitType.COUNTER else HabitType.BOOLEAN,
        frequency = Frequency.Daily,
        targetValue = target,
    )

    private fun log(date: LocalDate, value: Double = 0.0, isDone: Boolean = false) = HabitLog(
        id = UUID.randomUUID().toString(),
        habitId = "h1",
        userId = "u1",
        date = date,
        value = value,
        isDone = isDone,
    )

    @Test
    fun `boolean habit done every day in range scores 100 percent with full streak`() {
        val habit = dailyHabit()
        val logs = (0..6).map { today.minusDays(it.toLong()) }.map { log(it, isDone = true) }

        val report = buildHabitReport(habit, logs, today, ReportRange.WEEK)

        assertEquals(100, report.completionPercent)
        assertEquals(7, report.streak)
    }

    @Test
    fun `a gap breaks the streak but only lowers completion proportionally`() {
        val habit = dailyHabit()
        // Done today and yesterday, missed 2 days ago, done further back.
        val logs = listOf(
            log(today, isDone = true),
            log(today.minusDays(1), isDone = true),
            // today.minusDays(2) intentionally has no log => not done
            log(today.minusDays(3), isDone = true),
        )

        val report = buildHabitReport(habit, logs, today, ReportRange.WEEK)

        assertEquals(2, report.streak) // stops at the gap 2 days ago
        assertEquals(42, report.completionPercent) // 3 done / 7 scheduled days, truncated
    }

    @Test
    fun `weekdays-only habit is not penalized for non-scheduled days`() {
        // Frequency: Mon/Wed/Fri only (ISO 1,3,5). "today" 2026-09-13 is a Sunday.
        val habit = dailyHabit().copy(frequency = Frequency(kind = FrequencyKind.WEEKDAYS, days = listOf(1, 3, 5)))
        // The only scheduled days in the last 7 (2026-09-07..2026-09-13) are Mon 9/7... wait
        // Monday in that window is 2026-09-07; Wednesday 2026-09-09; Friday 2026-09-11.
        val logs = listOf(
            log(LocalDate.of(2026, 9, 7), isDone = true),
            log(LocalDate.of(2026, 9, 9), isDone = true),
            log(LocalDate.of(2026, 9, 11), isDone = true),
        )

        val report = buildHabitReport(habit, logs, today, ReportRange.WEEK)

        assertEquals(100, report.completionPercent) // all 3 scheduled days done
    }

    @Test
    fun `counter habit below target contributes partial heatmap intensity but does not count as done`() {
        val habit = dailyHabit(target = 8.0)
        val logs = listOf(log(today, value = 4.0, isDone = false))

        val report = buildHabitReport(habit, logs, today, ReportRange.WEEK)

        val todayCell = report.heatmap.last()
        assertEquals(0.5f, todayCell.intensity)
        assertEquals(0, report.streak) // not done, so no streak
    }

    @Test
    fun `no logs at all yields zero percent and zero streak, not a crash`() {
        val habit = dailyHabit()
        val report = buildHabitReport(habit, emptyList(), today, ReportRange.WEEK)

        assertEquals(0, report.completionPercent)
        assertEquals(0, report.streak)
        assertEquals(7, report.heatmap.size)
    }

    @Test
    fun `heatmap covers exactly the range length ending today`() {
        val habit = dailyHabit()
        val report = buildHabitReport(habit, emptyList(), today, ReportRange.MONTH)

        assertEquals(30, report.heatmap.size)
        assertEquals(today, report.heatmap.last().date)
        assertEquals(today.minusDays(29), report.heatmap.first().date)
    }
}
