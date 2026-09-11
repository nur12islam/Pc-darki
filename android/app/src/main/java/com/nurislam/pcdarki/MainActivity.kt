package com.nurislam.pcdarki

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { PCDarkiDesktop() }
    }
}

@androidx.compose.runtime.Composable
fun PCDarkiDesktop() {
    var startOpen by remember { mutableStateOf(false) }
    var activeWindow by remember { mutableStateOf<String?>(null) }

    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.linearGradient(
                            listOf(Color(0xFF101522), Color(0xFF251A43), Color(0xFF0A1020))
                        )
                    )
            ) {
                Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
                    Text(
                        text = "PC-DARKI",
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 20.sp
                    )
                    Spacer(Modifier.height(18.dp))
                    DesktopIcon("Files", Icons.Default.Folder) { activeWindow = "Files" }
                    Spacer(Modifier.height(14.dp))
                    DesktopIcon("Browser", Icons.Default.Language) { activeWindow = "Browser" }
                    Spacer(Modifier.height(14.dp))
                    DesktopIcon("Terminal", Icons.Default.Terminal) { activeWindow = "Terminal" }
                    Spacer(Modifier.height(14.dp))
                    DesktopIcon("Settings", Icons.Default.Settings) { activeWindow = "Settings" }
                }

                activeWindow?.let { title ->
                    AppWindow(title = title, onClose = { activeWindow = null })
                }

                if (startOpen) {
                    StartMenu(onOpen = { name ->
                        startOpen = false
                        activeWindow = name
                    })
                }

                Taskbar(
                    onStart = { startOpen = !startOpen },
                    activeWindow = activeWindow,
                    onHome = { activeWindow = null }
                )
            }
        }
    }
}

@androidx.compose.runtime.Composable
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

@androidx.compose.runtime.Composable
private fun Taskbar(onStart: () -> Unit, activeWindow: String?, onHome: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
        Surface(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp),
            shape = RoundedCornerShape(22.dp),
            color = Color(0xDD111521)
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
                        Box(contentAlignment = Alignment.Center) { Text("D", color = Color.White, fontSize = 20.sp) }
                    }
                    Spacer(Modifier.width(12.dp))
                    if (activeWindow != null) {
                        Text(activeWindow, color = Color.White.copy(alpha = 0.85f), fontSize = 14.sp)
                    } else {
                        Text("PC-DARKI", color = Color.White.copy(alpha = 0.7f), fontSize = 14.sp)
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date()),
                        color = Color.White,
                        fontSize = 14.sp
                    )
                    Spacer(Modifier.width(14.dp))
                    Text("▣", color = Color.White.copy(alpha = 0.75f), modifier = Modifier.clickable(onClick = onHome))
                }
            }
        }
    }
}

@androidx.compose.runtime.Composable
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

@androidx.compose.runtime.Composable
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

@androidx.compose.runtime.Composable
private fun AppWindow(title: String, onClose: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Surface(
            modifier = Modifier.fillMaxWidth(0.72f).fillMaxSize(0.68f),
            shape = RoundedCornerShape(18.dp),
            color = Color(0xF21A1D26),
            tonalElevation = 10.dp
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth().height(52.dp).padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(title, color = Color.White, fontSize = 16.sp)
                    Text("✕", color = Color.White, modifier = Modifier.clickable(onClick = onClose))
                }
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("$title — PC-DARKI v0.1", color = Color.White.copy(alpha = 0.65f), fontSize = 18.sp)
                }
            }
        }
    }
}
