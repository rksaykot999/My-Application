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
import androidx.compose.ui.graphics.Brush
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
    // State management for UI
    val contacts = viewModel.contacts // Using 'contacts' instead of 'users' as per latest ViewModel
    val unreadRooms = viewModel.unreadRooms
    val currentUser = viewModel.currentUser

    var searchQuery by remember { mutableStateOf("") }
    var showAddDialog by remember { mutableStateOf(false) }
    var friendQuery by remember { mutableStateOf("") }
    val context = LocalContext.current

    // Filtering logic for search
    val filteredContacts = contacts.filter {
        it.displayName.contains(searchQuery, ignoreCase = true)
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
                            // Online status indicator for current user
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
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("Search among your friends...", fontSize = 14.sp) },
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
                    contentPadding = PaddingValues(top = 8.dp, bottom = 80.dp) // Extra bottom padding for FAB
                ) {
                    items(filteredContacts, key = { it.uid }) { user ->
                        val myUid = currentUser?.uid ?: ""
                        // Generate Room ID (Smaller UID first for consistency)
                        val roomId = if (myUid < user.uid) "${myUid}_${user.uid}" else "${user.uid}_$myUid"
                        val hasUnread = unreadRooms[roomId] ?: false

                        UserChatItem(
                            user = user,
                            hasUnread = hasUnread,
                            onClick = { onContactClick(roomId, user.displayName) }
                        )
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = {
                showAddDialog = false
                friendQuery = ""
            },
            title = { Text("Add New Friend", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Enter email or phone number to find your friend.", fontSize = 14.sp, color = Color.Gray)
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = friendQuery,
                        onValueChange = { friendQuery = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Email or Phone") },
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
                                Toast.makeText(context, "Friend added successfully!", Toast.LENGTH_SHORT).show()
                                showAddDialog = false
                                friendQuery = ""
                            }, { error ->
                                Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
                            })
                        }
                    }
                ) {
                    Text("Add Friend")
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
    hasUnread: Boolean,
    onClick: () -> Unit
) {
    val timeFormatter = remember { SimpleDateFormat("h:mm a", Locale.getDefault()) }
    val displayTime = if (user.lastMessageTime > 0) {
        timeFormatter.format(Date(user.lastMessageTime))
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
                .padding(vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Profile Image Section
            Box(contentAlignment = Alignment.BottomEnd) {
                Surface(
                    modifier = Modifier.size(58.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    tonalElevation = 2.dp
                ) {
                    if (!user.profileImageUrl.isNullOrEmpty()) {
                        AsyncImage(
                            model = user.profileImageUrl,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.background(
                                Brush.linearGradient(
                                    listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary)
                                )
                            )
                        ) {
                            Text(
                                user.displayName.take(1).uppercase(),
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 22.sp
                            )
                        }
                    }
                }

                if (user.isOnline) {
                    Surface(
                        modifier = Modifier.size(16.dp).padding(2.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.background
                    ) {
                        Box(modifier = Modifier.fillMaxSize().background(Color(0xFF4CAF50), CircleShape))
                    }
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Information Section
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = user.displayName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = if (hasUnread) FontWeight.ExtraBold else FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Text(
                        text = displayTime,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (hasUnread) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (user.lastMessage.isNotEmpty()) user.lastMessage else if (user.isOnline) "Active now" else "Tap to chat",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (hasUnread) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    if (hasUnread) {
                        Box(
                            modifier = Modifier
                                .padding(start = 8.dp)
                                .size(10.dp)
                                .background(MaterialTheme.colorScheme.primary, CircleShape)
                        )
                    }
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
                text = if (isSearching) "No results found" else if (isEmptyList) "No friends yet" else "No messages yet",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = if (isSearching) "Try a different name" else if (isEmptyList) "Click the + button below to add friends by email or phone" else "Select a contact to start messaging",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}