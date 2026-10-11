package com.loopwise.app.domain.model

import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

data class Task(
    val id: String,
    val ownerId: String,
    val title: String,
    val dueDate: LocalDate? = null,
    val dueTime: LocalTime? = null,
    val priorityFlag: Boolean = false,
    val note: String? = null,
    val reminderTime: Instant? = null,
    val completedAt: Instant? = null,
    val createdAt: Instant = Instant.now(),
) {
    val isCompleted: Boolean get() = completedAt != null
}
