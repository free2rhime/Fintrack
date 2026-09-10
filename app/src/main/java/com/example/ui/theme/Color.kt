package com.example.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// =============================================================================
// FinTrack Design System v2 — Color Foundation Tokens
// Direction: Precision Fintech + Editorial Wealth Narrative
// WCAG AA/AAA Compliant Dual-Layer Architecture
// =============================================================================

// -----------------------------------------------------------------------------
// 1. LIGHT MODE FOUNDATIONS (Clean High-Contrast Palette)
// -----------------------------------------------------------------------------
val PageBackgroundLight = Color(0xFFF8FAFC)       // Slate 50 (Page canvas)
val SurfacePrimaryLight = Color(0xFFFFFFFF)       // Pure White (Primary card surfaces)
val SurfaceSecondaryLight = Color(0xFFF1F5F9)     // Slate 100 (Nested wells, insets, filter chips)
val SurfaceElevatedLight = Color(0xFFFFFFFF)      // Pure White (Modals, dropdown menus)
val SurfaceSelectedLight = Color(0xFFEFF6FF)      // Blue 50 (Selected states)

val BorderSubtleLight = Color(0xFFE2E8F0)         // Slate 200 (1dp structural micro-border)
val BorderStandardLight = Color(0xFFCBD5E1)       // Slate 300 (Input fields, active dividers)
val BorderActiveLight = Color(0xFF2563EB)         // Cobalt Core (Focus, active selections)

val TextPrimaryLight = Color(0xFF0F172A)          // Slate 900 (17.9:1 contrast on white)
val TextSecondaryLight = Color(0xFF475569)        // Slate 600 (7.5:1 contrast on white)
val TextMutedLight = Color(0xFF64748B)            // Slate 500 (4.8:1 contrast on white)

// Semantic Financial Indicators (Light Mode)
val IncomeLight = Color(0xFF059669)               // Emerald 600
val IncomeContainerLight = Color(0xFFDCFCE7)      // Emerald 100
val ExpenseLight = Color(0xFFDC2626)              // Red 600 (5.5:1 contrast on white)
val ExpenseContainerLight = Color(0xFFFFE4E6)     // Rose 100

// Financial Health & Metric Indicators (Light Mode)
val HealthPositiveLight = Color(0xFF0D9488)       // Teal 600
val HealthWarningLight = Color(0xFFD97706)        // Amber 600
val HealthCriticalLight = Color(0xFFE11D48)       // Rose 600
val HealthNeutralLight = Color(0xFF475569)        // Slate 600

// -----------------------------------------------------------------------------
// 2. DARK MODE FOUNDATIONS (Deep Obsidian Palette)
// -----------------------------------------------------------------------------
val PageBackgroundDark = Color(0xFF0B0F19)        // Obsidian Black (Page canvas)
val SurfacePrimaryDark = Color(0xFF131B2E)        // Deep Obsidian (Primary card surfaces)
val SurfaceSecondaryDark = Color(0xFF1E293B)      // Slate 800 (Nested wells, insets)
val SurfaceElevatedDark = Color(0xFF1E293B)       // Slate 800 (Modals, dropdown menus)
val SurfaceSelectedDark = Color(0xFF1E3A8A)       // Blue 900 (Selected states)

val BorderSubtleDark = Color(0xFF1E293B)          // Slate 800 (1dp structural micro-border)
val BorderStandardDark = Color(0xFF334155)        // Slate 700 (Input fields, active dividers)
val BorderActiveDark = Color(0xFF60A5FA)          // Cobalt Light (Focus, active selections)

val TextPrimaryDark = Color(0xFFF8FAFC)           // Slate 50 (16.4:1 contrast on obsidian)
val TextSecondaryDark = Color(0xFF94A3B8)         // Slate 400 (6.7:1 contrast on obsidian)
val TextMutedDark = Color(0xFF64748B)             // Slate 500 (3.6:1 contrast on obsidian)

// Semantic Financial Indicators (Dark Mode)
val IncomeDark = Color(0xFF10B981)                // Emerald 500
val IncomeContainerDark = Color(0x2610B981)       // Emerald 15% alpha
val ExpenseDark = Color(0xFFF87171)               // Red 400 (6.1:1 contrast on obsidian)
val ExpenseContainerDark = Color(0x26F87171)      // Red 15% alpha

