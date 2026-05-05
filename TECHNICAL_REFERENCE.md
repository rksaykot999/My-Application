# 🔧 Technical Reference

## 📝 File Structure

```
app/src/main/
├── java/com/rksaykot/myapplication/
│   ├── MainActivity.kt
│   ├── ui/
│   │   ├── screens/
│   │   │   ├── SplashScreen.kt          ← ✨ UPGRADED
│   │   │   ├── ChatScreen.kt            ← ✨ UPGRADED
│   │   │   ├── HomeScreen.kt
│   │   │   └── ... (others)
│   │   ├── components/
│   │   │   └── MessageBubble.kt         ← ✨ UPGRADED
│   │   └── theme/
│   ├── model/
│   │   ├── Message.kt                   ← ✨ UPGRADED
│   │   ├── User.kt
│   │   └── Contact.kt
│   ├── viewmodel/
│   │   ├── ChatViewModel.kt             ← ✨ UPGRADED
│   │   └── ThemeViewModel.kt
│   ├── service/
│   │   └── FCMService.kt                ← ✨ UPGRADED
│   └── util/
│       └── NotificationHelper.kt        ← ✨ NEW
└── AndroidManifest.xml
```

---

## 🔄 Data Flow Diagrams

### **Image/Video Sending Flow**
```
User clicks 📷/🎥
    ↓
File picker opens
    ↓
User selects file
    ↓
Preview shows
    ↓
User clicks Send ➤
    ↓
uploadImage() called
    ↓
Firebase Storage upload
    ↓
URL obtained
    ↓
sendMessage() with URL
    ↓
Message appears in chat
```

### **Message Editing Flow**
```
Long-press message
    ↓
Options dialog opens
    ↓
Click "Edit"
    ↓
Edit dialog appears
    ↓
User modifies text
    ↓
Click "Update"
    ↓
editMessage() called
    ↓
Firebase updates document
    ↓
"(edited)" label appears
```

### **Notification Flow**
```
Message received
    ↓
listenToMessages() detects new message
    ↓
LaunchedEffect triggers
    ↓
NotificationHelper.showMessageNotification()
    ↓
Creates notification
    ↓
Adds sound + vibration
    ↓
Shows on screen
    ↓
User taps → Opens chat
```

---

## 📚 API Reference

### **ChatViewModel Methods**

#### **sendMessage()**
```kotlin
fun sendMessage(
    roomName: String,           // e.g., "uid1_uid2"
    text: String,               // Message text
    imageUrl: String? = null,   // Image URL from storage
    videoUrl: String? = null,   // Video URL from storage
    replyToId: String? = null   // Reply to message ID
)
```

#### **editMessage()**
```kotlin
fun editMessage(
    roomName: String,           // Chat room ID
    messageId: String,          // Message document ID
    newText: String             // Updated text
)
```

#### **uploadImage()** (also for video)
```kotlin
fun uploadImage(
    uri: Uri,                   // File URI
    path: String,               // "chat_images" or "chat_videos"
    onSuccess: (String) -> Unit,// URL callback
    onError: (String) -> Unit   // Error callback
)
```

#### **deleteMessage()**
```kotlin
fun deleteMessage(
    roomName: String,           // Chat room ID
    messageId: String           // Message ID to delete
)
```

#### **addReaction()**
```kotlin
fun addReaction(
    roomName: String,           // Chat room ID
    messageId: String,          // Message ID
    emoji: String               // "❤️", "😂", etc.
)
```

---

### **NotificationHelper Methods**

#### **showMessageNotification()**
```kotlin
fun showMessageNotification(
    context: Context,           // Android context
    senderName: String,         // "John" 
    messageText: String,        // "Hello!"
    mediaType: String? = null   // "image", "video", or null
)
```

---

## 🗂️ Firestore Structure

### **Messages Collection**
```
rooms/
├── {roomId}/
│   └── messages/
│       └── {messageId}/
│           ├── senderId: String
│           ├── senderName: String
│           ├── text: String
│           ├── imageUrl: String? (null if no image)
│           ├── videoUrl: String? (null if no video)
│           ├── timestamp: Timestamp
│           ├── isSeen: Boolean
│           ├── isDelivered: Boolean
│           ├── isEdited: Boolean
│           ├── editedAt: Timestamp?
│           ├── replyToId: String? (null if no reply)
│           └── reactions: Map<String, String>
```

### **Room Metadata**
```
rooms/
└── {roomId}/
    ├── lastMessage: String
    └── lastMessageTime: Timestamp
```

---

## 🎨 Animation Details

