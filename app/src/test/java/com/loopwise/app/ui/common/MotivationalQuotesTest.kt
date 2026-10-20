package com.loopwise.app.ui.common

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class MotivationalQuotesTest {

    @Test
    fun `same date always yields the same quote`() {
        val date = LocalDate.of(2026, 10, 20)
        assertEquals(MotivationalQuotes.forDate(date), MotivationalQuotes.forDate(date))
    }

    @Test
    fun `result is always one of the listed quotes, for dates before and after the epoch`() {
        val dates = listOf(
            LocalDate.of(1970, 1, 1),
            LocalDate.of(1950, 6, 15),
            LocalDate.of(2026, 10, 20),
            LocalDate.of(2099, 12, 31),
        )
        dates.forEach { date ->
            assertTrue(MotivationalQuotes.forDate(date) in MotivationalQuotes.all)
        }
    }

    @Test
    fun `consecutive days do not always collide, confirming it is not a constant`() {
        val start = LocalDate.of(2026, 1, 1)
        val quotesSeen = (0 until MotivationalQuotes.all.size).map { MotivationalQuotes.forDate(start.plusDays(it.toLong())) }.toSet()
        assertTrue(quotesSeen.size > 1)
    }
}
