package com.example

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.BodyNarrative
import com.example.ui.theme.BorderStandardDark
import com.example.ui.theme.BorderStandardLight
import com.example.ui.theme.BorderSubtleDark
import com.example.ui.theme.BorderSubtleLight
import com.example.ui.theme.BrandAccentLight
import com.example.ui.theme.BrandPrimaryLight
import com.example.ui.theme.BreakpointCompactWidth
import com.example.ui.theme.BreakpointTabletWidth
import com.example.ui.theme.CardTitleAmount
import com.example.ui.theme.CurrencyUnitDecoupled
import com.example.ui.theme.DarkFinTrackColors
import com.example.ui.theme.ExpenseDark
import com.example.ui.theme.ExpenseLight
import com.example.ui.theme.FinTrackSpacing
import com.example.ui.theme.HeroFinancialDisplay
import com.example.ui.theme.HeroFinancialDisplayCompact
import com.example.ui.theme.IncomeDark
import com.example.ui.theme.IncomeLight
import com.example.ui.theme.LabelBadge
import com.example.ui.theme.LabelMicro
import com.example.ui.theme.LightFinTrackColors
import com.example.ui.theme.MaxContentWidthTablet
import com.example.ui.theme.MetricFinancialMedium
import com.example.ui.theme.MetricFinancialRegular
import com.example.ui.theme.MinTouchTargetSize
import com.example.ui.theme.PaddingScreenCompact
import com.example.ui.theme.PaddingScreenStandard
import com.example.ui.theme.PageBackgroundDark
import com.example.ui.theme.PageBackgroundLight
import com.example.ui.theme.RadiusCard
import com.example.ui.theme.RadiusHero
import com.example.ui.theme.RadiusInset
import com.example.ui.theme.RadiusMicro
import com.example.ui.theme.RadiusPill
import com.example.ui.theme.Space12
import com.example.ui.theme.Space16
import com.example.ui.theme.Space2
import com.example.ui.theme.Space20
import com.example.ui.theme.Space24
import com.example.ui.theme.Space32
import com.example.ui.theme.Space4
import com.example.ui.theme.Space40
import com.example.ui.theme.Space48
import com.example.ui.theme.Space8
import com.example.ui.theme.SurfaceHeroDarkMidnight
import com.example.ui.theme.SurfacePrimaryDark
import com.example.ui.theme.SurfacePrimaryLight
import com.example.ui.theme.SurfaceSecondaryDark
import com.example.ui.theme.SurfaceSecondaryLight
import com.example.ui.theme.TextMutedDark
import com.example.ui.theme.TextMutedLight
import com.example.ui.theme.TextPrimaryDark
import com.example.ui.theme.TextPrimaryLight
import com.example.ui.theme.TextSecondaryDark
import com.example.ui.theme.TextSecondaryLight
import com.example.ui.theme.TitleCard
import com.example.ui.theme.TitleScreen
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.pow

/**
 * Verification test suite for FinTrack Design System v2 Foundation (Checkpoint 2).
 * Verifies color contrast ratios (WCAG AA/AAA), typography scales and tabular numerals,
 * progressive geometry tokens, spacing grid alignment, and theme data contracts.
 */
class DesignSystemV2FoundationTest {

    // =========================================================================
    // WCAG 2.1 Relative Luminance & Contrast Ratio Formulas
    // =========================================================================

    private fun componentLuminance(channel: Float): Double {
        return if (channel <= 0.04045f) {
            channel.toDouble() / 12.92
        } else {
            ((channel.toDouble() + 0.055) / 1.055).pow(2.4)
        }
    }

    private fun relativeLuminance(color: Color): Double {
        val r = componentLuminance(color.red)
        val g = componentLuminance(color.green)
        val b = componentLuminance(color.blue)
        return 0.2126 * r + 0.7152 * g + 0.0722 * b
    }

    private fun contrastRatio(c1: Color, c2: Color): Double {
        val l1 = relativeLuminance(c1)
        val l2 = relativeLuminance(c2)
        val lighter = maxOf(l1, l2)
        val darker = minOf(l1, l2)
        return (lighter + 0.05) / (darker + 0.05)
    }

