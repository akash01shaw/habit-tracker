package com.loopwise.app.data.remote.dto

import com.loopwise.app.domain.model.Frequency
import com.loopwise.app.domain.model.Habit
import com.loopwise.app.domain.model.HabitType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.Instant
import java.time.LocalTime

@Serializable
data class HabitDto(
    val id: String,
    @SerialName("owner_id") val ownerId: String,
    val name: String,
    val color: String,
    val icon: String? = null,
    val type: String,
    val frequency: Frequency = Frequency.Daily,
    @SerialName("target_value") val targetValue: Double? = null,
    val unit: String? = null,
    val reminders: List<String> = emptyList(),
    @SerialName("health_sync_source") val healthSyncSource: String? = null,
    @SerialName("is_shared") val isShared: Boolean = false,
    @SerialName("created_at") val createdAt: String? = null,
)

fun HabitDto.toDomain() = Habit(
    id = id,
    ownerId = ownerId,
    name = name,
    color = color.removePrefix("#").toLong(16) or 0xFF000000L,
    icon = icon,
    type = HabitType.fromWire(type),
    frequency = frequency,
    targetValue = targetValue,
    unit = unit,
    reminders = reminders.map(LocalTime::parse),
    healthSyncSource = healthSyncSource,
    isShared = isShared,
    createdAt = createdAt?.let(Instant::parse) ?: Instant.now(),
)

fun Habit.toDto() = HabitDto(
    id = id,
    ownerId = ownerId,
    name = name,
    color = "#%06X".format(color and 0xFFFFFFL),
    icon = icon,
    type = type.wireValue,
    frequency = frequency,
    targetValue = targetValue,
    unit = unit,
    reminders = reminders.map { it.toString() },
    healthSyncSource = healthSyncSource,
    isShared = isShared,
    createdAt = createdAt.toString(),
)
