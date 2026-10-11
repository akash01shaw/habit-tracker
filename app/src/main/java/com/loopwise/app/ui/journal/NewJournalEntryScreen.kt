@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.loopwise.app.ui.journal

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.loopwise.app.domain.model.Habit
import com.loopwise.app.domain.model.JournalEntryType

@Composable
fun NewJournalEntryScreen(viewModel: JournalViewModel, onDone: () -> Unit) {
    val state by viewModel.formState.collectAsState()
    val habits = viewModel.uiState.collectAsState().value.habits

    LaunchedEffect(state.saved) {
        if (state.saved) {
            viewModel.resetForm()
            onDone()
        }
    }

    Scaffold(topBar = { TopAppBar(title = { Text("New Entry") }) }) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = state.type == JournalEntryType.TEXT,
                    onClick = { viewModel.onTypeChange(JournalEntryType.TEXT) },
                    label = { Text("Text") },
                )
                FilterChip(
                    selected = state.type == JournalEntryType.MOOD,
                    onClick = { viewModel.onTypeChange(JournalEntryType.MOOD) },
                    label = { Text("Mood") },
                )
            }

            when (state.type) {
                JournalEntryType.TEXT -> {
                    OutlinedTextField(
                        value = state.content,
                        onValueChange = viewModel::onContentChange,
                        label = { Text("What's on your mind?") },
                        modifier = Modifier.fillMaxWidth().height(160.dp).padding(top = 16.dp),
                    )
                }
                JournalEntryType.MOOD -> {
                    Text(
                        "Mood: ${state.moodValue}%",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(top = 16.dp),
                    )
                    Slider(
                        value = state.moodValue.toFloat(),
                        onValueChange = { viewModel.onMoodChange(it.toInt()) },
                        valueRange = 0f..100f,
                    )
                }
                JournalEntryType.CHECKLIST -> Unit
            }

            Text(
                "Link to a habit (optional)",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 20.dp, bottom = 8.dp),
            )
            HabitPicker(
                habits = habits,
                selectedHabitId = state.linkedHabitId,
                onSelect = viewModel::onLinkedHabitChange,
            )

            state.error?.let {
                Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 16.dp))
            }

            Button(
                onClick = viewModel::saveEntry,
                enabled = !state.isSaving,
                modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
            ) {
                Text("Save Entry")
            }
        }
    }
}

@Composable
private fun HabitPicker(
    habits: List<Habit>,
    selectedHabitId: String?,
    onSelect: (String?) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val label = habits.firstOrNull { it.id == selectedHabitId }?.name ?: "General"

    Box {
        OutlinedButton(onClick = { expanded = true }) { Text(label) }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(text = { Text("General") }, onClick = { onSelect(null); expanded = false })
            habits.forEach { habit ->
                DropdownMenuItem(text = { Text(habit.name) }, onClick = { onSelect(habit.id); expanded = false })
            }
        }
    }
}
