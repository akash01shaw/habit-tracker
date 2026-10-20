package com.loopwise.app.data.health

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant

class HealthMathTest {

    @Test
    fun `sums minutes across multiple sessions`() {
        val sessions = listOf(
            Instant.parse("2026-10-20T08:00:00Z") to Instant.parse("2026-10-20T08:30:00Z"), // 30 min
            Instant.parse("2026-10-20T18:00:00Z") to Instant.parse("2026-10-20T18:45:00Z"), // 45 min
        )
        assertEquals(75.0, sumExerciseMinutes(sessions), 0.0)
    }

    @Test
    fun `no sessions yields zero, not a crash`() {
        assertEquals(0.0, sumExerciseMinutes(emptyList()), 0.0)
    }

    @Test
    fun `sub-minute sessions truncate toward zero like Duration toMinutes`() {
        val sessions = listOf(Instant.parse("2026-10-20T08:00:00Z") to Instant.parse("2026-10-20T08:00:40Z"))
        assertEquals(0.0, sumExerciseMinutes(sessions), 0.0)
    }
}