    // =========================================================================
    // 1. TYPOGRAPHY TESTS (Tabular Numerals & Editorial Hierarchy)
    // =========================================================================

    @Test
    fun test_typography_tabularNumerals_configuredOnFinancialStyles() {
        assertEquals("tnum", HeroFinancialDisplay.fontFeatureSettings)
        assertEquals("tnum", HeroFinancialDisplayCompact.fontFeatureSettings)
        assertEquals("tnum", MetricFinancialMedium.fontFeatureSettings)
        assertEquals("tnum", MetricFinancialRegular.fontFeatureSettings)
        assertEquals("tnum", CardTitleAmount.fontFeatureSettings)
    }

    @Test
    fun test_typography_financialScale_hierarchy() {
        assertTrue(HeroFinancialDisplay.fontSize.value > HeroFinancialDisplayCompact.fontSize.value)
        assertTrue(HeroFinancialDisplayCompact.fontSize.value > MetricFinancialMedium.fontSize.value)
        assertTrue(MetricFinancialMedium.fontSize.value > MetricFinancialRegular.fontSize.value)
        assertTrue(MetricFinancialRegular.fontSize.value >= 15.sp.value)
        assertEquals(14.sp, CurrencyUnitDecoupled.fontSize)
    }

    @Test
    fun test_typography_uiEditorialScale_hierarchy() {
        assertTrue(TitleScreen.fontSize.value > TitleCard.fontSize.value)
        assertTrue(TitleCard.fontSize.value > BodyNarrative.fontSize.value)
        assertTrue(BodyNarrative.fontSize.value > LabelBadge.fontSize.value)
        assertTrue(LabelBadge.fontSize.value > LabelMicro.fontSize.value)
    }

    // =========================================================================
    // 2. SHAPE TESTS (Progressive Geometry Scale)
    // =========================================================================

    @Test
    fun test_progressiveGeometry_scaleHierarchy() {
        assertTrue(RadiusMicro < RadiusInset)
        assertTrue(RadiusInset < RadiusCard)
        assertTrue(RadiusCard < RadiusHero)
        assertTrue(RadiusHero < RadiusPill)

        assertEquals(6.dp, RadiusMicro)
        assertEquals(10.dp, RadiusInset)
        assertEquals(16.dp, RadiusCard)
        assertEquals(24.dp, RadiusHero)
        assertEquals(999.dp, RadiusPill)
    }

    @Test
    fun test_progressiveGeometry_nestingFormula() {
        // Radius_inner ≈ Radius_outer - padding
        // Card (16dp) with 6dp internal inset padding -> 10dp inner radius
        val computedInnerRadius = RadiusCard - 6.dp
        assertEquals(RadiusInset, computedInnerRadius)
    }

    // =========================================================================
    // 3. SPACING & RESPONSIVE LAYOUT TESTS (8dp Grid Discipline)
    // =========================================================================

    @Test
    fun test_spacing_baseTokens_multiplesOfGrid() {
        val tokens = listOf(Space2, Space4, Space8, Space12, Space16, Space20, Space24, Space32, Space40, Space48)
        for (token in tokens) {
            // All tokens must be divisible by 2dp (micro-steps) or 4dp/8dp
            assertEquals(0f, token.value % 2f, 0.001f)
        }
        assertEquals(8.dp, Space8)
        assertEquals(16.dp, Space16)
        assertEquals(24.dp, Space24)
        assertEquals(32.dp, Space32)
    }

    @Test
    fun test_responsiveLayout_breakpointPrimitives() {
        assertEquals(380.dp, BreakpointCompactWidth)
        assertEquals(600.dp, BreakpointTabletWidth)
        assertEquals(640.dp, MaxContentWidthTablet)
        assertEquals(12.dp, PaddingScreenCompact)
        assertEquals(16.dp, PaddingScreenStandard)
        assertEquals(48.dp, MinTouchTargetSize)
    }

    @Test
    fun test_finTrackSpacing_contract() {
        val spacing = FinTrackSpacing()
        assertEquals(12.dp, spacing.screenPaddingCompact)
        assertEquals(16.dp, spacing.screenPaddingStandard)
        assertEquals(640.dp, spacing.maxContentWidth)
        assertEquals(48.dp, spacing.minTouchTarget)
    }

