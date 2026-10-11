package com.loopwise.app.domain.model

import java.time.Instant

enum class JournalEntryType {
    TEXT,
    MOOD,
    CHECKLIST; // Not yet creatable from the UI (spec Phase 3) — modeled now so the schema doesn't need a later migration.

    val wireValue: String get() = name.lowercase()

    companion object {
        fun fromWire(value: String): JournalEntryType =
            entries.firstOrNull { it.wireValue == value } ?: TEXT
    }
}

enum class JournalVisibility {
    PRIVATE,
    SHARED; // Not yet used — sharing is Phase 2. Every entry is PRIVATE for now.

    val wireValue: String get() = name.lowercase()

    companion object {
        fun fromWire(value: String): JournalVisibility =
            entries.firstOrNull { it.wireValue == value } ?: PRIVATE
    }
}

data class JournalEntry(
    val id: String,
    val ownerId: String,
    val linkedHabitId: String? = null,
    val type: JournalEntryType,
    val content: String? = null,
    val moodValue: Int? = null,
    val visibility: JournalVisibility = JournalVisibility.PRIVATE,
    val createdAt: Instant = Instant.now(),
)
