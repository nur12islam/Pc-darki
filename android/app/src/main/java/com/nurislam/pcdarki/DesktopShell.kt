package com.nurislam.pcdarki

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

internal data class DesktopWindowV2(
    val id: Int,
    val title: String,
    val icon: ImageVector,
    val minimized: Boolean = false,
    val x: Float = 0f,
    val y: Float = 0f,
    val widthFraction: Float = .72f,
    val heightFraction: Float = .68f
)

@Composable
fun PCDarkiDesktopV2(security: SecurityStore) {
    var startOpen by remember { mutableStateOf(false) }
    var nextId by remember { mutableIntStateOf(1) }
    var activeId by remember { mutableStateOf<Int?>(null) }
    val windows = remember { mutableStateListOf<DesktopWindowV2>() }

    fun openWindow(title: String) {
        val existing = windows.lastOrNull { it.title == title }
        if (existing != null) {
            val i = windows.indexOfFirst { it.id == existing.id }
            windows[i] = existing.copy(minimized = false)
            activeId = existing.id
        } else {
            val id = nextId++
            windows.add(DesktopWindowV2(id, title, appIcon(title)))
            activeId = id
        }
    }

    MaterialTheme(colorScheme = darkColorScheme()) {
        Surface(Modifier.fillMaxSize()) {
            Box(Modifier.fillMaxSize().background(Brush.linearGradient(listOf(Color(0xFF0B1020), Color(0xFF21183D), Color(0xFF080C16))))) {
                Column(Modifier.fillMaxSize().padding(24.dp)) {
                    Text("PC-DARKI", color = Color.White.copy(alpha = .9f), fontSize = 20.sp)
                    Text("Signed in as ${security.username}", color = Color.White.copy(alpha = .55f), fontSize = 12.sp)
                    Spacer(Modifier.height(18.dp))
                    DesktopIconV2("Files", Icons.Default.Folder) { openWindow("Files") }
                    Spacer(Modifier.height(14.dp)); DesktopIconV2("Browser", Icons.Default.Language) { openWindow("Browser") }
                    Spacer(Modifier.height(14.dp)); DesktopIconV2("Terminal", Icons.Default.Terminal) { openWindow("Terminal") }
                    Spacer(Modifier.height(14.dp)); DesktopIconV2("Text Editor", Icons.Default.TextSnippet) { openWindow("Text Editor") }
                    Spacer(Modifier.height(14.dp)); DesktopIconV2("Image Viewer", Icons.Default.Image) { openWindow("Image Viewer") }
                    Spacer(Modifier.height(14.dp)); DesktopIconV2("App Manager", Icons.Default.Apps) { openWindow("App Manager") }
                    Spacer(Modifier.height(14.dp)); DesktopIconV2("Settings", Icons.Default.Settings) { openWindow("Settings") }
                }
                windows.filter { !it.minimized }.forEach { window ->
                    AppWindowV2(window, window.id == activeId,
                        { activeId = window.id },
                        { windows.removeAll { it.id == window.id }; activeId = windows.lastOrNull { !it.minimized }?.id },
                        { val i = windows.indexOfFirst { it.id == window.id }; if (i >= 0) windows[i] = windows[i].copy(minimized = true); activeId = windows.lastOrNull { !it.minimized && it.id != window.id }?.id },
                        { dx, dy -> val i = windows.indexOfFirst { it.id == window.id }; if (i >= 0) windows[i] = windows[i].copy(x = windows[i].x + dx, y = windows[i].y + dy) },
                        { dw, dh, maxW, maxH ->
                            val i = windows.indexOfFirst { it.id == window.id }
                            if (i >= 0) { val w = windows[i]; windows[i] = w.copy(widthFraction = (w.widthFraction + dw / maxW).coerceIn(.42f, .94f), heightFraction = (w.heightFraction + dh / maxH).coerceIn(.38f, .86f)) }
                        }, security)
                }
                if (startOpen) StartMenuV2 { name -> startOpen = false; openWindow(name) }
                TaskbarV2(windows, activeId, { startOpen = !startOpen }) { id -> val i = windows.indexOfFirst { it.id == id }; if (i >= 0) { windows[i] = windows[i].copy(minimized = false); activeId = id } }
            }
        }
    }
}

private fun appIcon(title: String): ImageVector = when (title) {
    "Files" -> Icons.Default.Folder
    "Browser" -> Icons.Default.Language
    "Terminal" -> Icons.Default.Terminal
    "Text Editor" -> Icons.Default.TextSnippet
    "Image Viewer" -> Icons.Default.Image
    "App Manager" -> Icons.Default.Apps
    "Settings" -> Icons.Default.Settings
    else -> Icons.Default.Apps
}

@Composable private fun DesktopIconV2(label: String, icon: ImageVector, onClick: () -> Unit) {
    Column(Modifier.width(86.dp).clickable(onClick = onClick), horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(shape = RoundedCornerShape(16.dp), color = Color.White.copy(alpha = .10f)) { Icon(icon, label, Modifier.padding(14.dp).size(34.dp), tint = Color.White) }
        Spacer(Modifier.height(5.dp)); Text(label, color = Color.White, fontSize = 12.sp)
    }
}

@Composable private fun TaskbarV2(windows: List<DesktopWindowV2>, activeId: Int?, onStart: () -> Unit, onWindow: (Int) -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
        Surface(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp), RoundedCornerShape(22.dp), color = Color(0xE8111521)) {
            Row(Modifier.fillMaxWidth().height(62.dp).padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                Surface(Modifier.size(44.dp).clickable(onClick = onStart), RoundedCornerShape(14.dp), color = Color(0xFF7657F6)) { Box(contentAlignment = Alignment.Center) { Text("D", color = Color.White, fontSize = 20.sp) } }
                Spacer(Modifier.width(10.dp))
                Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                    windows.forEach { w ->
                        Surface(Modifier.padding(3.dp).clickable { onWindow(w.id) }, RoundedCornerShape(10.dp), color = if (w.id == activeId && !w.minimized) Color(0xFF38304F) else Color.Transparent) {
                            Icon(w.icon, w.title, Modifier.padding(horizontal = 10.dp, vertical = 8.dp).size(21.dp), tint = Color.White.copy(alpha = if (w.minimized) .55f else .9f))
                        }
                    }
                }
                Spacer(Modifier.width(8.dp)); PCDarkiSystemTray()
            }
        }
    }
}

@Composable private fun StartMenuV2(onOpen: (String) -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomStart) {
        Surface(Modifier.padding(start = 24.dp, bottom = 92.dp).width(320.dp), RoundedCornerShape(22.dp), color = Color(0xF21A1E2A)) {
            Column(Modifier.padding(22.dp)) {
                Text("PC-DARKI", color = Color.White, fontSize = 24.sp)
                Text("Desktop", color = Color.White.copy(alpha = .55f), fontSize = 12.sp)
                Spacer(Modifier.height(18.dp))
                listOf("Files", "Browser", "Terminal", "Text Editor", "Image Viewer", "App Manager", "Settings").forEach { item ->
                    Row(Modifier.fillMaxWidth().clickable { onOpen(item) }.padding(vertical = 11.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(appIcon(item), item, Modifier.size(24.dp), tint = Color.White.copy(alpha = .9f)); Spacer(Modifier.width(14.dp)); Text(item, color = Color.White, fontSize = 15.sp)
                    }
                }
            }
        }
    }
}
