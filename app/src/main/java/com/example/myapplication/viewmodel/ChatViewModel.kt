package com.rksaykot.myapplication.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.rksaykot.myapplication.model.Message
import com.rksaykot.myapplication.model.User
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class ChatViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    
    var currentUser by mutableStateOf<User?>(null)
    val users = mutableStateListOf<User>()
    val messages = mutableStateListOf<Message>()
    var connectionStatus by mutableStateOf("")

    init {
        auth.addAuthStateListener { firebaseAuth ->
            val firebaseUser = firebaseAuth.currentUser
            currentUser = if (firebaseUser != null) {
                User(
                    uid = firebaseUser.uid,
                    displayName = firebaseUser.displayName ?: "User",
                    email = firebaseUser.email ?: ""
                )
            } else {
                null
            }
        }
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
                    // Save user to Firestore
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
        db.collection("users").addSnapshotListener { snapshot, e ->
            if (e != null || snapshot == null) return@addSnapshotListener
            users.clear()
            for (doc in snapshot.documents) {
                val user = doc.toObject(User::class.java)
                if (user != null && user.uid != auth.currentUser?.uid) {
                    users.add(user)
                }
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
                        val msg = doc.toObject(Message::class.java)?.copy(
                            id = doc.id,
                            isMe = senderId == auth.currentUser?.uid
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
            .document(messageId).update("isSeen", true)
    }

    fun sendMessage(roomName: String, text: String, replyToId: String? = null) {
        val user = auth.currentUser ?: return
        val messageData = hashMapOf(
            "senderId" to user.uid,
            "senderName" to (user.displayName ?: "Anonymous"),
            "text" to text,
            "timestamp" to Timestamp.now(),
            "isSeen" to false,
            "replyToId" to replyToId
        )
        
        db.collection("rooms").document(roomName).collection("messages")
            .add(messageData)
    }

    fun logout() {
        auth.signOut()
    }
}
