package com.example.ui.theme

import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.TweenSpec
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf

// ==========================================
// FinTrack Design System v1 — Motion Foundation
// Restrained, predictable, finite-by-default motion tokens.
// ==========================================

object FinTrackMotion {
    // Duration Tokens (milliseconds)
    const val DurationFast = 150
    const val DurationStandard = 200
    const val DurationEmphasized = 250
    const val DurationSyncSpin = 1000

    // Easing Curves
    val StandardEasing: Easing = FastOutSlowInEasing
    val LinearCurve: Easing = LinearEasing

    // Standard Tween Specs
    fun <T> fastTween(easing: Easing = StandardEasing): TweenSpec<T> =
        tween(durationMillis = DurationFast, easing = easing)

    fun <T> standardTween(easing: Easing = StandardEasing): TweenSpec<T> =
        tween(durationMillis = DurationStandard, easing = easing)

    fun <T> emphasizedTween(easing: Easing = StandardEasing): TweenSpec<T> =
        tween(durationMillis = DurationEmphasized, easing = easing)

    // Reusable Presentation Transitions
    /**
     * Subtle content fade for value updates (e.g. net balance or metric changes).
     * Enters smoothly with fade-in and exits with fade-out.
     */
    fun contentFade(
        enterDuration: Int = DurationEmphasized,
        exitDuration: Int = DurationStandard,
        easing: Easing = StandardEasing
    ): ContentTransform =
        fadeIn(animationSpec = tween(durationMillis = enterDuration, easing = easing)) togetherWith
                fadeOut(animationSpec = tween(durationMillis = exitDuration, easing = easing))
}

@Immutable
data class FinTrackMotionTokens(
    val durationFast: Int = FinTrackMotion.DurationFast,
    val durationStandard: Int = FinTrackMotion.DurationStandard,
    val durationEmphasized: Int = FinTrackMotion.DurationEmphasized,
    val durationSyncSpin: Int = FinTrackMotion.DurationSyncSpin
)

val LocalFinTrackMotion = staticCompositionLocalOf { FinTrackMotionTokens() }
