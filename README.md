# 🏫 Creche Management App

A comprehensive Android application designed to streamline creche (daycare) operations through role-based user management and child information systems. Built with modern Android development practices using Kotlin and Firebase.

## 📱 App Overview

The Creche Management App provides a centralized platform for managing daycare operations with distinct interfaces for parents, teachers, and administrators. The application ensures secure authentication and role-based access control to maintain data privacy and operational efficiency.

## ✨ Key Features

### 🔐 Authentication System
- **Firebase Authentication** with email/password login
- **Password reset functionality** via email
- **Role-based access control** (Parent, Teacher, Admin)
- **Secure user registration** with validation

### 👨‍👩‍👧‍👦 User Management
- **Multi-role support**: Parents, Teachers, and Administrators
- **Admin-controlled teacher registration**
- **User profile management** with contact information
- **Firestore database integration** for user data

### 👶 Child Management
- **Comprehensive child profiles** including:
  - Personal information (name, DOB, address, gender)
  - Medical information (allergies and details)
  - Emergency contact information
  - Parent association tracking
- **Child information viewing and editing**
- **Parent-child relationship management**

### 📊 Dashboard Systems
- **Parent Dashboard**: Child management and information access
- **Teacher Dashboard**: Student oversight and management
- **Admin Dashboard**: System administration and user management

## 🏗️ Technical Architecture

### Technology Stack
- **Language**: Kotlin
- **UI Framework**: Android Views with ViewBinding + Jetpack Compose
- **Backend**: Firebase (Authentication + Firestore)
- **Database**: Cloud Firestore
- **Architecture**: Activity-based with role separation

### SDK Information
- **Minimum SDK**: 24 (Android 7.0)
- **Target SDK**: 36 (Android 14)
- **Compile SDK**: 36
- **Java Version**: 11

### Key Dependencies
```kotlin
// Firebase Services
implementation(libs.firebase.auth)
implementation(libs.firebase.firestore)

// Android Core
implementation(libs.androidx.core.ktx)
implementation(libs.androidx.appcompat)
implementation(libs.material)

// UI Components
implementation(libs.androidx.constraintlayout)
implementation(libs.androidx.activity.compose)
implementation(platform(libs.androidx.compose.bom))

// Custom UI
implementation("de.hdodenhof:circleimageview:3.1.0")
```

## 🚀 Installation & Setup

### Prerequisites
- Android Studio Arctic Fox or later
- Android SDK API 24+
- Firebase project with Authentication and Firestore enabled
- Google Services configuration file (`google-services.json`)

### Setup Instructions

1. **Clone the repository**
   ```bash
   git clone https://github.com/ST10254797/CrecheManagementApp.git
   cd CrecheManagementApp
   ```

2. **Firebase Configuration**
   - Create a new Firebase project at [Firebase Console](https://console.firebase.google.com/)
   - Enable Authentication (Email/Password provider)
   - Enable Firestore Database
   - Download `google-services.json` and place it in the `app/` directory

3. **Open in Android Studio**
   - Open Android Studio
   - Select "Open an existing project"
   - Navigate to the cloned repository folder

4. **Sync and Build**
   - Let Android Studio sync the project
   - Build the project (`Build > Make Project`)

5. **Run the Application**
   - Connect an Android device or start an emulator
   - Click "Run" or press `Shift + F10`

## 📱 Application Flow

### User Journey
```
StartScreen → LoginActivity → Role-based Dashboard
                ↓
    ┌─────────────────┬─────────────────┬─────────────────┐
    │                 │                 │                 │
MainActivity    AdminDashboard    TeacherDashboard
(Parents)        (Admins)         (Teachers)
    │                 │                 │
    └─→ ChildInfo     └─→ AddAdmin      └─→ [Features TBD]
        Activity          Activity
```

## 🗃️ Data Models

### User Model
```kotlin
data class Users(
    val firstName: String,
    val lastName: String,
    val email: String,
    val number: String,
    val role: String  // "parent", "teacher", "admin"
)
```

### Child Model
```kotlin
data class Child(
    val firstName: String,
    val lastName: String,
    val dob: String,
    val address: String,
    val gender: String,
    val allergies: String,
    val allergiesDetail: String,
    val emergencyContact: String,
    val parentID: String,
    val parentEmail: String
)
```

## 🔒 Security Features

- **Firebase Authentication** ensures secure user login
- **Role-based access control** prevents unauthorized access
- **Email verification** for password resets
- **Firestore security rules** protect user data
- **Input validation** prevents malformed data

## 🧪 Testing

The project includes testing infrastructure:
- **Unit Tests**: JUnit framework
- **UI Tests**: Espresso and Compose testing
- **Instrumented Tests**: Android Test Runner

Run tests with:
```bash
./gradlew test           # Unit tests
./gradlew connectedAndroidTest  # Instrumented tests
```

## 📂 Project Structure

```
app/
├── src/main/java/com/example/myapplication/
│   ├── MainActivity.kt              # Parent dashboard
│   ├── LoginActivity.kt             # Authentication
│   ├── RegisterActivity.kt          # User registration
│   ├── StartScreen.kt               # App entry point
│   ├── AdminDashboardActivity.kt    # Admin interface
│   ├── TeacherDashboardActivity.kt  # Teacher interface
│   ├── ChildInfoActivity.kt         # Child management
│   ├── AddAdminActivity.kt          # Admin creation
│   ├── Child.kt                     # Child data model
│   └── Users.kt                     # User data model
├── src/main/res/
│   ├── layout/                      # XML layouts
│   ├── values/                      # Resources
│   └── mipmap/                      # App icons
└── build.gradle.kts                 # Build configuration
```

## 🚧 Future Enhancements

- [ ] **Attendance Tracking**: Daily check-in/check-out system
- [ ] **Meal Planning**: Nutritional meal scheduling
- [ ] **Photo Sharing**: Secure image sharing with parents
- [ ] **Messaging System**: Parent-teacher communication
- [ ] **Report Generation**: Progress and attendance reports
- [ ] **Push Notifications**: Real-time updates
- [ ] **Offline Support**: Local data caching

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## 📄 License

This project is developed for educational purposes as part of computer science coursework.

## 👨‍💻 Developers

**Students ID**: ST10254797, ST10377479, ST10378552, ST10247932, ST10266914, ST10279132
**Project**: Creche Management Application  
**Technology Stack**: Android (Kotlin) + Firebase

---

### 📞 Support

For issues or questions regarding this project:
- Create an issue in the GitHub repository
- Review the Firebase documentation for backend-related queries
- Check Android developer documentation for UI/UX components

**Built with ❤️ for efficient creche management**
