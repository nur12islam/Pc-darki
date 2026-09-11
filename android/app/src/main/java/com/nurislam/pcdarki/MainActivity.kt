package com.nurislam.pcdarki

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

data class DesktopWindow(val id: Int, val title: String, val minimized: Boolean = false, val x: Float = 0f, val y: Float = 0f)

class MainActivity : ComponentActivity() {
    private val security by lazy { SecurityStore(this) }
    private var selectedFile by mutableStateOf<String?>(null)
    private val openDocument = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> uri?.let { selectedFile = it.toString() } }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            var unlocked by remember { mutableStateOf(false) }
            if (unlocked) {
                PCDarkiDesktop(selectedFile) { openDocument.launch(arrayOf("*/*")) }
            } else {
                PCDarkiAccountLogin(
                    security = security,
                    onUnlocked = { unlocked = true },
                    onBiometric = { authenticateBiometric { unlocked = true } }
                )
            }
        }
    }

    override fun onStop() {
        super.onStop()
    }

    private fun authenticateBiometric(onSuccess: () -> Unit) {
        val authenticators = BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.BIOMETRIC_WEAK
        if (BiometricManager.from(this).canAuthenticate(authenticators) != BiometricManager.BIOMETRIC_SUCCESS) return
        BiometricPrompt(this, mainExecutor, object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) = onSuccess()
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
        Box(Modifier.fillMaxSize().background(Brush.linearGradient(listOf(Color(0xFF080C16), Color(0xFF21183D), Color(0xFF0B1020)))), Alignment.Center) {
            Surface(Modifier.width(390.dp), RoundedCornerShape(28.dp), color = Color(0xEE171B27), tonalElevation = 10.dp) {
                Column(Modifier.padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(if (setupRequired) Icons.Default.PersonAdd else Icons.Default.Lock, null, Modifier.size(58.dp), tint = Color.White)
                    Spacer(Modifier.height(14.dp))
                    Text(if (setupRequired) "Set up PC-DARKI" else "Unlock PC-DARKI", color = Color.White, fontSize = 26.sp)
                    Text(if (setupRequired) "Create your local account" else "Local desktop security", color = Color.White.copy(.6f), fontSize = 13.sp)
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
                            Icon(Icons.Default.Fingerprint, null); Spacer(Modifier.width(8.dp)); Text("Use biometrics")
                        }
                    }
                    error?.let { Spacer(Modifier.height(10.dp)); Text(it, color = MaterialTheme.colorScheme.error, fontSize = 12.sp) }
                }
            }
        }
    }
}

