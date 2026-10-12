package com.loopwise.app.domain.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class ChecklistItem(val text: String, val checked: Boolean = false)

private val checklistJson = Json { ignoreUnknownKeys = true }

/** Checklist entries store their items as JSON in [JournalEntry.content] — same pattern as [Frequency]. */
fun encodeChecklistItems(items: List<ChecklistItem>): String = checklistJson.encodeToString(items)

fun decodeChecklistItems(content: String?): List<ChecklistItem> =
    content?.let { runCatching { checklistJson.decodeFromString<List<ChecklistItem>>(it) }.getOrNull() }
        ?: emptyList()
