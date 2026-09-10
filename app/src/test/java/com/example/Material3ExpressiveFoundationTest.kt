package com.example

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Spring
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.BorderStandardDark
import com.example.ui.theme.BorderStandardLight
import com.example.ui.theme.BorderSubtleDark
import com.example.ui.theme.BorderSubtleLight
import com.example.ui.theme.DarkFinTrackColors
import com.example.ui.theme.DividerInsetHairline
import com.example.ui.theme.DividerThicknessHairline
import com.example.ui.theme.FinTrackMotion
import com.example.ui.theme.FinTrackMotionTokens
import com.example.ui.theme.FinTrackShapes
import com.example.ui.theme.FinTrackSpacing
import com.example.ui.theme.FinTrackTypography
import com.example.ui.theme.LightFinTrackColors
import com.example.ui.theme.RadiusCard
import com.example.ui.theme.RadiusHero
import com.example.ui.theme.RadiusInset
import com.example.ui.theme.RadiusMicro
import com.example.ui.theme.RadiusPill
import com.example.ui.theme.ShapeCard
import com.example.ui.theme.ShapeExtraLarge
import com.example.ui.theme.ShapeExtraSmall
import com.example.ui.theme.ShapeFloatingActionButton
import com.example.ui.theme.ShapeGroupedContainer
import com.example.ui.theme.ShapeGroupedItemBottom
import com.example.ui.theme.ShapeGroupedItemMiddle
import com.example.ui.theme.ShapeGroupedItemSingle
import com.example.ui.theme.ShapeGroupedItemTop
import com.example.ui.theme.ShapeHero
import com.example.ui.theme.ShapeHeroCanvas
import com.example.ui.theme.ShapeLarge
import com.example.ui.theme.ShapeMedium
import com.example.ui.theme.ShapeModalSheet
import com.example.ui.theme.ShapeNavigationCapsule
import com.example.ui.theme.ShapePill
import com.example.ui.theme.ShapeSmall
import com.example.ui.theme.SpacingCardInsets
import com.example.ui.theme.SpacingCompactControl
import com.example.ui.theme.SpacingGroupedContainer
import com.example.ui.theme.SpacingGroupedList
import com.example.ui.theme.SpacingHeroInsets
import com.example.ui.theme.SpacingListItemPadding
import com.example.ui.theme.SpacingSection
import com.example.ui.theme.SurfaceContainerDark
import com.example.ui.theme.SurfaceContainerHighDark
import com.example.ui.theme.SurfaceContainerHighLight
import com.example.ui.theme.SurfaceContainerHighestDark
import com.example.ui.theme.SurfaceContainerHighestLight
import com.example.ui.theme.SurfaceContainerLight
import com.example.ui.theme.SurfaceContainerLowDark
import com.example.ui.theme.SurfaceContainerLowLight
import com.example.ui.theme.SurfaceContainerLowestDark
import com.example.ui.theme.SurfaceContainerLowestLight
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.max
import kotlin.math.min

/**
 * Foundation verification test suite for FinTrack Material 3 Expressive (M3-1).
 * Verifies color hierarchy, tonal containers, semantic shapes, financial typography (tnum),
 * responsive spacing tokens, spring physics motion, and legacy theme compatibility.
 */
class Material3ExpressiveFoundationTest {

    private fun contrastRatio(foreground: Color, background: Color): Double {
        val lum1 = foreground.luminance()
        val lum2 = background.luminance()
        val brightest = max(lum1, lum2)
        val darkest = min(lum1, lum2)
        return (brightest + 0.05) / (darkest + 0.05)
    }

    // =========================================================================
    // 1. MATERIAL 3 SURFACE HIERARCHY & TONAL ELEVATION
    // =========================================================================

