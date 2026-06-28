package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// Standard base colors
val DarkBackground = Color(0xFF0C0E14)
val DarkSurface = Color(0xFF161922)
val GlassBg = Color(0x33FFFFFF)
val GlassBorder = Color(0x22FFFFFF)
val WatermarkColor = Color(0x66FFFFFF)

// Theme Colors
enum class PlayerTheme(
    val displayName: String,
    val primary: Color,
    val primaryContainer: Color,
    val secondary: Color,
    val background: Color,
    val surface: Color
) {
    NEON_BLUE(
        displayName = "Neon Blue",
        primary = Color(0xFF00E5FF),
        primaryContainer = Color(0xFF00363D),
        secondary = Color(0xFF80DEEA),
        background = Color(0xFF080C14),
        surface = Color(0xFF101726)
    ),
    CYBERPUNK_PINK(
        displayName = "Cyberpunk Pink",
        primary = Color(0xFFFF007F),
        primaryContainer = Color(0xFF3F001C),
        secondary = Color(0xFFFF80BF),
        background = Color(0xFF0F0814),
        surface = Color(0xFF1E1026)
    ),
    SUNSET_ORANGE(
        displayName = "Sunset Orange",
        primary = Color(0xFFFF6D00),
        primaryContainer = Color(0xFF4E1D00),
        secondary = Color(0xFFFFAB40),
        background = Color(0xFF140D08),
        surface = Color(0xFF261910)
    ),
    EMERALD_GREEN(
        displayName = "Emerald Green",
        primary = Color(0xFF00E676),
        primaryContainer = Color(0xFF003816),
        secondary = Color(0xFFB9F6CA),
        background = Color(0xFF08140E),
        surface = Color(0xFF10261C)
    ),
    ROYAL_GOLD(
        displayName = "Royal Gold",
        primary = Color(0xFFFFD600),
        primaryContainer = Color(0xFF3F3500),
        secondary = Color(0xFFFFE57F),
        background = Color(0xFF12120C),
        surface = Color(0xFF232318)
    ),
    DEEP_VIOLET(
        displayName = "Deep Violet",
        primary = Color(0xFF7C4DFF),
        primaryContainer = Color(0xFF220066),
        secondary = Color(0xFFB47CFF),
        background = Color(0xFF0D0A16),
        surface = Color(0xFF1A142D)
    )
}
