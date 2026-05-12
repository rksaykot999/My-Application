package com.rksaykot.myapplication.service

import android.app.Service
import android.content.Intent
import android.os.IBinder

class BackgroundSyncService : Service() {

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

}
