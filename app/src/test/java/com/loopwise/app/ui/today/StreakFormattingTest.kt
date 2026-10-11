package com.loopwise.app.ui.today

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Duration

class StreakFormattingTest {

    @Test
    fun `formats seconds-only duration without hours or days`() {
        assertEquals("0m, 45s", formatStreak(Duration.ofSeconds(45)))
    }

    @Test
    fun `formats minutes and seconds`() {
        assertEquals("5m, 3s", formatStreak(Duration.ofSeconds(5 * 60 + 3)))
    }

    @Test
    fun `formats hours minutes and seconds once past an hour`() {
        assertEquals("1h, 0m, 1s", formatStreak(Duration.ofSeconds(3_600 + 1)))
    }

    @Test
    fun `formats days hours minutes and seconds once past a day`() {
        val duration = Duration.ofDays(35).plusHours(1).plusMinutes(15).plusSeconds(31)
        assertEquals("35d, 1h, 15m, 31s", formatStreak(duration))
    }

    @Test
    fun `negative duration is clamped to zero rather than going negative`() {
        assertEquals("0m, 0s", formatStreak(Duration.ofSeconds(-10)))
    }
}
