package com.rksaykot.myapplication.model

import com.google.firebase.firestore.PropertyName

data class User(
    val uid: String = "",
    val displayName: String = "",
    val email: String = "",
    val profileImageUrl: String? = null,

    @get:PropertyName("isOnline")
    val isOnline: Boolean = false,

    @get:PropertyName("lastSeen")
    val lastSeen: Long = 0L,

    @get:PropertyName("fcmToken")
    val fcmToken: String? = null,

    val createdAt: Long = System.currentTimeMillis(),
    val bio: String? = null,
    val phoneNumber: String? = null,
    val status: String? = null
)