package com.loopwise.app.ui.theme

import org.junit.Assert.assertEquals
import org.junit.Test

class DayPeriodTest {

    @Test
    fun `hours map to the expected band`() {
        assertEquals(DayPeriod.NIGHT, DayPeriod.forHour(0))
        assertEquals(DayPeriod.NIGHT, DayPeriod.forHour(4))
        assertEquals(DayPeriod.DAWN, DayPeriod.forHour(5))
        assertEquals(DayPeriod.DAWN, DayPeriod.forHour(8))
        assertEquals(DayPeriod.DAY, DayPeriod.forHour(9))
        assertEquals(DayPeriod.DAY, DayPeriod.forHour(16))
        assertEquals(DayPeriod.DUSK, DayPeriod.forHour(17))
        assertEquals(DayPeriod.DUSK, DayPeriod.forHour(20))
        assertEquals(DayPeriod.NIGHT, DayPeriod.forHour(21))
        assertEquals(DayPeriod.NIGHT, DayPeriod.forHour(23))
    }

    @Test
    fun `dawn and dusk are 4 hours, day and night are 8 hours`() {
        val bands = (0..23).map { DayPeriod.forHour(it) }
        assertEquals(4, bands.count { it == DayPeriod.DAWN })
        assertEquals(8, bands.count { it == DayPeriod.DAY })
        assertEquals(4, bands.count { it == DayPeriod.DUSK })
        assertEquals(8, bands.count { it == DayPeriod.NIGHT })
    }
}
