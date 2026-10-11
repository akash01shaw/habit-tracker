package com.loopwise.app.domain.model

enum class HabitType {
    BOOLEAN,
    COUNTER,
    DURATION,
    AVOIDANCE;

    val wireValue: String get() = name.lowercase()

    companion object {
        fun fromWire(value: String): HabitType =
            entries.firstOrNull { it.wireValue == value } ?: BOOLEAN
    }
}
