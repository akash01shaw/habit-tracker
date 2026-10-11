@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.loopwise.app.ui.schedule

import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import com.loopwise.app.ui.common.ComingSoonBody

@Composable
fun ScheduleScreen() {
    Scaffold(topBar = { TopAppBar(title = { Text("Schedule") }) }) { padding ->
        ComingSoonBody(padding, "Week-grid schedule view is coming in Phase 2.")
    }
}
