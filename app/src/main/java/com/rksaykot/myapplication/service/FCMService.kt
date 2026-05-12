package com.rksaykot.myapplication.service

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.rksaykot.myapplication.notification.NotificationHelper

class FCMService : FirebaseMessagingService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        Log.d(TAG, "Message received: ${remoteMessage.notification?.title}")

        // Notification data extract করুন
        val title = remoteMessage.notification?.title ?: "New Message"
        val body = remoteMessage.notification?.body ?: ""
        val senderName = remoteMessage.data["senderName"] ?: "Friend"
        val senderId = remoteMessage.data["senderId"] ?: ""
        val roomId = remoteMessage.data["roomId"] ?: ""
        val messageText = remoteMessage.data["messageText"] ?: body

        // NotificationHelper ব্যবহার করে notification show করুন
        val notificationHelper = NotificationHelper(this)
        notificationHelper.showMessageNotification(senderName, messageText, roomId, senderId)
        notificationHelper.showHeadsUpNotification(senderName, messageText, roomId, senderId)
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "New token: $token")
        // Token কে Firebase এ save করুন (MainActivity তে save হয় already)
    }


    companion object {
        private const val TAG = "FCMService"
    }
}