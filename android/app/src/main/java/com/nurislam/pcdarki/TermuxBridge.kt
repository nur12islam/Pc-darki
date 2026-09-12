package com.nurislam.pcdarki

import android.content.Context
import android.content.Intent
import android.os.Build

/** Launches ani-cli in the user's Termux environment when Termux external commands are enabled. */
fun runAniInTermux(context: Context): Result<Unit> = runCatching {
    val intent = Intent().apply {
        setClassName("com.termux", "com.termux.app.RunCommandService")
        action = "com.termux.RUN_COMMAND"
        putExtra("com.termux.RUN_COMMAND_PATH", "/data/data/com.termux/files/usr/bin/bash")
        putExtra("com.termux.RUN_COMMAND_ARGUMENTS", arrayOf("-lc", "ani-cli --dub -q best"))
        putExtra("com.termux.RUN_COMMAND_WORKDIR", "/data/data/com.termux/files/home")
        putExtra("com.termux.RUN_COMMAND_BACKGROUND", false)
        putExtra("com.termux.RUN_COMMAND_SESSION_ACTION", "0")
        putExtra("com.termux.RUN_COMMAND_LABEL", "PC-DARKI Anime")
        putExtra("com.termux.RUN_COMMAND_DESCRIPTION", "Launch ani-cli from PC-DARKI")
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) context.startForegroundService(intent) else context.startService(intent)
}
