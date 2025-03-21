package com.example.vitalrite_1.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.vitalrite_1.data.UserPreferences

@Composable
fun UserBottomNav(navController: NavController) {
    var selectedRoute by remember { mutableStateOf("userDashboard") }
    val userPreferences = remember { UserPreferences(navController.context) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF6200EA))
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        IconButton(
            icon = Icons.Default.LocalPharmacy,
            isSelected = selectedRoute == "ePrescription",
            onClick = {
                selectedRoute = "ePrescription"
                navController.navigate("ePrescription")
            }
        )
        IconButton(
            icon = Icons.Default.CalendarToday,
            isSelected = selectedRoute == "appointments",
            onClick = {
                selectedRoute = "appointments"
                navController.navigate("appointments")
            }
        )
        IconButton(
            icon = Icons.Default.Home,
            isSelected = selectedRoute == "userDashboard",
            onClick = {
                selectedRoute = "userDashboard"
                navController.navigate("userDashboard")
            }
        )
        IconButton(
            icon = Icons.Default.Assignment,
            isSelected = selectedRoute == "medicalReports",
            onClick = {
                selectedRoute = "medicalReports"
                navController.navigate("medicalReports")
            }
        )
        IconButton(
            icon = Icons.Default.Alarm,
            isSelected = selectedRoute == "reminders",
            onClick = {
                selectedRoute = "reminders"
                navController.navigate("reminders")
            }
        )
    }
}

@Composable
fun DoctorBottomNav(navController: NavController) {
    var selectedRoute by remember { mutableStateOf("doctorDashboard") }
    val userPreferences = remember { UserPreferences(navController.context) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF6200EA))
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        IconButton(
            icon = Icons.Default.CalendarToday,
            isSelected = selectedRoute == "doctorAppointments",
            onClick = {
                selectedRoute = "doctorAppointments"
                navController.navigate("doctorAppointments")
            }
        )
        IconButton(
            icon = Icons.Default.Home,
            isSelected = selectedRoute == "doctorDashboard",
            onClick = {
                selectedRoute = "doctorDashboard"
                navController.navigate("doctorDashboard")
            }
        )
        IconButton(
            icon = Icons.Default.DateRange,
            isSelected = selectedRoute == "calendar",
            onClick = {
                selectedRoute = "calendar"
                navController.navigate("calendar")
            }
        )
        IconButton(
            icon = Icons.Default.Warning,
            isSelected = selectedRoute == "sos",
            onClick = {
                selectedRoute = "sos"
                navController.navigate("sos")
            }
        )
    }
}

@Composable
fun IconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val tintColor by animateColorAsState(if (isSelected) Color.White else Color.White.copy(alpha = 0.5f))
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp)
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = tintColor,
            modifier = Modifier.size(24.dp)
        )
    }
}