    @Test
    fun test_colorSystem_m3SurfaceHierarchy_lightMode() {
        val colors = LightFinTrackColors

        // In M3 Expressive Light Mode, surfaces are tonal and luminous:
        // Lowest (Canvas) is luminous, containers are tonal and airy
        assertTrue("SurfaceContainerLowest must be bright", colors.surfaceContainerLowest.luminance() > 0.9f)
        assertTrue("SurfaceContainerLow (Hero) must be luminous", colors.surfaceContainerLow.luminance() > 0.85f)
        assertTrue("SurfaceContainer must be tonal and breathable", colors.surfaceContainer.luminance() > 0.80f)

        // Verifies distinct container hierarchy
        assertEquals(SurfaceContainerLowestLight, colors.surfaceContainerLowest)
        assertEquals(SurfaceContainerLowLight, colors.surfaceContainerLow)
        assertEquals(SurfaceContainerLight, colors.surfaceContainer)
        assertEquals(SurfaceContainerHighLight, colors.surfaceContainerHigh)
        assertEquals(SurfaceContainerHighestLight, colors.surfaceContainerHighest)

        // Hierarchy differentiation: Lowest -> Highest tonal ramp in Light Mode
        assertTrue(colors.surfaceContainerLowest.luminance() >= colors.surfaceContainerLow.luminance())
        assertTrue(colors.surfaceContainerLow.luminance() >= colors.surfaceContainer.luminance())
        assertTrue(colors.surfaceContainer.luminance() >= colors.surfaceContainerHigh.luminance())
        assertTrue(colors.surfaceContainerHigh.luminance() >= colors.surfaceContainerHighest.luminance())
    }

    @Test
    fun test_colorSystem_m3SurfaceHierarchy_darkMode() {
        val colors = DarkFinTrackColors

        // In M3 Expressive Dark Mode:
        // Lowest is deepest obsidian, ascending elevation steps get tonally lighter
        assertEquals(SurfaceContainerLowestDark, colors.surfaceContainerLowest)
        assertEquals(SurfaceContainerLowDark, colors.surfaceContainerLow)
        assertEquals(SurfaceContainerDark, colors.surfaceContainer)
        assertEquals(SurfaceContainerHighDark, colors.surfaceContainerHigh)
        assertEquals(SurfaceContainerHighestDark, colors.surfaceContainerHighest)

        // Tonal elevation ramp in Dark Mode: lowest has lowest luminance
        assertTrue(colors.surfaceContainerLowest.luminance() <= colors.surfaceContainerLow.luminance())
        assertTrue(colors.surfaceContainerLow.luminance() <= colors.surfaceContainer.luminance())
        assertTrue(colors.surfaceContainer.luminance() <= colors.surfaceContainerHigh.luminance())
        assertTrue(colors.surfaceContainerHigh.luminance() <= colors.surfaceContainerHighest.luminance())
    }

    // =========================================================================
    // 2. LIGHT / DARK SEMANTIC PARITY & FINANCIAL CONTRAST
    // =========================================================================

    @Test
    fun test_colorSystem_semanticParity() {
        val light = LightFinTrackColors
        val dark = DarkFinTrackColors

        assertFalse(light.isDark)
        assertTrue(dark.isDark)

        // Essential semantic M3 roles exist and are non-null across both themes
        val lightRoles = listOf(
            light.primary, light.onPrimary, light.primaryContainer, light.onPrimaryContainer,
            light.secondary, light.onSecondary, light.secondaryContainer, light.onSecondaryContainer,
            light.tertiary, light.onTertiary, light.tertiaryContainer, light.onTertiaryContainer,
            light.error, light.onError, light.errorContainer, light.onErrorContainer,
            light.warning, light.onWarning, light.warningContainer, light.onWarningContainer,
            light.success, light.onSuccess, light.successContainer, light.onSuccessContainer,
            light.info, light.onInfo, light.infoContainer, light.onInfoContainer,
            light.outline, light.outlineVariant, light.scrim
        )
        val darkRoles = listOf(
            dark.primary, dark.onPrimary, dark.primaryContainer, dark.onPrimaryContainer,
            dark.secondary, dark.onSecondary, dark.secondaryContainer, dark.onSecondaryContainer,
            dark.tertiary, dark.onTertiary, dark.tertiaryContainer, dark.onTertiaryContainer,
            dark.error, dark.onError, dark.errorContainer, dark.onErrorContainer,
            dark.warning, dark.onWarning, dark.warningContainer, dark.onWarningContainer,
            dark.success, dark.onSuccess, dark.successContainer, dark.onSuccessContainer,
            dark.info, dark.onInfo, dark.infoContainer, dark.onInfoContainer,
            dark.outline, dark.outlineVariant, dark.scrim
        )

        assertEquals(lightRoles.size, darkRoles.size)
        lightRoles.forEach { assertNotNull(it) }
        darkRoles.forEach { assertNotNull(it) }
    }

