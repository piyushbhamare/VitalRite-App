package com.example.vitalrite_1.ui.doctor

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.example.vitalrite_1.data.Appointment
import com.example.vitalrite_1.data.Doctor
import com.example.vitalrite_1.data.Medicine
import com.example.vitalrite_1.data.Prescription
import com.example.vitalrite_1.data.Reminder
import com.example.vitalrite_1.data.Repository
import com.example.vitalrite_1.ui.components.DoctorBottomNav
import com.example.vitalrite_1.ui.components.TopBar
import com.example.vitalrite_1.utils.getTimeAsList
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun PrescribeScreen(navController: NavController, appointmentId: String) {
    val firestore = FirebaseFirestore.getInstance()
    var appointment by remember { mutableStateOf<Appointment?>(null) }
    var doctorName by remember { mutableStateOf("Doctor Name") }
    var weight by remember { mutableStateOf("") }
    var mainCause by remember { mutableStateOf("") }
    val medicines = remember { mutableStateListOf(Medicine()) }
    var isLoading by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }
    var showSuccessDialog by remember { mutableStateOf(false) }

    // Fetch appointment and doctor details
    LaunchedEffect(appointmentId) {
        if (appointmentId.isNotEmpty()) {
            firestore.collection("Appointments").document(appointmentId).get()
                .addOnSuccessListener { document ->
                    appointment = document.toObject(Appointment::class.java)?.copy(id = document.id)
                }
                .addOnFailureListener {
                    message = "Failed to load appointment details"
                }
        }

        val doctorId = FirebaseAuth.getInstance().currentUser?.uid
        if (doctorId != null) {
            firestore.collection("Doctors").document(doctorId).get()
                .addOnSuccessListener { document ->
                    val doctor = document.toObject(Doctor::class.java)
                    doctorName = doctor?.name ?: "Doctor Name"
                }
                .addOnFailureListener {
                    message = "Failed to load doctor details"
                }
        }
    }

    // Define a modern color palette
    val primaryColor = Color(0xFF6200EA)
    val secondaryColor = Color(0xFF03DAC5)
    val backgroundGradient = Brush.verticalGradient(
        colors = listOf(Color(0xFFF5F7FA), Color(0xFFE0E7FF))
    )

    // Success Dialog
    if (showSuccessDialog) {
        AlertDialog(
            onDismissRequest = {
                showSuccessDialog = false
                // Navigate to doctor dashboard
                navController.navigate("doctorDashboard") {
                    popUpTo("doctorDashboard") { inclusive = true }
                }
            },
            title = { Text("Success") },
            text = { Text("Prescription saved successfully and set to active!") },
            confirmButton = {
                Button(
                    onClick = {
                        showSuccessDialog = false
                        // Navigate to doctor dashboard
                        navController.navigate("doctorDashboard") {
                            popUpTo("doctorDashboard") { inclusive = true }
                        }
                    }
                ) {
                    Text("OK")
                }
            },
            containerColor = Color.White,
            titleContentColor = primaryColor,
            textContentColor = Color.Black
        )
    }

    Scaffold(
        topBar = { TopBar(navController, "Prescribe Medicines") },
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
                    "Prescribe for Patient",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp,
                        color = Color.Black
                    )
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Message Display - Moved here for better visibility
            if (message.isNotEmpty()) {
                Text(
                    message,
                    color = if (message.contains("successfully")) Color.Green else Color.Red,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .background(
                            color = if (message.contains("successfully")) Color(0xFFE8F5E9) else Color(0xFFFFEBEE),
                            shape = RoundedCornerShape(8.dp)
                        )
                        .padding(12.dp)
                )
            }

            // Prescribe Button
            Button(
                onClick = {
                    // Check if all fields are filled
                    if (weight.isEmpty() || mainCause.isEmpty() || medicines.any {
                            it.name.isEmpty() || it.diagnosis.isEmpty() || getTimeAsList(it.timeRaw).isEmpty() || it.noOfDays.isEmpty()
                        }) {
                        message = "Please fill all fields"
                    } else {
                        isLoading = true
                        message = "" // Clear any previous message
                        // Calculate expiry date based on the highest noOfDays
                        val maxDays = medicines.maxOfOrNull { it.noOfDays.toIntOrNull() ?: 0 } ?: 0
                        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                        val calendar = Calendar.getInstance()
                        appointment?.let { appointment ->
                            try {
                                calendar.time = dateFormat.parse(appointment.date) ?: Date()
                                calendar.add(Calendar.DAY_OF_YEAR, maxDays)
                                val expiryDate = dateFormat.format(calendar.time)

                                // Create the prescription with active set to true
                                val prescription = Prescription(
                                    userId = appointment.userId,
                                    name = "Prescription-${System.currentTimeMillis()}",
                                    doctorName = doctorName,
                                    date = appointment.date,
                                    mainCause = mainCause,
                                    medicines = medicines.filter { it.name.isNotEmpty() },
                                    age = appointment.age,
                                    weight = weight,
                                    expiryDate = expiryDate,
                                    active = true // Set active to true
                                )

                                // Save the prescription to Firestore
                                firestore.collection("Prescriptions").add(prescription)
                                    .addOnSuccessListener { documentReference ->
                                        // Update user's active prescriptions
                                        firestore.collection("Users").document(appointment.userId)
                                            .update("activePrescriptions", FieldValue.arrayUnion(documentReference.id))
                                            .addOnSuccessListener {
                                                // Create reminders for each medicine in the prescription
                                                val currentDate = dateFormat.format(Date()) // Use today's date for reminders
                                                prescription.medicines.forEach { medicine ->
                                                    val times = getTimeAsList(medicine.timeRaw)
                                                    val reminder = Reminder(
                                                        medicineName = medicine.name,
                                                        times = times,
                                                        taken = List(times.size) { false },
                                                        snoozeTimes = List(times.size) { null },
                                                        date = currentDate
                                                    )
                                                    Repository.updateReminder(
                                                        userId = appointment.userId,
                                                        reminder = reminder,
                                                        onSuccess = { updatedReminder ->
                                                            Log.d("PrescribeScreen", "Saved reminder for ${medicine.name}: ${updatedReminder.id}")
                                                        },
                                                        onFailure = { error ->
                                                            Log.e("PrescribeScreen", "Failed to save reminder for ${medicine.name}: $error")
                                                            message = "Failed to save reminder: $error"
                                                        }
                                                    )
                                                }

                                                // Delete the appointment
                                                firestore.collection("Appointments").document(appointmentId).delete()
                                                    .addOnSuccessListener {
                                                        isLoading = false
                                                        // Show success dialog instead of navigating immediately
                                                        showSuccessDialog = true
                                                    }
                                                    .addOnFailureListener { e ->
                                                        message = "Failed to delete appointment: ${e.message}"
                                                        isLoading = false
                                                    }
                                            }
                                            .addOnFailureListener { e ->
                                                message = "Failed to update user profile: ${e.message}"
                                                isLoading = false
                                            }
                                    }
                                    .addOnFailureListener { e ->
                                        message = "Failed to save prescription: ${e.message}"
                                        isLoading = false
                                    }
                            } catch (e: Exception) {
                                message = "Error processing date: ${e.message}"
                                isLoading = false
                            }
                        } ?: run {
                            message = "Appointment data not available"
                            isLoading = false
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = primaryColor,
                    contentColor = Color.White
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, bottom = 16.dp)
                    .shadow(4.dp, RoundedCornerShape(12.dp))
                    .clip(RoundedCornerShape(12.dp))
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                } else {
                    Text(
                        "Prescribe",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                }
            }

            // Rest of the form content...
            // Form Fields in a Card
            appointment?.let { appointment ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight()
                        .shadow(4.dp, RoundedCornerShape(12.dp))
                        .clip(RoundedCornerShape(12.dp)),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        // Patient Details
                        Text(
                            "Patient Details",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = Color.Black
                            )
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                "Date: ${appointment.date}",
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontSize = 16.sp,
                                    color = Color.Black
                                )
                            )
                            Text(
                                "Age: ${appointment.age}",
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontSize = 16.sp,
                                    color = Color.Black
                                )
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Name: ${appointment.patientName}",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontSize = 16.sp,
                                color = Color.Black
                            )
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        // Weight and Main Cause
                        OutlinedTextField(
                            value = weight,
                            onValueChange = { weight = it },
                            label = { Text("Weight (kg)") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = primaryColor,
                                unfocusedBorderColor = Color.Gray,
                                focusedLabelColor = primaryColor
                            ),
                            textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 16.sp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        OutlinedTextField(
                            value = mainCause,
                            onValueChange = { mainCause = it },
                            label = { Text("Main Cause") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = primaryColor,
                                unfocusedBorderColor = Color.Gray,
                                focusedLabelColor = primaryColor
                            ),
                            textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 16.sp)
                        )
                        Spacer(modifier = Modifier.height(24.dp))

                        // Medicines Section
                        Text(
                            "Medicines",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = Color.Black
                            )
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        // Medicine Inputs
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 300.dp)
                        ) {
                            items(medicines) { medicine ->
                                MedicineInput(
                                    medicine = medicine,
                                    primaryColor = primaryColor,
                                    onMedicineChange = { updatedMedicine ->
                                        val index = medicines.indexOf(medicine)
                                        if (index != -1) {
                                            medicines[index] = updatedMedicine
                                        }
                                    }
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                            }
                            item {
                                Button(
                                    onClick = { medicines.add(Medicine()) },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = secondaryColor,
                                        contentColor = Color.White
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .shadow(4.dp, RoundedCornerShape(12.dp))
                                        .clip(RoundedCornerShape(12.dp))
                                ) {
                                    Text("Add Medicine", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))

                        // Prescribed By
                        Text(
                            "Prescribed by: $doctorName",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = FontWeight.Medium,
                                fontSize = 16.sp,
                                color = Color.Black
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MedicineInput(
    medicine: Medicine,
    primaryColor: Color,
    onMedicineChange: (Medicine) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(2.dp, RoundedCornerShape(12.dp))
            .clip(RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF9FAFB))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            OutlinedTextField(
                value = medicine.name,
                onValueChange = { newName ->
                    val updatedMedicine = medicine.copy(name = newName)
                    onMedicineChange(updatedMedicine)
                },
                label = { Text("Medicine Name") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = primaryColor,
                    unfocusedBorderColor = Color.Gray,
                    focusedLabelColor = primaryColor
                ),
                textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 16.sp),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = medicine.diagnosis,
                onValueChange = { newDiagnosis ->
                    val updatedMedicine = medicine.copy(diagnosis = newDiagnosis)
                    onMedicineChange(updatedMedicine)
                },
                label = { Text("Diagnosis") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = primaryColor,
                    unfocusedBorderColor = Color.Gray,
                    focusedLabelColor = primaryColor
                ),
                textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 16.sp),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(12.dp))
            MultiSelectDropdownMenuBox(
                label = "Time",
                options = listOf(
                    "Before Breakfast", "Before Lunch", "Before Dinner",
                    "After Breakfast", "After Lunch", "After Dinner", "Before Sleep"
                ),
                selectedOptions = getTimeAsList(medicine.timeRaw),
                onSelectedChange = { selectedTimes ->
                    val updatedMedicine = medicine.copy(timeRaw = selectedTimes)
                    onMedicineChange(updatedMedicine)
                },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = medicine.noOfDays,
                onValueChange = { newDays ->
                    val updatedMedicine = medicine.copy(noOfDays = newDays)
                    onMedicineChange(updatedMedicine)
                },
                label = { Text("Days") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = primaryColor,
                    unfocusedBorderColor = Color.Gray,
                    focusedLabelColor = primaryColor
                ),
                textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 16.sp),
                singleLine = true
            )
        }
    }
}

