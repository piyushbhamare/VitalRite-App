package com.example.vitalrite_1

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.vitalrite_1.data.UserPreferences
import com.example.vitalrite_1.ui.auth.LoginScreen
import com.example.vitalrite_1.ui.auth.SignupScreen
import com.example.vitalrite_1.ui.doctor.AppointmentScreen
import com.example.vitalrite_1.ui.doctor.CalendarScreen
import com.example.vitalrite_1.ui.doctor.DoctorDashboard
import com.example.vitalrite_1.ui.doctor.PrescribeScreen
import com.example.vitalrite_1.ui.doctor.SOSScreen
import com.example.vitalrite_1.ui.user.AppointmentsScreen
import com.example.vitalrite_1.ui.user.BookAppointmentScreen
import com.example.vitalrite_1.ui.user.EPreScreen
import com.example.vitalrite_1.ui.user.MedicalReportsScreen
import com.example.vitalrite_1.ui.user.PrescriptionDetailScreen
import com.example.vitalrite_1.ui.user.RemindersScreen
import com.example.vitalrite_1.ui.user.UserDashboard
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.delay
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras

class MainActivity : ComponentActivity() {
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        // Check if POST_NOTIFICATIONS was granted (only relevant for API 33+)
        val notificationsGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions[Manifest.permission.POST_NOTIFICATIONS] ?: false
        } else {
            true // Notifications permission not required below API 33
        }

        // Check if SCHEDULE_EXACT_ALARM was granted (only relevant for API 31+)
        val exactAlarmGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissions[Manifest.permission.SCHEDULE_EXACT_ALARM] ?: false
        } else {
            true // Exact alarm permission not required below API 31
        }

        if (notificationsGranted && exactAlarmGranted) {
            Log.d("MainActivity", "All required permissions granted")
            proceedWithAppSetup()
        } else {
            Log.w("MainActivity", "Required permissions denied: Notifications=$notificationsGranted, ExactAlarm=$exactAlarmGranted")
            showPermissionDeniedDialog = true
        }
    }

    private var showPermissionDeniedDialog by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Check Google Play Services availability
        val googleApiAvailability = GoogleApiAvailability.getInstance()
        val resultCode = googleApiAvailability.isGooglePlayServicesAvailable(this)
        if (resultCode != ConnectionResult.SUCCESS) {
            Log.e("MainActivity", "Google Play Services is not available: $resultCode")
            if (googleApiAvailability.isUserResolvableError(resultCode)) {
                googleApiAvailability.getErrorDialog(this, resultCode, 9000)?.show()
            }
            return
        } else {
            Log.d("MainActivity", "Google Play Services is available")
        }

        // Check and request permissions
        val permissionsToRequest = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.SCHEDULE_EXACT_ALARM
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                permissionsToRequest.add(Manifest.permission.SCHEDULE_EXACT_ALARM)
            }
        }

        if (permissionsToRequest.isEmpty()) {
            proceedWithAppSetup()
        } else {
            requestPermissionLauncher.launch(permissionsToRequest.toTypedArray())
        }

        setContent {
            if (showPermissionDeniedDialog) {
                PermissionDeniedDialog(
                    onDismiss = { showPermissionDeniedDialog = false },
                    onRequestAgain = {
                        showPermissionDeniedDialog = false
                        requestPermissionLauncher.launch(permissionsToRequest.toTypedArray())
                    }
                )
            }
            VitalRiteApp()
        }
    }

    private fun proceedWithAppSetup() {
        setContent {
            VitalRiteApp()
        }
    }
}

@Composable
fun PermissionDeniedDialog(onDismiss: () -> Unit, onRequestAgain: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Permissions Required") },
        text = { Text("This app requires notification and exact alarm permissions to send reminders. Please grant the permissions to continue.") },
        confirmButton = {
            TextButton(onClick = onRequestAgain) {
                Text("Grant Permissions")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun VitalRiteApp() {
    val navController = rememberNavController()
    val auth = FirebaseAuth.getInstance()
    val firestore = FirebaseFirestore.getInstance()
    val userPreferences = remember { UserPreferences(navController.context) }
    val viewModel: UserTypeViewModel = viewModel(factory = UserTypeViewModelFactory(userPreferences))
    val userType by userPreferences.userType.collectAsState(initial = null)

    NavHost(navController = navController, startDestination = "splash") {
        composable("splash") {
            CircularProgressIndicator()
            LaunchedEffect(Unit) {
                // Initialize user type from DataStore
                viewModel.initializeUserType()

                // Ensure a minimum loading time for UX
                delay(500L)

                // Determine user type if not cached
                if (auth.currentUser != null && userType == null) {
                    viewModel.determineUserType(auth, firestore)
                }

                // Navigate based on auth state and user type
                val destination = when {
                    auth.currentUser == null -> "login"
                    userType == "User" -> "userDashboard"
                    userType == "Doctor" -> "doctorDashboard"
                    else -> "login" // Fallback if user type determination fails
                }
                navController.navigate(destination) {
                    popUpTo("splash") { inclusive = true }
                }
            }
        }
        composable("login") { LoginScreen(navController) }
        composable("signup") { SignupScreen(navController) }
        composable("userDashboard") { UserDashboard(navController) }
        composable("doctorDashboard") { DoctorDashboard(navController) }
        composable("ePrescription") { EPreScreen(navController) }
        composable("appointments") { AppointmentsScreen(navController) }
        composable("doctorAppointments") { AppointmentScreen(navController) }
        composable("bookAppointment") { BookAppointmentScreen(navController) }
        composable("medicalReports") { MedicalReportsScreen(navController) }
        composable("reminders") { RemindersScreen(navController) }
        composable("prescribe/{appointmentId}") { backStackEntry ->
            PrescribeScreen(navController, backStackEntry.arguments?.getString("appointmentId") ?: "")
        }
        composable("calendar") { CalendarScreen(navController) }
        composable("patientHistory") { PatientHistoryScreen(navController) }
        composable("prescriptionDetail/{prescriptionId}") { backStackEntry ->
            val prescriptionId = backStackEntry.arguments?.getString("prescriptionId") ?: ""
            PrescriptionDetailScreen(navController = navController, prescriptionId = prescriptionId)
        }
    }
}

class UserTypeViewModelFactory(private val userPreferences: UserPreferences) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
        if (modelClass.isAssignableFrom(UserTypeViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return UserTypeViewModel(userPreferences) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