    @Test
    fun test_colorSystem_financialContrast_wcagCompliance() {
        // Light theme financial indicators contrast on background (WCAG UI component/graphical contrast >= 3.0:1)
        val incomeContrastLight = contrastRatio(LightFinTrackColors.income, LightFinTrackColors.background)
        assertTrue("Income light contrast must be >= 3.0:1, was $incomeContrastLight", incomeContrastLight >= 3.0)

        val expenseContrastLight = contrastRatio(LightFinTrackColors.expense, LightFinTrackColors.background)
        assertTrue("Expense light contrast must be >= 4.5:1, was $expenseContrastLight", expenseContrastLight >= 4.5)

        // Dark theme financial indicators contrast on background (WCAG AA >= 4.5:1)
        val incomeContrastDark = contrastRatio(DarkFinTrackColors.income, DarkFinTrackColors.background)
        assertTrue("Income dark contrast must be >= 4.5:1, was $incomeContrastDark", incomeContrastDark >= 4.5)

        val expenseContrastDark = contrastRatio(DarkFinTrackColors.expense, DarkFinTrackColors.background)
        assertTrue("Expense dark contrast must be >= 4.5:1, was $expenseContrastDark", expenseContrastDark >= 4.5)

        // TextPrimary contrast on background (WCAG AAA >= 7.0:1)
        val textContrastLight = contrastRatio(LightFinTrackColors.textPrimary, LightFinTrackColors.background)
        assertTrue("TextPrimary light contrast >= 7.0:1, was $textContrastLight", textContrastLight >= 7.0)

        val textContrastDark = contrastRatio(DarkFinTrackColors.textPrimary, DarkFinTrackColors.background)
        assertTrue("TextPrimary dark contrast >= 7.0:1, was $textContrastDark", textContrastDark >= 7.0)
    }

    // =========================================================================
    // 3. EXPRESSIVE SHAPE SYSTEM & MEANINGFUL DIFFERENTIATION
    // =========================================================================

    @Test
    fun test_shapes_semanticAvailability() {
        val shapes = FinTrackShapes()

        assertNotNull(shapes.compactControl)
        assertNotNull(shapes.pill)
        assertNotNull(shapes.button)
        assertNotNull(shapes.chip)
        assertNotNull(shapes.card)
        assertNotNull(shapes.prominentCard)
        assertNotNull(shapes.hero)
        assertNotNull(shapes.listContainer)
        assertNotNull(shapes.listItem)
        assertNotNull(shapes.listItemTop)
        assertNotNull(shapes.listItemMiddle)
        assertNotNull(shapes.listItemBottom)
        assertNotNull(shapes.dialog)
        assertNotNull(shapes.bottomSheet)
        assertNotNull(shapes.navigationCapsule)
        assertNotNull(shapes.inputField)
        assertNotNull(shapes.microIndicator)
        assertNotNull(shapes.fab)
    }

