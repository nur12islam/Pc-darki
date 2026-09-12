package com.nurislam.pcdarki

import androidx.compose.ui.Modifier

internal fun Modifier.clickable(onClick: () -> Unit): Modifier = androidx.compose.foundation.clickable(onClick = onClick)
