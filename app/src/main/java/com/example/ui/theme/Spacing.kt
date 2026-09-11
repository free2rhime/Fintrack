package com.example.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// =============================================================================
// FinTrack Design System v3 — Spacing & Layout System
// Direction: Material 3 Expressive — Family Finance Experience
// Primary design grid = 8dp (with 2dp/4dp micro-steps), touch target >= 48dp
// =============================================================================

// -----------------------------------------------------------------------------
// 1. BASE GRID SPACING TOKENS
// -----------------------------------------------------------------------------
val Space2: Dp = 2.dp
val Space4: Dp = 4.dp
val Space8: Dp = 8.dp
val Space12: Dp = 12.dp
val Space16: Dp = 16.dp
val Space20: Dp = 20.dp
val Space24: Dp = 24.dp
val Space32: Dp = 32.dp
val Space40: Dp = 40.dp
val Space48: Dp = 48.dp

// -----------------------------------------------------------------------------
// 2. RESPONSIVE LAYOUT BREAKPOINTS & PRIMITIVES
// -----------------------------------------------------------------------------

/**
 * Narrow viewport breakpoint (< 380dp, e.g. 360dp devices).
 * Triggers compact padding and stacked/dense metric values.
 */
val BreakpointCompactWidth: Dp = 380.dp

/**
 * Tablet / Foldable viewport breakpoint (> 600dp).
 * Triggers centered column layout with max content constraint.
 */
val BreakpointTabletWidth: Dp = 600.dp

/**
 * Maximum content width on wide/tablet displays.
 */
val MaxContentWidthTablet: Dp = 640.dp

/**
 * Screen edge padding on compact viewports (< 380dp).
 */
val PaddingScreenCompact: Dp = 12.dp

/**
 * Standard screen edge padding (380dp - 600dp).
 */
val PaddingScreenStandard: Dp = 16.dp

/**
 * Minimum touch target dimension complying with Android Accessibility standards.
 */
val MinTouchTargetSize: Dp = 48.dp

// -----------------------------------------------------------------------------
// 3. MATERIAL 3 EXPRESSIVE LAYOUT & CONTAINER TOKENS
// -----------------------------------------------------------------------------
val SpacingCompactControl: Dp = 6.dp
val SpacingGroupedList: Dp = 2.dp
val SpacingGroupedContainer: Dp = 20.dp
val SpacingSection: Dp = 24.dp
val SpacingCardInsets: Dp = 16.dp
val SpacingHeroInsets: Dp = 20.dp
val SpacingListItemPadding: Dp = 14.dp
val DividerInsetHairline: Dp = 56.dp
val DividerThicknessHairline: Dp = 1.dp

// -----------------------------------------------------------------------------
// 4. FINTRACK SPACING CONTRACT
// -----------------------------------------------------------------------------
@Immutable
data class FinTrackSpacing(
    val space2: Dp = Space2,
    val space4: Dp = Space4,
    val space8: Dp = Space8,
    val space12: Dp = Space12,
    val space16: Dp = Space16,
    val space20: Dp = Space20,
    val space24: Dp = Space24,
    val space32: Dp = Space32,
    val space40: Dp = Space40,
    val space48: Dp = Space48,
    val screenPaddingCompact: Dp = PaddingScreenCompact,
    val screenPaddingStandard: Dp = PaddingScreenStandard,
    val maxContentWidth: Dp = MaxContentWidthTablet,
    val minTouchTarget: Dp = MinTouchTargetSize,
    // --- Material 3 Expressive Container & Layout Tokens ---
    val compactControlPadding: Dp = SpacingCompactControl,
    val groupedListSpacing: Dp = SpacingGroupedList,
    val groupedContainerPadding: Dp = SpacingGroupedContainer,
    val sectionSpacing: Dp = SpacingSection,
    val screenMarginsCompact: Dp = PaddingScreenCompact,
    val screenMarginsStandard: Dp = PaddingScreenStandard,
    val cardInsets: Dp = SpacingCardInsets,
    val heroInsets: Dp = SpacingHeroInsets,
    val listItemPadding: Dp = SpacingListItemPadding,
    val hairlineDividerInset: Dp = DividerInsetHairline,
    val hairlineDividerThickness: Dp = DividerThicknessHairline,
    val touchTargetMin: Dp = MinTouchTargetSize
)

val LocalFinTrackSpacing = staticCompositionLocalOf { FinTrackSpacing() }

