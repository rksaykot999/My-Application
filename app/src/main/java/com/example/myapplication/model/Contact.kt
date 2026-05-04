package com.rksaykot.myapplication.model

data class Contact(
    val id: String,
    val name: String,
    val lastMessage: String,
    val time: String,
    val unreadCount: Int = 0
)
