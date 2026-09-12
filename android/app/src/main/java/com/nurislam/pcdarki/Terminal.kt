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
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun PCDarkiTerminal() {
    var input by remember { mutableStateOf(TextFieldValue("")) }
    var output by remember { mutableStateOf("PC-DARKI Terminal v0.2\nAndroid shell backend ready.\nType help for built-ins.\n\n") }
    var running by remember { mutableStateOf(false) }
    val scroll = rememberScrollState()
    val scope = rememberCoroutineScope()

    fun runCommand(raw: String) {
        val command = raw.trim()
        if (command.isEmpty() || running) return
        input = TextFieldValue("")
        output += "$ $command\n"
        if (command == "clear") {
            output = ""
            return
        }
        if (command == "help") {
            output += "Built-ins: help, clear, version\nShell examples: pwd, ls, date, id, uname, getprop\nCommands run as the PC-DARKI Android app user.\n\n"
            return
        }
        if (command == "version") {
            output += "PC-DARKI Terminal v0.2\n\n"
            return
        }
        running = true
        scope.launch {
            val result = withContext(Dispatchers.IO) { executeShell(command) }
            output += result + "\n\n"
            running = false
        }
    }

    Column(Modifier.fillMaxSize().background(Color(0xFF090B10)).padding(10.dp)) {
        Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(scroll)) {
            Text(output, color = Color(0xFFE6E6E6), fontSize = 13.sp, lineHeight = 19.sp)
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("$ ", color = Color(0xFFB69CFF), fontSize = 14.sp)
            OutlinedTextField(value = input, onValueChange = { input = it }, modifier = Modifier.weight(1f), singleLine = true, placeholder = { Text(if (running) "running…" else "command") }, enabled = !running)
            Spacer(Modifier.width(6.dp))
            Button(onClick = { runCommand(input.text) }, enabled = !running) { Text(if (running) "…" else "Run") }
        }
    }
}

private fun executeShell(command: String): String {
    return try {
        val process = ProcessBuilder("/system/bin/sh", "-c", command)
            .redirectErrorStream(true)
            .start()
        val text = process.inputStream.bufferedReader().use { it.readText() }
        process.waitFor()
        if (text.isBlank()) "(exit ${process.exitValue()})" else text.trimEnd()
    } catch (e: Exception) {
        "Terminal error: ${e.message ?: "unknown error"}"
    }
}
