package com.loopwise.app.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class NightlyRitualReceiver : BroadcastReceiver(), KoinComponent {

    private val scheduler: NightlyRitualScheduler by inject()
    private val ritualState: NightlyRitualState by inject()

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_SHOW -> {
                // Already done tonight (ran it early from the manual entry point, or this
                // is a stale snooze firing after the user finished from inside the app) —
                // skip showing it, but still re-arm tomorrow's alarm so the schedule
                // doesn't drift.
                if (!ritualState.isCompletedToday()) {
                    showNightlyRitualNotification(context)
                }
                scheduler.scheduleNightly()
            }
            ACTION_SNOOZE -> {
                cancelNightlyRitualNotification(context)
                scheduler.scheduleSnooze()
            }
        }
    }

    companion object {
        const val ACTION_SHOW = "com.loopwise.app.action.NIGHTLY_RITUAL_SHOW"
        const val ACTION_SNOOZE = "com.loopwise.app.action.NIGHTLY_RITUAL_SNOOZE"

        fun showIntent(context: Context): Intent =
            Intent(context, NightlyRitualReceiver::class.java).apply { action = ACTION_SHOW }

        fun snoozeIntent(context: Context): Intent =
            Intent(context, NightlyRitualReceiver::class.java).apply { action = ACTION_SNOOZE }
    }
}
