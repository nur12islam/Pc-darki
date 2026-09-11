package com.nurislam.pcdarki

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

data class DesktopWindow(val id: Int, val title: String, val minimized: Boolean = false, val x: Float = 0f, val y: Float = 0f)

class MainActivity : ComponentActivity() {
    private var selectedFile by mutableStateOf<String?>(null)
    private val openDocument = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { selectedFile = it.toString() }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PCDarkiDesktop(
                selectedFile = selectedFile,
                onOpenFile = { openDocument.launch(arrayOf("*/*")) }
            )
        }
    }
}

@Composable
fun PCDarkiDesktop(selectedFile: String?, onOpenFile: () -> Unit) {
    var startOpen by remember { mutableStateOf(false) }
    var nextId by remember { mutableStateOf(1) }
    var activeId by remember { mutableStateOf<Int?>(null) }
    val windows = remember { mutableStateListOf<DesktopWindow>() }

    fun openWindow(title: String) {
        val existing = windows.lastOrNull { it.title == title }
        if (existing != null) {
            val i = windows.indexOfFirst { it.id == existing.id }
            windows[i] = existing.copy(minimized = false)
            activeId = existing.id
            return
        }
        val id = nextId++
        windows.add(DesktopWindow(id, title))
        activeId = id
    }
    fun closeWindow(id: Int) {
        windows.removeAll { it.id == id }
        activeId = windows.lastOrNull { !it.minimized }?.id
    }
    fun minimizeWindow(id: Int) {
        val i = windows.indexOfFirst { it.id == id }
        if (i >= 0) windows[i] = windows[i].copy(minimized = true)
        activeId = windows.lastOrNull { !it.minimized && it.id != id }?.id
    }

    MaterialTheme {
        Surface(Modifier.fillMaxSize()) {
            Box(Modifier.fillMaxSize().background(Brush.linearGradient(listOf(Color(0xFF0B1020), Color(0xFF21183D), Color(0xFF080C16))))) {
                Column(Modifier.fillMaxSize().padding(24.dp)) {
                    Text("PC-DARKI", color = Color.White.copy(.9f), fontSize = 20.sp)
                    Spacer(Modifier.height(18.dp))
                    DesktopIcon("Files", Icons.Default.Folder) { openWindow("Files") }
                    Spacer(Modifier.height(14.dp))
                    DesktopIcon("Browser", Icons.Default.Language) {
                        LocalContext.current.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com")))
                    }
                    Spacer(Modifier.height(14.dp))
                    DesktopIcon("Terminal", Icons.Default.Terminal) { openWindow("Terminal") }
                    Spacer(Modifier.height(14.dp))
                    DesktopIcon("Settings", Icons.Default.Settings) { openWindow("Settings") }
                }
                windows.filter { !it.minimized }.forEach { window ->
                    AppWindow(
                        window = window,
                        active = window.id == activeId,
                        selectedFile = selectedFile,
                        onFocus = { activeId = window.id },
                        onClose = { closeWindow(window.id) },
                        onMinimize = { minimizeWindow(window.id) },
                        onOpenFile = onOpenFile,
                        onMove = { dx, dy ->
                            val i = windows.indexOfFirst { it.id == window.id }
                            if (i >= 0) windows[i] = windows[i].copy(x = windows[i].x + dx, y = windows[i].y + dy)
                        }
                    )
                }
                if (startOpen) StartMenu { name -> startOpen = false; openWindow(name) }
                Taskbar(windows, activeId, { startOpen = !startOpen }) { id ->
                    val i = windows.indexOfFirst { it.id == id }
                    if (i >= 0) { windows[i] = windows[i].copy(minimized = false); activeId = id }
                }
            }
        }
    }
}

@Composable
private fun DesktopIcon(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Column(Modifier.width(86.dp).clickable(onClick = onClick), Alignment.CenterHorizontally) {
        Surface(RoundedCornerShape(16.dp), color = Color.White.copy(.10f)) { Icon(icon, label, Modifier.padding(14.dp).size(34.dp), tint = Color.White) }
        Spacer(Modifier.height(5.dp)); Text(label, Color.White, 12.sp)
    }
}

