package com.nurislam.pcdarki

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage

@Composable
fun PCDarkiImageViewer() {
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var status by remember { mutableStateOf("Choose an image to view") }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            imageUri = uri
            status = uri.lastPathSegment?.substringAfterLast('/')?.substringAfterLast(':') ?: "Image"
        }
    }

    Column(Modifier.fillMaxSize().background(Color(0xFF0A0D14))) {
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Image, null, tint = Color.White)
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text("Image Viewer", color = Color.White, fontSize = 18.sp)
                Text(status, color = Color.White.copy(alpha = .55f), fontSize = 11.sp)
            }
            Button(onClick = { launcher.launch(arrayOf("image/*")) }) {
                Icon(Icons.Default.FolderOpen, null)
                Spacer(Modifier.width(6.dp))
                Text("Open")
            }
        }
        HorizontalDivider()
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            val uri = imageUri
            if (uri == null) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Image, null, Modifier.size(72.dp), tint = Color.White.copy(alpha = .35f))
                    Spacer(Modifier.height(12.dp))
                    Text("No image selected", color = Color.White.copy(alpha = .7f))
                }
            } else {
                AsyncImage(
                    model = uri,
                    contentDescription = status,
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    contentScale = ContentScale.Fit
                )
            }
        }
    }
}
