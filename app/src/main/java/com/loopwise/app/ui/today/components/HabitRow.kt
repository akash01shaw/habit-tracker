package com.loopwise.app.ui.today.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.loopwise.app.domain.model.HabitType
import com.loopwise.app.ui.today.HabitWithProgress

/**
 * Row visual pattern is inspired by the user-supplied reference screenshots: a colored
 * name pill on the left, a progress readout, and a right-side action control whose shape
 * depends on habit type — outlined "+" for counters, outlined sync icon for health-synced
 * habits, and once the day's target is met, the control morphs into a solid filled
 * checkmark in the habit's color (replaces a plain strikethrough completion cue).
 */
@Composable
fun HabitRow(
    item: HabitWithProgress,
    onPrimaryAction: () -> Unit,
    onNameClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val habit = item.habit
    val color = Color(habit.color)

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
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(color)
                .clickable(onClick = onNameClick)
                .padding(horizontal = 10.dp, vertical = 6.dp),
        ) {
            Text(
                habit.name,
                color = Color.White,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
            )
        }

        Text(
            progressLabel(item),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
        )

        HabitActionControl(item = item, color = color, onClick = onPrimaryAction)
    }
}

@Composable
private fun HabitActionControl(item: HabitWithProgress, color: Color, onClick: () -> Unit) {
    val isDone = item.isDone
    val isSynced = item.habit.healthSyncSource != null

    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .then(
                if (isDone) Modifier.background(color)
                else Modifier.border(1.5.dp, color, CircleShape),
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        val icon = when {
            isDone -> Icons.Filled.Check
            isSynced -> Icons.Filled.Sync
            item.habit.type == HabitType.BOOLEAN -> null
            else -> Icons.Filled.Add
        }
        val tint = if (isDone) Color.White else color
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(18.dp))
        }
    }
}

private fun progressLabel(item: HabitWithProgress): String {
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
