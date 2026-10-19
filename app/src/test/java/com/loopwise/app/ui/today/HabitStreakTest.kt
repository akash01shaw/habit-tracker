package com.loopwise.app.ui.today

import com.loopwise.app.domain.model.Frequency
import com.loopwise.app.domain.model.Habit
import com.loopwise.app.domain.model.HabitLog
import com.loopwise.app.domain.model.HabitType
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class HabitStreakTest {

    private val today = LocalDate.of(2026, 10, 15)

    private fun habit(type: HabitType = HabitType.BOOLEAN, target: Double? = null) = Habit(
        id = "h1",
        ownerId = "u1",
        name = "Test habit",
        color = 0xFF3B82F6,
        type = type,
        frequency = Frequency.Daily,
        targetValue = target,
    )

    private fun log(date: LocalDate, isDone: Boolean = true, value: Double = 0.0, isSkipped: Boolean = false) = HabitLog(
        id = date.toString(),
        habitId = "h1",
        userId = "u1",
        date = date,
        value = value,
        isDone = isDone,
        isSkipped = isSkipped,
    )

    @Test
    fun `counts consecutive done days ending today`() {
        val logs = listOf(log(today), log(today.minusDays(1)), log(today.minusDays(2)))
        assertEquals(3, computeCurrentStreak(habit(), logs, today))
    }

    @Test
    fun `stops at the first missed scheduled day`() {
        val logs = listOf(log(today), log(today.minusDays(1)), log(today.minusDays(3)))
        assertEquals(2, computeCurrentStreak(habit(), logs, today))
    }

    @Test
    fun `no log for today breaks the streak immediately`() {
        val logs = listOf(log(today.minusDays(1)), log(today.minusDays(2)))
        assertEquals(0, computeCurrentStreak(habit(), logs, today))
    }

    @Test
    fun `counter habit counts a day complete once value reaches target`() {
        val h = habit(type = HabitType.COUNTER, target = 8.0)
        val logs = listOf(log(today, isDone = false, value = 8.0), log(today.minusDays(1), isDone = false, value = 5.0))
        assertEquals(1, computeCurrentStreak(h, logs, today))
    }

    @Test
    fun `isLogComplete is false for a null log`() {
        assertEquals(false, isLogComplete(habit(), null))
    }

    @Test
    fun `a skipped day is transparent - neither counts nor breaks the streak`() {
        val logs = listOf(
            log(today, isSkipped = true),
            log(today.minusDays(1)),
            log(today.minusDays(2)),
        )
        assertEquals(2, computeCurrentStreak(habit(), logs, today))
    }

    @Test
    fun `a skipped day in the middle of a streak keeps both sides connected`() {
        val logs = listOf(
            log(today),
            log(today.minusDays(1), isSkipped = true),
            log(today.minusDays(2)),
        )
        assertEquals(2, computeCurrentStreak(habit(), logs, today))
    }
}
