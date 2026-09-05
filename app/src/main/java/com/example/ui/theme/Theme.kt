package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

// ==========================================
// FinTrack Design System v1 — Material 3 Dark Palette
// ==========================================
private val DarkColorScheme = darkColorScheme(
    primary = IncomeEmerald,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF15803D),
    onPrimaryContainer = Color.White,
    secondary = CobaltBlue,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF1D4ED8),
    onSecondaryContainer = Color.White,
    tertiary = TertiaryViolet,
    background = CanvasDark,
    onBackground = TextPrimary,
    surface = SurfaceDark,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceContainerHighDark,
    onSurfaceVariant = TextSecondary,
    surfaceContainerLowest = CanvasDark,
    surfaceContainer = SurfaceContainerDark,
    surfaceContainerHigh = SurfaceContainerHighDark,
    error = ExpenseCoral,
    onError = Color.White,
    errorContainer = ExpenseContainer,
    onErrorContainer = ExpenseCoral,
    outline = Color(0xFF475569)
)

private val LightColorScheme = lightColorScheme(
    primary = IncomeEmerald,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDCFCE7),
    onPrimaryContainer = Color(0xFF14532D),
    secondary = CobaltBlue,
    onSecondary = Color.White,
    tertiary = TertiaryViolet,
    background = BackgroundLight,
    onBackground = TextPrimaryLight,
    surface = SurfaceLight,
    onSurface = TextPrimaryLight,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = TextSecondaryLight,
    error = ExpenseCoral,
    onError = Color.White,
    outline = Color(0xFFCBD5E1)
)

@Composable
fun FinTrackTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Set false to maintain consistent brand design
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    CompositionLocalProvider(
        LocalFinTrackSpacing provides FinTrackSpacing(),
        LocalFinTrackMotion provides FinTrackMotionTokens()
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            shapes = Shapes,
            content = content
        )
    }
}