@Composable
fun PCDarkiDesktop(selectedFile: String?, onOpenFile: () -> Unit) {
    var startOpen by remember { mutableStateOf(false) }; var nextId by remember { mutableStateOf(1) }; var activeId by remember { mutableStateOf<Int?>(null) }; val windows = remember { mutableStateListOf<DesktopWindow>() }
    fun openWindow(title: String) { val existing = windows.lastOrNull { it.title == title }; if (existing != null) { val i = windows.indexOfFirst { it.id == existing.id }; windows[i] = existing.copy(minimized = false); activeId = existing.id; return }; val id = nextId++; windows.add(DesktopWindow(id, title)); activeId = id }
    fun closeWindow(id: Int) { windows.removeAll { it.id == id }; activeId = windows.lastOrNull { !it.minimized }?.id }
    fun minimizeWindow(id: Int) { val i = windows.indexOfFirst { it.id == id }; if (i >= 0) windows[i] = windows[i].copy(minimized = true); activeId = windows.lastOrNull { !it.minimized && it.id != id }?.id }
    MaterialTheme { Surface(Modifier.fillMaxSize()) { Box(Modifier.fillMaxSize().background(Brush.linearGradient(listOf(Color(0xFF0B1020), Color(0xFF21183D), Color(0xFF080C16))))) {
        Column(Modifier.fillMaxSize().padding(24.dp)) { Text("PC-DARKI", Color.White.copy(.9f), 20.sp); Spacer(Modifier.height(18.dp)); DesktopIcon("Files", Icons.Default.Folder) { openWindow("Files") }; Spacer(Modifier.height(14.dp)); DesktopIcon("Browser", Icons.Default.Language) { LocalContext.current.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com"))) }; Spacer(Modifier.height(14.dp)); DesktopIcon("Terminal", Icons.Default.Terminal) { openWindow("Terminal") }; Spacer(Modifier.height(14.dp)); DesktopIcon("Settings", Icons.Default.Settings) { openWindow("Settings") } }
        windows.filter { !it.minimized }.forEach { w -> AppWindow(w, w.id == activeId, selectedFile, { activeId = w.id }, { closeWindow(w.id) }, { minimizeWindow(w.id) }, onOpenFile) { dx, dy -> val i = windows.indexOfFirst { it.id == w.id }; if (i >= 0) windows[i] = windows[i].copy(x = windows[i].x + dx, y = windows[i].y + dy) } }
        if (startOpen) StartMenu { name -> startOpen = false; openWindow(name) }; Taskbar(windows, activeId, { startOpen = !startOpen }) { id -> val i = windows.indexOfFirst { it.id == id }; if (i >= 0) { windows[i] = windows[i].copy(minimized = false); activeId = id } }
    } } }
}

@Composable private fun DesktopIcon(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) { Column(Modifier.width(86.dp).clickable(onClick = onClick), Alignment.CenterHorizontally) { Surface(RoundedCornerShape(16.dp), Color.White.copy(.10f)) { Icon(icon, label, Modifier.padding(14.dp).size(34.dp), tint = Color.White) }; Spacer(Modifier.height(5.dp)); Text(label, Color.White, 12.sp) } }
@Composable private fun Taskbar(windows: List<DesktopWindow>, activeId: Int?, onStart: () -> Unit, onWindow: (Int) -> Unit) { Box(Modifier.fillMaxSize(), Alignment.BottomCenter) { Surface(Modifier.fillMaxWidth().padding(20.dp, 14.dp), RoundedCornerShape(22.dp), Color(0xE8111521)) { Row(Modifier.fillMaxWidth().height(62.dp).padding(horizontal = 14.dp), Alignment.CenterVertically, Arrangement.SpaceBetween) { Row(Alignment.CenterVertically) { Surface(Modifier.size(44.dp).clickable(onClick = onStart), RoundedCornerShape(14.dp), Color(0xFF7657F6)) { Box(Alignment.Center) { Text("D", Color.White, 20.sp) } }; Spacer(Modifier.width(10.dp)); windows.forEach { w -> Surface(Modifier.padding(3.dp).clickable { onWindow(w.id) }, RoundedCornerShape(10.dp), if (w.id == activeId && !w.minimized) Color(0xFF38304F) else Color.Transparent) { Text(w.title, Color.White.copy(if (w.minimized) .55f else .9f), 12.sp, Modifier.padding(10.dp, 8.dp)) } } }; Text(SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date()), Color.White, 14.sp) } } } }
@Composable private fun StartMenu(onOpen: (String) -> Unit) { Surface(Modifier.padding(start = 24.dp, bottom = 92.dp).width(320.dp), RoundedCornerShape(22.dp), Color(0xF21A1E2A)) { Column(Modifier.padding(22.dp)) { Text("PC-DARKI", Color.White, 24.sp); Text("Desktop", Color.White.copy(.55f), 12.sp); Spacer(Modifier.height(18.dp)); StartItem("Files", Icons.Default.Folder, onOpen); StartItem("Browser", Icons.Default.Language, onOpen); StartItem("Terminal", Icons.Default.Terminal, onOpen); StartItem("Text Editor", Icons.Default.TextSnippet, onOpen); StartItem("Settings", Icons.Default.Settings, onOpen) } } }
@Composable private fun StartItem(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onOpen: (String) -> Unit) { Row(Modifier.fillMaxWidth().clickable { onOpen(label) }.padding(vertical = 11.dp), Alignment.CenterVertically) { Icon(icon, label, Modifier.size(24.dp), tint = Color.White.copy(.9f)); Spacer(Modifier.width(14.dp)); Text(label, Color.White, 15.sp) } }
@Composable private fun AppWindow(window: DesktopWindow, active: Boolean, selectedFile: String?, onFocus: () -> Unit, onClose: () -> Unit, onMinimize: () -> Unit, onOpenFile: () -> Unit, onMove: (Float, Float) -> Unit) { Box(Modifier.fillMaxSize().offset { IntOffset(window.x.roundToInt(), window.y.roundToInt()) }, Alignment.Center) { Surface(Modifier.fillMaxWidth(.72f).fillMaxSize(.68f).clickable(onClick = onFocus), RoundedCornerShape(18.dp), if (active) Color(0xF21A1D26) else Color(0xE8161922), tonalElevation = if (active) 12.dp else 4.dp) { Column { Row(Modifier.fillMaxWidth().height(52.dp).pointerInput(window.id) { detectDragGestures { change, amount -> change.consume(); onMove(amount.x, amount.y) } }.padding(horizontal = 16.dp), Alignment.CenterVertically, Arrangement.SpaceBetween) { Text(window.title, Color.White, 16.sp); Row { Text("—", Color.White.copy(.8f), Modifier.clickable(onClick = onMinimize).padding(horizontal = 10.dp)); Text("✕", Color.White, Modifier.clickable(onClick = onClose).padding(horizontal = 6.dp)) } }; WindowContent(window.title, selectedFile, onOpenFile) } } } }
@Composable private fun WindowContent(title: String, selectedFile: String?, onOpenFile: () -> Unit) { Box(Modifier.fillMaxSize().padding(28.dp), Alignment.Center) { when (title) { "Files" -> Column(horizontalAlignment = Alignment.CenterHorizontally) { Icon(Icons.Default.Folder, null, Modifier.size(52.dp), tint = Color.White); Spacer(Modifier.height(12.dp)); Text("PC-DARKI File Manager", Color.White, 20.sp); Spacer(Modifier.height(18.dp)); Button(onClick = onOpenFile) { Text("Open file") }; Spacer(Modifier.height(14.dp)); Text(selectedFile?.let { "Selected: $it" } ?: "Use Android's document picker to access your storage.", Color.White.copy(.65f), 13.sp) }; "Text Editor" -> Text("Text Editor — next module", Color.White.copy(.7f), 18.sp); "Terminal" -> Text("Terminal — native command backend coming next", Color.White.copy(.7f), 18.sp); "Settings" -> Text("Settings — Accounts & Security coming next", Color.White.copy(.7f), 18.sp); else -> Text("$title — PC-DARKI v0.1", Color.White.copy(.65f), 18.sp) } } }
