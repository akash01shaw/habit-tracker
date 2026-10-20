package com.loopwise.app.ui.common

import org.junit.Assert.assertEquals
import org.junit.Test

class DayPeriodTest {

    @Test
    fun `hours map to the expected band`() {
        assertEquals(DayPeriod.NIGHT, DayPeriod.forHour(0))
        assertEquals(DayPeriod.NIGHT, DayPeriod.forHour(4))
        assertEquals(DayPeriod.DAWN, DayPeriod.forHour(5))
        assertEquals(DayPeriod.DAWN, DayPeriod.forHour(7))
        assertEquals(DayPeriod.DAY, DayPeriod.forHour(8))
        assertEquals(DayPeriod.DAY, DayPeriod.forHour(16))
        assertEquals(DayPeriod.DUSK, DayPeriod.forHour(17))
        assertEquals(DayPeriod.DUSK, DayPeriod.forHour(19))
        assertEquals(DayPeriod.NIGHT, DayPeriod.forHour(20))
        assertEquals(DayPeriod.NIGHT, DayPeriod.forHour(23))
    }

    @Test
    fun `every band covers a distinct part of the day with no gaps`() {
        val bands = (0..23).map { DayPeriod.forHour(it) }.toSet()
        assertEquals(DayPeriod.entries.toSet(), bands)
    }
}
