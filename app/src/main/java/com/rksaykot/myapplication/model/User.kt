package com.rksaykot.myapplication.model

import com.google.firebase.firestore.PropertyName
import java.io.Serializable

data class User(

    val uid: String = "",

    val displayName: String = "",

    val email: String = "",

    val profileImageUrl: String = "",

    @get:PropertyName("isOnline")
    @set:PropertyName("isOnline")
    var isOnline: Boolean = false,

    @get:PropertyName("lastSeen")
    @set:PropertyName("lastSeen")
    var lastSeen: Long = 0L,

    @get:PropertyName("fcmToken")
    @set:PropertyName("fcmToken")
    var fcmToken: String = "",

    val createdAt: Long = System.currentTimeMillis(),

    val bio: String = "",

    val phoneNumber: String = "",

    val status: String = "Hey there! I am using RK Chat.",

    @get:PropertyName("lastMessage")
    @set:PropertyName("lastMessage")
    var lastMessage: String = "",

    @get:PropertyName("lastMessageTime")
    @set:PropertyName("lastMessageTime")
    var lastMessageTime: Long = 0L,

    val typing: Boolean = false

) : Serializable