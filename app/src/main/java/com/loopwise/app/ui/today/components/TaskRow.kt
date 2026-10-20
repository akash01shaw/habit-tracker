@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.loopwise.app.ui.today.components

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.automirrored.filled.StickyNote2
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.loopwise.app.domain.model.Task
import com.loopwise.app.ui.theme.LocalDayPeriod
import java.time.format.DateTimeFormatter

private val timeFormatter = DateTimeFormatter.ofPattern("h:mm a")

@Composable
fun TaskRow(task: Task, onToggle: () -> Unit, onClick: () -> Unit, onDelete: () -> Unit, modifier: Modifier = Modifier) {
    var showMenu by remember { mutableStateOf(false) }
    val taskAccentColor = LocalDayPeriod.current.accentTodo

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(taskAccentColor.copy(alpha = 0.12f))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Box(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.combinedClickable(onClick = onClick, onLongClick = { showMenu = true }),
            ) {
                if (task.priorityFlag) {
                    Icon(
                        Icons.Filled.Flag,
                        contentDescription = "Priority",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(16.dp),
                    )
                }
                Text(
                    task.title,
                    style = MaterialTheme.typography.bodyLarge,
                    textDecoration = if (task.isCompleted) TextDecoration.LineThrough else null,
                    color = if (task.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(start = if (task.priorityFlag) 6.dp else 0.dp).weight(1f, fill = false),
                )
                if (task.note != null) {
                    Icon(
                        Icons.AutoMirrored.Filled.StickyNote2,
                        contentDescription = "Has note",
                        modifier = Modifier.padding(start = 6.dp).size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (task.reminderTime != null) {
                    Icon(
                        Icons.Filled.Notifications,
                        contentDescription = "Reminder set",
                        modifier = Modifier.padding(start = 6.dp).size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                DropdownMenuItem(
                    text = { Text("Edit") },
                    leadingIcon = { Icon(Icons.Filled.Edit, contentDescription = null) },
                    onClick = { showMenu = false; onClick() },
                )
                DropdownMenuItem(
                    text = { Text("Delete") },
                    leadingIcon = { Icon(Icons.Filled.Delete, contentDescription = null) },
                    onClick = { showMenu = false; onDelete() },
                )
            }
        }

        task.dueTime?.let {
            Text(
                it.format(timeFormatter),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(end = 8.dp),
            )
        }

        IconButton(onClick = onToggle) {
            Icon(
                imageVector = if (task.isCompleted) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked,
                contentDescription = if (task.isCompleted) "Completed" else "Mark complete",
                tint = if (task.isCompleted) taskAccentColor else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
