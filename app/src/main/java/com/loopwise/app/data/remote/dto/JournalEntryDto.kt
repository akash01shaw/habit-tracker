package com.loopwise.app.data.remote.dto

import com.loopwise.app.domain.model.JournalCategory
import com.loopwise.app.domain.model.JournalEntry
import com.loopwise.app.domain.model.JournalEntryType
import com.loopwise.app.domain.model.JournalVisibility
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.Instant

@Serializable
data class JournalEntryDto(
    val id: String,
    @SerialName("owner_id") val ownerId: String,
    @SerialName("linked_habit_id") val linkedHabitId: String? = null,
    val type: String,
    val category: String = "general",
    val content: String? = null,
    @SerialName("mood_value") val moodValue: Int? = null,
    val visibility: String = "private",
    @SerialName("created_at") val createdAt: String? = null,
)

fun JournalEntryDto.toDomain() = JournalEntry(
    id = id,
    ownerId = ownerId,
    linkedHabitId = linkedHabitId,
    type = JournalEntryType.fromWire(type),
    category = JournalCategory.fromWire(category),
    content = content,
    moodValue = moodValue,
    visibility = JournalVisibility.fromWire(visibility),
    createdAt = createdAt?.let(Instant::parse) ?: Instant.now(),
)

fun JournalEntry.toDto() = JournalEntryDto(
    id = id,
    ownerId = ownerId,
    linkedHabitId = linkedHabitId,
    type = type.wireValue,
    category = category.wireValue,
    content = content,
    moodValue = moodValue,
    visibility = visibility.wireValue,
    createdAt = createdAt.toString(),
)
