package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = HubNavyLight,
    onPrimary = Color.White,
    primaryContainer = HubNavyDark,
    onPrimaryContainer = Color.White,
    secondary = HubTealLight,
    onSecondary = Color.White,
    secondaryContainer = HubTeal,
    onSecondaryContainer = Color.White,
    tertiary = HubAmber,
    background = Color(0xFF0F172A),
    surface = Color(0xFF1E293B),
    onBackground = Color(0xFFF8FAFC),
    onSurface = Color(0xFFF8FAFC),
    outline = Color(0xFF334155),
    surfaceVariant = Color(0xFF1E293B)
)

private val LightColorScheme = lightColorScheme(
    primary = HubNavy,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE2E8F0),
    onPrimaryContainer = HubNavy,
    secondary = HubTeal,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE6FFFA),
    onSecondaryContainer = HubTeal,
    tertiary = HubAmber,
    background = SurfaceLight,
    surface = CardBackground,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    outline = DividerColor,
    surfaceVariant = Color(0xFFF1F5F9)
)

@Composable
fun MarkazAmaliTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        content = content
    )
}