// Financial Health & Metric Indicators (Dark Mode)
val HealthPositiveDark = Color(0xFF2DD4BF)        // Teal 400
val HealthWarningDark = Color(0xFFFBBF24)         // Amber 400
val HealthCriticalDark = Color(0xFFFB7185)        // Rose 400
val HealthNeutralDark = Color(0xFF94A3B8)         // Slate 400

// -----------------------------------------------------------------------------
// 3. UNIVERSAL SURFACES & BRAND TOKENS
// -----------------------------------------------------------------------------
// SurfaceHero: Adaptive Financial Apex Hero Card surface
val SurfaceHeroLight = Color(0xFFFFFFFF)          // Crisp Luminous White (Hero surface in light mode)
val SurfaceHeroBorderLight = Color(0xFFE2E8F0)    // Slate 200 subtle border for hero card in light mode
val SurfaceHeroDarkMidnight = Color(0xFF0F172A)   // Dark Midnight (#0F172A) (Hero surface in dark mode)
val SurfaceHeroBorder = Color(0xFF334155)         // Slate 700 subtle border for hero card in dark mode

val BrandPrimaryLight = Color(0xFF1E3A8A)         // Deep Sapphire
val BrandPrimaryDark = Color(0xFF3B82F6)          // Vivid Cobalt
val BrandSecondaryLight = Color(0xFF0284C7)       // Sky Slate
val BrandSecondaryDark = Color(0xFF38BDF8)        // Sky Bright
val BrandAccentLight = Color(0xFF2563EB)          // Cobalt Core
val BrandAccentDark = Color(0xFF60A5FA)           // Cobalt Soft

// -----------------------------------------------------------------------------
// 4. EXTENDED SEMANTIC COLOR CONTRACT (FinTrackColors)
// -----------------------------------------------------------------------------
@Immutable
data class FinTrackColors(
    val pageBackground: Color,
    val surfacePrimary: Color,
    val surfaceSecondary: Color,
    val surfaceHero: Color,
    val surfaceHeroBorder: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val surfaceElevated: Color,
    val surfaceSelected: Color,
    val borderSubtle: Color,
    val borderStandard: Color,
    val borderActive: Color,
    val income: Color,
    val incomeContainer: Color,
    val expense: Color,
    val expenseContainer: Color,
    val healthPositive: Color,
    val healthWarning: Color,
    val healthCritical: Color,
    val healthNeutral: Color,
    val brandPrimary: Color,
    val brandSecondary: Color,
    val brandAccent: Color,
    val chartLineIncome: Color,
    val chartFillIncome: Color,
    val chartLineExpense: Color,
    val chartFillExpense: Color,
    val chartGrid: Color,
    val chartIndicatorLine: Color,
    val isDark: Boolean
)

val LightFinTrackColors = FinTrackColors(
    pageBackground = PageBackgroundLight,
    surfacePrimary = SurfacePrimaryLight,
    surfaceSecondary = SurfaceSecondaryLight,
    surfaceHero = SurfaceHeroLight,
    surfaceHeroBorder = SurfaceHeroBorderLight,
    textPrimary = TextPrimaryLight,
    textSecondary = TextSecondaryLight,
    textMuted = TextMutedLight,
    surfaceElevated = SurfaceElevatedLight,
    surfaceSelected = SurfaceSelectedLight,
    borderSubtle = BorderSubtleLight,
    borderStandard = BorderStandardLight,
    borderActive = BorderActiveLight,
    income = IncomeLight,
    incomeContainer = IncomeContainerLight,
    expense = ExpenseLight,
    expenseContainer = ExpenseContainerLight,
    healthPositive = HealthPositiveLight,
    healthWarning = HealthWarningLight,
    healthCritical = HealthCriticalLight,
    healthNeutral = HealthNeutralLight,
    brandPrimary = BrandPrimaryLight,
    brandSecondary = BrandSecondaryLight,
    brandAccent = BrandAccentLight,
    chartLineIncome = IncomeLight,
    chartFillIncome = IncomeLight.copy(alpha = 0.12f),
    chartLineExpense = ExpenseLight,
    chartFillExpense = ExpenseLight.copy(alpha = 0.12f),
    chartGrid = BorderSubtleLight,
    chartIndicatorLine = TextMutedLight,
    isDark = false
)

