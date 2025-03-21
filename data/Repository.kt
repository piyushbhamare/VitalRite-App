package com.example.vitalrite_1.data

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.concurrent.TimeUnit
import android.app.NotificationChannel
import android.app.NotificationManager
import java.util.*

object Repository {
    private const val TAG = "Repository"

    private fun getAuth() = FirebaseAuth.getInstance()
    private fun getFirestore() = FirebaseFirestore.getInstance()

    fun registerUser(user: User, password: String, onSuccess: () -> Unit, onFailure: (String) -> Unit) {
        Log.d(TAG, "Attempting to register user with email: ${user.email}")
        getAuth().createUserWithEmailAndPassword(user.email, password).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val uid = getAuth().currentUser?.uid
                if (uid != null) {
                    getFirestore().collection("Users").document(uid).set(user.copy(uid = uid))
                        .addOnSuccessListener {
                            Log.d(TAG, "User data saved to Firestore successfully")
                            onSuccess()
                        }
                        .addOnFailureListener { e ->
                            Log.e(TAG, "Error saving user data to Firestore: ${e.message}")
                            onFailure(e.message ?: "Error saving user data")
                        }
                } else {
                    onFailure("Failed to retrieve user ID")
                }
            } else {
                onFailure(task.exception?.message ?: "Registration failed")
            }
        }
    }

    fun registerDoctor(doctor: Doctor, password: String, onSuccess: () -> Unit, onFailure: (String) -> Unit) {
        Log.d(TAG, "Attempting to register doctor with email: ${doctor.email}")
        getAuth().createUserWithEmailAndPassword(doctor.email, password).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val uid = getAuth().currentUser?.uid
                if (uid != null) {
                    Log.d(TAG, "Authentication successful, UID: $uid")
                    val updatedDoctor = doctor.copy(uid = uid)
                    getFirestore().collection("Doctors").document(uid).set(updatedDoctor)
                        .addOnSuccessListener {
                            Log.d(TAG, "Doctor data saved to Firestore successfully")
                            onSuccess()
                        }
                        .addOnFailureListener { e ->
                            Log.e(TAG, "Error saving doctor data to Firestore: ${e.message}")
                            onFailure(e.message ?: "Error saving doctor data")
                        }
                } else {
                    Log.e(TAG, "Failed to retrieve user ID after authentication")
                    onFailure("Failed to retrieve user ID")
                }
            } else {
                Log.e(TAG, "Authentication failed: ${task.exception?.message}")
                onFailure("Authentication failed: ${task.exception?.message}")
            }
        }
    }

    fun login(email: String, password: String, onSuccess: (String) -> Unit, onFailure: (String) -> Unit) {
        Log.d(TAG, "Attempting to login with email: $email")
        getAuth().signInWithEmailAndPassword(email, password).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val uid = getAuth().currentUser?.uid
                if (uid != null) {
                    Log.d(TAG, "Login successful, UID: $uid")
                    getFirestore().collection("Users").document(uid).get()
                        .addOnSuccessListener { userDoc ->
                            if (userDoc.exists()) {
                                Log.d(TAG, "User found in Firestore")
                                onSuccess("User")
                            } else {
                                getFirestore().collection("Doctors").document(uid).get()
                                    .addOnSuccessListener { doctorDoc ->
                                        if (doctorDoc.exists()) {
                                            Log.d(TAG, "Doctor found in Firestore")
                                            onSuccess("Doctor")
                                        } else {
                                            Log.e(TAG, "User not registered in Firestore")
                                            onFailure("Not Registered")
                                        }
                                    }
                                    .addOnFailureListener { e ->
                                        Log.e(TAG, "Error checking doctor in Firestore: ${e.message}")
                                        onFailure(e.message ?: "Error checking doctor")
                                    }
                            }
                        }
                        .addOnFailureListener { e ->
                            Log.e(TAG, "Error checking user in Firestore: ${e.message}")
                            onFailure(e.message ?: "Error checking user")
                        }
                } else {
                    Log.e(TAG, "UID is null after login")
                    onFailure("Failed to retrieve user ID")
                }
            } else {
                Log.e(TAG, "Login failed: ${task.exception?.message}")
                onFailure("Invalid Credentials")
            }
        }
    }

    fun updateReminder(
        userId: String,
        reminder: Reminder,
        onSuccess: (Reminder) -> Unit = {},
        onFailure: (String) -> Unit = {}
    ) {
        val reminderId = if (reminder.id.isEmpty()) UUID.randomUUID().toString() else reminder.id
        val updatedReminder = reminder.copy(id = reminderId)
        val reminderData = hashMapOf(
            "id" to updatedReminder.id,
            "medicineName" to updatedReminder.medicineName,
            "times" to updatedReminder.times,
            "taken" to updatedReminder.taken,
            "snoozeTimes" to updatedReminder.snoozeTimes,
            "date" to updatedReminder.date
        )
        Log.d(TAG, "Saving reminder to Firestore for user $userId at path: Users/$userId/Reminders/$reminderId, data: $reminderData")
        getFirestore().collection("Users").document(userId)
            .collection("Reminders").document(reminderId)
            .set(reminderData)
            .addOnSuccessListener {
                Log.d(TAG, "Reminder $reminderId updated successfully")
                onSuccess(updatedReminder)
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error updating reminder $reminderId: ${e.message}")
                onFailure(e.message ?: "Error updating reminder")
            }
    }

    fun scheduleReminder(context: Context, reminder: Reminder, user: User, timeIndex: Int) {
        if (timeIndex >= reminder.times.size || reminder.taken.getOrNull(timeIndex) == true) return

        try {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager?
            if (alarmManager == null) {
                Log.e(TAG, "Failed to get AlarmManager service")
                throw IllegalStateException("AlarmManager service is unavailable")
            }

            val intent = Intent(context, ReminderReceiver::class.java).apply {
                putExtra("reminderId", reminder.id)
                putExtra("medicineName", reminder.medicineName)
                putExtra("time", reminder.times[timeIndex])
                putExtra("timeIndex", timeIndex)
            }

            val pendingIntent = PendingIntent.getBroadcast(
                context,
                (reminder.id.hashCode() + timeIndex),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
            ) ?: run {
                Log.e(TAG, "Failed to create PendingIntent for reminder: ${reminder.id} at index $timeIndex")
                throw IllegalStateException("Failed to create PendingIntent for reminder")
            }

            val triggerTime = reminder.getNextReminderTime(user, timeIndex)
            if (triggerTime == Long.MAX_VALUE) {
                Log.w(TAG, "Not scheduling reminder ${reminder.id} at index $timeIndex as it exceeds cutoff time")
                return
            }
            if (triggerTime <= System.currentTimeMillis()) {
                Log.w(TAG, "Trigger time for reminder ${reminder.id} at index $timeIndex is in the past: $triggerTime")
                return
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
                )
                Log.d(TAG, "Scheduled exact alarm for reminder ${reminder.id} at index $timeIndex at ${Date(triggerTime)}")
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
                )
                Log.d(TAG, "Scheduled reminder: ${reminder.id} for ${reminder.medicineName} at ${reminder.times[timeIndex]}, triggerTime: $triggerTime")
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException while scheduling reminder ${reminder.id} at index $timeIndex: ${e.message}")
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Failed to schedule reminder ${reminder.id} at index $timeIndex: ${e.message}")
            throw RuntimeException("Failed to schedule reminder: ${e.message}", e)
        }
    }

    fun cancelReminder(context: Context, reminder: Reminder, timeIndex: Int) {
        try {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager?
            if (alarmManager == null) {
                Log.e(TAG, "Failed to get AlarmManager service")
                throw IllegalStateException("AlarmManager service is unavailable")
            }

            val intent = Intent(context, ReminderReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                (reminder.id.hashCode() + timeIndex),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
            ) ?: run {
                Log.w(TAG, "PendingIntent not found for reminder: ${reminder.id} at index $timeIndex, possibly already canceled")
                return
            }

            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
            Log.d(TAG, "Canceled reminder: ${reminder.id} for ${reminder.medicineName} at index $timeIndex")
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException while canceling reminder ${reminder.id} at index $timeIndex: ${e.message}")
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Failed to cancel reminder ${reminder.id} at index $timeIndex: ${e.message}")
            throw RuntimeException("Failed to cancel reminder: ${e.message}", e)
        }
    }
}

class ReminderWorker(context: Context, params: WorkerParameters) : Worker(context, params) {
    override fun doWork(): Result {
        val medicineName = inputData.getString("medicineName") ?: return Result.failure()
        val time = inputData.getString("time") ?: return Result.failure()

        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "reminder_channel"
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Reminders", NotificationManager.IMPORTANCE_HIGH)
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Medicine Reminder")
            .setContentText("Time to take $medicineName ($time)")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
        return Result.success()
    }
}