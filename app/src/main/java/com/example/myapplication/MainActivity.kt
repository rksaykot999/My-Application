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
import com.rksaykot.myapplication.ui.screens.SplashScreen
import com.rksaykot.myapplication.ui.screens.auth.LoginScreen
import com.rksaykot.myapplication.ui.theme.MyApplicationTheme
import com.rksaykot.myapplication.viewmodel.ChatViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                val viewModel: ChatViewModel = viewModel()
                AppNavigation(viewModel)
            }
        }
    }
}

@Composable
fun AppNavigation(viewModel: ChatViewModel) {
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
                viewModel = viewModel
            )
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
