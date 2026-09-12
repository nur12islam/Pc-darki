package com.nurislam.pcdarki

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

private data class AppEntry(val label: String, val packageName: String, val system: Boolean)

@Composable
fun PCDarkiAppManager() {
    val context = LocalContext.current
    var apps by remember { mutableStateOf(loadApps(context)) }
    var showSystem by remember { mutableStateOf(false) }
    val visibleApps = apps.filter { showSystem || !it.system }

    Column(Modifier.fillMaxSize().background(Color(0xFF0A0D14))) {
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Apps, null, tint = Color.White, modifier = Modifier.size(28.dp))
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text("App Manager", color = Color.White, fontSize = 18.sp)
                Text("${visibleApps.size} apps shown", color = Color.White.copy(alpha = .55f), fontSize = 11.sp)
            }
            TextButton(onClick = { apps = loadApps(context) }) { Text("Refresh") }
            FilterChip(selected = showSystem, onClick = { showSystem = !showSystem }, label = { Text("System") })
        }
        HorizontalDivider()
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            items(visibleApps, key = { it.packageName }) { app ->
                AppRow(app) {
                    val intent = context.packageManager.getLaunchIntentForPackage(app.packageName)
                    if (intent != null) context.startActivity(intent)
                    else context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("package:${app.packageName}")))
                }
            }
        }
    }
}

@Composable
private fun AppRow(app: AppEntry, onOpen: () -> Unit) {
    Surface(color = Color.White.copy(alpha = .06f), shape = MaterialTheme.shapes.medium, onClick = onOpen) {
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Info, null, tint = Color.White.copy(alpha = .75f), modifier = Modifier.size(30.dp))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(app.label, color = Color.White, fontSize = 15.sp)
                Text(app.packageName, color = Color.White.copy(alpha = .5f), fontSize = 11.sp)
            }
            if (app.system) Text("SYSTEM", color = Color.White.copy(alpha = .45f), fontSize = 9.sp)
        }
    }
}

private fun loadApps(context: android.content.Context): List<AppEntry> {
    return context.packageManager.getInstalledApplications(0)
        .map { info ->
            val label = context.packageManager.getApplicationLabel(info).toString()
            AppEntry(label, info.packageName, (info.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM) != 0)
        }
        .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.label })
}
