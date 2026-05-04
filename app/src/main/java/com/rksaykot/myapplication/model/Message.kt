package com.rksaykot.myapplication.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.PropertyName

data class Message(
    val id: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val text: String = "",
    val imageUrl: String? = null,
    val timestamp: Timestamp? = null,
    
    @get:PropertyName("isMe")
    @set:PropertyName("isMe")
    var isMe: Boolean = false,
    
    @get:PropertyName("isDelivered")
    @set:PropertyName("isDelivered")
    var isDelivered: Boolean = false,
    
    @get:PropertyName("isSeen")
    @set:PropertyName("isSeen")
    var isSeen: Boolean = false,

    val replyToId: String? = null,
    val reactions: Map<String, String> = emptyMap()
)