val DarkFinTrackColors = FinTrackColors(
    pageBackground = PageBackgroundDark,
    surfacePrimary = SurfacePrimaryDark,
    surfaceSecondary = SurfaceSecondaryDark,
    surfaceHero = SurfaceHeroDarkMidnight,
    surfaceHeroBorder = SurfaceHeroBorder,
    textPrimary = TextPrimaryDark,
    textSecondary = TextSecondaryDark,
    textMuted = TextMutedDark,
    surfaceElevated = SurfaceElevatedDark,
    surfaceSelected = SurfaceSelectedDark,
    borderSubtle = BorderSubtleDark,
    borderStandard = BorderStandardDark,
    borderActive = BorderActiveDark,
    income = IncomeDark,
    incomeContainer = IncomeContainerDark,
    expense = ExpenseDark,
    expenseContainer = ExpenseContainerDark,
    healthPositive = HealthPositiveDark,
    healthWarning = HealthWarningDark,
    healthCritical = HealthCriticalDark,
    healthNeutral = HealthNeutralDark,
    brandPrimary = BrandPrimaryDark,
    brandSecondary = BrandSecondaryDark,
    brandAccent = BrandAccentDark,
    chartLineIncome = IncomeDark,
    chartFillIncome = IncomeDark.copy(alpha = 0.15f),
    chartLineExpense = ExpenseDark,
    chartFillExpense = ExpenseDark.copy(alpha = 0.15f),
    chartGrid = BorderSubtleDark,
    chartIndicatorLine = TextSecondaryDark,
    isDark = true
)

val LocalFinTrackColors = staticCompositionLocalOf { LightFinTrackColors }

// -----------------------------------------------------------------------------
// 5. BACKWARD-COMPATIBILITY TOKENS & ALIASES
// (Preserves existing screens, components and tests without breaking changes)
// -----------------------------------------------------------------------------
val CanvasDark = PageBackgroundDark
val SurfaceDark = SurfacePrimaryDark
val SurfaceContainerDark = SurfaceSecondaryDark
val SurfaceContainerHighDark = Color(0xFF334155)

val IncomeEmerald = IncomeDark
val IncomeContainer = IncomeContainerDark
val ExpenseCoral = ExpenseDark
val ExpenseContainer = ExpenseContainerDark

val CobaltBlue = Color(0xFF3B82F6)
val WarningAmber = Color(0xFFF59E0B)

val TextPrimary = TextPrimaryDark
val TextSecondary = TextSecondaryDark
val TextMuted = TextMutedDark

val PrimaryGreen = IncomeEmerald
val PrimaryGreenDark = Color(0xFF16A34A)
val PrimaryGreenContainer = Color(0xFFDCFCE7)
val PrimaryGreenLight = Color(0xFF86EFAC)

val SecondaryBlue = CobaltBlue
val SecondaryBlueDark = Color(0xFF2563EB)

val TertiaryViolet = Color(0xFF8B5CF6)

val IncomeGreen = IncomeEmerald
val ExpenseRed = ExpenseCoral

val BackgroundLight = PageBackgroundLight
val SurfaceLight = SurfacePrimaryLight
val SurfaceContainerLowestLight = SurfacePrimaryLight
val SurfaceContainerLowLight = PageBackgroundLight
val SurfaceContainerLight = SurfaceSecondaryLight
val SurfaceContainerHighLight = Color(0xFFE2E8F0)
val SurfaceContainerHighestLight = Color(0xFFCBD5E1)
val SurfaceVariantLight = SurfaceSecondaryLight
val OutlineLight = BorderStandardLight
val OutlineVariantLight = BorderSubtleLight

val BackgroundDark = PageBackgroundDark
val SurfaceVariantDark = SurfaceContainerHighDark

val StatusGreen = IncomeEmerald
val StatusOrange = WarningAmber
val StatusRed = ExpenseCoral



