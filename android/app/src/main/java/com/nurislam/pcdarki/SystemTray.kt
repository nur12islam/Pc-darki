package com.nurislam.pcdarki

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun PCDarkiSystemTray(onOpenNotifications: () -> Unit = {}) {
    val context = LocalContext.current
    var expanded by remember { mutableStateOf(false) }
    val time = remember { mutableStateOf(SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())) }

    LaunchedEffect(Unit) {
        while (true) {
            time.value = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
            kotlinx.coroutines.delay(1000)
        }
    }

    Box {
        Surface(
            modifier = Modifier.clickable { expanded = !expanded },
            shape = RoundedCornerShape(14.dp),
            color = Color.White.copy(alpha = .07f)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Wifi, "Network", tint = Color.White.copy(alpha = .8f), modifier = Modifier.size(17.dp))
                Spacer(Modifier.width(9.dp))
                Icon(Icons.Default.BatteryFull, "Battery", tint = Color.White.copy(alpha = .8f), modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(9.dp))
                Icon(Icons.Default.Notifications, "Notifications", tint = Color.White.copy(alpha = .8f), modifier = Modifier.size(18.dp).clickable { onOpenNotifications() })
                Spacer(Modifier.width(10.dp))
                Text(time.value, color = Color.White, fontSize = 13.sp)
            }
        }

        if (expanded) {
            Surface(
                modifier = Modifier.align(Alignment.BottomEnd).offset(y = (-52).dp).width(260.dp),
                shape = RoundedCornerShape(18.dp),
                color = Color(0xF21A1E2A),
                tonalElevation = 8.dp
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text("Quick Settings", color = Color.White, fontSize = 18.sp)
                    Spacer(Modifier.height(10.dp))
                    TrayAction("Wi-Fi", "Open Android network settings") {
                        context.startActivity(Intent(Settings.ACTION_WIFI_SETTINGS))
                        expanded = false
                    }
                    TrayAction("Notifications", "Open PC-DARKI notifications") {
                        onOpenNotifications()
                        expanded = false
                    }
                    TrayAction("Android Settings", "Open system settings") {
                        context.startActivity(Intent(Settings.ACTION_SETTINGS))
                        expanded = false
                    }
                }
            }
        }
    }
}

@Composable
private fun TrayAction(title: String, subtitle: String, onClick: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 9.dp)
    ) {
        Text(title, color = Color.White, fontSize = 14.sp)
        Text(subtitle, color = Color.White.copy(alpha = .5f), fontSize = 10.sp)
    }
}

fun openAndroidSettings(context: Context) {
    context.startActivity(Intent(Settings.ACTION_SETTINGS))
}
