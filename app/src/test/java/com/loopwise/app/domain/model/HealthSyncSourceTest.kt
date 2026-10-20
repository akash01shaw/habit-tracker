package com.loopwise.app.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class HealthSyncSourceTest {

    @Test
    fun `wireValue round-trips through fromWire for every source`() {
        HealthSyncSource.entries.forEach { source ->
            assertEquals(source, HealthSyncSource.fromWire(source.wireValue))
        }
    }

    @Test
    fun `fromWire returns null for an unknown or null value`() {
        assertNull(HealthSyncSource.fromWire("workouts"))
        assertNull(HealthSyncSource.fromWire(null))
    }

    @Test
    fun `wireValue matches the existing Habit healthSyncSource convention`() {
        assertEquals("steps", HealthSyncSource.STEPS.wireValue)
        assertEquals("active_minutes", HealthSyncSource.ACTIVE_MINUTES.wireValue)
    }
}
