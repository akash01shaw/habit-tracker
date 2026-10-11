package com.loopwise.app.data.local

import androidx.room.TypeConverter
import com.loopwise.app.domain.model.Frequency
import com.loopwise.app.domain.model.HabitType
import com.loopwise.app.domain.model.JournalEntryType
import com.loopwise.app.domain.model.JournalVisibility
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

private val json = Json { ignoreUnknownKeys = true }

class Converters {
    @TypeConverter
    fun habitTypeToString(value: HabitType): String = value.wireValue

    @TypeConverter
    fun stringToHabitType(value: String): HabitType = HabitType.fromWire(value)

    @TypeConverter
    fun frequencyToJson(value: Frequency): String = json.encodeToString(value)

    @TypeConverter
    fun jsonToFrequency(value: String): Frequency =
        runCatching { json.decodeFromString<Frequency>(value) }.getOrDefault(Frequency.Daily)

    @TypeConverter
    fun remindersToJson(value: List<LocalTime>): String =
        json.encodeToString(value.map { it.toString() })

    @TypeConverter
    fun jsonToReminders(value: String): List<LocalTime> =
        runCatching { json.decodeFromString<List<String>>(value).map(LocalTime::parse) }
            .getOrDefault(emptyList())

    @TypeConverter
    fun instantToEpochMillis(value: Instant?): Long? = value?.toEpochMilli()

    @TypeConverter
    fun epochMillisToInstant(value: Long?): Instant? = value?.let(Instant::ofEpochMilli)

    @TypeConverter
    fun localDateToString(value: LocalDate?): String? = value?.toString()

    @TypeConverter
    fun stringToLocalDate(value: String?): LocalDate? = value?.let(LocalDate::parse)

    @TypeConverter
    fun localTimeToString(value: LocalTime?): String? = value?.toString()

    @TypeConverter
    fun stringToLocalTime(value: String?): LocalTime? = value?.let(LocalTime::parse)

    @TypeConverter
    fun journalEntryTypeToString(value: JournalEntryType): String = value.wireValue

    @TypeConverter
    fun stringToJournalEntryType(value: String): JournalEntryType = JournalEntryType.fromWire(value)

    @TypeConverter
    fun journalVisibilityToString(value: JournalVisibility): String = value.wireValue

    @TypeConverter
    fun stringToJournalVisibility(value: String): JournalVisibility = JournalVisibility.fromWire(value)
}
