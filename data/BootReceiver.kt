package com.example.vitalrite_1.data

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class BootReceiver : BroadcastReceiver() {
    private val TAG = "BootReceiver"

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        Log.d(TAG, "Received BOOT_COMPLETED intent")

        val auth = FirebaseAuth.getInstance()
        val firestore = FirebaseFirestore.getInstance()
        val userId = auth.currentUser?.uid

        if (userId == null) {
            Log.w(TAG, "No user logged in, cannot reschedule reminders")
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Fetch user data
                val userDoc = firestore.collection("Users").document(userId).get().await()
                val user = userDoc.toObject(User::class.java)
                if (user == null) {
                    Log.e(TAG, "User data not found for userId: $userId")
                    return@launch
                }

                // Fetch all reminders for the user
                val reminderDocs = firestore.collection("Users")
                    .document(userId)
                    .collection("Reminders")
                    .get()
                    .await()

                val reminders = reminderDocs.mapNotNull { it.toObject(Reminder::class.java) }
                Log.d(TAG, "Found ${reminders.size} reminders to reschedule")

                // Reschedule each reminder for each time slot
                reminders.forEach { reminder ->
                    reminder.times.forEachIndexed { timeIndex, time ->
                        // Only reschedule if the time slot hasn't been taken
                        if (reminder.taken.getOrNull(timeIndex) != true) {
                            try {
                                Repository.scheduleReminder(context, reminder, user, timeIndex)
                                Log.d(TAG, "Rescheduled reminder: ${reminder.id} for ${reminder.medicineName} at $time (index $timeIndex)")
                            } catch (e: Exception) {
                                Log.e(TAG, "Failed to reschedule reminder ${reminder.id} at index $timeIndex: ${e.message}")
                            }
                        } else {
                            Log.d(TAG, "Skipping rescheduling for reminder ${reminder.id} at index $timeIndex: already taken")
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error rescheduling reminders after reboot: ${e.message}")
            }
        }
    }
}