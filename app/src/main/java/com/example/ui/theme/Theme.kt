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

private val CosmicDarkColorScheme = darkColorScheme(
    primary = CosmicPrimary,
    onPrimary = Color.White,
    secondary = CosmicSecondary,
    onSecondary = Color.White,
    background = CosmicBackground,
    onBackground = Color(0xFFF1F5F9), // slate-100
    surface = CosmicSurface,
    onSurface = Color(0xFFF1F5F9),     // slate-100
    surfaceVariant = CosmicSurfaceVariant,
    onSurfaceVariant = Color(0xFF94A3B8), // slate-400
    outline = CosmicBorder,
    error = CosmicAccentCritical,
    onError = Color.White
)

private val CosmicLightColorScheme = lightColorScheme(
    primary = CosmicPrimary,
    onPrimary = Color.White,
    secondary = CosmicSecondary,
    onSecondary = Color.White,
    background = Color(0xFFF8FAFC), // slate-50
    onBackground = Color(0xFF0F172A), // slate-900
    surface = Color.White,
    onSurface = Color(0xFF0F172A), // slate-900
    surfaceVariant = Color(0xFFE2E8F0), // slate-200
    onSurfaceVariant = Color(0xFF475569), // slate-600
    outline = Color(0xFFCBD5E1), // slate-300
    error = CosmicAccentCritical,
    onError = Color.White
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false, // Disable dynamic colors to keep design system consistent
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) CosmicDarkColorScheme else CosmicLightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
