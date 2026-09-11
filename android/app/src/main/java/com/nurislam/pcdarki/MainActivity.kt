package com.nurislam.pcdarki

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

data class DesktopWindow(val id: Int, val title: String, val minimized: Boolean = false, val x: Float = 0f, val y: Float = 0f)

class MainActivity : ComponentActivity() {
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
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) = onSuccess()
        }).authenticate(BiometricPrompt.PromptInfo.Builder().setTitle("Unlock PC-DARKI").setSubtitle("Use your device biometric").setNegativeButtonText("Use PIN").build())
    }
}

@Composable
private fun PCDarkiAccountLogin(security: SecurityStore, onUnlocked: () -> Unit, onBiometric: () -> Unit) {
    val setupRequired = !security.isConfigured
    var username by remember { mutableStateOf("") }; var pin by remember { mutableStateOf("") }; var confirm by remember { mutableStateOf("") }; var error by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    val authenticators = BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.BIOMETRIC_WEAK
    val biometricAvailable = remember { BiometricManager.from(context).canAuthenticate(authenticators) == BiometricManager.BIOMETRIC_SUCCESS }
    MaterialTheme { Box(Modifier.fillMaxSize().background(Brush.linearGradient(listOf(Color(0xFF080C16), Color(0xFF21183D), Color(0xFF0B1020)))), Alignment.Center) {
        Surface(Modifier.width(390.dp), RoundedCornerShape(28.dp), color = Color(0xEE171B27), tonalElevation = 10.dp) { Column(Modifier.padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(if (setupRequired) Icons.Default.PersonAdd else Icons.Default.Lock, null, Modifier.size(58.dp), tint = Color.White); Spacer(Modifier.height(14.dp)); Text(if (setupRequired) "Set up PC-DARKI" else "Unlock PC-DARKI", color = Color.White, fontSize = 26.sp); Text(if (setupRequired) "Create your local account" else "Local desktop security", color = Color.White.copy(.6f), fontSize = 13.sp); Spacer(Modifier.height(22.dp))
            if (setupRequired) { OutlinedTextField(username, { username = it; error = null }, label = { Text("Username") }, singleLine = true); Spacer(Modifier.height(10.dp)) } else { Text(security.username, color = Color.White, fontSize = 18.sp); Spacer(Modifier.height(10.dp)) }
            OutlinedTextField(pin, { pin = it; error = null }, label = { Text("PIN") }, singleLine = true, visualTransformation = PasswordVisualTransformation())
            if (setupRequired) { Spacer(Modifier.height(10.dp)); OutlinedTextField(confirm, { confirm = it; error = null }, label = { Text("Confirm PIN") }, singleLine = true, visualTransformation = PasswordVisualTransformation()) }
            Spacer(Modifier.height(16.dp)); Button(onClick = { when { setupRequired && username.trim().isEmpty() -> error = "Enter a username."; setupRequired && username.trim().length > 32 -> error = "Username must be 32 characters or fewer."; pin.length < 4 -> error = "PIN must be at least 4 characters."; setupRequired && pin != confirm -> error = "PINs do not match."; setupRequired -> { security.createAccount(username.trim(), pin); onUnlocked() }; security.verifyPin(pin) -> onUnlocked(); else -> error = "Incorrect PIN." } }, Modifier.fillMaxWidth()) { Text(if (setupRequired) "Create account" else "Unlock") }
            if (!setupRequired && biometricAvailable) { Spacer(Modifier.height(10.dp)); OutlinedButton(onClick = onBiometric, Modifier.fillMaxWidth()) { Icon(Icons.Default.Fingerprint, null); Spacer(Modifier.width(8.dp)); Text("Use biometrics") } }
            error?.let { Spacer(Modifier.height(10.dp)); Text(it, color = MaterialTheme.colorScheme.error, fontSize = 12.sp) }
        } }
    } }
}

