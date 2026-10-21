package com.loopwise.app

import android.app.Application
import com.loopwise.app.di.appModule
import com.loopwise.app.notifications.NightlyRitualScheduler
import com.loopwise.app.notifications.createNightlyRitualChannel
import com.loopwise.app.notifications.createNotificationChannel
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class LoopwiseApp : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@LoopwiseApp)
            modules(appModule)
        }
        createNotificationChannel(this)
        createNightlyRitualChannel(this)
        // Idempotent (AlarmManager just overwrites the same pending intent) — arming it on
        // every process start, not just first install, is what keeps it alive across a
        // reboot: BootReceiver's WorkManager job starts this process anyway to run
        // RescheduleRemindersWorker, which re-runs this onCreate.
        NightlyRitualScheduler(this).scheduleNightly()
    }
}
