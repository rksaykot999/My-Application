package com.rksaykot.myapplication.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rksaykot.myapplication.model.User
import com.rksaykot.myapplication.viewmodel.ChatViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onContactClick: (String, String) -> Unit, // roomId, displayName
    onLogout: () -> Unit,
    viewModel: ChatViewModel = viewModel()
) {
    var showMenu by remember { mutableStateOf(false) }
    var showAddDialog by remember { mutableStateOf(false) }
    var friendEmail by remember { mutableStateOf("") }
    
    LaunchedEffect(Unit) {
        viewModel.fetchAllUsers()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    val name = viewModel.currentUser?.displayName ?: "User"
                    Text("Chat with $name", fontWeight = FontWeight.Bold) 
                },
                actions = {
                    IconButton(onClick = { }) { Icon(Icons.Default.Search, contentDescription = "Search") }
                    Box {
                        IconButton(onClick = { showMenu = true }) { 
                            Icon(Icons.Default.MoreVert, contentDescription = "Settings") 
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(text = { Text("Profile") }, onClick = { showMenu = false })
                            DropdownMenuItem(text = { Text("Settings") }, onClick = { showMenu = false })
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
                items(viewModel.users) { user ->
                    UserItem(user) {
                        // Generate a simple room ID based on both UIDs
                        val myUid = viewModel.currentUser?.uid ?: ""
                        val peerUid = user.uid
                        val roomId = if (myUid < peerUid) "${myUid}_${peerUid}" else "${peerUid}_${myUid}"
                        onContactClick(roomId, user.displayName)
                    }
                    HorizontalDivider(modifier = Modifier.padding(start = 72.dp), thickness = 0.5.dp, color = Color.LightGray)
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
                                // For now, we just open the chat with them
                                val myUid = viewModel.currentUser?.uid ?: ""
                                val peerUid = user.uid
                                val roomId = if (myUid < peerUid) "${myUid}_${peerUid}" else "${peerUid}_${myUid}"
                                onContactClick(roomId, user.displayName)
                                showAddDialog = false
                            },
                            onError = { /* Show error toast/text */ }
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
fun UserItem(user: User, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
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

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(text = user.displayName, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            Text(text = "Tap to chat", color = Color.Gray, fontSize = 14.sp)
        }
    }
}