@Composable
private fun Taskbar(windows: List<DesktopWindow>, activeId: Int?, onStart: () -> Unit, onWindow: (Int) -> Unit) {
    Box(Modifier.fillMaxSize(), Alignment.BottomCenter) {
        Surface(Modifier.fillMaxWidth().padding(20.dp, 14.dp), RoundedCornerShape(22.dp), Color(0xE8111521)) {
            Row(Modifier.fillMaxWidth().height(62.dp).padding(horizontal = 14.dp), Alignment.CenterVertically, Arrangement.SpaceBetween) {
                Row(Alignment.CenterVertically) {
                    Surface(Modifier.size(44.dp).clickable(onClick = onStart), RoundedCornerShape(14.dp), Color(0xFF7657F6)) { Box(Alignment.Center) { Text("D", Color.White, 20.sp) } }
                    Spacer(Modifier.width(10.dp))
                    windows.forEach { w -> Surface(Modifier.padding(3.dp).clickable { onWindow(w.id) }, RoundedCornerShape(10.dp), if (w.id == activeId && !w.minimized) Color(0xFF38304F) else Color.Transparent) { Text(w.title, Color.White.copy(if (w.minimized) .55f else .9f), 12.sp, Modifier.padding(10.dp, 8.dp)) } }
                }
                Text(SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date()), Color.White, 14.sp)
            }
        }
    }
}

@Composable
private fun StartMenu(onOpen: (String) -> Unit) {
    Surface(Modifier.padding(start = 24.dp, bottom = 92.dp).width(320.dp), RoundedCornerShape(22.dp), Color(0xF21A1E2A)) {
        Column(Modifier.padding(22.dp)) {
            Text("PC-DARKI", Color.White, 24.sp); Text("Desktop", Color.White.copy(.55f), 12.sp); Spacer(Modifier.height(18.dp))
            StartItem("Files", Icons.Default.Folder, onOpen); StartItem("Browser", Icons.Default.Language, onOpen); StartItem("Terminal", Icons.Default.Terminal, onOpen); StartItem("Text Editor", Icons.Default.TextSnippet, onOpen); StartItem("Settings", Icons.Default.Settings, onOpen)
        }
    }
}

@Composable
private fun StartItem(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onOpen: (String) -> Unit) {
    Row(Modifier.fillMaxWidth().clickable { onOpen(label) }.padding(vertical = 11.dp), Alignment.CenterVertically) { Icon(icon, label, Modifier.size(24.dp), tint = Color.White.copy(.9f)); Spacer(Modifier.width(14.dp)); Text(label, Color.White, 15.sp) }
}

@Composable
private fun AppWindow(window: DesktopWindow, active: Boolean, selectedFile: String?, onFocus: () -> Unit, onClose: () -> Unit, onMinimize: () -> Unit, onOpenFile: () -> Unit, onMove: (Float, Float) -> Unit) {
    Box(Modifier.fillMaxSize().offset { IntOffset(window.x.roundToInt(), window.y.roundToInt()) }, Alignment.Center) {
        Surface(Modifier.fillMaxWidth(.72f).fillMaxSize(.68f).clickable(onClick = onFocus), RoundedCornerShape(18.dp), if (active) Color(0xF21A1D26) else Color(0xE8161922), tonalElevation = if (active) 12.dp else 4.dp) {
            Column {
                Row(Modifier.fillMaxWidth().height(52.dp).pointerInput(window.id) { detectDragGestures { change, amount -> change.consume(); onMove(amount.x, amount.y) } }.padding(horizontal = 16.dp), Alignment.CenterVertically, Arrangement.SpaceBetween) {
                    Text(window.title, Color.White, 16.sp)
                    Row { Text("—", Color.White.copy(.8f), Modifier.clickable(onClick = onMinimize).padding(horizontal = 10.dp)); Text("✕", Color.White, Modifier.clickable(onClick = onClose).padding(horizontal = 6.dp)) }
                }
                WindowContent(window.title, selectedFile, onOpenFile)
            }
        }
    }
}

@Composable
private fun WindowContent(title: String, selectedFile: String?, onOpenFile: () -> Unit) {
    Box(Modifier.fillMaxSize().padding(28.dp), Alignment.Center) {
        when (title) {
            "Files" -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.Folder, null, Modifier.size(52.dp), tint = Color.White)
                Spacer(Modifier.height(12.dp)); Text("PC-DARKI File Manager", Color.White, 20.sp); Spacer(Modifier.height(18.dp))
                Button(onClick = onOpenFile) { Text("Open file") }
                Spacer(Modifier.height(14.dp)); Text(selectedFile?.let { "Selected: $it" } ?: "Use Android's document picker to access your storage.", Color.White.copy(.65f), 13.sp)
            }
            "Text Editor" -> Text("Text Editor — next module", Color.White.copy(.7f), 18.sp)
            "Terminal" -> Text("Terminal — native command backend coming next", Color.White.copy(.7f), 18.sp)
            "Settings" -> Text("Settings — configuration coming next", Color.White.copy(.7f), 18.sp)
            else -> Text("$title — PC-DARKI v0.1", Color.White.copy(.65f), 18.sp)
        }
    }
}
