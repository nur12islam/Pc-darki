package com.nurislam.pcdarki

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun PCDarkiTextEditor() {
    val context = LocalContext.current
    var fileUri by remember { mutableStateOf<Uri?>(null) }
    var fileName by remember { mutableStateOf("Untitled.txt") }
    var text by remember { mutableStateOf(TextFieldValue("")) }
    var dirty by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("Ready") }

    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = MaterialTheme.colorScheme.onSurface,
        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
        disabledTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = .5f),
        focusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
        unfocusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
        focusedLabelColor = MaterialTheme.colorScheme.primary,
        unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
        cursorColor = MaterialTheme.colorScheme.primary
    )

    val openLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        fileUri = uri
        fileName = uri.lastPathSegment?.substringAfterLast('/')?.substringAfterLast(':') ?: "Document.txt"
        val loaded = runCatching { context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() } }.getOrNull()
        if (loaded != null) {
            text = TextFieldValue(loaded)
            dirty = false
            status = "Opened $fileName"
        } else status = "Could not open file"
    }

    val saveLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/plain")) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        fileUri = uri
        fileName = uri.lastPathSegment?.substringAfterLast('/')?.substringAfterLast(':') ?: fileName
        val ok = runCatching {
            context.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use { it.write(text.text) }
        }.isSuccess
        dirty = !ok
        status = if (ok) "Saved $fileName" else "Could not save file"
    }

    fun saveCurrent() {
        val uri = fileUri
        if (uri == null) saveLauncher.launch(fileName) else {
            val ok = runCatching {
                context.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use { it.write(text.text) }
            }.isSuccess
            dirty = !ok
            status = if (ok) "Saved $fileName" else "Could not save file"
        }
    }

    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilledTonalButton(onClick = {
                fileUri = null; fileName = "Untitled.txt"; text = TextFieldValue(""); dirty = false; status = "New document"
            }) { Icon(Icons.Default.NoteAdd, null); Spacer(Modifier.width(6.dp)); Text("New") }
            FilledTonalButton(onClick = { openLauncher.launch(arrayOf("text/*", "text/plain", "text/markdown", "application/json")) }) { Icon(Icons.Default.FolderOpen, null); Spacer(Modifier.width(6.dp)); Text("Open") }
            FilledTonalButton(onClick = { saveCurrent() }) { Icon(Icons.Default.Save, null); Spacer(Modifier.width(6.dp)); Text("Save") }
        }
        Spacer(Modifier.height(10.dp))
        Text(if (dirty) "$fileName •" else fileName, color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = text,
            onValueChange = { text = it; dirty = true },
            modifier = Modifier.fillMaxWidth().weight(1f),
            placeholder = { Text("Start typing…") },
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
            colors = fieldColors
        )
        Spacer(Modifier.height(6.dp))
        Text("${text.text.length} characters  •  $status", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
