package com.nurislam.pcdarki

import android.graphics.Bitmap
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView

@Composable
fun PCDarkiBrowser() {
    val context = LocalContext.current
    var address by remember { mutableStateOf("https://www.google.com") }
    var webView by remember { mutableStateOf<WebView?>(null) }
    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = Color.White,
        unfocusedTextColor = Color.White,
        focusedBorderColor = Color(0xFFB69CFF),
        unfocusedBorderColor = Color.White.copy(alpha = .35f),
        focusedLabelColor = Color(0xFFB69CFF),
        unfocusedLabelColor = Color.White.copy(alpha = .65f),
        cursorColor = Color(0xFFB69CFF)
    )

    Column(Modifier.fillMaxSize().padding(8.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            OutlinedTextField(
                value = address,
                onValueChange = { address = it },
                modifier = Modifier.weight(1f),
                singleLine = true,
                label = { Text("Address") },
                colors = fieldColors
            )
            Spacer(Modifier.width(6.dp))
            Button(onClick = { loadAddress(webView, address) }) { Text("Go") }
        }
        Spacer(Modifier.width(1.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Button(onClick = { webView?.goBack() }, enabled = webView?.canGoBack() == true) { Text("Back") }
            Button(onClick = { webView?.goForward() }, enabled = webView?.canGoForward() == true) { Text("Forward") }
            Button(onClick = { webView?.reload() }) { Text("Refresh") }
        }
        Spacer(Modifier.width(4.dp))
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = {
                WebView(context).apply {
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.loadsImagesAutomatically = true
                    webViewClient = object : WebViewClient() {
                        override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean = false
                        override fun onPageStarted(view: WebView, url: String?, favicon: Bitmap?) {
                            if (!url.isNullOrBlank()) address = url
                        }
                        override fun onPageFinished(view: WebView, url: String?) {
                            if (!url.isNullOrBlank()) address = url
                        }
                    }
                    loadUrl("https://www.google.com")
                    webView = this
                }
            },
            update = { webView = it }
        )
    }
}

private fun loadAddress(webView: WebView?, raw: String) {
    val value = raw.trim()
    if (value.isEmpty()) return
    val url = if (value.startsWith("http://") || value.startsWith("https://")) value else "https://www.google.com/search?q=${android.net.Uri.encode(value)}"
    webView?.loadUrl(url)
}
