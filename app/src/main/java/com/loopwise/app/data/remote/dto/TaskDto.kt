package com.loopwise.app.data.remote.dto

import com.loopwise.app.domain.model.Task
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

@Serializable
data class TaskDto(
    val id: String,
    @SerialName("owner_id") val ownerId: String,
    val title: String,
    val color: String = "#3B82F6",
    @SerialName("due_date") val dueDate: String? = null,
    @SerialName("due_time") val dueTime: String? = null,
    @SerialName("priority_flag") val priorityFlag: Boolean = false,
    val note: String? = null,
    @SerialName("reminder_time") val reminderTime: String? = null,
    @SerialName("completed_at") val completedAt: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
)

fun TaskDto.toDomain() = Task(
    id = id,
    ownerId = ownerId,
    title = title,
    color = color.removePrefix("#").toLong(16) or 0xFF000000L,
    dueDate = dueDate?.let(LocalDate::parse),
    dueTime = dueTime?.let(LocalTime::parse),
    priorityFlag = priorityFlag,
    note = note,
    reminderTime = reminderTime?.let(Instant::parse),
    completedAt = completedAt?.let(Instant::parse),
    createdAt = createdAt?.let(Instant::parse) ?: Instant.now(),
)

fun Task.toDto() = TaskDto(
    id = id,
    ownerId = ownerId,
    title = title,
    color = "#%06X".format(color and 0xFFFFFFL),
    dueDate = dueDate?.toString(),
    dueTime = dueTime?.toString(),
    priorityFlag = priorityFlag,
    note = note,
    reminderTime = reminderTime?.toString(),
    completedAt = completedAt?.toString(),
    createdAt = createdAt.toString(),
)