    // =========================================================================
    // 4. COLOR CONTRAST RATIO TESTS (WCAG 2.1 AA / AAA Compliance)
    // =========================================================================

    @Test
    fun test_colorContrast_lightMode_textOnSurfaces() {
        // TextPrimaryLight (#0F172A) on SurfacePrimaryLight (#FFFFFF) -> WCAG AAA (>= 7.0:1)
        val primaryTextContrastOnWhite = contrastRatio(TextPrimaryLight, SurfacePrimaryLight)
        assertTrue(
            "Expected TextPrimaryLight on SurfacePrimaryLight >= 7.0:1 (AAA), was $primaryTextContrastOnWhite",
            primaryTextContrastOnWhite >= 7.0
        )

        // TextSecondaryLight (#475569) on SurfacePrimaryLight (#FFFFFF) -> WCAG AAA / AA (>= 4.5:1)
        val secondaryTextContrastOnWhite = contrastRatio(TextSecondaryLight, SurfacePrimaryLight)
        assertTrue(
            "Expected TextSecondaryLight on SurfacePrimaryLight >= 4.5:1 (AA), was $secondaryTextContrastOnWhite",
            secondaryTextContrastOnWhite >= 4.5
        )

        // TextMutedLight (#64748B) on SurfacePrimaryLight (#FFFFFF) -> WCAG AA (>= 4.5:1)
        val mutedTextContrastOnWhite = contrastRatio(TextMutedLight, SurfacePrimaryLight)
        assertTrue(
            "Expected TextMutedLight on SurfacePrimaryLight >= 4.5:1 (AA), was $mutedTextContrastOnWhite",
            mutedTextContrastOnWhite >= 4.5
        )

        // TextPrimaryLight on PageBackgroundLight (#F8FAFC) -> WCAG AAA (>= 7.0:1)
        val primaryTextContrastOnPage = contrastRatio(TextPrimaryLight, PageBackgroundLight)
        assertTrue(
            "Expected TextPrimaryLight on PageBackgroundLight >= 7.0:1 (AAA), was $primaryTextContrastOnPage",
            primaryTextContrastOnPage >= 7.0
        )
    }

    @Test
    fun test_colorContrast_lightMode_interactiveAndIndicators() {
        // White text on BrandPrimaryLight (#1E3A8A) -> WCAG AAA (>= 7.0:1)
        val brandPrimaryContrast = contrastRatio(Color.White, BrandPrimaryLight)
        assertTrue(
            "Expected White on BrandPrimaryLight >= 7.0:1, was $brandPrimaryContrast",
            brandPrimaryContrast >= 7.0
        )

        // White text on BrandAccentLight (#2563EB) -> WCAG AA (>= 4.5:1)
        val brandAccentContrast = contrastRatio(Color.White, BrandAccentLight)
        assertTrue(
            "Expected White on BrandAccentLight >= 4.5:1, was $brandAccentContrast",
            brandAccentContrast >= 4.5
        )

        // Expense indicator (#DC2626) on SurfacePrimaryLight (#FFFFFF) -> WCAG AA (>= 4.5:1)
        val expenseContrastOnWhite = contrastRatio(ExpenseLight, SurfacePrimaryLight)
        assertTrue(
            "Expected ExpenseLight on SurfacePrimaryLight >= 4.5:1, was $expenseContrastOnWhite",
            expenseContrastOnWhite >= 4.5
        )
    }

