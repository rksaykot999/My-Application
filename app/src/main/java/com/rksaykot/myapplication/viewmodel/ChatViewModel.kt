package com.rksaykot.myapplication.viewmodel

import android.net.Uri
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
import com.google.firebase.storage.FirebaseStorage
import com.rksaykot.myapplication.MyApplication
import com.rksaykot.myapplication.model.Message
import com.rksaykot.myapplication.model.User
import java.util.UUID

class ChatViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val storage = FirebaseStorage.getInstance()

    private val messageListeners = mutableMapOf<String, ListenerRegistration>()
    private val userListeners = mutableMapOf<String, ListenerRegistration>()

    var currentUser by mutableStateOf<User?>(null)
    val users = mutableStateListOf<User>()
    val messages = mutableStateListOf<Message>()
    val lastMessages = mutableStateMapOf<String, String>()
    val unreadRooms = mutableStateMapOf<String, Boolean>()
    var connectionStatus by mutableStateOf("")
    var typingUser by mutableStateOf<String?>(null)
    var selectedUserStatus by mutableStateOf<User?>(null)
    var activeRoomId by mutableStateOf<String?>(null)

    init {
        auth.addAuthStateListener { firebaseAuth ->
            val firebaseUser = firebaseAuth.currentUser
            currentUser = if (firebaseUser != null) {
                val user = User(
                    uid = firebaseUser.uid,
                    displayName = firebaseUser.displayName ?: "User",
                    email = firebaseUser.email ?: ""
                )
                saveUserToFirestore(user)
                setUserOnline(true)

                FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        updateFcmToken(task.result)
                    }
                }
                user
            } else {
                null
            }
        }
    }

    private fun saveUserToFirestore(user: User) {
        db.collection("users").document(user.uid).set(user, SetOptions.merge())
    }

    fun setUserOnline(isOnline: Boolean) {
        val uid = auth.currentUser?.uid ?: return
        db.collection("users").document(uid).update(
            "isOnline", isOnline,
            "lastSeen", System.currentTimeMillis()
        ).addOnFailureListener { e ->
            Log.e(TAG, "Failed to update online status", e)
        }
    }

    fun updateFcmToken(token: String) {
        val uid = auth.currentUser?.uid ?: return
        db.collection("users").document(uid).update("fcmToken", token)
    }

    fun login(email: String, pass: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        if (email.isBlank() || pass.isBlank()) {
            onError("Email and Password cannot be empty")
            return
        }
        auth.signInWithEmailAndPassword(email, pass)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onError(it.message ?: "Login Failed") }
    }

    fun signUp(email: String, pass: String, name: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        auth.createUserWithEmailAndPassword(email, pass)
            .addOnSuccessListener { result ->
                val user = result.user
                val profileUpdates = com.google.firebase.auth.userProfileChangeRequest {
                    displayName = name
                }
                user?.updateProfile(profileUpdates)?.addOnCompleteListener {
                    val userData = User(
                        uid = user.uid,
                        displayName = name,
                        email = email
                    )
                    db.collection("users").document(user.uid).set(userData)
                        .addOnSuccessListener { onSuccess() }
                        .addOnFailureListener { onError(it.message ?: "Failed to save user data") }
                }
            }
            .addOnFailureListener { onError(it.message ?: "Sign Up Failed") }
    }

    fun fetchAllUsers() {
        val myUid = auth.currentUser?.uid
        val listener = db.collection("users").addSnapshotListener { snapshot, e ->
            if (e != null || snapshot == null) return@addSnapshotListener
            users.clear()
            for (doc in snapshot.documents) {
                val user = doc.toObject(User::class.java)
                if (user != null && user.uid != myUid) {
                    users.add(user)
                    val peerUid = user.uid
                    if (myUid != null) {
                        val roomId = if (myUid < peerUid) "${myUid}_${peerUid}" else "${peerUid}_${myUid}"
                        listenToRoomMetadata(roomId)
                    }
                }
            }
        }
        userListeners["all_users"] = listener
    }

    private fun listenToRoomMetadata(roomId: String) {
        db.collection("rooms").document(roomId).addSnapshotListener { snapshot, _ ->
            val lastMsg = snapshot?.getString("lastMessage")
            val senderId = snapshot?.getString("lastMessageSenderId")
            val isSeen = snapshot?.getBoolean("isLastMessageSeen") ?: true

            if (lastMsg != null) {
                lastMessages[roomId] = lastMsg
            }
            
            // If the last message was NOT sent by me and hasn't been seen, mark room as unread
            if (senderId != null && senderId != auth.currentUser?.uid && !isSeen) {
                unreadRooms[roomId] = true
            } else {
                unreadRooms[roomId] = false
            }
        }
    }

    fun findUserByEmail(email: String, onSuccess: (User) -> Unit, onError: (String) -> Unit) {
        db.collection("users")
            .whereEqualTo("email", email)
            .get()
            .addOnSuccessListener { snapshot ->
                val user = snapshot.documents.firstOrNull()?.toObject(User::class.java)
                if (user != null) onSuccess(user)
                else onError("User not found")
            }
            .addOnFailureListener { onError(it.message ?: "Error finding user") }
    }

    fun listenToMessages(roomName: String) {
        messageListeners[roomName]?.remove()

        val myUid = auth.currentUser?.uid ?: ""
        val peerUid = roomName.split("_").find { it != myUid }

        if (peerUid != null) {
            fetchUserInfo(peerUid)
            val userListener = db.collection("users").document(peerUid)
                .addSnapshotListener { snapshot, _ ->
                    selectedUserStatus = snapshot?.toObject(User::class.java)
                }
            userListeners[roomName] = userListener
        }

        val listener = db.collection("rooms").document(roomName).collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    connectionStatus = "Error: ${e.message}"
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    messages.clear()
                    for (doc in snapshot.documents) {
                        val senderId = doc.getString("senderId")
                        val seen = doc.getBoolean("isSeen") ?: false
                        val delivered = doc.getBoolean("isDelivered") ?: false

                        val msg = doc.toObject(Message::class.java)?.copy(
                            id = doc.id,
                            isMe = senderId == myUid,
                            isSeen = seen,
                            isDelivered = delivered
                        )
                        if (msg != null) {
                            messages.add(msg)
                            if (senderId != myUid && !msg.isSeen && roomName == activeRoomId) {
                                markMessageAsSeen(roomName, doc.id)
                                // Cancel notification when entering chat
                                MyApplication.notificationHelper.cancelNotification(roomName)
                            } else if (senderId != myUid && !msg.isSeen) {
                                showLocalMessageNotification(msg, roomName, peerUid ?: "")
                            }
                        }
                    }
                    connectionStatus = "Connected"
                }
            }
        messageListeners[roomName] = listener

        db.collection("rooms").document(roomName).addSnapshotListener { snapshot, _ ->
            val typingMap = snapshot?.get("typing") as? Map<String, Boolean>
            typingUser = typingMap?.filter { it.value && it.key != myUid }
                ?.keys?.firstOrNull()?.let { "Someone" }
        }
    }

    private fun showLocalMessageNotification(message: Message, roomId: String, senderId: String) {
        val senderName = message.senderName ?: "Friend"
        MyApplication.notificationHelper.showMessageNotification(senderName, message.text ?: "New message", roomId, senderId)
    }

    fun stopListeningToMessages() {
        messageListeners.values.forEach { it.remove() }
        messageListeners.clear()
        messages.clear()
    }

    private fun markMessageAsSeen(roomName: String, messageId: String) {
        db.collection("rooms").document(roomName).collection("messages")
            .document(messageId).update("isSeen", true, "isDelivered", true)
            
        // Also update room metadata to reflect that the last message is seen
        db.collection("rooms").document(roomName).update("isLastMessageSeen", true)
    }

    fun fetchUserInfo(uid: String) {
        db.collection("users").document(uid).get()
            .addOnSuccessListener { snapshot ->
                val user = snapshot.toObject(User::class.java)
                if (user != null) {
                    selectedUserStatus = user
                }
            }
    }

    fun updateDisplayName(newName: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        val user = auth.currentUser ?: return
        val profileUpdates = com.google.firebase.auth.userProfileChangeRequest {
            displayName = newName
        }
        user.updateProfile(profileUpdates).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                db.collection("users").document(user.uid).update("displayName", newName)
                    .addOnSuccessListener {
                        currentUser = currentUser?.copy(displayName = newName)
                        onSuccess()
                    }
                    .addOnFailureListener { onError(it.message ?: "Failed to update Firestore") }
            } else {
                onError(task.exception?.message ?: "Failed to update profile")
            }
        }
    }

    fun uploadImage(uri: Uri, path: String, onSuccess: (String) -> Unit, onError: (String) -> Unit) {
        val fileName = UUID.randomUUID().toString()
        val ref = storage.reference.child(path).child(fileName)

        ref.putFile(uri)
            .continueWithTask { task ->
                if (!task.isSuccessful) {
                    task.exception?.let { throw it }
                }
                ref.downloadUrl
            }
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    onSuccess(task.result.toString())
                } else {
                    onError(task.exception?.message ?: "Upload failed")
                }
            }
    }

    fun updateProfileImage(imageUrl: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        val uid = auth.currentUser?.uid ?: return
        db.collection("users").document(uid).update("profileImageUrl", imageUrl)
            .addOnSuccessListener {
                currentUser = currentUser?.copy(profileImageUrl = imageUrl)
                onSuccess()
            }
            .addOnFailureListener { onError(it.message ?: "Failed to update profile image") }
    }

    fun sendMessage(roomName: String, text: String, imageUrl: String? = null, replyToId: String? = null) {
        val user = auth.currentUser ?: return
        val now = Timestamp.now()
        val messageData = hashMapOf(
            "senderId" to user.uid,
            "senderName" to (user.displayName ?: "Anonymous"),
            "text" to text,
            "imageUrl" to imageUrl,
            "timestamp" to now,
            "isSeen" to false,
            "isDelivered" to false,
            "replyToId" to replyToId,
            "isEdited" to false
        )

        db.collection("rooms").document(roomName).collection("messages").add(messageData)
            .addOnSuccessListener {
                sendMessageNotification(roomName, user.displayName ?: "Someone", text, imageUrl)
            }

        val lastMsgText = if (imageUrl != null && text.isEmpty()) "Sent an image" else text
        db.collection("rooms").document(roomName).set(
            mapOf(
                "lastMessage" to lastMsgText,
                "lastMessageTime" to now,
                "lastMessageSenderId" to user.uid,
                "isLastMessageSeen" to false
            ),
            SetOptions.merge()
        )

        db.collection("users").document(user.uid).update(
            "lastMessage", lastMsgText,
            "lastMessageTime", now.seconds * 1000
        )
    }

    fun editMessage(roomName: String, messageId: String, newText: String) {
        db.collection("rooms").document(roomName).collection("messages")
            .document(messageId).update("text", newText, "isEdited", true)
    }

    fun deleteMessage(roomName: String, messageId: String) {
        db.collection("rooms").document(roomName).collection("messages")
            .document(messageId).delete()
    }

    fun addReaction(roomName: String, messageId: String, emoji: String) {
        val userId = auth.currentUser?.uid ?: return
        db.collection("rooms").document(roomName).collection("messages")
            .document(messageId).update("reactions.$userId", emoji)
    }

    fun removeReaction(roomName: String, messageId: String) {
        val userId = auth.currentUser?.uid ?: return
        db.collection("rooms").document(roomName).collection("messages")
            .document(messageId).update("reactions.$userId", FieldValue.delete())
    }

    fun setTypingStatus(roomName: String, isTyping: Boolean) {
        val uid = auth.currentUser?.uid ?: return
        db.collection("rooms").document(roomName).update("typing.$uid", isTyping)
    }

    fun clearChatHistory(roomName: String) {
        db.collection("rooms").document(roomName).collection("messages")
            .get()
            .addOnSuccessListener { snapshot ->
                val batch = db.batch()
                for (doc in snapshot.documents) {
                    batch.delete(doc.reference)
                }
                batch.commit()
            }
        db.collection("rooms").document(roomName).update("lastMessage", "")
    }

    private fun sendMessageNotification(roomName: String, senderName: String, messageText: String, imageUrl: String?) {
        val myUid = auth.currentUser?.uid ?: return
        val peerUid = roomName.split("_").find { it != myUid } ?: return

        db.collection("users").document(peerUid).get()
            .addOnSuccessListener { document ->
                val fcmToken = document.getString("fcmToken") ?: return@addOnSuccessListener
                val notificationMessage = if (imageUrl != null) "Sent an image" else messageText.take(100)
                sendFCMNotification(fcmToken, senderName, notificationMessage)
            }
    }

    private fun sendFCMNotification(token: String, title: String, message: String) {
        db.collection("notifications").add(mapOf(
            "token" to token,
            "title" to title,
            "body" to message,
            "timestamp" to Timestamp.now(),
            "sent" to false
        ))
    }

    fun logout() {
        setUserOnline(false)
        messageListeners.values.forEach { it.remove() }
        userListeners.values.forEach { it.remove() }
        auth.signOut()
    }

    companion object {
        private const val TAG = "ChatViewModel"
    }
}
