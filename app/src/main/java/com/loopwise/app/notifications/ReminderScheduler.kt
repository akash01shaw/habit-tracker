package com.loopwise.app.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.os.Build
import java.time.Instant
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

fun habitReminderRequestCode(habitId: String, time: LocalTime): Int = "habit:$habitId:$time".hashCode()
fun taskReminderRequestCode(taskId: String): Int = "task:$taskId".hashCode()

/**
 * Wraps AlarmManager for the two reminder shapes the spec needs: a habit reminder that
 * repeats at the same time every day (self-rescheduled by the receiver each time it
 * fires, since AlarmManager alarms are one-shot), and a task reminder at one specific
 * instant. WorkManager's periodic jobs can't guarantee exact-time firing (15-minute
 * minimum granularity), so AlarmManager is the right tool here despite needing more
 * manual bookkeeping (surviving reboots, in particular — see BootReceiver).
 */
class ReminderScheduler(private val context: Context) {
    private val alarmManager: AlarmManager? = context.getSystemService(AlarmManager::class.java)

    fun scheduleHabitReminder(habitId: String, habitName: String, time: LocalTime) {
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            habitReminderRequestCode(habitId, time),
            ReminderBroadcastReceiver.habitIntent(context, habitId, habitName, time),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        setExactAlarm(nextOccurrenceMillis(time), pendingIntent)
    }

    fun cancelHabitReminder(habitId: String, time: LocalTime) {
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            habitReminderRequestCode(habitId, time),
            ReminderBroadcastReceiver.habitIntent(context, habitId, "", time),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        alarmManager?.cancel(pendingIntent)
    }

    fun scheduleTaskReminder(taskId: String, title: String, at: Instant) {
        if (at.isBefore(Instant.now())) return
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            taskReminderRequestCode(taskId),
            ReminderBroadcastReceiver.taskIntent(context, taskId, title),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        setExactAlarm(at.toEpochMilli(), pendingIntent)
    }

    fun cancelTaskReminder(taskId: String) {
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            taskReminderRequestCode(taskId),
            ReminderBroadcastReceiver.taskIntent(context, taskId, ""),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        alarmManager?.cancel(pendingIntent)
    }

    private fun setExactAlarm(triggerAtMillis: Long, pendingIntent: PendingIntent) {
        val am = alarmManager ?: return
        val canScheduleExact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || am.canScheduleExactAlarms()
        if (canScheduleExact) {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        } else {
            // User hasn't granted the Android 12+ "Alarms & reminders" special permission —
            // fall back to an inexact alarm rather than crash.
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        }
    }

    private fun nextOccurrenceMillis(time: LocalTime): Long {
        val now = LocalDateTime.now()
        var candidate = LocalDateTime.of(now.toLocalDate(), time)
        if (!candidate.isAfter(now)) candidate = candidate.plusDays(1)
        return candidate.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }
}
