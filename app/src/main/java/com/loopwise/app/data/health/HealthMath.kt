package com.loopwise.app.data.health

import java.time.Duration
import java.time.Instant

/** Minutes across a day's exercise sessions — pulled out of HealthConnectManager so it's testable without a real Health Connect client. */
fun sumExerciseMinutes(sessions: List<Pair<Instant, Instant>>): Double =
    sessions.sumOf { (start, end) -> Duration.between(start, end).toMinutes().toDouble() }
