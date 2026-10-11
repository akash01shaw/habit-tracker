package com.loopwise.app.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class FrequencyKind {
    @SerialName("daily") DAILY,
    @SerialName("weekdays") WEEKDAYS,
    @SerialName("per_week") PER_WEEK,
    @SerialName("per_month") PER_MONTH,
}

/**
 * One shape persisted both as Room's TypeConverter JSON and Supabase's jsonb column
 * (spec §4 Habit.frequency). [days] is a set of ISO day-of-week numbers (1=Mon..7=Sun),
 * used only when [kind] is WEEKDAYS. [count] is used for PER_WEEK / PER_MONTH.
 */
@Serializable
data class Frequency(
    val kind: FrequencyKind = FrequencyKind.DAILY,
    val days: List<Int>? = null,
    val count: Int? = null,
) {
    companion object {
        val Daily = Frequency(kind = FrequencyKind.DAILY)
    }
}
