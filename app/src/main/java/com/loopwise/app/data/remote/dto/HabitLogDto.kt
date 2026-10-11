package com.loopwise.app.data.remote.dto

import com.loopwise.app.domain.model.HabitLog
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.Instant
import java.time.LocalDate

@Serializable
data class HabitLogDto(
    val id: String,
    @SerialName("habit_id") val habitId: String,
    @SerialName("user_id") val userId: String,
    val date: String,
    val value: Double = 0.0,
    @SerialName("is_done") val isDone: Boolean = false,
    val note: String? = null,
    @SerialName("logged_at") val loggedAt: String? = null,
)

fun HabitLogDto.toDomain() = HabitLog(
    id = id,
    habitId = habitId,
    userId = userId,
    date = LocalDate.parse(date),
    value = value,
    isDone = isDone,
    note = note,
    loggedAt = loggedAt?.let(Instant::parse) ?: Instant.now(),
)

fun HabitLog.toDto() = HabitLogDto(
    id = id,
    habitId = habitId,
    userId = userId,
    date = date.toString(),
    value = value,
    isDone = isDone,
    note = note,
    loggedAt = loggedAt.toString(),
)
