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
import androidx.compose.ui.input.key.onKeyEvent
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
    var output by remember { mutableStateOf("PC-DARKI Terminal v0.3\nAndroid shell backend ready.\nType help for built-ins.\n\n") }
    var running by remember { mutableStateOf(false) }
    var history by remember { mutableStateOf(listOf<String>()) }
    var historyIndex by remember { mutableIntStateOf(-1) }
    val scroll = rememberScrollState()
    val scope = rememberCoroutineScope()

    fun runCommand(raw: String) {
        val command = raw.trim()
        if (command.isEmpty() || running) return
        history = (history + command).takeLast(50)
        historyIndex = -1
        input = TextFieldValue("")
        if (command == "clear") { output = ""; return }
        output += "$ $command\n"
        when (command) {
            "help" -> output += "Built-ins: help, clear, version\nShell: pwd, ls, date, id, uname, getprop, echo, cd\nCommands run as the PC-DARKI Android app user.\n\n"
            "version" -> output += "PC-DARKI Terminal v0.3\n\n"
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
        Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(scroll)) {
            Text(output, color = Color(0xFFE6E6E6), fontSize = 13.sp, lineHeight = 19.sp)
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("$ ", color = Color(0xFFB69CFF), fontSize = 14.sp)
            OutlinedTextField(
                value = input,
                onValueChange = { input = it; historyIndex = -1 },
                modifier = Modifier.weight(1f).onKeyEvent { event ->
                    if (event.key == Key.Enter) { runCommand(input.text); true } else false
                },
                singleLine = true,
                placeholder = { Text(if (running) "running…" else "command") },
                enabled = !running
            )
            Spacer(Modifier.width(6.dp))
            Button(onClick = { runCommand(input.text) }, enabled = !running) { Text(if (running) "…" else "Run") }
        }
    }
}

private fun executeShell(command: String): String {
    if (command.length > 4096) return "Command too long (maximum 4096 characters)."
    return try {
        val process = ProcessBuilder("/system/bin/sh", "-c", command)
            .redirectErrorStream(true)
            .start()
        val finished = withTimeoutOrNull(10_000) {
            process.inputStream.bufferedReader().use { it.readText() }
            process.waitFor()
            true
        } ?: false
        if (!finished) {
            process.destroyForcibly()
            "Process timed out after 10 seconds."
        } else {
            val text = process.inputStream.bufferedReader().use { it.readText() }
            if (text.isBlank()) "(exit ${process.exitValue()})" else text.trimEnd()
        }
    } catch (e: Exception) {
        "Terminal error: ${e.message ?: "unknown error"}"
    }
}
