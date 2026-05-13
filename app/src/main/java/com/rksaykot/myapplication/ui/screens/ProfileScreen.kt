package com.rksaykot.myapplication.ui.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.rksaykot.myapplication.viewmodel.ChatViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onBack: () -> Unit,
    onLogoutSuccess: () -> Unit = {},
    viewModel: ChatViewModel = viewModel(),
    onNavigateToPrivacy: () -> Unit
) {
    val user = viewModel.currentUser
    val context = LocalContext.current

    // Dialog States
    var showEditNameDialog by remember { mutableStateOf(false) }
    var showEditPhoneDialog by remember { mutableStateOf(false) }
    var showEditGenderDialog by remember { mutableStateOf(false) }
    var showConfirmLogout by remember { mutableStateOf(false) }
    var showConfirmDelete by remember { mutableStateOf(false) }

    // Input States
    var newName by remember(user?.displayName) { mutableStateOf(user?.displayName ?: "") }
    var newPhone by remember(user?.phoneNumber) { mutableStateOf(user?.phoneNumber ?: "") }
    var selectedGender by remember(user?.gender) { mutableStateOf(user?.gender ?: "Male") }

    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = { Text("Profile", fontWeight = FontWeight.ExtraBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        if (user == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(bottom = 32.dp)
            ) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(contentAlignment = Alignment.BottomEnd) {
                            Surface(
                                modifier = Modifier
                                    .size(120.dp)
                                    .shadow(12.dp, CircleShape),
                                shape = CircleShape,
                                color = Color(0xFF546081),
                                border = BorderStroke(4.dp, MaterialTheme.colorScheme.surface)
                            ) {
                                if (user.profileImageUrl.isNotEmpty()) {
                                    AsyncImage(
                                        model = user.profileImageUrl,
                                        contentDescription = "Profile Image",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier.fillMaxSize()
                                    ) {
                                        Icon(
                                            imageVector = if (user.gender.lowercase() == "female")
                                                Icons.Default.Woman
                                            else
                                                Icons.Default.Person,
                                            contentDescription = null,
                                            modifier = Modifier.size(72.dp),
                                            tint = Color(0xFFD1D9FF)
                                        )
                                    }
                                }
                            }

                            // Online Status Indicator
                            Surface(
                                modifier = Modifier.size(28.dp).offset(x = (-4).dp, y = (-4).dp),
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.surface,
                                tonalElevation = 2.dp
                            ) {
                                Box(
                                    modifier = Modifier
                                        .padding(4.dp)
                                        .fillMaxSize()
                                        .background(
                                            if (user.isOnline) Color(0xFF4CAF50) else Color.Gray,
                                            CircleShape
                                        )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = user.displayName.ifBlank { "User" },
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = user.email,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        val locale = LocalConfiguration.current.locale
                        val statusText = if (user.isOnline) {
                            "Active Now"
                        } else {
                            val format = SimpleDateFormat("MMM d, h:mm a", locale)
                            "Last seen: ${format.format(Date(user.lastSeen))}"
                        }

                        Text(
                            text = statusText,
                            style = MaterialTheme.typography.labelMedium,
                            color = if (user.isOnline) Color(0xFF4CAF50) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }
                }

                item {
                    ProfileSectionHeader("Personal Info")
                    ProfileGroup {
                        ProfileOptionItem(
                            title = "Display Name",
                            value = user.displayName.takeIf { it.isNotBlank() } ?: "Set name",
                            icon = Icons.Outlined.Person,
                            onClick = {
                                newName = user.displayName
                                showEditNameDialog = true
                            }
                        )
                        ProfileDivider()
                        ProfileOptionItem(
                            title = "Phone Number",
                            value = user.phoneNumber.takeIf { it.isNotBlank() } ?: "Not provided",
                            icon = Icons.Outlined.Phone,
                            onClick = {
                                newPhone = user.phoneNumber
                                showEditPhoneDialog = true
                            }
                        )
                        ProfileDivider()
                        ProfileOptionItem(
                            title = "Gender",
                            value = user.gender.takeIf { it.isNotBlank() }?.replaceFirstChar { it.uppercase() } ?: "Not specified",
                            icon = Icons.Outlined.Wc,
                            onClick = {
                                selectedGender = user.gender.ifBlank { "Male" }
                                showEditGenderDialog = true
                            }
                        )
                    }
                }

                item {
                    ProfileSectionHeader("Account Actions")
                    ProfileGroup {
                        ProfileOptionItem(
                            title = "Privacy Policy",
                            description = "How we handle your data",
                            icon = Icons.Outlined.Description,
                            onClick = onNavigateToPrivacy
                        )
                        ProfileDivider()
                        ProfileOptionItem(
                            title = "Logout",
                            icon = Icons.AutoMirrored.Filled.ExitToApp,
                            iconColor = MaterialTheme.colorScheme.error,
                            textColor = MaterialTheme.colorScheme.error,
                            onClick = { showConfirmLogout = true }
                        )
                        ProfileDivider()
                        ProfileOptionItem(
                            title = "Delete Account",
                            description = "Permanently remove your data",
                            icon = Icons.Outlined.DeleteForever,
                            iconColor = MaterialTheme.colorScheme.error,
                            textColor = MaterialTheme.colorScheme.error,
                            onClick = { showConfirmDelete = true }
                        )
                    }
                }
            }
        }
    }

    // Dialogs
    if (showEditNameDialog) {
        AlertDialog(
            onDismissRequest = { showEditNameDialog = false },
            title = { Text("Edit Display Name", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("New Name") },
                    shape = RoundedCornerShape(12.dp)
                )
            },
            confirmButton = {
                Button(onClick = {
                    if (newName.isNotBlank()) {
                        viewModel.updateDisplayName(newName, {
                            Toast.makeText(context, "Updated!", Toast.LENGTH_SHORT).show()
                            showEditNameDialog = false
                        }, { Toast.makeText(context, it, Toast.LENGTH_SHORT).show() })
                    }
                }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { showEditNameDialog = false }) { Text("Cancel") } }
        )
    }

    if (showEditPhoneDialog) {
        AlertDialog(
            onDismissRequest = { showEditPhoneDialog = false },
            title = { Text("Update Phone Number", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = newPhone,
                    onValueChange = { newPhone = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Phone Number") },
                    shape = RoundedCornerShape(12.dp)
                )
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.updateDisplayName(newPhone, {
                        Toast.makeText(context, "Phone updated!", Toast.LENGTH_SHORT).show()
                        showEditPhoneDialog = false
                    }, { Toast.makeText(context, it, Toast.LENGTH_SHORT).show() })
                }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { showEditPhoneDialog = false }) { Text("Cancel") } }
        )
    }

    if (showEditGenderDialog) {
        AlertDialog(
            onDismissRequest = { showEditGenderDialog = false },
            title = { Text("Select Gender", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    listOf("Male", "Female", "Other").forEach { gender ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedGender = gender }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = selectedGender == gender, onClick = { selectedGender = gender })
                            Text(gender, modifier = Modifier.padding(start = 8.dp))
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    showEditGenderDialog = false
                }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { showEditGenderDialog = false }) { Text("Cancel") } }
        )
    }

    if (showConfirmLogout) {
        AlertDialog(
            onDismissRequest = { showConfirmLogout = false },
            title = { Text("Logout?") },
            text = { Text("Are you sure you want to sign out?") },
            confirmButton = {
                Button(onClick = {
                    viewModel.logout()
                    onLogoutSuccess()
                }) { Text("Logout") }
            },
            dismissButton = { TextButton(onClick = { showConfirmLogout = false }) { Text("Cancel") } }
        )
    }

    if (showConfirmDelete) {
        AlertDialog(
            onDismissRequest = { showConfirmDelete = false },
            title = { Text("Delete Account?", color = MaterialTheme.colorScheme.error) },
            text = { Text("This action is permanent and cannot be undone. All your chats and data will be lost.") },
            confirmButton = {
                Button(
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    onClick = {
                        viewModel.deleteAccount({ onLogoutSuccess() }, { Toast.makeText(context, it, Toast.LENGTH_SHORT).show() })
                    }
                ) { Text("Delete Permanently") }
            },
            dismissButton = { TextButton(onClick = { showConfirmDelete = false }) { Text("Cancel") } }
        )
    }
}

@Composable
fun ProfileSectionHeader(title: String) {
    Text(
        text = title.uppercase(),
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 24.dp, top = 24.dp, bottom = 8.dp),
        letterSpacing = 1.2.sp
    )
}

@Composable
fun ProfileGroup(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
    ) {
        Column(content = content)
    }
}

@Composable
fun ProfileOptionItem(
    title: String,
    value: String? = null,
    description: String? = null,
    icon: ImageVector,
    iconColor: Color = MaterialTheme.colorScheme.primary,
    textColor: Color = MaterialTheme.colorScheme.onSurface,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier.size(40.dp),
            shape = CircleShape,
            color = iconColor.copy(alpha = 0.1f)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.padding(10.dp),
                tint = iconColor
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, color = textColor)
            if (!value.isNullOrBlank()) {
                Text(value, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else if (!description.isNullOrBlank()) {
                Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
fun ProfileDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(start = 72.dp),
        thickness = 0.5.dp,
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
    )
}
