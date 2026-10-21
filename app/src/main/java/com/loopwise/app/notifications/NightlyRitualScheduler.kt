package com.loopwise.app.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.os.Build
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

private const val RITUAL_REQUEST_CODE = 990_011 // exactly one global alarm — no per-item variants needed
const val NIGHTLY_RITUAL_HOUR = 22
const val NIGHTLY_RITUAL_MINUTE = 0
const val NIGHTLY_RITUAL_SNOOZE_MINUTES = 15L

/**
 * Schedules the single nightly-ritual alarm, reusing the same exact-alarm + inexact
 * fallback pattern ReminderScheduler uses for habit/task reminders. A snooze simply
 * re-arms the same alarm slot for [NIGHTLY_RITUAL_SNOOZE_MINUTES] from now rather than
 * needing a second alarm — when it fires it's indistinguishable from the regular nightly
 * firing, which is what we want (same "is it already done" check applies either way).
 */
class NightlyRitualScheduler(private val context: Context) {
    private val alarmManager: AlarmManager? = context.getSystemService(AlarmManager::class.java)

    fun scheduleNightly(time: LocalTime = LocalTime.of(NIGHTLY_RITUAL_HOUR, NIGHTLY_RITUAL_MINUTE)) {
        setAlarm(nextOccurrenceMillis(time))
    }

    fun scheduleSnooze(minutes: Long = NIGHTLY_RITUAL_SNOOZE_MINUTES) {
        setAlarm(System.currentTimeMillis() + minutes * 60_000L)
    }

    /** Dismisses tonight's notification if it's showing. Doesn't touch the armed alarm —
     *  if a snooze is still pending it'll fire later and silently no-op (see
     *  NightlyRitualReceiver), which is simpler and safer than trying to distinguish
     *  "cancel today's leftover snooze" from "don't cancel tomorrow's already-armed slot"
     *  when both share the same AlarmManager request code. */
    fun dismissForTonight() {
        cancelNightlyRitualNotification(context)
    }

    private fun pendingIntent(): PendingIntent = PendingIntent.getBroadcast(
        context,
        RITUAL_REQUEST_CODE,
        NightlyRitualReceiver.showIntent(context),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    private fun setAlarm(triggerAtMillis: Long) {
        val am = alarmManager ?: return
        val pendingIntent = pendingIntent()
        val canScheduleExact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || am.canScheduleExactAlarms()
        if (canScheduleExact) {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        } else {
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
