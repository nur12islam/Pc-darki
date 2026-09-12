package com.nurislam.pcdarki

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

data class DesktopWindow(val id: Int, val title: String, val minimized: Boolean = false, val x: Float = 0f, val y: Float = 0f)

class MainActivity : FragmentActivity() {
    private val security by lazy { SecurityStore(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            var unlocked by remember { mutableStateOf(false) }
            if (unlocked) PCDarkiDesktop(security)
            else PCDarkiAccountLogin(security, { unlocked = true }) { authenticateBiometric { unlocked = true } }
        }
    }

    private fun authenticateBiometric(onSuccess: () -> Unit) {
        val authenticators = BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.BIOMETRIC_WEAK
        if (BiometricManager.from(this).canAuthenticate(authenticators) != BiometricManager.BIOMETRIC_SUCCESS) return
        BiometricPrompt(this, mainExecutor, object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) { onSuccess() }
        }).authenticate(
            BiometricPrompt.PromptInfo.Builder()
                .setTitle("Unlock PC-DARKI")
                .setSubtitle("Use your device biometric")
                .setNegativeButtonText("Use PIN")
                .build()
        )
    }
}

@Composable
private fun PCDarkiAccountLogin(security: SecurityStore, onUnlocked: () -> Unit, onBiometric: () -> Unit) {
    val setupRequired = !security.isConfigured
    var username by remember { mutableStateOf("") }
    var pin by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    val authenticators = BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.BIOMETRIC_WEAK
    val biometricAvailable = remember { BiometricManager.from(context).canAuthenticate(authenticators) == BiometricManager.BIOMETRIC_SUCCESS }

    MaterialTheme {
        Box(
            Modifier.fillMaxSize().background(Brush.linearGradient(listOf(Color(0xFF080C16), Color(0xFF21183D), Color(0xFF0B1020)))),
            contentAlignment = Alignment.Center
        ) {
            Surface(Modifier.width(390.dp), shape = RoundedCornerShape(28.dp), color = Color(0xEE171B27), tonalElevation = 10.dp) {
                Column(Modifier.padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(if (setupRequired) Icons.Default.PersonAdd else Icons.Default.Lock, null, Modifier.size(58.dp), tint = Color.White)
                    Spacer(Modifier.height(14.dp))
                    Text(if (setupRequired) "Set up PC-DARKI" else "Unlock PC-DARKI", color = Color.White, fontSize = 26.sp)
                    Text(if (setupRequired) "Create your local account" else "Local desktop security", color = Color.White.copy(alpha = .6f), fontSize = 13.sp)
                    Spacer(Modifier.height(22.dp))
                    if (setupRequired) {
                        OutlinedTextField(username, { username = it; error = null }, label = { Text("Username") }, singleLine = true)
                        Spacer(Modifier.height(10.dp))
                    } else {
                        Text(security.username, color = Color.White, fontSize = 18.sp)
                        Spacer(Modifier.height(10.dp))
                    }
                    OutlinedTextField(pin, { pin = it; error = null }, label = { Text("PIN") }, singleLine = true, visualTransformation = PasswordVisualTransformation())
                    if (setupRequired) {
                        Spacer(Modifier.height(10.dp))
                        OutlinedTextField(confirm, { confirm = it; error = null }, label = { Text("Confirm PIN") }, singleLine = true, visualTransformation = PasswordVisualTransformation())
                    }
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = {
                        when {
                            setupRequired && username.trim().isEmpty() -> error = "Enter a username."
                            setupRequired && username.trim().length > 32 -> error = "Username must be 32 characters or fewer."
                            pin.length < 4 -> error = "PIN must be at least 4 characters."
                            setupRequired && pin != confirm -> error = "PINs do not match."
                            setupRequired -> { security.createAccount(username.trim(), pin); onUnlocked() }
                            security.verifyPin(pin) -> onUnlocked()
                            else -> error = "Incorrect PIN."
                        }
                    }, Modifier.fillMaxWidth()) { Text(if (setupRequired) "Create account" else "Unlock") }
                    if (!setupRequired && biometricAvailable) {
                        Spacer(Modifier.height(10.dp))
                        OutlinedButton(onClick = onBiometric, Modifier.fillMaxWidth()) {
                            Icon(Icons.Default.Fingerprint, null)
                            Spacer(Modifier.width(8.dp))
                            Text("Use biometrics")
                        }
                    }
                    error?.let {
                        Spacer(Modifier.height(10.dp))
                        Text(it, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun PCDarkiDesktop(security: SecurityStore) {
    val context = LocalContext.current
    var startOpen by remember { mutableStateOf(false) }
    var nextId by remember { mutableIntStateOf(1) }
    var activeId by remember { mutableStateOf<Int?>(null) }
    val windows = remember { mutableStateListOf<DesktopWindow>() }

    fun openWindow(title: String) {
        val existing = windows.lastOrNull { it.title == title }
        if (existing != null) {
            val i = windows.indexOfFirst { it.id == existing.id }
            windows[i] = existing.copy(minimized = false)
            activeId = existing.id
        } else {
            val id = nextId++
            windows.add(DesktopWindow(id, title))
            activeId = id
        }
    }

    MaterialTheme {
        Surface(Modifier.fillMaxSize()) {
            Box(Modifier.fillMaxSize().background(Brush.linearGradient(listOf(Color(0xFF0B1020), Color(0xFF21183D), Color(0xFF080C16))))) {
                Column(Modifier.fillMaxSize().padding(24.dp)) {
                    Text("PC-DARKI", color = Color.White.copy(alpha = .9f), fontSize = 20.sp)
                    Text("Signed in as ${security.username}", color = Color.White.copy(alpha = .55f), fontSize = 12.sp)
                    Spacer(Modifier.height(18.dp))
                    DesktopIcon("Files", Icons.Default.Folder) { openWindow("Files") }
                    Spacer(Modifier.height(14.dp))
                    DesktopIcon("Browser", Icons.Default.Language) { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com"))) }
                    Spacer(Modifier.height(14.dp))
                    DesktopIcon("Terminal", Icons.Default.Terminal) { openWindow("Terminal") }
                    Spacer(Modifier.height(14.dp))
                    DesktopIcon("Text Editor", Icons.Default.TextSnippet) { openWindow("Text Editor") }
                    Spacer(Modifier.height(14.dp))
                    DesktopIcon("Image Viewer", Icons.Default.Image) { openWindow("Image Viewer") }
                    Spacer(Modifier.height(14.dp))
                    DesktopIcon("App Manager", Icons.Default.Apps) { openWindow("App Manager") }
                    Spacer(Modifier.height(14.dp))
                    DesktopIcon("Settings", Icons.Default.Settings) { openWindow("Settings") }
                }

                windows.filter { !it.minimized }.forEach { window ->
                    AppWindow(
                        window,
                        window.id == activeId,
                        { activeId = window.id },
                        { windows.removeAll { it.id == window.id }; activeId = windows.lastOrNull { !it.minimized }?.id },
                        {
                            val i = windows.indexOfFirst { it.id == window.id }
                            if (i >= 0) windows[i] = windows[i].copy(minimized = true)
                            activeId = windows.lastOrNull { !it.minimized && it.id != window.id }?.id
                        },
                        security
                    ) { dx, dy ->
                        val i = windows.indexOfFirst { it.id == window.id }
                        if (i >= 0) windows[i] = windows[i].copy(x = windows[i].x + dx, y = windows[i].y + dy)
                    }
                }

                if (startOpen) StartMenu { name -> startOpen = false; openWindow(name) }
                Taskbar(windows, activeId, { startOpen = !startOpen }) { id ->
                    val i = windows.indexOfFirst { it.id == id }
                    if (i >= 0) {
                        windows[i] = windows[i].copy(minimized = false)
                        activeId = id
                    }
                }
            }
        }
    }
}

@Composable
private fun DesktopIcon(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Column(Modifier.width(86.dp).clickable(onClick = onClick), horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(shape = RoundedCornerShape(16.dp), color = Color.White.copy(alpha = .10f)) {
            Icon(icon, label, Modifier.padding(14.dp).size(34.dp), tint = Color.White)
        }
        Spacer(Modifier.height(5.dp))
        Text(label, color = Color.White, fontSize = 12.sp)
    }
}

@Composable
private fun Taskbar(windows: List<DesktopWindow>, activeId: Int?, onStart: () -> Unit, onWindow: (Int) -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
        Surface(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp),
            RoundedCornerShape(22.dp),
            color = Color(0xE8111521)
        ) {
            Row(
                Modifier.fillMaxWidth().height(62.dp).padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Surface(Modifier.size(44.dp).clickable(onClick = onStart), RoundedCornerShape(14.dp), color = Color(0xFF7657F6)) {
                        Box(contentAlignment = Alignment.Center) { Text("D", color = Color.White, fontSize = 20.sp) }
                    }
                    Spacer(Modifier.width(10.dp))
                    Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                        windows.forEach { w ->
                            Surface(
                                Modifier.padding(3.dp).clickable { onWindow(w.id) },
                                RoundedCornerShape(10.dp),
                                color = if (w.id == activeId && !w.minimized) Color(0xFF38304F) else Color.Transparent
                            ) {
                                Text(
                                    w.title,
                                    color = Color.White.copy(alpha = if (w.minimized) .55f else .9f),
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.width(8.dp))
                PCDarkiSystemTray()
            }
        }
    }
}

@Composable
private fun StartMenu(onOpen: (String) -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomStart) {
        Surface(Modifier.padding(start = 24.dp, bottom = 92.dp).width(320.dp), RoundedCornerShape(22.dp), color = Color(0xF21A1E2A)) {
            Column(Modifier.padding(22.dp)) {
                Text("PC-DARKI", color = Color.White, fontSize = 24.sp)
                Text("Desktop", color = Color.White.copy(alpha = .55f), fontSize = 12.sp)
                Spacer(Modifier.height(18.dp))
                StartItem("Files", Icons.Default.Folder, onOpen)
                StartItem("Browser", Icons.Default.Language, onOpen)
                StartItem("Terminal", Icons.Default.Terminal, onOpen)
                StartItem("Text Editor", Icons.Default.TextSnippet, onOpen)
                StartItem("Image Viewer", Icons.Default.Image, onOpen)
                StartItem("App Manager", Icons.Default.Apps, onOpen)
                StartItem("Settings", Icons.Default.Settings, onOpen)
            }
        }
    }
}

@Composable
private fun StartItem(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onOpen: (String) -> Unit) {
    Row(Modifier.fillMaxWidth().clickable { onOpen(label) }.padding(vertical = 11.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, label, Modifier.size(24.dp), tint = Color.White.copy(alpha = .9f))
        Spacer(Modifier.width(14.dp))
        Text(label, color = Color.White, fontSize = 15.sp)
    }
}

@Composable
private fun AppWindow(window: DesktopWindow, active: Boolean, onFocus: () -> Unit, onClose: () -> Unit, onMinimize: () -> Unit, security: SecurityStore, onMove: (Float, Float) -> Unit) {
    Box(Modifier.fillMaxSize().offset { IntOffset(window.x.roundToInt(), window.y.roundToInt()) }, contentAlignment = Alignment.Center) {
        Surface(
            Modifier.fillMaxWidth(.72f).fillMaxHeight(.68f).clickable(onClick = onFocus),
            RoundedCornerShape(18.dp),
            color = if (active) Color(0xF21A1D26) else Color(0xE8161922),
            tonalElevation = if (active) 12.dp else 4.dp
        ) {
            Column(Modifier.fillMaxSize()) {
                Row(
                    Modifier.fillMaxWidth().height(52.dp)
                        .pointerInput(window.id) { detectDragGestures { change, amount -> change.consume(); onMove(amount.x, amount.y) } }
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(window.title, color = Color.White, fontSize = 16.sp)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("—", color = Color.White.copy(alpha = .8f), Modifier.clickable(onClick = onMinimize).padding(horizontal = 10.dp))
                        Text("✕", color = Color.White, Modifier.clickable(onClick = onClose).padding(horizontal = 6.dp))
                    }
                }
                WindowContent(window.title, security)
            }
        }
    }
}

@Composable
private fun WindowContent(title: String, security: SecurityStore) {
    Box(Modifier.fillMaxSize().padding(18.dp)) {
        when (title) {
            "Files" -> PCDarkiFileManagerHost()
            "Settings" -> SettingsWindow(security)
            "Text Editor" -> PCDarkiTextEditor()
            "Terminal" -> PCDarkiTerminal()
            "Image Viewer" -> PCDarkiImageViewer()
            "App Manager" -> PCDarkiAppManager()
            else -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("$title — PC-DARKI", color = Color.White.copy(alpha = .65f), fontSize = 18.sp)
            }
        }
    }
}

@Composable
private fun SettingsWindow(security: SecurityStore) {
    var name by remember(security.username) { mutableStateOf(security.username) }
    var oldPin by remember { mutableStateOf("") }
    var newPin by remember { mutableStateOf("") }
    var status by remember { mutableStateOf<String?>(null) }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Text("Settings", color = Color.White, fontSize = 24.sp)
        Text("Account and security", color = Color.White.copy(alpha = .55f), fontSize = 12.sp)
        Spacer(Modifier.height(20.dp))
        OutlinedTextField(name, { name = it }, label = { Text("Username") }, singleLine = true, Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        Button(onClick = {
            try {
                security.updateUsername(name)
                status = "Username updated."
            } catch (e: IllegalArgumentException) {
                status = e.message ?: "Unable to update username."
            }
        }) { Text("Save username") }
        Spacer(Modifier.height(24.dp))
        OutlinedTextField(oldPin, { oldPin = it }, label = { Text("Current PIN") }, singleLine = true, visualTransformation = PasswordVisualTransformation(), Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(newPin, { newPin = it }, label = { Text("New PIN") }, singleLine = true, visualTransformation = PasswordVisualTransformation(), Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        Button(onClick = {
            try {
                if (security.changePin(oldPin, newPin)) {
                    oldPin = ""
                    newPin = ""
                    status = "PIN changed."
                } else {
                    status = "Current PIN is incorrect or new PIN is too short."
                }
            } catch (e: IllegalArgumentException) {
                status = e.message ?: "Unable to change PIN."
            }
        }) { Text("Change PIN") }
        status?.let {
            Spacer(Modifier.height(12.dp))
            Text(it, color = Color.White.copy(alpha = .75f), fontSize = 12.sp)
        }
    }
}
