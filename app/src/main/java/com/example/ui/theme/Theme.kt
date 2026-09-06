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
    inversePrimary = PrimaryGreenLight,
    secondary = CobaltBlue,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF1D4ED8),
    onSecondaryContainer = Color.White,
    tertiary = TertiaryViolet,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFF6D28D9),
    onTertiaryContainer = Color.White,
    background = CanvasDark,
    onBackground = TextPrimary,
    surface = SurfaceDark,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceContainerHighDark,
    onSurfaceVariant = TextSecondary,
    surfaceTint = IncomeEmerald,
    inverseSurface = Color(0xFFF8FAFC),
    inverseOnSurface = Color(0xFF0F172A),
    error = ExpenseCoral,
    onError = Color.White,
    errorContainer = ExpenseContainer,
    onErrorContainer = ExpenseCoral,
    outline = Color(0xFF475569),
    outlineVariant = Color(0xFF334155),
    scrim = Color(0xFF000000),
    surfaceBright = Color(0xFF334155),
    surfaceDim = CanvasDark,
    surfaceContainerLowest = CanvasDark,
    surfaceContainerLow = Color(0xFF162032),
    surfaceContainer = SurfaceContainerDark,
    surfaceContainerHigh = SurfaceContainerHighDark,
    surfaceContainerHighest = Color(0xFF3B4D66)
)

// ==========================================
// FinTrack Design System v1 — Material 3 Light Palette
// ==========================================
private val LightColorScheme = lightColorScheme(
    primary = IncomeEmerald,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDCFCE7),
    onPrimaryContainer = Color(0xFF14532D),
    inversePrimary = Color(0xFF16A34A),
    secondary = CobaltBlue,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFDBEAFE),
    onSecondaryContainer = Color(0xFF1E40AF),
    tertiary = TertiaryViolet,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFEDE9FE),
    onTertiaryContainer = Color(0xFF581C87),
    background = BackgroundLight,
    onBackground = TextPrimaryLight,
    surface = SurfaceLight,
    onSurface = TextPrimaryLight,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = TextSecondaryLight,
    surfaceTint = IncomeEmerald,
    inverseSurface = Color(0xFF1E293B),
    inverseOnSurface = Color(0xFFF8FAFC),
    error = ExpenseCoral,
    onError = Color.White,
    errorContainer = Color(0xFFFFE4E6),
    onErrorContainer = Color(0xFF9F1239),
    outline = OutlineLight,
    outlineVariant = OutlineVariantLight,
    scrim = Color(0xFF000000),
    surfaceBright = SurfaceLight,
    surfaceDim = SurfaceContainerHighLight,
    surfaceContainerLowest = SurfaceContainerLowestLight,
    surfaceContainerLow = SurfaceContainerLowLight,
    surfaceContainer = SurfaceContainerLight,
    surfaceContainerHigh = SurfaceContainerHighLight,
    surfaceContainerHighest = SurfaceContainerHighestLight
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

