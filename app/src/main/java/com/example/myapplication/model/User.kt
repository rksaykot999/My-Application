package com.rksaykot.myapplication.model

data class User(
    val uid: String = "",
    val displayName: String = "",
    val email: String = "",
    val profileImageUrl: String = "",
    val bio: String = "",
    val lastMessage: String = "",
    val lastMessageTime: Long = 0
)
