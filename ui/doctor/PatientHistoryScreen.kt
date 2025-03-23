package com.example.vitalrite_1.ui.doctor

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.vitalrite_1.data.User
import com.example.vitalrite_1.ui.components.DoctorBottomNav
import com.example.vitalrite_1.ui.components.TopBar
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

// Define a sealed class to represent the search state
sealed class SearchState {
    object Idle : SearchState()
    object Loading : SearchState()
    object Success : SearchState()
    object Failure : SearchState()
}

@Composable
fun PatientHistoryScreen(navController: NavController) {
    val firestore = FirebaseFirestore.getInstance()
    var patientName by remember { mutableStateOf("") }
    var patient by remember { mutableStateOf<User?>(null) }
    var searchState by remember { mutableStateOf<SearchState>(SearchState.Idle) }
    var message by remember { mutableStateOf("") }
    val coroutineScope = rememberCoroutineScope()

    // Function to search for a patient by name
    fun searchPatient() {
        if (patientName.isNotEmpty()) {
            val trimmedPatientName = patientName.trim()
            val lowercasePatientName = trimmedPatientName.lowercase()
            searchState = SearchState.Loading

            coroutineScope.launch {
                try {
                    val patientSnapshot = firestore.collection("Users")
                        .whereEqualTo("nameLowercase", lowercasePatientName)
                        .get()
                        .await()

                    if (!patientSnapshot.isEmpty) {
                        patient = patientSnapshot.documents[0].toObject(User::class.java)
                        searchState = SearchState.Success
                    } else {
                        patient = null
                        searchState = SearchState.Failure
                    }
                } catch (e: Exception) {
                    patient = null
                    searchState = SearchState.Failure
                }
            }
        } else {
            searchState = SearchState.Idle
            patient = null
        }
    }

    // Define a premium color palette
    val primaryColor = Color(0xFF6200EA) // Deep Purple
    val accentColor = Color(0xFF03DAC5) // Teal
    val backgroundGradient = Brush.verticalGradient(
        colors = listOf(Color(0xFFF5F7FA), Color(0xFFE0E7FF))
    )
    val cardGradient = Brush.linearGradient(
        colors = listOf(Color.White, Color(0xFFF0F4FF))
    )

    Scaffold(
        topBar = { TopBar(navController, "Patient History") },
        bottomBar = { DoctorBottomNav(navController) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundGradient)
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            // Heading Section
            Column(modifier = Modifier.padding(top = 16.dp)) {
                Text(
                    "Access Patient History",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp,
                        color = Color.Black
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Search Box with Search Icon
            OutlinedTextField(
                value = patientName,
                onValueChange = {
                    patientName = it
                    searchState = SearchState.Idle
                    patient = null
                    message = ""
                },
                label = { Text("Patient Name") },
                modifier = Modifier.fillMaxWidth(),
                trailingIcon = {
                    IconButton(onClick = { searchPatient() }) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search Patient",
                            tint = primaryColor
                        )
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = primaryColor,
                    unfocusedBorderColor = Color.Gray,
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White
                ),
                singleLine = true,
                keyboardActions = KeyboardActions(
                    onDone = { searchPatient() } // Trigger search when Enter is pressed
                ),
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Done // Set the IME action to "Done" (Enter key)
                ),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Search Results
            when (searchState) {
                SearchState.Loading -> {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(4.dp, RoundedCornerShape(12.dp))
                            .clip(RoundedCornerShape(12.dp)),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                color = primaryColor,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                }
                SearchState.Success -> {
                    patient?.let { user ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .shadow(6.dp, RoundedCornerShape(12.dp))
                                .clip(RoundedCornerShape(12.dp)),
                            colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(cardGradient)
                                    .padding(16.dp)
                            ) {
                                Column {
                                    // Patient Name (Header)
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(bottom = 8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Person,
                                            contentDescription = null,
                                            tint = primaryColor,
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            user.name,
                                            style = MaterialTheme.typography.headlineSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 20.sp,
                                                color = Color.Black
                                            )
                                        )
                                    }

                                    // Divider
                                    Divider(
                                        color = Color.Gray.copy(alpha = 0.2f),
                                        thickness = 1.dp,
                                        modifier = Modifier.padding(vertical = 8.dp)
                                    )

                                    // Patient Details in Two-Column Layout
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        // Left Column
                                        Column {
                                            PatientDetailRow(
                                                icon = Icons.Default.Cake,
                                                label = "Age",
                                                value = user.age.toString()
                                            )
                                            Spacer(modifier = Modifier.height(8.dp))
                                            PatientDetailRow(
                                                icon = Icons.Default.Wc,
                                                label = "Gender",
                                                value = user.gender
                                            )
                                            Spacer(modifier = Modifier.height(8.dp))
                                            PatientDetailRow(
                                                icon = Icons.Default.Warning,
                                                label = "Allergy",
                                                value = user.allergy
                                            )
                                            Spacer(modifier = Modifier.height(8.dp))
                                            PatientDetailRow(
                                                icon = Icons.Default.LocalHospital,
                                                label = "Operation",
                                                value = user.operation
                                            )
                                        }

                                        // Right Column
                                        Column {
                                            PatientDetailRow(
                                                icon = Icons.Default.MedicalServices,
                                                label = "Medical Condition",
                                                value = user.medicalCondition
                                            )
                                            Spacer(modifier = Modifier.height(8.dp))
                                            PatientDetailRow(
                                                icon = Icons.Default.Bloodtype,
                                                label = "Blood Group",
                                                value = user.bloodGroup
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                SearchState.Failure -> {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(4.dp, RoundedCornerShape(12.dp))
                            .clip(RoundedCornerShape(12.dp)),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Text(
                            "No Record Found!",
                            color = Color.Red,
                            fontSize = 16.sp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
                SearchState.Idle -> {
                    // Do nothing, no UI to show
                }
            }

            // Message Display with Animation (if needed in the future)
            AnimatedVisibility(
                visible = message.isNotEmpty(),
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Text(
                    message,
                    color = if (message.contains("successfully") || message.contains("Emergency Contact")) Color.Green else Color.Red,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(top = 8.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
    }
}

// Composable for each patient detail row
@Composable
fun PatientDetailRow(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 2.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color(0xFF6200EA),
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            "$label: ",
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = Color.Black
            )
        )
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                color = Color.Gray
            )
        )
    }
}
