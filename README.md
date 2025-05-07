# 🌥️ Clouda - Your Personal Cloud Storage Solution

Welcome to **Clouda**, a modern Android application designed to provide seamless cloud storage functionality, inspired by platforms like Google Drive. With Clouda, you can securely upload, download, manage, and back up your files using a user-friendly interface built with **Jetpack Compose**. Powered by **Supabase** and **Firebase**, Clouda ensures efficient file storage, real-time metadata syncing, and robust authentication. 🚀

This README provides a comprehensive guide to Clouda, including its features, tech stack, system architecture, file structure, setup instructions, and usage guide. Let’s dive in! 😊

---

## 📖 Table of Contents
- [What is Clouda?](#what-is-clouda)
- [Key Features](#key-features)
- [Tech Stack](#tech-stack)
- [System Architecture](#system-architecture)
- [Project Structure](#project-structure)
- [Setup Instructions](#setup-instructions)
- [How to Use Clouda](#how-to-use-clouda)
- [Contributing](#contributing)
- [Team](#team)
- [License](#license)

---

## 🌟 What is Clouda?

Clouda is an Android application that allows users to store, manage, and access files in the cloud. It combines the power of **Supabase Storage** for file operations and **Firebase** for authentication and metadata management. With features like manual file uploads, automatic folder backups, and a sleek UI, Clouda is designed for individuals seeking a reliable and intuitive cloud storage solution. 📂

### 🎯 Goals
- Provide a secure and user-friendly cloud storage experience.
- Enable automatic backups for selected folders to prevent data loss.
- Offer seamless file management (upload, download, rename, delete, share).
- Support cross-device access with real-time synchronization.

---

## ✨ Key Features

Clouda is packed with features to make file management a breeze. Here’s what you can do:

- **Account Creation & Login** 🔐
  - Securely sign in using **Firebase Authentication** with Google Sign-In.
  - Future support planned for Apple Sign-In.

- **File Upload & Download** 📤📥
  - Manually upload individual files or entire folders to **Supabase Storage**.
  - Download files to your device’s cache or Downloads folder.
  - Access files anytime, anywhere with an internet connection.

- **File Management** 🗂️
  - **Rename**: Change file names while preserving extensions.
  - **Delete**: Remove files from the cloud and their metadata.
  - **Share**: Download and share files via other apps (e.g., WhatsApp, email).
  - **Search & Sort**: Search files by name and sort by name or upload time (ascending/descending).

- **Automatic Backup** 🔄
  - Select folders for automatic backup to Supabase.
  - Files in selected folders are uploaded automatically, reducing the risk of data loss.
  - Folder URIs are persisted for continuous access.

- **User Interface** 🎨
  - Modern, responsive UI built with **Jetpack Compose**.
  - Displays the 10 most recent files in a scrollable view on the home screen.
  - Navigation drawer for accessing backup settings and sign-out.
  - Theme toggle (dark/light) with dark theme as default for power efficiency.
  - Custom branding with the **Sen** font and polished splash screen.

- **Efficient Background Processing** ⚙️
  - Uses **WorkManager** for scheduling automatic backup tasks.
  - Handles file uploads and downloads efficiently in the background.

---

## 🛠️ Tech Stack

Clouda is built with a modern Android development stack to ensure performance, scalability, and maintainability. Here’s what powers Clouda:

- **Language**: Kotlin 🦸‍♂️
  - Modern, concise, and the official language for Android development.
- **UI Framework**: Jetpack Compose 🎨
  - Declarative UI framework for dynamic, responsive interfaces.
- **Storage & Database**:
  - **Supabase Storage** ☁️: Primary backend for file uploads, downloads, and management.
  - **Supabase Database**: Stores file metadata (name, user ID, timestamp) via REST API.
  - **Firestore** 🔥: Stores user metadata and file details for real-time synchronization.
  - **Room** 🗄️: Local caching for offline support (future implementation).
- **Authentication**: Firebase Authentication 🔒
  - Supports Google Sign-In for secure user management.
- **Background Tasks**: WorkManager ⏰
  - Schedules automatic backups and file sync tasks.
- **Networking**: OkHttp 🌐
  - Handles HTTP requests to Supabase’s REST API.
- **System Tools**:
  - **SensorManager**: For potential sensor-based features (future use).
  - **ConnectivityManager**: Monitors network status for reliable file operations.
- **Dependencies**:
  - Firebase SDKs (Authentication, Firestore, Storage)
  - Supabase Kotlin SDK
  - Jetpack Compose libraries
  - Google Play Services
  - Material Design 3 components

---

## 🏗️ System Architecture

Clouda’s architecture is designed for modularity and scalability, integrating client-side mobile components with cloud-based services. Here’s a breakdown:

### Client-Side Mobile App 📱
- **User Interface Components**:
  - **Splash Screen**: Displays branding and initializes resources (`SplashScreen.kt`).
  - **Login Screen**: Handles Google Sign-In (`LoginScreen.kt`).
  - **Home Screen**: Shows recent files, search bar, and manual upload/download options (`MainScreen.kt`).
  - **File List Screen**: Lists all uploaded files with search and sort functionality (`FileListScreen.kt`).
  - **Folder Detail Screen**: Displays files in a selected backup folder (`FolderDetailScreen.kt`).
  - **Navigation Drawer**: Provides access to backup settings and sign-out (`AppDrawerContent`).
- **Local Storage**:
  - Uses **SharedPreferences** to store folder URIs for automatic backups.
  - Future support for **Room** for offline caching.
- **Background Services**:
  - **WorkManager**: Schedules automatic backup tasks and file sync operations.

### Cloud Services ☁️
- **Firebase Authentication**:
  - Manages user login and session state.
- **Supabase Storage**:
  - Stores user files in user-specific paths (`<userId>/<fileName>`).
  - Supports signed URLs for secure downloads.
- **Supabase Database**:
  - Stores file metadata via REST API.
- **Firestore**:
  - Real-time synchronization of user metadata and file details.

### Data Flow 🔄
1. User signs in via Firebase Authentication.
2. Files are uploaded to Supabase Storage, with metadata saved to Supabase Database or Firestore.
3. Automatic backups use WorkManager to upload files from selected folders.
4. File lists are fetched from Supabase/Firestore and displayed in the UI.
5. Downloads and shares use signed URLs or direct file access.

---

## 📁 Project Structure

Here’s an overview of Clouda’s key files and their responsibilities:

- **`MainActivity.kt`**:
  - Entry point of the app.
  - Sets up Jetpack Compose, theme (dark/light), and navigation.
- **`SplashScreen.kt`**:
  - Displays the app’s branding and checks authentication status.
  - Redirects to login or main screen.
- **`LoginScreen.kt`**:
  - Handles Google Sign-In using Firebase Authentication.
  - Displays custom Sen font and branding.
- **`MainScreen.kt`**:
  - Home screen with recent files, search bar, and manual upload/download buttons.
  - Integrates navigation drawer and theme toggle.
- **`FileListScreen.kt`**:
  - Lists all uploaded files with search and sort options.
  - Supports file actions (download, rename, delete, share).
- **`FolderDetailScreen.kt`**:
  - Shows files in a selected backup folder.
- **`Navigation.kt`**:
  - Defines Jetpack Navigation routes and handles folder picker for backups.
- **`SupabaseStorageHelper.kt`**:
  - Manages file operations (upload, download, rename, delete, share) with Supabase Storage.
  - Handles metadata storage via Supabase Database REST API.
- **`FirebaseStorageHelper.kt`**:
  - Secondary storage backend for file uploads and downloads.
  - Saves metadata to Firestore.
- **`LoginViewModel.kt`**:
  - Manages authentication state and Google Sign-In logic.
- **`AppDrawerContent.kt`**:
  - Navigation drawer UI with backup settings and sign-out options.
- **`build.gradle.kts`**:
  - Defines dependencies (Firebase, Supabase, Compose, etc.) and build configuration.
  - Minimum SDK: 24, Target SDK: 35.

---

## 🚀 Setup Instructions

Follow these steps to set up and run Clouda on your local machine:

### Prerequisites
- **Android Studio** (latest stable version)
- **JDK 17** or higher
- **Supabase Account** (for storage and database)
- **Firebase Project** (for authentication and Firestore)
- Android device/emulator with **API 24** or higher

### Steps
1. **Clone the Repository**:
   ```bash
   git clone <repository-url>
   cd clouda
   ```

2. **Set Up Supabase**:
   - Create a Supabase project at [supabase.com](https://supabase.com).
   - Obtain your **Supabase URL** and **API Key**.
   - Create a storage bucket (e.g., `uploads`) and set permissions for authenticated users.
   - Update `SupabaseStorageHelper.kt` with your Supabase credentials:
     ```kotlin
     private const val SUPABASE_URL = "your-supabase-url"
     private const val SUPABASE_KEY = "your-supabase-key"
     ```
     **Note**: For production, store credentials securely (e.g., in `local.properties`).

3. **Set Up Firebase**:
   - Create a Firebase project at [console.firebase.google.com](https://console.firebase.google.com).
   - Enable **Authentication** (Google Sign-In) and **Firestore**.
   - Add your Android app to the Firebase project and download `google-services.json`.
   - Place `google-services.json` in the `app/` directory.
   - Ensure Firebase dependencies are included in `build.gradle.kts`.

4. **Sync Project**:
   - Open the project in Android Studio.
   - Sync the project with Gradle (`File > Sync Project with Gradle Files`).

5. **Run the App**:
   - Connect an Android device or start an emulator.
   - Click `Run` in Android Studio to build and install the app.

6. **Test the App**:
   - Sign in with a Google account.
   - Upload files, enable automatic backups, and test file management features.

---

## 📱 How to Use Clouda

1. **Launch the App**:
   - The splash screen displays the Clouda logo and initializes resources.
2. **Sign In**:
   - Use Google Sign-In on the login screen to authenticate.
3. **Home Screen**:
   - View the 10 most recent files in a scrollable list.
   - Use the search bar to find files.
   - Click the hamburger button to open the navigation drawer.
   - Toggle the theme (dark/light) using the manage account button.
4. **File Management**:
   - **Upload**: Click the upload button to select files or folders.
   - **Download**: Select a file from the file list and click download.
   - **Rename/Delete/Share**: Use the file list screen’s action buttons.
   - **Sort**: Sort files by name or upload time.
5. **Automatic Backup**:
   - Open the navigation drawer and select “Add Backup Folder.”
   - Choose a folder using the folder picker.
   - Files in the selected folder are automatically uploaded to Supabase.
6. **Sign Out**:
   - Use the sign-out option in the navigation drawer.

---

## 🤝 Contributing

We welcome contributions to make Clouda even better! To contribute:

1. Fork the repository.
2. Create a new branch (`git checkout -b feature/your-feature`).
3. Make your changes and commit (`git commit -m "Add your feature"`).
4. Push to your branch (`git push origin feature/your-feature`).
5. Open a pull request with a detailed description of your changes.

Please follow the code style and include tests for new features. 📝

---

## 👥 Team

Clouda was built by a team of two for a semester project submission:
- **Tejas Jaiswal**
- **Dhruv Sharma**

---

Happy cloud storing with Clouda! 🌥️ If you have any questions or feedback, feel free to reach out. 😊
