package com.loopwise.app.domain.model

import java.time.Instant
import java.time.LocalTime

data class Habit(
    val id: String,
    val ownerId: String,
    val name: String,
    val color: Long,
    val icon: String? = null,
    val type: HabitType,
    val frequency: Frequency = Frequency.Daily,
    val targetValue: Double? = null,
    val unit: String? = null,
    val reminders: List<LocalTime> = emptyList(),
    val healthSyncSource: String? = null,
    val isShared: Boolean = false,
    val createdAt: Instant = Instant.now(),
) {
    val isAvoidance: Boolean get() = type == HabitType.AVOIDANCE
}
