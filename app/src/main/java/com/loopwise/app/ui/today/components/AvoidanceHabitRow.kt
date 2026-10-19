@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.loopwise.app.ui.today.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.loopwise.app.ui.today.AvoidanceHabitState
import com.loopwise.app.ui.today.formatStreak
import kotlinx.coroutines.delay
import java.time.Duration
import java.time.Instant

/**
 * Dedicated row style for avoidance/quit habits (spec §2.3): dark name pill (QuitPalette
 * colors, set at creation), a live-counting "35d, 1h, 15m, 31s" clock, and a reset button
 * that logs the relapse and restarts the clock from now — mirrors the reference
 * screenshots' "Quit Habits" section. Long-pressing the name opens Edit/Delete — no
 * "Skip" here, since the continuous streak clock already is the avoidance equivalent.
 */
@Composable
fun AvoidanceHabitRow(
    item: AvoidanceHabitState,
    onReset: () -> Unit,
    onNameClick: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val color = Color(item.habit.color)
    val elapsed by rememberElapsedTime(item.since)
    var showMenu by remember { mutableStateOf(false) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(color.copy(alpha = 0.16f))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Box {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(color)
                    .combinedClickable(onClick = onNameClick, onLongClick = { showMenu = true })
                    .padding(horizontal = 10.dp, vertical = 6.dp),
            ) {
                Text(
                    item.habit.name,
                    color = Color.White,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                )
            }

            DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                DropdownMenuItem(
                    text = { Text("Edit") },
                    leadingIcon = { Icon(Icons.Filled.Edit, contentDescription = null) },
                    onClick = { showMenu = false; onNameClick() },
                )
                DropdownMenuItem(
                    text = { Text("Delete") },
                    leadingIcon = { Icon(Icons.Filled.Delete, contentDescription = null) },
                    onClick = { showMenu = false; onDelete() },
                )
            }
        }

        Text(
            formatStreak(elapsed),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
        )

        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(color)
                .clickable(onClick = onReset),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Filled.Refresh,
                contentDescription = "I slipped — reset streak",
                tint = Color.White,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@Composable
private fun rememberElapsedTime(since: Instant): State<Duration> =
    produceState(initialValue = Duration.between(since, Instant.now()), since) {
        while (true) {
            value = Duration.between(since, Instant.now())
            delay(1_000)
        }
    }
