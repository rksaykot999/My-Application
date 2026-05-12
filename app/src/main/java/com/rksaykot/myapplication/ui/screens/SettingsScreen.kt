package com.rksaykot.myapplication.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import com.rksaykot.myapplication.viewmodel.ThemeViewModel
import com.rksaykot.myapplication.viewmodel.ThemeOption

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onNavigateToPrivacy: () -> Unit,
    themeViewModel: ThemeViewModel,
    onNavigateToLanguage: () -> Unit = {}
) {
    var notificationEnabled by remember { mutableStateOf(true) }
    var soundEnabled by remember { mutableStateOf(true) }
    var vibrationEnabled by remember { mutableStateOf(true) }
    val context = LocalContext.current

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "Settings",
                        fontWeight = FontWeight.ExtraBold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            item {
                SettingsSectionHeader("Appearance")
                SettingsGroup {
                    ThemeSelectorItem(
                        selectedOption = themeViewModel.selectedOption,
                        onOptionSelected = { themeViewModel.setThemeOption(it) }
                    )
                }
            }

            item {
                SettingsSectionHeader("Notifications")
                SettingsGroup {
                    SettingsSwitchItem(
                        icon = Icons.Outlined.Notifications,
                        title = "Enable Notifications",
                        description = "Receive important updates and alerts",
                        checked = notificationEnabled,
                        onCheckedChange = { notificationEnabled = it }
                    )
                    HorizontalDivider(modifier = Modifier.padding(start = 56.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                    SettingsSwitchItem(
                        icon = Icons.Outlined.VolumeUp,
                        title = "Notification Sound",
                        description = "Play sound with notifications",
                        checked = soundEnabled,
                        onCheckedChange = { soundChanged ->
                            soundEnabled = soundChanged
                        },
                        enabled = notificationEnabled
                    )
                    HorizontalDivider(modifier = Modifier.padding(start = 56.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                    SettingsSwitchItem(
                        icon = Icons.Outlined.Vibration,
                        title = "Vibration",
                        description = "Feel haptic feedback for alerts",
                        checked = vibrationEnabled,
                        onCheckedChange = { vibrationEnabled = it },
                        enabled = notificationEnabled
                    )
                }
            }

            item {
                SettingsSectionHeader("Legal & Info")
                SettingsGroup {
                    SettingsClickableItem(
                        icon = Icons.Outlined.Description,
                        title = "Privacy Policy",
                        description = "How we handle your data",
                        onClick = onNavigateToPrivacy
                    )
                    HorizontalDivider(modifier = Modifier.padding(start = 56.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                    SettingsClickableItem(
                        icon = Icons.Outlined.Download,
                        title = "Check for Updates",
                        description = "Current version: 3.0.0",
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW)
                            intent.data = "https://drive.google.com/drive/folders/1NoSBuYJNoy-RCSNBXjJQStKQhvAVCl8t?usp=sharing".toUri()
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            context.startActivity(intent)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun ThemeSelectorItem(
    selectedOption: ThemeOption,
    onOptionSelected: (ThemeOption) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Outlined.Palette,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text("Overall appearance", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                Text("Adapts theme to your preference", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Segmented Control (matches image_d21655.png style)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            ThemeOption.entries.forEach { option ->
                val isSelected = selectedOption == option
                val backgroundColor by animateColorAsState(
                    if (isSelected) MaterialTheme.colorScheme.surface else Color.Transparent,
                    label = "bg"
                )
                val contentColor by animateColorAsState(
                    if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    label = "content"
                )
                val elevation by animateDpAsState(if (isSelected) 2.dp else 0.dp, label = "elev")

                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    shape = RoundedCornerShape(8.dp),
                    color = backgroundColor,
                    tonalElevation = elevation,
                    onClick = { onOptionSelected(option) }
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = when(option) {
                                ThemeOption.SYSTEM -> Icons.Default.SettingsSuggest
                                ThemeOption.LIGHT -> Icons.Default.LightMode
                                ThemeOption.DARK -> Icons.Default.DarkMode
                            },
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = contentColor
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = option.name.lowercase().replaceFirstChar { it.uppercase() },
                            style = MaterialTheme.typography.labelLarge,
                            color = contentColor,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsSectionHeader(title: String) {
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
fun SettingsGroup(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
        border = BorderStroke(
            width = 0.5.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)
        )
    ) {
        Column(content = content)
    }
}

@Composable
fun SettingsSwitchItem(
    icon: ImageVector,
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f),
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
            )
            Text(
                description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled
        )
    }
}

@Composable
fun SettingsClickableItem(
    icon: ImageVector,
    title: String,
    description: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
            Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        )
    }
}