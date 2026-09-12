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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteSweep
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
    var notificationsOpen by remember { mutableStateOf(false) }
    val notifications by PCDarkiNotificationBus.notifications.collectAsState()
    val time = remember { mutableStateOf(SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())) }

    LaunchedEffect(context) {
        PCDarkiNotificationBus.initialize(context)
    }

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
                BadgedBox(badge = {
                    if (notifications.isNotEmpty()) Badge { Text(notifications.size.toString()) }
                }) {
                    Icon(
                        Icons.Default.Notifications,
                        "Notifications",
                        tint = Color.White.copy(alpha = .8f),
                        modifier = Modifier.size(18.dp).clickable {
                            notificationsOpen = true
                            expanded = false
                            onOpenNotifications()
                        }
                    )
                }
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
                    TrayAction("Notifications", "Open PC-DARKI notification center") {
                        notificationsOpen = true
                        expanded = false
                        onOpenNotifications()
                    }
                    TrayAction("Android Settings", "Open system settings") {
                        context.startActivity(Intent(Settings.ACTION_SETTINGS))
                        expanded = false
                    }
                }
            }
        }

        if (notificationsOpen) {
            NotificationCenter(
                notifications = notifications,
                onDismiss = { notificationsOpen = false },
                onClear = { PCDarkiNotificationBus.clear() },
                onDismissNotification = { PCDarkiNotificationBus.dismiss(it) }
            )
        }
    }
}

@Composable
private fun NotificationCenter(
    notifications: List<PCDarkiNotification>,
    onDismiss: () -> Unit,
    onClear: () -> Unit,
    onDismissNotification: (Long) -> Unit
) {
    Surface(
        modifier = Modifier.align(Alignment.BottomEnd).offset(y = (-62).dp).width(340.dp).heightIn(max = 430.dp),
        shape = RoundedCornerShape(20.dp),
        color = Color(0xF2191D29),
        tonalElevation = 12.dp
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Notification Center", color = Color.White, fontSize = 19.sp)
                    Text("${notifications.size} notification${if (notifications.size == 1) "" else "s"}", color = Color.White.copy(alpha = .5f), fontSize = 11.sp)
                }
                IconButton(onClick = onClear, enabled = notifications.isNotEmpty()) {
                    Icon(Icons.Default.DeleteSweep, "Clear all", tint = Color.White.copy(alpha = .75f))
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, "Close", tint = Color.White.copy(alpha = .75f))
                }
            }
            Spacer(Modifier.height(8.dp))
            if (notifications.isEmpty()) {
                Box(Modifier.fillMaxWidth().height(150.dp), contentAlignment = Alignment.Center) {
                    Text("You're all caught up.", color = Color.White.copy(alpha = .55f), fontSize = 13.sp)
                }
            } else {
                Column(Modifier.fillMaxWidth()) {
                    notifications.forEach { notification ->
                        Surface(
                            Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            shape = RoundedCornerShape(14.dp),
                            color = Color.White.copy(alpha = .06f)
                        ) {
                            Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.Top) {
                                Column(Modifier.weight(1f)) {
                                    Row(Modifier.fillMaxWidth()) {
                                        Text(notification.title, color = Color.White, fontSize = 14.sp, modifier = Modifier.weight(1f))
                                        Text(notification.timeLabel, color = Color.White.copy(alpha = .4f), fontSize = 10.sp)
                                    }
                                    Spacer(Modifier.height(4.dp))
                                    Text(notification.message, color = Color.White.copy(alpha = .62f), fontSize = 11.sp)
                                }
                                IconButton(onClick = { onDismissNotification(notification.id) }, modifier = Modifier.size(30.dp)) {
                                    Icon(Icons.Default.Close, "Dismiss", tint = Color.White.copy(alpha = .45f), modifier = Modifier.size(16.dp))
                                }
                            }
                        }
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
