package com.example.vitalrite_1.data

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class ReminderActionReceiver : BroadcastReceiver() {
    private val TAG = "ReminderActionReceiver"

    override fun onReceive(context: Context, intent: Intent) {
        val reminderId = intent.getStringExtra("reminderId") ?: return
        val timeIndex = intent.getIntExtra("timeIndex", -1)
        if (timeIndex == -1) return
        val firestore = FirebaseFirestore.getInstance()
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        // Fetch the reminder from Firestore
        firestore.collection("Users").document(userId)
            .collection("Reminders").document(reminderId).get()
            .addOnSuccessListener { document ->
                val reminder = document.toObject(Reminder::class.java)
                if (reminder != null) {
                    when (intent.action) {
                        "ACTION_TAKEN" -> {
                            // Mark the specific time as taken
                            val updatedTaken = reminder.taken.toMutableList().apply {
                                if (size <= timeIndex) {
                                    repeat(timeIndex - size + 1) { add(false) }
                                }
                                set(timeIndex, true)
                            }
                            val updatedSnoozeTimes = reminder.snoozeTimes.toMutableList().apply {
                                if (size <= timeIndex) {
                                    repeat(timeIndex - size + 1) { add(null) }
                                }
                                set(timeIndex, null)
                            }
                            val updatedReminder = reminder.copy(taken = updatedTaken, snoozeTimes = updatedSnoozeTimes)
                            Repository.updateReminder(userId, updatedReminder, onSuccess = {
                                Repository.cancelReminder(context, reminder, timeIndex)
                                Log.d(TAG, "Reminder $reminderId at index $timeIndex marked as taken")
                            })
                        }
                        "ACTION_SNOOZE" -> {
                            // Snooze the specific time
                            val snoozeMinutes = if (reminder.times[timeIndex].contains("Before")) 5 else 10
                            val snoozeTime = Calendar.getInstance().apply {
                                add(Calendar.MINUTE, snoozeMinutes)
                            }
                            val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
                            val updatedSnoozeTimes = reminder.snoozeTimes.toMutableList().apply {
                                if (size <= timeIndex) {
                                    repeat(timeIndex - size + 1) { add(null) }
                                }
                                set(timeIndex, timeFormat.format(snoozeTime.time))
                            }
                            val updatedReminder = reminder.copy(snoozeTimes = updatedSnoozeTimes)
                            Repository.updateReminder(userId, updatedReminder, onSuccess = {
                                // Fetch user data to reschedule the reminder
                                firestore.collection("Users").document(userId).get()
                                    .addOnSuccessListener { userDoc ->
                                        val user = userDoc.toObject(User::class.java)
                                        if (user != null) {
                                            Repository.scheduleReminder(context, updatedReminder, user, timeIndex)
                                            Log.d(TAG, "Reminder $reminderId at index $timeIndex snoozed to ${updatedReminder.snoozeTimes[timeIndex]}")
                                        }
                                    }
                            })
                        }
                    }
                    // Cancel the notification after action
                    with(NotificationManagerCompat.from(context)) {
                        cancel((reminderId.hashCode() + timeIndex))
                    }
                }
            }
    }
}