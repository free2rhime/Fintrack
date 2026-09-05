package com.example.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// ==========================================
// FinTrack Design System v1 — Spacing Tokens
// Primary design grid = 8dp (with 2dp/4dp micro-steps)
// ==========================================

val Space2: Dp = 2.dp
val Space4: Dp = 4.dp
val Space8: Dp = 8.dp
val Space12: Dp = 12.dp
val Space16: Dp = 16.dp
val Space20: Dp = 20.dp
val Space24: Dp = 24.dp
val Space32: Dp = 32.dp

@Immutable
data class FinTrackSpacing(
    val space2: Dp = Space2,
    val space4: Dp = Space4,
    val space8: Dp = Space8,
    val space12: Dp = Space12,
    val space16: Dp = Space16,
    val space20: Dp = Space20,
    val space24: Dp = Space24,
    val space32: Dp = Space32
)

val LocalFinTrackSpacing = staticCompositionLocalOf { FinTrackSpacing() }
