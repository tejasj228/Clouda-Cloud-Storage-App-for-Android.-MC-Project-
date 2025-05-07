package com.mcp.oogabooga.screens

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.mcp.oogabooga.R
import com.mcp.oogabooga.supabase.SupabaseStorageHelper
import com.mcp.oogabooga.supabase.SupabaseStorageHelper.FileMeta
import com.mcp.oogabooga.viewmodel.LoginViewModel
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.text.font.FontWeight
import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.launch
import com.google.firebase.auth.FirebaseAuth
import com.mcp.oogabooga.supabase.SupabaseStorageHelper.uploadFolderToSupabase
import androidx.core.content.edit
import com.mcp.oogabooga.ui.theme.OogaboogaTheme

@RequiresApi(Build.VERSION_CODES.O)
@SuppressLint("ContextCastToActivity")
@Composable
fun MainScreen(
    navController: NavController,
    loginViewModel: LoginViewModel,
    backupFolders: SnapshotStateList<Uri>,
    folderPickerLauncher: ManagedActivityResultLauncher<Uri?, Uri?>,
    isDarkTheme: Boolean,
    toggleTheme: () -> Unit
) {
    val context = LocalContext.current
    var fileList by remember { mutableStateOf<List<FileMeta>>(emptyList()) }

    var searchText by remember { mutableStateOf("") }
    var showRenameDialog by remember { mutableStateOf(false) }
    var fileToRename by remember { mutableStateOf<String?>(null) }
    val activity = LocalContext.current as Activity

    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: "anonymous"
    val prefs = context.getSharedPreferences("clouda_prefs", Context.MODE_PRIVATE)
    val userFolderKey = "backup_uris_$currentUserId"


    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            SupabaseStorageHelper.uploadFile(context, it) {
                SupabaseStorageHelper.getUploadedFiles { files -> fileList = files }
            }
        }
    }

    LaunchedEffect(Unit) {
        val savedUris = prefs.getStringSet(userFolderKey, emptySet()) ?: emptySet()
        backupFolders.clear()
        backupFolders.addAll(savedUris.mapNotNull { runCatching { Uri.parse(it) }.getOrNull() })

        SupabaseStorageHelper.getUploadedFiles { files -> fileList = files }
    }

    val filteredList = remember(fileList, searchText) {
        if (searchText.isBlank()) fileList
        else fileList.filter {
            it.file_name.substringAfterLast("/").contains(searchText.trim(), ignoreCase = true)
        }
    }

    fileToRename?.let { oldPath ->
        if (showRenameDialog) {
            showRenameDialog(
                context = context,
                oldPath = oldPath,
                onRenamed = {
                    Log.d("MainScreen", "Rename completed, refreshing file list")
                    SupabaseStorageHelper.getUploadedFiles { files ->
                        fileList = files
                        showRenameDialog = false
                        fileToRename = null
                    }
                }
                ,
                onDismiss = {
                    showRenameDialog = false
                    fileToRename = null
                }
            )
        }
    }

    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    OogaboogaTheme(darkTheme = isDarkTheme) {
        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                AppDrawerContent(
                    navController = navController,
                    folderPickerLauncher = folderPickerLauncher,
                    backupFolders = backupFolders
                )
            }
        ) {
            MainContent(
                searchText = searchText,
                onSearchTextChange = { searchText = it },
                onMenuClick = { scope.launch { drawerState.open() } },
                onProfileClick = {
                    loginViewModel.signIn(activity) {
                        SupabaseStorageHelper.getUploadedFiles { files -> fileList = files }
                    }
                },
                onUploadClick = { launcher.launch("*/*") },
                onNavigateToFileList = { navController.navigate("file_list") },
                fileList = fileList,
                onDownloadClick = { fileName -> SupabaseStorageHelper.downloadFile(context, fileName) },
                onRenameClick = { fileToRename = it; showRenameDialog = true },
                onDeleteClick = { fileName ->
                    SupabaseStorageHelper.deleteFile(context, fileName)
                    fileList = fileList.filterNot { it.file_name == fileName }
                },
                onShareClick = { fileName ->
                    SupabaseStorageHelper.downloadAndShareFile(context, fileName)
                },
                isDarkTheme = isDarkTheme,
                onToggleTheme = toggleTheme
            )
        }
    }
}
@Composable
fun AppDrawerContent(
    navController: NavController,
    folderPickerLauncher: ManagedActivityResultLauncher<Uri?, Uri?>? = null,
    backupFolders: List<Uri> = emptyList()
) {
    DrawerContent(
        navController = navController,
        onSignOut = {
            FirebaseAuth.getInstance().signOut()
            navController.navigate("login") {
                popUpTo(0) { inclusive = true }
            }
        },
        onRequestBackupFolder = {
            folderPickerLauncher?.launch(null)
        },
        backupFolders = backupFolders
    )
}
@Composable
fun DrawerContent(
    navController: NavController,
    onSignOut: () -> Unit = {},
    onRequestBackupFolder: () -> Unit = {},
    backupFolders: List<Uri> = emptyList(),
    autoBackupEnabled: Boolean = false,
    onToggleAutoBackup: (Boolean) -> Unit = {}
) {
    val context = LocalContext.current
    val currentUser = FirebaseAuth.getInstance().currentUser
    val emailOrUid = currentUser?.email ?: currentUser?.uid ?: "Unknown User"

    var autoBackupEnabled by remember { mutableStateOf(true) }

    Column(
        modifier = Modifier
            .fillMaxHeight()
            .width(320.dp)
            .background(Color(0xFF00695C), shape = MaterialTheme.shapes.large) // 🔧 Fixed green background
            .padding(start = 20.dp, end = 20.dp, top = 40.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(
                "Clouda",
                color = Color.White,
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Automatic Backup", color = Color.White, fontSize = 16.sp)
                Spacer(modifier = Modifier.weight(1f))
                Switch(
                    checked = autoBackupEnabled,
                    onCheckedChange = { autoBackupEnabled = it },
                    colors = SwitchDefaults.colors(checkedThumbColor = Color.White)
                )
            }

            if (autoBackupEnabled) {
                Spacer(modifier = Modifier.height(16.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(12.dp))
                        .padding(12.dp)
                ) {
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Selected Folders",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )

                            TextButton(onClick = onRequestBackupFolder) {
                                Text("+ Add Folder", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        if (backupFolders.isEmpty()) {
                            Text("No folders selected", color = Color.White.copy(alpha = 0.7f), fontSize = 13.sp)
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                backupFolders.forEach { uri ->
                                    val name = try {
                                        DocumentFile.fromTreeUri(context, uri)?.name
                                    } catch (e: Exception) {
                                        null
                                    } ?: uri.toString()

                                    LaunchedEffect(uri) {
                                        backupAllFilesFromFolder(context, uri)
                                    }

                                    Card(
                                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                navController.navigate("folder_detail/${Uri.encode(name)}")
                                            }
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .padding(12.dp)
                                                .fillMaxWidth()
                                        ) {
                                            Text(
                                                text = "• $name",
                                                color = MaterialTheme.colorScheme.secondary,
                                                fontSize = 13.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        Column {
            Text(
                text = "Logged in as",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 14.sp,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            Text(
                text = emailOrUid,
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            Button(
                onClick = onSignOut,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = Color(0xFF00695C)
                ),
                shape = MaterialTheme.shapes.medium
            ) {
                Text("Sign Out", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}


@Composable
fun showRenameDialog(
    context: Context,
    oldPath: String,
    onRenamed: () -> Unit,
    onDismiss: () -> Unit
) {
    var newName by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = { onDismiss() },
        title = { Text("Rename File", color = Color.White) },
        text = {
            OutlinedTextField(
                value = newName,
                onValueChange = { newName = it },
                label = { Text("New File Name") },
                singleLine = true,
                textStyle = TextStyle(color = Color.White),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.White,
                    unfocusedBorderColor = Color.Gray
                )
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    SupabaseStorageHelper.renameFile(
                        context = context,
                        oldPath = oldPath,
                        newFileName = newName
                    ) {
                        onRenamed() // trigger list refresh in UI
                    }
                }
            ) {
                Text("Rename")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        containerColor = Color(0xFF2B2B2B)
    )
}

@Composable
fun getFileIcon(extension: String): Int {
    return when (extension.lowercase()) {
        "pdf" -> R.drawable.ic_pdf
        "jpg", "jpeg", "png", "gif" -> R.drawable.ic_image
        "zip", "rar", "7z" -> R.drawable.ic_zip
        "mp3", "wav" -> R.drawable.ic_mp3
        "mp4", "avi", "mkv" -> R.drawable.ic_mp4
        else -> R.drawable.ic_file
    }
}

@RequiresApi(Build.VERSION_CODES.O)
fun formatUploadedAt(isoString: String): String {
    return try {
        val instant = Instant.parse(isoString)
        val formatter = DateTimeFormatter.ofPattern("MMM d, yyyy • HH:mm")
            .withZone(ZoneId.systemDefault())
        formatter.format(instant)
    } catch (e: Exception) {
        isoString // fallback
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun FileItemCard(
    fileName: String,
    uploadedAt: String,
    onDownload: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    onShare: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    val colors = MaterialTheme.colorScheme

    Card(
        colors = CardDefaults.cardColors(containerColor = colors.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth()
        ) {
            val extension = fileName.substringAfterLast('.', "").lowercase()
            val iconRes = getFileIcon(extension)

            Image(
                painter = painterResource(id = iconRes),
                contentDescription = "File Icon",
                modifier = Modifier.size(32.dp)
            )

            Column(
                modifier = Modifier
                    .padding(start = 12.dp)
                    .weight(1f)
            ) {
                Text(
                    text = fileName,
                    color = colors.onSurface
                )
                Text(
                    text = formatUploadedAt(uploadedAt),
                    color = colors.onSurfaceVariant,
                    fontSize = 12.sp
                )
            }

            IconButton(onClick = { onDownload() }) {
                Icon(
                    imageVector = Icons.Default.Download,
                    contentDescription = "Download",
                    tint = colors.onSurface
                )
            }

            Box {
                IconButton(onClick = { expanded = true }) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "More",
                        tint = colors.onSurface
                    )
                }

                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    modifier = Modifier.background(colors.surfaceVariant)
                ) {
                    DropdownMenuItem(
                        text = { Text("Rename", color = colors.onSurface) },
                        onClick = {
                            expanded = false
                            onRename()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Share", color = colors.onSurface) },
                        onClick = {
                            expanded = false
                            onShare()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                        onClick = {
                            expanded = false
                            onDelete()
                        }
                    )
                }
            }
        }
    }
}


@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun MainContent(
    searchText: String,
    onSearchTextChange: (String) -> Unit,
    onMenuClick: () -> Unit,
    onProfileClick: () -> Unit,
    onUploadClick: () -> Unit,
    onNavigateToFileList: () -> Unit,
    fileList: List<FileMeta>,
    onDownloadClick: (String) -> Unit,
    onRenameClick: (String) -> Unit,
    onDeleteClick: (String) -> Unit,
    onShareClick: (String) -> Unit,
    isDarkTheme: Boolean,
    onToggleTheme: () -> Unit
) {
    val filteredList = remember(fileList, searchText) {
        if (searchText.isBlank()) fileList
        else fileList.filter {
            it.file_name.substringAfterLast("/").contains(searchText.trim(), ignoreCase = true)
        }
    }

    val recentFiles = remember(filteredList) {
        filteredList.sortedByDescending { Instant.parse(it.uploaded_at) }.take(10)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = Icons.Default.Menu,
                contentDescription = "Menu",
                tint = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier
                    .clickable { onMenuClick() }
                    .padding(end = 12.dp)
            )

            BasicTextField(
                value = searchText,
                onValueChange = onSearchTextChange,
                singleLine = true,
                modifier = Modifier
                    .weight(0.8f)
                    .padding(horizontal = 12.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant, shape = MaterialTheme.shapes.small)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                textStyle = TextStyle(color = MaterialTheme.colorScheme.onBackground, fontSize = 16.sp),
                decorationBox = { innerTextField ->
                    if (searchText.isEmpty()) {
                        Text("Search in APP", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    innerTextField()
                }
            )

            IconButton(onClick = { onToggleTheme() }) {
                Icon(
                    imageVector = if (isDarkTheme) Icons.Default.LightMode else Icons.Default.DarkMode,
                    contentDescription = "Toggle Theme",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(40.dp)
                    .background(MaterialTheme.colorScheme.primary, CircleShape)
                    .clickable { onProfileClick() }
            ) {
                Text("T", color = MaterialTheme.colorScheme.onPrimary, fontSize = 18.sp)
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            ActionCard(
                title = "Download Files",
                icon = Icons.Default.Download,
                modifier = Modifier.weight(1f),
                onClick = { onNavigateToFileList() }
            )

            ActionCard(
                title = "Upload Files",
                icon = Icons.Default.Upload,
                modifier = Modifier.weight(1f),
                onClick = { onUploadClick() }
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            "Recently Uploaded",
            color = MaterialTheme.colorScheme.onBackground,
            fontSize = 20.sp,
            modifier = Modifier.padding(vertical = 8.dp)
        )

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(recentFiles) { file ->
                val displayName = file.file_name.substringAfterLast("/")
                val context = LocalContext.current

                FileItemCard(
                    fileName = displayName,
                    uploadedAt = file.uploaded_at,
                    onDownload = { onDownloadClick(file.file_name) },
                    onRename = { onRenameClick(file.file_name) },
                    onDelete = { onDeleteClick(file.file_name) },
                    onShare = { onShareClick(file.file_name) }
                )
            }
        }
    }
}

@Composable
fun ActionCard(
    title: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val colors = MaterialTheme.colorScheme

    Card(
        colors = CardDefaults.cardColors(containerColor = colors.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        modifier = modifier
            .height(120.dp)
            .clickable { onClick() }
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            Icon(
                icon,
                contentDescription = title,
                tint = colors.onSurface,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = title,
                color = colors.onSurface,
                fontSize = 16.sp
            )
        }
    }
}

fun loadBackupUrisForCurrentUser(context: Context): List<Uri> {
    val prefs = context.getSharedPreferences("clouda_prefs", Context.MODE_PRIVATE)
    val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return emptyList()
    val set = prefs.getStringSet("backup_uris_$userId", emptySet()) ?: emptySet()
    return set.mapNotNull { runCatching { Uri.parse(it) }.getOrNull() }
}

fun backupAllFilesFromFolder(context: Context, folderUri: Uri) {
    val folder = DocumentFile.fromTreeUri(context, folderUri)

    if (folder != null && folder.isDirectory) {
        val files = folder.listFiles().filter { it.isFile }
        for (file in files) {
            val fileUri = file.uri
            SupabaseStorageHelper.uploadFile(context, fileUri)
        }
    } else {
        Log.w("Backup", "Invalid folder URI or not a directory: $folderUri")
    }
}

@Composable
fun FolderDetailScreen(folderName: String) {
    val context = LocalContext.current
    val backupFolders = loadBackupUrisForCurrentUser(context)
    val selectedUri = backupFolders.find {
        DocumentFile.fromTreeUri(context, it)?.name == folderName
    }

    val files = remember(selectedUri) {
        selectedUri?.let {
            DocumentFile.fromTreeUri(context, it)?.listFiles()?.filter { file -> file.isFile }
        } ?: emptyList()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 20.dp, vertical = 32.dp)
    ) {
        Text(
            text = "Files in \"$folderName\"",
            color = MaterialTheme.colorScheme.onBackground,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (files.isEmpty()) {
            Text(
                text = "No files found in this folder.",
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                fontSize = 14.sp,
                modifier = Modifier.padding(top = 8.dp)
            )
        } else {
            files.forEach { file ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .padding(horizontal = 12.dp, vertical = 10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.InsertDriveFile,
                            contentDescription = "File",
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = file.name ?: "Unnamed file",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }
}

