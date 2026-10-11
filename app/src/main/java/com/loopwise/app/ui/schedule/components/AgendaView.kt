package com.loopwise.app.ui.schedule.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.loopwise.app.ui.schedule.ScheduleBlock
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val dayHeaderFormatter = DateTimeFormatter.ofPattern("EEEE, MMM d")
private val timeFormatter = DateTimeFormatter.ofPattern("h:mm a")

@Composable
fun AgendaView(weekDates: List<LocalDate>, blocks: List<ScheduleBlock>, onBlockClick: (ScheduleBlock) -> Unit) {
    val blocksByDate = blocks.groupBy { it.date }

    LazyColumn(modifier = Modifier.fillMaxWidth()) {
        weekDates.forEach { date ->
            val dayBlocks = blocksByDate[date].orEmpty()
            item {
                Text(
                    dayHeaderFormatter.format(date),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 4.dp),
                )
                HorizontalDivider()
            }
            if (dayBlocks.isEmpty()) {
                item {
                    Text(
                        "Nothing scheduled",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    )
                }
            } else {
                items(dayBlocks, key = { it.key }) { block -> AgendaRow(block, onClick = { onBlockClick(block) }) }
            }
        }
    }
}

@Composable
private fun AgendaRow(block: ScheduleBlock, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .padding(top = 6.dp)
                .size(10.dp)
                .background(Color(block.color), CircleShape),
        )
        Column {
            Text(
                timeFormatter.format(block.time),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(block.label, style = MaterialTheme.typography.bodyLarge)
        }
    }
}
