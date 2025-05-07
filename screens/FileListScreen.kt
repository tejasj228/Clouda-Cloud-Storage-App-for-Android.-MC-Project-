package com.mcp.oogabooga.screens

import android.net.Uri
import android.os.Build
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.annotation.RequiresApi
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.mcp.oogabooga.R
import com.mcp.oogabooga.supabase.SupabaseStorageHelper
import com.mcp.oogabooga.supabase.SupabaseStorageHelper.FileMeta
import com.mcp.oogabooga.ui.theme.OogaboogaTheme
import kotlinx.coroutines.launch
import java.time.Instant

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun FileListScreen(
    navController: NavController,
    backupFolders: SnapshotStateList<Uri>,
    folderPickerLauncher: ManagedActivityResultLauncher<Uri?, Uri?>,
    isDarkTheme: Boolean,
    toggleTheme: () -> Unit
) {
    val context = LocalContext.current
    var fileList by remember { mutableStateOf<List<FileMeta>>(emptyList()) }
    var searchText by remember { mutableStateOf("") }
    var fileToRename by remember { mutableStateOf<String?>(null) }
    var showRenameDialog by remember { mutableStateOf(false) }

    var sortAscending by remember { mutableStateOf(true) }
    var sortBy by remember { mutableStateOf("name") }

    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        SupabaseStorageHelper.getUploadedFiles { files -> fileList = files }
    }

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
            FileListContent(
                navController = navController,
                fileList = fileList,
                searchText = searchText,
                onSearchTextChange = { searchText = it },
                sortAscending = sortAscending,
                onToggleSortDirection = { sortAscending = !sortAscending },
                sortBy = sortBy,
                onToggleSortBy = {
                    sortBy = if (sortBy == "name") "time" else "name"
                },
                onDownload = {
                    SupabaseStorageHelper.downloadFile(context, it)
                },
                onRename = {
                    fileToRename = it
                    showRenameDialog = true
                },
                onDelete = {
                    SupabaseStorageHelper.deleteFile(context, it)
                    fileList = fileList.filterNot { file -> file.file_name == it }
                },
                onMenuClick = { scope.launch { drawerState.open() } },
                onShare = { fileName ->
                    SupabaseStorageHelper.downloadAndShareFile(context, fileName)
                },
                isDarkTheme = isDarkTheme,
                onToggleTheme = toggleTheme
            )
        }
    }

    fileToRename?.let { oldPath ->
        if (showRenameDialog) {
            showRenameDialog(
                context = context,
                oldPath = oldPath,
                onRenamed = {
                    SupabaseStorageHelper.getUploadedFiles { files -> fileList = files }
                    showRenameDialog = false
                    fileToRename = null
                },
                onDismiss = {
                    showRenameDialog = false
                    fileToRename = null
                }
            )
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun FileListContent(
    navController: NavController,
    fileList: List<FileMeta>,
    searchText: String,
    onSearchTextChange: (String) -> Unit,
    sortAscending: Boolean,
    onToggleSortDirection: () -> Unit,
    sortBy: String,
    onToggleSortBy: () -> Unit,
    onDownload: (String) -> Unit,
    onRename: (String) -> Unit,
    onDelete: (String) -> Unit,
    onShare: (String) -> Unit,
    onMenuClick: () -> Unit,
    isDarkTheme: Boolean,
    onToggleTheme: () -> Unit
) {
    val filteredList = remember(fileList, searchText, sortAscending, sortBy) {
        val filtered = if (searchText.isBlank()) fileList else fileList.filter {
            it.file_name.substringAfterLast("/").contains(searchText.trim(), ignoreCase = true)
        }
        when (sortBy) {
            "name" -> if (sortAscending) filtered.sortedBy { it.file_name.lowercase() } else filtered.sortedByDescending { it.file_name.lowercase() }
            "time" -> if (sortAscending) filtered.sortedBy { Instant.parse(it.uploaded_at) } else filtered.sortedByDescending { Instant.parse(it.uploaded_at) }
            else -> filtered
        }
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
            ) {
                Text("T", color = MaterialTheme.colorScheme.onPrimary, fontSize = 18.sp)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable { onToggleSortDirection() }
            ) {
                Icon(
                    imageVector = if (sortAscending) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                    contentDescription = "Sort",
                    tint = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = if (sortAscending) "Ascending" else "Descending", color = MaterialTheme.colorScheme.onBackground)
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable { onToggleSortBy() }
            ) {
                Icon(Icons.Default.Sort, contentDescription = "Sort by", tint = MaterialTheme.colorScheme.onBackground)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Sort by: ${if (sortBy == "name") "Name" else "Time"}", color = MaterialTheme.colorScheme.onBackground)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(filteredList) { file ->
                val displayName = file.file_name.substringAfterLast("/")
                FileItemCard(
                    fileName = displayName,
                    uploadedAt = file.uploaded_at,
                    onDownload = { onDownload(file.file_name) },
                    onRename = { onRename(file.file_name) },
                    onDelete = { onDelete(file.file_name) },
                    onShare = { onShare(file.file_name) }
                )
            }
        }
    }
}
