package com.loopwise.app.data.remote.dto

import com.loopwise.app.domain.model.TimerEvent
import com.loopwise.app.domain.model.TimerEventType
import com.loopwise.app.domain.model.TimerMode
import com.loopwise.app.domain.model.TimerSession
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.Instant

@Serializable
data class TimerEventDto(val type: String, val at: String)

@Serializable
data class TimerSessionDto(
    val id: String,
    @SerialName("owner_id") val ownerId: String,
    @SerialName("habit_id") val habitId: String? = null,
    val mode: String,
    @SerialName("target_seconds") val targetSeconds: Int? = null,
    val events: List<TimerEventDto> = emptyList(),
    @SerialName("created_at") val createdAt: String? = null,
)

fun TimerSessionDto.toDomain() = TimerSession(
    id = id,
    ownerId = ownerId,
    habitId = habitId,
    mode = TimerMode.fromWire(mode),
    targetSeconds = targetSeconds,
    events = events.map { TimerEvent(TimerEventType.fromWire(it.type), Instant.parse(it.at)) },
    createdAt = createdAt?.let(Instant::parse) ?: Instant.now(),
)

fun TimerSession.toDto() = TimerSessionDto(
    id = id,
    ownerId = ownerId,
    habitId = habitId,
    mode = mode.wireValue,
    targetSeconds = targetSeconds,
    events = events.map { TimerEventDto(it.type.wireValue, it.at.toString()) },
    createdAt = createdAt.toString(),
)
