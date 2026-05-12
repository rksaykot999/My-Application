package com.rksaykot.myapplication.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import android.content.Intent
import android.net.Uri
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.rksaykot.myapplication.viewmodel.ThemeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onNavigateToPrivacy: () -> Unit,
    themeViewModel: ThemeViewModel,
    onNavigateToLanguage: () -> Unit
) {
    var notificationEnabled by remember { mutableStateOf(true) }
    var soundEnabled by remember { mutableStateOf(true) }
    var vibrationEnabled by remember { mutableStateOf(true) }
    var headsUpEnabled by remember { mutableStateOf(true) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "Settings",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        val context = LocalContext.current
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Theme Section
            item {
                SettingsSectionTitle("Appearance")
            }
            item {
                SettingItem(
                    icon = "🌙",
                    title = "Dark Mode",
                    description = "Enable dark theme",
                    isToggle = true,
                    isEnabled = themeViewModel.isDarkMode,
                    onToggle = { themeViewModel.toggleTheme() }
                )
            }

            // Notification Settings
            item {
                SettingsSectionTitle("Notifications")
            }
            item {
                SettingItem(
                    icon = "🔔",
                    title = "Enable Notifications",
                    description = "Receive message notifications",
                    isToggle = true,
                    isEnabled = notificationEnabled,
                    onToggle = { notificationEnabled = it }
                )
            }
            item {
                SettingItem(
                    icon = "🔊",
                    title = "Notification Sound",
                    description = "Play sound with notifications",
                    isToggle = true,
                    isEnabled = soundEnabled,
                    onToggle = { soundEnabled = it },
                    enabled = notificationEnabled
                )
            }
            item {
                SettingItem(
                    icon = "📳",
                    title = "Vibration",
                    description = "Vibrate on new message",
                    isToggle = true,
                    isEnabled = vibrationEnabled,
                    onToggle = { vibrationEnabled = it },
                    enabled = notificationEnabled
                )
            }
            item {
                SettingItem(
                    icon = "⚡",
                    title = "Heads-Up Notifications",
                    description = "Show popup for incoming messages",
                    isToggle = true,
                    isEnabled = headsUpEnabled,
                    onToggle = { headsUpEnabled = it },
                    enabled = notificationEnabled
                )
            }

            // General Settings
            item {
                SettingsSectionTitle("General")
            }
            item {
                SettingItemClickable(
                    icon = "📜",
                    title = "Privacy Policy",
                    description = "Read our privacy policy",
                    onClick = onNavigateToPrivacy
                )
            }

            // App Info
            item {
                SettingsSectionTitle("About")
            }
            item {
                SettingItem(
                    icon = "ℹ️",
                    title = "App Version",
                    description = "2.9.0",
                    isClickable = false
                )
            }
            item {
                SettingItemClickable(
                    icon = "⬇️",
                    title = "Download latest version",
                    description = "Get the newest release",
                    onClick = {
                        // open external link
                        val intent = Intent(Intent.ACTION_VIEW)
                        intent.data = Uri.parse("https://drive.google.com/drive/folders/1NoSBuYJNoy-RCSNBXjJQStKQhvAVCl8t?usp=sharing")
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        context.startActivity(intent)
                    }
                )
            }
        }
    }
}

@Composable
fun SettingsSectionTitle(title: String) {
    Text(
        title,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
    )
}

@Composable
fun SettingItem(
    icon: String,
    title: String,
    description: String,
    isToggle: Boolean = false,
    isEnabled: Boolean = true,
    enabled: Boolean = true,
    isClickable: Boolean = true,
    onToggle: ((Boolean) -> Unit)? = null,
    onClick: (() -> Unit)? = null
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(icon, style = MaterialTheme.typography.headlineMedium)
                Column {
                    Text(
                        title,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (isToggle) {
                Switch(
                    checked = isEnabled,
                    onCheckedChange = { onToggle?.invoke(it) },
                    enabled = enabled
                )
            }
        }
    }
}

@Composable
fun SettingItemClickable(
    icon: String,
    title: String,
    description: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .clickable { onClick() },
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(icon, style = MaterialTheme.typography.headlineMedium)
                Column {
                    Text(
                        title,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Text("→", style = MaterialTheme.typography.titleLarge)
        }
    }
}
