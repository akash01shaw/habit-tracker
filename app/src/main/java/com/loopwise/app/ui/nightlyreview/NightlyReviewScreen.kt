@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.loopwise.app.ui.nightlyreview

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.loopwise.app.domain.model.Task

@Composable
fun NightlyReviewScreen(viewModel: NightlyReviewViewModel, onDone: () -> Unit) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(state.finished) {
        if (state.finished) onDone()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Nightly Review") },
                navigationIcon = {
                    IconButton(onClick = onDone) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        if (!state.isSignedIn) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("Sign in to do tonight's review.")
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        ) {
            SectionLabel("Finish today's to-dos")
            if (state.todayIncompleteTasks.isEmpty()) {
                Text(
                    "All done for today.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                state.todayIncompleteTasks.forEach { task ->
                    QuickTaskRow(task = task, onToggle = { viewModel.toggleTask(task) })
                }
            }

            SectionLabel("Journal about today")
            OutlinedTextField(
                value = state.journalText,
                onValueChange = viewModel::onJournalTextChange,
                label = { Text("What happened today?") },
                modifier = Modifier.fillMaxWidth().height(120.dp),
            )
            OutlinedButton(
                onClick = viewModel::saveJournalEntry,
                enabled = state.journalText.isNotBlank() && !state.isSavingJournal,
                modifier = Modifier.padding(top = 8.dp),
            ) {
                Text(if (state.journalSaved) "Saved" else "Save journal entry")
            }

            SectionLabel("Plan tomorrow")
            if (state.tomorrowTasks.isNotEmpty()) {
                state.tomorrowTasks.forEach { task ->
                    Text(
                        "• ${task.title}",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(vertical = 2.dp),
                    )
                }
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(top = 8.dp),
            ) {
                OutlinedTextField(
                    value = state.newTaskTitle,
                    onValueChange = viewModel::onNewTaskTitleChange,
                    label = { Text("Add a to-do for tomorrow") },
                    modifier = Modifier.weight(1f),
                )
                Button(onClick = viewModel::addTomorrowTask) { Text("Add") }
            }

            Button(
                onClick = viewModel::finishForTonight,
                modifier = Modifier.fillMaxWidth().padding(top = 32.dp),
            ) {
                Text("Done for tonight")
            }
        }
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

@Composable
private fun QuickTaskRow(task: Task, onToggle: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Checkbox(checked = task.isCompleted, onCheckedChange = { onToggle() })
        Text(task.title, modifier = Modifier.weight(1f))
    }
}
