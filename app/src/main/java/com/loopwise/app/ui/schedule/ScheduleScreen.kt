@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.loopwise.app.ui.schedule

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.loopwise.app.ui.common.ComingSoonBody
import com.loopwise.app.ui.schedule.components.AgendaView
import com.loopwise.app.ui.schedule.components.ScheduleHeader
import com.loopwise.app.ui.schedule.components.WeekGrid

/** Tapping a block jumps into that habit's or task's edit screen (spec §2.5). */
@Composable
fun ScheduleScreen(
    viewModel: ScheduleViewModel,
    onEditHabit: (String) -> Unit,
    onEditTask: (String) -> Unit,
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(topBar = { TopAppBar(title = { Text("Schedule") }) }) { padding ->
        if (!state.isSignedIn) {
            ComingSoonBody(padding, "Sign in to see your schedule.")
            return@Scaffold
        }

        val onBlockClick: (ScheduleBlock) -> Unit = { block ->
            when (block.kind) {
                ScheduleBlockKind.HABIT -> onEditHabit(block.sourceId)
                ScheduleBlockKind.TASK -> onEditTask(block.sourceId)
            }
        }

        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            ScheduleHeader(
                weekStart = state.weekStart,
                viewMode = state.viewMode,
                onPreviousWeek = viewModel::previousWeek,
                onNextWeek = viewModel::nextWeek,
                onToday = viewModel::goToCurrentWeek,
                onViewModeChange = viewModel::setViewMode,
            )

            if (state.blocks.isEmpty()) {
                Text(
                    "Nothing with a time set this week — add a reminder to a habit, or a due time to a task, to see it here.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(16.dp),
                )
            }

            when (state.viewMode) {
                ScheduleViewMode.AGENDA -> AgendaView(state.weekDates, state.blocks, onBlockClick = onBlockClick)
                ScheduleViewMode.GRID -> WeekGrid(state.weekDates, state.blocks, onBlockClick = onBlockClick)
            }
        }
    }
}
