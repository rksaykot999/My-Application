package com.rksaykot.myapplication.ui.screens.auth

import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rksaykot.myapplication.R
import com.rksaykot.myapplication.viewmodel.ChatViewModel

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    viewModel: ChatViewModel = viewModel()
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var isSignUp by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    val primaryColor = MaterialTheme.colorScheme.primary
    val gradient = Brush.verticalGradient(
        colors = listOf(primaryColor.copy(alpha = 0.8f), primaryColor)
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        // Top Decorative Background
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp)
                .clip(RoundedCornerShape(bottomStart = 60.dp, bottomEnd = 60.dp))
                .background(gradient)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 30.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(60.dp))
            
            // App Logo and Title
            Surface(
                modifier = Modifier
                    .size(100.dp)
                    .shadow(10.dp, CircleShape)
                    .clip(CircleShape),
                color = Color.White
            ) {
                Image(
                    painter = painterResource(id = R.drawable.app_logo),
                    contentDescription = "App Logo",
                    modifier = Modifier.padding(15.dp),
                    contentScale = ContentScale.Fit
                )
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            Text(
                text = if (isSignUp) "Create Account" else "Welcome Back",
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White
            )
            Text(
                text = if (isSignUp) "Join our secret messaging space" else "Sign in to catch up with friends",
                fontSize = 14.sp,
                color = Color.White.copy(alpha = 0.8f),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(40.dp))

            // Input Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(15.dp, RoundedCornerShape(25.dp)),
                shape = RoundedCornerShape(25.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(25.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (isSignUp) {
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text("Full Name") },
                            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(15.dp),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.height(15.dp))
                    }

                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email Address") },
                        leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(15.dp),
                        singleLine = true
                    )
                    
                    Spacer(modifier = Modifier.height(15.dp))
                    
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Password") },
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth(),
                        visualTransformation = PasswordVisualTransformation(),
                        shape = RoundedCornerShape(15.dp),
                        singleLine = true
                    )

                    if (errorMessage != null) {
                        Text(
                            text = errorMessage!!,
                            color = Color.Red,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(top = 10.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(30.dp))
                    
                    Button(
                        onClick = {
                            val trimmedEmail = email.trim()
                            val trimmedPassword = password.trim()
                            isLoading = true
                            errorMessage = null
                            if (isSignUp) {
                                viewModel.signUp(trimmedEmail, trimmedPassword, name, 
                                    onSuccess = { 
                                        isLoading = false
                                        onLoginSuccess() 
                                    },
                                    onError = { 
                                        isLoading = false
                                        errorMessage = it 
                                    }
                                )
                            } else {
                                viewModel.login(trimmedEmail, trimmedPassword,
                                    onSuccess = { 
                                        isLoading = false
                                        onLoginSuccess() 
                                    },
                                    onError = { 
                                        isLoading = false
                                        errorMessage = it 
                                    }
                                )
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(55.dp),
                        enabled = !isLoading,
                        shape = RoundedCornerShape(15.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = primaryColor)
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White, strokeWidth = 2.dp)
                        } else {
                            Text(
                                text = if (isSignUp) "SIGN UP" else "LOGIN",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            TextButton(
                onClick = { isSignUp = !isSignUp },
                modifier = Modifier.padding(bottom = 30.dp)
            ) {
                Text(
                    text = if (isSignUp) "Already have an account? Login" else "Don't have an account? Sign Up",
                    color = primaryColor,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