@Composable
fun PCDarkiDesktop(security: SecurityStore) {
    var startOpen by remember { mutableStateOf(false) }; var nextId by remember { mutableStateOf(1) }; var activeId by remember { mutableStateOf<Int?>(null) }; val windows = remember { mutableStateListOf<DesktopWindow>() }
    fun openWindow(title: String) { val existing = windows.lastOrNull { it.title == title }; if (existing != null) { val i = windows.indexOfFirst { it.id == existing.id }; windows[i] = existing.copy(minimized = false); activeId = existing.id; return }; val id = nextId++; windows.add(DesktopWindow(id, title)); activeId = id }
    fun closeWindow(id: Int) { windows.removeAll { it.id == id }; activeId = windows.lastOrNull { !it.minimized }?.id }
    fun minimizeWindow(id: Int) { val i = windows.indexOfFirst { it.id == id }; if (i >= 0) windows[i] = windows[i].copy(minimized = true); activeId = windows.lastOrNull { !it.minimized && it.id != id }?.id }
    MaterialTheme { Surface(Modifier.fillMaxSize()) { Box(Modifier.fillMaxSize().background(Brush.linearGradient(listOf(Color(0xFF0B1020), Color(0xFF21183D), Color(0xFF080C16))))) {
        Column(Modifier.fillMaxSize().padding(24.dp)) { Text("PC-DARKI", Color.White.copy(.9f), 20.sp); Text("Signed in as ${security.username}", Color.White.copy(.55f), 12.sp); Spacer(Modifier.height(18.dp)); DesktopIcon("Files", Icons.Default.Folder) { openWindow("Files") }; Spacer(Modifier.height(14.dp)); DesktopIcon("Browser", Icons.Default.Language) { LocalContext.current.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com"))) }; Spacer(Modifier.height(14.dp)); DesktopIcon("Terminal", Icons.Default.Terminal) { openWindow("Terminal") }; Spacer(Modifier.height(14.dp)); DesktopIcon("Settings", Icons.Default.Settings) { openWindow("Settings") } }
        windows.filter { !it.minimized }.forEach { w -> AppWindow(w, w.id == activeId, { activeId = w.id }, { closeWindow(w.id) }, { minimizeWindow(w.id) }, security) { dx, dy -> val i = windows.indexOfFirst { it.id == w.id }; if (i >= 0) windows[i] = windows[i].copy(x = windows[i].x + dx, y = windows[i].y + dy) } }
        if (startOpen) StartMenu { name -> startOpen = false; openWindow(name) }; Taskbar(windows, activeId, { startOpen = !startOpen }) { id -> val i = windows.indexOfFirst { it.id == id }; if (i >= 0) { windows[i] = windows[i].copy(minimized = false); activeId = id } }
    } } }
}

@Composable private fun DesktopIcon(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) { Column(Modifier.width(86.dp).clickable(onClick = onClick), Alignment.CenterHorizontally) { Surface(RoundedCornerShape(16.dp), Color.White.copy(.10f)) { Icon(icon, label, Modifier.padding(14.dp).size(34.dp), tint = Color.White) }; Spacer(Modifier.height(5.dp)); Text(label, Color.White, 12.sp) } }
@Composable private fun Taskbar(windows: List<DesktopWindow>, activeId: Int?, onStart: () -> Unit, onWindow: (Int) -> Unit) { Box(Modifier.fillMaxSize(), Alignment.BottomCenter) { Surface(Modifier.fillMaxWidth().padding(20.dp, 14.dp), RoundedCornerShape(22.dp), Color(0xE8111521)) { Row(Modifier.fillMaxWidth().height(62.dp).padding(horizontal = 14.dp), Alignment.CenterVertically, Arrangement.SpaceBetween) { Row(Alignment.CenterVertically) { Surface(Modifier.size(44.dp).clickable(onClick = onStart), RoundedCornerShape(14.dp), Color(0xFF7657F6)) { Box(Alignment.Center) { Text("D", Color.White, 20.sp) } }; Spacer(Modifier.width(10.dp)); windows.forEach { w -> Surface(Modifier.padding(3.dp).clickable { onWindow(w.id) }, RoundedCornerShape(10.dp), if (w.id == activeId && !w.minimized) Color(0xFF38304F) else Color.Transparent) { Text(w.title, Color.White.copy(if (w.minimized) .55f else .9f), 12.sp, Modifier.padding(10.dp, 8.dp)) } } }; Text(SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date()), Color.White, 14.sp) } } } }
@Composable private fun StartMenu(onOpen: (String) -> Unit) { Surface(Modifier.padding(start = 24.dp, bottom = 92.dp).width(320.dp), RoundedCornerShape(22.dp), Color(0xF21A1E2A)) { Column(Modifier.padding(22.dp)) { Text("PC-DARKI", Color.White, 24.sp); Text("Desktop", Color.White.copy(.55f), 12.sp); Spacer(Modifier.height(18.dp)); StartItem("Files", Icons.Default.Folder, onOpen); StartItem("Browser", Icons.Default.Language, onOpen); StartItem("Terminal", Icons.Default.Terminal, onOpen); StartItem("Text Editor", Icons.Default.TextSnippet, onOpen); StartItem("Settings", Icons.Default.Settings, onOpen) } } }
@Composable private fun StartItem(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onOpen: (String) -> Unit) { Row(Modifier.fillMaxWidth().clickable { onOpen(label) }.padding(vertical = 11.dp), Alignment.CenterVertically) { Icon(icon, label, Modifier.size(24.dp), tint = Color.White.copy(.9f)); Spacer(Modifier.width(14.dp)); Text(label, Color.White, 15.sp) } }
@Composable private fun AppWindow(window: DesktopWindow, active: Boolean, onFocus: () -> Unit, onClose: () -> Unit, onMinimize: () -> Unit, security: SecurityStore, onMove: (Float, Float) -> Unit) { Box(Modifier.fillMaxSize().offset { IntOffset(window.x.roundToInt(), window.y.roundToInt()) }, Alignment.Center) { Surface(Modifier.fillMaxWidth(.72f).fillMaxSize(.68f).clickable(onClick = onFocus), RoundedCornerShape(18.dp), if (active) Color(0xF21A1D26) else Color(0xE8161922), tonalElevation = if (active) 12.dp else 4.dp) { Column { Row(Modifier.fillMaxWidth().height(52.dp).pointerInput(window.id) { detectDragGestures { change, amount -> change.consume(); onMove(amount.x, amount.y) } }.padding(horizontal = 16.dp), Alignment.CenterVertically, Arrangement.SpaceBetween) { Text(window.title, Color.White, 16.sp); Row { Text("—", Color.White.copy(.8f), Modifier.clickable(onClick = onMinimize).padding(horizontal = 10.dp)); Text("✕", Color.White, Modifier.clickable(onClick = onClose).padding(horizontal = 6.dp)) } }; WindowContent(window.title, security) } } } }
@Composable private fun WindowContent(title: String, security: SecurityStore) { Box(Modifier.fillMaxSize().padding(18.dp)) { when (title) { "Files" -> PCDarkiFileManagerHost(); "Settings" -> AccountSettings(security); "Text Editor" -> Box(Modifier.fillMaxSize(), Alignment.Center) { Text("Text Editor — next module", Color.White.copy(.7f), 18.sp) }; "Terminal" -> Box(Modifier.fillMaxSize(), Alignment.Center) { Text("Terminal — native command backend coming next", Color.White.copy(.7f), 18.sp) }; else -> Box(Modifier.fillMaxSize(), Alignment.Center) { Text("$title — PC-DARKI v0.2", Color.White.copy(.65f), 18.sp) } } } }

@Composable
private fun PCDarkiFileManagerHost() {
    val context = LocalContext.current
    var treeUri by remember { mutableStateOf<Uri?>(null) }
    val treeLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) {
            runCatching { context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION) }
            treeUri = uri
        }
    }
    PCDarkiFileManager(treeUri, treeLauncher)
}

