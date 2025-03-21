package com.example.vitalrite_1

import android.util.Log
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.vitalrite_1.data.UserPreferences
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull

class UserTypeViewModel(private val userPreferences: UserPreferences) : ViewModel() {
    private val _userType = mutableStateOf<String?>(null)
    val userType: State<String?> get() = _userType

    private val _userName = mutableStateOf<String?>(null)
    val userName: State<String?> get() = _userName

    private val _errorMessage = mutableStateOf<String?>(null)
    val errorMessage: State<String?> get() = _errorMessage

    // Initialize user type and name from DataStore
    init {
        viewModelScope.launch {
            _userType.value = userPreferences.userType.firstOrNull()
            _userName.value = userPreferences.userName.firstOrNull()
        }
    }

    // Load user type and name from DataStore on initialization
    fun initializeUserType() {
        viewModelScope.launch {
            val cachedUserType = userPreferences.userType.firstOrNull()
            val cachedUserName = userPreferences.userName.firstOrNull()
            _userType.value = cachedUserType
            _userName.value = cachedUserName
        }
    }

    // Determine user type and fetch user name, then save to DataStore
    fun determineUserType(auth: FirebaseAuth, firestore: FirebaseFirestore) {
        viewModelScope.launch {
            // Check if already cached
            val cachedUserType = userPreferences.userType.firstOrNull()
            val cachedUserName = userPreferences.userName.firstOrNull()
            if (cachedUserType != null && cachedUserName != null) {
                _userType.value = cachedUserType
                _userName.value = cachedUserName
                return@launch
            }

            val userId = auth.currentUser?.uid ?: run {
                Log.e("UserTypeViewModel", "No authenticated user found")
                _errorMessage.value = "No authenticated user found"
                _userType.value = null
                _userName.value = null
                userPreferences.saveUserType(null)
                userPreferences.saveUserName(null)
                return@launch
            }

            val result = withTimeoutOrNull(2000L) { // Reduced timeout to 2 seconds
                try {
                    val userDoc = firestore.collection("Users").document(userId).get().await()
                    if (userDoc.exists()) {
                        Pair("User", userDoc.getString("name") ?: "User")
                    } else {
                        val doctorDoc = firestore.collection("Doctors").document(userId).get().await()
                        if (doctorDoc.exists()) {
                            Pair("Doctor", doctorDoc.getString("name") ?: "Doctor")
                        } else {
                            Pair(null, null)
                        }
                    }
                } catch (e: Exception) {
                    Log.e("UserTypeViewModel", "Error determining user type: ${e.message}")
                    _errorMessage.value = "Error determining user type: ${e.message}"
                    null
                }
            }

            _userType.value = result?.first
            _userName.value = result?.second
            userPreferences.saveUserType(result?.first)
            userPreferences.saveUserName(result?.second)
            if (result?.first == null) {
                auth.signOut()
            }
        }
    }

    // Clear user type and name on logout
    fun clearUserType() {
        viewModelScope.launch {
            _userType.value = null
            _userName.value = null
            userPreferences.clearUserType()
            userPreferences.clearUserName()
        }
    }
}

