package com.loopwise.app.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.loopwise.app.domain.model.TimerEvent
import com.loopwise.app.domain.model.TimerMode
import com.loopwise.app.domain.model.TimerSession
import java.time.Instant

@Entity(tableName = "timer_sessions")
data class TimerSessionEntity(
    @PrimaryKey val id: String,
    val ownerId: String,
    val habitId: String?,
    val mode: TimerMode,
    val targetSeconds: Int?,
    val events: List<TimerEvent>,
    val createdAt: Instant,
    val isSynced: Boolean = true,
)

fun TimerSessionEntity.toDomain() = TimerSession(
    id = id,
    ownerId = ownerId,
    habitId = habitId,
    mode = mode,
    targetSeconds = targetSeconds,
    events = events,
    createdAt = createdAt,
)

fun TimerSession.toEntity(isSynced: Boolean = true) = TimerSessionEntity(
    id = id,
    ownerId = ownerId,
    habitId = habitId,
    mode = mode,
    targetSeconds = targetSeconds,
    events = events,
    createdAt = createdAt,
    isSynced = isSynced,
)
