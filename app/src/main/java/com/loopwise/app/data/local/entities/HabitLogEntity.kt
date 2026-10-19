package com.loopwise.app.data.local.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.loopwise.app.domain.model.HabitLog
import java.time.Instant
import java.time.LocalDate

@Entity(
    tableName = "habit_logs",
    indices = [Index(value = ["habitId", "userId", "date"], unique = true)],
)
data class HabitLogEntity(
    @PrimaryKey val id: String,
    val habitId: String,
    val userId: String,
    val date: LocalDate,
    val value: Double,
    val isDone: Boolean,
    val isSkipped: Boolean = false,
    val note: String?,
    val loggedAt: Instant,
    val isSynced: Boolean = true,
)

fun HabitLogEntity.toDomain() = HabitLog(
    id = id,
    habitId = habitId,
    userId = userId,
    date = date,
    value = value,
    isDone = isDone,
    isSkipped = isSkipped,
    note = note,
    loggedAt = loggedAt,
)

fun HabitLog.toEntity(isSynced: Boolean = true) = HabitLogEntity(
    id = id,
    habitId = habitId,
    userId = userId,
    date = date,
    value = value,
    isDone = isDone,
    isSkipped = isSkipped,
    note = note,
    loggedAt = loggedAt,
    isSynced = isSynced,
)