@Composable
fun MultiSelectDropdownMenuBox(
    label: String,
    options: List<String>,
    selectedOptions: List<String>,
    onSelectedChange: (List<String>) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val primaryColor = Color(0xFF6200EA)

    Box(modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    onClick = { expanded = true },
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                )
        ) {
            OutlinedTextField(
                value = if (selectedOptions.isEmpty()) "" else selectedOptions.joinToString(", "),
                onValueChange = {},
                label = { Text(label) },
                modifier = Modifier.fillMaxWidth(),
                readOnly = true,
                enabled = false,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = primaryColor,
                    unfocusedBorderColor = Color.Gray,
                    focusedLabelColor = primaryColor,
                    disabledBorderColor = Color.Gray,
                    disabledLabelColor = primaryColor,
                    disabledTextColor = Color.Black
                ),
                textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 16.sp)
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White, RoundedCornerShape(8.dp))
                .shadow(4.dp, RoundedCornerShape(8.dp))
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                option,
                                color = Color.Black,
                                fontSize = 16.sp,
                                modifier = Modifier.weight(1f)
                            )
                            Checkbox(
                                checked = selectedOptions.contains(option),
                                onCheckedChange = { checked ->
                                    val newSelectedOptions = if (checked) {
                                        selectedOptions + option
                                    } else {
                                        selectedOptions - option
                                    }
                                    onSelectedChange(newSelectedOptions)
                                },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = primaryColor,
                                    uncheckedColor = Color.Gray
                                )
                            )
                        }
                    },
                    onClick = {
                        val newSelectedOptions = if (selectedOptions.contains(option)) {
                            selectedOptions - option
                        } else {
                            selectedOptions + option
                        }
                        onSelectedChange(newSelectedOptions)
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}