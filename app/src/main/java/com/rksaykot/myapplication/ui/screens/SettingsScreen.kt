package com.rksaykot.myapplication.ui.screens

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rksaykot.myapplication.viewmodel.ThemeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onNavigateToPrivacy: () -> Unit,
    onNavigateToLanguage: () -> Unit,
    themeViewModel: ThemeViewModel = viewModel()
) {
    var notificationsEnabled by remember { mutableStateOf(true) }
    var soundEnabled by remember { mutableStateOf(true) }
    var showVersionDialog by remember { mutableStateOf(false) }
    
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
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
                .padding(innerPadding)
        ) {
            item {
                SettingsHeader("Appearance")
                SettingsSwitchItem(
                    title = "Dark Mode",
                    icon = Icons.Default.DarkMode,
                    checked = themeViewModel.isDarkMode,
                    onCheckedChange = { themeViewModel.toggleTheme() }
                )
            }

            item {
                SettingsHeader("Notifications")
                SettingsSwitchItem(
                    title = "Enable Notifications",
                    icon = Icons.Default.Notifications,
                    checked = notificationsEnabled,
                    onCheckedChange = { notificationsEnabled = it }
                )
                SettingsSwitchItem(
                    title = "Sound",
                    icon = Icons.AutoMirrored.Filled.VolumeUp,
                    checked = soundEnabled,
                    onCheckedChange = { soundEnabled = it },
                    enabled = notificationsEnabled
                )
            }

            item {
                SettingsHeader("System & Privacy")
                SettingsClickItem(
                    title = "Privacy Policy",
                    icon = Icons.Default.PrivacyTip,
                    onClick = onNavigateToPrivacy
                )
                SettingsClickItem(
                    title = "App Language",
                    icon = Icons.Default.Language,
                    onClick = onNavigateToLanguage
                )
                SettingsClickItem(
                    title = "Clear Cache",
                    icon = Icons.Default.DeleteSweep,
                    onClick = { 
                        Toast.makeText(context, "Cache cleared successfully!", Toast.LENGTH_SHORT).show()
                    }
                )
            }

            item {
                SettingsHeader("About")
                SettingsClickItem(
                    title = "App Version",
                    subtitle = "1.1 (Build 2)",
                    icon = Icons.Default.Info,
                    onClick = { showVersionDialog = true }
                )
            }
        }

        if (showVersionDialog) {
            AlertDialog(
                onDismissRequest = { showVersionDialog = false },
                title = { Text("App Update Info") },
                text = {
                    Column {
                        Text("A new version might be available with better performance and new features.")
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Latest Version: 1.1 (2)", fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Download Latest APK",
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.clickable { 
                                uriHandler.openUri("https://drive.google.com/drive/folders/1NoSBuYJNoy-RCSNBXjJQStKQhvAVCl8t?usp=drive_link")
                            }
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showVersionDialog = false }) { Text("Close") }
                }
            )
        }
    }
}

@Composable
fun SettingsHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp)
    )
}

@Composable
fun SettingsSwitchItem(
    title: String,
    icon: ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true
) {
    ListItem(
        headlineContent = { Text(title) },
        leadingContent = { Icon(icon, contentDescription = null) },
        trailingContent = {
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                enabled = enabled
            )
        },
        modifier = Modifier.clickable(enabled = enabled) { onCheckedChange(!checked) }
    )
}

@Composable
fun SettingsClickItem(
    title: String,
    subtitle: String? = null,
    icon: ImageVector,
    onClick: () -> Unit
) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = subtitle?.let { { Text(it) } },
        leadingContent = { Icon(icon, contentDescription = null) },
        trailingContent = { Icon(Icons.Default.ChevronRight, contentDescription = null) },
        modifier = Modifier.clickable { onClick() }
    )
}
