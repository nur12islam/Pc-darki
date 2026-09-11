package com.nurislam.pcdarki

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.TextSnippet
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

data class DesktopWindow(
    val id: Int,
    val title: String,
    val minimized: Boolean = false,
    val x: Float = 0f,
    val y: Float = 0f
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { PCDarkiDesktop() }
    }
}

@Composable
fun PCDarkiDesktop() {
    var startOpen by remember { mutableStateOf(false) }
    var nextId by remember { mutableStateOf(1) }
    var activeId by remember { mutableStateOf<Int?>(null) }
    val windows = remember { mutableStateListOf<DesktopWindow>() }

    fun openWindow(title: String) {
        val existing = windows.lastOrNull { it.title == title }
        if (existing != null) {
            val index = windows.indexOfFirst { it.id == existing.id }
            windows[index] = existing.copy(minimized = false)
            activeId = existing.id
            return
        }
        val id = nextId++
        windows.add(DesktopWindow(id = id, title = title, x = 0f, y = 0f))
        activeId = id
    }

    fun closeWindow(id: Int) {
        windows.removeAll { it.id == id }
        activeId = windows.lastOrNull { !it.minimized }?.id
    }

    fun minimizeWindow(id: Int) {
        val index = windows.indexOfFirst { it.id == id }
        if (index >= 0) windows[index] = windows[index].copy(minimized = true)
        activeId = windows.lastOrNull { !it.minimized && it.id != id }?.id
    }

    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.linearGradient(
                            listOf(Color(0xFF0B1020), Color(0xFF21183D), Color(0xFF080C16))
                        )
                    )
            ) {
                Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
                    Text("PC-DARKI", color = Color.White.copy(alpha = 0.9f), fontSize = 20.sp)
                    Spacer(Modifier.height(18.dp))
                    DesktopIcon("Files", Icons.Default.Folder) { openWindow("Files") }
                    Spacer(Modifier.height(14.dp))
                    DesktopIcon("Browser", Icons.Default.Language) {
                        val context = LocalContext.current
                        context.startActivity(Intent(Intent.ACTION_VIEW).apply { data = android.net.Uri.parse("https://www.google.com") })
                    }
                    Spacer(Modifier.height(14.dp))
                    DesktopIcon("Terminal", Icons.Default.Terminal) { openWindow("Terminal") }
                    Spacer(Modifier.height(14.dp))
                    DesktopIcon("Settings", Icons.Default.Settings) { openWindow("Settings") }
                }

                windows.filter { !it.minimized }.forEach { window ->
                    val isActive = window.id == activeId
                    AppWindow(
                        window = window,
                        active = isActive,
                        onFocus = { activeId = window.id },
                        onClose = { closeWindow(window.id) },
                        onMinimize = { minimizeWindow(window.id) },
                        onMove = { dx, dy ->
                            val index = windows.indexOfFirst { it.id == window.id }
                            if (index >= 0) {
                                val current = windows[index]
                                windows[index] = current.copy(x = current.x + dx, y = current.y + dy)
                            }
                        }
                    )
                }

                if (startOpen) {
                    StartMenu(onOpen = { name ->
                        startOpen = false
                        openWindow(name)
                    })
                }

                Taskbar(
                    windows = windows,
                    activeId = activeId,
                    onStart = { startOpen = !startOpen },
                    onWindow = { id ->
                        val index = windows.indexOfFirst { it.id == id }
                        if (index >= 0) {
                            val current = windows[index]
                            windows[index] = current.copy(minimized = false)
                            activeId = id
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun DesktopIcon(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Column(
        modifier = Modifier.width(86.dp).clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(shape = RoundedCornerShape(16.dp), color = Color.White.copy(alpha = 0.10f)) {
            Icon(icon, contentDescription = label, modifier = Modifier.padding(14.dp).size(34.dp), tint = Color.White)
        }
        Spacer(Modifier.height(5.dp))
        Text(label, color = Color.White, fontSize = 12.sp)
    }
}

@Composable
private fun Taskbar(
    windows: List<DesktopWindow>,
    activeId: Int?,
    onStart: () -> Unit,
    onWindow: (Int) -> Unit
) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
        Surface(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp),
            shape = RoundedCornerShape(22.dp),
            color = Color(0xE8111521)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().height(62.dp).padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        modifier = Modifier.size(44.dp).clickable(onClick = onStart),
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFF7657F6)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("D", color = Color.White, fontSize = 20.sp)
                        }
                    }
                    Spacer(Modifier.width(10.dp))
                    windows.forEach { window ->
                        Surface(
                            modifier = Modifier.padding(horizontal = 3.dp).clickable { onWindow(window.id) },
                            shape = RoundedCornerShape(10.dp),
                            color = if (window.id == activeId && !window.minimized) Color(0xFF38304F) else Color.Transparent
                        ) {
                            Text(
                                text = window.title,
                                color = Color.White.copy(alpha = if (window.minimized) 0.55f else 0.9f),
                                fontSize = 12.sp,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                            )
                        }
                    }
                }
                Text(
                    SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date()),
                    color = Color.White,
                    fontSize = 14.sp
                )
            }
        }
    }
}

