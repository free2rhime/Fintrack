package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// =============================================================================
// FinTrack Design System v2 — Typography System
// Direction: Precision Fintech + Editorial Wealth Narrative
// Strict tabular numerals ("tnum") for all financial figures.
// =============================================================================

// -----------------------------------------------------------------------------
// 1. FINANCIAL NUMERIC DISPLAY SCALE (Strict Tabular Figures)
// -----------------------------------------------------------------------------

/**
 * Apex Financial Display: Net worth, total balance, major hero anchors.
 */
val HeroFinancialDisplay = TextStyle(
    fontFamily = FontFamily.Default,
    fontWeight = FontWeight.Bold,
    fontSize = 32.sp,
    lineHeight = 40.sp,
    letterSpacing = (-0.5).sp,
    fontFeatureSettings = "tnum"
)

/**
 * Compact Financial Display: Hero balance on narrow viewports (<380dp) or dense cards.
 */
val HeroFinancialDisplayCompact = TextStyle(
    fontFamily = FontFamily.Default,
    fontWeight = FontWeight.Bold,
    fontSize = 26.sp,
    lineHeight = 32.sp,
    letterSpacing = (-0.3).sp,
    fontFeatureSettings = "tnum"
)

/**
 * Medium Financial Metric: Secondary hero metrics (Income/Expense totals, major KPIs).
 */
val MetricFinancialMedium = TextStyle(
    fontFamily = FontFamily.Default,
    fontWeight = FontWeight.SemiBold,
    fontSize = 20.sp,
    lineHeight = 26.sp,
    letterSpacing = (-0.2).sp,
    fontFeatureSettings = "tnum"
)

/**
 * Regular Financial Metric: Transaction item amounts, category breakdown values.
 */
val MetricFinancialRegular = TextStyle(
    fontFamily = FontFamily.Default,
    fontWeight = FontWeight.SemiBold,
    fontSize = 15.sp,
    lineHeight = 20.sp,
    letterSpacing = 0.sp,
    fontFeatureSettings = "tnum"
)

/**
 * Decoupled Currency Unit: Sits adjacent or stacked to financial figures.
 */
val CurrencyUnitDecoupled = TextStyle(
    fontFamily = FontFamily.Default,
    fontWeight = FontWeight.Medium,
    fontSize = 14.sp,
    lineHeight = 18.sp,
    letterSpacing = 0.5.sp
)

// -----------------------------------------------------------------------------
// 2. UI & EDITORIAL DISPLAY SCALE
// -----------------------------------------------------------------------------

/**
 * Screen Title: Clean primary top-level screen anchor.
 */
val TitleScreen = TextStyle(
    fontFamily = FontFamily.Default,
    fontWeight = FontWeight.SemiBold,
    fontSize = 22.sp,
    lineHeight = 28.sp,
    letterSpacing = 0.sp
)

/**
 * Card Title: Section headings and card headers.
 */
val TitleCard = TextStyle(
    fontFamily = FontFamily.Default,
    fontWeight = FontWeight.SemiBold,
    fontSize = 16.sp,
    lineHeight = 22.sp,
    letterSpacing = 0.sp
)

/**
 * Narrative Body: Insights, explanations, descriptions, narrative pulse.
 */
val BodyNarrative = TextStyle(
    fontFamily = FontFamily.Default,
    fontWeight = FontWeight.Normal,
    fontSize = 14.sp,
    lineHeight = 20.sp,
    letterSpacing = 0.sp
)

/**
 * Badge & Chip Label: Interactive pills, status badges, filter tags.
 */
val LabelBadge = TextStyle(
    fontFamily = FontFamily.Default,
    fontWeight = FontWeight.SemiBold,
    fontSize = 12.sp,
    lineHeight = 16.sp,
    letterSpacing = 0.sp
)

/**
 * Micro Metadata: Chart axes, timestamps, conversion rate notes, tiny tags.
 */
val LabelMicro = TextStyle(
    fontFamily = FontFamily.Default,
    fontWeight = FontWeight.Medium,
    fontSize = 10.sp,
    lineHeight = 14.sp,
    letterSpacing = 0.4.sp
)

// -----------------------------------------------------------------------------
// 3. BACKWARD-COMPATIBILITY ALIASES
// (Ensures zero compilation breaks for existing screens and tests)
// -----------------------------------------------------------------------------
val SectionHeadline = TitleScreen

val CardTitleAmount = TextStyle(
    fontFamily = FontFamily.Default,
    fontWeight = FontWeight.SemiBold,
    fontSize = 16.sp,
    lineHeight = 22.sp,
    letterSpacing = 0.sp,
    fontFeatureSettings = "tnum"
)

val BodyRegular = BodyNarrative
val LabelBadgeMedium = LabelBadge
val MicroMetadata = LabelMicro

// -----------------------------------------------------------------------------
// 4. MATERIAL 3 TYPOGRAPHY SCALE MAPPING
// -----------------------------------------------------------------------------
val Typography = Typography(
    displayLarge = HeroFinancialDisplay,
    displayMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 36.sp,
        letterSpacing = 0.sp,
        fontFeatureSettings = "tnum"
    ),
    displaySmall = HeroFinancialDisplayCompact,
    headlineLarge = TitleScreen,
    headlineMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 28.sp
    ),
    headlineSmall = MetricFinancialMedium,
    titleLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp,
        lineHeight = 24.sp
    ),
    titleMedium = TitleCard,
    titleSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.15.sp
    ),
    bodyMedium = BodyNarrative,
    bodySmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    labelMedium = LabelBadge,
    labelSmall = LabelMicro
)

// -----------------------------------------------------------------------------
// 5. SEMANTIC TYPOGRAPHY CONTRACT (FinTrackTypography)
// -----------------------------------------------------------------------------
@androidx.compose.runtime.Immutable
data class FinTrackTypography(
    val screenTitle: TextStyle = TitleScreen,
    val sectionHeading: TextStyle = TitleCard,
    val cardHeading: TextStyle = TitleCard,
    val body: TextStyle = BodyNarrative,
    val bodyMedium: TextStyle = BodyNarrative,
    val bodySmall: TextStyle = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp
    ),
    val supportingText: TextStyle = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.sp
    ),
    val label: TextStyle = LabelBadge,
    val labelSmall: TextStyle = LabelMicro,
    val metadata: TextStyle = LabelMicro,
    val financialDisplay: TextStyle = HeroFinancialDisplay,
    val financialDisplayCompact: TextStyle = HeroFinancialDisplayCompact,
    val financialMetric: TextStyle = MetricFinancialMedium,
    val financialMetricRegular: TextStyle = MetricFinancialRegular,
    val financialExpressive: TextStyle = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Black,
        fontSize = 36.sp,
        lineHeight = 44.sp,
        letterSpacing = (-0.8).sp,
        fontFeatureSettings = "tnum"
    ),
    val currencyUnit: TextStyle = CurrencyUnitDecoupled
)

val LocalFinTrackTypography = androidx.compose.runtime.staticCompositionLocalOf { FinTrackTypography() }



