package com.loopwise.app.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Timer
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Destination(val route: String) {
    data object SignIn : Destination("sign_in")
    data object SignUp : Destination("sign_up")

    data object Today : Destination("today")
    data object Schedule : Destination("schedule")
    data object Timer : Destination("timer")
    data object Reports : Destination("reports")
    data object Journal : Destination("journal")

    data object CreateHabit : Destination("habit/new")
    data object EditHabit : Destination("habit/{habitId}") {
        fun createRoute(habitId: String) = "habit/$habitId"
    }
    data object CreateTask : Destination("task/new")
    data object EditTask : Destination("task/{taskId}") {
        fun createRoute(taskId: String) = "task/$taskId"
    }

    data object NewJournalEntry : Destination("journal/new")
}

data class BottomTab(val destination: Destination, val label: String, val icon: ImageVector)

val bottomTabs = listOf(
    BottomTab(Destination.Today, "Today", Icons.Filled.CheckCircle),
    BottomTab(Destination.Schedule, "Schedule", Icons.Filled.CalendarMonth),
    BottomTab(Destination.Timer, "Timer", Icons.Filled.Timer),
    BottomTab(Destination.Reports, "Reports", Icons.Filled.BarChart),
    BottomTab(Destination.Journal, "Journal", Icons.Filled.Book),
)
