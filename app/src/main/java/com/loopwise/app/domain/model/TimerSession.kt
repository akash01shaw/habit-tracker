package com.loopwise.app.domain.model

import java.time.Instant

enum class TimerMode {
    STOPWATCH,
    COUNTDOWN;

    val wireValue: String get() = name.lowercase()

    companion object {
        fun fromWire(value: String): TimerMode = entries.firstOrNull { it.wireValue == value } ?: STOPWATCH
    }
}

enum class TimerEventType {
    START,
    PAUSE,
    RESUME,
    STOP;

    val wireValue: String get() = name.lowercase()

    companion object {
        fun fromWire(value: String): TimerEventType =
            entries.firstOrNull { it.wireValue == value } ?: START
    }
}

data class TimerEvent(val type: TimerEventType, val at: Instant)

data class TimerSession(
    val id: String,
    val ownerId: String,
    val habitId: String? = null,
    val mode: TimerMode,
    val targetSeconds: Int? = null,
    val events: List<TimerEvent> = emptyList(),
    val createdAt: Instant = Instant.now(),
)
