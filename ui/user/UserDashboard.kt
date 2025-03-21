package com.example.vitalrite_1.ui.user

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MedicalServices
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
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.vitalrite_1.UserTypeViewModel
import com.example.vitalrite_1.UserTypeViewModelFactory
import com.example.vitalrite_1.data.Appointment
import com.example.vitalrite_1.data.Prescription
import com.example.vitalrite_1.data.User
import com.example.vitalrite_1.data.UserPreferences
import com.example.vitalrite_1.ui.components.TopBar
import com.example.vitalrite_1.ui.components.UserBottomNav
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun UserDashboard(navController: NavController) {
    val userPreferences = remember { UserPreferences(navController.context) }
    val viewModel: UserTypeViewModel = viewModel(factory = UserTypeViewModelFactory(userPreferences))
    val userName by viewModel.userName
    var appointments by remember { mutableStateOf(listOf<Appointment>()) }
    var prescriptions by remember { mutableStateOf(listOf<Prescription>()) }
    var appointmentListener by remember { mutableStateOf<ListenerRegistration?>(null) }
    var prescriptionListener by remember { mutableStateOf<ListenerRegistration?>(null) }

    LaunchedEffect(Unit) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return@LaunchedEffect

        // Real-time listener for appointments
        appointmentListener = FirebaseFirestore.getInstance()
            .collection("Appointments")
            .whereEqualTo("userId", uid)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.w("UserDashboard", "Listen failed for appointments.", e)
                    return@addSnapshotListener
                }
                appointments = snapshot?.documents?.mapNotNull { document ->
                    document.toObject(Appointment::class.java)?.copy(id = document.id)
                } ?: emptyList()
            }

        // Real-time listener for prescriptions
        prescriptionListener = FirebaseFirestore.getInstance()
            .collection("Prescriptions")
            .whereEqualTo("userId", uid)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.w("UserDashboard", "Listen failed for prescriptions.", e)
                    return@addSnapshotListener
                }

                val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val currentDate = Date()

                // Process all prescriptions for the user
                val allPrescriptions = snapshot?.documents?.mapNotNull { doc ->
                    val prescription = doc.toObject(Prescription::class.java)?.copy(id = doc.id)
                    if (prescription != null) {
                        val expiryDate = try {
                            dateFormat.parse(prescription.expiryDate) ?: Date()
                        } catch (e: Exception) {
                            Log.e("UserDashboard", "Failed to parse expiryDate for ${prescription.id}: ${e.message}")
                            Date() // Default to current date if parsing fails
                        }

                        // If the prescription is expired but still marked as active, update it
                        if (prescription.active && expiryDate.before(currentDate)) {
                            FirebaseFirestore.getInstance().collection("Prescriptions").document(doc.id)
                                .update("active", false)
                                .addOnSuccessListener {
                                    Log.d("UserDashboard", "Set active=false for prescription ${doc.id} (expired)")
                                }
                                .addOnFailureListener { e ->
                                    Log.e("UserDashboard", "Failed to update active field for ${doc.id}: ${e.message}")
                                }
                        }
                        prescription
                    } else {
                        null
                    }
                }?.filterNotNull() ?: emptyList()

                // Filter for ongoing prescriptions (active and not expired)
                prescriptions = allPrescriptions.filter { prescription ->
                    val expiryDate = try {
                        dateFormat.parse(prescription.expiryDate) ?: Date()
                    } catch (e: Exception) {
                        Date()
                    }
                    prescription.active && expiryDate.after(currentDate)
                }

                // Update user's activePrescriptions list
                FirebaseFirestore.getInstance().collection("Users").document(uid).get()
                    .addOnSuccessListener { document ->
                        val user = document.toObject(User::class.java)
                        val activePrescriptionIds = user?.activePrescriptions ?: emptyList()
                        val updatedActivePrescriptionIds = activePrescriptionIds.filter { id ->
                            allPrescriptions.any { it.id == id && it.active && (dateFormat.parse(it.expiryDate) ?: Date()).after(currentDate) }
                        }
                        if (activePrescriptionIds != updatedActivePrescriptionIds) {
                            FirebaseFirestore.getInstance().collection("Users").document(uid)
                                .update("activePrescriptions", updatedActivePrescriptionIds)
                                .addOnSuccessListener {
                                    Log.d("UserDashboard", "Updated activePrescriptions list for user $uid")
                                }
                                .addOnFailureListener { e ->
                                    Log.e("UserDashboard", "Failed to update activePrescriptions: ${e.message}")
                                }
                        }
                    }
                    .addOnFailureListener { e ->
                        Log.e("UserDashboard", "Failed to fetch user data: ${e.message}")
                    }
            }
    }

    DisposableEffect(Unit) {
        onDispose {
            appointmentListener?.remove()
            prescriptionListener?.remove()
        }
    }

    val primaryColor = Color(0xFF6200EA)
    val secondaryColor = Color(0xFF03DAC5)
    val backgroundGradient = Brush.verticalGradient(
        colors = listOf(Color(0xFFF5F7FA), Color(0xFFE0E7FF))
    )

    Scaffold(
        topBar = { TopBar(navController, "Welcome Back, ${userName ?: "User"}!") },
        bottomBar = { UserBottomNav(navController) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundGradient)
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            Column(modifier = Modifier.padding(top = 16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(end = 8.dp)
                ) {
                    Text(
                        "Appointments",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 24.sp,
                            color = Color.Black
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                val upcomingAppointments = appointments.filter { it.isUpcoming() }
                if (upcomingAppointments.isEmpty()) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(4.dp, RoundedCornerShape(12.dp))
                            .clip(RoundedCornerShape(12.dp)),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Text(
                            "No Upcoming Appointments",
                            modifier = Modifier.padding(16.dp),
                            style = MaterialTheme.typography.bodyLarge.copy(
                                color = Color.Gray,
                                fontSize = 16.sp
                            )
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = { navController.navigate("bookAppointment") },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = primaryColor,
                            contentColor = Color.White
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(4.dp, RoundedCornerShape(12.dp))
                            .clip(RoundedCornerShape(12.dp))
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Book Appointment", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .horizontalScroll(rememberScrollState())
                            .fillMaxWidth()
                    ) {
                        upcomingAppointments.forEach { appointment ->
                            AppointmentCard(appointment)
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Card(
                            modifier = Modifier
                                .width(150.dp)
                                .height(120.dp)
                                .shadow(4.dp, RoundedCornerShape(12.dp))
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { navController.navigate("bookAppointment") },
                            colors = CardDefaults.cardColors(containerColor = secondaryColor)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    Icons.Default.Add,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    "Book New",
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }

            Column(modifier = Modifier.padding(top = 24.dp, bottom = 16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(end = 8.dp)
                ) {
                    Text(
                        "Ongoing Prescriptions",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 24.sp,
                            color = Color.Black
                        ),
                        modifier = Modifier.weight(1f)
                    )
                    if (prescriptions.isNotEmpty()) {
                        Button(
                            onClick = { navController.navigate("ePrescription") },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = primaryColor,
                                contentColor = Color.White
                            ),
                            modifier = Modifier
                                .shadow(4.dp, RoundedCornerShape(12.dp))
                                .clip(RoundedCornerShape(12.dp))
                        ) {
                            Text("View All", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                if (prescriptions.isEmpty()) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(4.dp, RoundedCornerShape(12.dp))
                            .clip(RoundedCornerShape(12.dp)),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Text(
                            "No Ongoing Prescriptions",
                            modifier = Modifier.padding(16.dp),
                            style = MaterialTheme.typography.bodyLarge.copy(
                                color = Color.Gray,
                                fontSize = 16.sp
                            )
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 300.dp)
                    ) {
                        items(prescriptions) { prescription ->
                            PrescriptionCard(prescription) {
                                navController.navigate("prescriptionDetail/${prescription.id}")
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AppointmentCard(appointment: Appointment) {
    Card(
        modifier = Modifier
            .width(200.dp)
            .height(120.dp)
            .shadow(4.dp, RoundedCornerShape(12.dp))
            .clip(RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Text(
                appointment.doctorName,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color.Black
                )
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "${appointment.date} at ${appointment.time}",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 14.sp,
                    color = Color.Gray
                )
            )
        }
    }
}

@Composable
fun PrescriptionCard(prescription: Prescription, onClick: () -> Unit) {
    Log.d("PrescriptionCard", "Rendering prescription: ${prescription.name}")
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp)
            .clickable { onClick() }
            .shadow(4.dp, RoundedCornerShape(12.dp))
            .clip(RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.MedicalServices,
                contentDescription = null,
                tint = Color(0xFF6200EA),
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    prescription.name,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Color.Black
                    )
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "Cause: ${prescription.mainCause}",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 14.sp,
                        color = Color.Gray
                    )
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "Expiry: ${prescription.expiryDate}",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 14.sp,
                        color = Color.Gray
                    )
                )
            }
        }
    }
}