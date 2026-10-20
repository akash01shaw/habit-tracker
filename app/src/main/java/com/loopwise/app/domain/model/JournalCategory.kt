package com.loopwise.app.domain.model

enum class JournalCategory {
    GENERAL,
    IDEAS,
    PERSONAL,
    PSYCHOLOGICAL;

    val wireValue: String get() = name.lowercase()

    val label: String
        get() = when (this) {
            GENERAL -> "General"
            IDEAS -> "Ideas"
            PERSONAL -> "Personal"
            PSYCHOLOGICAL -> "Psychological"
        }

    companion object {
        fun fromWire(value: String?): JournalCategory = entries.firstOrNull { it.wireValue == value } ?: GENERAL
    }
}
