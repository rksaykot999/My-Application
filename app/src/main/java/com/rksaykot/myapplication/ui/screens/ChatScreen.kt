package com.rksaykot.myapplication.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.VideoCall
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.rksaykot.myapplication.model.Message
import com.rksaykot.myapplication.ui.components.MessageBubble
import com.rksaykot.myapplication.viewmodel.ChatViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    roomId: String, 
    displayName: String,
    onBack: () -> Unit,
    viewModel: ChatViewModel = viewModel()
) {
    var messageText by remember { mutableStateOf("") }
    var replyingToMessage by remember { mutableStateOf<Message?>(null) }
    var showOptionsDialog by remember { mutableStateOf<Message?>(null) }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var isUploading by remember { mutableStateOf(false) }
    
    val messages = viewModel.messages
    val context = LocalContext.current
    val listState = rememberLazyListState()

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        selectedImageUri = uri
    }

    LaunchedEffect(roomId) {
        viewModel.listenToMessages(roomId)
    }

    // Auto-scroll to bottom when new messages arrive
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text(displayName)
                        if (viewModel.connectionStatus.isNotEmpty()) {
                            Text(
                                text = viewModel.connectionStatus,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (viewModel.connectionStatus.contains("Error")) Color.Red else Color.Green
                            )
                        } else {
                            val isOnline = viewModel.selectedUserStatus?.isOnline == true
                            Text(
                                text = if (isOnline) "Online" else "Offline",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isOnline) Color.Green else Color.Gray
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { 
                        Toast.makeText(context, "Video Call feature coming soon!", Toast.LENGTH_SHORT).show()
                    }) { Icon(Icons.Default.VideoCall, contentDescription = "Video Call") }
                    IconButton(onClick = { 
                        Toast.makeText(context, "Audio Call feature coming soon!", Toast.LENGTH_SHORT).show()
                    }) { Icon(Icons.Default.Call, contentDescription = "Call") }
                    IconButton(onClick = { }) { Icon(Icons.Default.MoreVert, contentDescription = "More") }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .imePadding()
            ) {
                if (viewModel.typingUser != null) {
                    Text(
                        text = "${viewModel.typingUser} is typing...",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 16.dp, bottom = 4.dp)
                    )
                }

                if (replyingToMessage != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(start = 8.dp)) {
                            Text(
                                text = if (replyingToMessage!!.isMe) "Replying to yourself" else "Replying to ${replyingToMessage!!.senderName}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = replyingToMessage!!.text,
                                fontSize = 14.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        IconButton(onClick = { replyingToMessage = null }) {
                            Icon(Icons.Default.Close, contentDescription = "Cancel Reply")
                        }
                    }
                }

                if (selectedImageUri != null) {
                    Box(modifier = Modifier.padding(8.dp)) {
                        AsyncImage(
                            model = selectedImageUri,
                            contentDescription = "Selected Image",
                            modifier = Modifier
                                .size(100.dp)
                                .clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Crop
                        )
                        IconButton(
                            onClick = { selectedImageUri = null },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .size(24.dp)
                                .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Remove Image", tint = Color.White, modifier = Modifier.size(16.dp))
                        }
                    }
                }

                Surface(tonalElevation = 3.dp) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { imagePickerLauncher.launch("image/*") }) {
                            Icon(Icons.Default.Image, contentDescription = "Pick Image")
                        }
                        TextField(
                            value = messageText,
                            onValueChange = { 
                                messageText = it 
                                viewModel.setTypingStatus(roomId, it.isNotEmpty())
                            },
                            modifier = Modifier.weight(1f),
                            placeholder = { Text("Type a message...") },
                            maxLines = 4
                        )
                        if (isUploading) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp).padding(4.dp))
                        } else {
                            IconButton(
                                onClick = {
                                    if (messageText.isNotBlank() || selectedImageUri != null) {
                                        if (selectedImageUri != null) {
                                            isUploading = true
                                            viewModel.uploadImage(selectedImageUri!!, "chat_images", { url ->
                                                viewModel.sendMessage(roomId, messageText, url, replyingToMessage?.id)
                                                messageText = ""
                                                selectedImageUri = null
                                                replyingToMessage = null
                                                isUploading = false
                                            }, { error ->
                                                isUploading = false
                                                Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
                                            })
                                        } else {
                                            viewModel.sendMessage(roomId, messageText, null, replyingToMessage?.id)
                                            messageText = ""
                                            replyingToMessage = null
                                        }
                                    }
                                }
                            ) {
                                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send")
                            }
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            state = listState,
            reverseLayout = false,
            contentPadding = PaddingValues(8.dp)
        ) {
            items(messages) { message ->
                val replyText = messages.find { it.id == message.replyToId }?.text
                MessageBubble(
                    message = message,
                    replyMessageText = replyText,
                    onSwipeToReply = { swipedMessage ->
                        replyingToMessage = swipedMessage
                    },
                    onLongClick = { clickedMessage ->
                        showOptionsDialog = clickedMessage
                    }
                )
            }
        }
    }

    if (showOptionsDialog != null) {
        AlertDialog(
            onDismissRequest = { showOptionsDialog = null },
            title = { Text("Message Options") },
            text = {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        listOf("❤️", "😂", "😮", "😢", "😡", "👍").forEach { emoji ->
                            Text(
                                text = emoji,
                                fontSize = 32.sp,
                                modifier = Modifier.clickable {
                                    viewModel.addReaction(roomId, showOptionsDialog!!.id, emoji)
                                    showOptionsDialog = null
                                }
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    if (showOptionsDialog!!.reactions.containsKey(viewModel.currentUser?.uid)) {
                        TextButton(
                            onClick = {
                                viewModel.removeReaction(roomId, showOptionsDialog!!.id)
                                showOptionsDialog = null
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Remove Reaction")
                        }
                    }

                    if (showOptionsDialog!!.isMe) {
                        TextButton(
                            onClick = {
                                viewModel.deleteMessage(roomId, showOptionsDialog!!.id)
                                showOptionsDialog = null
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.textButtonColors(contentColor = Color.Red)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Unsend Message")
                        }
                    }
                }
            },
            confirmButton = {}
        )
    }
}
