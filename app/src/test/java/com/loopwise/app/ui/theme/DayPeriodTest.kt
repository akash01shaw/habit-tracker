package com.loopwise.app.ui.theme

import org.junit.Assert.assertEquals
import org.junit.Test

class DayPeriodTest {

    @Test
    fun `hours map to the expected band`() {
        assertEquals(DayPeriod.NIGHT, DayPeriod.forHour(0))
        assertEquals(DayPeriod.NIGHT, DayPeriod.forHour(4))
        assertEquals(DayPeriod.DAWN, DayPeriod.forHour(5))
        assertEquals(DayPeriod.DAWN, DayPeriod.forHour(10))
        assertEquals(DayPeriod.DAY, DayPeriod.forHour(11))
        assertEquals(DayPeriod.DAY, DayPeriod.forHour(16))
        assertEquals(DayPeriod.DUSK, DayPeriod.forHour(17))
        assertEquals(DayPeriod.DUSK, DayPeriod.forHour(22))
        assertEquals(DayPeriod.NIGHT, DayPeriod.forHour(23))
    }

    @Test
    fun `every band covers exactly a 6-hour window`() {
        val bands = (0..23).map { DayPeriod.forHour(it) }
        DayPeriod.entries.forEach { period ->
            assertEquals(6, bands.count { it == period })
        }
    }
}
