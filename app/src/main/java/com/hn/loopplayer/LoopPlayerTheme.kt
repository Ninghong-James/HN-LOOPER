package com.hn.loopplayer

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val colors = darkColorScheme(
    primary = Color(0xFF9EF0C4),
    onPrimary = Color(0xFF063721),
    primaryContainer = Color(0xFF23563C),
    onPrimaryContainer = Color(0xFFD3FBE1),
    secondaryContainer = Color(0xFF243A3C),
    onSecondaryContainer = Color(0xFFD2F5F0),
    background = Color(0xFF10131B),
    onBackground = Color(0xFFF1F4F4),
    surface = Color(0xFF171D25),
    surfaceContainer = Color(0xFF1B232B),
    surfaceContainerHigh = Color(0xFF242D36),
    onSurface = Color(0xFFF1F4F4),
    onSurfaceVariant = Color(0xFFB8C3C5),
    errorContainer = Color(0xFF5E2425),
    onErrorContainer = Color(0xFFFFDAD6),
)

@Composable
fun LoopPlayerTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = colors, content = content)
}
