package com.example.vitalrite_1.ui.auth

import android.util.Log
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.vitalrite_1.data.Doctor
import com.example.vitalrite_1.data.Repository
import com.example.vitalrite_1.data.User
import android.util.Patterns
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border

@Composable
fun SignupScreen(navController: NavController) {
    var name by remember { mutableStateOf("") }
    var age by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf("Male") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var role by remember { mutableStateOf("User") }
    // User-specific fields
    var breakfastTime by remember { mutableStateOf("") }
    var lunchTime by remember { mutableStateOf("") }
    var dinnerTime by remember { mutableStateOf("") }
    var sleepTime by remember { mutableStateOf("") }
    var bloodGroup by remember { mutableStateOf("") }
    var medicalCondition by remember { mutableStateOf("") }
    var operation by remember { mutableStateOf("") }
    var allergy by remember { mutableStateOf("") }
    var emergencyContact by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    // Doctor-specific fields
    var degree by remember { mutableStateOf("") }
    var specialization by remember { mutableStateOf("General Doc") }
    var experience by remember { mutableStateOf("") }
    var clinicName by remember { mutableStateOf("") }
    var clinicAddress by remember { mutableStateOf("") }
    var clinicPhone by remember { mutableStateOf("") }
    var hospitalName by remember { mutableStateOf("") }
    var hospitalAddress by remember { mutableStateOf("") }
    var hospitalPhone by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    // Validation states
    var nameError by remember { mutableStateOf(false) }
    var emailError by remember { mutableStateOf(false) }
    var passwordError by remember { mutableStateOf(false) }

    // Define a modern color palette
    val primaryColor = Color(0xFF6200EA) // Deep Purple
    val secondaryColor = Color(0xFFBB86FC) // Light Purple
    val buttonStartColor = Color(0xFF6D28D9) // Darker purple for button gradient start
    val buttonEndColor = Color(0xFF9F67FA) // Lighter purple for button gradient end
    val backgroundGradient = Brush.verticalGradient(
        colors = listOf(Color(0xFFE0E7FF), Color(0xFFB3C8FF)) // Deeper gradient for premium feel
    )
    val errorColor = MaterialTheme.colorScheme.error
    val borderColor = Color(0xFFD1D5DB) // Gray border color matching the image

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundGradient)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 32.dp)
                .align(Alignment.Center)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // App Logo Placeholder
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.LightGray)
                    .padding(bottom = 24.dp)
            )
            // Heading
            Text(
                text = "VitalRite",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 26.sp,
                    color = Color.Black
                ),
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Text(
                text = "Create Your Account",
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.Medium,
                    fontSize = 18.sp,
                    color = Color.Gray
                ),
                modifier = Modifier.padding(bottom = 24.dp)
            )

            // Sign Up Form
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(6.dp, RoundedCornerShape(20.dp)),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    // Common fields
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it; nameError = false },
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = "Name Icon", tint = if (nameError) errorColor else primaryColor) },
                        label = { Text("Name", color = if (nameError) errorColor else Color.Gray) },
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = if (nameError) errorColor else primaryColor,
                            unfocusedBorderColor = if (nameError) errorColor else Color.LightGray
                        ),
                        shape = RoundedCornerShape(8.dp)
                    )
                    OutlinedTextField(
                        value = age,
                        onValueChange = { age = it },
                        label = { Text("Age", color = Color.Gray) },
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = primaryColor,
                            unfocusedBorderColor = Color.LightGray
                        ),
                        shape = RoundedCornerShape(8.dp)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Gender",
                            color = Color.Black,
                            fontSize = 16.sp,
                            modifier = Modifier.padding(end = 16.dp)
                        )
                        DropdownMenuBox("Gender", listOf("Male", "Female", "Other")) { gender = it }
                    }
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it; emailError = false },
                        leadingIcon = { Icon(Icons.Default.Email, contentDescription = "Email Icon", tint = if (emailError) errorColor else primaryColor) },
                        label = { Text("Email", color = if (emailError) errorColor else Color.Gray) },
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = if (emailError) errorColor else primaryColor,
                            unfocusedBorderColor = if (emailError) errorColor else Color.LightGray
                        ),
                        shape = RoundedCornerShape(8.dp)
                    )
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it; passwordError = false },
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = "Password Icon", tint = if (passwordError) errorColor else primaryColor) },
                        label = { Text("Password", color = if (passwordError) errorColor else Color.Gray) },
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = if (passwordError) errorColor else primaryColor,
                            unfocusedBorderColor = if (passwordError) errorColor else Color.LightGray
                        ),
                        shape = RoundedCornerShape(8.dp)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Role",
                            color = Color.Black,
                            fontSize = 16.sp,
                            modifier = Modifier.padding(end = 16.dp)
                        )
                        DropdownMenuBox("Role", listOf("User", "Doctor")) { role = it }
                    }

                    // Role-specific fields with section headers
                    if (role == "User") {
                        Text(
                            text = "User Details",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = FontWeight.Medium,
                                fontSize = 16.sp,
                                color = primaryColor
                            ),
                            modifier = Modifier.padding(bottom = 8.dp, top = 8.dp)
                        )
                        OutlinedTextField(
                            value = breakfastTime,
                            onValueChange = { breakfastTime = it },
                            label = { Text("Breakfast Time (HH:mm)", color = Color.Gray) },
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = primaryColor,
                                unfocusedBorderColor = Color.LightGray
                            ),
                            shape = RoundedCornerShape(8.dp)
                        )
                        OutlinedTextField(
                            value = lunchTime,
                            onValueChange = { lunchTime = it },
                            label = { Text("Lunch Time (HH:mm)", color = Color.Gray) },
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = primaryColor,
                                unfocusedBorderColor = Color.LightGray
                            ),
                            shape = RoundedCornerShape(8.dp)
                        )
                        OutlinedTextField(
                            value = dinnerTime,
                            onValueChange = { dinnerTime = it },
                            label = { Text("Dinner Time (HH:mm)", color = Color.Gray) },
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = primaryColor,
                                unfocusedBorderColor = Color.LightGray
                            ),
                            shape = RoundedCornerShape(8.dp)
                        )
                        OutlinedTextField(
                            value = sleepTime,
                            onValueChange = { sleepTime = it },
                            label = { Text("Sleep Time (HH:mm)", color = Color.Gray) },
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = primaryColor,
                                unfocusedBorderColor = Color.LightGray
                            ),
                            shape = RoundedCornerShape(8.dp)
                        )
                        OutlinedTextField(
                            value = bloodGroup,
                            onValueChange = { bloodGroup = it },
                            label = { Text("Blood Group", color = Color.Gray) },
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = primaryColor,
                                unfocusedBorderColor = Color.LightGray
                            ),
                            shape = RoundedCornerShape(8.dp)
                        )
                        OutlinedTextField(
                            value = medicalCondition,
                            onValueChange = { medicalCondition = it },
                            label = { Text("Medical Condition", color = Color.Gray) },
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = primaryColor,
                                unfocusedBorderColor = Color.LightGray
                            ),
                            shape = RoundedCornerShape(8.dp)
                        )
                        OutlinedTextField(
                            value = operation,
                            onValueChange = { operation = it },
                            label = { Text("Operation History", color = Color.Gray) },
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = primaryColor,
                                unfocusedBorderColor = Color.LightGray
                            ),
                            shape = RoundedCornerShape(8.dp)
                        )
                        OutlinedTextField(
                            value = allergy,
                            onValueChange = { allergy = it },
                            label = { Text("Allergy", color = Color.Gray) },
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = primaryColor,
                                unfocusedBorderColor = Color.LightGray
                            ),
                            shape = RoundedCornerShape(8.dp)
                        )
                        OutlinedTextField(
                            value = emergencyContact,
                            onValueChange = { emergencyContact = it },
                            label = { Text("Emergency Contact", color = Color.Gray) },
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = primaryColor,
                                unfocusedBorderColor = Color.LightGray
                            ),
                            shape = RoundedCornerShape(8.dp)
                        )
                        OutlinedTextField(
                            value = address,
                            onValueChange = { address = it },
                            label = { Text("Address", color = Color.Gray) },
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = primaryColor,
                                unfocusedBorderColor = Color.LightGray
                            ),
                            shape = RoundedCornerShape(8.dp)
                        )
                    }

                    // Doctor-specific fields
                    if (role == "Doctor") {
                        Text(
                            text = "Doctor Details",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = FontWeight.Medium,
                                fontSize = 16.sp,
                                color = primaryColor
                            ),
                            modifier = Modifier.padding(bottom = 8.dp, top = 8.dp)
                        )
                        OutlinedTextField(
                            value = degree,
                            onValueChange = { degree = it },
                            label = { Text("Degree", color = Color.Gray) },
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = primaryColor,
                                unfocusedBorderColor = Color.LightGray
                            ),
                            shape = RoundedCornerShape(8.dp)
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Specialization",
                                color = Color.Black,
                                fontSize = 16.sp,
                                modifier = Modifier.padding(end = 16.dp)
                            )
                            DropdownMenuBox("Specialization", listOf("General Doc", "Neurosurgeon", "Cardiologist")) { specialization = it }
                        }
                        OutlinedTextField(
                            value = experience,
                            onValueChange = { experience = it },
                            label = { Text("Years of Experience", color = Color.Gray) },
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = primaryColor,
                                unfocusedBorderColor = Color.LightGray
                            ),
                            shape = RoundedCornerShape(8.dp)
                        )
                        OutlinedTextField(
                            value = clinicName,
                            onValueChange = { clinicName = it },
                            label = { Text("Clinic Name", color = Color.Gray) },
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = primaryColor,
                                unfocusedBorderColor = Color.LightGray
                            ),
                            shape = RoundedCornerShape(8.dp)
                        )
                        OutlinedTextField(
                            value = clinicAddress,
                            onValueChange = { clinicAddress = it },
                            label = { Text("Clinic Address", color = Color.Gray) },
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = primaryColor,
                                unfocusedBorderColor = Color.LightGray
                            ),
                            shape = RoundedCornerShape(8.dp)
                        )
                        OutlinedTextField(
                            value = clinicPhone,
                            onValueChange = { clinicPhone = it },
                            label = { Text("Clinic Phone", color = Color.Gray) },
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = primaryColor,
                                unfocusedBorderColor = Color.LightGray
                            ),
                            shape = RoundedCornerShape(8.dp)
                        )
                        OutlinedTextField(
                            value = hospitalName,
                            onValueChange = { hospitalName = it },
                            label = { Text("Hospital Name", color = Color.Gray) },
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = primaryColor,
                                unfocusedBorderColor = Color.LightGray
                            ),
                            shape = RoundedCornerShape(8.dp)
                        )
                        OutlinedTextField(
                            value = hospitalAddress,
                            onValueChange = { hospitalAddress = it },
                            label = { Text("Hospital Address", color = Color.Gray) },
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = primaryColor,
                                unfocusedBorderColor = Color.LightGray
                            ),
                            shape = RoundedCornerShape(8.dp)
                        )
                        OutlinedTextField(
                            value = hospitalPhone,
                            onValueChange = { hospitalPhone = it },
                            label = { Text("Hospital Phone", color = Color.Gray) },
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = primaryColor,
                                unfocusedBorderColor = Color.LightGray
                            ),
                            shape = RoundedCornerShape(8.dp)
                        )
                    }

                    // Redesigned Sign Up Button with animation
                    var signUpButtonScale by remember { mutableStateOf(1f) }
                    val animatedSignUpScale by animateFloatAsState(targetValue = signUpButtonScale)
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .padding(top = 16.dp)
                            .scale(animatedSignUpScale)
                            .clickable(enabled = !isLoading) {
                                if (!isLoading) {
                                    signUpButtonScale = 0.95f
                                    isLoading = true
                                    message = ""

                                    // Validation
                                    nameError = name.isBlank()
                                    emailError = !Patterns.EMAIL_ADDRESS.matcher(email).matches()
                                    passwordError = password.length < 6
                                    if (nameError || emailError || passwordError) {
                                        message = "Please fix the errors above"
                                        isLoading = false
                                        signUpButtonScale = 1f
                                        return@clickable
                                    }

                                    if (role == "User") {
                                        val user = User(
                                            name = name, age = age, gender = gender, email = email, breakfastTime = breakfastTime,
                                            lunchTime = lunchTime, dinnerTime = dinnerTime, sleepTime = sleepTime, bloodGroup = bloodGroup,
                                            medicalCondition = medicalCondition, operation = operation, allergy = allergy,
                                            emergencyContact = emergencyContact, address = address
                                        )
                                        Log.d("SignupScreen", "Registering user with email: ${user.email}")
                                        Repository.registerUser(user, password,
                                            onSuccess = {
                                                message = "User registered successfully!"
                                                isLoading = false
                                                signUpButtonScale = 1f
                                                navController.navigate("userDashboard") { popUpTo("signup") { inclusive = true } }
                                            },
                                            onFailure = { error ->
                                                message = "Error: $error"
                                                isLoading = false
                                                signUpButtonScale = 1f
                                                Log.e("SignupScreen", "User registration failed: $error")
                                            }
                                        )
                                    } else {
                                        val doctor = Doctor(
                                            name = name, age = age, gender = gender, email = email, degree = degree, specialization = specialization,
                                            experience = experience, clinicName = clinicName, clinicAddress = clinicAddress,
                                            clinicPhone = clinicPhone, hospitalName = hospitalName, hospitalAddress = hospitalAddress,
                                            hospitalPhone = hospitalPhone
                                        )
                                        Log.d("SignupScreen", "Registering doctor with email: ${doctor.email}")
                                        Repository.registerDoctor(doctor, password,
                                            onSuccess = {
                                                message = "Doctor registered successfully!"
                                                isLoading = false
                                                signUpButtonScale = 1f
                                                navController.navigate("doctorDashboard") { popUpTo("signup") { inclusive = true } }
                                            },
                                            onFailure = { error ->
                                                message = "Error: $error"
                                                isLoading = false
                                                signUpButtonScale = 1f
                                                Log.e("SignupScreen", "Doctor registration failed: $error")
                                            }
                                        )
                                    }
                                }
                            }
                            .shadow(2.dp, RoundedCornerShape(24.dp))
                            .border(1.dp, borderColor, RoundedCornerShape(24.dp)),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    brush = Brush.horizontalGradient(colors = listOf(buttonStartColor, buttonEndColor)),
                                    shape = RoundedCornerShape(24.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                            } else {
                                Text(
                                    "Sign Up",
                                    color = Color.White,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            // Message
            if (message.isNotEmpty()) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    shape = RoundedCornerShape(8.dp),
                    color = if (message.startsWith("Error")) errorColor.copy(alpha = 0.1f) else primaryColor.copy(alpha = 0.1f),
                    border = BorderStroke(1.dp, if (message.startsWith("Error")) errorColor else primaryColor)
                ) {
                    Text(
                        text = message,
                        color = if (message.startsWith("Error")) errorColor else primaryColor,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun DropdownMenuBox(label: String, options: List<String>, onSelected: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    var selectedOption by remember { mutableStateOf(options[0]) }

    Box {
        OutlinedTextField(
            value = selectedOption,
            onValueChange = {},
            label = { Text(label, color = Color.Gray) },
            readOnly = true,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = true },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF6200EA),
                unfocusedBorderColor = Color.LightGray
            ),
            shape = RoundedCornerShape(8.dp),
            trailingIcon = {
                Icon(
                    Icons.Default.ArrowDropDown,
                    contentDescription = "Expand",
                    tint = Color(0xFF6200EA),
                    modifier = Modifier.clickable { expanded = true }
                )
            }
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.width(200.dp)
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option, color = Color.Black, fontSize = 16.sp) },
                    onClick = {
                        selectedOption = option
                        expanded = false
                        onSelected(option)
                    }
                )
            }
        }
    }
}