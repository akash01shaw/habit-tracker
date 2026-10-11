package com.loopwise.app.domain.model

import java.time.Instant
import java.time.LocalDate

data class HabitLog(
    val id: String,
    val habitId: String,
    val userId: String,
    val date: LocalDate,
    val value: Double = 0.0,
    val isDone: Boolean = false,
    val note: String? = null,
    val loggedAt: Instant = Instant.now(),
)
