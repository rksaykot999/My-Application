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

    private val messageListeners =
        mutableMapOf<String, ListenerRegistration>()

    private val userListeners =
        mutableMapOf<String, ListenerRegistration>()

    private var isUpdatingSeen = false

    var currentUser by mutableStateOf<User?>(null)

    val users = mutableStateListOf<User>()

    val messages = mutableStateListOf<Message>()

    val lastMessages =
        mutableStateMapOf<String, String>()

    val unreadRooms =
        mutableStateMapOf<String, Boolean>()

    var connectionStatus by mutableStateOf("")

    var typingUser by mutableStateOf<String?>(null)

    var selectedUserStatus by mutableStateOf<User?>(null)

    var activeRoomId by mutableStateOf<String?>(null)

    init {

        auth.addAuthStateListener { firebaseAuth ->
            val firebaseUser = firebaseAuth.currentUser

            if (firebaseUser != null) {
                val user = User(
                    uid = firebaseUser.uid,
                    displayName = firebaseUser.displayName ?: "User",
                    email = firebaseUser.email ?: ""
                )

                // ensure document exists and mark online
                saveUserToFirestore(user)
                setUserOnline(true)

                // update local state and start listening for realtime updates
                currentUser = user
                listenToCurrentUser(firebaseUser.uid)

                FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        updateFcmToken(task.result)
                    }
                }
            } else {
                // user signed out: stop listener and clear
                userListeners["current_user"]?.remove()
                userListeners.remove("current_user")
                currentUser = null
            }
        }
    }

    private fun saveUserToFirestore(user: User) {

        db.collection("users")
            .document(user.uid)
            .set(user, SetOptions.merge())
    }

    // Keep a realtime listener on the current user's document so UI shows up-to-date online/profile info
    private fun listenToCurrentUser(uid: String) {
        try {
            userListeners["current_user"]?.remove()
        } catch (_: Exception) {}

        val listener = db.collection("users")
            .document(uid)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.e(TAG, "Current user listener error", e)
                    return@addSnapshotListener
                }

                val updated = snapshot?.toObject(User::class.java)
                if (updated != null) {
                    currentUser = updated
                }
            }

        userListeners["current_user"] = listener
    }

    fun setUserOnline(isOnline: Boolean) {
        val uid = auth.currentUser?.uid ?: return

        // Update local cached user immediately so UI reflects change without waiting for Firestore
        try {
            val now = System.currentTimeMillis()
            currentUser = currentUser?.copy(isOnline = isOnline, lastSeen = now)
        } catch (_: Exception) {}

        db.collection("users")
            .document(uid)
            .update(
                "isOnline", isOnline,
                "lastSeen", System.currentTimeMillis()
            )
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to update online status", e)
            }
    }

    fun updateFcmToken(token: String) {

        val uid = auth.currentUser?.uid ?: return

        db.collection("users")
            .document(uid)
            .update("fcmToken", token)
    }

    fun login(
        email: String,
        pass: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {

        if (email.isBlank() || pass.isBlank()) {

            onError("Email and Password cannot be empty")

            return
        }

        auth.signInWithEmailAndPassword(email, pass)
            .addOnSuccessListener {
                onSuccess()
            }
            .addOnFailureListener {
                onError(it.message ?: "Login Failed")
            }
    }

    fun signUp(
        email: String,
        pass: String,
        name: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {

        auth.createUserWithEmailAndPassword(email, pass)
            .addOnSuccessListener { result ->

                val user = result.user

                val profileUpdates =
                    com.google.firebase.auth.userProfileChangeRequest {
                        displayName = name
                    }

                user?.updateProfile(profileUpdates)
                    ?.addOnCompleteListener {

                        val userData = User(
                            uid = user.uid,
                            displayName = name,
                            email = email
                        )

                        db.collection("users")
                            .document(user.uid)
                            .set(userData)
                            .addOnSuccessListener {
                                onSuccess()
                            }
                            .addOnFailureListener {
                                onError(
                                    it.message
                                        ?: "Failed to save user data"
                                )
                            }
                    }
            }
            .addOnFailureListener {
                onError(it.message ?: "Sign Up Failed")
            }
    }

    fun fetchAllUsers() {

        val myUid = auth.currentUser?.uid

        val listener =
            db.collection("users")
                .addSnapshotListener { snapshot, e ->

                    if (e != null || snapshot == null) {
                        return@addSnapshotListener
                    }

                    users.clear()

                    for (doc in snapshot.documents) {

                        val user =
                            doc.toObject(User::class.java)

                        if (
                            user != null &&
                            user.uid != myUid
                        ) {

                            users.add(user)

                            val peerUid = user.uid

                            if (myUid != null) {

                                val roomId =
                                    if (myUid < peerUid)
                                        "${myUid}_${peerUid}"
                                    else
                                        "${peerUid}_${myUid}"

                                listenToRoomMetadata(roomId)
                            }
                        }
                    }
                }

        userListeners["all_users"] = listener
    }

    private fun listenToRoomMetadata(roomId: String) {

        db.collection("rooms")
            .document(roomId)
            .addSnapshotListener { snapshot, _ ->

                val lastMsg =
                    snapshot?.getString("lastMessage")

                val senderId =
                    snapshot?.getString(
                        "lastMessageSenderId"
                    )

                val isSeen =
                    snapshot?.getBoolean(
                        "isLastMessageSeen"
                    ) ?: true

                if (lastMsg != null) {
                    lastMessages[roomId] = lastMsg
                }

                unreadRooms[roomId] =
                    senderId != auth.currentUser?.uid && !isSeen
            }
    }

    fun listenToMessages(roomName: String) {

        messageListeners[roomName]?.remove()

        val myUid =
            auth.currentUser?.uid ?: ""

        val peerUid =
            roomName.split("_").find { it != myUid }

        if (peerUid != null) {

            fetchUserInfo(peerUid)

            val userListener =
                db.collection("users")
                    .document(peerUid)
                    .addSnapshotListener { snapshot, _ ->

                        selectedUserStatus =
                            snapshot?.toObject(
                                User::class.java
                            )
                    }

            userListeners[roomName] = userListener
        }

        val listener =
            db.collection("rooms")
                .document(roomName)
                .collection("messages")
                .orderBy(
                    "timestamp",
                    Query.Direction.ASCENDING
                )
                .addSnapshotListener { snapshot, e ->

                    if (e != null) {

                        connectionStatus =
                            "Error: ${e.message}"

                        return@addSnapshotListener
                    }

                    if (snapshot != null) {

                        val newList =
                            snapshot.documents.mapNotNull { doc ->

                                val senderId =
                                    doc.getString("senderId")

                                val seen =
                                    doc.getBoolean(
                                        "isSeen"
                                    ) ?: false

                                val delivered =
                                    doc.getBoolean(
                                        "isDelivered"
                                    ) ?: false

                                doc.toObject(
                                    Message::class.java
                                )?.copy(
                                    id = doc.id,
                                    isMe = senderId == myUid,
                                    isSeen = seen,
                                    isDelivered = delivered
                                )
                            }

                        messages.clear()
                        messages.addAll(newList)

                        val unreadIds =
                            snapshot.documents.filter { doc ->

                                val senderId =
                                    doc.getString("senderId")

                                val seen =
                                    doc.getBoolean(
                                        "isSeen"
                                    ) ?: false

                                senderId != myUid && !seen

                            }.map { it.id }

                        if (
                            unreadIds.isNotEmpty() &&
                            roomName == activeRoomId
                        ) {

                            markMessagesAsSeenBatch(
                                roomName,
                                unreadIds
                            )

                            MyApplication.notificationHelper
                                .cancelNotification(roomName)
                        }

                        if (roomName != activeRoomId) {

                            val latestMessage =
                                newList.lastOrNull()

                            if (
                                latestMessage != null &&
                                !latestMessage.isMe &&
                                !latestMessage.isSeen
                            ) {

                                showLocalMessageNotification(
                                    latestMessage,
                                    roomName,
                                    peerUid ?: ""
                                )
                            }
                        }

                        connectionStatus = "Connected"
                    }
                }

        messageListeners[roomName] = listener
    }

    private fun markMessagesAsSeenBatch(
        roomName: String,
        messageIds: List<String>
    ) {

        if (isUpdatingSeen) return

        isUpdatingSeen = true

        val batch = db.batch()

        messageIds.forEach { id ->

            val docRef =
                db.collection("rooms")
                    .document(roomName)
                    .collection("messages")
                    .document(id)

            batch.update(
                docRef,
                "isSeen", true,
                "isDelivered", true
            )
        }

        val roomRef =
            db.collection("rooms")
                .document(roomName)

        batch.update(
            roomRef,
            "isLastMessageSeen", true
        )

        batch.commit()
            .addOnSuccessListener {
                isUpdatingSeen = false
            }
            .addOnFailureListener { e ->

                isUpdatingSeen = false

                Log.e(
                    TAG,
                    "Failed to update seen status",
                    e
                )
            }
    }

    private fun showLocalMessageNotification(
        message: Message,
        roomId: String,
        senderId: String
    ) {

        val senderName =
            message.senderName ?: "Friend"

        MyApplication.notificationHelper
            .showMessageNotification(
                senderName,
                message.text ?: "New message",
                roomId,
                senderId
            )
    }

    fun stopListeningToMessages() {

        messageListeners.values.forEach {
            it.remove()
        }

        messageListeners.clear()

        messages.clear()
    }

    fun fetchUserInfo(uid: String) {

        db.collection("users")
            .document(uid)
            .get()
            .addOnSuccessListener { snapshot ->

                val user =
                    snapshot.toObject(User::class.java)

                if (user != null) {
                    selectedUserStatus = user
                }
            }
    }

    fun uploadImage(
        uri: Uri,
        path: String,
        fileName: String? = null,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {

        val name = fileName ?: UUID.randomUUID().toString()

        val ref = storage.reference.child(path).child(name)

        Log.d(TAG, "Uploading file to: ${ref.path} (bucket=${ref.bucket})")

        // Use explicit success/failure listeners to surface errors clearly
        ref.putFile(uri)
            .addOnSuccessListener { _uploadTaskSnapshot ->
                // get download url
                ref.downloadUrl
                    .addOnSuccessListener { downloadUrl ->
                        Log.d(TAG, "Upload successful, downloadUrl=$downloadUrl")
                        onSuccess(downloadUrl.toString())
                    }
                    .addOnFailureListener { e ->
                        Log.e(TAG, "Failed to retrieve download URL", e)
                        onError(e.message ?: "Failed to retrieve download URL")
                    }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Upload failed for path=${ref.path}", e)
                onError(e.message ?: "Upload failed")
            }
    }

    fun sendMessage(
        roomName: String,
        text: String,
        imageUrl: String? = null,
        replyToId: String? = null
    ) {

        val user =
            auth.currentUser ?: return

        val now = Timestamp.now()

        val messageData = hashMapOf(
            "senderId" to user.uid,
            "senderName" to (
                    user.displayName ?: "Anonymous"
                    ),
            "text" to text,
            "imageUrl" to imageUrl,
            "timestamp" to now,
            "isSeen" to false,
            "isDelivered" to false,
            "replyToId" to replyToId,
            "isEdited" to false
        )

        db.collection("rooms")
            .document(roomName)
            .collection("messages")
            .add(messageData)
            .addOnSuccessListener { docRef ->
                // only update room metadata after message is successfully stored
                val lastMsgText = if (imageUrl != null && text.isEmpty()) "📷 Sent an image" else text

                db.collection("rooms")
                    .document(roomName)
                    .set(
                        mapOf(
                            "lastMessage" to lastMsgText,
                            "lastMessageTime" to now,
                            "lastMessageSenderId" to user.uid,
                            "isLastMessageSeen" to false
                        ),
                        SetOptions.merge()
                    )

                db.collection("users")
                    .document(user.uid)
                    .update(
                        "lastMessage", lastMsgText,
                        "lastMessageTime", System.currentTimeMillis()
                    )

                // send push/notification after success
                sendMessageNotification(
                    roomName,
                    user.displayName ?: "Someone",
                    text,
                    imageUrl
                )
            }
            .addOnFailureListener { e ->
                // don't update room metadata if message write failed
                Log.e(TAG, "Failed to send message document", e)
            }
    }

    private fun sendMessageNotification(
        roomName: String,
        senderName: String,
        messageText: String,
        imageUrl: String?
    ) {

        val myUid =
            auth.currentUser?.uid ?: return

        val peerUid =
            roomName.split("_")
                .find { it != myUid }
                ?: return

        db.collection("users")
            .document(peerUid)
            .get()
            .addOnSuccessListener { document ->

                val fcmToken =
                    document.getString("fcmToken")
                        ?: return@addOnSuccessListener

                val notificationMessage =
                    if (imageUrl != null)
                        "📷 Sent an image"
                    else
                        messageText.ifEmpty {
                            "New message"
                        }

                sendFCMNotification(
                    token = fcmToken,
                    title = senderName,
                    message = notificationMessage
                )
            }
    }

    private fun sendFCMNotification(
        token: String,
        title: String,
        message: String
    ) {

        db.collection("notifications")
            .add(
                mapOf(
                    "token" to token,
                    "title" to title,
                    "body" to message,
                    "timestamp" to Timestamp.now(),
                    "sent" to false
                )
            )
    }

    fun editMessage(
        roomName: String,
        messageId: String,
        newText: String
    ) {

        db.collection("rooms")
            .document(roomName)
            .collection("messages")
            .document(messageId)
            .update(
                "text", newText,
                "isEdited", true
            )
    }

    fun deleteMessage(
        roomName: String,
        messageId: String
    ) {

        db.collection("rooms")
            .document(roomName)
            .collection("messages")
            .document(messageId)
            .delete()
    }

    fun addReaction(
        roomName: String,
        messageId: String,
        emoji: String
    ) {

        val userId =
            auth.currentUser?.uid ?: return

        db.collection("rooms")
            .document(roomName)
            .collection("messages")
            .document(messageId)
            .update(
                "reactions.$userId",
                emoji
            )
    }

    fun removeReaction(
        roomName: String,
        messageId: String
    ) {

        val userId =
            auth.currentUser?.uid ?: return

        db.collection("rooms")
            .document(roomName)
            .collection("messages")
            .document(messageId)
            .update(
                "reactions.$userId",
                FieldValue.delete()
            )
    }

    fun clearChatHistory(roomName: String) {

        db.collection("rooms")
            .document(roomName)
            .collection("messages")
            .get()
            .addOnSuccessListener { snapshot ->

                val batch = db.batch()

                snapshot.documents.forEach {
                    batch.delete(it.reference)
                }

                batch.commit()
            }

        db.collection("rooms")
            .document(roomName)
            .update(
                "lastMessage", "",
                "lastMessageTime", 0
            )
    }

    fun logout() {

        setUserOnline(false)

        messageListeners.values.forEach {
            it.remove()
        }

        userListeners.values.forEach {
            it.remove()
        }

        auth.signOut()
    }

    fun setTypingStatus(
        roomId: String,
        notEmpty: Boolean
    ) {

        val uid = auth.currentUser?.uid ?: return

        db.collection("rooms")
            .document(roomId)
            .set(
                mapOf(
                    "typing" to mapOf(
                        uid to notEmpty
                    )
                ),
                SetOptions.merge()
            )
            .addOnFailureListener { e ->

                Log.e(
                    TAG,
                    "Failed to update typing status",
                    e
                )
            }
    }

    fun findUserByEmail(
        email: String,
        onSuccess: (User) -> Unit,
        onError: (String) -> Unit
    ) {

        if (email.isBlank()) {

            onError("Email cannot be empty")

            return
        }

        db.collection("users")
            .whereEqualTo("email", email.trim())
            .limit(1)
            .get()
            .addOnSuccessListener { snapshot ->

                if (!snapshot.isEmpty) {

                    val user =
                        snapshot.documents.firstOrNull()
                            ?.toObject(User::class.java)

                    if (user != null) {

                        onSuccess(user)

                    } else {

                        onError("User data is invalid")
                    }

                } else {

                    onError("User not found")
                }
            }
            .addOnFailureListener { e ->

                Log.e(
                    TAG,
                    "Failed to find user",
                    e
                )

                onError(
                    e.message ?: "Something went wrong"
                )
            }
    }

    fun updateProfileImage(
        imageUrl: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {

        val uid = auth.currentUser?.uid

        if (uid == null) {
            onError("User not logged in")
            return
        }

        db.collection("users")
            .document(uid)
            .update("profileImageUrl", imageUrl)
            .addOnSuccessListener {

                currentUser = currentUser?.copy(
                    profileImageUrl = imageUrl
                )

                onSuccess()
            }
            .addOnFailureListener { e ->

                Log.e(
                    TAG,
                    "Failed to update profile image",
                    e
                )

                onError(
                    e.message ?: "Update failed"
                )
            }
    }

    fun updateDisplayName(
        newName: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {

        val user = auth.currentUser

        if (user == null) {
            onError("User not logged in")
            return
        }

        val profileUpdates =
            com.google.firebase.auth.userProfileChangeRequest {
                displayName = newName
            }

        user.updateProfile(profileUpdates)
            .addOnCompleteListener { task ->

                if (task.isSuccessful) {

                    db.collection("users")
                        .document(user.uid)
                        .update("displayName", newName)
                        .addOnSuccessListener {

                            currentUser = currentUser?.copy(
                                displayName = newName
                            )

                            onSuccess()
                        }
                        .addOnFailureListener { e ->

                            Log.e(
                                TAG,
                                "Failed to update Firestore name",
                                e
                            )

                            onError(
                                e.message ?: "Firestore update failed"
                            )
                        }

                } else {

                    onError(
                        task.exception?.message
                            ?: "Profile update failed"
                    )
                }
            }
    }

    // removed updateUserInfoField as Add Info option was removed from UI
    companion object {
        private const val TAG =
            "ChatViewModel"
    }
}