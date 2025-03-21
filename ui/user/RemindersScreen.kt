package com.example.vitalrite_1.ui.user

import android.util.Log
import androidx.compose.foundation.background
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.vitalrite_1.data.Reminder
import com.example.vitalrite_1.data.Prescription
import com.example.vitalrite_1.data.User
import com.example.vitalrite_1.data.Repository
import com.example.vitalrite_1.ui.components.TopBar
import com.example.vitalrite_1.ui.components.UserBottomNav
import com.example.vitalrite_1.utils.getTimeAsList
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun RemindersScreen(navController: NavController) {
    val context = LocalContext.current
    val firestore = FirebaseFirestore.getInstance()
    val userId = FirebaseAuth.getInstance().currentUser!!.uid
    var reminders by remember { mutableStateOf(listOf<Reminder>()) }
    var user by remember { mutableStateOf<User?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()
    var reminderListener by remember { mutableStateOf<ListenerRegistration?>(null) }

    // Function to get the current date as a string for reset tracking
    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val currentDate = dateFormat.format(Date())

    // Load user data and set up real-time listener for reminders
    LaunchedEffect(Unit) {
        coroutineScope.launch {
            // Fetch user data (needed for scheduling reminders and last reset date)
            firestore.collection("Users").document(userId).get()
                .addOnSuccessListener { userDoc ->
                    user = userDoc.toObject(User::class.java)
                    if (user == null) {
                        errorMessage = "User data not found"
                        Log.e("RemindersScreen", "User data not found for userId: $userId")
                        return@addOnSuccessListener
                    }

                    // Check if we need to reset reminders for the new day
                    val lastResetDate = user?.lastReminderResetDate ?: ""
                    if (lastResetDate != currentDate) {
                        // Clear old reminders
                        firestore.collection("Users").document(userId).collection("Reminders").get()
                            .addOnSuccessListener { reminderDocs ->
                                reminderDocs.forEach { doc ->
                                    val reminder = doc.toObject(Reminder::class.java)
                                    reminder.times.forEachIndexed { index, _ ->
                                        Repository.cancelReminder(context, reminder, index)
                                    }
                                    doc.reference.delete()
                                        .addOnSuccessListener {
                                            Log.d("RemindersScreen", "Deleted old reminder: ${doc.id}")
                                        }
                                        .addOnFailureListener { e ->
                                            Log.e("RemindersScreen", "Failed to delete old reminder ${doc.id}: ${e.message}")
                                        }
                                }

                                // Create new reminders based on current prescriptions
                                firestore.collection("Prescriptions").whereEqualTo("userId", userId).get()
                                    .addOnSuccessListener { documents ->
                                        if (documents.isEmpty) {
                                            Log.w("RemindersScreen", "No prescriptions found for userId: $userId")
                                            // Update last reset date even if no prescriptions
                                            firestore.collection("Users").document(userId)
                                                .update("lastReminderResetDate", currentDate)
                                                .addOnSuccessListener {
                                                    Log.d("RemindersScreen", "Updated lastReminderResetDate to $currentDate")
                                                }
                                                .addOnFailureListener { e ->
                                                    Log.e("RemindersScreen", "Failed to update lastReminderResetDate: ${e.message}")
                                                }
                                        } else {
                                            // Group medicines by name and collect their times
                                            val medicineTimesMap = mutableMapOf<String, MutableList<String>>()
                                            documents.forEach { doc ->
                                                val prescription = doc.toObject(Prescription::class.java)
                                                // Check if the prescription is active and not expired
                                                val expiryDate = try {
                                                    dateFormat.parse(prescription.expiryDate)?.time ?: Long.MAX_VALUE
                                                } catch (e: Exception) {
                                                    Long.MAX_VALUE
                                                }
                                                if (!prescription.active || expiryDate < System.currentTimeMillis()) {
                                                    Log.d("RemindersScreen", "Skipping expired/inactive prescription: ${prescription.id}")
                                                    return@forEach
                                                }
                                                prescription.medicines.forEach { medicine ->
                                                    val times = getTimeAsList(medicine.timeRaw)
                                                    medicineTimesMap.getOrPut(medicine.name) { mutableListOf() }.addAll(times)
                                                }
                                            }

                                            // Create a single Reminder for each medicine
                                            val newReminders = medicineTimesMap.map { (medicineName, times) ->
                                                Reminder(
                                                    medicineName = medicineName,
                                                    times = times,
                                                    taken = List(times.size) { false },
                                                    snoozeTimes = List(times.size) { null },
                                                    date = currentDate
                                                )
                                            }

                                            // Save the new reminders to Firestore
                                            newReminders.forEach { reminder ->
                                                Repository.updateReminder(
                                                    userId,
                                                    reminder,
                                                    onSuccess = { updatedReminder ->
                                                        Log.d("RemindersScreen", "Saved new reminder: ${updatedReminder.id}")
                                                    },
                                                    onFailure = { error ->
                                                        Log.e("RemindersScreen", "Failed to save new reminder: $error")
                                                        errorMessage = "Failed to save reminder: $error"
                                                    }
                                                )
                                            }

                                            // Update the last reset date in Firestore
                                            firestore.collection("Users").document(userId)
                                                .update("lastReminderResetDate", currentDate)
                                                .addOnSuccessListener {
                                                    Log.d("RemindersScreen", "Updated lastReminderResetDate to $currentDate")
                                                }
                                                .addOnFailureListener { e ->
                                                    Log.e("RemindersScreen", "Failed to update lastReminderResetDate: ${e.message}")
                                                }
                                        }
                                    }
                                    .addOnFailureListener { e ->
                                        errorMessage = "Failed to load prescriptions: ${e.message}"
                                        Log.e("RemindersScreen", "Failed to load prescriptions: ${e.message}")
                                    }
                            }
                            .addOnFailureListener { e ->
                                errorMessage = "Failed to clear old reminders: ${e.message}"
                                Log.e("RemindersScreen", "Failed to clear old reminders: ${e.message}")
                            }
                    }

                    // Set up real-time listener for reminders
                    reminderListener?.remove() // Remove any existing listener
                    reminderListener = firestore.collection("Users").document(userId)
                        .collection("Reminders")
                        .whereEqualTo("date", currentDate)
                        .addSnapshotListener { snapshot, e ->
                            if (e != null) {
                                errorMessage = "Failed to listen for reminders: ${e.message}"
                                Log.e("RemindersScreen", "Failed to listen for reminders: ${e.message}")
                                return@addSnapshotListener
                            }
                            if (snapshot != null) {
                                val updatedReminders = snapshot.documents.mapNotNull { doc ->
                                    doc.toObject(Reminder::class.java)
                                }
                                reminders = updatedReminders
                                Log.d("RemindersScreen", "Updated reminders from Firestore: $updatedReminders")
                            }
                        }
                }
                .addOnFailureListener { e ->
                    errorMessage = "Failed to load user data: ${e.message}"
                    Log.e("RemindersScreen", "Failed to load user data: ${e.message}")
                }
        }
    }

    // Schedule reminders after data is loaded
    LaunchedEffect(reminders, user) {
        if (reminders.isNotEmpty() && user != null) {
            reminders.forEach { reminder ->
                reminder.times.forEachIndexed { index, _ ->
                    if (reminder.taken.getOrNull(index) != true) {
                        Repository.scheduleReminder(context, reminder, user!!, index)
                    }
                }
            }
        }
    }

    // Clean up the listener when the composable is disposed
    DisposableEffect(Unit) {
        onDispose {
            reminderListener?.remove()
        }
    }

    val primaryColor = Color(0xFF6200EA)
    val backgroundGradient = Brush.verticalGradient(
        colors = listOf(Color(0xFFF5F7FA), Color(0xFFE0E7FF))
    )

    Scaffold(
        topBar = { TopBar(navController, "Reminders") },
        bottomBar = { UserBottomNav(navController) }
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(end = 8.dp)
                ) {
                    Text(
                        "Your Reminders",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp,
                            color = Color.Black
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Error message if any
            errorMessage?.let { message ->
                Text(
                    text = message,
                    color = Color.Red,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }

            // Reminders list
            val allTaken = reminders.isNotEmpty() && reminders.all { reminder ->
                reminder.taken.all { it }
            }
            if (allTaken) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight()
                        .shadow(4.dp, RoundedCornerShape(12.dp))
                        .clip(RoundedCornerShape(12.dp)),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "Taken all Medicine",
                            color = Color.Green,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            } else if (reminders.isEmpty()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight()
                        .shadow(4.dp, RoundedCornerShape(12.dp))
                        .clip(RoundedCornerShape(12.dp)),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "No Reminders",
                            color = Color.Gray,
                            fontSize = 16.sp
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(reminders) { reminder ->
                        ReminderCard(
                            reminder = reminder,
                            onTakenChange = { timeIndex, taken ->
                                val updatedTaken = reminder.taken.toMutableList().apply {
                                    set(timeIndex, taken)
                                }
                                val updatedSnoozeTimes = reminder.snoozeTimes.toMutableList().apply {
                                    set(timeIndex, null)
                                }
                                val updatedReminder = reminder.copy(taken = updatedTaken, snoozeTimes = updatedSnoozeTimes)
                                Repository.updateReminder(
                                    userId,
                                    updatedReminder,
                                    onSuccess = {
                                        Log.d("RemindersScreen", "Updated reminder ${updatedReminder.id} at index $timeIndex with taken=$taken")
                                        if (taken) {
                                            Repository.cancelReminder(context, reminder, timeIndex)
                                        } else {
                                            user?.let { Repository.scheduleReminder(context, updatedReminder, it, timeIndex) }
                                        }
                                    },
                                    onFailure = { error ->
                                        Log.e("RemindersScreen", "Failed to update reminder ${updatedReminder.id}: $error")
                                        errorMessage = "Failed to update reminder: $error"
                                    }
                                )
                            },
                            onSnooze = { timeIndex ->
                                val snoozeMinutes = if (reminder.times[timeIndex].contains("Before")) 5 else 10
                                // Use the original reminder time if available, otherwise use current time
                                val originalTimeMillis = reminder.getNextReminderTime(user!!, timeIndex)
                                val snoozeTime = Calendar.getInstance().apply {
                                    timeInMillis = if (originalTimeMillis != Long.MAX_VALUE) originalTimeMillis else System.currentTimeMillis()
                                    add(Calendar.MINUTE, snoozeMinutes)
                                }
                                val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
                                val updatedSnoozeTimes = reminder.snoozeTimes.toMutableList().apply {
                                    set(timeIndex, timeFormat.format(snoozeTime.time))
                                }
                                val updatedReminder = reminder.copy(snoozeTimes = updatedSnoozeTimes)
                                Repository.updateReminder(
                                    userId,
                                    updatedReminder,
                                    onSuccess = {
                                        Log.d("RemindersScreen", "Snoozed reminder ${updatedReminder.id} at index $timeIndex to ${updatedReminder.snoozeTimes[timeIndex]}")
                                        user?.let { Repository.scheduleReminder(context, updatedReminder, it, timeIndex) }
                                    },
                                    onFailure = { error ->
                                        Log.e("RemindersScreen", "Failed to snooze reminder ${updatedReminder.id}: $error")
                                        errorMessage = "Failed to snooze reminder: $error"
                                    }
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ReminderCard(
    reminder: Reminder,
    onTakenChange: (Int, Boolean) -> Unit,
    onSnooze: (Int) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(12.dp))
            .clip(RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Medicine name
            Text(
                text = reminder.medicineName,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
            Spacer(modifier = Modifier.height(8.dp))

            // List of times with their respective controls
            reminder.times.forEachIndexed { index, time ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Checkbox for marking as taken
                    Checkbox(
                        checked = reminder.taken.getOrNull(index) ?: false,
                        onCheckedChange = { onTakenChange(index, it) },
                        colors = CheckboxDefaults.colors(
                            checkedColor = Color(0xFF6200EA),
                            uncheckedColor = Color.Gray
                        )
                    )

                    Spacer(modifier = Modifier.width(16.dp))

                    // Reminder details
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Time: $time${reminder.snoozeTimes.getOrNull(index)?.let { " (Snoozed to $it)" } ?: ""}",
                            fontSize = 14.sp,
                            color = Color.Gray
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    // Action buttons
                    Row {
                        // Taken button
                        Button(
                            onClick = { onTakenChange(index, true) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF4CAF50),
                                contentColor = Color.White
                            ),
                            modifier = Modifier
                                .height(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                        ) {
                            Text("Taken", fontSize = 14.sp)
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        // Snooze button (only visible if not taken)
                        if (reminder.taken.getOrNull(index) != true) {
                            Button(
                                onClick = { onSnooze(index) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF6200EA),
                                    contentColor = Color.White
                                ),
                                modifier = Modifier
                                    .height(36.dp)
                                    .clip(RoundedCornerShape(8.dp))
                            ) {
                                Text("Snooze", fontSize = 14.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

fun getTimeAsList(timeRaw: Any?): List<String> {
    return when (timeRaw) {
        is List<*> -> timeRaw.filterIsInstance<String>()
        else -> emptyList()
    }
}