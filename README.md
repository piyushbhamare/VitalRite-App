# VitalRite
**Team: CloudVision**
VitalRite is a modern healthcare application built for Android using Kotlin and Jetpack Compose, designed to streamline medical management for both patients and doctors. It integrates Firebase for authentication and real-time data storage, offering features like appointment scheduling, prescription management, and medicine reminders. The app aims to enhance patient care and doctor efficiency with a user-friendly interface and robust functionality.

## Features

### For Patients
- **User Dashboard: View upcoming appointments, active prescriptions, and reminders at a glance.
- **E-Prescriptions: Access and manage digital prescriptions with detailed medicine schedules.
- **Appointment Booking: Schedule appointments with doctors based on their availability.
- **Medical Reports: Store and review medical history and reports.
- **Reminders: Receive notifications for medicine intake with snooze and taken options.

### For Doctors
- **Doctor Dashboard: Monitor today's appointments and manage patient care.
- **Prescribe Medicines: Create and assign prescriptions to patients seamlessly.
- **Appointment Management: View and handle upcoming appointments.
- **Calendar: Set availability and holidays to manage schedules effectively.
- **SOS: Send emergency alerts to a patient's emergency contact.

### Technical Highlights
- Built with **Kotlin** and **Jetpack Compose** for a modern, reactive UI.
- Uses **Firebase Authentication** for secure user login and **Firestore** for real-time data management.
Implements **AlarmManager** for precise reminder scheduling and **WorkManager** for background tasks.
Follows **MVVM architecture** with a clean folder structure for scalability.

## Prerequisites
- **Android Studio: Version 2023.1.1 or later (with Kotlin plugin).
- **JDK: Version 17 or higher.
- **Firebase Account: For authentication and Firestore setup.
- **Google Play Services: Required for Firebase integration.
- **Minimum API: Android 7.0 (API 24).

## Installation

1. **Clone the Repository**:
git clone https://github.com/yourusername/vitalrite.git
cd vitalrite

2. **Set Up Firebase**:
- Create a Firebase project in the Firebase Console.
- Add an Android app to your project and download the google-services.json file.
- Place the google-services.json file in the app/ directory of the project.

3. **Configure Firestore Rules**:
- Copy the Firestore rules from FirestoreRules in the project to your Firebase Firestore security rules:

rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    // Users collection
    match /Users/{userId} {
      allow read: if request.auth != null;
      allow write: if request.auth != null && request.auth.uid == userId;
      // Allow doctors to update only the activePrescriptions field
      allow update: if request.auth != null && 
                     exists(/databases/$(database)/documents/Doctors/$(request.auth.uid)) && 
                     request.resource.data.diff(resource.data).affectedKeys().hasOnly(['activePrescriptions']);
    
      // Reminders subcollection under Users
      match /Reminders/{reminderId} {
        // Allow the user to read and write their own reminders
        allow read, write: if request.auth != null && request.auth.uid == userId;
        // Allow doctors to create reminders for the user
        allow create: if request.auth != null && 
                      exists(/databases/$(database)/documents/Doctors/$(request.auth.uid));
      }
    }

    // Doctors collection
    match /Doctors/{doctorId} {
      allow read: if request.auth != null;
      allow write: if request.auth != null && request.auth.uid == doctorId;
    }

    // Prescriptions collection
    match /Prescriptions/{prescriptionId} {
      // Allow the user to read their own prescriptions
      allow read: if request.auth != null && request.auth.uid == resource.data.userId;
      // Allow doctors to read and write prescriptions
      allow read, write: if request.auth != null && 
                         exists(/databases/$(database)/documents/Doctors/$(request.auth.uid));
    }

    // Appointments collection
    match /Appointments/{appointmentId} {
      // Allow the user or doctor to read the appointment
      allow read: if request.auth != null && 
                   (request.auth.uid == resource.data.userId || 
                    request.auth.uid == resource.data.doctorId);
      // Allow the user to create an appointment for themselves
      allow create: if request.auth != null && 
                     request.auth.uid == request.resource.data.userId;
      // Allow the user to update their own appointment
      allow update: if request.auth != null && 
                     request.auth.uid == resource.data.userId;
      // Allow the user or doctor to delete the appointment
      allow delete: if request.auth != null && 
                     (request.auth.uid == resource.data.userId || 
                      request.auth.uid == resource.data.doctorId);
    }

    // DoctorAvailability collection
    match /DoctorAvailability/{doctorId} {
      allow read: if request.auth != null;
      allow write: if request.auth != null && request.auth.uid == doctorId;
    }

    // Allow querying collections
    match /Users {
      allow read: if request.auth != null;
    }
    match /Doctors {
      allow read: if request.auth != null;
    }
    match /Appointments {
      allow read: if request.auth != null;
    }
  }
}

