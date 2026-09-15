package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = EmeraldPrimary,
    onPrimary = Color(0xFF003824),
    primaryContainer = EmeraldSecondary,
    onPrimaryContainer = Color(0xFF9DF2D0),
    secondary = ArcaneGold,
    onSecondary = Color(0xFF452B00),
    secondaryContainer = ArcaneAmber,
    onSecondaryContainer = Color(0xFFFFDF9E),
    tertiary = TwilightCyan,
    onTertiary = Color(0xFF00363D),
    background = DeepDarkBackground,
    onBackground = ForestOnSurface,
    surface = DarkSurface,
    onSurface = ForestOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = ForestOnSurfaceVariant,
    error = Color(0xFFEF4444),
    onError = Color.White
)

private val LightColorScheme = darkColorScheme(
    // Keep dark fantasy scholar theme for consistent immersion
    primary = EmeraldPrimary,
    onPrimary = Color(0xFF003824),
    secondary = ArcaneGold,
    tertiary = TwilightCyan,
    background = DeepDarkBackground,
    surface = DarkSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurface = ForestOnSurface,
    onSurfaceVariant = ForestOnSurfaceVariant
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Default to rich dark forest scholar immersion
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = DarkColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
