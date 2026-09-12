package com.nurislam.pcdarki

import androidx.compose.foundation.clickable
import androidx.compose.ui.Modifier

internal fun Modifier.pcdarkiClickable(onClick: () -> Unit): Modifier = clickable(onClick = onClick)
