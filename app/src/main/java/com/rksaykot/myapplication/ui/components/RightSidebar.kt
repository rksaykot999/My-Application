package com.rksaykot.myapplication.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.unit.LayoutDirection
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * A simple right-side overlay panel occupying ~50% width with options and a logout button at bottom.
 */
@Composable
fun RightSidebar(
    visible: Boolean,
    onClose: () -> Unit,
    onProfile: () -> Unit,
    onSettings: () -> Unit,
    onLogout: () -> Unit
) {
    val config = LocalConfiguration.current
    val screenWidth = config.screenWidthDp
    // use ~66% width as requested
    val widthDp = (screenWidth * 0.66).dp

    // localVisible controls internal animation state so we can play an exit animation before invoking callbacks
    var localVisible by remember { mutableStateOf(visible) }
    LaunchedEffect(visible) {
        localVisible = visible
    }

    // full-screen scrim with alignment to the right
    if (localVisible) {
        Box(modifier = Modifier.fillMaxSize()) {
            // scrim - clicking outside closes
            Box(modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.32f))
                .clickable {
                    // start exit animation
                    localVisible = false
                }
            )

            AnimatedVisibility(
                visible = localVisible,
                enter = slideInHorizontally(animationSpec = tween(280)) { fullWidth -> fullWidth },
                exit = slideOutHorizontally(animationSpec = tween(220)) { fullWidth -> fullWidth } + fadeOut(animationSpec = tween(220))
            ) {
                // Ensure layout direction is LTR so "end" aligns to right on all devices
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    Surface(
                        modifier = Modifier
                            .fillMaxHeight()
                            .width(widthDp)
                            .align(Alignment.CenterEnd),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Text("Menu", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f))
                                IconButton(onClick = {
                                    localVisible = false
                                }) {
                                    Icon(Icons.Default.ExitToApp, contentDescription = "Close")
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            val scope = rememberCoroutineScope()

                            // Options - animate close then call callbacks
                            SidebarItem(icon = Icons.Default.Person, label = "Profile", onClick = {
                                scope.launch {
                                    localVisible = false
                                    delay(260)
                                    onProfile()
                                    onClose()
                                }
                            })

                            SidebarItem(icon = Icons.Default.Settings, label = "Settings", onClick = {
                                scope.launch {
                                    localVisible = false
                                    delay(260)
                                    onSettings()
                                    onClose()
                                }
                            })

                            Spacer(modifier = Modifier.weight(1f))

                            // Logout bottom
                            Button(onClick = {
                                scope.launch {
                                    localVisible = false
                                    delay(260)
                                    onLogout()
                                    onClose()
                                }
                            }, modifier = Modifier.fillMaxWidth()) {
                                Icon(Icons.Default.ExitToApp, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Logout")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SidebarItem(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    Row(modifier = Modifier
        .fillMaxWidth()
        .padding(vertical = 8.dp)
        .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
        .padding(12.dp)
        .wrapContentHeight()
        , verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null)
        Spacer(modifier = Modifier.width(12.dp))
        Text(label, style = MaterialTheme.typography.bodyLarge)
    }
}

