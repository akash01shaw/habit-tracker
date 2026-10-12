package com.loopwise.app.ui.journal.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.loopwise.app.domain.model.BulletKind
import com.loopwise.app.domain.model.Habit
import com.loopwise.app.domain.model.JournalEntry
import com.loopwise.app.domain.model.JournalEntryType
import com.loopwise.app.domain.model.decodeBulletItems
import com.loopwise.app.domain.model.decodeChecklistItems
import com.loopwise.app.domain.model.symbol
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val dateTimeFormatter = DateTimeFormatter.ofPattern("MMM d, h:mm a")

/** Card style follows the reference screenshots: content up top, a metadata chip row below. */
@Composable
fun JournalEntryCard(
    entry: JournalEntry,
    linkedHabit: Habit?,
    modifier: Modifier = Modifier,
    onToggleChecklistItem: (Int) -> Unit = {},
    onToggleBulletItem: (Int) -> Unit = {},
) {
    Card(modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            when (entry.type) {
                JournalEntryType.MOOD -> {
                    Text(moodLabel(entry.moodValue), style = MaterialTheme.typography.bodyLarge)
                }
                JournalEntryType.CHECKLIST -> {
                    val items = decodeChecklistItems(entry.content)
                    Column {
                        items.forEachIndexed { index, item ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(
                                    checked = item.checked,
                                    onCheckedChange = { onToggleChecklistItem(index) },
                                )
                                Text(
                                    item.text,
                                    style = MaterialTheme.typography.bodyLarge,
                                    textDecoration = if (item.checked) TextDecoration.LineThrough else null,
                                )
                            }
                        }
                    }
                }
                JournalEntryType.BULLET_LOG -> {
                    val items = decodeBulletItems(entry.content)
                    Column {
                        items.forEachIndexed { index, item ->
                            if (item.kind == BulletKind.TASK) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Checkbox(
                                        checked = item.done,
                                        onCheckedChange = { onToggleBulletItem(index) },
                                    )
                                    Text(
                                        item.text,
                                        style = MaterialTheme.typography.bodyLarge,
                                        textDecoration = if (item.done) TextDecoration.LineThrough else null,
                                    )
                                }
                            } else {
                                Text(
                                    "${item.kind.symbol} ${item.text}",
                                    style = MaterialTheme.typography.bodyLarge,
                                    modifier = Modifier.padding(vertical = 8.dp),
                                )
                            }
                        }
                    }
                }
                else -> {
                    Text(
                        entry.content?.takeIf { it.isNotBlank() } ?: "(empty entry)",
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Chip(text = dateTimeFormatter.format(entry.createdAt.atZone(ZoneId.systemDefault())))

                if (linkedHabit != null) {
                    Chip(text = linkedHabit.name, background = Color(linkedHabit.color).copy(alpha = 0.2f))
                } else {
                    Chip(text = "General")
                }

                if (entry.type == JournalEntryType.MOOD && entry.moodValue != null) {
                    Chip(text = "${entry.moodValue}%")
                }
            }
        }
    }
}

@Composable
private fun Chip(text: String, background: Color = Color.Gray.copy(alpha = 0.15f)) {
    Text(
        text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(background)
            .padding(horizontal = 8.dp, vertical = 4.dp),
    )
}

private fun moodLabel(value: Int?): String {
    if (value == null) return "Mood entry"
    val descriptor = when {
        value >= 80 -> "Great"
        value >= 60 -> "Good"
        value >= 40 -> "Okay"
        value >= 20 -> "Low"
        else -> "Rough"
    }
    return "Mood: $value% ($descriptor)"
}
