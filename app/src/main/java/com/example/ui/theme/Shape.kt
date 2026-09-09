package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

// =============================================================================
// FinTrack Design System v2 — Progressive Geometry Scale
// Direction: Precision Fintech + Editorial Wealth Narrative
// Radius_inner ≈ Radius_outer - Padding (Nested Geometric Harmony)
// =============================================================================

// -----------------------------------------------------------------------------
// 1. PROGRESSIVE GEOMETRY TOKENS
// -----------------------------------------------------------------------------

/**
 * Pill: Interactive chips, currency toggle switches, status badges.
 */
val RadiusPill = 999.dp

/**
 * Hero Surface: Financial Apex Card, Bottom Sheets.
 */
val RadiusHero = 24.dp

/**
 * Standard Card: Primary content surfaces (FinTrackCard, Chart containers).
 */
val RadiusCard = 16.dp

/**
 * Inset Well: Inset data blocks within cards (Chart HUD row, progress bar well).
 */
val RadiusInset = 10.dp

/**
 * Micro Element: Category color indicators, bar caps, chart selection dots.
 */
val RadiusMicro = 6.dp

// -----------------------------------------------------------------------------
// 2. PREDEFINED SHAPE SINGLETONS
// -----------------------------------------------------------------------------
val ShapePill = RoundedCornerShape(RadiusPill)
val ShapeHero = RoundedCornerShape(RadiusHero)
val ShapeCard = RoundedCornerShape(RadiusCard)
val ShapeInset = RoundedCornerShape(RadiusInset)
val ShapeMicro = RoundedCornerShape(RadiusMicro)

// -----------------------------------------------------------------------------
// 3. BACKWARD-COMPATIBILITY ALIASES
// (Preserves existing screens and components without breaking changes)
// -----------------------------------------------------------------------------
val RadiusSmall = 8.dp
val RadiusMedium = 12.dp
val RadiusLarge = RadiusCard
val RadiusXLarge = RadiusHero
val RadiusFull = RadiusPill

// -----------------------------------------------------------------------------
// 4. MATERIAL 3 SHAPES MAPPING
// -----------------------------------------------------------------------------
val Shapes = Shapes(
    extraSmall = ShapeMicro,
    small = RoundedCornerShape(RadiusSmall),
    medium = ShapeInset,
    large = ShapeCard,
    extraLarge = ShapeHero
)

