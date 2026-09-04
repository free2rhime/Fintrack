package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

// ==========================================
// FinTrack Design System v1 — Shape Tokens
// ==========================================

val RadiusSmall = 8.dp
val RadiusMedium = 12.dp
val RadiusLarge = 16.dp
val RadiusXLarge = 24.dp
val RadiusFull = 999.dp

val Shapes = Shapes(
    extraSmall = RoundedCornerShape(RadiusSmall),
    small = RoundedCornerShape(RadiusSmall),
    medium = RoundedCornerShape(RadiusMedium),
    large = RoundedCornerShape(RadiusLarge),
    extraLarge = RoundedCornerShape(RadiusXLarge)
)
