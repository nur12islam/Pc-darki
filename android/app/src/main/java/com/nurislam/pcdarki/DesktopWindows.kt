package com.nurislam.pcdarki

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
internal fun AppWindowV2(
    window: DesktopWindowV2,
    active: Boolean,
    onFocus: () -> Unit,
    onClose: () -> Unit,
    onMinimize: () -> Unit,
    onMove: (Float, Float) -> Unit,
    onResize: (Float, Float, Float, Float) -> Unit,
    security: SecurityStore
) {
    BoxWithConstraints(Modifier.fillMaxSize().offset { IntOffset(window.x.toInt(), window.y.toInt()) }) {
        val maxW = maxWidth.value
        val maxH = maxHeight.value
        val density = LocalDensity.current
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Surface(
                Modifier.width((maxW * window.widthFraction).dp).height((maxH * window.heightFraction).dp),
                RoundedCornerShape(18.dp),
                color = if (active) Color(0xF21A1D26) else Color(0xE8161922),
                tonalElevation = if (active) 12.dp else 4.dp
            ) {
                Box(Modifier.fillMaxSize()) {
                    Column(Modifier.fillMaxSize()) {
                        Row(
                            Modifier.fillMaxWidth().height(52.dp).pointerInput(window.id) {
                                detectDragGestures { change, amount -> change.consume(); onMove(amount.x, amount.y) }
                            }.padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(window.icon, null, Modifier.size(20.dp), tint = Color.White.copy(alpha = .9f))
                                Spacer(Modifier.width(8.dp)); Text(window.title, color = Color.White, fontSize = 16.sp)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("—", color = Color.White.copy(alpha = .8f), modifier = Modifier.padding(horizontal = 10.dp).clickable(onClick = onMinimize))
                                Text("✕", color = Color.White, modifier = Modifier.padding(horizontal = 6.dp).clickable(onClick = onClose))
                            }
                        }
                        WindowContentV2(window.title, security)
                    }
                    Box(
                        Modifier.align(Alignment.BottomEnd).size(30.dp).pointerInput(window.id) {
                            detectDragGestures { change, amount ->
                                change.consume()
                                val dx = with(density) { amount.x.toDp().value }
                                val dy = with(density) { amount.y.toDp().value }
                                onResize(dx, dy, maxW, maxH)
                            }
                        },
                        contentAlignment = Alignment.BottomEnd
                    ) { Text("↘", color = Color.White.copy(alpha = .55f), fontSize = 17.sp, modifier = Modifier.padding(5.dp)) }
                }
            }
        }
    }
}

@Composable
private fun WindowContentV2(title: String, security: SecurityStore) {
    Box(Modifier.fillMaxSize().padding(18.dp)) {
        when (title) {
            "Files" -> PCDarkiFileManagerHost()
            "Browser" -> PCDarkiBrowser()
            "Settings" -> SettingsWindowV2(security)
            "Text Editor" -> PCDarkiTextEditor()
            "Terminal" -> PCDarkiTerminal()
            "Image Viewer" -> PCDarkiImageViewer()
            "App Manager" -> PCDarkiAppManager()
        }
    }
}

@Composable
private fun SettingsWindowV2(security: SecurityStore) {
    var name by remember(security.username) { mutableStateOf(security.username) }
    var oldPin by remember { mutableStateOf("") }
    var newPin by remember { mutableStateOf("") }
    var status by remember { mutableStateOf<String?>(null) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Text("Settings", color = Color.White, fontSize = 24.sp)
        Text("Account and security", color = Color.White.copy(alpha = .55f), fontSize = 12.sp)
        Spacer(Modifier.height(20.dp))
        OutlinedTextField(name, { name = it }, label = { Text("Username") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        Button(onClick = { try { security.updateUsername(name); status = "Username updated." } catch (e: IllegalArgumentException) { status = e.message ?: "Unable to update username." } }) { Text("Save username") }
        Spacer(Modifier.height(24.dp))
        OutlinedTextField(oldPin, { oldPin = it }, label = { Text("Current PIN") }, singleLine = true, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(newPin, { newPin = it }, label = { Text("New PIN") }, singleLine = true, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        Button(onClick = { try { if (security.changePin(oldPin, newPin)) { oldPin = ""; newPin = ""; status = "PIN changed." } else status = "Current PIN is incorrect or new PIN is too short." } catch (e: IllegalArgumentException) { status = e.message ?: "Unable to change PIN." } }) { Text("Change PIN") }
        status?.let { Spacer(Modifier.height(12.dp)); Text(it, color = Color.White.copy(alpha = .8f), fontSize = 12.sp) }
    }
}
