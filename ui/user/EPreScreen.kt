package com.example.vitalrite_1.ui.user

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FilterAlt
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
import com.example.vitalrite_1.data.Prescription
import com.example.vitalrite_1.data.User
import com.example.vitalrite_1.ui.components.TopBar
import com.example.vitalrite_1.ui.components.UserBottomNav
import com.example.vitalrite_1.utils.getTimeAsList
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun EPreScreen(navController: NavController) {
    val firestore = FirebaseFirestore.getInstance()
    val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilters by remember { mutableStateOf(mutableSetOf<String>()) }
    var activePrescriptions by remember { mutableStateOf(listOf<Prescription>()) }
    var pastPrescriptions by remember { mutableStateOf(listOf<Prescription>()) }
    var isFilterExpanded by remember { mutableStateOf(false) }
    var activePrescriptionListener by remember { mutableStateOf<ListenerRegistration?>(null) }
    var pastPrescriptionListener by remember { mutableStateOf<ListenerRegistration?>(null) }

    // Fetch user's prescriptions and check their status with real-time listeners
    LaunchedEffect(searchQuery, selectedFilters) {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val currentDate = Date()

        // Fetch all prescriptions for the user first
        pastPrescriptionListener = firestore.collection("Prescriptions")
            .whereEqualTo("userId", userId)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.w("EPreScreen", "Listen failed for prescriptions.", e)
                    return@addSnapshotListener
                }

                val allPrescriptions = snapshot?.documents?.mapNotNull { doc ->
                    val prescription = doc.toObject(Prescription::class.java)?.copy(id = doc.id)
                    if (prescription != null) {
                        val expiryDate = try {
                            dateFormat.parse(prescription.expiryDate) ?: Date()
                        } catch (e: Exception) {
                            Log.e("EPreScreen", "Failed to parse expiryDate for ${prescription.id}: ${e.message}")
                            Date() // Default to current date if parsing fails
                        }

                        // If the prescription is expired but still marked as active, update it
                        if (prescription.active && expiryDate.before(currentDate)) {
                            firestore.collection("Prescriptions").document(doc.id)
                                .update("active", false)
                                .addOnSuccessListener {
                                    Log.d("EPreScreen", "Set active=false for prescription ${doc.id} (expired)")
                                }
                                .addOnFailureListener { e ->
                                    Log.e("EPreScreen", "Failed to update active field for ${doc.id}: ${e.message}")
                                }
                        }
                        prescription
                    } else {
                        null
                    }
                }?.filterNotNull() ?: emptyList()

                // Split into active and past prescriptions
                val activeList = mutableListOf<Prescription>()
                val pastList = mutableListOf<Prescription>()
                allPrescriptions.forEach { prescription ->
                    val expiryDate = try {
                        dateFormat.parse(prescription.expiryDate) ?: Date()
                    } catch (e: Exception) {
                        Date()
                    }
                    if (prescription.active && expiryDate.after(currentDate)) {
                        activeList.add(prescription)
                    } else {
                        pastList.add(prescription)
                    }
                }

                // Apply filters to active prescriptions
                activePrescriptions = activeList.filter { prescription ->
                    if (searchQuery.isEmpty()) true
                    else {
                        when {
                            selectedFilters.contains("Diagnosis") -> prescription.mainCause.contains(searchQuery, ignoreCase = true)
                            selectedFilters.contains("Doctor Name") -> prescription.doctorName.contains(searchQuery, ignoreCase = true)
                            selectedFilters.contains("Prescription") -> prescription.name.contains(searchQuery, ignoreCase = true)
                            else -> true
                        }
                    }
                }

                // Apply filters to past prescriptions
                pastPrescriptions = pastList.filter { prescription ->
                    if (searchQuery.isEmpty()) true
                    else {
                        when {
                            selectedFilters.contains("Diagnosis") -> prescription.mainCause.contains(searchQuery, ignoreCase = true)
                            selectedFilters.contains("Doctor Name") -> prescription.doctorName.contains(searchQuery, ignoreCase = true)
                            selectedFilters.contains("Prescription") -> prescription.name.contains(searchQuery, ignoreCase = true)
                            else -> true
                        }
                    }
                }

                // Update user's activePrescriptions list
                firestore.collection("Users").document(userId).get()
                    .addOnSuccessListener { document ->
                        val user = document.toObject(User::class.java)
                        val activePrescriptionIds = user?.activePrescriptions ?: emptyList()
                        val updatedActivePrescriptionIds = activePrescriptionIds.filter { id ->
                            allPrescriptions.any { it.id == id && it.active && (dateFormat.parse(it.expiryDate) ?: Date()).after(currentDate) }
                        }
                        if (activePrescriptionIds != updatedActivePrescriptionIds) {
                            firestore.collection("Users").document(userId)
                                .update("activePrescriptions", updatedActivePrescriptionIds)
                                .addOnSuccessListener {
                                    Log.d("EPreScreen", "Updated activePrescriptions list for user $userId")
                                }
                                .addOnFailureListener { e ->
                                    Log.e("EPreScreen", "Failed to update activePrescriptions: ${e.message}")
                                }
                        }
                    }
                    .addOnFailureListener { e ->
                        Log.e("EPreScreen", "Failed to fetch user data: ${e.message}")
                    }
            }
    }

    // Clean up listeners when the composable is disposed
    DisposableEffect(Unit) {
        onDispose {
            activePrescriptionListener?.remove()
            pastPrescriptionListener?.remove()
        }
    }

    val primaryColor = Color(0xFF6200EA)
    val backgroundGradient = Brush.verticalGradient(
        colors = listOf(Color(0xFFF5F7FA), Color(0xFFE0E7FF))
    )
    val filterCount = selectedFilters.size

    Scaffold(
        topBar = { TopBar(navController, "E-Prescriptions") },
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
                        "Your E-Prescriptions",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 24.sp,
                            color = Color.Black
                        ),
                        modifier = Modifier.weight(1f)
                    )
                    Box {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.wrapContentSize()
                        ) {
                            Icon(
                                Icons.Default.FilterAlt,
                                contentDescription = "Filter Icon",
                                tint = primaryColor,
                                modifier = Modifier
                                    .size(24.dp)
                                    .clickable { isFilterExpanded = true }
                            )
                            if (filterCount > 0) {
                                Box(
                                    modifier = Modifier
                                        .padding(start = 4.dp)
                                        .size(18.dp)
                                        .background(Color.Red, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        filterCount.toString(),
                                        color = Color.White,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(1.dp)
                                    )
                                }
                            }
                        }
                        DropdownMenu(
                            expanded = isFilterExpanded,
                            onDismissRequest = { isFilterExpanded = false },
                            modifier = Modifier
                                .background(Color.White, RoundedCornerShape(8.dp))
                                .shadow(4.dp, RoundedCornerShape(8.dp))
                                .align(Alignment.TopEnd)
                        ) {
                            listOf("Prescription", "Diagnosis", "Doctor Name").forEach { option ->
                                DropdownMenuItem(
                                    text = {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            Text(
                                                option,
                                                color = Color.Black,
                                                fontSize = 16.sp,
                                                modifier = Modifier.weight(1f)
                                            )
                                            if (selectedFilters.contains(option)) {
                                                Icon(
                                                    Icons.Default.Check,
                                                    contentDescription = "Selected",
                                                    tint = Color.Green,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                    },
                                    onClick = {
                                        if (selectedFilters.contains(option)) {
                                            selectedFilters.remove(option)
                                        } else {
                                            selectedFilters.add(option)
                                        }
                                        isFilterExpanded = false
                                    }
                                )
                            }
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        "Clear All Filters",
                                        color = Color.Red,
                                        fontSize = 16.sp,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                },
                                onClick = {
                                    selectedFilters.clear()
                                    searchQuery = ""
                                    isFilterExpanded = false
                                }
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))

                // Search Field
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text("Search") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = primaryColor,
                        unfocusedBorderColor = Color.Gray,
                        focusedLabelColor = primaryColor
                    ),
                    textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 16.sp)
                )
            }

            // No Records Found Message
            if (searchQuery.isNotEmpty() && activePrescriptions.isEmpty() && pastPrescriptions.isEmpty()) {
                Text(
                    "No record found",
                    color = Color.Red,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp)
                        .wrapContentWidth(Alignment.CenterHorizontally)
                )
            }

            // Prescriptions Section
            Column(modifier = Modifier.padding(top = 24.dp, bottom = 16.dp)) {
                // Active Prescriptions
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                ) {
                    Text(
                        "Active Prescriptions",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = Color.Black
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    if (activePrescriptions.isEmpty()) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .shadow(4.dp, RoundedCornerShape(12.dp))
                                .clip(RoundedCornerShape(12.dp)),
                            colors = CardDefaults.cardColors(containerColor = Color.White)
                        ) {
                            Text(
                                "No Active Prescriptions",
                                modifier = Modifier.padding(16.dp),
                                color = Color.Gray,
                                fontSize = 16.sp
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 300.dp)
                        ) {
                            itemsIndexed(activePrescriptions) { index, prescription ->
                                CondensedPrescriptionItem(
                                    prescription = prescription,
                                    prescriptionNumber = index + 1,
                                    onClick = {
                                        navController.navigate("prescriptionDetail/${prescription.id}")
                                    }
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                        }
                    }
                }

                // Past Prescriptions
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                ) {
                    Text(
                        "Past Prescriptions",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = Color.Black
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    if (pastPrescriptions.isEmpty()) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .shadow(4.dp, RoundedCornerShape(12.dp))
                                .clip(RoundedCornerShape(12.dp)),
                            colors = CardDefaults.cardColors(containerColor = Color.White)
                        ) {
                            Text(
                                "No Past Prescriptions",
                                modifier = Modifier.padding(16.dp),
                                color = Color.Gray,
                                fontSize = 16.sp
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 300.dp)
                        ) {
                            itemsIndexed(pastPrescriptions) { index, prescription ->
                                CondensedPrescriptionItem(
                                    prescription = prescription,
                                    prescriptionNumber = index + 1,
                                    onClick = {
                                        navController.navigate("prescriptionDetail/${prescription.id}")
                                    }
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

// Condensed Prescription Item for list display
@Composable
fun CondensedPrescriptionItem(
    prescription: Prescription,
    prescriptionNumber: Int,
    onClick: () -> Unit
) {
    Log.d("CondensedPrescriptionItem", "Rendering prescription: ${prescription.name}")
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .shadow(4.dp, RoundedCornerShape(12.dp))
            .clip(RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Prescription - $prescriptionNumber",
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color.Black
                )
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "Expiry Date: ${prescription.expiryDate}",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 14.sp,
                    color = Color.Gray
                )
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "Doctor: ${prescription.doctorName}",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 14.sp,
                    color = Color.Gray
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
        }
    }
}

// Full Prescription Item for detailed view (used in PrescriptionDetailScreen)
@Composable
fun PrescriptionItem(prescription: Prescription, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .shadow(4.dp, RoundedCornerShape(12.dp))
            .clip(RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    prescription.name,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Color.Black
                    )
                )
                Text(
                    "Expiry: ${prescription.expiryDate}",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 14.sp,
                        color = Color.Gray
                    )
                )
            }
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
                "Doctor: ${prescription.doctorName}",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 14.sp,
                    color = Color.Gray
                )
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "Date: ${prescription.date}",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 14.sp,
                    color = Color.Gray
                )
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Medicines:",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = Color.Black
                )
            )
            prescription.medicines.forEach { medicine ->
                Text(
                    "- ${medicine.name}: ${medicine.diagnosis}, ${getTimeAsList(medicine.timeRaw).joinToString(", ")}, ${medicine.noOfDays} days",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 14.sp,
                        color = Color.Gray
                    )
                )
            }
        }
    }
}