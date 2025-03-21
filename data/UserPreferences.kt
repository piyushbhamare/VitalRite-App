package com.example.vitalrite_1.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// Create a DataStore instance
val Context.userDataStore: DataStore<Preferences> by preferencesDataStore(name = "user_prefs")

class UserPreferences(private val context: Context) {
    companion object {
        private val USER_TYPE_KEY = stringPreferencesKey("user_type")
        private val USER_NAME_KEY = stringPreferencesKey("user_name")
    }

    // Get the user type as a Flow
    val userType: Flow<String?> = context.userDataStore.data
        .map { preferences ->
            preferences[USER_TYPE_KEY]
        }

    // Get the user name as a Flow
    val userName: Flow<String?> = context.userDataStore.data
        .map { preferences ->
            preferences[USER_NAME_KEY]
        }

    // Save the user type
    suspend fun saveUserType(userType: String?) {
        context.userDataStore.edit { preferences ->
            if (userType != null) {
                preferences[USER_TYPE_KEY] = userType
            } else {
                preferences.remove(USER_TYPE_KEY)
            }
        }
    }

    // Save the user name
    suspend fun saveUserName(userName: String?) {
        context.userDataStore.edit { preferences ->
            if (userName != null) {
                preferences[USER_NAME_KEY] = userName
            } else {
                preferences.remove(USER_NAME_KEY)
            }
        }
    }

    // Clear the user type
    suspend fun clearUserType() {
        context.userDataStore.edit { preferences ->
            preferences.remove(USER_TYPE_KEY)
        }
    }

    // Clear the user name
    suspend fun clearUserName() {
        context.userDataStore.edit { preferences ->
            preferences.remove(USER_NAME_KEY)
        }
    }
}