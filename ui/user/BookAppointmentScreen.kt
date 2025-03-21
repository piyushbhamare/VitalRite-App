package com.example.vitalrite_1.ui.user

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Search
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
import com.example.vitalrite_1.data.Appointment
import com.example.vitalrite_1.data.DoctorAvailability
import com.example.vitalrite_1.ui.components.TopBar
import com.example.vitalrite_1.ui.components.UserBottomNav
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

// Define a sealed class to represent the search state
sealed class SearchState {
    object Idle : SearchState()
    object Loading : SearchState()
    object Success : SearchState()
    object Failure : SearchState()
}

@Composable
fun BookAppointmentScreen(navController: NavController) {
    val firestore = FirebaseFirestore.getInstance()
    val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
    var doctorName by remember { mutableStateOf("") }
    var doctorAvailability by remember { mutableStateOf<DoctorAvailability?>(null) }
    var doctorId by remember { mutableStateOf<String?>(null) }
    var patientName by remember { mutableStateOf("") }
    var age by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf("Male") }
    var date by remember { mutableStateOf("") }
    var time by remember { mutableStateOf("") }
    var availableSlots by remember { mutableStateOf(listOf<String>()) }
    var message by remember { mutableStateOf("") }
    var showForm by remember { mutableStateOf(true) }
    var existingAppointments by remember { mutableStateOf(listOf<Appointment>()) }
    var searchState by remember { mutableStateOf<SearchState>(SearchState.Idle) } // Replace searchTriggered with searchState
    val coroutineScope = rememberCoroutineScope()

    // Function to perform the doctor search
    fun searchDoctor() {
        if (doctorName.isNotEmpty()) {
            val trimmedDoctorName = doctorName.trim()
            val lowercaseDoctorName = trimmedDoctorName.lowercase()
            Log.d("BookAppointmentScreen", "Searching for doctor: $trimmedDoctorName (lowercase: $lowercaseDoctorName)")

            searchState = SearchState.Loading // Set to loading state

            coroutineScope.launch {
                try {
                    val doctorSnapshot = firestore.collection("Doctors")
                        .whereEqualTo("nameLowercase", lowercaseDoctorName)
                        .get()
                        .await()

                    if (!doctorSnapshot.isEmpty) {
                        Log.d("BookAppointmentScreen", "Doctor found: ${doctorSnapshot.documents[0].data}")
                        doctorId = doctorSnapshot.documents[0].id
                        doctorName = doctorSnapshot.documents[0].getString("name") ?: trimmedDoctorName

                        // Fetch doctor availability
                        val availabilitySnapshot = firestore.collection("DoctorAvailability")
                            .document(doctorId!!)
                            .get()
                            .await()

                        doctorAvailability = availabilitySnapshot.toObject(DoctorAvailability::class.java)
                        Log.d("BookAppointmentScreen", "Doctor availability: $doctorAvailability")

                        if (doctorAvailability != null) {
                            if (date.isNotEmpty()) {
                                val apptSnapshot = firestore.collection("Appointments")
                                    .whereEqualTo("doctorId", doctorId)
                                    .whereEqualTo("date", date)
                                    .get()
                                    .await()

                                existingAppointments = apptSnapshot.map { it.toObject(Appointment::class.java) }
                                val bookedSlots = existingAppointments.map { it.time }
                                val allSlots = generateTimeSlots(
                                    doctorAvailability?.openTiming ?: "09:00",
                                    doctorAvailability?.closeTiming ?: "17:00"
                                )
                                availableSlots = allSlots.filter { !bookedSlots.contains(it) }
                                Log.d("BookAppointmentScreen", "Available slots: $availableSlots")
                            } else {
                                availableSlots = generateTimeSlots(
                                    doctorAvailability?.openTiming ?: "09:00",
                                    doctorAvailability?.closeTiming ?: "17:00"
                                )
                                Log.d("BookAppointmentScreen", "Initial available slots: $availableSlots")
                            }
                            searchState = SearchState.Success // Set to success state
                        } else {
                            doctorId = null
                            availableSlots = emptyList()
                            searchState = SearchState.Failure // Set to failure state if availability is null
                        }
                    } else {
                        Log.d("BookAppointmentScreen", "No doctor found for name: $lowercaseDoctorName")
                        doctorAvailability = null
                        doctorId = null
                        availableSlots = emptyList()
                        searchState = SearchState.Failure // Set to failure state
                    }
                } catch (e: Exception) {
                    Log.e("BookAppointmentScreen", "Failed to search for doctor: ${e.message}", e)
                    doctorAvailability = null
                    doctorId = null
                    availableSlots = emptyList()
                    searchState = SearchState.Failure // Set to failure state on error
                }
            }
        } else {
            searchState = SearchState.Idle // Reset to idle if doctorName is empty
        }
    }

    // Update available slots when date changes
    LaunchedEffect(date, doctorId) {
        if (doctorId != null && date.isNotEmpty()) {
            firestore.collection("Appointments")
                .whereEqualTo("doctorId", doctorId)
                .whereEqualTo("date", date)
                .get()
                .addOnSuccessListener { apptDocs ->
                    existingAppointments = apptDocs.map { it.toObject(Appointment::class.java) }
                    val bookedSlots = existingAppointments.map { it.time }
                    val allSlots = generateTimeSlots(
                        doctorAvailability?.openTiming ?: "09:00",
                        doctorAvailability?.closeTiming ?: "17:00"
                    )
                    availableSlots = allSlots.filter { !bookedSlots.contains(it) }
                    Log.d("BookAppointmentScreen", "Updated available slots for date $date: $availableSlots")
                }
        }
    }

    // Define a modern color palette
    val primaryColor = Color(0xFF6200EA) // Deep Purple
    val backgroundGradient = Brush.verticalGradient(
        colors = listOf(Color(0xFFF5F7FA), Color(0xFFE0E7FF))
    )

    Scaffold(
        topBar = { TopBar(navController, "Book Appointment") },
        bottomBar = { UserBottomNav(navController) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundGradient)
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            // Search Box with Search Icon
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = doctorName,
                onValueChange = {
                    doctorName = it
                    showForm = true
                    message = ""
                    searchState = SearchState.Idle // Reset search state when typing
                    doctorAvailability = null
                    doctorId = null
                    availableSlots = emptyList()
                },
                label = { Text("Doctor Name") },
                modifier = Modifier.fillMaxWidth(),
                trailingIcon = {
                    IconButton(onClick = { searchDoctor() }) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = primaryColor
                        )
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = primaryColor,
                    unfocusedBorderColor = Color.Gray
                ),
                singleLine = true,
                keyboardActions = KeyboardActions(
                    onDone = { searchDoctor() } // Trigger search when Enter is pressed
                ),
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Done // Set the IME action to "Done" (Enter key)
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Doctor Availability and Form
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
                            CircularProgressIndicator(color = primaryColor)
                        }
                    }
                }
                SearchState.Success -> {
                    if (doctorAvailability != null && showForm) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .shadow(4.dp, RoundedCornerShape(12.dp))
                                .clip(RoundedCornerShape(12.dp)),
                            colors = CardDefaults.cardColors(containerColor = Color.White)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    "Open: ${doctorAvailability!!.openTiming} - Close: ${doctorAvailability!!.closeTiming}",
                                    color = Color.Gray,
                                    fontSize = 14.sp
                                )
                                Spacer(modifier = Modifier.height(8.dp))
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
                                OutlinedTextField(
                                    value = age,
                                    onValueChange = { age = it },
                                    label = { Text("Age") },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = primaryColor,
                                        unfocusedBorderColor = Color.Gray
                                    )
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                DropdownMenuBox(
                                    label = "Gender",
                                    options = listOf("Male", "Female", "Other"),
                                    onSelected = { gender = it }
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedTextField(
                                    value = date,
                                    onValueChange = { date = it },
                                    label = { Text("Date (yyyy-MM-dd)") },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = primaryColor,
                                        unfocusedBorderColor = Color.Gray
                                    )
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                if (doctorAvailability!!.holidays.contains(date)) {
                                    Text(
                                        "Doctor Unavailable",
                                        color = MaterialTheme.colorScheme.error,
                                        fontSize = 14.sp,
                                        modifier = Modifier.padding(bottom = 8.dp)
                                    )
                                }
                                DropdownMenuBox(
                                    label = "Time",
                                    options = availableSlots,
                                    onSelected = { time = it }
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(
                                    onClick = {
                                        if (patientName.isEmpty() || age.isEmpty() || date.isEmpty() || time.isEmpty()) {
                                            message = "Please fill all fields"
                                        } else if (doctorAvailability!!.holidays.contains(date)) {
                                            message = "Doctor is unavailable on this date. Please select another date."
                                        } else if (!isValidDateFormat(date)) {
                                            message = "Invalid date format. Please use yyyy-MM-dd."
                                        } else if (!isFutureDate(date)) {
                                            message = "Please select a future date."
                                        } else {
                                            val appointment = Appointment(
                                                userId = userId,
                                                doctorId = doctorId!!,
                                                patientName = patientName,
                                                doctorName = doctorName,
                                                date = date,
                                                time = time,
                                                age = age,
                                                gender = gender
                                            )
                                            Log.d("BookAppointmentScreen", "Attempting to book appointment: $appointment")
                                            Log.d("BookAppointmentScreen", "Authenticated userId: $userId")
                                            firestore.collection("Appointments").add(appointment)
                                                .addOnSuccessListener {
                                                    message = "Appointment booked successfully!"
                                                    showForm = false
                                                    patientName = ""
                                                    age = ""
                                                    date = ""
                                                    time = ""
                                                    searchState = SearchState.Idle // Reset search state
                                                    doctorAvailability = null
                                                    doctorId = null
                                                    availableSlots = emptyList()
                                                    Log.d("BookAppointmentScreen", "Appointment booked successfully: $appointment")
                                                }
                                                .addOnFailureListener { e ->
                                                    message = "Failed to book appointment: ${e.message}"
                                                    Log.e("BookAppointmentScreen", "Failed to book appointment", e)
                                                }
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = primaryColor,
                                        contentColor = Color.White
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .shadow(4.dp, RoundedCornerShape(12.dp))
                                        .clip(RoundedCornerShape(12.dp))
                                ) {
                                    Text("Book Appointment", fontSize = 14.sp)
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
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
                SearchState.Idle -> {
                    // Do nothing, no UI to show
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

@Composable
fun DropdownMenuBox(
    label: String,
    options: List<String>,
    onSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    var selectedOption by remember { mutableStateOf(options.firstOrNull() ?: "") }

    Box(modifier = modifier) {
        OutlinedTextField(
            value = selectedOption,
            onValueChange = { /* Read-only, so no-op */ },
            label = { Text(label) },
            modifier = Modifier.fillMaxWidth(),
            readOnly = true,
            trailingIcon = {
                IconButton(onClick = { expanded = true }) {
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = "Expand"
                    )
                }
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF6200EA),
                unfocusedBorderColor = Color.Gray
            )
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        selectedOption = option
                        onSelected(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

fun generateTimeSlots(open: String, close: String): List<String> {
    val slots = mutableListOf<String>()
    val openHour = open.split(":")[0].toInt()
    val closeHour = close.split(":")[0].toInt()
    for (hour in openHour until closeHour) {
        slots.add(String.format("%02d:00", hour))
        slots.add(String.format("%02d:30", hour))
    }
    return slots
}

fun isValidDateFormat(date: String): Boolean {
    return try {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        sdf.isLenient = false
        sdf.parse(date)
        true
    } catch (e: Exception) {
        false
    }
}

fun isFutureDate(date: String): Boolean {
    return try {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val selectedDate = sdf.parse(date)
        val currentDate = Calendar.getInstance().time
        selectedDate?.after(currentDate) ?: false
    } catch (e: Exception) {
        false
    }
}