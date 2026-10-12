package com.loopwise.app.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** Rapid-log bullet kinds, per the Bullet Journal method: tasks can be done, events/notes can't. */
@Serializable
enum class BulletKind {
    @SerialName("task") TASK,
    @SerialName("event") EVENT,
    @SerialName("note") NOTE,
}

val BulletKind.symbol: String
    get() = when (this) {
        BulletKind.TASK -> "•" // •
        BulletKind.EVENT -> "○" // ○
        BulletKind.NOTE -> "–" // –
    }

@Serializable
data class BulletItem(val text: String, val kind: BulletKind = BulletKind.TASK, val done: Boolean = false)

private val bulletJson = Json { ignoreUnknownKeys = true }

/** Bullet-log entries store their items as JSON in [JournalEntry.content] — same pattern as [ChecklistItem]. */
fun encodeBulletItems(items: List<BulletItem>): String = bulletJson.encodeToString(items)

fun decodeBulletItems(content: String?): List<BulletItem> =
    content?.let { runCatching { bulletJson.decodeFromString<List<BulletItem>>(it) }.getOrNull() }
        ?: emptyList()
