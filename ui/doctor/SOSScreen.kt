package com.example.vitalrite_1.ui.doctor

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
import com.example.vitalrite_1.data.User
import com.example.vitalrite_1.ui.auth.DropdownMenuBox
import com.example.vitalrite_1.ui.components.DoctorBottomNav
import com.example.vitalrite_1.ui.components.TopBar
import com.google.firebase.firestore.FirebaseFirestore

@Composable
fun SOSScreen(navController: NavController) {
    val firestore = FirebaseFirestore.getInstance()
    var patientName by remember { mutableStateOf("") }
    var patient by remember { mutableStateOf<User?>(null) }
    var accidentCause by remember { mutableStateOf("") }
    var hospitalName by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }

    LaunchedEffect(patientName) {
        if (patientName.isNotEmpty()) {
            firestore.collection("Users").whereEqualTo("name", patientName).get()
                .addOnSuccessListener { documents ->
                    if (!documents.isEmpty) patient = documents.documents[0].toObject(User::class.java)
                }
        }
    }

    // Define a modern color palette (consistent with user screens)
    val primaryColor = Color(0xFF6200EA) // Deep Purple
    val backgroundGradient = Brush.verticalGradient(
        colors = listOf(Color(0xFFF5F7FA), Color(0xFFE0E7FF))
    )

    Scaffold(
        topBar = { TopBar(navController, "SOS") },
        bottomBar = { DoctorBottomNav(navController) },
        floatingActionButton = {
            if (patient != null) {
                FloatingActionButton(
                    onClick = {
                        if (accidentCause.isEmpty() || hospitalName.isEmpty()) {
                            message = "Please fill all fields"
                        } else {
                            isLoading = true
                            // Simulate sending SOS (replace with actual implementation)
                            println("SOS sent to ${patient!!.emergencyContact}: ${patient!!.name} is in $hospitalName due to $accidentCause")
                            message = "SOS sent successfully!"
                            isLoading = false
                        }
                    },
                    containerColor = primaryColor,
                    contentColor = Color.White,
                    modifier = Modifier.shadow(8.dp, RoundedCornerShape(16.dp))
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    } else {
                        Text("Send SOS", fontSize = 16.sp)
                    }
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
                    "Send SOS",
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
                        value = patientName,
                        onValueChange = { patientName = it },
                        label = { Text("Patient Name") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = primaryColor,
                            unfocusedBorderColor = Color.Gray
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    patient?.let { user ->
                        Text(
                            "Age: ${user.age}, Gender: ${user.gender}, Emergency Contact: ${user.emergencyContact}",
                            color = Color.Gray,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = accidentCause,
                            onValueChange = { accidentCause = it },
                            label = { Text("Accident Cause") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = primaryColor,
                                unfocusedBorderColor = Color.Gray
                            )
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        DropdownMenuBox(
                            label = "Hospital",
                            options = listOf("Hospital A", "Hospital B"),
                            onSelected = { selectedHospital -> hospitalName = selectedHospital }
                        )
                    }
                }
            }

            // Message Display
            if (message.isNotEmpty()) {
                Text(
                    message,
                    color = if (message.contains("successfully")) Color.Green else Color.Red,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }
}