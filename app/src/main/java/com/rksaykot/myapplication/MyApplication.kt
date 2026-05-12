package com.rksaykot.myapplication

import android.app.Application
import androidx.work.Configuration
import androidx.work.WorkManager
import com.rksaykot.myapplication.notification.NotificationHelper

class MyApplication : Application() {

    companion object {
        lateinit var notificationHelper: NotificationHelper
            private set
    }

    override fun onCreate() {
        super.onCreate()

        // Notification channels create করুন
        notificationHelper = NotificationHelper(this)
        notificationHelper.createNotificationChannels()

        // WorkManager configure করুন - safely
        try {
            val config = Configuration.Builder()
                .setMinimumLoggingLevel(android.util.Log.INFO)
                .build()
            WorkManager.initialize(this, config)
        } catch (e: Exception) {
            // WorkManager might already be initialized, ignore
            e.printStackTrace()
        } catch (e: IllegalStateException) {
            // WorkManager already initialized
        }
    }
}