    @Test
    fun test_shapes_meaningfulDifferentiation() {
        // Ensure FinTrack is not using monotonous 16dp rounded rectangles everywhere
        assertEquals(ShapeExtraSmall, FinTrackShapes().microIndicator)
        assertEquals(ShapeSmall, FinTrackShapes().compactControl)
        assertEquals(ShapeMedium, FinTrackShapes().inputField)
        assertEquals(ShapeLarge, FinTrackShapes().card)
        assertEquals(ShapeGroupedItemSingle, FinTrackShapes().prominentCard)
        assertEquals(ShapeHeroCanvas, FinTrackShapes().hero)
        assertEquals(ShapeGroupedContainer, FinTrackShapes().listContainer)
        assertEquals(ShapeModalSheet, FinTrackShapes().bottomSheet)
        assertEquals(ShapeNavigationCapsule, FinTrackShapes().navigationCapsule)
        assertEquals(ShapeFloatingActionButton, FinTrackShapes().fab)

        // Contextual shapes must not be identical
        assertNotEquals(FinTrackShapes().card, FinTrackShapes().hero)
        assertNotEquals(FinTrackShapes().card, FinTrackShapes().listContainer)
        assertNotEquals(FinTrackShapes().card, FinTrackShapes().pill)
        assertNotEquals(FinTrackShapes().compactControl, FinTrackShapes().inputField)
        assertNotEquals(FinTrackShapes().listItemTop, FinTrackShapes().listItemMiddle)
        assertNotEquals(FinTrackShapes().listItemMiddle, FinTrackShapes().listItemBottom)
    }

    @Test
    fun test_shapes_legacyCompatibility() {
        // Ensures older tokens are preserved for existing components
        assertEquals(6.dp, RadiusMicro)
        assertEquals(10.dp, RadiusInset)
        assertEquals(16.dp, RadiusCard)
        assertEquals(24.dp, RadiusHero)
        assertEquals(999.dp, RadiusPill)

        assertNotNull(ShapeHero)
        assertNotNull(ShapeCard)
        assertNotNull(ShapePill)
    }

    // =========================================================================
    // 4. FINANCIAL TYPOGRAPHY & TABULAR NUMERALS (tnum)
    // =========================================================================

    @Test
    fun test_typography_financialRetainsTnum() {
        val typo = FinTrackTypography()

        // Tabular numerals MUST be enforced on all financial figures
        assertEquals("tnum", typo.financialDisplay.fontFeatureSettings)
        assertEquals("tnum", typo.financialDisplayCompact.fontFeatureSettings)
        assertEquals("tnum", typo.financialMetric.fontFeatureSettings)
        assertEquals("tnum", typo.financialMetricRegular.fontFeatureSettings)
        assertEquals("tnum", typo.financialExpressive.fontFeatureSettings)
    }

    @Test
    fun test_typography_hierarchy() {
        val typo = FinTrackTypography()

        // Financial hierarchy: Expressive (36sp) > Display (32sp) > Compact (26sp) > Metric (20sp) > Regular (15sp)
        assertTrue(typo.financialExpressive.fontSize.value > typo.financialDisplay.fontSize.value)
        assertTrue(typo.financialDisplay.fontSize.value > typo.financialDisplayCompact.fontSize.value)
        assertTrue(typo.financialDisplayCompact.fontSize.value > typo.financialMetric.fontSize.value)
        assertTrue(typo.financialMetric.fontSize.value > typo.financialMetricRegular.fontSize.value)

        // Editorial hierarchy: Screen Title (22sp) > Card Heading (16sp) > Body (14sp) > Label (12sp) > Metadata (10sp)
        assertTrue(typo.screenTitle.fontSize.value > typo.cardHeading.fontSize.value)
        assertTrue(typo.cardHeading.fontSize.value > typo.body.fontSize.value)
        assertTrue(typo.body.fontSize.value > typo.label.fontSize.value)
        assertTrue(typo.label.fontSize.value > typo.metadata.fontSize.value)
    }

    // =========================================================================
    // 5. SPACING TOKENS & RESPONSIVE LAYOUT
    // =========================================================================

