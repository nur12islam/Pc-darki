package com.nurislam.pcdarki

import androidx.biometric.BiometricManager
import androidx.compose.foundation.background
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun PCDarkiAccountLogin(security: SecurityStore, onUnlocked: () -> Unit, onBiometric: () -> Unit) {
    val setupRequired = !security.isConfigured
    var username by remember { mutableStateOf("") }
    var pin by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    val authenticators = BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.BIOMETRIC_WEAK
    val biometricAvailable = remember { BiometricManager.from(context).canAuthenticate(authenticators) == BiometricManager.BIOMETRIC_SUCCESS }
    MaterialTheme(colorScheme = darkColorScheme()) {
        Box(Modifier.fillMaxSize().background(Brush.linearGradient(listOf(Color(0xFF080C16), Color(0xFF21183D), Color(0xFF0B1020)))), contentAlignment = Alignment.Center) {
            Surface(Modifier.width(390.dp), shape = RoundedCornerShape(28.dp), color = Color(0xEE171B27), tonalElevation = 10.dp) {
                Column(Modifier.padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(if (setupRequired) Icons.Default.PersonAdd else Icons.Default.Lock, null, Modifier.size(58.dp), tint = Color.White)
                    Spacer(Modifier.height(14.dp)); Text(if (setupRequired) "Set up PC-DARKI" else "Unlock PC-DARKI", color = Color.White, fontSize = 26.sp)
                    Text(if (setupRequired) "Create your local account" else "Local desktop security", color = Color.White.copy(alpha = .6f), fontSize = 13.sp)
                    Spacer(Modifier.height(22.dp))
                    if (setupRequired) { OutlinedTextField(username, { username = it; error = null }, label = { Text("Username") }, singleLine = true, modifier = Modifier.fillMaxWidth()); Spacer(Modifier.height(10.dp)) }
                    else { Text(security.username, color = Color.White, fontSize = 18.sp); Spacer(Modifier.height(10.dp)) }
                    OutlinedTextField(pin, { pin = it; error = null }, label = { Text("PIN") }, singleLine = true, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth())
                    if (setupRequired) { Spacer(Modifier.height(10.dp)); OutlinedTextField(confirm, { confirm = it; error = null }, label = { Text("Confirm PIN") }, singleLine = true, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth()) }
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
                    }, modifier = Modifier.fillMaxWidth()) { Text(if (setupRequired) "Create account" else "Unlock") }
                    if (!setupRequired && biometricAvailable) { Spacer(Modifier.height(10.dp)); OutlinedButton(onClick = onBiometric, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Default.Fingerprint, null); Spacer(Modifier.width(8.dp)); Text("Use biometrics") } }
                    error?.let { Spacer(Modifier.height(10.dp)); Text(it, color = MaterialTheme.colorScheme.error, fontSize = 12.sp) }
                }
            }
        }
    }
}
