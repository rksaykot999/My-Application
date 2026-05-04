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
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.firebase.appdistribution.FirebaseAppDistribution
import com.google.firebase.messaging.FirebaseMessaging
import com.rksaykot.myapplication.ui.screens.*
import com.rksaykot.myapplication.ui.screens.auth.LoginScreen
import com.rksaykot.myapplication.ui.theme.MyApplicationTheme
import com.rksaykot.myapplication.viewmodel.ChatViewModel
import com.rksaykot.myapplication.viewmodel.ThemeViewModel

class MainActivity : ComponentActivity() {

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean -> }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        createNotificationChannel()
        askNotificationPermission()
        
        setContent {
            val themeViewModel: ThemeViewModel = viewModel()
            val chatViewModel: ChatViewModel = viewModel()
            val lifecycleOwner = LocalLifecycleOwner.current

            DisposableEffect(lifecycleOwner) {
                val observer = LifecycleEventObserver { _, event ->
                    when (event) {
                        Lifecycle.Event.ON_START -> chatViewModel.setUserOnline(true)
                        Lifecycle.Event.ON_STOP -> chatViewModel.setUserOnline(false)
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
                        chatViewModel.updateFcmToken(task.result)
                    }
                }
            }

            MyApplicationTheme(darkTheme = themeViewModel.isDarkMode) {
                AppNavigation(chatViewModel, themeViewModel)
            }
        }
    }

    private fun askNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelId = "chat_messages"
            val name = "Chat Messages"
            val descriptionText = "Notifications for new chat messages"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(channelId, name, importance).apply {
                description = descriptionText
                enableLights(true)
                enableVibration(true)
            }
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}


@Composable
fun AppNavigation(viewModel: ChatViewModel, themeViewModel: ThemeViewModel) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "splash") {
        composable("splash") {
            SplashScreen(onNext = {
                val nextDest = if (viewModel.currentUser != null) "home" else "login"
                navController.navigate(nextDest) {
                    popUpTo("splash") { inclusive = true }
                }
            })
        }
        composable("login") {
            LoginScreen(onLoginSuccess = {
                navController.navigate("home") {
                    popUpTo("login") { inclusive = true }
                }
            })
        }
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
                onSettingsClick = {
                    navController.navigate("settings")
                },
                onProfileClick = {
                    navController.navigate("profile")
                },
                viewModel = viewModel
            )
        }
        composable("profile") {
            ProfileScreen(onBack = { navController.popBackStack() })
        }
        composable("settings") {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onNavigateToPrivacy = { navController.navigate("privacy") },
                onNavigateToLanguage = { navController.navigate("language") },
                themeViewModel = themeViewModel
            )
        }
        composable("privacy") {
            PrivacyPolicyScreen(onBack = { navController.popBackStack() })
        }
        composable("language") {
            LanguageScreen(onBack = { navController.popBackStack() })
        }
        composable("chat/{roomId}/{displayName}") { backStackEntry ->
            val roomId = backStackEntry.arguments?.getString("roomId") ?: "default_room"
            val displayName = backStackEntry.arguments?.getString("displayName") ?: "Chat"
            ChatScreen(
                roomId = roomId,
                displayName = displayName,
                onBack = { navController.popBackStack() },
                onDetailsClick = { 
                    navController.navigate("userDetail")
                },
                viewModel = viewModel
            )
        }
        composable("userDetail") {
            UserDetailScreen(
                user = viewModel.selectedUserStatus,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
