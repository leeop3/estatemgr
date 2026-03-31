package com.estate.manager.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColors = darkColorScheme(
    primary         = Color(0xFF4CAF50),   // estate green
    onPrimary       = Color.Black,
    primaryContainer= Color(0xFF1B5E20),
    secondary       = Color(0xFFFFA726),   // harvest amber
    tertiary        = Color(0xFF29B6F6),   // sky
    background      = Color(0xFF121212),
    surface         = Color(0xFF1E1E1E),
    error           = Color(0xFFEF5350)
)

@Composable
fun EstateTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColors,
        content     = content
    )
}
