package com.example.vitalrite_1.ui.doctor

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.vitalrite_1.data.DoctorAvailability
import com.example.vitalrite_1.ui.components.DoctorBottomNav
import com.example.vitalrite_1.ui.components.TopBar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun CalendarScreen(navController: NavController) {
    val firestore = FirebaseFirestore.getInstance()
    val doctorId = FirebaseAuth.getInstance().currentUser!!.uid
    var openTiming by remember { mutableStateOf("09:00") }
    var closeTiming by remember { mutableStateOf("17:00") }
    var maxAppointments by remember { mutableStateOf("15") }
    var holidays by remember { mutableStateOf(listOf<String>()) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    var successMessage by remember { mutableStateOf("") }
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        firestore.collection("DoctorAvailability").document(doctorId).get()
            .addOnSuccessListener { doc ->
                val availability = doc.toObject(DoctorAvailability::class.java)
                openTiming = availability?.openTiming ?: "09:00"
                closeTiming = availability?.closeTiming ?: "17:00"
                maxAppointments = availability?.maxAppointmentsPerHour?.toString() ?: "15"
                holidays = availability?.holidays ?: emptyList()
            }
    }

    // Define a modern color palette (consistent with user screens)
    val primaryColor = Color(0xFF6200EA) // Deep Purple
    val backgroundGradient = Brush.verticalGradient(
        colors = listOf(Color(0xFFF5F7FA), Color(0xFFE0E7FF))
    )

    Scaffold(
        topBar = { TopBar(navController, "Calendar") },
        bottomBar = { DoctorBottomNav(navController) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    isLoading = true
                    val availability = DoctorAvailability(
                        doctorId = doctorId,
                        openTiming = openTiming,
                        closeTiming = closeTiming,
                        maxAppointmentsPerHour = maxAppointments.toIntOrNull() ?: 15,
                        holidays = holidays
                    )
                    firestore.collection("DoctorAvailability").document(doctorId).set(availability)
                        .addOnSuccessListener {
                            successMessage = "Availability saved successfully!"
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar(successMessage)
                                delay(3000) // Show for 3 seconds
                                successMessage = ""
                            }
                            isLoading = false
                        }
                        .addOnFailureListener {
                            errorMessage = "Failed to save availability"
                            isLoading = false
                        }
                },
                containerColor = primaryColor,
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .padding(bottom = 16.dp, end = 16.dp) // Proper spacing from bottom and end
                    .size(64.dp) // Slightly larger for premium feel
                    .shadow(8.dp, RoundedCornerShape(16.dp)) // Enhanced shadow for depth
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                } else {
                    Text(
                        "Save",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                }
            }
        },
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.padding(16.dp)
            ) { data ->
                Snackbar(
                    modifier = Modifier
                        .shadow(4.dp, RoundedCornerShape(8.dp))
                        .clip(RoundedCornerShape(8.dp)),
                    containerColor = Color(0xFF2ECC71), // Green for success
                    contentColor = Color.White
                ) {
                    Text(
                        text = data.visuals.message,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
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
                    "Set Availability",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp,
                        color = Color.Black
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Form Fields in a Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight()
                    .shadow(4.dp, RoundedCornerShape(12.dp))
                    .clip(RoundedCornerShape(12.dp)),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    OutlinedTextField(
                        value = holidays.joinToString(", "),
                        onValueChange = { holidays = it.split(", ").map { it.trim() } },
                        label = { Text("Holidays (yyyy-MM-dd)") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = primaryColor,
                            unfocusedBorderColor = Color.Gray
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = openTiming,
                        onValueChange = { openTiming = it },
                        label = { Text("Open Timing (HH:mm)") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = primaryColor,
                            unfocusedBorderColor = Color.Gray
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = closeTiming,
                        onValueChange = { closeTiming = it },
                        label = { Text("Close Timing (HH:mm)") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = primaryColor,
                            unfocusedBorderColor = Color.Gray
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = maxAppointments,
                        onValueChange = { maxAppointments = it },
                        label = { Text("Max Appointments per Hour") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = primaryColor,
                            unfocusedBorderColor = Color.Gray
                        )
                    )
                }
            }

            // Error Message Display
            if (errorMessage.isNotEmpty()) {
                Text(
                    errorMessage,
                    color = Color.Red,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier
                        .padding(top = 8.dp)
                        .animateContentSize()
                )
            }
        }
    }
}