@Composable private fun AccountSettings(security: SecurityStore) { var section by remember { mutableStateOf(SettingsSection.ACCOUNT) }; Row(Modifier.fillMaxSize().padding(14.dp)) { Surface(Modifier.width(190.dp).fillMaxHeight(), RoundedCornerShape(16.dp), Color(0xFF141821)) { Column(Modifier.padding(10.dp)) { Text("Settings", color = Color.White, fontSize = 22.sp, modifier = Modifier.padding(12.dp)); SettingsSection.entries.forEach { item -> val selected = item == section; Surface(Modifier.fillMaxWidth().clickable { section = item }, RoundedCornerShape(10.dp), if (selected) Color(0xFF39304F) else Color.Transparent) { Row(Modifier.padding(horizontal = 12.dp, vertical = 11.dp), verticalAlignment = Alignment.CenterVertically) { Icon(when (item) { SettingsSection.ACCOUNT -> Icons.Default.Person; SettingsSection.APPEARANCE -> Icons.Default.Palette; SettingsSection.DISPLAY -> Icons.Default.DisplaySettings; SettingsSection.ABOUT -> Icons.Default.Info }, null, Modifier.size(20.dp), tint = Color.White.copy(if (selected) .95f else .65f)); Spacer(Modifier.width(10.dp)); Text(item.title, color = Color.White.copy(if (selected) 1f else .7f), fontSize = 13.sp) } }; Spacer(Modifier.height(4.dp)) } } }; Spacer(Modifier.width(14.dp)); Surface(Modifier.weight(1f).fillMaxHeight(), RoundedCornerShape(16.dp), Color(0xFF10141D)) { Box(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) { when (section) { SettingsSection.ACCOUNT -> AccountSettingsPage(security); SettingsSection.APPEARANCE -> AppearanceSettingsPage(); SettingsSection.DISPLAY -> DisplaySettingsPage(); SettingsSection.ABOUT -> AboutSettingsPage() } } } } }
@Composable private fun SettingsHeader(title: String, subtitle: String) { Column(Modifier.fillMaxWidth().padding(24.dp, 22.dp, 24.dp, 10.dp)) { Text(title, color = Color.White, fontSize = 24.sp); Spacer(Modifier.height(4.dp)); Text(subtitle, color = Color.White.copy(.55f), fontSize = 12.sp) } }
@Composable private fun AccountSettingsPage(security: SecurityStore) { var name by remember(security.username) { mutableStateOf(security.username) }; var oldPin by remember { mutableStateOf("") }; var newPin by remember { mutableStateOf("") }; var status by remember { mutableStateOf<String?>(null) }; Column(Modifier.fillMaxWidth()) { SettingsHeader("Account", "Manage your local PC-DARKI account and security."); Column(Modifier.padding(24.dp)) { Text("Profile", color = Color.White, fontSize = 17.sp); Spacer(Modifier.height(10.dp)); OutlinedTextField(name, { name = it }, label = { Text("Username") }, singleLine = true, modifier = Modifier.fillMaxWidth()); Spacer(Modifier.height(10.dp)); Button(onClick = { try { security.updateUsername(name); status = "Username updated." } catch (e: IllegalArgumentException) { status = e.message } }) { Text("Save username") }; Spacer(Modifier.height(26.dp)); HorizontalDivider(color = Color.White.copy(.08f)); Spacer(Modifier.height(22.dp)); Text("Security", color = Color.White, fontSize = 17.sp); Spacer(Modifier.height(10.dp)); OutlinedTextField(oldPin, { oldPin = it }, label = { Text("Current PIN") }, singleLine = true, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth()); Spacer(Modifier.height(8.dp)); OutlinedTextField(newPin, { newPin = it }, label = { Text("New PIN") }, singleLine = true, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth()); Spacer(Modifier.height(10.dp)); Button(onClick = { status = if (security.changePin(oldPin, newPin)) { oldPin = ""; newPin = ""; "PIN changed successfully." } else "Current PIN is incorrect or new PIN is too short." }) { Text("Change PIN") }; status?.let { Spacer(Modifier.height(12.dp)); Text(it, color = Color.White.copy(.8f), fontSize = 12.sp) } } } }
@Composable private fun AppearanceSettingsPage() { var darkMode by remember { mutableStateOf(true) }; var animations by remember { mutableStateOf(true) }; Column(Modifier.fillMaxWidth()) { SettingsHeader("Appearance", "Customize how the PC-DARKI desktop looks and feels."); Column(Modifier.padding(24.dp)) { SettingsToggle("Dark interface", "Use the dark PC-DARKI visual theme.", darkMode) { darkMode = it }; Spacer(Modifier.height(8.dp)); SettingsToggle("Window animations", "Enable visual transitions where supported.", animations) { animations = it }; Spacer(Modifier.height(18.dp)); Text("Accent", color = Color.White, fontSize = 16.sp); Spacer(Modifier.height(8.dp)); Text("PC-DARKI Purple", color = Color.White.copy(.7f), fontSize = 13.sp) } } }
@Composable private fun SettingsToggle(title: String, description: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) { Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text(title, color = Color.White, fontSize = 14.sp); Spacer(Modifier.height(3.dp)); Text(description, color = Color.White.copy(.5f), fontSize = 11.sp) }; Switch(checked = checked, onCheckedChange = onCheckedChange) } }
@Composable private fun DisplaySettingsPage() { var scale by remember { mutableFloatStateOf(1f) }; Column(Modifier.fillMaxWidth()) { SettingsHeader("Display", "Desktop display preferences for the current Android device."); Column(Modifier.padding(24.dp)) { Text("Interface scale", color = Color.White, fontSize = 16.sp); Spacer(Modifier.height(4.dp)); Text("${(scale * 100).roundToInt()}%", color = Color.White.copy(.6f), fontSize = 12.sp); Slider(value = scale, onValueChange = { scale = it }, valueRange = .8f..1.2f, steps = 3); Spacer(Modifier.height(18.dp)); Text("Window mode", color = Color.White, fontSize = 16.sp); Spacer(Modifier.height(6.dp)); Text("Fullscreen desktop shell", color = Color.White.copy(.65f), fontSize = 13.sp) } } }
@Composable private fun AboutSettingsPage() { Column(Modifier.fillMaxWidth()) { SettingsHeader("About PC-DARKI", "Native Android desktop shell prototype."); Column(Modifier.padding(24.dp)) { Text("PC-DARKI", color = Color.White, fontSize = 28.sp); Spacer(Modifier.height(6.dp)); Text("Version 0.2", color = Color.White.copy(.6f), fontSize = 13.sp); Spacer(Modifier.height(20.dp)); Text("PC-DARKI is being built as a native Android desktop-style environment with touch, mouse and keyboard support.", color = Color.White.copy(.72f), fontSize = 14.sp); Spacer(Modifier.height(20.dp)); Text("Current milestone", color = Color.White, fontSize = 16.sp); Spacer(Modifier.height(6.dp)); Text("Desktop shell • Start menu • Taskbar • Windows • Local account security • Settings • File Manager", color = Color.White.copy(.62f), fontSize = 13.sp) } } }
