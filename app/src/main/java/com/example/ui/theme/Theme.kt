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
// FinTrack Design System v3 — Material 3 Expressive Dark Palette
// ==========================================
private val DarkColorScheme = darkColorScheme(
    primary = BrandPrimaryDark,
    onPrimary = Color(0xFF0F172A),
    primaryContainer = Color(0xFF1E3A8A),
    onPrimaryContainer = Color(0xFFDBEAFE),
    inversePrimary = BrandPrimaryLight,
    secondary = BrandSecondaryDark,
    onSecondary = Color(0xFF0F172A),
    secondaryContainer = Color(0xFF0369A1),
    onSecondaryContainer = Color(0xFFE0F2FE),
    tertiary = Color(0xFFA78BFA),
    onTertiary = Color(0xFF1E1B4B),
    tertiaryContainer = Color(0xFF5B21B6),
    onTertiaryContainer = Color(0xFFEDE9FE),
    background = PageBackgroundDark,
    onBackground = TextPrimaryDark,
    surface = SurfacePrimaryDark,
    onSurface = TextPrimaryDark,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = TextSecondaryDark,
    surfaceTint = BrandPrimaryDark,
    inverseSurface = Color(0xFFF8FAFC),
    inverseOnSurface = Color(0xFF0F172A),
    error = ExpenseDark,
    onError = Color.White,
    errorContainer = ExpenseContainerDark,
    onErrorContainer = Color(0xFFFECACA),
    outline = BorderStandardDark,
    outlineVariant = BorderSubtleDark,
    scrim = Color(0xFF000000),
    surfaceBright = SurfaceContainerHighDark,
    surfaceDim = PageBackgroundDark,
    surfaceContainerLowest = SurfaceContainerLowestDark,
    surfaceContainerLow = SurfaceContainerLowDark,
    surfaceContainer = SurfaceContainerDark,
    surfaceContainerHigh = SurfaceContainerHighDark,
    surfaceContainerHighest = SurfaceContainerHighestDark
)

// ==========================================
// FinTrack Design System v3 — Material 3 Expressive Light Palette
// ==========================================
private val LightColorScheme = lightColorScheme(
    primary = BrandPrimaryLight,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDBEAFE),
    onPrimaryContainer = Color(0xFF1E3A8A),
    inversePrimary = BrandPrimaryDark,
    secondary = BrandSecondaryLight,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE0F2FE),
    onSecondaryContainer = Color(0xFF0369A1),
    tertiary = Color(0xFF7C3AED),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFEDE9FE),
    onTertiaryContainer = Color(0xFF5B21B6),
    background = PageBackgroundLight,
    onBackground = TextPrimaryLight,
    surface = SurfacePrimaryLight,
    onSurface = TextPrimaryLight,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = TextSecondaryLight,
    surfaceTint = BrandPrimaryLight,
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
    surfaceDim = SurfaceContainerLowLight,
    surfaceContainerLowest = SurfaceContainerLowestLight,
    surfaceContainerLow = SurfaceContainerLowLight,
    surfaceContainer = SurfaceContainerLight,
    surfaceContainerHigh = SurfaceContainerHighLight,
    surfaceContainerHighest = SurfaceContainerHighestLight
)

/**
 * Centralized Design System accessor for FinTrack v3 Material 3 Expressive.
 * Provides unified, single-source-of-truth access to colors, typography, shapes, spacing, and motion.
 */
object FinTrackTheme {
    val colors: FinTrackColors
        @Composable
        get() = LocalFinTrackColors.current

    val typography: androidx.compose.material3.Typography
        @Composable
        get() = MaterialTheme.typography

    val typographyTokens: FinTrackTypography
        @Composable
        get() = LocalFinTrackTypography.current

    val shapes: FinTrackShapes
        @Composable
        get() = LocalFinTrackShapes.current

    val materialShapes: androidx.compose.material3.Shapes
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
        LocalFinTrackShapes provides FinTrackShapes(),
        LocalFinTrackTypography provides FinTrackTypography(),
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

