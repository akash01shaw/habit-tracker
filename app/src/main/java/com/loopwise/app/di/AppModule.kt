package com.loopwise.app.di

import androidx.room.Room
import com.loopwise.app.data.local.LoopwiseDatabase
import com.loopwise.app.data.remote.buildSupabaseClient
import com.loopwise.app.data.repository.AuthRepository
import com.loopwise.app.data.repository.HabitRepository
import com.loopwise.app.data.repository.JournalRepository
import com.loopwise.app.data.repository.TaskRepository
import com.loopwise.app.data.repository.TimerRepository
import com.loopwise.app.notifications.ReminderScheduler
import com.loopwise.app.ui.auth.AuthViewModel
import com.loopwise.app.ui.habitedit.CreateEditHabitViewModel
import com.loopwise.app.ui.journal.JournalViewModel
import com.loopwise.app.ui.reports.ReportsViewModel
import com.loopwise.app.ui.taskedit.CreateTaskViewModel
import com.loopwise.app.ui.timer.TimerViewModel
import com.loopwise.app.ui.today.TodayViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    single { buildSupabaseClient() }

    single {
        Room.databaseBuilder(get(), LoopwiseDatabase::class.java, LoopwiseDatabase.NAME)
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()
    }
    single { get<LoopwiseDatabase>().habitDao() }
    single { get<LoopwiseDatabase>().taskDao() }
    single { get<LoopwiseDatabase>().habitLogDao() }
    single { get<LoopwiseDatabase>().journalEntryDao() }
    single { get<LoopwiseDatabase>().timerSessionDao() }

    single { AuthRepository(get()) }
    single { HabitRepository(get(), get(), get()) }
    single { TaskRepository(get(), get()) }
    single { JournalRepository(get(), get()) }
    single { TimerRepository(get(), get()) }
    single { ReminderScheduler(get()) }

    viewModel { AuthViewModel(get()) }
    viewModel { TodayViewModel(get(), get(), get()) }
    viewModel { CreateEditHabitViewModel(get(), get(), get()) }
    viewModel { ReportsViewModel(get(), get()) }
    viewModel { JournalViewModel(get(), get(), get()) }
    viewModel { CreateTaskViewModel(get(), get(), get()) }
    viewModel { TimerViewModel(get(), get(), get()) }
}
