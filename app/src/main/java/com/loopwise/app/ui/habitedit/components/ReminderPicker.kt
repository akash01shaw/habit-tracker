package com.loopwise.app.ui.habitedit.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Icon
import androidx.compose.material3.InputChip
import androidx.compose.material3.InputChipDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.loopwise.app.ui.common.TimePickerDialog
import java.time.LocalTime
import java.time.format.DateTimeFormatter

private val timeFormatter = DateTimeFormatter.ofPattern("h:mm a")

/** Reminder times as removable chips, plus an "Add reminder" chip that opens a time picker dialog. */
@Composable
fun ReminderPicker(
    reminders: List<LocalTime>,
    onAdd: (LocalTime) -> Unit,
    onRemove: (LocalTime) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showPicker by remember { mutableStateOf(false) }

    Column(modifier = modifier) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            reminders.forEach { time ->
                InputChip(
                    selected = false,
                    onClick = { onRemove(time) },
                    label = { Text(time.format(timeFormatter)) },
                    trailingIcon = { Icon(Icons.Filled.Close, contentDescription = "Remove reminder") },
                    colors = InputChipDefaults.inputChipColors(),
                )
            }
            AssistChip(
                onClick = { showPicker = true },
                leadingIcon = { Icon(Icons.Filled.Add, contentDescription = null) },
                label = { Text("Add reminder") },
            )
        }
    }

    if (showPicker) {
        TimePickerDialog(
            onConfirm = { time ->
                onAdd(time)
                showPicker = false
            },
            onDismiss = { showPicker = false },
        )
    }
}
