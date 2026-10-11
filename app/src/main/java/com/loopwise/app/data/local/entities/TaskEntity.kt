package com.loopwise.app.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.loopwise.app.domain.model.Task
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey val id: String,
    val ownerId: String,
    val title: String,
    val dueDate: LocalDate?,
    val dueTime: LocalTime?,
    val priorityFlag: Boolean,
    val note: String?,
    val reminderTime: Instant?,
    val completedAt: Instant?,
    val createdAt: Instant,
    val isSynced: Boolean = true,
    val isDeleted: Boolean = false,
)

fun TaskEntity.toDomain() = Task(
    id = id,
    ownerId = ownerId,
    title = title,
    dueDate = dueDate,
    dueTime = dueTime,
    priorityFlag = priorityFlag,
    note = note,
    reminderTime = reminderTime,
    completedAt = completedAt,
    createdAt = createdAt,
)

fun Task.toEntity(isSynced: Boolean = true) = TaskEntity(
    id = id,
    ownerId = ownerId,
    title = title,
    dueDate = dueDate,
    dueTime = dueTime,
    priorityFlag = priorityFlag,
    note = note,
    reminderTime = reminderTime,
    completedAt = completedAt,
    createdAt = createdAt,
    isSynced = isSynced,
)
