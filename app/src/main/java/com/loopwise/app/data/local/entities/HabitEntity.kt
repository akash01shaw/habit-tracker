package com.loopwise.app.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.loopwise.app.domain.model.Frequency
import com.loopwise.app.domain.model.Habit
import com.loopwise.app.domain.model.HabitType
import java.time.Instant
import java.time.LocalTime

@Entity(tableName = "habits")
data class HabitEntity(
    @PrimaryKey val id: String,
    val ownerId: String,
    val name: String,
    val color: Long,
    val icon: String?,
    val type: HabitType,
    val frequency: Frequency,
    val targetValue: Double?,
    val unit: String?,
    val reminders: List<LocalTime>,
    val healthSyncSource: String?,
    val isShared: Boolean,
    val createdAt: Instant,
    // Local-only bookkeeping for the offline-first push-on-write sync (not sent to Supabase).
    val isSynced: Boolean = true,
    val isDeleted: Boolean = false,
)

fun HabitEntity.toDomain() = Habit(
    id = id,
    ownerId = ownerId,
    name = name,
    color = color,
    icon = icon,
    type = type,
    frequency = frequency,
    targetValue = targetValue,
    unit = unit,
    reminders = reminders,
    healthSyncSource = healthSyncSource,
    isShared = isShared,
    createdAt = createdAt,
)

fun Habit.toEntity(isSynced: Boolean = true) = HabitEntity(
    id = id,
    ownerId = ownerId,
    name = name,
    color = color,
    icon = icon,
    type = type,
    frequency = frequency,
    targetValue = targetValue,
    unit = unit,
    reminders = reminders,
    healthSyncSource = healthSyncSource,
    isShared = isShared,
    createdAt = createdAt,
    isSynced = isSynced,
)
