package com.rksaykot.myapplication.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.rksaykot.myapplication.viewmodel.ChatViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onBack: () -> Unit,
    onNavigateToPrivacy: () -> Unit = {},
    viewModel: ChatViewModel = viewModel()
) {

    val user = viewModel.currentUser

    val context = LocalContext.current

    var showEditNameDialog by remember {
        mutableStateOf(false)
    }

    var newName by remember {
        mutableStateOf(user?.displayName ?: "")
    }

    // Profile image upload disabled per user request

    Scaffold(

        topBar = {

            TopAppBar(

                title = {
                    Text(
                        text = "My Profile",
                        fontWeight = FontWeight.Bold
                    )
                },

                navigationIcon = {

                    IconButton(
                        onClick = onBack
                    ) {

                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }

    ) { innerPadding ->

        LazyColumn(

            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(
                    Brush.verticalGradient(
                        listOf(
                            MaterialTheme.colorScheme.background,
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        )
                    )
                ),

            horizontalAlignment = Alignment.CenterHorizontally

        ) {

            item {

                Spacer(modifier = Modifier.height(24.dp))

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),

                    shape = RoundedCornerShape(28.dp),

                    elevation = CardDefaults.cardElevation(
                        defaultElevation = 8.dp
                    )
                ) {

                    Column(

                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),

                        horizontalAlignment = Alignment.CenterHorizontally

                    ) {

                        Box(
                            contentAlignment = Alignment.BottomEnd
                        ) {

                            Surface(
                                modifier = Modifier
                                    .size(130.dp)
                                    .clip(CircleShape),

                                shape = CircleShape,

                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {

                                if (
                                    user?.profileImageUrl
                                        ?.isNotEmpty() == true
                                ) {

                                    AsyncImage(
                                        model = user.profileImageUrl,
                                        contentDescription = "Profile Image",

                                        modifier = Modifier.fillMaxSize(),

                                        contentScale = ContentScale.Crop
                                    )

                                } else {

                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {

                                        Text(
                                            text = user?.displayName
                                                ?.take(1)
                                                ?.uppercase()
                                                ?: "?",

                                            style = MaterialTheme.typography.displayLarge,

                                            color = MaterialTheme.colorScheme.onPrimaryContainer,

                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }

                            // image upload disabled
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        Text(
                            text = user?.displayName ?: "User",

                            style =
                                MaterialTheme.typography.headlineMedium,

                            fontWeight = FontWeight.Bold,

                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = user?.email ?: "",

                            style =
                                MaterialTheme.typography.bodyMedium,

                            color = Color.Gray
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Surface(
                            shape = RoundedCornerShape(50.dp),
                            color = if (user?.isOnline == true) Color(0xFF1DB954).copy(alpha = 0.15f) else Color.Gray.copy(alpha = 0.15f)
                        ) {
                            val statusText = if (user?.isOnline == true) {
                                "Online"
                            } else {
                                val last = user?.lastSeen ?: 0L
                                val date = java.util.Date(last)
                                val format = java.text.SimpleDateFormat("MMM d, h:mm a", java.util.Locale.getDefault())
                                "Last seen: ${format.format(date)}"
                            }

                            Text(
                                text = statusText,
                                modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp),
                                color = if (user?.isOnline == true) Color(0xFF1DB954) else Color.Gray,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                HorizontalDivider(
                    thickness = 0.7.dp,
                    color = Color.LightGray.copy(alpha = 0.5f)
                )
            }

            item {

                Spacer(modifier = Modifier.height(8.dp))

                ProfileOptionItem(
                    title = "Display Name",

                    value = user?.displayName ?: "",

                    icon = Icons.Default.Person,

                    onClick = {
                        showEditNameDialog = true
                    }
                )

                ProfileOptionItem(
                    title = "Change Password",

                    icon = Icons.Default.Lock,

                    onClick = {

                        Toast.makeText(
                            context,
                            "Password reset feature coming soon",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                )

                // Privacy Settings navigates to PrivacyPolicyScreen
                ProfileOptionItem(
                    title = "Privacy Settings",
                    icon = Icons.Default.Shield,
                    onClick = {
                        onNavigateToPrivacy()
                    }
                )

                var showConfirmLogout by remember { mutableStateOf(false) }

                // Destructive-style logout card (red border)
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal = 16.dp,
                            vertical = 6.dp
                        )
                        .clickable { showConfirmLogout = true }
                        .border(
                            width = 1.dp,
                            color = Color.Red.copy(alpha = 0.9f),
                            shape = RoundedCornerShape(20.dp)
                        ),

                    shape = RoundedCornerShape(20.dp),

                    elevation = CardDefaults.cardElevation(
                        defaultElevation = 3.dp
                    )
                ) {

                    ListItem(

                        headlineContent = {

                            Text(
                                text = "Logout",
                                fontWeight = FontWeight.SemiBold,
                                color = Color.Red
                            )
                        },

                        leadingContent = {

                            Surface(

                                shape = CircleShape,

                                color = Color.Red.copy(alpha = 0.12f)

                            ) {

                                Icon(
                                    imageVector = Icons.Default.ExitToApp,
                                    contentDescription = null,

                                    modifier = Modifier.padding(10.dp),

                                    tint = Color.Red
                                )
                            }
                        },

                        trailingContent = {

                            Icon(
                                Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = Color.LightGray
                            )
                        }
                    )
                }

                if (showConfirmLogout) {
                    AlertDialog(
                        onDismissRequest = { showConfirmLogout = false },
                        title = { Text("Logout") },
                        text = { Text("Are you sure you want to logout?") },
                        confirmButton = {
                            Button(onClick = {
                                showConfirmLogout = false
                                viewModel.logout()
                                Toast.makeText(context, "Logged out", Toast.LENGTH_SHORT).show()
                                onBack()
                            }) { Text("Yes") }
                        },
                        dismissButton = {
                            TextButton(onClick = { showConfirmLogout = false }) { Text("Cancel") }
                        }
                    )
                }

                // removed Add Info dialog and related code as requested

                Spacer(modifier = Modifier.height(24.dp))
            }
        }

        if (showEditNameDialog) {

            AlertDialog(

                onDismissRequest = {
                    showEditNameDialog = false
                },

                title = {
                    Text(
                        text = "Edit Display Name",
                        fontWeight = FontWeight.Bold
                    )
                },

                text = {

                    OutlinedTextField(

                        value = newName,

                        onValueChange = {
                            newName = it
                        },

                        modifier = Modifier.fillMaxWidth(),

                        label = {
                            Text("New Name")
                        },

                        singleLine = true
                    )
                },

                confirmButton = {

                    Button(

                        onClick = {

                            if (newName.isBlank()) {

                                Toast.makeText(
                                    context,
                                    "Name cannot be empty",
                                    Toast.LENGTH_SHORT
                                ).show()

                                return@Button
                            }

                            viewModel.updateDisplayName(

                                newName = newName,

                                onSuccess = {

                                    Toast.makeText(
                                        context,
                                        "Name updated successfully!",
                                        Toast.LENGTH_SHORT
                                    ).show()

                                    showEditNameDialog = false
                                },

                                onError = { error ->

                                    Toast.makeText(
                                        context,
                                        error,
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            )
                        }
                    ) {

                        Text("Save")
                    }
                },

                dismissButton = {

                    TextButton(
                        onClick = {
                            showEditNameDialog = false
                        }
                    ) {

                        Text("Cancel")
                    }
                }
            )
        }
    }
}

@Composable
fun ProfileOptionItem(

    title: String,

    value: String? = null,

    icon: ImageVector,

    onClick: () -> Unit
) {

    Card(

        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = 16.dp,
                vertical = 6.dp
            )
            .clickable {
                onClick()
            },

        shape = RoundedCornerShape(20.dp),

        elevation = CardDefaults.cardElevation(
            defaultElevation = 3.dp
        )

    ) {

        ListItem(

            headlineContent = {

                Text(
                    text = title,
                    fontWeight = FontWeight.SemiBold
                )
            },

            supportingContent = value?.let {
                {
                    Text(
                        text = it,
                        color = Color.Gray
                    )
                }
            },

            leadingContent = {

                Surface(

                    shape = CircleShape,

                    color =
                        MaterialTheme.colorScheme.primaryContainer

                ) {

                    Icon(
                        imageVector = icon,
                        contentDescription = null,

                        modifier = Modifier.padding(10.dp),

                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            },

            trailingContent = {

                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = Color.LightGray
                )
            }
        )
    }
}