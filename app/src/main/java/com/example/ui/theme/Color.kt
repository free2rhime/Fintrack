package com.example.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// =============================================================================
// FinTrack Design System v3 — Color Foundation Tokens
// Direction: Material 3 Expressive — Family Finance Experience
// Vibrant, approachable, high-contrast, WCAG AA/AAA compliant dual-layer architecture
// =============================================================================

// -----------------------------------------------------------------------------
// 1. LIGHT MODE FOUNDATIONS (Material 3 Expressive Tonal Palette)
// -----------------------------------------------------------------------------
val PageBackgroundLight = Color(0xFFF8F9FA)       // Canvas Background (Screen foundation)
val SurfacePrimaryLight = Color(0xFFFFFFFF)       // Pure White (Baseline surface)
val SurfaceSecondaryLight = Color(0xFFF1F5F9)     // Slate 100 (Secondary surface)
val SurfaceElevatedLight = Color(0xFFE2E5E9)      // Elevated surfaces (Modals, dropdown menus)
val SurfaceSelectedLight = Color(0xFFEFF6FF)      // Blue 50 (Selected states)

// Material 3 Expressive Tonal Hierarchy (Light Mode)
val SurfaceContainerLowestLight = Color(0xFFFFFFFF)  // Pure White
val SurfaceContainerLowLight = Color(0xFFF1F3F5)     // Hero canvas, bleed header
val SurfaceContainerLight = Color(0xFFEBEDF0)        // Grouped containers, list panels
val SurfaceContainerHighLight = Color(0xFFE2E5E9)     // Elevated surfaces, search bars, sheets
val SurfaceContainerHighestLight = Color(0xFFD9DDE2)  // Input wells, inactive toggles
val SurfaceVariantLight = Color(0xFFE2E5E9)

val BorderSubtleLight = Color(0xFFE2E8F0)         // Slate 200 (1dp structural micro-border / outlineVariant)
val BorderStandardLight = Color(0xFFCBD5E1)       // Slate 300 (Input fields, active dividers / outline)
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
// 2. DARK MODE FOUNDATIONS (Material 3 Expressive Deep Tonal Palette)
// -----------------------------------------------------------------------------
val PageBackgroundDark = Color(0xFF0E1116)        // Canvas Background (Page canvas)
val SurfacePrimaryDark = Color(0xFF131B2E)        // Deep Obsidian (Primary card surfaces)
val SurfaceSecondaryDark = Color(0xFF1E293B)      // Slate 800 (Secondary surface)
val SurfaceElevatedDark = Color(0xFF232936)       // Elevated surfaces (Modals, dropdown menus)
val SurfaceSelectedDark = Color(0xFF1E3A8A)       // Blue 900 (Selected states)

// Material 3 Expressive Tonal Hierarchy (Dark Mode)
val SurfaceContainerLowestDark = Color(0xFF090C10)   // Deepest well
val SurfaceContainerLowDark = Color(0xFF151921)      // Hero canvas, bleed header
val SurfaceContainerDark = Color(0xFF1C212B)         // Grouped containers, list panels
val SurfaceContainerHighDark = Color(0xFF232936)     // Elevated surfaces, search bars, sheets
val SurfaceContainerHighestDark = Color(0xFF2C3342)  // Input wells, inactive toggles
val SurfaceVariantDark = Color(0xFF232936)

val BorderSubtleDark = Color(0xFF1E293B)          // Slate 800 (1dp structural micro-border / outlineVariant)
val BorderStandardDark = Color(0xFF334155)        // Slate 700 (Input fields, active dividers / outline)
val BorderActiveDark = Color(0xFF60A5FA)          // Cobalt Light (Focus, active selections)

val TextPrimaryDark = Color(0xFFF8FAFC)           // Slate 50 (16.4:1 contrast on obsidian)
val TextSecondaryDark = Color(0xFF94A3B8)         // Slate 400 (6.7:1 contrast on obsidian)
val TextMutedDark = Color(0xFF64748B)             // Slate 500 (3.6:1 contrast on obsidian)

