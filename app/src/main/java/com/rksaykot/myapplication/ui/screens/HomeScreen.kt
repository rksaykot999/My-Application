package com.rksaykot.myapplication.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.rksaykot.myapplication.viewmodel.ChatViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onContactClick: (String, String) -> Unit,
    onLogout: () -> Unit,
    onSettingsClick: () -> Unit,
    onProfileClick: () -> Unit,
    viewModel: ChatViewModel = viewModel()
) {
    // Sync state with ViewModel
    val contacts = viewModel.contacts
    val unreadRooms = viewModel.unreadRooms
    val currentUser = viewModel.currentUser

    var searchQuery by remember { mutableStateOf("") }
    var showAddDialog by remember { mutableStateOf(false) }
    var friendQuery by remember { mutableStateOf("") }
    val context = LocalContext.current

    // Filtering logic based on search query
    val filteredContacts = if (searchQuery.isEmpty()) {
        contacts
    } else {
        contacts.filter { it.displayName.contains(searchQuery, ignoreCase = true) }
    }

    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = {
                    Text(
                        "Messages",
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = (-0.5).sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onProfileClick) {
                        Box(contentAlignment = Alignment.BottomEnd) {
                            Surface(
                                modifier = Modifier.size(34.dp),
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                if (!currentUser?.profileImageUrl.isNullOrEmpty()) {
                                    AsyncImage(
                                        model = currentUser?.profileImageUrl,
                                        contentDescription = null,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.padding(6.dp))
                                }
                            }
                            // Small indicator for self-status
                            Surface(
                                modifier = Modifier.size(10.dp),
                                shape = CircleShape,
                                color = Color(0xFF4CAF50),
                                border = androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.background)
                            ) {}
                        }
                    }
                },
                actions = {
                    IconButton(onClick = onSettingsClick) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                },
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.PersonAdd, contentDescription = "Add Friend")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("Search your contacts...", fontSize = 14.sp) },
                leadingIcon = { Icon(Icons.Outlined.Search, null, modifier = Modifier.size(20.dp)) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Close, null, modifier = Modifier.size(16.dp))
                        }
                    }
                },
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    unfocusedBorderColor = Color.Transparent,
                    focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                ),
                singleLine = true
            )

            if (filteredContacts.isEmpty()) {
                EmptyStateView(
                    isSearching = searchQuery.isNotEmpty(),
                    isEmptyList = contacts.isEmpty()
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(top = 8.dp, bottom = 80.dp)
                ) {
                    items(filteredContacts, key = { it.uid }) { user ->
                        val myUid = currentUser?.uid ?: ""
                        // Room ID logic: alphanumeric sort ensures both users land in the same doc
                        val roomId = if (myUid < user.uid) "${myUid}_${user.uid}" else "${user.uid}_$myUid"
                        val hasUnread = unreadRooms[roomId] ?: false
                        val lastMsg = viewModel.lastMessages[roomId] ?: ""
                        val lastTime = viewModel.lastMessageTimes[roomId] ?: 0L
                        val isLastFromMe = viewModel.lastMessageSenderIds[roomId] == myUid

                        UserChatItem(
                            user = user,
                            lastMessage = lastMsg,
                            lastMessageTime = lastTime,
                            hasUnread = hasUnread,
                            isLastMessageFromMe = isLastFromMe,
                            onClick = { onContactClick(roomId, user.displayName) }
                        )
                    }
                }
            }
        }
    }

    // Add Friend Dialog
    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = {
                showAddDialog = false
                friendQuery = ""
            },
            title = { Text("Connect with a Friend", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Search for your friend using their registered email or phone number.", fontSize = 14.sp, color = Color.Gray)
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = friendQuery,
                        onValueChange = { friendQuery = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Email or Phone Number") },
                        leadingIcon = { Icon(Icons.Default.PersonSearch, null) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (friendQuery.isNotBlank()) {
                            viewModel.addFriend(friendQuery, {
                                Toast.makeText(context, "Contact added successfully!", Toast.LENGTH_SHORT).show()
                                showAddDialog = false
                                friendQuery = ""
                            }, { error ->
                                Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
                            })
                        }
                    }
                ) {
                    Text("Add Now")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showAddDialog = false
                    friendQuery = ""
                }) { Text("Cancel") }
            }
        )
    }
}

@Composable
fun UserChatItem(
    user: com.rksaykot.myapplication.model.User,
    lastMessage: String,
    lastMessageTime: Long,
    hasUnread: Boolean,
    isLastMessageFromMe: Boolean,
    onClick: () -> Unit
) {
    val timeFormatter = remember { SimpleDateFormat("h:mm a", Locale.getDefault()) }
    val displayTime = if (lastMessageTime > 0) {
        timeFormatter.format(Date(lastMessageTime))
    } else {
        ""
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 2.dp),
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Profile Icon / Image
            Box(contentAlignment = Alignment.BottomEnd) {
                Surface(
                    modifier = Modifier.size(56.dp),
                    shape = CircleShape,
                    color = Color(0xFF546081),
                ) {
                    if (!user.profileImageUrl.isNullOrEmpty()) {
                        AsyncImage(
                            model = user.profileImageUrl,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = user.displayName.take(1).uppercase(),
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp
                            )
                        }
                    }
                }

                if (user.isOnline) {
                    Surface(
                        modifier = Modifier.size(14.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.background,
                        border = androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.background)
                    ) {
                        Box(modifier = Modifier.fillMaxSize().background(Color(0xFF4CAF50), CircleShape))
                    }
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Main Info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = user.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = if (hasUnread) FontWeight.Bold else FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(2.dp))

                // Priority: Typing > Last Message Snippet
                val subText = when {
                    user.typing -> "Typing..."
                    lastMessage.isNotEmpty() -> {
                        if (isLastMessageFromMe) "You: $lastMessage" else lastMessage
                    }
                    else -> "No messages yet"
                }

                val subTextColor = if (user.typing) {
                    MaterialTheme.colorScheme.primary
                } else if (hasUnread) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                }

                Text(
                    text = subText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = subTextColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontWeight = if (user.typing || hasUnread) FontWeight.Medium else FontWeight.Normal
                )
            }

            // Meta Info (Time replaces the dot)
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(start = 8.dp)
            ) {
                if (displayTime.isNotEmpty()) {
                    Text(
                        text = displayTime,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (hasUnread) FontWeight.Bold else FontWeight.Normal,
                        color = if (hasUnread) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }

                if (hasUnread && displayTime.isEmpty()) {
                    // Fallback to dot only if time is not available
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(Color(0xFF546081), CircleShape)
                    )
                }
            }
        }
    }
}

@Composable
fun EmptyStateView(isSearching: Boolean, isEmptyList: Boolean) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Surface(
                modifier = Modifier.size(140.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
            ) {
                Icon(
                    imageVector = when {
                        isSearching -> Icons.Default.SearchOff
                        isEmptyList -> Icons.Default.PeopleOutline
                        else -> Icons.Default.ChatBubbleOutline
                    },
                    contentDescription = null,
                    modifier = Modifier.padding(40.dp),
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = if (isSearching) "No results found" else if (isEmptyList) "Start your journey" else "No messages yet",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = if (isSearching) "Try a different name" else if (isEmptyList) "Click the + button below to add friends and start chatting!" else "Select a contact to start messaging",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}