    @Test
    fun test_spacing_tokensAndLayouts() {
        val spacing = FinTrackSpacing()

        // Base grid: divisible by 8dp (or micro 2dp/4dp steps)
        assertEquals(2.dp, spacing.space2)
        assertEquals(4.dp, spacing.space4)
        assertEquals(8.dp, spacing.space8)
        assertEquals(16.dp, spacing.space16)
        assertEquals(24.dp, spacing.space24)
        assertEquals(32.dp, spacing.space32)
        assertEquals(48.dp, spacing.space48)

        // Container & layout tokens
        assertEquals(SpacingCompactControl, spacing.compactControlPadding)
        assertEquals(SpacingGroupedList, spacing.groupedListSpacing)
        assertEquals(SpacingGroupedContainer, spacing.groupedContainerPadding)
        assertEquals(SpacingSection, spacing.sectionSpacing)
        assertEquals(SpacingCardInsets, spacing.cardInsets)
        assertEquals(SpacingHeroInsets, spacing.heroInsets)
        assertEquals(SpacingListItemPadding, spacing.listItemPadding)
        assertEquals(DividerInsetHairline, spacing.hairlineDividerInset)
        assertEquals(DividerThicknessHairline, spacing.hairlineDividerThickness)

        // Touch target compliance
        assertTrue("Min touch target must be at least 48dp", spacing.minTouchTarget >= 48.dp)
        assertTrue("Touch target min must be at least 48dp", spacing.touchTargetMin >= 48.dp)
    }

    // =========================================================================
    // 6. MOTION TOKENS & SPRING PHYSICS
    // =========================================================================

    @Test
    fun test_motion_expressiveSpringsAndTokens() {
        val motion = FinTrackMotionTokens()

        // Press / tactile interaction constants
        assertEquals(100, FinTrackMotion.DurationPress)
        assertEquals(100, motion.durationPress)
        assertEquals(0.975f, FinTrackMotion.PressScaleTarget, 0.001f)
        assertEquals(0.975f, motion.pressScaleTarget, 0.001f)

        // Expressive spring physics hierarchy
        assertEquals(Spring.DampingRatioLowBouncy, FinTrackMotion.SpatialSpring.dampingRatio, 0.001f)
        assertEquals(Spring.StiffnessLow, FinTrackMotion.SpatialSpring.stiffness, 0.001f)

        assertEquals(Spring.DampingRatioMediumBouncy, FinTrackMotion.InteractiveSpring.dampingRatio, 0.001f)
        assertEquals(Spring.StiffnessMedium, FinTrackMotion.InteractiveSpring.stiffness, 0.001f)

        assertEquals(Spring.DampingRatioNoBouncy, FinTrackMotion.MicroSpring.dampingRatio, 0.001f)
        assertEquals(Spring.StiffnessMedium, FinTrackMotion.MicroSpring.stiffness, 0.001f)

        // Expressive easing curve
        val expectedEasing = CubicBezierEasing(0.2f, 0.0f, 0.0f, 1.0f)
        assertEquals(expectedEasing, FinTrackMotion.ExpressiveEasing)
        assertEquals(expectedEasing, motion.expressiveEasing)

        // Semantic motion helper specifications are available
        assertNotNull(FinTrackMotion.pressInteractionSpec<Float>())
        assertNotNull(FinTrackMotion.selectionSpring<Float>())
        assertNotNull(FinTrackMotion.containerMorphSpec<Float>())
        assertNotNull(FinTrackMotion.contentEntranceSpec<Float>())
        assertNotNull(FinTrackMotion.contentChangeSpec<Float>())
        assertNotNull(FinTrackMotion.stateChangeSpec<Float>())
        assertNotNull(FinTrackMotion.navigationSpring<Float>())
        assertNotNull(FinTrackMotion.chartInteractionSpring<Float>())
        assertNotNull(FinTrackMotion.emphasisSpring<Float>())
        assertNotNull(FinTrackMotion.dismissSpring<Float>())
    }
}
