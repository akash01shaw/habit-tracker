package com.loopwise.app.domain.model

/**
 * The two Health Connect metrics the spec names explicitly against a habit (§2.2's
 * "optional link to Health data (steps, active minutes) for auto-fill"). Distance,
 * stand hours, active calories, and workouts (§2.8's broader list) are a later
 * extension — not wired up yet. Stored as [Habit.healthSyncSource]'s raw string.
 */
enum class HealthSyncSource {
    STEPS,
    ACTIVE_MINUTES;

    val wireValue: String get() = name.lowercase()

    val label: String
        get() = when (this) {
            STEPS -> "Steps"
            ACTIVE_MINUTES -> "Active minutes"
        }

    companion object {
        fun fromWire(value: String?): HealthSyncSource? = entries.firstOrNull { it.wireValue == value }
    }
}
