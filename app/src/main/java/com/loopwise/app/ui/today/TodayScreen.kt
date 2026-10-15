@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.loopwise.app.ui.today

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircleOutline
import androidx.compose.material.icons.filled.ChecklistRtl
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.loopwise.app.domain.model.HabitType
import com.loopwise.app.ui.today.components.AvoidanceHabitRow
import com.loopwise.app.ui.today.components.DateStrip
import com.loopwise.app.ui.today.components.HabitRow
import com.loopwise.app.ui.today.components.SectionHeader
import com.loopwise.app.ui.today.components.TaskRow

@Composable
fun TodayScreen(
    viewModel: TodayViewModel,
    onAddHabit: () -> Unit,
    onAddTask: () -> Unit,
    onEditHabit: (String) -> Unit,
    onEditTask: (String) -> Unit,
) {
    val state by viewModel.uiState.collectAsState()
    var tasksExpanded by remember { mutableStateOf(true) }
    var habitsExpanded by remember { mutableStateOf(true) }
    var avoidanceExpanded by remember { mutableStateOf(true) }
    var fabMenuExpanded by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Today") },
                actions = {
                    IconButton(onClick = viewModel::signOut) {
                        Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "Sign out")
                    }
                },
            )
        },
        floatingActionButton = {
            Box {
                FloatingActionButton(onClick = { fabMenuExpanded = true }) {
                    Icon(Icons.Filled.Add, contentDescription = "Add habit or task")
                }
                DropdownMenu(expanded = fabMenuExpanded, onDismissRequest = { fabMenuExpanded = false }) {
                    DropdownMenuItem(
                        text = { Text("New Task") },
                        onClick = { fabMenuExpanded = false; onAddTask() },
                    )
                    DropdownMenuItem(
                        text = { Text("New Habit") },
                        onClick = { fabMenuExpanded = false; onAddHabit() },
                    )
                }
            }
        },
    ) { padding ->
        if (!state.isSignedIn) {
            Column(modifier = Modifier.fillMaxSize().padding(padding)) {
                Text("Signed out", modifier = Modifier.padding(16.dp))
            }
            return@Scaffold
        }

        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
            item {
                DateStrip(selectedDate = state.selectedDate, onDateSelected = viewModel::selectDate)
            }

            item {
                SectionHeader(
                    label = "To-Do List",
                    count = state.tasks.size,
                    icon = Icons.Filled.ChecklistRtl,
                    expanded = tasksExpanded,
                    onToggle = { tasksExpanded = !tasksExpanded },
                )
            }
            if (tasksExpanded) {
                if (state.tasks.isEmpty()) {
                    item { EmptyRow("No tasks for this day") }
                } else {
                    items(state.tasks, key = { it.id }) { task ->
                        TaskRow(
                            task = task,
                            onToggle = { viewModel.toggleTask(task) },
                            onClick = { onEditTask(task.id) },
                        )
                    }
                }
            }

            item {
                SectionHeader(
                    label = "Daily Habits",
                    count = state.habits.size,
                    icon = Icons.Filled.CheckCircleOutline,
                    expanded = habitsExpanded,
                    onToggle = { habitsExpanded = !habitsExpanded },
                )
            }
            if (habitsExpanded) {
                if (state.habits.isEmpty()) {
                    item { EmptyRow("No habits yet — tap + to create one") }
                } else {
                    items(state.habits, key = { it.habit.id }) { item ->
                        HabitRow(
                            item = item,
                            onPrimaryAction = { onHabitPrimaryAction(viewModel, item) },
                            onNameClick = { onEditHabit(item.habit.id) },
                        )
                    }
                }
            }

            item {
                SectionHeader(
                    label = "Habits I'm Quitting",
                    count = state.avoidanceHabits.size,
                    icon = Icons.Filled.Block,
                    expanded = avoidanceExpanded,
                    onToggle = { avoidanceExpanded = !avoidanceExpanded },
                )
            }
            if (avoidanceExpanded) {
                if (state.avoidanceHabits.isEmpty()) {
                    item { EmptyRow("No quit habits yet") }
                } else {
                    items(state.avoidanceHabits, key = { it.habit.id }) { item ->
                        AvoidanceHabitRow(
                            item = item,
                            onReset = { viewModel.resetAvoidanceStreak(item) },
                            onNameClick = { onEditHabit(item.habit.id) },
                        )
                    }
                }
            }
        }
    }
}

private fun onHabitPrimaryAction(viewModel: TodayViewModel, item: HabitWithProgress) {
    when (item.habit.type) {
        HabitType.BOOLEAN -> viewModel.toggleBooleanHabit(item)
        HabitType.COUNTER -> viewModel.incrementCounterHabit(item, TodayViewModel.defaultIncrement(item.habit))
        HabitType.DURATION -> viewModel.logDurationMinutes(item, TodayViewModel.defaultIncrement(item.habit))
        HabitType.AVOIDANCE -> Unit
    }
}

@Composable
private fun EmptyRow(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
    )
}
