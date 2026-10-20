@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.loopwise.app.ui.today.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.RemoveCircleOutline
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.loopwise.app.domain.model.HabitType
import com.loopwise.app.ui.today.HabitWithProgress

/**
 * Row visual pattern is inspired by the user-supplied reference screenshots: a colored
 * name pill on the left (with a streak badge once there's a streak worth showing), a
 * progress readout, and a right-side action control whose shape depends on habit type —
 * outlined "+" for counters, outlined sync icon for health-synced habits, and once the
 * day's target is met, the control morphs into a solid filled checkmark in the habit's
 * color with a small celebratory pulse (replaces a plain strikethrough completion cue).
 * Long-pressing the name/streak area opens a quick-action menu (spec §2.1).
 */
@Composable
fun HabitRow(
    item: HabitWithProgress,
    onPrimaryAction: () -> Unit,
    onNameClick: () -> Unit,
    onSkip: () -> Unit,
    onUnskip: () -> Unit,
    onDelete: () -> Unit,
    onSyncNow: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val habit = item.habit
    val color = Color(habit.color)
    var showMenu by remember { mutableStateOf(false) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(color.copy(alpha = 0.14f))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Box {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.combinedClickable(onClick = onNameClick, onLongClick = { showMenu = true }),
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(color)
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                ) {
                    Text(
                        habit.name,
                        color = Color.White,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                if (item.streak > 0) {
                    StreakBadge(streak = item.streak, color = color)
                }
            }

            DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                DropdownMenuItem(
                    text = { Text("Edit") },
                    leadingIcon = { Icon(Icons.Filled.Edit, contentDescription = null) },
                    onClick = { showMenu = false; onNameClick() },
                )
                DropdownMenuItem(
                    text = { Text(if (item.isSkipped) "Unskip today" else "Skip today") },
                    leadingIcon = { Icon(Icons.Filled.RemoveCircleOutline, contentDescription = null) },
                    onClick = { showMenu = false; if (item.isSkipped) onUnskip() else onSkip() },
                )
                DropdownMenuItem(
                    text = { Text("Delete") },
                    leadingIcon = { Icon(Icons.Filled.Delete, contentDescription = null) },
                    onClick = { showMenu = false; onDelete() },
                )
            }
        }

        Text(
            progressLabel(item),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
        )

        HabitActionControl(
            item = item,
            color = color,
            onClick = when {
                item.isSkipped -> onUnskip
                item.habit.healthSyncSource != null -> onSyncNow
                else -> onPrimaryAction
            },
        )
    }
}

/** Flame + day count — the one piece of "how disciplined have I been" data worth seeing at a glance. */
@Composable
private fun StreakBadge(streak: Int, color: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.22f))
            .padding(horizontal = 6.dp, vertical = 3.dp),
    ) {
        Icon(Icons.Filled.LocalFireDepartment, contentDescription = "Streak", tint = color, modifier = Modifier.size(13.dp))
        Text(
            streak.toString(),
            color = color,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 2.dp),
        )
    }
}

@Composable
private fun HabitActionControl(item: HabitWithProgress, color: Color, onClick: () -> Unit) {
    val isDone = item.isDone
    val isSkipped = item.isSkipped
    val isSynced = item.habit.healthSyncSource != null

    val scale = remember { Animatable(1f) }
    var previousDone by remember { mutableStateOf(isDone) }
    LaunchedEffect(isDone) {
        if (isDone && !previousDone) {
            scale.animateTo(1.35f, animationSpec = tween(120))
            scale.animateTo(1f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy))
        }
        previousDone = isDone
    }

    Box(
        modifier = Modifier
            .scale(scale.value)
            .size(36.dp)
            .clip(CircleShape)
            .then(
                when {
                    isDone -> Modifier.background(color)
                    isSkipped -> Modifier.background(MaterialTheme.colorScheme.surfaceVariant)
                    else -> Modifier.border(1.5.dp, color, CircleShape)
                },
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        val icon = when {
            isDone -> Icons.Filled.Check
            isSkipped -> Icons.Filled.RemoveCircleOutline
            isSynced -> Icons.Filled.Sync
            item.habit.type == HabitType.BOOLEAN -> null
            else -> Icons.Filled.Add
        }
        val tint = when {
            isDone -> Color.White
            isSkipped -> MaterialTheme.colorScheme.onSurfaceVariant
            else -> color
        }
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(18.dp))
        }
    }
}

private fun progressLabel(item: HabitWithProgress): String {
    if (item.isSkipped) return "Skipped"
    val habit = item.habit
    val unit = habit.unit.orEmpty()
    return when (habit.type) {
        HabitType.BOOLEAN -> if (item.isDone) "Done" else "1 / 1 time"
        HabitType.COUNTER -> {
            val target = habit.targetValue?.let { formatNumber(it) } ?: "-"
            "${formatNumber(item.currentValue)} / $target $unit".trim()
        }
        HabitType.DURATION -> {
            val target = habit.targetValue?.let { formatNumber(it) } ?: "-"
            "${formatNumber(item.currentValue)} / $target min"
        }
        HabitType.AVOIDANCE -> ""
    }
}

private fun formatNumber(value: Double): String =
    if (value == value.toLong().toDouble()) value.toLong().toString() else "%.1f".format(value)
