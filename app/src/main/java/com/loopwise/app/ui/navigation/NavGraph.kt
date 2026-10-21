package com.loopwise.app.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.loopwise.app.ui.auth.AuthViewModel
import com.loopwise.app.ui.auth.SignInScreen
import com.loopwise.app.ui.auth.SignUpScreen
import com.loopwise.app.ui.habitedit.CreateEditHabitScreen
import com.loopwise.app.ui.habitedit.CreateEditHabitViewModel
import com.loopwise.app.ui.journal.JournalScreen
import com.loopwise.app.ui.journal.JournalViewModel
import com.loopwise.app.ui.journal.NewJournalEntryScreen
import com.loopwise.app.ui.nightlyreview.NightlyReviewScreen
import com.loopwise.app.ui.nightlyreview.NightlyReviewViewModel
import com.loopwise.app.ui.reports.ReportsScreen
import com.loopwise.app.ui.reports.ReportsViewModel
import com.loopwise.app.ui.schedule.ScheduleScreen
import com.loopwise.app.ui.schedule.ScheduleViewModel
import com.loopwise.app.ui.taskedit.CreateTaskScreen
import com.loopwise.app.ui.taskedit.CreateTaskViewModel
import com.loopwise.app.ui.timer.TimerScreen
import com.loopwise.app.ui.timer.TimerViewModel
import com.loopwise.app.ui.today.TodayScreen
import com.loopwise.app.ui.today.TodayViewModel
import org.koin.androidx.compose.koinViewModel

@Composable
fun LoopwiseNavHost(pendingRoute: String? = null, onPendingRouteConsumed: () -> Unit = {}) {
    val authViewModel: AuthViewModel = koinViewModel()
    val user by authViewModel.currentUser.collectAsState()

    if (user == null) {
        AuthNavHost(authViewModel)
    } else {
        MainNavHost(pendingRoute, onPendingRouteConsumed)
    }
}

@Composable
private fun AuthNavHost(authViewModel: AuthViewModel) {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = Destination.SignIn.route) {
        composable(Destination.SignIn.route) {
            SignInScreen(
                viewModel = authViewModel,
                onNavigateToSignUp = { navController.navigate(Destination.SignUp.route) },
            )
        }
        composable(Destination.SignUp.route) {
            SignUpScreen(
                viewModel = authViewModel,
                onNavigateToSignIn = { navController.popBackStack() },
            )
        }
    }
}

@Composable
private fun MainNavHost(pendingRoute: String? = null, onPendingRouteConsumed: () -> Unit = {}) {
    val navController = rememberNavController()

    LaunchedEffect(pendingRoute) {
        if (pendingRoute != null) {
            navController.navigate(pendingRoute)
            onPendingRouteConsumed()
        }
    }

    Scaffold(bottomBar = { LoopwiseBottomNavBar(navController) }) { padding ->
        NavHost(
            navController = navController,
            startDestination = Destination.Today.route,
            modifier = Modifier.padding(padding),
        ) {
            composable(Destination.Today.route) {
                val viewModel: TodayViewModel = koinViewModel()
                TodayScreen(
                    viewModel = viewModel,
                    onAddHabit = { navController.navigate(Destination.CreateHabit.route) },
                    onAddTask = { navController.navigate(Destination.CreateTask.route) },
                    onEditHabit = { habitId -> navController.navigate(Destination.EditHabit.createRoute(habitId)) },
                    onEditTask = { taskId -> navController.navigate(Destination.EditTask.createRoute(taskId)) },
                    onOpenNightlyReview = { navController.navigate(Destination.NightlyReview.route) },
                )
            }
            composable(Destination.Schedule.route) {
                val viewModel: ScheduleViewModel = koinViewModel()
                ScheduleScreen(
                    viewModel = viewModel,
                    onEditHabit = { habitId -> navController.navigate(Destination.EditHabit.createRoute(habitId)) },
                    onEditTask = { taskId -> navController.navigate(Destination.EditTask.createRoute(taskId)) },
                )
            }
            composable(Destination.Timer.route) {
                val viewModel: TimerViewModel = koinViewModel()
                TimerScreen(viewModel = viewModel)
            }
            composable(Destination.Reports.route) {
                val viewModel: ReportsViewModel = koinViewModel()
                ReportsScreen(viewModel = viewModel)
            }
            composable(Destination.Journal.route) {
                val viewModel: JournalViewModel = koinViewModel()
                JournalScreen(
                    viewModel = viewModel,
                    onAddEntry = { navController.navigate(Destination.NewJournalEntry.route) },
                )
            }
            composable(Destination.NewJournalEntry.route) {
                val viewModel: JournalViewModel = koinViewModel()
                NewJournalEntryScreen(
                    viewModel = viewModel,
                    onDone = { navController.popBackStack() },
                )
            }
            composable(Destination.CreateHabit.route) {
                val viewModel: CreateEditHabitViewModel = koinViewModel()
                CreateEditHabitScreen(
                    viewModel = viewModel,
                    onDone = { navController.popBackStack() },
                )
            }
            composable(
                route = Destination.EditHabit.route,
                arguments = listOf(navArgument("habitId") { type = NavType.StringType }),
            ) { backStackEntry ->
                val viewModel: CreateEditHabitViewModel = koinViewModel()
                CreateEditHabitScreen(
                    viewModel = viewModel,
                    habitId = backStackEntry.arguments?.getString("habitId"),
                    onDone = { navController.popBackStack() },
                )
            }
            composable(Destination.CreateTask.route) {
                val viewModel: CreateTaskViewModel = koinViewModel()
                CreateTaskScreen(
                    viewModel = viewModel,
                    onDone = { navController.popBackStack() },
                )
            }
            composable(
                route = Destination.EditTask.route,
                arguments = listOf(navArgument("taskId") { type = NavType.StringType }),
            ) { backStackEntry ->
                val viewModel: CreateTaskViewModel = koinViewModel()
                CreateTaskScreen(
                    viewModel = viewModel,
                    taskId = backStackEntry.arguments?.getString("taskId"),
                    onDone = { navController.popBackStack() },
                )
            }
            composable(Destination.NightlyReview.route) {
                val viewModel: NightlyReviewViewModel = koinViewModel()
                NightlyReviewScreen(
                    viewModel = viewModel,
                    onDone = { navController.popBackStack() },
                )
            }
        }
    }
}
