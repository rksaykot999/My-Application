package com.rksaykot.myapplication.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.rksaykot.myapplication.model.Message
import com.rksaykot.myapplication.model.User
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import android.net.Uri
import java.util.UUID

class ChatViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val storage = FirebaseStorage.getInstance()
    
    var currentUser by mutableStateOf<User?>(null)
    val users = mutableStateListOf<User>()
    val messages = mutableStateListOf<Message>()
    val lastMessages = mutableStateMapOf<String, String>() // roomId -> lastMessage
    var connectionStatus by mutableStateOf("")
    var selectedUserStatus by mutableStateOf<User?>(null)

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
                user
            } else {
                null
            }
        }
    }

    private fun saveUserToFirestore(user: User) {
        db.collection("users").document(user.uid).set(user, com.google.firebase.firestore.SetOptions.merge())
    }

    fun setUserOnline(isOnline: Boolean) {
        val uid = auth.currentUser?.uid ?: return
        db.collection("users").document(uid).update(
            "isOnline", isOnline,
            "lastSeen", System.currentTimeMillis()
        )
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
        val myUid = auth.currentUser?.uid ?: return
        db.collection("users").addSnapshotListener { snapshot, e ->
            if (e != null || snapshot == null) return@addSnapshotListener
            users.clear()
            for (doc in snapshot.documents) {
                val user = doc.toObject(User::class.java)
                if (user != null && user.uid != myUid) {
                    users.add(user)
                    // Listen to last message for this specific chat
                    val peerUid = user.uid
                    val roomId = if (myUid < peerUid) "${myUid}_${peerUid}" else "${peerUid}_${myUid}"
                    listenToRoomMetadata(roomId)
                }
            }
        }
    }

    private fun listenToRoomMetadata(roomId: String) {
        db.collection("rooms").document(roomId).addSnapshotListener { snapshot, _ ->
            val lastMsg = snapshot?.getString("lastMessage")
            if (lastMsg != null) {
                lastMessages[roomId] = lastMsg
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
        // Find peer UID from room name (format: uid1_uid2)
        val myUid = auth.currentUser?.uid ?: ""
        val peerUid = roomName.split("_").find { it != myUid }
        
        if (peerUid != null) {
            db.collection("users").document(peerUid)
                .addSnapshotListener { snapshot, _ ->
                    selectedUserStatus = snapshot?.toObject(User::class.java)
                }
        }

        db.collection("rooms").document(roomName).collection("messages")
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
                            isMe = senderId == auth.currentUser?.uid,
                            isSeen = seen,
                            isDelivered = delivered
                        )
                        if (msg != null) {
                            messages.add(msg)
                            if (senderId != auth.currentUser?.uid && !msg.isSeen) {
                                markMessageAsSeen(roomName, doc.id)
                            }
                        }
                    }
                }
            }
    }

    private fun markMessageAsSeen(roomName: String, messageId: String) {
        db.collection("rooms").document(roomName).collection("messages")
            .document(messageId).update("isSeen", true, "isDelivered", true)
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
                    val downloadUri = task.result
                    onSuccess(downloadUri.toString())
                } else {
                    onError(task.exception?.message ?: "Upload failed: Object could not be created")
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
            "replyToId" to replyToId
        )
        
        db.collection("rooms").document(roomName).collection("messages")
            .add(messageData)

        // Update room metadata for last message
        val lastMsgText = if (imageUrl != null && text.isEmpty()) "Sent an image" else text
        db.collection("rooms").document(roomName).set(
            mapOf(
                "lastMessage" to lastMsgText,
                "lastMessageTime" to now
            ),
            com.google.firebase.firestore.SetOptions.merge()
        )

        db.collection("users").document(user.uid).update(
            "lastMessage", lastMsgText,
            "lastMessageTime", now.seconds * 1000
        )
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

    fun logout() {
        setUserOnline(false)
        auth.signOut()
    }
}
