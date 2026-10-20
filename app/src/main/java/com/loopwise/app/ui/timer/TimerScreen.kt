@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.loopwise.app.ui.timer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.loopwise.app.domain.model.Habit
import com.loopwise.app.domain.model.TimerEvent
import com.loopwise.app.domain.model.TimerEventType
import com.loopwise.app.domain.model.TimerMode
import com.loopwise.app.ui.common.ComingSoonBody
import com.loopwise.app.ui.common.TopBannerHeader
import com.loopwise.app.ui.timer.components.TimerRing
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val eventTimeFormatter = DateTimeFormatter.ofPattern("MMM d yyyy h:mm a")

@Composable
fun TimerScreen(viewModel: TimerViewModel) {
    val state by viewModel.state.collectAsState()

    Scaffold(topBar = { TopBannerHeader(screenLabel = "Timer") }) { padding ->
        if (!state.isSignedIn) {
            ComingSoonBody(padding, "Sign in to use the focus timer.")
            return@Scaffold
        }

        val isIdle = state.runState == TimerRunState.IDLE

        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            TabRow(selectedTabIndex = if (state.mode == TimerMode.STOPWATCH) 0 else 1) {
                Tab(
                    selected = state.mode == TimerMode.STOPWATCH,
                    onClick = { viewModel.selectMode(TimerMode.STOPWATCH) },
                    text = { Text("Stopwatch") },
                    enabled = isIdle,
                )
                Tab(
                    selected = state.mode == TimerMode.COUNTDOWN,
                    onClick = { viewModel.selectMode(TimerMode.COUNTDOWN) },
                    text = { Text("Countdown") },
                    enabled = isIdle,
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Focus", style = MaterialTheme.typography.titleMedium)
                FocusHabitDropdown(
                    habits = state.habits,
                    selectedHabitId = state.linkedHabitId,
                    enabled = isIdle,
                    onSelect = viewModel::selectHabit,
                )
            }

            if (state.mode == TimerMode.COUNTDOWN && isIdle) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Duration:", style = MaterialTheme.typography.bodyMedium)
                    OutlinedButton(onClick = { viewModel.setTargetMinutes(state.targetMinutes - 5) }) { Text("-5") }
                    Text("${state.targetMinutes} min", style = MaterialTheme.typography.titleMedium)
                    OutlinedButton(onClick = { viewModel.setTargetMinutes(state.targetMinutes + 5) }) { Text("+5") }
                }
            }

            Box(modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp), contentAlignment = Alignment.Center) {
                TimerRing(
                    progress = if (state.mode == TimerMode.COUNTDOWN) state.progressFraction else 0f,
                    timeText = formatSeconds(state.displaySeconds),
                    subtitleText = if (state.mode == TimerMode.COUNTDOWN) "${state.targetMinutes}m target" else "elapsed",
                    color = MaterialTheme.colorScheme.primary,
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.Center,
            ) {
                when (state.runState) {
                    TimerRunState.IDLE -> {
                        Button(onClick = viewModel::start) { Text("Start") }
                    }
                    TimerRunState.RUNNING -> {
                        OutlinedButton(onClick = viewModel::stop, modifier = Modifier.padding(end = 12.dp)) { Text("Stop") }
                        Button(onClick = viewModel::pause) { Text("Pause") }
                    }
                    TimerRunState.PAUSED -> {
                        OutlinedButton(onClick = viewModel::stop, modifier = Modifier.padding(end = 12.dp)) { Text("Stop") }
                        Button(onClick = viewModel::resume) { Text("Resume") }
                    }
                }
            }

            if (state.events.isNotEmpty()) {
                HorizontalDivider(modifier = Modifier.padding(top = 24.dp, bottom = 8.dp))
                LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
                    items(state.events.reversed()) { event -> SessionEventRow(event) }
                }
            }
        }
    }
}

@Composable
private fun FocusHabitDropdown(
    habits: List<Habit>,
    selectedHabitId: String?,
    enabled: Boolean,
    onSelect: (String?) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val label = habits.firstOrNull { it.id == selectedHabitId }?.name ?: "None"

    Box {
        OutlinedButton(onClick = { expanded = true }, enabled = enabled) { Text(label) }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(text = { Text("None") }, onClick = { onSelect(null); expanded = false })
            habits.forEach { habit ->
                DropdownMenuItem(text = { Text(habit.name) }, onClick = { onSelect(habit.id); expanded = false })
            }
        }
    }
}

@Composable
private fun SessionEventRow(event: TimerEvent) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            eventTimeFormatter.format(event.at.atZone(ZoneId.systemDefault())),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(eventLabel(event.type), style = MaterialTheme.typography.labelLarge)
    }
}

private fun eventLabel(type: TimerEventType) = when (type) {
    TimerEventType.START -> "Start"
    TimerEventType.PAUSE -> "Pause"
    TimerEventType.RESUME -> "Resume"
    TimerEventType.STOP -> "Stop"
}

private fun formatSeconds(totalSeconds: Long): String {
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%02d:%02d".format(minutes, seconds)
}
