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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rksaykot.myapplication.model.Message
import com.rksaykot.myapplication.ui.components.MessageBubble
import com.rksaykot.myapplication.viewmodel.ChatViewModel
import java.text.SimpleDateFormat
import java.util.*


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    roomId: String,
    displayName: String,
    onBack: () -> Unit,
    onDetailsClick: () -> Unit,
    viewModel: ChatViewModel
) {
    var messageText by remember { mutableStateOf("") }
    var replyingToMessage by remember { mutableStateOf<Message?>(null) }
    var showOptionsDialog by remember { mutableStateOf<Message?>(null) }
    var showEditDialog by remember { mutableStateOf<Message?>(null) }
    var editText by remember { mutableStateOf("") }
    var showMenu by remember { mutableStateOf(false) }

    val messages = viewModel.messages
    val context = LocalContext.current
    val listState = rememberLazyListState()

    val imageLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            Toast.makeText(context, "Uploading image...", Toast.LENGTH_SHORT).show()
            viewModel.uploadImage(it, "chat_images",
                onSuccess = { url ->
                    viewModel.sendMessage(roomId, "", imageUrl = url)
                },
                onError = { error ->
                    Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
                }
            )
        }
    }

    DisposableEffect(roomId) {
        viewModel.activeRoomId = roomId
        viewModel.listenToMessages(roomId)
        onDispose {
            viewModel.activeRoomId = null
            viewModel.stopListeningToMessages()
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.clickable { onDetailsClick() }
                    ) {
                        Text(
                            displayName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        val status = viewModel.selectedUserStatus
                        Text(
                            text = if (status?.isOnline == true) "Online" else "Offline",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (status?.isOnline == true) Color.Green else Color.Gray,
                            fontSize = 10.sp
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        Toast.makeText(context, "Audio Call feature coming soon!", Toast.LENGTH_SHORT).show()
                    }) { Icon(Icons.Default.Call, contentDescription = "Call") }
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "More")
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Clear Message History") },
                                onClick = {
                                    showMenu = false
                                    viewModel.clearChatHistory(roomId)
                                }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
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
                        text = "Someone is typing...",
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

                Surface(tonalElevation = 3.dp) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { imageLauncher.launch("image/*") }) {
                            Icon(Icons.Default.Image, contentDescription = "Send Image", tint = MaterialTheme.colorScheme.primary)
                        }

                        TextField(
                            value = messageText,
                            onValueChange = {
                                messageText = it
                                viewModel.setTypingStatus(roomId, it.isNotEmpty())
                            },
                            modifier = Modifier.weight(1f),
                            placeholder = { Text("Type a message...") },
                            maxLines = 4,
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent
                            )
                        )
                        IconButton(
                            onClick = {
                                if (messageText.isNotBlank()) {
                                    viewModel.sendMessage(roomId, messageText, replyToId = replyingToMessage?.id)
                                    messageText = ""
                                    replyingToMessage = null
                                    viewModel.setTypingStatus(roomId, false)
                                }
                            }
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = MaterialTheme.colorScheme.primary)
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
            reverseLayout = true,
            contentPadding = PaddingValues(8.dp)
        ) {
            items(messages.reversed()) { message ->
                val replyText = messages.find { it.id == message.replyToId }?.text
                MessageBubble(
                    message = message,
                    replyMessageText = replyText,
                    onSwipeToReply = { swipedMessage ->
                        replyingToMessage = swipedMessage
                    },
                    onLongClick = { clickedMessage ->
                        showOptionsDialog = clickedMessage
                    },
                    onProfileClick = { uid ->
                        viewModel.fetchUserInfo(uid)
                        onDetailsClick()
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
                        listOf("👍", "❤️", "😂", "😮", "😢", "😡").forEach { emoji ->
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
                                editText = showOptionsDialog!!.text
                                showEditDialog = showOptionsDialog
                                showOptionsDialog = null
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Edit Message")
                        }

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

    if (showEditDialog != null) {
        AlertDialog(
            onDismissRequest = { showEditDialog = null },
            title = { Text("Edit Message") },
            text = {
                OutlinedTextField(
                    value = editText,
                    onValueChange = { editText = it },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.editMessage(roomId, showEditDialog!!.id, editText)
                    showEditDialog = null
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = null }) { Text("Cancel") }
            }
        )
    }
}
