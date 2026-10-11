package com.loopwise.app.notifications

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.loopwise.app.data.repository.AuthRepository
import com.loopwise.app.data.repository.HabitRepository
import com.loopwise.app.data.repository.TaskRepository
import kotlinx.coroutines.flow.first
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * AlarmManager alarms are cleared on reboot, so BootReceiver enqueues this worker to
 * re-arm every habit/task reminder from what's in Room. Runs via WorkManager (not
 * directly in the receiver) since re-reading the database and rescheduling many alarms
 * can take longer than a BroadcastReceiver's execution window allows.
 */
class RescheduleRemindersWorker(context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params), KoinComponent {

    private val authRepository: AuthRepository by inject()
    private val habitRepository: HabitRepository by inject()
    private val taskRepository: TaskRepository by inject()
    private val scheduler: ReminderScheduler by inject()

    override suspend fun doWork(): Result {
        val userId = authRepository.currentUserIdOrNull() ?: return Result.success()

        habitRepository.observeHabits(userId).first().forEach { habit ->
            habit.reminders.forEach { time -> scheduler.scheduleHabitReminder(habit.id, habit.name, time) }
        }

        taskRepository.getUpcomingReminders(userId).forEach { task ->
            val reminderTime = task.reminderTime ?: return@forEach
            scheduler.scheduleTaskReminder(task.id, task.title, reminderTime)
        }

        return Result.success()
    }
}
