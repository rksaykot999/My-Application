package com.rksaykot.myapplication

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.rksaykot.myapplication.ui.screens.ChatScreen
import com.rksaykot.myapplication.ui.screens.HomeScreen
import com.rksaykot.myapplication.ui.screens.LanguageScreen
import com.rksaykot.myapplication.ui.screens.PrivacyPolicyScreen
import com.rksaykot.myapplication.ui.screens.ProfileScreen
import com.rksaykot.myapplication.ui.screens.SettingsScreen
import com.rksaykot.myapplication.ui.screens.SplashScreen
import com.rksaykot.myapplication.ui.screens.auth.LoginScreen
import com.rksaykot.myapplication.ui.theme.MyApplicationTheme
import com.rksaykot.myapplication.viewmodel.ChatViewModel
import com.rksaykot.myapplication.viewmodel.ThemeViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val themeViewModel: ThemeViewModel = viewModel()
            MyApplicationTheme(darkTheme = themeViewModel.isDarkMode) {
                val viewModel: ChatViewModel = viewModel()
                AppNavigation(viewModel, themeViewModel)
            }
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
                viewModel = viewModel
            )
        }
    }
}