// Semantic Financial Indicators (Dark Mode)
val IncomeDark = Color(0xFF10B981)                // Emerald 500
val IncomeContainerDark = Color(0x2610B981)       // Emerald 15% alpha
val ExpenseDark = Color(0xFFEF4444)               // Red 500 (Coral)
val ExpenseContainerDark = Color(0x26EF4444)      // Red 15% alpha

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
    val isDark: Boolean,
    // --- Material 3 Expressive Surface Hierarchy ---
    val background: Color = pageBackground,
    val onBackground: Color = textPrimary,
    val surface: Color = surfacePrimary,
    val onSurface: Color = textPrimary,
    val surfaceContainerLowest: Color = surfacePrimary,
    val surfaceContainerLow: Color = surfaceHero,
    val surfaceContainer: Color = surfaceSecondary,
    val surfaceContainerHigh: Color = surfaceElevated,
    val surfaceContainerHighest: Color = borderSubtle,
    val surfaceVariant: Color = surfaceSecondary,
    val onSurfaceVariant: Color = textSecondary,
    // --- Material 3 Key Accent Roles ---
    val primary: Color = brandPrimary,
    val onPrimary: Color = if (isDark) Color(0xFF0F172A) else Color.White,
    val primaryContainer: Color = if (isDark) Color(0xFF1E3A8A) else Color(0xFFDBEAFE),
    val onPrimaryContainer: Color = if (isDark) Color(0xFFDBEAFE) else Color(0xFF1E3A8A),
    val secondary: Color = brandSecondary,
    val onSecondary: Color = if (isDark) Color(0xFF0F172A) else Color.White,
    val secondaryContainer: Color = if (isDark) Color(0xFF0369A1) else Color(0xFFE0F2FE),
    val onSecondaryContainer: Color = if (isDark) Color(0xFFE0F2FE) else Color(0xFF0369A1),
    val tertiary: Color = Color(0xFF7C3AED),
    val onTertiary: Color = Color.White,
    val tertiaryContainer: Color = if (isDark) Color(0xFF5B21B6) else Color(0xFFEDE9FE),
    val onTertiaryContainer: Color = if (isDark) Color(0xFFEDE9FE) else Color(0xFF5B21B6),
    // --- Material 3 Error & Structural Roles ---
    val error: Color = expense,
    val onError: Color = Color.White,
    val errorContainer: Color = expenseContainer,
    val onErrorContainer: Color = if (isDark) Color(0xFFFECACA) else Color(0xFF991B1B),
    val outline: Color = borderStandard,
    val outlineVariant: Color = borderSubtle,
    val scrim: Color = Color.Black,
    // --- Semantic Financial Indicators ---
    val onIncome: Color = if (isDark) Color(0xFF064E3B) else Color.White,
    val onIncomeContainer: Color = if (isDark) Color(0xFFA7F3D0) else Color(0xFF065F46),
    val onExpense: Color = Color.White,
    val onExpenseContainer: Color = if (isDark) Color(0xFFFECACA) else Color(0xFF991B1B),
    val warning: Color = healthWarning,
    val warningContainer: Color = if (isDark) Color(0x26F59E0B) else Color(0xFFFEF3C7),
    val onWarning: Color = if (isDark) Color(0xFF451A03) else Color.White,
    val onWarningContainer: Color = if (isDark) Color(0xFFFDE68A) else Color(0xFF92400E),
    val success: Color = healthPositive,
    val successContainer: Color = incomeContainer,
    val onSuccess: Color = onIncome,
    val onSuccessContainer: Color = onIncomeContainer,
    val info: Color = brandAccent,
    val infoContainer: Color = if (isDark) Color(0x2660A5FA) else Color(0xFFEFF6FF),
    val onInfo: Color = Color.White,
    val onInfoContainer: Color = if (isDark) Color(0xFFBFDBFE) else Color(0xFF1D4ED8)
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
    isDark = false,
    background = PageBackgroundLight,
    onBackground = TextPrimaryLight,
    surface = SurfacePrimaryLight,
    onSurface = TextPrimaryLight,
    surfaceContainerLowest = SurfaceContainerLowestLight,
    surfaceContainerLow = SurfaceContainerLowLight,
    surfaceContainer = SurfaceContainerLight,
    surfaceContainerHigh = SurfaceContainerHighLight,
    surfaceContainerHighest = SurfaceContainerHighestLight,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = TextSecondaryLight,
    primary = BrandPrimaryLight,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDBEAFE),
    onPrimaryContainer = Color(0xFF1E3A8A),
    secondary = BrandSecondaryLight,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE0F2FE),
    onSecondaryContainer = Color(0xFF0369A1),
    tertiary = Color(0xFF7C3AED),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFEDE9FE),
    onTertiaryContainer = Color(0xFF5B21B6),
    error = ExpenseLight,
    onError = Color.White,
    errorContainer = ExpenseContainerLight,
    onErrorContainer = Color(0xFF991B1B),
    outline = BorderStandardLight,
    outlineVariant = BorderSubtleLight,
    scrim = Color(0xFF000000),
    onIncome = Color.White,
    onIncomeContainer = Color(0xFF065F46),
    onExpense = Color.White,
    onExpenseContainer = Color(0xFF991B1B),
    warning = HealthWarningLight,
    warningContainer = Color(0xFFFEF3C7),
    onWarning = Color.White,
    onWarningContainer = Color(0xFF92400E),
    success = HealthPositiveLight,
    successContainer = IncomeContainerLight,
    onSuccess = Color.White,
    onSuccessContainer = Color(0xFF065F46),
    info = BrandAccentLight,
    infoContainer = Color(0xFFEFF6FF),
    onInfo = Color.White,
    onInfoContainer = Color(0xFF1D4ED8)
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
    isDark = true,
    background = PageBackgroundDark,
    onBackground = TextPrimaryDark,
    surface = SurfacePrimaryDark,
    onSurface = TextPrimaryDark,
    surfaceContainerLowest = SurfaceContainerLowestDark,
    surfaceContainerLow = SurfaceContainerLowDark,
    surfaceContainer = SurfaceContainerDark,
    surfaceContainerHigh = SurfaceContainerHighDark,
    surfaceContainerHighest = SurfaceContainerHighestDark,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = TextSecondaryDark,
    primary = BrandPrimaryDark,
    onPrimary = Color(0xFF0F172A),
    primaryContainer = Color(0xFF1E3A8A),
    onPrimaryContainer = Color(0xFFDBEAFE),
    secondary = BrandSecondaryDark,
    onSecondary = Color(0xFF0F172A),
    secondaryContainer = Color(0xFF0369A1),
    onSecondaryContainer = Color(0xFFE0F2FE),
    tertiary = Color(0xFFA78BFA),
    onTertiary = Color(0xFF1E1B4B),
    tertiaryContainer = Color(0xFF5B21B6),
    onTertiaryContainer = Color(0xFFEDE9FE),
    error = ExpenseDark,
    onError = Color.White,
    errorContainer = ExpenseContainerDark,
    onErrorContainer = Color(0xFFFECACA),
    outline = BorderStandardDark,
    outlineVariant = BorderSubtleDark,
    scrim = Color(0xFF000000),
    onIncome = Color(0xFF064E3B),
    onIncomeContainer = Color(0xFFA7F3D0),
    onExpense = Color.White,
    onExpenseContainer = Color(0xFFFECACA),
    warning = HealthWarningDark,
    warningContainer = Color(0x26F59E0B),
    onWarning = Color(0xFF451A03),
    onWarningContainer = Color(0xFFFDE68A),
    success = HealthPositiveDark,
    successContainer = IncomeContainerDark,
    onSuccess = Color(0xFF064E3B),
    onSuccessContainer = Color(0xFFA7F3D0),
    info = BrandAccentDark,
    infoContainer = Color(0x2660A5FA),
    onInfo = Color(0xFF172554),
    onInfoContainer = Color(0xFFBFDBFE)
)

val LocalFinTrackColors = staticCompositionLocalOf { LightFinTrackColors }

// -----------------------------------------------------------------------------
// 5. BACKWARD-COMPATIBILITY TOKENS & ALIASES
// (Preserves existing screens, components and tests without breaking changes)
// -----------------------------------------------------------------------------
val CanvasDark = PageBackgroundDark
val SurfaceDark = SurfacePrimaryDark

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
val OutlineLight = BorderStandardLight
val OutlineVariantLight = BorderSubtleLight

val BackgroundDark = PageBackgroundDark

val StatusGreen = IncomeEmerald
val StatusOrange = WarningAmber
val StatusRed = ExpenseCoral



