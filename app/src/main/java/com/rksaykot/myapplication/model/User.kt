package com.rksaykot.myapplication.model

import com.google.firebase.firestore.PropertyName
import java.io.Serializable

/**
 * Professional User Data Model for Chat Application.
 */
data class User(
    // Core Identity
    val uid: String = "",
    val displayName: String = "",
    val username: String = "", // Unique handle (e.g., @rksaykot)
    val email: String = "",
    val profileImageUrl: String = "",
    val coverImageUrl: String = "", // For professional profile look

    // Contact & Identity
    val phoneNumber: String = "",
    val gender: String = "", // Male, Female, Other, Secret
    val dateOfBirth: Long = 0L,
    val location: String = "", // City, Country

    // Security & Auth
    val password: String = "",
    @get:PropertyName("isVerified")
    @set:PropertyName("isVerified")
    var isVerified: Boolean = false, // For blue tick/official badges

    // Real-time Status and Activity
    @get:PropertyName("isOnline")
    @set:PropertyName("isOnline")
    var isOnline: Boolean = false,

    @get:PropertyName("lastSeen")
    @set:PropertyName("lastSeen")
    var lastSeen: Long = 0L,

    @get:PropertyName("fcmToken")
    @set:PropertyName("fcmToken")
    var fcmToken: String = "",

    @get:PropertyName("typing")
    @set:PropertyName("typing")
    var typing: Boolean = false,

    // Messaging Metadata (for Chat List optimization)
    @get:PropertyName("lastMessage")
    @set:PropertyName("lastMessage")
    var lastMessage: String = "",

    @get:PropertyName("lastMessageTime")
    @set:PropertyName("lastMessageTime")
    var lastMessageTime: Long = 0L,

    @get:PropertyName("unreadCount")
    @set:PropertyName("unreadCount")
    var unreadCount: Int = 0,

    // Account Management
    val createdAt: Long = System.currentTimeMillis(),
    val accountStatus: String = "active", // active, suspended, deactivated

    // Privacy Settings
    @get:PropertyName("showLastSeen")
    @set:PropertyName("showLastSeen")
    var showLastSeen: Boolean = true,

    @get:PropertyName("showOnlineStatus")
    @set:PropertyName("showOnlineStatus")
    var showOnlineStatus: Boolean = true,

    @get:PropertyName("isPrivateAccount")
    @set:PropertyName("isPrivateAccount")
    var isPrivateAccount: Boolean = false,

    // Extra Personalization
    val website: String = "",
    val customStatus: String = "" // Custom text status like "At the Gym"
) : Serializable {

    // Helper function to get initials for profile placeholder
    fun getInitials(): String {
        if (displayName.isBlank()) return "?"
        val parts = displayName.trim().split(" ")
        return if (parts.size > 1) {
            "${parts[0][0]}${parts[1][0]}".uppercase()
        } else {
            parts[0].take(2).uppercase()
        }
    }
}