    @Test
    fun test_colorContrast_darkMode_textOnSurfaces() {
        // TextPrimaryDark (#F8FAFC) on SurfacePrimaryDark (#131B2E) -> WCAG AAA (>= 7.0:1)
        val primaryTextContrastOnObsidian = contrastRatio(TextPrimaryDark, SurfacePrimaryDark)
        assertTrue(
            "Expected TextPrimaryDark on SurfacePrimaryDark >= 7.0:1 (AAA), was $primaryTextContrastOnObsidian",
            primaryTextContrastOnObsidian >= 7.0
        )

        // TextSecondaryDark (#94A3B8) on SurfacePrimaryDark (#131B2E) -> WCAG AA (>= 4.5:1)
        val secondaryTextContrastOnObsidian = contrastRatio(TextSecondaryDark, SurfacePrimaryDark)
        assertTrue(
            "Expected TextSecondaryDark on SurfacePrimaryDark >= 4.5:1 (AA), was $secondaryTextContrastOnObsidian",
            secondaryTextContrastOnObsidian >= 4.5
        )

        // TextPrimaryDark on PageBackgroundDark (#0B0F19) -> WCAG AAA (>= 7.0:1)
        val primaryTextContrastOnDarkPage = contrastRatio(TextPrimaryDark, PageBackgroundDark)
        assertTrue(
            "Expected TextPrimaryDark on PageBackgroundDark >= 7.0:1 (AAA), was $primaryTextContrastOnDarkPage",
            primaryTextContrastOnDarkPage >= 7.0
        )

        // Income indicator (#10B981) on SurfacePrimaryDark (#131B2E) -> WCAG AA (>= 4.5:1)
        val incomeDarkContrast = contrastRatio(IncomeDark, SurfacePrimaryDark)
        assertTrue(
            "Expected IncomeDark on SurfacePrimaryDark >= 4.5:1, was $incomeDarkContrast",
            incomeDarkContrast >= 4.5
        )

        // Expense indicator (#F87171) on SurfacePrimaryDark (#131B2E) -> WCAG AA (>= 4.5:1)
        val expenseDarkContrast = contrastRatio(ExpenseDark, SurfacePrimaryDark)
        assertTrue(
            "Expected ExpenseDark on SurfacePrimaryDark >= 4.5:1, was $expenseDarkContrast",
            expenseDarkContrast >= 4.5
        )
    }

    @Test
    fun test_colorContrast_surfaceHero_invertedCanvas() {
        // SurfaceHeroDarkMidnight (#0F172A) is dark across BOTH Light and Dark themes
        assertEquals(Color(0xFF0F172A), SurfaceHeroDarkMidnight)
        assertEquals(SurfaceHeroDarkMidnight, LightFinTrackColors.surfaceHero)
        assertEquals(SurfaceHeroDarkMidnight, DarkFinTrackColors.surfaceHero)

        // White text on SurfaceHeroDarkMidnight -> WCAG AAA (>= 7.0:1)
        val whiteOnHeroContrast = contrastRatio(Color.White, SurfaceHeroDarkMidnight)
        assertTrue(
            "Expected White on SurfaceHeroDarkMidnight >= 7.0:1 (AAA), was $whiteOnHeroContrast",
            whiteOnHeroContrast >= 7.0
        )

        // TextPrimary on SurfaceHeroDarkMidnight -> WCAG AAA (>= 7.0:1)
        val textPrimaryOnHeroContrast = contrastRatio(TextPrimaryDark, SurfaceHeroDarkMidnight)
        assertTrue(
            "Expected TextPrimaryDark on SurfaceHeroDarkMidnight >= 7.0:1 (AAA), was $textPrimaryOnHeroContrast",
            textPrimaryOnHeroContrast >= 7.0
        )
    }

    // =========================================================================
    // 5. THEME CONTRACT & REPOSITORIES INTEGRITY
    // =========================================================================

    @Test
    fun test_finTrackColors_themeParity() {
        assertFalse(LightFinTrackColors.isDark)
        assertTrue(DarkFinTrackColors.isDark)

        assertEquals(SurfacePrimaryLight, LightFinTrackColors.surfacePrimary)
        assertEquals(Color(0xFFFFFFFF), LightFinTrackColors.surfacePrimary)
        assertEquals(SurfacePrimaryDark, DarkFinTrackColors.surfacePrimary)
        assertEquals(Color(0xFF131B2E), DarkFinTrackColors.surfacePrimary)

        assertEquals(BorderSubtleLight, LightFinTrackColors.borderSubtle)
        assertEquals(Color(0xFFE2E8F0), LightFinTrackColors.borderSubtle)
        assertEquals(BorderSubtleDark, DarkFinTrackColors.borderSubtle)
        assertEquals(Color(0xFF1E293B), DarkFinTrackColors.borderSubtle)
    }
}
