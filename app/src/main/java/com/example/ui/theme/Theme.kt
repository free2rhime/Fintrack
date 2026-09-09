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
// FinTrack Design System v2 — Material 3 Dark Palette
// ==========================================
private val DarkColorScheme = darkColorScheme(
    primary = IncomeDark,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF064E3B),
    onPrimaryContainer = Color(0xFF6EE7B7),
    inversePrimary = Color(0xFF059669),
    secondary = CobaltBlue,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF1D4ED8),
    onSecondaryContainer = Color.White,
    tertiary = TertiaryViolet,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFF6D28D9),
    onTertiaryContainer = Color.White,
    background = PageBackgroundDark,
    onBackground = TextPrimaryDark,
    surface = SurfacePrimaryDark,
    onSurface = TextPrimaryDark,
    surfaceVariant = SurfaceSecondaryDark,
    onSurfaceVariant = TextSecondaryDark,
    surfaceTint = IncomeDark,
    inverseSurface = Color(0xFFF8FAFC),
    inverseOnSurface = Color(0xFF0F172A),
    error = ExpenseDark,
    onError = Color.White,
    errorContainer = ExpenseContainerDark,
    onErrorContainer = ExpenseDark,
    outline = BorderStandardDark,
    outlineVariant = BorderSubtleDark,
    scrim = Color(0xFF000000),
    surfaceBright = Color(0xFF1E293B),
    surfaceDim = PageBackgroundDark,
    surfaceContainerLowest = PageBackgroundDark,
    surfaceContainerLow = Color(0xFF0E1424),
    surfaceContainer = SurfacePrimaryDark,
    surfaceContainerHigh = SurfaceSecondaryDark,
    surfaceContainerHighest = Color(0xFF27354A)
)

// ==========================================
// FinTrack Design System v2 — Material 3 Light Palette
// ==========================================
private val LightColorScheme = lightColorScheme(
    primary = IncomeLight,
    onPrimary = Color.White,
    primaryContainer = IncomeContainerLight,
    onPrimaryContainer = Color(0xFF064E3B),
    inversePrimary = Color(0xFF10B981),
    secondary = CobaltBlue,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFDBEAFE),
    onSecondaryContainer = Color(0xFF1E40AF),
    tertiary = TertiaryViolet,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFEDE9FE),
    onTertiaryContainer = Color(0xFF581C87),
    background = PageBackgroundLight,
    onBackground = TextPrimaryLight,
    surface = SurfacePrimaryLight,
    onSurface = TextPrimaryLight,
    surfaceVariant = SurfaceSecondaryLight,
    onSurfaceVariant = TextSecondaryLight,
    surfaceTint = IncomeLight,
    inverseSurface = Color(0xFF1E293B),
    inverseOnSurface = Color(0xFFF8FAFC),
    error = ExpenseLight,
    onError = Color.White,
    errorContainer = ExpenseContainerLight,
    onErrorContainer = Color(0xFF991B1B),
    outline = BorderStandardLight,
    outlineVariant = BorderSubtleLight,
    scrim = Color(0xFF000000),
    surfaceBright = SurfacePrimaryLight,
    surfaceDim = SurfaceSecondaryLight,
    surfaceContainerLowest = SurfacePrimaryLight,
    surfaceContainerLow = PageBackgroundLight,
    surfaceContainer = SurfacePrimaryLight,
    surfaceContainerHigh = SurfaceSecondaryLight,
    surfaceContainerHighest = BorderSubtleLight
)

/**
 * Centralized Design System accessor for FinTrack v2.
 * Provides unified, single-source-of-truth access to colors, typography, shapes, spacing, and motion.
 */
object FinTrackTheme {
    val colors: FinTrackColors
        @Composable
        get() = LocalFinTrackColors.current

    val typography: androidx.compose.material3.Typography
        @Composable
        get() = MaterialTheme.typography

    val shapes: androidx.compose.material3.Shapes
        @Composable
        get() = MaterialTheme.shapes

    val spacing: FinTrackSpacing
        @Composable
        get() = LocalFinTrackSpacing.current

    val motion: FinTrackMotionTokens
        @Composable
        get() = LocalFinTrackMotion.current
}

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

    val finTrackColors = if (darkTheme) DarkFinTrackColors else LightFinTrackColors

    CompositionLocalProvider(
        LocalFinTrackColors provides finTrackColors,
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

