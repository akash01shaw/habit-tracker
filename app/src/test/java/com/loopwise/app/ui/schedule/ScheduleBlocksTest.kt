package com.loopwise.app.ui.schedule

import com.loopwise.app.domain.model.Frequency
import com.loopwise.app.domain.model.FrequencyKind
import com.loopwise.app.domain.model.Habit
import com.loopwise.app.domain.model.HabitType
import com.loopwise.app.domain.model.Task
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

class ScheduleBlocksTest {

    private val weekDates = (0..6).map { LocalDate.of(2026, 9, 7).plusDays(it.toLong()) } // Mon 9/7..Sun 9/13

    private fun dailyHabitWithReminder(time: LocalTime) = Habit(
        id = "h1",
        ownerId = "u1",
        name = "Read",
        color = 0xFF3B82F6,
        type = HabitType.BOOLEAN,
        frequency = Frequency.Daily,
        reminders = listOf(time),
    )

    @Test
    fun `a daily habit with a reminder produces one block per day in the week`() {
        val habit = dailyHabitWithReminder(LocalTime.of(8, 0))
        val blocks = buildScheduleBlocks(listOf(habit), emptyList(), weekDates)

        assertEquals(7, blocks.size)
        assertTrue(blocks.all { it.time == LocalTime.of(8, 0) && it.kind == ScheduleBlockKind.HABIT })
    }

    @Test
    fun `a habit with no reminders produces no blocks`() {
        val habit = dailyHabitWithReminder(LocalTime.of(8, 0)).copy(reminders = emptyList())
        val blocks = buildScheduleBlocks(listOf(habit), emptyList(), weekDates)
        assertTrue(blocks.isEmpty())
    }

    @Test
    fun `a weekdays-only habit only produces blocks on its scheduled days`() {
        // Mon/Wed/Fri only (ISO 1,3,5)
        val habit = dailyHabitWithReminder(LocalTime.of(7, 30))
            .copy(frequency = Frequency(kind = FrequencyKind.WEEKDAYS, days = listOf(1, 3, 5)))
        val blocks = buildScheduleBlocks(listOf(habit), emptyList(), weekDates)

        assertEquals(3, blocks.size)
        assertEquals(
            setOf(LocalDate.of(2026, 9, 7), LocalDate.of(2026, 9, 9), LocalDate.of(2026, 9, 11)),
            blocks.map { it.date }.toSet(),
        )
    }

    @Test
    fun `a task with no due time is skipped`() {
        val task = Task(id = "t1", ownerId = "u1", title = "No time", dueDate = weekDates[0], dueTime = null)
        val blocks = buildScheduleBlocks(emptyList(), listOf(task), weekDates)
        assertTrue(blocks.isEmpty())
    }

    @Test
    fun `a task outside the given week is skipped`() {
        val task = Task(
            id = "t1",
            ownerId = "u1",
            title = "Next week",
            dueDate = weekDates.last().plusDays(1),
            dueTime = LocalTime.of(9, 0),
        )
        val blocks = buildScheduleBlocks(emptyList(), listOf(task), weekDates)
        assertTrue(blocks.isEmpty())
    }

    @Test
    fun `a task with a due time in the week produces one block`() {
        val task = Task(
            id = "t1",
            ownerId = "u1",
            title = "Dentist",
            dueDate = weekDates[2],
            dueTime = LocalTime.of(14, 0),
        )
        val blocks = buildScheduleBlocks(emptyList(), listOf(task), weekDates)

        assertEquals(1, blocks.size)
        assertEquals(ScheduleBlockKind.TASK, blocks.first().kind)
        assertEquals("Dentist", blocks.first().label)
    }

    @Test
    fun `blocks are sorted by date then time`() {
        val habit = dailyHabitWithReminder(LocalTime.of(20, 0))
        val task = Task(id = "t1", ownerId = "u1", title = "Early", dueDate = weekDates[0], dueTime = LocalTime.of(6, 0))
        val blocks = buildScheduleBlocks(listOf(habit), listOf(task), weekDates)

        assertEquals("Early", blocks.first().label)
        assertEquals(weekDates[0], blocks.first().date)
    }
}
