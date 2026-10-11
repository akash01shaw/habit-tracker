@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.loopwise.app.ui.timer

import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import com.loopwise.app.ui.common.ComingSoonBody

@Composable
fun TimerScreen() {
    Scaffold(topBar = { TopAppBar(title = { Text("Timer") }) }) { padding ->
        ComingSoonBody(padding, "Stopwatch and countdown focus timer is coming in Phase 2.")
    }
}
