package com.loopwise.app.ui.schedule.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.loopwise.app.ui.schedule.ScheduleBlock
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

/**
 * One row per hour (fixed 6 AM-10 PM window, not dynamically sized to the data) with
 * 7 day columns — simpler to get right than a pixel-positioned/time-proportional
 * canvas grid, at the cost of not showing anything scheduled outside that window.
 * Habit reminders and task due times are both single points in time (neither carries
 * a duration in the data model), so every block occupies exactly one hour cell.
 */
@Composable
fun WeekGrid(weekDates: List<LocalDate>, blocks: List<ScheduleBlock>, onBlockClick: (ScheduleBlock) -> Unit) {
    val hours = (6..22).toList()
    val blocksByDateHour = blocks.groupBy { it.date to it.time.hour }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Box(modifier = Modifier.width(40.dp))
            weekDates.forEach { date ->
                Box(modifier = Modifier.weight(1f).padding(vertical = 4.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()).take(2),
                            style = MaterialTheme.typography.labelSmall,
                        )
                        Text(date.dayOfMonth.toString(), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
            }
        }
        HorizontalDivider()
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(hours) { hour ->
                Row(modifier = Modifier.fillMaxWidth().height(52.dp)) {
                    Box(modifier = Modifier.width(40.dp), contentAlignment = Alignment.TopCenter) {
                        Text(
                            formatHour(hour),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    weekDates.forEach { date ->
                        val cellBlocks = blocksByDateHour[date to hour].orEmpty()
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxSize()
                                .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
                                .padding(1.dp),
                        ) {
                            Column {
                                cellBlocks.forEach { block -> GridBlockChip(block, onClick = { onBlockClick(block) }) }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GridBlockChip(block: ScheduleBlock, onClick: () -> Unit) {
    Text(
        block.label,
        style = MaterialTheme.typography.labelSmall,
        fontSize = 9.sp,
        color = Color.White,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier
            .fillMaxWidth()
            .padding(1.dp)
            .background(Color(block.color), RoundedCornerShape(3.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 3.dp, vertical = 2.dp),
    )
}

private fun formatHour(hour: Int): String = when {
    hour == 0 -> "12 AM"
    hour < 12 -> "$hour AM"
    hour == 12 -> "12 PM"
    else -> "${hour - 12} PM"
}
