package com.loopwise.app.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.loopwise.app.domain.model.JournalEntry
import com.loopwise.app.domain.model.JournalEntryType
import com.loopwise.app.domain.model.JournalVisibility
import java.time.Instant

@Entity(tableName = "journal_entries")
data class JournalEntryEntity(
    @PrimaryKey val id: String,
    val ownerId: String,
    val linkedHabitId: String?,
    val type: JournalEntryType,
    val content: String?,
    val moodValue: Int?,
    val visibility: JournalVisibility,
    val createdAt: Instant,
    val isSynced: Boolean = true,
    val isDeleted: Boolean = false,
)

fun JournalEntryEntity.toDomain() = JournalEntry(
    id = id,
    ownerId = ownerId,
    linkedHabitId = linkedHabitId,
    type = type,
    content = content,
    moodValue = moodValue,
    visibility = visibility,
    createdAt = createdAt,
)

fun JournalEntry.toEntity(isSynced: Boolean = true) = JournalEntryEntity(
    id = id,
    ownerId = ownerId,
    linkedHabitId = linkedHabitId,
    type = type,
    content = content,
    moodValue = moodValue,
    visibility = visibility,
    createdAt = createdAt,
    isSynced = isSynced,
)
