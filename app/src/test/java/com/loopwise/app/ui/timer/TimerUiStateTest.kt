package com.loopwise.app.ui.timer

import com.loopwise.app.domain.model.TimerMode
import org.junit.Assert.assertEquals
import org.junit.Test

class TimerUiStateTest {

    @Test
    fun `stopwatch displaySeconds counts up and has no progress`() {
        val state = TimerUiState(mode = TimerMode.STOPWATCH, elapsedSeconds = 125)
        assertEquals(125L, state.displaySeconds)
        assertEquals(0f, state.progressFraction)
    }

    @Test
    fun `countdown displaySeconds counts down from target`() {
        val state = TimerUiState(mode = TimerMode.COUNTDOWN, targetMinutes = 30, elapsedSeconds = 60)
        assertEquals(30L * 60 - 60, state.displaySeconds)
    }

    @Test
    fun `countdown displaySeconds never goes negative past the target`() {
        val state = TimerUiState(mode = TimerMode.COUNTDOWN, targetMinutes = 1, elapsedSeconds = 90)
        assertEquals(0L, state.displaySeconds)
    }

    @Test
    fun `countdown progressFraction is 0 at start and 1 at target`() {
        val start = TimerUiState(mode = TimerMode.COUNTDOWN, targetMinutes = 10, elapsedSeconds = 0)
        val end = TimerUiState(mode = TimerMode.COUNTDOWN, targetMinutes = 10, elapsedSeconds = 600)
        val half = TimerUiState(mode = TimerMode.COUNTDOWN, targetMinutes = 10, elapsedSeconds = 300)

        assertEquals(0f, start.progressFraction)
        assertEquals(1f, end.progressFraction)
        assertEquals(0.5f, half.progressFraction)
    }

    @Test
    fun `countdown progressFraction is clamped to 1 if elapsed overshoots target`() {
        val state = TimerUiState(mode = TimerMode.COUNTDOWN, targetMinutes = 5, elapsedSeconds = 1_000)
        assertEquals(1f, state.progressFraction)
    }

    @Test
    fun `linkedHabitName resolves from the habits list, null when unset`() {
        val habit = com.loopwise.app.domain.model.Habit(
            id = "h1",
            ownerId = "u1",
            name = "Read",
            color = 0xFF3B82F6,
            type = com.loopwise.app.domain.model.HabitType.DURATION,
        )
        val withLink = TimerUiState(habits = listOf(habit), linkedHabitId = "h1")
        val withoutLink = TimerUiState(habits = listOf(habit), linkedHabitId = null)

        assertEquals("Read", withLink.linkedHabitName)
        assertEquals(null, withoutLink.linkedHabitName)
    }
}
