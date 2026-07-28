package org.carlospinan.bloqueador.app.adaptive

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp

enum class WindowSizeClass {
    Compact,
    Medium,
    Expanded,
}

@Composable
fun rememberWindowSizeClass(): WindowSizeClass {
    var sizeClass by remember { mutableStateOf(WindowSizeClass.Compact) }
    BoxWithConstraints {
        sizeClass = when {
            maxWidth >= 840.dp -> WindowSizeClass.Expanded
            maxWidth >= 600.dp -> WindowSizeClass.Medium
            else -> WindowSizeClass.Compact
        }
    }
    return sizeClass
}
