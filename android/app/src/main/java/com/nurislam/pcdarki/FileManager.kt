package com.nurislam.pcdarki

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.result.ActivityResult
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.documentfile.provider.DocumentFile
import java.text.DateFormat
import java.util.Date

@Composable
fun PCDarkiFileManager(
    initialUri: Uri?,
    treeLauncher: ManagedActivityResultLauncher<Intent, ActivityResult>
) {
    val context = LocalContext.current
    var currentUri by remember(initialUri) { mutableStateOf(initialUri) }
    var current by remember(initialUri) { mutableStateOf(initialUri?.let { DocumentFile.fromTreeUri(context, it) }) }
    var refresh by remember { mutableIntStateOf(0) }

    fun openTree() {
        treeLauncher.launch(Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
        })
    }

    fun goUp() {
        val uri = currentUri ?: return
        val parent = DocumentFile.fromTreeUri(context, uri)?.parentFile
        if (parent != null) {
            current = parent
            currentUri = parent.uri
        }
    }

    val entries = remember(currentUri, refresh) {
        current?.listFiles()?.sortedWith(compareBy<DocumentFile> { !it.isDirectory }.thenBy { it.name?.lowercase() ?: "" }) ?: emptyList()
    }

    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { goUp() }, enabled = current != null) { Icon(Icons.Default.ArrowBack, "Back") }
            IconButton(onClick = { refresh++ }, enabled = current != null) { Icon(Icons.Default.Refresh, "Refresh") }
            Spacer(Modifier.width(6.dp))
            Column(Modifier.weight(1f)) {
                Text(current?.name ?: "Files", fontSize = 18.sp)
                Text(if (current == null) "Choose a storage location" else "${entries.size} items", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            OutlinedButton(onClick = { openTree() }) { Icon(Icons.Default.FolderOpen, null); Spacer(Modifier.width(6.dp)); Text(if (current == null) "Choose" else "Change") }
        }
        HorizontalDivider()

        if (current == null) {
            Box(Modifier.fillMaxSize(), Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Folder, null, Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(14.dp))
                    Text("No folder selected", fontSize = 20.sp)
                    Spacer(Modifier.height(6.dp))
                    Text("Choose a folder to start browsing", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(18.dp))
                    Button(onClick = { openTree() }) { Text("Choose storage") }
                }
            }
        } else if (entries.isEmpty()) {
            Box(Modifier.fillMaxSize(), Alignment.Center) { Text("This folder is empty", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        } else {
            LazyColumn(Modifier.fillMaxSize().padding(horizontal = 10.dp)) {
                items(entries, key = { it.uri.toString() }) { file ->
                    FileRow(file) {
                        if (file.isDirectory) {
                            current = file
                            currentUri = file.uri
                        } else {
                            runCatching {
                                context.startActivity(Intent(Intent.ACTION_VIEW).apply {
                                    setDataAndType(file.uri, file.type ?: "*/*")
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                })
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FileRow(file: DocumentFile, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 10.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(if (file.isDirectory) Icons.Default.Folder else Icons.Default.InsertDriveFile, null, Modifier.size(34.dp), tint = if (file.isDirectory) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(file.name ?: "Unnamed", fontSize = 14.sp)
            Text(if (file.isDirectory) "Folder" else formatFileInfo(file), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private fun formatFileInfo(file: DocumentFile): String {
    val size = file.length()
    val sizeText = when {
        size < 1024 -> "$size B"
        size < 1024 * 1024 -> "%.1f KB".format(size / 1024.0)
        size < 1024 * 1024 * 1024 -> "%.1f MB".format(size / (1024.0 * 1024.0))
        else -> "%.1f GB".format(size / (1024.0 * 1024.0 * 1024.0))
    }
    val modified = file.lastModified().takeIf { it > 0 }?.let { DateFormat.getDateInstance(DateFormat.SHORT).format(Date(it)) } ?: ""
    return listOf(sizeText, modified).filter { it.isNotEmpty() }.joinToString(" • ")
}