@Composable
private fun StartMenu(onOpen: (String) -> Unit) {
    Surface(
        modifier = Modifier.padding(start = 24.dp, bottom = 92.dp).width(320.dp),
        shape = RoundedCornerShape(22.dp),
        color = Color(0xF21A1E2A)
    ) {
        Column(modifier = Modifier.padding(22.dp)) {
            Text("PC-DARKI", color = Color.White, fontSize = 24.sp)
            Text("Desktop", color = Color.White.copy(alpha = 0.55f), fontSize = 12.sp)
            Spacer(Modifier.height(18.dp))
            StartItem("Files", Icons.Default.Folder, onOpen)
            StartItem("Browser", Icons.Default.Language, onOpen)
            StartItem("Terminal", Icons.Default.Terminal, onOpen)
            StartItem("Text Editor", Icons.Default.TextSnippet, onOpen)
            StartItem("Settings", Icons.Default.Settings, onOpen)
        }
    }
}

@Composable
private fun StartItem(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onOpen: (String) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onOpen(label) }.padding(vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = label, tint = Color.White.copy(alpha = 0.9f), modifier = Modifier.size(24.dp))
        Spacer(Modifier.width(14.dp))
        Text(label, color = Color.White, fontSize = 15.sp)
    }
}

@Composable
private fun AppWindow(
    window: DesktopWindow,
    active: Boolean,
    onFocus: () -> Unit,
    onClose: () -> Unit,
    onMinimize: () -> Unit,
    onMove: (Float, Float) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .offset { IntOffset(window.x.roundToInt(), window.y.roundToInt()) },
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.72f)
                .fillMaxSize(0.68f)
                .clickable(onClick = onFocus),
            shape = RoundedCornerShape(18.dp),
            color = if (active) Color(0xF21A1D26) else Color(0xE8161922),
            tonalElevation = if (active) 12.dp else 4.dp
        ) {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .pointerInput(window.id) {
                            detectDragGestures { change, dragAmount ->
                                change.consume()
                                onMove(dragAmount.x, dragAmount.y)
                            }
                        }
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(window.title, color = Color.White, fontSize = 16.sp)
                    Row {
                        Text("—", color = Color.White.copy(alpha = 0.8f), modifier = Modifier.clickable(onClick = onMinimize).padding(horizontal = 10.dp))
                        Text("✕", color = Color.White, modifier = Modifier.clickable(onClick = onClose).padding(horizontal = 6.dp))
                    }
                }
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("${window.title} — PC-DARKI v0.1", color = Color.White.copy(alpha = 0.65f), fontSize = 18.sp)
                }
            }
        }
    }
}
