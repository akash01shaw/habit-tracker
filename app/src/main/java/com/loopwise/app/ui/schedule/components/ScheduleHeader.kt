package com.loopwise.app.ui.schedule.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.loopwise.app.ui.schedule.ScheduleViewMode
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val monthFormatter = DateTimeFormatter.ofPattern("MMM yyyy")

@Composable
fun ScheduleHeader(
    weekStart: LocalDate,
    viewMode: ScheduleViewMode,
    onPreviousWeek: () -> Unit,
    onNextWeek: () -> Unit,
    onToday: () -> Unit,
    onViewModeChange: (ScheduleViewMode) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            monthFormatter.format(weekStart.plusDays(3)), // mid-week date keeps the label stable across month boundaries
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(start = 8.dp),
        )

        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onPreviousWeek) {
                Icon(Icons.Filled.ChevronLeft, contentDescription = "Previous week")
            }
            TextButton(onClick = onToday) { Text("Today") }
            IconButton(onClick = onNextWeek) {
                Icon(Icons.Filled.ChevronRight, contentDescription = "Next week")
            }
            IconButton(onClick = {
                onViewModeChange(if (viewMode == ScheduleViewMode.GRID) ScheduleViewMode.AGENDA else ScheduleViewMode.GRID)
            }) {
                Icon(
                    imageVector = if (viewMode == ScheduleViewMode.GRID) Icons.Filled.ViewAgenda else Icons.Filled.GridView,
                    contentDescription = "Toggle grid/agenda view",
                )
            }
        }
    }
}
