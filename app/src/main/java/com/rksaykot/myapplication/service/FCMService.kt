package com.rksaykot.myapplication.service

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.rksaykot.myapplication.notification.NotificationHelper

class FCMService : FirebaseMessagingService() {

    override fun onCreate() {
        super.onCreate()

        // 🔥 IMPORTANT: Always initialize notification channels
        NotificationHelper(this).createNotificationChannels()

        Log.d(TAG, "FCM Service Created & Channels Initialized")
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        Log.d(TAG, "FCM Received: data=${remoteMessage.data}")

        // =========================
        // SAFE DATA EXTRACTION
        // =========================

        val data = remoteMessage.data

        val title = data["title"]
            ?: remoteMessage.notification?.title
            ?: "New Message"

        val messageText = data["messageText"]
            ?: remoteMessage.notification?.body
            ?: ""

        val senderName = data["senderName"] ?: "Friend"
        val senderId = data["senderId"] ?: ""
        val roomId = data["roomId"] ?: ""

        // =========================
        // VALIDATION CHECK
        // =========================

        if (roomId.isBlank()) {
            Log.e(TAG, "RoomId missing, notification skipped")
            return
        }

        // =========================
        // SHOW NOTIFICATION
        // =========================

        val notificationHelper = NotificationHelper(this)

        notificationHelper.showMessageNotification(
            title = senderName,
            message = messageText,
            roomId = roomId,
            senderId = senderId
        )

        notificationHelper.showHeadsUpNotification(
            senderName = senderName,
            messageText = messageText,
            roomId = roomId,
            senderId = senderId
        )
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)

        Log.d(TAG, "New FCM Token: $token")

        // TODO: Send token to your server
        // Example:
        // sendTokenToServer(token)
    }

    companion object {
        private const val TAG = "FCMService"
    }
}