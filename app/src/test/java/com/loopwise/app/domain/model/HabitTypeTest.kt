package com.loopwise.app.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class HabitTypeTest {

    @Test
    fun `wireValue round-trips through fromWire for every type`() {
        HabitType.entries.forEach { type ->
            assertEquals(type, HabitType.fromWire(type.wireValue))
        }
    }

    @Test
    fun `fromWire falls back to BOOLEAN for an unknown value`() {
        assertEquals(HabitType.BOOLEAN, HabitType.fromWire("not-a-real-type"))
    }

    @Test
    fun `wireValue is lowercase snake-free name matching the SQL check constraint`() {
        assertEquals("boolean", HabitType.BOOLEAN.wireValue)
        assertEquals("counter", HabitType.COUNTER.wireValue)
        assertEquals("duration", HabitType.DURATION.wireValue)
        assertEquals("avoidance", HabitType.AVOIDANCE.wireValue)
    }
}
