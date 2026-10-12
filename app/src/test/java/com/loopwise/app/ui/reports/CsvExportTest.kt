package com.loopwise.app.ui.reports

import com.loopwise.app.domain.model.Habit
import com.loopwise.app.domain.model.HabitLog
import com.loopwise.app.domain.model.HabitType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class CsvExportTest {

    private val habit = Habit(
        id = "h1",
        ownerId = "u1",
        name = "Drink Water",
        color = 0xFF3B82F6,
        type = HabitType.COUNTER,
        targetValue = 8.0,
        unit = "cups",
    )

    private fun log(value: Double, done: Boolean = false) = HabitLog(
        id = "l1",
        habitId = "h1",
        userId = "u1",
        date = LocalDate.of(2026, 10, 12),
        value = value,
        isDone = done,
    )

    @Test
    fun `header row is always present even with no logs`() {
        val csv = buildHabitLogsCsv(emptyList(), emptyList())
        assertEquals("Habit,Type,Date,Value,Unit,Completed,Logged At", csv.trim())
    }

    @Test
    fun `a log row includes the habit name, type, and formatted whole-number value`() {
        val csv = buildHabitLogsCsv(listOf(habit), listOf(log(value = 6.0)))
        val dataLine = csv.lines()[1]

        assertTrue(dataLine.startsWith("Drink Water,counter,2026-10-12,6,cups,no,"))
    }

    @Test
    fun `a log for a deleted habit still exports with a placeholder name instead of crashing`() {
        val csv = buildHabitLogsCsv(emptyList(), listOf(log(value = 1.0)))
        val dataLine = csv.lines()[1]

        assertTrue(dataLine.startsWith("(deleted habit),,2026-10-12,1,,no,"))
    }

    @Test
    fun `a habit name containing a comma is quoted and escaped per CSV rules`() {
        val trickyHabit = habit.copy(name = "Read, then write")
        val csv = buildHabitLogsCsv(listOf(trickyHabit), listOf(log(value = 1.0)))

        assertTrue(csv.lines()[1].startsWith("\"Read, then write\","))
    }

    @Test
    fun `a habit name containing a double quote is escaped by doubling it`() {
        val trickyHabit = habit.copy(name = "Say \"hi\"")
        val csv = buildHabitLogsCsv(listOf(trickyHabit), listOf(log(value = 1.0)))

        assertTrue(csv.lines()[1].startsWith("\"Say \"\"hi\"\"\","))
    }

    @Test
    fun `a fractional value is not truncated to a whole number`() {
        val csv = buildHabitLogsCsv(listOf(habit), listOf(log(value = 4.5)))
        assertTrue(csv.lines()[1].contains(",4.5,"))
    }

    @Test
    fun `completed is rendered as yes or no`() {
        val csv = buildHabitLogsCsv(listOf(habit), listOf(log(value = 8.0, done = true)))
        assertTrue(csv.lines()[1].contains(",yes,"))
    }
}
