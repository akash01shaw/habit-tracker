package com.loopwise.app.notifications

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.loopwise.app.MainActivity

const val REMINDER_CHANNEL_ID = "reminders"
const val RITUAL_CHANNEL_ID = "nightly_ritual"
const val NIGHTLY_RITUAL_NOTIFICATION_ID = 99_001

/** MainActivity reads this action to deep-link straight into the Nightly Review screen. */
const val ACTION_OPEN_NIGHTLY_REVIEW = "com.loopwise.app.action.OPEN_NIGHTLY_REVIEW"

fun createNotificationChannel(context: Context) {
    val channel = NotificationChannel(
        REMINDER_CHANNEL_ID,
        "Habit & task reminders",
        NotificationManager.IMPORTANCE_DEFAULT,
    ).apply {
        description = "Reminders for habits and tasks you've scheduled"
    }
    context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
}

/** Higher importance than the ordinary reminder channel — this one is meant to nag. */
fun createNightlyRitualChannel(context: Context) {
    val channel = NotificationChannel(
        RITUAL_CHANNEL_ID,
        "Nightly review",
        NotificationManager.IMPORTANCE_HIGH,
    ).apply {
        description = "A nightly reminder to wrap up today's to-dos, journal, and plan tomorrow"
    }
    context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
}

private fun hasNotificationPermission(context: Context): Boolean =
    Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
        PackageManager.PERMISSION_GRANTED

/** Shows a reminder notification. Silently no-ops if POST_NOTIFICATIONS hasn't been granted. */
@SuppressLint("MissingPermission") // guarded by hasNotificationPermission() above — lint can't trace it cross-function
fun showReminderNotification(context: Context, notificationId: Int, title: String, text: String) {
    if (!hasNotificationPermission(context)) return

    val openAppIntent = Intent(context, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
    }
    val contentIntent = PendingIntent.getActivity(
        context,
        notificationId,
        openAppIntent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    val notification = NotificationCompat.Builder(context, REMINDER_CHANNEL_ID)
        .setSmallIcon(android.R.drawable.ic_popup_reminder) // TODO: replace with a Loopwise-branded icon
        .setContentTitle(title)
        .setContentText(text)
        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        .setAutoCancel(true)
        .setContentIntent(contentIntent)
        .build()

    NotificationManagerCompat.from(context).notify(notificationId, notification)
}

/**
 * The nightly "wrap up your day" notification: ongoing (can't be swiped away — the closest
 * practical equivalent Android allows to "must complete"; it can still only truly be closed
 * by completing the in-app ritual or by the user digging into system notification settings)
 * with a single Snooze action. No "mark complete" action button on purpose — completion
 * happens by actually doing the ritual in NightlyReviewScreen, not by tapping it away.
 */
@SuppressLint("MissingPermission") // guarded by hasNotificationPermission() above — lint can't trace it cross-function
fun showNightlyRitualNotification(context: Context) {
    if (!hasNotificationPermission(context)) return

    val openAppIntent = Intent(context, MainActivity::class.java).apply {
        action = ACTION_OPEN_NIGHTLY_REVIEW
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
    }
    val contentIntent = PendingIntent.getActivity(
        context,
        NIGHTLY_RITUAL_NOTIFICATION_ID,
        openAppIntent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )
    val snoozeIntent = PendingIntent.getBroadcast(
        context,
        NIGHTLY_RITUAL_NOTIFICATION_ID,
        NightlyRitualReceiver.snoozeIntent(context),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    val notification = NotificationCompat.Builder(context, RITUAL_CHANNEL_ID)
        .setSmallIcon(android.R.drawable.ic_popup_reminder) // TODO: replace with a Loopwise-branded icon
        .setContentTitle("Wrap up your day")
        .setContentText("Finish today's to-dos, jot a journal entry, and plan tomorrow.")
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setOngoing(true)
        .setAutoCancel(false)
        .setContentIntent(contentIntent)
        .addAction(0, "Snooze 15 min", snoozeIntent)
        .build()

    NotificationManagerCompat.from(context).notify(NIGHTLY_RITUAL_NOTIFICATION_ID, notification)
}

fun cancelNightlyRitualNotification(context: Context) {
    NotificationManagerCompat.from(context).cancel(NIGHTLY_RITUAL_NOTIFICATION_ID)
}
