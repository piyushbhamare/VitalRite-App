package com.example.vitalrite_1.data

import android.util.Log
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.PropertyName
import java.text.SimpleDateFormat
import java.util.*

data class User(
    val uid: String = "",
    val name: String = "",
    val age: String = "",
    val gender: String = "",
    val email: String = "",
    val breakfastTime: String = "",
    val lunchTime: String = "",
    val dinnerTime: String = "",
    val sleepTime: String = "",
    val bloodGroup: String = "",
    val medicalCondition: String = "",
    val operation: String = "",
    val allergy: String = "",
    val emergencyContact: String = "",
    val address: String = "",
    val activePrescriptions: List<String> = emptyList(),
    val lastReminderResetDate: String = "" // Add this field to track the last reset date
)

data class Doctor(
    val uid: String = "",
    val name: String = "",
    val nameLowercase: String = "",
    val age: String = "",
    val gender: String = "",
    val email: String = "",
    val degree: String = "",
    val specialization: String = "",
    val experience: String = "",
    val clinicName: String = "",
    val clinicAddress: String = "",
    val clinicPhone: String = "",
    val hospitalName: String = "",
    val hospitalAddress: String = "",
    val hospitalPhone: String = "",
)

@IgnoreExtraProperties
data class Appointment(
    val id: String = "",
    val userId: String = "",
    val doctorId: String = "",
    val patientName: String = "",
    val doctorName: String = "",
    val date: String = "",
    val time: String = "",
    val age: String = "",
    val gender: String = ""
) {
    fun isUpcoming(): Boolean {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        val appointmentDateTime = dateFormat.parse("$date $time") ?: return false
        return appointmentDateTime.after(Date())
    }

    fun isToday(): Boolean {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val appointmentDate = dateFormat.parse(date) ?: return false
        val today = Calendar.getInstance().apply { set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0) }
        val appointmentCalendar = Calendar.getInstance().apply { time = appointmentDate }
        return today.get(Calendar.YEAR) == appointmentCalendar.get(Calendar.YEAR) &&
                today.get(Calendar.MONTH) == appointmentCalendar.get(Calendar.MONTH) &&
                today.get(Calendar.DAY_OF_MONTH) == appointmentCalendar.get(Calendar.DAY_OF_MONTH)
    }
}

data class Prescription(
    val id: String = "",
    val userId: String = "",
    val name: String = "",
    val doctorName: String = "",
    val date: String = "",
    val mainCause: String = "",
    val medicines: List<Medicine> = emptyList(),
    val weight: String = "",
    val age: String = "",
    val expiryDate: String = "",
    val active: Boolean = false
)

data class DoctorAvailability(
    val doctorId: String = "",
    val openTiming: String = "",
    val closeTiming: String = "",
    val maxAppointmentsPerHour: Int = 0,
    val holidays: List<String> = emptyList()
)

data class Medicine(
    var name: String = "",
    var diagnosis: String = "",
    @PropertyName("time") var timeRaw: Any? = emptyList<String>(),
    var noOfDays: String = ""
)

data class Reminder(
    val id: String = "", // Empty by default, set when saving to Firestore
    val medicineName: String = "",
    val times: List<String> = emptyList(), // List of times for this medicine
    val taken: List<Boolean> = emptyList(), // List of taken statuses corresponding to times
    val snoozeTimes: List<String?> = emptyList(), // List of snooze times corresponding to times
    val date: String = "" // Date the reminder applies to (e.g., "2025-03-20")
) {
    fun getNextReminderTime(user: User, timeIndex: Int): Long {
        if (timeIndex >= times.size) return Long.MAX_VALUE

        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val dateTimeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

        // If there's a snooze time, use it
        val snoozeTime = snoozeTimes.getOrNull(timeIndex)
        if (snoozeTime != null) {
            try {
                // Combine the snooze time with the reminder's date
                val snoozeDateTime = dateTimeFormat.parse("$date $snoozeTime")
                val snoozeMillis = snoozeDateTime?.time ?: Long.MAX_VALUE
                // Check if the snooze time is in the past
                val cutoffTime = System.currentTimeMillis()
                if (snoozeMillis < cutoffTime) {
                    Log.d("Reminder", "Snooze time for reminder $id at index $timeIndex is in the past: $snoozeDateTime")
                    return Long.MAX_VALUE // Return a large value to indicate the time is invalid
                }
                Log.d("Reminder", "Calculated snooze trigger time for reminder $id at index $timeIndex: ${Date(snoozeMillis)} (snoozeTime: $snoozeTime)")
                return snoozeMillis
            } catch (e: Exception) {
                Log.e("Reminder", "Failed to parse snooze time for reminder $id at index $timeIndex: $snoozeTime, error: ${e.message}")
                return Long.MAX_VALUE
            }
        }

        // Otherwise, calculate the time based on the user's schedule
        val timeString = times.getOrNull(timeIndex) ?: return Long.MAX_VALUE
        val userTime = when (timeString) {
            "Before Breakfast" -> user.breakfastTime?.minus(15) ?: "08:00"
            "After Breakfast" -> user.breakfastTime ?: "08:15"
            "Before Lunch" -> user.lunchTime?.minus(15) ?: "13:00"
            "After Lunch" -> user.lunchTime ?: "13:15"
            "Before Dinner" -> user.dinnerTime?.minus(15) ?: "19:00"
            "After Dinner" -> user.dinnerTime ?: "19:15"
            "Before Sleep" -> user.sleepTime?.minus(15) ?: "22:00"
            else -> "08:00" // Default fallback
        }

        try {
            // Combine the time with the reminder's date
            val triggerDateTime = dateTimeFormat.parse("$date $userTime")
            val triggerMillis = triggerDateTime?.time ?: Long.MAX_VALUE
            // Check if the trigger time is in the past
            val cutoffTime = System.currentTimeMillis()
            if (triggerMillis < cutoffTime) {
                Log.d("Reminder", "Trigger time for reminder $id at index $timeIndex is in the past: $triggerDateTime")
                return Long.MAX_VALUE
            }
            Log.d("Reminder", "Calculated trigger time for reminder $id at index $timeIndex: ${Date(triggerMillis)} (time: $timeString, userTime: $userTime)")
            return triggerMillis
        } catch (e: Exception) {
            Log.e("Reminder", "Failed to parse trigger time for reminder $id at index $timeIndex: $userTime, error: ${e.message}")
            return Long.MAX_VALUE
        }
    }

    private fun String.minus(minutes: Int): String {
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        val calendar = Calendar.getInstance()
        calendar.time = timeFormat.parse(this) ?: return this
        calendar.add(Calendar.MINUTE, -minutes)
        return timeFormat.format(calendar.time)
    }
}