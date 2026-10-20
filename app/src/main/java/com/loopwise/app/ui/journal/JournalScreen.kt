@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.loopwise.app.ui.journal

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.loopwise.app.domain.model.Habit
import com.loopwise.app.domain.model.JournalCategory
import com.loopwise.app.domain.model.JournalEntryType
import com.loopwise.app.ui.common.TopBannerHeader
import com.loopwise.app.ui.journal.components.JournalEntryCard

@Composable
fun JournalScreen(viewModel: JournalViewModel, onAddEntry: () -> Unit) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = { TopBannerHeader(screenLabel = "Journal") },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddEntry) {
                Icon(Icons.Filled.Add, contentDescription = "New journal entry")
            }
        },
    ) { padding ->
        if (!state.isSignedIn) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("Sign in to see your journal.")
            }
            return@Scaffold
        }

        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            OutlinedTextField(
                value = state.filter.searchQuery,
                onValueChange = viewModel::setSearchQuery,
                placeholder = { Text("Search journal entries") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                HabitFilterDropdown(
                    habits = state.habits,
                    selectedHabitId = state.filter.habitId,
                    onSelect = viewModel::setHabitFilter,
                )
                TypeFilterDropdown(
                    selectedType = state.filter.type,
                    onSelect = viewModel::setTypeFilter,
                )
                CategoryFilterDropdown(
                    selectedCategory = state.filter.category,
                    onSelect = viewModel::setCategoryFilter,
                )
                DateRangeFilterDropdown(
                    selectedRange = state.filter.dateRange,
                    onSelect = viewModel::setDateRangeFilter,
                )
            }

            if (state.entries.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                    Text(
                        "No journal entries yet — tap + to write one.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
            } else {
                val habitsById = state.habits.associateBy { it.id }
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(state.entries, key = { it.id }) { entry ->
                        JournalEntryCard(
                            entry = entry,
                            linkedHabit = entry.linkedHabitId?.let(habitsById::get),
                            onToggleChecklistItem = { index -> viewModel.toggleChecklistItem(entry, index) },
                            onToggleBulletItem = { index -> viewModel.toggleBulletItem(entry, index) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HabitFilterDropdown(
    habits: List<Habit>,
    selectedHabitId: String?,
    onSelect: (String?) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val label = habits.firstOrNull { it.id == selectedHabitId }?.name ?: "All Habits"

    Box {
        OutlinedButton(onClick = { expanded = true }) { Text(label) }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(text = { Text("All Habits") }, onClick = { onSelect(null); expanded = false })
            habits.forEach { habit ->
                DropdownMenuItem(text = { Text(habit.name) }, onClick = { onSelect(habit.id); expanded = false })
            }
        }
    }
}

@Composable
private fun TypeFilterDropdown(selectedType: JournalEntryType?, onSelect: (JournalEntryType?) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val label = when (selectedType) {
        null -> "All Entries"
        JournalEntryType.TEXT -> "Text"
        JournalEntryType.MOOD -> "Mood"
        JournalEntryType.CHECKLIST -> "Checklist"
        JournalEntryType.BULLET_LOG -> "Bullet Log"
    }

    Box {
        OutlinedButton(onClick = { expanded = true }) { Text(label) }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(text = { Text("All Entries") }, onClick = { onSelect(null); expanded = false })
            DropdownMenuItem(text = { Text("Text") }, onClick = { onSelect(JournalEntryType.TEXT); expanded = false })
            DropdownMenuItem(text = { Text("Mood") }, onClick = { onSelect(JournalEntryType.MOOD); expanded = false })
            DropdownMenuItem(
                text = { Text("Checklist") },
                onClick = { onSelect(JournalEntryType.CHECKLIST); expanded = false },
            )
            DropdownMenuItem(
                text = { Text("Bullet Log") },
                onClick = { onSelect(JournalEntryType.BULLET_LOG); expanded = false },
            )
        }
    }
}

@Composable
private fun CategoryFilterDropdown(selectedCategory: JournalCategory?, onSelect: (JournalCategory?) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val label = selectedCategory?.label ?: "All Categories"

    Box {
        OutlinedButton(onClick = { expanded = true }) { Text(label) }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(text = { Text("All Categories") }, onClick = { onSelect(null); expanded = false })
            JournalCategory.entries.forEach { category ->
                DropdownMenuItem(
                    text = { Text(category.label) },
                    onClick = { onSelect(category); expanded = false },
                )
            }
        }
    }
}

@Composable
private fun DateRangeFilterDropdown(selectedRange: JournalDateRange, onSelect: (JournalDateRange) -> Unit) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        OutlinedButton(onClick = { expanded = true }) { Text(selectedRange.label) }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            JournalDateRange.entries.forEach { range ->
                DropdownMenuItem(
                    text = { Text(range.label) },
                    onClick = { onSelect(range); expanded = false },
                )
            }
        }
    }
}
