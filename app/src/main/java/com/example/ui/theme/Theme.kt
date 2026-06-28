package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

@Composable
fun VideoPlayerTheme(
    playerTheme: PlayerTheme = PlayerTheme.NEON_BLUE,
    content: @Composable () -> Unit
) {
    val colorScheme = darkColorScheme(
        primary = playerTheme.primary,
        onPrimary = Color.Black,
        primaryContainer = playerTheme.primaryContainer,
        onPrimaryContainer = Color.White,
        secondary = playerTheme.secondary,
        onSecondary = Color.Black,
        background = playerTheme.background,
        onBackground = Color.White,
        surface = playerTheme.surface,
        onSurface = Color.White,
        surfaceVariant = playerTheme.surface.copy(alpha = 0.8f),
        onSurfaceVariant = Color.White,
        outline = playerTheme.primary.copy(alpha = 0.5f)
    )

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
