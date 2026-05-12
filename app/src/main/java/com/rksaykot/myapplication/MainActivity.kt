package com.rksaykot.myapplication

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.firebase.messaging.FirebaseMessaging
import com.rksaykot.myapplication.service.ChatForegroundService
import com.rksaykot.myapplication.ui.screens.ChatScreen
import com.rksaykot.myapplication.ui.screens.HomeScreen
import com.rksaykot.myapplication.ui.screens.LanguageScreen
import com.rksaykot.myapplication.ui.screens.PrivacyPolicyScreen
import com.rksaykot.myapplication.ui.screens.ProfileScreen
import com.rksaykot.myapplication.ui.screens.SettingsScreen
import com.rksaykot.myapplication.ui.screens.SplashScreen
import com.rksaykot.myapplication.ui.screens.UserDetailScreen
import com.rksaykot.myapplication.ui.screens.auth.LoginScreen
import com.rksaykot.myapplication.ui.theme.MyApplicationTheme
import com.rksaykot.myapplication.viewmodel.ChatViewModel
import com.rksaykot.myapplication.viewmodel.ThemeViewModel

class MainActivity : ComponentActivity() {

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        // Create Notification Channel
        createNotificationChannel()

        // Ask Notification Permission
        askNotificationPermission()

        setContent {

            val themeViewModel: ThemeViewModel = viewModel()
            val chatViewModel: ChatViewModel = viewModel()

            val lifecycleOwner = LocalLifecycleOwner.current

            // User Online Offline Status
            DisposableEffect(lifecycleOwner) {

                val observer = LifecycleEventObserver { _, event ->

                    when (event) {

                        Lifecycle.Event.ON_START -> {
                            chatViewModel.setUserOnline(true)
                        }

                        Lifecycle.Event.ON_STOP -> {
                            chatViewModel.setUserOnline(false)
                        }

                        else -> {}
                    }
                }

                lifecycleOwner.lifecycle.addObserver(observer)

                onDispose {
                    lifecycleOwner.lifecycle.removeObserver(observer)
                }
            }

            // Firebase FCM Token
            LaunchedEffect(Unit) {

                FirebaseMessaging.getInstance().token
                    .addOnCompleteListener { task ->

                        if (task.isSuccessful) {

                            val token = task.result

                            chatViewModel.updateFcmToken(token)
                        }
                    }
            }

            MyApplicationTheme(
                darkTheme = themeViewModel.isDarkMode
            ) {

                AppNavigation(
                    viewModel = chatViewModel,
                    themeViewModel = themeViewModel
                )
            }
        }
    }

    // Notification Permission
    private fun askNotificationPermission() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {

            if (
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {

                requestPermissionLauncher.launch(
                    Manifest.permission.POST_NOTIFICATIONS
                )
            }
        }
    }

    // Battery Optimization Disable
    private fun askBatteryOptimizationPermission() {

        try {

            val intent = Intent()

            intent.action =
                Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS

            intent.data =
                Uri.parse("package:$packageName")

            startActivity(intent)

        } catch (e: Exception) {

            e.printStackTrace()
        }
    }

    // Start Foreground Service
    private fun startChatService() {

        val serviceIntent =
            Intent(this, ChatForegroundService::class.java)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            startForegroundService(serviceIntent)

        } else {

            startService(serviceIntent)
        }
    }

    // Notification Channel
    private fun createNotificationChannel() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            val channelId = "chat_messages"

            val name = "Chat Messages"

            val descriptionText =
                "Notifications for new chat messages"

            val importance =
                NotificationManager.IMPORTANCE_HIGH

            val channel = NotificationChannel(
                channelId,
                name,
                importance
            ).apply {

                description = descriptionText

                enableLights(true)

                enableVibration(true)
            }

            val notificationManager =
                getSystemService(
                    Context.NOTIFICATION_SERVICE
                ) as NotificationManager

            notificationManager.createNotificationChannel(
                channel
            )
        }
    }
}

@Composable
fun AppNavigation(
    viewModel: ChatViewModel,
    themeViewModel: ThemeViewModel
) {

    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "splash"
    ) {

        // Splash Screen
        composable("splash") {

            SplashScreen(

                onNext = {

                    val nextDest =
                        if (viewModel.currentUser != null)
                            "home"
                        else
                            "login"

                    navController.navigate(nextDest) {

                        popUpTo("splash") {
                            inclusive = true
                        }
                    }
                }
            )
        }

        // Login Screen
        composable("login") {

            LoginScreen(

                onLoginSuccess = {

                    navController.navigate("home") {

                        popUpTo("login") {
                            inclusive = true
                        }
                    }
                }
            )
        }

        // Home Screen
        composable("home") {

            HomeScreen(

                onContactClick = { roomId, displayName ->

                    navController.navigate(
                        "chat/$roomId/$displayName"
                    )
                },

                onLogout = {

                    navController.navigate("login") {

                        popUpTo("home") {
                            inclusive = true
                        }
                    }
                },

                onSettingsClick = {

                    navController.navigate("settings")
                },

                onProfileClick = {

                    navController.navigate("profile")
                },

                viewModel = viewModel
            )
        }

        // Profile
        composable("profile") {

            ProfileScreen(

                onBack = {
                    navController.popBackStack()
                },

                onNavigateToPrivacy = {
                    navController.navigate("privacy")
                }
            )
        }

        // Settings
        composable("settings") {

            SettingsScreen(

                onBack = {
                    navController.popBackStack()
                },

                onNavigateToPrivacy = {

                    navController.navigate("privacy")
                },

                onNavigateToLanguage = {

                    navController.navigate("language")
                },

                themeViewModel = themeViewModel
            )
        }

        // Privacy
        composable("privacy") {

            PrivacyPolicyScreen(

                onBack = {
                    navController.popBackStack()
                }
            )
        }

        // Language
        composable("language") {

            LanguageScreen(

                onBack = {
                    navController.popBackStack()
                }
            )
        }

        // Chat Screen
        composable(
            "chat/{roomId}/{displayName}"
        ) { backStackEntry ->

            val roomId =
                backStackEntry.arguments
                    ?.getString("roomId")
                    ?: "default_room"

            val displayName =
                backStackEntry.arguments
                    ?.getString("displayName")
                    ?: "Chat"

            ChatScreen(

                roomId = roomId,

                displayName = displayName,

                onBack = {
                    navController.popBackStack()
                },

                onDetailsClick = {

                    navController.navigate("userDetail")
                },

                viewModel = viewModel
            )
        }

        // User Details
        composable("userDetail") {

            UserDetailScreen(

                user = viewModel.selectedUserStatus,

                onBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}
