package com.nurislam.pcdarki

import androidx.compose.ui.Modifier

internal fun Modifier.pcdarkiClickable(onClick: () -> Unit): Modifier = androidx.compose.foundation.clickable(onClick = onClick)
