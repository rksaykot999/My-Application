package com.rksaykot.myapplication.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rksaykot.myapplication.model.User
import com.rksaykot.myapplication.viewmodel.ChatViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onContactClick: (String, String) -> Unit,
    onLogout: () -> Unit,
    onSettingsClick: () -> Unit,
    onProfileClick: () -> Unit,
    viewModel: ChatViewModel = viewModel()
) {
    var showMenu by remember { mutableStateOf(false) }
    var showAddDialog by remember { mutableStateOf(false) }
    var friendEmail by remember { mutableStateOf("") }
    var isSearchActive by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    
    val context = LocalContext.current
    
    LaunchedEffect(Unit) {
        viewModel.fetchAllUsers()
    }

    val filteredUsers = if (searchQuery.isEmpty()) {
        viewModel.users
    } else {
        viewModel.users.filter { 
            it.displayName.contains(searchQuery, ignoreCase = true) || 
            it.email.contains(searchQuery, ignoreCase = true) 
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    if (isSearchActive) {
                        TextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Search friends...") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            )
                        )
                    } else {
                        Text(text = "Best Friend", fontWeight = FontWeight.Bold)
                    }
                },
                actions = {
                    IconButton(onClick = { 
                        isSearchActive = !isSearchActive 
                        if (!isSearchActive) searchQuery = ""
                    }) { 
                        Icon(
                            imageVector = if (isSearchActive) Icons.Default.Close else Icons.Default.Search, 
                            contentDescription = null
                        ) 
                    }
                    Box {
                        IconButton(onClick = { showMenu = true }) { 
                            Icon(Icons.Default.MoreVert, contentDescription = null) 
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(text = { Text("Profile") }, onClick = { showMenu = false; onProfileClick() })
                            DropdownMenuItem(text = { Text("Settings") }, onClick = { showMenu = false; onSettingsClick() })
                            DropdownMenuItem(
                                text = { Text("Logout", color = Color.Red) },
                                onClick = { 
                                    showMenu = false
                                    viewModel.logout()
                                    onLogout()
                                }
                            )
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = "Add Friend")
            }
        }
    ) { innerPadding ->
        if (viewModel.users.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                Text("No friends yet. Add one by email!", color = Color.Gray)
            }
        } else {
            LazyColumn(modifier = Modifier.padding(innerPadding)) {
                items(filteredUsers) { user ->
                    val myUid = viewModel.currentUser?.uid ?: ""
                    val peerUid = user.uid
                    val roomId = if (myUid < peerUid) "${myUid}_${peerUid}" else "${peerUid}_${myUid}"
                    val lastMsg = viewModel.lastMessages[roomId] ?: user.lastMessage

                    UserItem(user, lastMsg) {
                        onContactClick(roomId, user.displayName)
                    }
                    Box(modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 72.dp, end = 16.dp)
                        .height(0.5.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant)
                    )
                }
            }
        }

        if (showAddDialog) {
            AlertDialog(
                onDismissRequest = { showAddDialog = false },
                title = { Text("Add Friend") },
                text = {
                    OutlinedTextField(
                        value = friendEmail,
                        onValueChange = { friendEmail = it },
                        label = { Text("Friend's Email") },
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                confirmButton = {
                    Button(onClick = {
                        viewModel.findUserByEmail(friendEmail, 
                            onSuccess = { user ->
                                val myUid = viewModel.currentUser?.uid ?: ""
                                val peerUid = user.uid
                                val roomId = if (myUid < peerUid) "${myUid}_${peerUid}" else "${peerUid}_${myUid}"
                                onContactClick(roomId, user.displayName)
                                showAddDialog = false
                            },
                            onError = { 
                                Toast.makeText(context, "User not found", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }) { Text("Add") }
                },
                dismissButton = {
                    TextButton(onClick = { showAddDialog = false }) { Text("Cancel") }
                }
            )
        }
    }
}

@Composable
fun UserItem(user: User, lastMessage: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box {
            Surface(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape),
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = user.displayName.take(1).uppercase(),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
            // Online/Offline Status Bubble
            Box(
                modifier = Modifier
                    .size(14.dp)
                    .align(Alignment.BottomEnd)
                    .clip(CircleShape)
                    .background(if (user.isOnline) Color.Green else Color.Gray)
                    .border(2.dp, MaterialTheme.colorScheme.surface, CircleShape)
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(text = user.displayName, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            Text(
                text = if (lastMessage.isNotEmpty()) lastMessage else "Tap to chat",
                color = Color.Gray,
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
