package com.rksaykot.myapplication

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
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
import com.rksaykot.myapplication.ui.screens.*
import com.rksaykot.myapplication.ui.screens.auth.LoginScreen
import com.rksaykot.myapplication.ui.theme.MyApplicationTheme
import com.rksaykot.myapplication.viewmodel.ChatViewModel
import com.rksaykot.myapplication.viewmodel.ThemeViewModel

class MainActivity : ComponentActivity() {

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        // Handle permission result if needed
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Initialize notification channel for messages
        createNotificationChannel()

        // Ask for runtime notification permission on Android 13+
        askNotificationPermission()

        setContent {
            val themeViewModel: ThemeViewModel = viewModel()
            val chatViewModel: ChatViewModel = viewModel()
            val lifecycleOwner = LocalLifecycleOwner.current

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

            LaunchedEffect(Unit) {
                FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val token = task.result
                        chatViewModel.updateFcmToken(token)
                    }
                }
            }

            MyApplicationTheme(darkTheme = themeViewModel.isDarkMode) {
                AppNavigation(
                    viewModel = chatViewModel,
                    themeViewModel = themeViewModel
                )
            }
        }
    }

    private fun askNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "chat_messages",
                "Chat Messages",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for new chat messages"
                enableLights(true)
                enableVibration(true)
            }
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
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
        // Splash Screen route
        composable("splash") {
            SplashScreen(
                onNext = {
                    val nextDest = if (viewModel.currentUser != null) "home" else "login"
                    navController.navigate(nextDest) {
                        popUpTo("splash") { inclusive = true }
                    }
                }
            )
        }

        // Login Screen route
        composable("login") {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate("home") {
                        popUpTo("login") { inclusive = true }
                    }
                },
                viewModel = viewModel
            )
        }

        // Home Screen route
        composable("home") {
            HomeScreen(
                onContactClick = { roomId, displayName ->
                    navController.navigate("chat/$roomId/$displayName")
                },
                onLogout = {
                    navController.navigate("login") {
                        popUpTo("home") { inclusive = true }
                    }
                },
                onSettingsClick = { navController.navigate("settings") },
                onProfileClick = { navController.navigate("profile") },
                viewModel = viewModel
            )
        }

        composable("profile") {
            ProfileScreen(
                onBack = { navController.popBackStack() },
                onLogoutSuccess = {
                    navController.navigate("login") {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onNavigateToPrivacy = { navController.navigate("privacy") },
                viewModel = viewModel
            )
        }

        composable("settings") {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onNavigateToPrivacy = { navController.navigate("privacy") },
                themeViewModel = themeViewModel
            )
        }

        composable("privacy") {
            PrivacyPolicyScreen(onBack = { navController.popBackStack() })
        }


        composable("chat/{roomId}/{displayName}") { backStackEntry ->
            val roomId = backStackEntry.arguments?.getString("roomId") ?: "default_room"
            val displayName = backStackEntry.arguments?.getString("displayName") ?: "Chat"

            ChatScreen(
                roomId = roomId,
                displayName = displayName,
                onBack = { navController.popBackStack() },
                onDetailsClick = { navController.navigate("userDetail") },
                viewModel = viewModel
            )
        }

        // User Details route
        composable("userDetail") {
            UserDetailScreen(
                user = viewModel.selectedUserStatus,
                onBack = { navController.popBackStack() }
            )
        }
    }
}