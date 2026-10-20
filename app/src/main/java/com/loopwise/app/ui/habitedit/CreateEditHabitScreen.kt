@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.loopwise.app.ui.habitedit

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.health.connect.client.PermissionController
import com.loopwise.app.domain.model.FrequencyKind
import com.loopwise.app.domain.model.HabitType
import com.loopwise.app.domain.model.HealthSyncSource
import com.loopwise.app.ui.habitedit.components.ReminderPicker

@Composable
fun CreateEditHabitScreen(
    viewModel: CreateEditHabitViewModel,
    habitId: String? = null,
    onDone: () -> Unit,
) {
    val state by viewModel.formState.collectAsState()
    var showDeleteConfirm by remember { mutableStateOf(false) }
    val healthPermissionLauncher = rememberLauncherForActivityResult(
        contract = PermissionController.createRequestPermissionResultContract(),
    ) {}

    LaunchedEffect(habitId) {
        if (habitId != null) viewModel.loadForEdit(habitId)
    }

    LaunchedEffect(state.saved, state.deleted) {
        if (state.saved || state.deleted) onDone()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (state.isEditing) "Edit Habit" else "New Habit") },
                navigationIcon = {
                    IconButton(onClick = onDone) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (state.isEditing) {
                        IconButton(onClick = { showDeleteConfirm = true }) {
                            Icon(Icons.Filled.Delete, contentDescription = "Delete habit")
                        }
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        ) {
            OutlinedTextField(
                value = state.name,
                onValueChange = viewModel::onNameChange,
                label = { Text("Habit name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            SectionLabel("Type")
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                HabitType.entries.forEach { type ->
                    FilterChip(
                        selected = state.type == type,
                        onClick = { viewModel.onTypeChange(type) },
                        label = { Text(type.name.lowercase().replaceFirstChar(Char::uppercase)) },
                    )
                }
            }

            SectionLabel("Color")
            LazyRow {
                items(state.availableColors) { colorLong ->
                    val color = Color(colorLong)
                    val selected = state.color == colorLong
                    Box(
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(color)
                            .border(
                                width = if (selected) 3.dp else 0.dp,
                                color = MaterialTheme.colorScheme.onSurface,
                                shape = CircleShape,
                            )
                            .clickable { viewModel.onColorChange(colorLong) },
                    )
                }
            }

            SectionLabel("Frequency")
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FrequencyKind.entries.forEach { kind ->
                    FilterChip(
                        selected = state.frequencyKind == kind,
                        onClick = { viewModel.onFrequencyKindChange(kind) },
                        label = { Text(frequencyLabel(kind)) },
                    )
                }
            }
            if (state.frequencyKind == FrequencyKind.WEEKDAYS) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.padding(top = 8.dp)) {
                    listOf(1 to "M", 2 to "T", 3 to "W", 4 to "T", 5 to "F", 6 to "S", 7 to "S").forEach { (day, label) ->
                        FilterChip(
                            selected = day in state.selectedWeekdays,
                            onClick = { viewModel.onWeekdayToggle(day) },
                            label = { Text(label) },
                        )
                    }
                }
            }

            if (state.needsTarget) {
                SectionLabel("Target")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = state.targetValue,
                        onValueChange = viewModel::onTargetValueChange,
                        label = { Text("Target") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        value = state.unit,
                        onValueChange = viewModel::onUnitChange,
                        label = { Text("Unit (e.g. cups, min)") },
                        modifier = Modifier.weight(1f),
                    )
                }

                SectionLabel("Sync from Health Connect")
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    FilterChip(
                        selected = state.healthSyncSource == null,
                        onClick = { viewModel.onHealthSyncSourceChange(null) },
                        label = { Text("Manual") },
                    )
                    HealthSyncSource.entries.forEach { source ->
                        FilterChip(
                            selected = state.healthSyncSource == source,
                            onClick = {
                                viewModel.onHealthSyncSourceChange(source)
                                healthPermissionLauncher.launch(viewModel.healthPermissions)
                            },
                            label = { Text(source.label) },
                        )
                    }
                }
                if (state.healthSyncSource != null) {
                    Text(
                        "Today's value auto-fills from Health Connect — tap the sync icon on Today to refresh it manually.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }

            SectionLabel("Reminders")
            ReminderPicker(
                reminders = state.reminders,
                onAdd = viewModel::addReminder,
                onRemove = viewModel::removeReminder,
            )

            state.error?.let {
                Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 16.dp))
            }

            Button(
                onClick = viewModel::save,
                enabled = !state.isSaving && !state.isDeleting,
                modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
            ) {
                if (state.isSaving) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text(if (state.isEditing) "Save Changes" else "Save Habit")
                }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete \"${state.name}\"?") },
            text = { Text("This removes the habit and its logged history. This can't be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirm = false
                    viewModel.delete()
                }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") }
            },
        )
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(top = 20.dp, bottom = 8.dp),
    )
}

private fun frequencyLabel(kind: FrequencyKind) = when (kind) {
    FrequencyKind.DAILY -> "Daily"
    FrequencyKind.WEEKDAYS -> "Specific days"
    FrequencyKind.PER_WEEK -> "Per week"
    FrequencyKind.PER_MONTH -> "Per month"
}
