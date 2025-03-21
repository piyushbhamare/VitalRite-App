package com.example.vitalrite_1.ui.auth

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.vitalrite_1.data.Repository
import androidx.compose.foundation.border
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun LoginScreen(navController: NavController) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var emailError by remember { mutableStateOf(false) }
    var passwordError by remember { mutableStateOf(false) }

    val primaryColor = Color(0xFF6200EA)
    val secondaryColor = Color(0xFFBB86FC)
    val backgroundGradient = Brush.verticalGradient(
        colors = listOf(Color(0xFFF5F7FA), Color(0xFFDDE5F0))
    )
    val errorColor = MaterialTheme.colorScheme.error

    val scope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundGradient)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 32.dp)
                .align(Alignment.Center)
                .widthIn(max = 400.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.LightGray)
                    .padding(bottom = 24.dp)
            )
            Text(
                "VitalRite",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp,
                    color = Color.Black
                ),
                modifier = Modifier.padding(bottom = 16.dp)
            )
            Text(
                "Welcome Back",
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.Medium,
                    fontSize = 16.sp,
                    color = Color.Gray
                ),
                modifier = Modifier.padding(bottom = 32.dp)
            )

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(8.dp, RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it; emailError = false },
                        leadingIcon = { Icon(Icons.Default.Email, contentDescription = "Email Icon", tint = if (emailError) errorColor else primaryColor) },
                        label = { Text("Email", color = if (emailError) errorColor else Color.Gray) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = if (emailError) errorColor else primaryColor,
                            unfocusedBorderColor = if (emailError) errorColor else Color.LightGray
                        ),
                        shape = RoundedCornerShape(8.dp)
                    )
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it; passwordError = false },
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = "Password Icon", tint = if (passwordError) errorColor else primaryColor) },
                        label = { Text("Password", color = if (passwordError) errorColor else Color.Gray) },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = if (passwordError) errorColor else primaryColor,
                            unfocusedBorderColor = if (passwordError) errorColor else Color.LightGray
                        ),
                        shape = RoundedCornerShape(8.dp)
                    )
                    Text(
                        "Forgot Password?",
                        color = primaryColor,
                        fontSize = 14.sp,
                        modifier = Modifier
                            .align(Alignment.End)
                            .padding(bottom = 16.dp)
                            .clickable { /* Navigate to Forgot Password screen */ }
                    )
                    Button(
                        onClick = {
                            if (!isLoading) {
                                emailError = email.isBlank() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
                                passwordError = password.isBlank() || password.length < 6
                                if (emailError || passwordError) {
                                    message = "Please fix the errors above"
                                    return@Button
                                }

                                isLoading = true
                                scope.launch {
                                    try {
                                        withContext(Dispatchers.IO) {
                                            Repository.login(
                                                email,
                                                password,
                                                onSuccess = { role ->
                                                    message = "Login successful as $role!"
                                                    val destination = if (role == "User") "userDashboard" else "doctorDashboard"
                                                    navController.navigate(destination) {
                                                        popUpTo("login") { inclusive = true }
                                                    }
                                                },
                                                onFailure = { error ->
                                                    message = when (error) {
                                                        "Not Registered" -> "Not Registered"
                                                        "Invalid Credentials" -> "Invalid Credentials"
                                                        else -> "Error: $error"
                                                    }
                                                }
                                            )
                                        }
                                    } catch (e: Exception) {
                                        message = "Login failed: ${e.message}"
                                    } finally {
                                        isLoading = false
                                    }
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .shadow(2.dp, RoundedCornerShape(8.dp)),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = primaryColor,
                            disabledContainerColor = Color.Gray
                        ),
                        shape = RoundedCornerShape(8.dp),
                        enabled = !isLoading
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                        } else {
                            Text("Login", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }

            if (message.isNotEmpty()) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    shape = RoundedCornerShape(8.dp),
                    color = if (message.startsWith("Error") || message == "Not Registered" || message == "Invalid Credentials")
                        errorColor.copy(alpha = 0.1f) else primaryColor.copy(alpha = 0.1f),
                    border = BorderStroke(1.dp, if (message.startsWith("Error") || message == "Not Registered" || message == "Invalid Credentials") errorColor else primaryColor)
                ) {
                    Text(
                        text = message,
                        color = if (message.startsWith("Error") || message == "Not Registered" || message == "Invalid Credentials")
                            errorColor else primaryColor,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            Text(
                text = "Don’t have an account? Sign up",
                color = primaryColor,
                fontSize = 14.sp,
                modifier = Modifier
                    .padding(top = 24.dp)
                    .clickable { navController.navigate("signup") }
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(Color.Transparent, primaryColor.copy(alpha = 0.2f), Color.Transparent)
                        ),
                        shape = RoundedCornerShape(4.dp)
                    )
                    .padding(4.dp)
            )
        }
    }
}