package com.rksaykot.myapplication.viewmodel

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.google.firebase.messaging.FirebaseMessaging
import com.rksaykot.myapplication.model.Message
import com.rksaykot.myapplication.model.User

class ChatViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val messageListeners = mutableMapOf<String, ListenerRegistration>()
    private val userListeners = mutableMapOf<String, ListenerRegistration>()

    private var isUpdatingSeen = false

    var currentUser by mutableStateOf<User?>(null)
    val contacts = mutableStateListOf<User>()
    val messages = mutableStateListOf<Message>()
    val unreadRooms = mutableStateMapOf<String, Boolean>()
    val lastMessages = mutableStateMapOf<String, String>()
    val lastMessageTimes = mutableStateMapOf<String, Long>()
    val lastMessageSenderIds = mutableStateMapOf<String, String>()

    var typingUser by mutableStateOf<String?>(null)
    var selectedUserStatus by mutableStateOf<User?>(null)
    var activeRoomId by mutableStateOf<String?>(null)

    init {
        auth.addAuthStateListener { firebaseAuth ->
            val firebaseUser = firebaseAuth.currentUser
            if (firebaseUser != null) {
                listenToCurrentUser(firebaseUser.uid)
                setUserOnline(true)
                fetchMyContacts(firebaseUser.uid) // Only fetch added contacts
                listenToAllUnreadMessages(firebaseUser.uid)

                FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        updateFcmToken(task.result)
                    }
                }
            } else {
                stopAllListeners()
                currentUser = null
            }
        }
    }

    private fun fetchMyContacts(myUid: String) {
        db.collection("users").document(myUid).collection("contacts")
            .addSnapshotListener { snapshot, e ->
                if (e != null) return@addSnapshotListener

                val contactIds = snapshot?.documents?.map { it.id } ?: emptyList()
                if (contactIds.isEmpty()) {
                    contacts.clear()
                    return@addSnapshotListener
                }

                // Fetch user details for each contact ID in our friend list
                db.collection("users")
                    .whereIn("uid", contactIds)
                    .addSnapshotListener { userSnapshot, _ ->
                        val userList = userSnapshot?.documents?.mapNotNull { it.toObject(User::class.java) } ?: emptyList()
                        contacts.clear()
                        contacts.addAll(userList)
                    }
            }
    }

    fun login(email: String, password: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        if (email.isEmpty() || password.isEmpty()) {
            onError("Please fill in all fields")
            return
        }
        auth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener {
                setUserOnline(true)
                onSuccess()
            }
            .addOnFailureListener { onError(it.message ?: "Login failed") }
    }

    fun signUp(user: com.rksaykot.myapplication.model.User, password: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        if (user.email.isEmpty() || password.isEmpty() || user.displayName.isEmpty()) {
            onError("Please fill in all fields")
            return
        }

        auth.createUserWithEmailAndPassword(user.email, password)
            .addOnSuccessListener { authResult ->
                val uid = authResult.user?.uid ?: ""
                val finalUser = user.copy(uid = uid, createdAt = System.currentTimeMillis())

                // Save user profile to Firestore
                db.collection("users").document(uid).set(finalUser)
                    .addOnSuccessListener {
                        setUserOnline(true)
                        onSuccess()
                    }
                    .addOnFailureListener { onError("Auth success, but profile creation failed.") }
            }
            .addOnFailureListener { onError(it.message ?: "Sign up failed") }
    }

    private fun listenToCurrentUser(uid: String) {
        userListeners["current_user"]?.remove()
        val listener = db.collection("users")
            .document(uid)
            .addSnapshotListener { snapshot, e ->
                if (e != null) return@addSnapshotListener
                val updated = snapshot?.toObject(User::class.java)
                if (updated != null) {
                    currentUser = updated
                }
            }
        userListeners["current_user"] = listener
    }

    fun setUserOnline(isOnline: Boolean) {
        val uid = auth.currentUser?.uid ?: return
        db.collection("users")
            .document(uid)
            .update(
                "isOnline", isOnline,
                "lastSeen", System.currentTimeMillis()
            )
    }

    fun updateFcmToken(token: String) {
        val uid = auth.currentUser?.uid ?: return
        db.collection("users").document(uid).update("fcmToken", token)
    }

    private fun listenToAllUnreadMessages(myUid: String) {
        db.collection("rooms").addSnapshotListener { snapshot, _ ->
            snapshot?.documents?.forEach { roomDoc ->
                val roomId = roomDoc.id
                if (roomId.contains(myUid)) {
                    // Track last message and time
                    lastMessages[roomId] = roomDoc.getString("lastMessage") ?: ""
                    lastMessageTimes[roomId] = roomDoc.getTimestamp("lastMessageTime")?.toDate()?.time ?: 0L
                    lastMessageSenderIds[roomId] = roomDoc.getString("lastMessageSenderId") ?: ""

                    roomDoc.reference.collection("messages")
                        .whereEqualTo("isSeen", false)
                        .addSnapshotListener { msgSnap, _ ->
                            val hasUnread = msgSnap?.documents?.any {
                                it.getString("senderId") != myUid
                            } ?: false
                            unreadRooms[roomId] = hasUnread
                        }
                }
            }
        }
    }

    fun listenToMessages(roomName: String) {
        messageListeners[roomName]?.remove()
        val myUid = auth.currentUser?.uid ?: ""
        val peerUid = roomName.split("_").find { it != myUid }

        if (peerUid != null) {
            val userListener = db.collection("users").document(peerUid)
                .addSnapshotListener { snapshot, _ ->
                    selectedUserStatus = snapshot?.toObject(User::class.java)
                }
            userListeners[roomName] = userListener
        }

        val listener = db.collection("rooms").document(roomName).collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null || snapshot == null) return@addSnapshotListener

                val newList = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(Message::class.java)?.copy(
                        id = doc.id,
                        isMe = doc.getString("senderId") == myUid
                    )
                }
                messages.clear()
                messages.addAll(newList)

                db.collection("rooms").document(roomName).addSnapshotListener { roomSnap, _ ->
                    val typingMap = roomSnap?.get("typing") as? Map<String, Boolean>
                    typingUser = typingMap?.filter { it.key != myUid && it.value }?.keys?.firstOrNull()
                }

                val unreadIds = snapshot.documents.filter {
                    it.getString("senderId") != myUid && it.getBoolean("isSeen") != true
                }.map { it.id }

                if (unreadIds.isNotEmpty() && roomName == activeRoomId) {
                    markMessagesAsSeenBatch(roomName, unreadIds)
                }
            }
        messageListeners[roomName] = listener
    }

    private fun markMessagesAsSeenBatch(roomName: String, messageIds: List<String>) {
        if (isUpdatingSeen) return
        isUpdatingSeen = true
        val batch = db.batch()
        messageIds.forEach { id ->
            val docRef = db.collection("rooms").document(roomName).collection("messages").document(id)
            batch.update(docRef, "isSeen", true)
        }
        batch.commit().addOnCompleteListener { isUpdatingSeen = false }
    }

    fun sendMessage(roomName: String, text: String, imageUrl: String? = null, replyToId: String? = null) {
        val user = auth.currentUser ?: return
        val now = Timestamp.now()
        val messageData = hashMapOf(
            "senderId" to user.uid,
            "senderName" to (currentUser?.displayName ?: "User"),
            "text" to text,
            "imageUrl" to imageUrl,
            "timestamp" to now,
            "isSeen" to false,
            "replyToId" to replyToId
        )

        db.collection("rooms").document(roomName).collection("messages").add(messageData)
            .addOnSuccessListener {
                db.collection("rooms").document(roomName).set(
                    mapOf(
                        "lastMessage" to (imageUrl?.let { "📷 Image" } ?: text),
                        "lastMessageTime" to now,
                        "lastMessageSenderId" to user.uid
                    ), SetOptions.merge()
                )
            }
    }

    fun setTypingStatus(roomId: String, isTyping: Boolean) {
        val uid = auth.currentUser?.uid ?: return
        db.collection("rooms").document(roomId).update("typing.$uid", isTyping)
    }

    fun clearChatHistory(roomId: String) {
        db.collection("rooms").document(roomId).collection("messages").get()
            .addOnSuccessListener { snapshot ->
                val batch = db.batch()
                snapshot.documents.forEach { batch.delete(it.reference) }
                batch.commit()
            }
    }

    fun logout() {
        setUserOnline(false)
        auth.signOut()
    }

    private fun stopAllListeners() {
        messageListeners.values.forEach { it.remove() }
        messageListeners.clear()
        userListeners.values.forEach { it.remove() }
        userListeners.clear()
    }

    fun stopListeningToMessages() {
        messageListeners.values.forEach { it.remove() }
        messageListeners.clear()
    }

    fun addReaction(roomId: String, messageId: String, emoji: String) {
        val uid = auth.currentUser?.uid ?: return
        db.collection("rooms").document(roomId).collection("messages").document(messageId)
            .update("reactions.$uid", emoji)
    }

    fun removeReaction(roomId: String, messageId: String) {
        val uid = auth.currentUser?.uid ?: return
        db.collection("rooms").document(roomId).collection("messages").document(messageId)
            .update("reactions.$uid", FieldValue.delete())
    }

    fun editMessage(roomId: String, messageId: String, newText: String) {
        db.collection("rooms").document(roomId).collection("messages").document(messageId)
            .update("text", newText, "isEdited", true)
    }

    fun deleteMessage(roomId: String, messageId: String) {
        db.collection("rooms").document(roomId).collection("messages").document(messageId)
            .delete()
    }

    fun fetchUserInfo(uid: String) {
        db.collection("users").document(uid).get().addOnSuccessListener { snapshot ->
            selectedUserStatus = snapshot.toObject(User::class.java)
        }
    }

    fun updateDisplayName(newName: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        val uid = auth.currentUser?.uid ?: return
        db.collection("users").document(uid).update("displayName", newName)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onError(it.message ?: "Failed to update") }
    }

    fun deleteAccount(onSuccess: () -> Unit, onError: (String) -> Unit) {
        val user = auth.currentUser ?: return
        val uid = user.uid
        user.delete().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                db.collection("users").document(uid).delete()
                onSuccess()
            } else {
                onError(task.exception?.message ?: "Authentication failed. Please re-login and try again.")
            }
        }
    }

    fun addFriend(query: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        val myUid = auth.currentUser?.uid ?: return

        // Search by email
        db.collection("users")
            .whereEqualTo("email", query.trim())
            .get()
            .addOnSuccessListener { snapshot ->
                val friendDoc = snapshot.documents.firstOrNull()
                if (friendDoc == null) {
                    // Try searching by phone number if email fails
                    db.collection("users").whereEqualTo("phoneNumber", query.trim()).get()
                        .addOnSuccessListener { phoneSnap ->
                            val phoneFriend = phoneSnap.documents.firstOrNull()
                            if (phoneFriend != null) {
                                performAddFriend(myUid, phoneFriend.id, onSuccess, onError)
                            } else {
                                onError("User not found.")
                            }
                        }
                } else {
                    performAddFriend(myUid, friendDoc.id, onSuccess, onError)
                }
            }
            .addOnFailureListener { onError(it.message ?: "Error searching user") }
    }

    private fun performAddFriend(myUid: String, friendUid: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        if (myUid == friendUid) {
            onError("You cannot add yourself!")
            return
        }

        val timestamp = FieldValue.serverTimestamp()

        // Add to current user's contact sub-collection
        db.collection("users").document(myUid).collection("contacts").document(friendUid)
            .set(mapOf("addedAt" to timestamp))
            .addOnSuccessListener {
                // Also add current user to friend's contacts (mutual connection)
                db.collection("users").document(friendUid).collection("contacts").document(myUid)
                    .set(mapOf("addedAt" to timestamp))
                    .addOnSuccessListener { onSuccess() }
            }
            .addOnFailureListener { onError("Failed to add contact.") }
    }
}