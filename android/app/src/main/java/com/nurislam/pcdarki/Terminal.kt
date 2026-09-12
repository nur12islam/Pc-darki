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

@Composable
fun PCDarkiTerminal() {
    var input by remember { mutableStateOf(TextFieldValue("")) }
    var output by remember { mutableStateOf("PC-DARKI Terminal v0.1\nType help for commands.\n\n") }
    val scroll = rememberScrollState()

    fun runCommand(command: String) {
        val value = command.trim()
        if (value.isEmpty()) return
        val result = when (value) {
            "help" -> "Available commands:\nhelp - show this help\nclear - clear terminal\npwd - show working directory\nwhoami - show app user\nversion - show terminal version\necho TEXT - print text"
            "clear" -> ""
            "pwd" -> "/data/data/com.nurislam.pcdarki"
            "whoami" -> "pc-darki"
            "version" -> "PC-DARKI Terminal v0.1"
            else -> if (value.startsWith("echo ")) value.removePrefix("echo ") else "Command not available in this native Android terminal."
        }
        output = if (value == "clear") "" else output + "$ $value\n$result\n\n"
        input = TextFieldValue("")
    }

    Column(Modifier.fillMaxSize().background(Color(0xFF090B10)).padding(10.dp)) {
        Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(scroll)) {
            Text(output, color = Color(0xFFE6E6E6), fontSize = 13.sp, lineHeight = 19.sp)
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("$ ", color = Color(0xFFB69CFF), fontSize = 14.sp)
            OutlinedTextField(value = input, onValueChange = { input = it }, modifier = Modifier.weight(1f), singleLine = true, placeholder = { Text("command") })
            Spacer(Modifier.width(6.dp))
            Button(onClick = { runCommand(input.text) }) { Text("Run") }
        }
    }
}
