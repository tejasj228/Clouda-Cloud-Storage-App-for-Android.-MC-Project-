package com.mcp.oogabooga.navigation

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.firebase.auth.FirebaseAuth
import com.mcp.oogabooga.screens.FileListScreen
import com.mcp.oogabooga.screens.FolderDetailScreen
import com.mcp.oogabooga.screens.LoginScreen
import com.mcp.oogabooga.screens.MainScreen
import com.mcp.oogabooga.screens.SplashScreen
import com.mcp.oogabooga.supabase.SupabaseStorageHelper.uploadFolderToSupabase
import com.mcp.oogabooga.viewmodel.LoginViewModel


@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun AppNavigation(
    isDarkTheme: Boolean,
    toggleTheme: () -> Unit
) {
    val navController = rememberNavController()
    val loginViewModel: LoginViewModel = viewModel()
    val context = LocalContext.current

    // Shared state: lifted here
    val backupFolders = remember { mutableStateListOf<Uri>() }
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: "anonymous"
    val prefs = context.getSharedPreferences("clouda_prefs", Context.MODE_PRIVATE)
    val userFolderKey = "backup_uris_$currentUserId"

    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        uri?.let {
            context.contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION)

            if (!backupFolders.contains(it)) {
                backupFolders.add(it)

                prefs.edit().putStringSet(userFolderKey, backupFolders.map { it.toString() }.toSet()).apply()

                uploadFolderToSupabase(context, it)
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = "splash"
    ) {
        composable("splash") { SplashScreen(navController) }
        composable("login") { LoginScreen(navController) }
        composable("main") {
            MainScreen(
                navController = navController,
                loginViewModel = loginViewModel,
                backupFolders = backupFolders,
                folderPickerLauncher = folderPickerLauncher,
                isDarkTheme = isDarkTheme,
                toggleTheme = toggleTheme
            )
        }
        composable("file_list") {
            FileListScreen(
                navController = navController,
                backupFolders = backupFolders,
                folderPickerLauncher = folderPickerLauncher,
                isDarkTheme = isDarkTheme,
                toggleTheme = toggleTheme
            )
        }

        composable("folder_detail/{folderName}") { backStackEntry ->
            val folderName = backStackEntry.arguments?.getString("folderName") ?: ""
            FolderDetailScreen(folderName = folderName)
        }
    }
}
