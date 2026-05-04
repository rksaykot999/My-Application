package com.rksaykot.myapplication.model

import com.google.firebase.Timestamp

data class Message(
    val id: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val text: String = "",
    val timestamp: Timestamp? = null,
    val isMe: Boolean = false,
    val isDelivered: Boolean = false,
    val isSeen: Boolean = false,
    val replyToId: String? = null,
    val reactions: Map<String, String> = emptyMap()
)