### **SplashScreen Animations**
```kotlin
// Logo Scale (Overshoot)
scale → 0f to 0.8f
duration: 1200ms
easing: OvershootInterpolator(4f)

// Logo Rotation
rotationZ → 0f to 360f
duration: 1500ms
easing: FastOutSlowInEasing

// Text Y Offset (Euler)
offset → 50f to 0f
duration: 900ms
delay: 200ms
easing: EaseOutExpo

// Text Alpha
alpha → 0f to 1f
duration: 900ms
delay: 200ms
```

---

## 🔔 Notification Specs

### **Notification Channels**
- **Channel ID:** `chat_messages`
- **Importance:** `HIGH`
- **Vibration:** Enabled
- **Lights:** Enabled (Blue: -0x10000)
- **Sound:** Default notification sound

### **Vibration Pattern**
```
longArrayOf(0, 500, 250, 500)
├─ 0ms: Start
├─ 500ms: Vibrate
├─ 250ms: Pause
└─ 500ms: Vibrate
```

---

## 🐛 Debugging

### **Enable Firestore Logging**
```kotlin
// In ChatViewModel
private val db = FirebaseFirestore.getInstance().apply {
    firestoreSettings = firestoreSettings {
        isPersistenceEnabled = true
    }
}
```

### **Check FCM Token**
```kotlin
FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
    Log.d("FCM_TOKEN", token)
}
```

### **View Notification Permissions**
```kotlin
// In MainActivity
val notificationPermission = ContextCompat.checkSelfPermission(
    this,
    Manifest.permission.POST_NOTIFICATIONS
)
Log.d("PERM", "Notification: $notificationPermission")
```

---

## 🚀 Performance Tips

1. **Lazy Loading Messages:**
   ```kotlin
   // Add pagination in listenToMessages
   .limit(50)  // Load in chunks
   .startAt(lastMessage)  // For pagination
   ```

2. **Image Compression:**
   ```kotlin
   // Compress before upload
   val compressedUri = compressImage(uri)
   uploadImage(compressedUri, ...)
   ```

3. **Batch Updates:**
   ```kotlin
   val batch = db.batch()
   batch.update(doc1, updates)
   batch.update(doc2, updates)
   batch.commit()
   ```

---

## 📱 Device Compatibility

| Feature | Min API | Notes |
|---------|---------|-------|
| Video display | 21 (5.0) | Works on all modern devices |
| Notifications | 24 (7.0) | Enhanced on 26+ |
| Animations | 21 (5.0) | Smooth on modern devices |
| Copy to clipboard | 11 (2.3) | Standard Android feature |
| Share intent | 4 (1.6) | System-wide sharing |

---

## 🔐 Security Considerations

### **Firestore Security Rules**
```javascript
// Required for this app:
match /rooms/{roomId}/messages/{messageId} {
  allow read, write: if request.auth.uid != null;
  allow delete: if request.auth.uid == resource.data.senderId;
  allow update: if request.auth.uid == resource.data.senderId;
}

match /chat_images/{document=**} {
  allow read, write: if request.auth.uid != null;
}

match /chat_videos/{document=**} {
  allow read, write: if request.auth.uid != null;
}
```

---

## 📊 Testing Commands

### **Gradle Build**
```bash
./gradlew build
./gradlew clean build
./gradlew build -x test  # Skip tests
```

### **Run Tests**
```bash
./gradlew test
./gradlew connectedAndroidTest
```

### **Lint Check**
```bash
./gradlew lint
```

---

## 🎯 Common Tasks

### **Add New Reaction Emoji**
Edit `ChatScreen.kt`:
```kotlin
listOf("❤️", "😂", "😮", "😢", "😡", "👍", "🔥", "✨")  // Add here
```

### **Change Notification Sound**
Edit `NotificationHelper.kt`:
```kotlin
val soundUri = RingtoneManager.getDefaultUri(
    RingtoneManager.TYPE_NOTIFICATION  // Change to TYPE_RINGTONE
)
```

### **Increase Video Upload Size**
Firebase Storage rules - no client-side limit needed.

---

## 📞 Troubleshooting Checklist

- [ ] Android API level 21+
- [ ] Firebase project configured
- [ ] Google Services JSON imported
- [ ] Notification permission granted
- [ ] Internet permission enabled
- [ ] Read/Write storage access if needed
- [ ] Firebase Firestore enabled
- [ ] Firebase Storage enabled
- [ ] Firebase Authentication enabled

---

## 🔗 Useful Documentation Links

- [Firebase Firestore](https://firebase.google.com/docs/firestore)
- [Jetpack Compose](https://developer.android.com/jetpack/compose)
- [Android Notifications](https://developer.android.com/guide/topics/ui/notifiers/notifications)
- [Firebase Storage](https://firebase.google.com/docs/storage)
- [Firebase Messaging](https://firebase.google.com/docs/cloud-messaging)

---

**Last Updated:** May 5, 2026
**Version:** 1.5.0
**Status:** Production Ready ✅

