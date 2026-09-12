package com.nurislam.pcdarki

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

@Composable
fun PCDarkiTerminal() {
    var input by remember { mutableStateOf(TextFieldValue("")) }
    var output by remember { mutableStateOf("PC-DARKI Terminal v0.4\nAndroid shell backend ready.\nType help for built-ins.\nType ani to launch ani-cli in Termux.\n\n") }
    var running by remember { mutableStateOf(false) }
    val scroll = rememberScrollState()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = Color.White, unfocusedTextColor = Color.White, disabledTextColor = Color.White.copy(alpha = .5f),
        focusedPlaceholderColor = Color.White.copy(alpha = .5f), unfocusedPlaceholderColor = Color.White.copy(alpha = .5f),
        focusedBorderColor = Color(0xFFB69CFF), unfocusedBorderColor = Color.White.copy(alpha = .35f),
        focusedLabelColor = Color(0xFFB69CFF), unfocusedLabelColor = Color.White.copy(alpha = .65f), cursorColor = Color(0xFFB69CFF)
    )

    fun runCommand(raw: String) {
        val command = raw.trim()
        if (command.isEmpty() || running) return
        input = TextFieldValue("")
        if (command == "clear") { output = ""; return }
        output += "$ $command\n"
        when (command) {
            "help" -> output += "Built-ins: help, clear, version, ani\nShell: pwd, ls, date, id, uname, getprop, echo, cd\nani = ani-cli --dub -q best in Termux.\n\n"
            "version" -> output += "PC-DARKI Terminal v0.4\n\n"
            "ani" -> {
                val result = runAniInTermux(context)
                output += if (result.isSuccess) "Launching ani-cli in Termux…\n\n" else "Could not launch Termux ani-cli. Enable RUN_COMMAND permission and allow-external-apps in Termux.\n\n"
            }
            else -> {
                running = true
                scope.launch {
                    val result = withContext(Dispatchers.IO) { executeShell(command) }
                    output += result + "\n\n"
                    running = false
                }
            }
        }
    }

    Column(Modifier.fillMaxSize().background(Color(0xFF090B10)).padding(10.dp)) {
        Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(scroll)) { Text(output, color = Color(0xFFE6E6E6), fontSize = 13.sp, lineHeight = 19.sp) }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("$ ", color = Color(0xFFB69CFF), fontSize = 14.sp)
            OutlinedTextField(value = input, onValueChange = { input = it }, modifier = Modifier.weight(1f).onKeyEvent { event -> if (event.type == KeyEventType.KeyUp && event.key == Key.Enter) { runCommand(input.text); true } else false }, singleLine = true, placeholder = { Text(if (running) "running…" else "command") }, enabled = !running, textStyle = LocalTextStyle.current.copy(color = Color.White), colors = fieldColors)
            Spacer(Modifier.width(6.dp)); Button(onClick = { runCommand(input.text) }, enabled = !running) { Text(if (running) "…" else "Run") }
        }
    }
}

private fun executeShell(command: String): String {
    if (command.length > 4096) return "Command too long (maximum 4096 characters)."
    return try {
        val process = ProcessBuilder("/system/bin/sh", "-c", command).redirectErrorStream(true).start()
        val result = runCatching { kotlinx.coroutines.runBlocking { withTimeoutOrNull(10_000) { val text = process.inputStream.bufferedReader().use { it.readText() }; val exitCode = process.waitFor(); if (text.isBlank()) "(exit $exitCode)" else text.trimEnd() } } }.getOrNull()
        result ?: run { process.destroyForcibly(); "Process timed out after 10 seconds." }
    } catch (e: Exception) { "Terminal error: ${e.message ?: "unknown error"}" }
}
