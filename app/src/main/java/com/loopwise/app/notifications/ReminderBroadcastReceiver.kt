package com.loopwise.app.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.time.LocalTime

class ReminderBroadcastReceiver : BroadcastReceiver(), KoinComponent {

    private val scheduler: ReminderScheduler by inject()

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_HABIT_REMINDER -> {
                val habitId = intent.getStringExtra(EXTRA_HABIT_ID) ?: return
                val habitName = intent.getStringExtra(EXTRA_HABIT_NAME) ?: return
                val time = intent.getStringExtra(EXTRA_TIME)?.let(LocalTime::parse) ?: return

                showReminderNotification(
                    context = context,
                    notificationId = habitReminderRequestCode(habitId, time),
                    title = habitName,
                    text = "Time for $habitName",
                )
                // AlarmManager alarms are one-shot — reschedule for the same time tomorrow.
                scheduler.scheduleHabitReminder(habitId, habitName, time)
            }
            ACTION_TASK_REMINDER -> {
                val taskId = intent.getStringExtra(EXTRA_TASK_ID) ?: return
                val title = intent.getStringExtra(EXTRA_TASK_TITLE) ?: return

                showReminderNotification(
                    context = context,
                    notificationId = taskReminderRequestCode(taskId),
                    title = title,
                    text = "Task reminder",
                )
            }
        }
    }

    companion object {
        const val ACTION_HABIT_REMINDER = "com.loopwise.app.action.HABIT_REMINDER"
        const val ACTION_TASK_REMINDER = "com.loopwise.app.action.TASK_REMINDER"
        private const val EXTRA_HABIT_ID = "habit_id"
        private const val EXTRA_HABIT_NAME = "habit_name"
        private const val EXTRA_TIME = "time"
        private const val EXTRA_TASK_ID = "task_id"
        private const val EXTRA_TASK_TITLE = "task_title"

        fun habitIntent(context: Context, habitId: String, habitName: String, time: LocalTime): Intent =
            Intent(context, ReminderBroadcastReceiver::class.java).apply {
                action = ACTION_HABIT_REMINDER
                putExtra(EXTRA_HABIT_ID, habitId)
                putExtra(EXTRA_HABIT_NAME, habitName)
                putExtra(EXTRA_TIME, time.toString())
            }

        fun taskIntent(context: Context, taskId: String, title: String): Intent =
            Intent(context, ReminderBroadcastReceiver::class.java).apply {
                action = ACTION_TASK_REMINDER
                putExtra(EXTRA_TASK_ID, taskId)
                putExtra(EXTRA_TASK_TITLE, title)
            }
    }
}
