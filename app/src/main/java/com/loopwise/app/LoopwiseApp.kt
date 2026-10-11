package com.loopwise.app

import android.app.Application
import com.loopwise.app.di.appModule
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
    }
}