4. **Open in Android Studio**:
- Open the project in Android Studio.
- Sync the project with Gradle files (File > Sync Project with Gradle Files).

5. **Run the App**:
- Connect an Android device or start an emulator.
- Click Run > Run 'app' in Android Studio.

  
## Usage
1. **Sign Up**:
- Launch the app and navigate to the signup screen.
- Choose a role (User or Doctor) and fill in the required details.

2. **Log In**:
- Use your registered email and password to log in.
- The app will direct you to the appropriate dashboard based on your role.

3. **Permissions**:
- Grant notification and exact alarm permissions when prompted for reminders to work correctly.

4. **Explore Features**:
- Users: Book appointments, view prescriptions, and manage reminders.
- Doctors: Prescribe medicines, set availability, and send SOS alerts.

## Project Structure
com.example.vitalrite_1/
├── MainActivity.kt
├── UserTypeViewModel.kt
├── data
│   ├── models.kt
│   └── Repository.kt
│   └── BootReceiver.kt
│   └── ReminderActionReceiver.kt
│   └── UserPrefences.kt
├── ui
│   ├── auth
│   │   ├── LoginScreen.kt
│   │   ├── SignupScreen.kt
│   │   ├── DropdownMenuBox.kt
│   ├── user
│   │   ├── UserDashboard.kt
│   │   ├── EPreScreen.kt
│   │   ├── AppointmentsScreen.kt
│   │   ├── BookAppointmentScreen.kt
│   │   ├── MedicalReportsScreen.kt
│   │   └── RemindersScreen.kt
│   │   └── PrescriptionDetailScreen.kt
│   ├── doctor
│   │   ├── DoctorDashboard.kt
│   │   ├── AppointmentScreen.kt
│   │   ├── PrescribeScreen.kt
│   │   ├── CalendarScreen.kt
│   │   └── SOSScreen.kt
│   └── components
│       ├── BottomNav.kt
│       └── Common.kt
│       └── TopBar.kt
│   ├── utils
│   │   ├── Utils.kt

## Dependencies

- **Jetpack Compose: UI toolkit for building native Android UIs.
- **Firebase: Authentication (firebase-auth), Firestore (firebase-firestore).
- **Coroutines: For asynchronous programming (kotlinx-coroutines).
- **WorkManager: Background task scheduling (androidx.work).
- **Navigation: Screen navigation (androidx.navigation).

## Add these to your build.gradle (project-level):
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    id("com.google.gms.google-services") version "4.4.2" apply false
}

## Add these to your build.gradle (app-level):
dependencies {
    // AndroidX Core & UI
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.2")
    implementation("androidx.activity:activity-compose:1.8.0")

    // Jetpack Compose
    implementation(platform("androidx.compose:compose-bom:2023.10.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")

    // Firebase (Using BoM to manage versions automatically)
    implementation(platform("com.google.firebase:firebase-bom:33.8.0"))
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-firestore")
    implementation("com.google.firebase:firebase-storage")
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.firebase:firebase-messaging")
    implementation("com.google.firebase:firebase-functions")

    // Google Play Services (Managed by BoM)
    implementation("com.google.android.gms:play-services-base")
    implementation("com.google.android.gms:play-services-location:21.0.1")
    implementation("com.google.android.gms:play-services-maps:18.1.0")

    // UI & Material Components
    implementation("com.google.android.material:material:1.9.0")

    // Networking & API Calls
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")

    // Image Loading & Caching
    implementation("com.github.bumptech.glide:glide:4.16.0")
    implementation(libs.androidx.navigation.runtime.android)
    implementation(libs.androidx.navigation.compose)
    kapt("com.github.bumptech.glide:compiler:4.16.0")

    // Room Database (Local Storage)
    implementation("androidx.room:room-runtime:2.5.2")
    kapt("androidx.room:room-compiler:2.5.2")
    implementation("androidx.room:room-ktx:2.5.2")

    // WorkManager (Background Tasks)
    implementation("androidx.work:work-runtime-ktx:2.8.1")

    // Testing Dependencies
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    // Icons
    implementation("androidx.compose.material:material-icons-core")
    implementation("androidx.compose.material:material-icons-extended")

    // ViewModel
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.0")

    //DataStore
    implementation("androidx.datastore:datastore-preferences:1.0.0")
}
