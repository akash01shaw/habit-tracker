package com.loopwise.app.ui.today

import com.loopwise.app.domain.model.Frequency
import com.loopwise.app.domain.model.FrequencyKind
import com.loopwise.app.domain.model.Habit
import com.loopwise.app.domain.model.HabitType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class HabitSchedulingTest {

    private fun habit(frequency: Frequency) = Habit(
        id = "h1",
        ownerId = "u1",
        name = "Test habit",
        color = 0xFF3B82F6,
        type = HabitType.BOOLEAN,
        frequency = frequency,
    )

    @Test
    fun `daily habit is scheduled on every date`() {
        val h = habit(Frequency.Daily)
        assertTrue(h.isScheduledOn(LocalDate.of(2026, 9, 13))) // Sunday
        assertTrue(h.isScheduledOn(LocalDate.of(2026, 9, 17))) // Thursday
    }

    @Test
    fun `weekdays habit only matches selected ISO days`() {
        // Monday=1 .. Sunday=7. Select Mon/Wed/Fri.
        val h = habit(Frequency(kind = FrequencyKind.WEEKDAYS, days = listOf(1, 3, 5)))

        assertTrue(h.isScheduledOn(LocalDate.of(2026, 9, 14))) // Monday
        assertTrue(h.isScheduledOn(LocalDate.of(2026, 9, 16))) // Wednesday
        assertFalse(h.isScheduledOn(LocalDate.of(2026, 9, 15))) // Tuesday
        assertFalse(h.isScheduledOn(LocalDate.of(2026, 9, 13))) // Sunday
    }

    @Test
    fun `weekdays habit with no days selected matches nothing`() {
        val h = habit(Frequency(kind = FrequencyKind.WEEKDAYS, days = null))
        assertFalse(h.isScheduledOn(LocalDate.of(2026, 9, 14)))
    }

    @Test
    fun `per week and per month habits show every day in the MVP`() {
        val perWeek = habit(Frequency(kind = FrequencyKind.PER_WEEK, count = 3))
        val perMonth = habit(Frequency(kind = FrequencyKind.PER_MONTH, count = 10))
        val someDate = LocalDate.of(2026, 9, 15)

        assertTrue(perWeek.isScheduledOn(someDate))
        assertTrue(perMonth.isScheduledOn(someDate))
    }

    @Test
    fun `describe formats each frequency kind`() {
        assertEquals("Every day", Frequency.Daily.describe())
        assertEquals("Select days", Frequency(kind = FrequencyKind.WEEKDAYS).describe())
        assertEquals("3x per week", Frequency(kind = FrequencyKind.PER_WEEK, count = 3).describe())
        assertEquals("1x per week", Frequency(kind = FrequencyKind.PER_WEEK, count = null).describe())
        assertEquals("10x per month", Frequency(kind = FrequencyKind.PER_MONTH, count = 10).describe())
    }
}
