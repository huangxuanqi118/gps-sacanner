package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val InstrumentDarkColorScheme = darkColorScheme(
    primary = SignalEmerald,
    onPrimary = Color(0xFF04261D),
    primaryContainer = SignalEmeraldContainer,
    onPrimaryContainer = SignalEmerald,
    secondary = SignalCyan,
    onSecondary = Color(0xFF052436),
    secondaryContainer = SignalCyanContainer,
    onSecondaryContainer = SignalCyan,
    tertiary = SignalAmber,
    onTertiary = Color(0xFF2D1D02),
    tertiaryContainer = SignalAmberContainer,
    onTertiaryContainer = SignalAmber,
    error = SignalCoral,
    onError = Color(0xFF3E0810),
    errorContainer = SignalCoralContainer,
    onErrorContainer = SignalCoral,
    background = SignalBgDeep,
    onBackground = SignalTextPrimary,
    surface = SignalSurface,
    onSurface = SignalTextPrimary,
    surfaceVariant = SignalSurfaceElevated,
    onSurfaceVariant = SignalTextSecondary,
    surfaceContainerHigh = SignalSurfaceElevated,
    surfaceContainerHighest = SignalSurfaceHighlight,
    outline = SignalOutline
)

@Composable
fun GpsDetectorTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = InstrumentDarkColorScheme,
        typography = AppTypography,
        content = content
    )
}
