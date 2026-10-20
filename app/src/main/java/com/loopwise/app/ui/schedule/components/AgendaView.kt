package com.loopwise.app.ui.schedule.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.IntrinsicSize
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
import androidx.compose.ui.draw.clip
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
    val color = Color(block.color)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 5.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(color.copy(alpha = 0.12f))
            .clickable(onClick = onClick)
            .height(IntrinsicSize.Min)
            .padding(start = 0.dp, top = 10.dp, bottom = 10.dp, end = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(4.dp)
                .background(color, RoundedCornerShape(topEnd = 4.dp, bottomEnd = 4.dp)),
        )
        Column {
            Text(
                timeFormatter.format(block.time),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = color,
            )
            Text(block.label, style = MaterialTheme.typography.bodyLarge)
        }
    }
}
