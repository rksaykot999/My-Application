package com.rksaykot.myapplication.ui.screens

import android.widget.Toast
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
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rksaykot.myapplication.model.Message
import com.rksaykot.myapplication.ui.components.MessageBubble
import com.rksaykot.myapplication.viewmodel.ChatViewModel

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
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clickable { onDetailsClick() }
                            .padding(vertical = 4.dp)
                    ) {
                        Surface(
                            modifier = Modifier.size(38.dp),
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(displayName.take(1).uppercase(), fontWeight = FontWeight.Bold)
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                displayName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            val status = viewModel.selectedUserStatus
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .background(
                                            if (status?.isOnline == true) Color(0xFF4CAF50) else Color.Gray,
                                            CircleShape
                                        )
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (status?.isOnline == true) "Active now" else "Offline",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.primary)
                    }
                },
                actions = {
                    IconButton(onClick = {
                        Toast.makeText(context, "Audio Call coming soon!", Toast.LENGTH_SHORT).show()
                    }) { Icon(Icons.Default.Call, contentDescription = "Call", tint = MaterialTheme.colorScheme.primary) }

                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Default.Info, contentDescription = "Details", tint = MaterialTheme.colorScheme.primary)
                        }
                        DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                            DropdownMenuItem(
                                text = { Text("Clear History") },
                                onClick = {
                                    showMenu = false
                                    viewModel.clearChatHistory(roomId)
                                },
                                leadingIcon = { Icon(Icons.Default.DeleteSweep, null) }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .imePadding()
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                // Typing Indicator
                if (viewModel.typingUser != null) {
                    Text(
                        text = "Typing...",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 16.dp, bottom = 4.dp, top = 4.dp)
                    )
                }

                // Reply Preview
                if (replyingToMessage != null) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(modifier = Modifier.width(4.dp).height(32.dp).background(MaterialTheme.colorScheme.primary, RoundedCornerShape(2.dp)))
                            Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
                                Text(
                                    text = if (replyingToMessage!!.isMe) "Replying to yourself" else "Replying to ${replyingToMessage!!.senderName}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = replyingToMessage!!.text,
                                    fontSize = 13.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(onClick = { replyingToMessage = null }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.Close, contentDescription = "Cancel", modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Feature icons
                    IconButton(onClick = {
                        Toast.makeText(context, "Media sharing feature coming soon!", Toast.LENGTH_SHORT).show()
                    }) {
                        Icon(Icons.Default.AddCircle, contentDescription = "More", tint = MaterialTheme.colorScheme.primary)
                    }

                    // Rounded Input Field
                    OutlinedTextField(
                        value = messageText,
                        onValueChange = {
                            messageText = it
                            viewModel.setTypingStatus(roomId, it.isNotEmpty())
                        },
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 4.dp),
                        placeholder = { Text("Message...", fontSize = 15.sp) },
                        maxLines = 5,
                        shape = RoundedCornerShape(24.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                            unfocusedBorderColor = Color.Transparent,
                            focusedBorderColor = Color.Transparent
                        )
                    )

                    if (messageText.isNotBlank()) {
                        IconButton(
                            onClick = {
                                viewModel.sendMessage(roomId, messageText, replyToId = replyingToMessage?.id)
                                messageText = ""
                                replyingToMessage = null
                                viewModel.setTypingStatus(roomId, false)
                            }
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = MaterialTheme.colorScheme.primary)
                        }
                    } else {
                        IconButton(onClick = {
                            viewModel.sendMessage(roomId, "👍")
                            Toast.makeText(context, "Liked!", Toast.LENGTH_SHORT).show()
                        }) {
                            Icon(Icons.Default.ThumbUp, contentDescription = "Like", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            state = listState,
            reverseLayout = true,
            contentPadding = PaddingValues(bottom = 16.dp, top = 8.dp, start = 8.dp, end = 8.dp)
        ) {
            items(messages.reversed(), key = { it.id }) { message ->
                val replyText = messages.find { it.id == message.replyToId }?.text
                MessageBubble(
                    message = message,
                    replyMessageText = replyText,
                    onSwipeToReply = { swipedMessage -> replyingToMessage = swipedMessage },
                    onLongClick = { clickedMessage -> showOptionsDialog = clickedMessage },
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
            title = { Text("Message Options", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        listOf("👍", "❤️", "😂", "😮", "😢", "😡").forEach { emoji ->
                            Text(
                                text = emoji,
                                fontSize = 28.sp,
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .clickable {
                                        viewModel.addReaction(roomId, showOptionsDialog!!.id, emoji)
                                        showOptionsDialog = null
                                    }
                                    .padding(4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    if (showOptionsDialog!!.isMe) {
                        ListItem(
                            headlineContent = { Text("Edit Message") },
                            leadingContent = { Icon(Icons.Default.Edit, null) },
                            modifier = Modifier.clickable {
                                editText = showOptionsDialog!!.text
                                showEditDialog = showOptionsDialog
                                showOptionsDialog = null
                            }
                        )
                        ListItem(
                            headlineContent = { Text("Unsend", color = Color.Red) },
                            leadingContent = { Icon(Icons.Default.Delete, null, tint = Color.Red) },
                            modifier = Modifier.clickable {
                                viewModel.deleteMessage(roomId, showOptionsDialog!!.id)
                                showOptionsDialog = null
                            }
                        )
                    } else {
                        ListItem(
                            headlineContent = { Text("Reply") },
                            leadingContent = { Icon(Icons.Default.Reply, null) },
                            modifier = Modifier.clickable {
                                replyingToMessage = showOptionsDialog
                                showOptionsDialog = null
                            }
                        )
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
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            },
            confirmButton = {
                Button(onClick = {
                    if (editText.isNotBlank()) {
                        viewModel.editMessage(roomId, showEditDialog!!.id, editText)
                        showEditDialog = null
                    }
                }) { Text("Save Changes") }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = null }) { Text("Cancel") }
            }
        )
    }
}