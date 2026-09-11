package com.example.ui.theme

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

// =============================================================================
// FinTrack Design System v3 — Material 3 Expressive Shape Vocabulary
// Direction: Organic, fluid, tactile Android-native family finance experience.
// Replaces static radii with a purposeful, semantic geometry scale.
// =============================================================================

// -----------------------------------------------------------------------------
// 1. BASE GEOMETRY SCALES
// -----------------------------------------------------------------------------
val ShapeNone = RectangleShape
val ShapeExtraSmall = RoundedCornerShape(4.dp)      // Inner badges, micro indicators
val ShapeSmall = RoundedCornerShape(8.dp)           // Subcategory chips, toast alerts
val ShapeMedium = RoundedCornerShape(12.dp)         // Form inputs, segment buttons
val ShapeLarge = RoundedCornerShape(16.dp)          // Floating dialogs, interactive cards
val ShapeExtraLarge = RoundedCornerShape(24.dp)     // Grouped container panels
val ShapePill = CircleShape                         // Primary buttons, status capsules

// -----------------------------------------------------------------------------
// 2. CONTEXTUAL & COMPONENT GEOMETRIES (M3 Expressive Family Finance)
// -----------------------------------------------------------------------------
val ShapeHeroCanvas = RoundedCornerShape(
    topStart = 0.dp,
    topEnd = 0.dp,
    bottomStart = 32.dp,
    bottomEnd = 32.dp
)

val ShapeGroupedContainer = RoundedCornerShape(24.dp)
val ShapeGroupedItemTop = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomStart = 4.dp, bottomEnd = 4.dp)
val ShapeGroupedItemMiddle = RoundedCornerShape(4.dp)
val ShapeGroupedItemBottom = RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp, bottomStart = 20.dp, bottomEnd = 20.dp)
val ShapeGroupedItemSingle = RoundedCornerShape(20.dp)

val ShapeFloatingActionButton = RoundedCornerShape(18.dp)
val ShapeModalSheet = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
val ShapeNavigationCapsule = RoundedCornerShape(28.dp)
val ShapeSquircleIcon = RoundedCornerShape(14.dp)
val ShapeBadgeOrganic = RoundedCornerShape(topStart = 12.dp, topEnd = 4.dp, bottomStart = 12.dp, bottomEnd = 12.dp)

// -----------------------------------------------------------------------------
// 3. SEMANTIC SHAPES CONTRACT
// -----------------------------------------------------------------------------
@Immutable
data class FinTrackShapes(
    val compactControl: Shape = ShapeSmall,
    val pill: Shape = ShapePill,
    val button: Shape = RoundedCornerShape(20.dp),
    val chip: Shape = ShapeSmall,
    val card: Shape = ShapeLarge,
    val prominentCard: Shape = ShapeGroupedItemSingle,
    val hero: Shape = ShapeHeroCanvas,
    val listContainer: Shape = ShapeGroupedContainer,
    val listItem: Shape = ShapeGroupedItemSingle,
    val listItemTop: Shape = ShapeGroupedItemTop,
    val listItemMiddle: Shape = ShapeGroupedItemMiddle,
    val listItemBottom: Shape = ShapeGroupedItemBottom,
    val dialog: Shape = ShapeLarge,
    val bottomSheet: Shape = ShapeModalSheet,
    val navigationCapsule: Shape = ShapeNavigationCapsule,
    val inputField: Shape = ShapeMedium,
    val microIndicator: Shape = ShapeExtraSmall,
    val fab: Shape = ShapeFloatingActionButton,
    val squircleIcon: Shape = ShapeSquircleIcon,
    val organicBadge: Shape = ShapeBadgeOrganic
)

val LocalFinTrackShapes = staticCompositionLocalOf { FinTrackShapes() }

// -----------------------------------------------------------------------------
// 4. BACKWARD-COMPATIBILITY ALIASES & PROGRESSIVE TOKENS
// (Preserves existing screens, components and tests without breaking changes)
// -----------------------------------------------------------------------------
val RadiusPill = 999.dp
val RadiusHero = 24.dp
val RadiusCard = 16.dp
val RadiusInset = 10.dp
val RadiusMicro = 6.dp

val ShapeHero = RoundedCornerShape(RadiusHero)
val ShapeCard = RoundedCornerShape(RadiusCard)
val ShapeInset = RoundedCornerShape(RadiusInset)
val ShapeMicro = RoundedCornerShape(RadiusMicro)

val RadiusSmall = 8.dp
val RadiusMedium = 12.dp
val RadiusLarge = RadiusCard
val RadiusXLarge = RadiusHero
val RadiusFull = RadiusPill

// -----------------------------------------------------------------------------
// 5. MATERIAL 3 SHAPES MAPPING
// -----------------------------------------------------------------------------
val Shapes = Shapes(
    extraSmall = ShapeMicro,
    small = RoundedCornerShape(RadiusSmall),
    medium = ShapeInset,
    large = ShapeCard,
    extraLarge = ShapeHero
)

