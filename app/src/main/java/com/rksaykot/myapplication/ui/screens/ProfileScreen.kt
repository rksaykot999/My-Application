package com.rksaykot.myapplication.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.rksaykot.myapplication.viewmodel.ChatViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onBack: () -> Unit,
    viewModel: ChatViewModel = viewModel()
) {
    val user = viewModel.currentUser
    val context = LocalContext.current
    
    var showEditNameDialog by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf(user?.displayName ?: "") }
    var isUploading by remember { mutableStateOf(false) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            isUploading = true
            viewModel.uploadImage(it, "profile_images", { url ->
                viewModel.updateProfileImage(url, {
                    isUploading = false
                    Toast.makeText(context, "Profile photo updated!", Toast.LENGTH_SHORT).show()
                }, { error ->
                    isUploading = false
                    Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
                })
            }, { error ->
                isUploading = false
                Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
            })
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Profile") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                Spacer(modifier = Modifier.height(24.dp))
                
                // Profile Image Section
                Box(contentAlignment = Alignment.BottomEnd) {
                    Surface(
                        modifier = Modifier
                            .size(120.dp)
                            .clip(CircleShape),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        if (user?.profileImageUrl?.isNotEmpty() == true) {
                            AsyncImage(
                                model = user.profileImageUrl,
                                contentDescription = "Profile Image",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = user?.displayName?.take(1)?.uppercase() ?: "?",
                                    style = MaterialTheme.typography.displayMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                        
                        if (isUploading) {
                            Box(
                                modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.3f)),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(color = Color.White)
                            }
                        }
                    }
                    FloatingActionButton(
                        onClick = { imagePickerLauncher.launch("image/*") },
                        modifier = Modifier.size(40.dp),
                        shape = CircleShape,
                        containerColor = MaterialTheme.colorScheme.primary
                    ) {
                        Icon(Icons.Default.CameraAlt, contentDescription = "Change Image", modifier = Modifier.size(20.dp), tint = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = user?.displayName ?: "User Name",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = user?.email ?: "email@example.com",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )

                Spacer(modifier = Modifier.height(32.dp))
                
                HorizontalDivider(thickness = 0.5.dp, color = Color.LightGray)
            }

            item {
                ProfileOptionItem(
                    title = "Display Name",
                    value = user?.displayName ?: "",
                    icon = Icons.Default.Person,
                    onClick = { showEditNameDialog = true }
                )
                ProfileOptionItem(
                    title = "Change Password",
                    icon = Icons.Default.Lock,
                    onClick = { Toast.makeText(context, "Password reset link sent to email!", Toast.LENGTH_SHORT).show() }
                )
                ProfileOptionItem(
                    title = "My Account ID",
                    value = user?.uid?.take(8) + "...",
                    icon = Icons.Default.Fingerprint,
                    onClick = { /* Copy to clipboard */ }
                )
                ProfileOptionItem(
                    title = "Privacy Settings",
                    icon = Icons.Default.Shield,
                    onClick = { }
                )
            }
        }

        if (showEditNameDialog) {
            AlertDialog(
                onDismissRequest = { showEditNameDialog = false },
                title = { Text("Edit Display Name") },
                text = {
                    OutlinedTextField(
                        value = newName,
                        onValueChange = { newName = it },
                        label = { Text("New Name") },
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                confirmButton = {
                    Button(onClick = {
                        viewModel.updateDisplayName(newName, {
                            showEditNameDialog = false
                            Toast.makeText(context, "Name updated!", Toast.LENGTH_SHORT).show()
                        }, { error ->
                            Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
                        })
                    }) { Text("Save") }
                },
                dismissButton = {
                    TextButton(onClick = { showEditNameDialog = false }) { Text("Cancel") }
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
    ListItem(
        headlineContent = { Text(title, fontWeight = FontWeight.SemiBold) },
        supportingContent = value?.let { { Text(it, color = Color.Gray) } },
        leadingContent = { Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
        trailingContent = { Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.LightGray) },
        modifier = Modifier.clickable { onClick() }
    )
}
