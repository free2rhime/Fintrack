package com.example.ui.theme

import android.content.Context
import android.provider.Settings
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.TweenSpec
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp

// =============================================================================
// FinTrack Motion System v3
// Direction: Material 3 Expressive — Family Finance Experience
// Tactile, spring-driven, organic response with reduced-motion accessibility
// =============================================================================

object FinTrackMotion {
    // -------------------------------------------------------------------------
    // 1. DURATION TOKENS (milliseconds)
    // -------------------------------------------------------------------------
    const val DurationInstant = 0
    const val DurationPress = 100          // Tactile press response
    const val DurationMicro = 120          // Micro interactions, chip selections, icon states
    const val DurationFast = 150           // DS v1 backward-compatibility
    const val DurationStandard = 200       // DS v1 backward-compatibility
    const val DurationV2Standard = 220     // Menus, sheet expansion, tab transitions
    const val DurationEmphasized = 250     // DS v1 backward-compatibility
    const val DurationV2Emphasized = 350   // Screen transitions, hero crossfades
    const val DurationChartSweep = 600     // Spline path initial draw-in
    const val DurationSyncSpin = 1000      // Sync spinner cycle

    // -------------------------------------------------------------------------
    // 2. TACTILE PROPERTIES & EASING CURVES (M3 Expressive)
    // -------------------------------------------------------------------------
    const val PressScaleTarget = 0.975f     // Subtle physical depression on press
    val ExpressiveEasing: Easing = CubicBezierEasing(0.2f, 0.0f, 0.0f, 1.0f)
    val StandardEasing: Easing = FastOutSlowInEasing
    val StandardDecelerate: Easing = FastOutSlowInEasing
    val StandardDecel: Easing = FastOutSlowInEasing
    val LinearCurve: Easing = LinearEasing
    val EmphasizedCubic: Easing = ExpressiveEasing

    // -------------------------------------------------------------------------
    // 3. COMPOSE SPRING PHYSICS (M3 Expressive Spring Hierarchy)
    // -------------------------------------------------------------------------
    /**
     * Spatial Spring:
     * DampingRatioLowBouncy, StiffnessLow.
     * Used for large surface movements, sheet expansion, dialogs, container morphs.
     */
    val SpatialSpring: SpringSpec<Float> = spring(
        dampingRatio = Spring.DampingRatioLowBouncy,
        stiffness = Spring.StiffnessLow
    )

    val SpatialSpringDp: SpringSpec<Dp> = spring(
        dampingRatio = Spring.DampingRatioLowBouncy,
        stiffness = Spring.StiffnessLow
    )

    /**
     * Interactive Spring:
     * DampingRatioMediumBouncy, StiffnessMedium.
     * Used for direct touch manipulation, button presses, selection pills, and tactile feedback.
     */
    val InteractiveSpring: SpringSpec<Float> = spring(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessMedium
    )

    val InteractiveSpringDp: SpringSpec<Dp> = spring(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessMedium
    )

    /**
     * Micro Spring:
     * DampingRatioNoBouncy, StiffnessMedium.
     * Used for state dots, micro indicators, badges, tiny toggles.
     */
    val MicroSpring: SpringSpec<Float> = spring(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMedium
    )

    val MicroSpringDp: SpringSpec<Dp> = spring(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMedium
    )

    /**
     * Content Spring:
     * DampingRatioNoBouncy, StiffnessLow.
     * Used for content transitions, layout reflows, and smooth state updates without overshoot.
     */
    val ContentSpring: SpringSpec<Float> = spring(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessLow
    )

    val ContentSpringDp: SpringSpec<Dp> = spring(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessLow
    )

    fun <T> interactiveSpring(visibilityThreshold: T? = null): SpringSpec<T> = spring(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessMedium,
        visibilityThreshold = visibilityThreshold
    )

    fun <T> contentSpring(visibilityThreshold: T? = null): SpringSpec<T> = spring(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessLow,
        visibilityThreshold = visibilityThreshold
    )

    // -------------------------------------------------------------------------
    // 4. SEMANTIC MOTION SPECIFICATIONS (M3 Expressive)
    // -------------------------------------------------------------------------
    fun <T> pressInteractionSpec(visibilityThreshold: T? = null): SpringSpec<T> = spring(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessMedium,
        visibilityThreshold = visibilityThreshold
    )

    fun <T> selectionSpring(visibilityThreshold: T? = null): SpringSpec<T> = spring(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessMediumLow,
        visibilityThreshold = visibilityThreshold
    )

    fun <T> containerMorphSpec(visibilityThreshold: T? = null): SpringSpec<T> = spring(
        dampingRatio = Spring.DampingRatioLowBouncy,
        stiffness = Spring.StiffnessLow,
        visibilityThreshold = visibilityThreshold
    )

    fun <T> contentEntranceSpec(visibilityThreshold: T? = null): SpringSpec<T> = spring(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessLow,
        visibilityThreshold = visibilityThreshold
    )

    fun <T> contentChangeSpec(visibilityThreshold: T? = null): SpringSpec<T> = spring(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMedium,
        visibilityThreshold = visibilityThreshold
    )

    fun <T> stateChangeSpec(visibilityThreshold: T? = null): SpringSpec<T> = spring(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMedium,
        visibilityThreshold = visibilityThreshold
    )

    fun <T> navigationSpring(visibilityThreshold: T? = null): SpringSpec<T> = spring(
        dampingRatio = Spring.DampingRatioLowBouncy,
        stiffness = Spring.StiffnessLow,
        visibilityThreshold = visibilityThreshold
    )

    fun <T> chartInteractionSpring(visibilityThreshold: T? = null): SpringSpec<T> = spring(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessMedium,
        visibilityThreshold = visibilityThreshold
    )

    fun <T> emphasisSpring(visibilityThreshold: T? = null): SpringSpec<T> = spring(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessLow,
        visibilityThreshold = visibilityThreshold
    )

    fun <T> dismissSpring(visibilityThreshold: T? = null): SpringSpec<T> = spring(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMedium,
        visibilityThreshold = visibilityThreshold
    )

    // -------------------------------------------------------------------------
    // 4. STANDARD TWEEN SPECS
    // -------------------------------------------------------------------------
    fun <T> microTween(easing: Easing = StandardDecelerate): TweenSpec<T> =
        tween(durationMillis = DurationMicro, easing = easing)

    fun <T> fastTween(easing: Easing = StandardEasing): TweenSpec<T> =
        tween(durationMillis = DurationFast, easing = easing)

    fun <T> standardTween(easing: Easing = StandardEasing): TweenSpec<T> =
        tween(durationMillis = DurationStandard, easing = easing)

    fun <T> standardV2Tween(easing: Easing = StandardDecelerate): TweenSpec<T> =
        tween(durationMillis = DurationV2Standard, easing = easing)

    fun <T> emphasizedTween(easing: Easing = StandardEasing): TweenSpec<T> =
        tween(durationMillis = DurationEmphasized, easing = easing)

    fun <T> emphasizedV2Tween(easing: Easing = EmphasizedCubic): TweenSpec<T> =
        tween(durationMillis = DurationV2Emphasized, easing = easing)

    // -------------------------------------------------------------------------
    // 5. REUSABLE PRESENTATION TRANSITIONS
    // -------------------------------------------------------------------------
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
    val durationInstant: Int = FinTrackMotion.DurationInstant,
    val durationPress: Int = FinTrackMotion.DurationPress,
    val durationMicro: Int = FinTrackMotion.DurationMicro,
    val durationFast: Int = FinTrackMotion.DurationFast,
    val durationStandard: Int = FinTrackMotion.DurationStandard,
    val durationV2Standard: Int = FinTrackMotion.DurationV2Standard,
    val durationEmphasized: Int = FinTrackMotion.DurationEmphasized,
    val durationV2Emphasized: Int = FinTrackMotion.DurationV2Emphasized,
    val durationChartSweep: Int = FinTrackMotion.DurationChartSweep,
    val durationSyncSpin: Int = FinTrackMotion.DurationSyncSpin,
    val interactiveSpring: SpringSpec<Float> = FinTrackMotion.InteractiveSpring,
    val contentSpring: SpringSpec<Float> = FinTrackMotion.ContentSpring,
    val spatialSpring: SpringSpec<Float> = FinTrackMotion.SpatialSpring,
    val microSpring: SpringSpec<Float> = FinTrackMotion.MicroSpring,
    val standardDecelerate: Easing = FinTrackMotion.StandardDecelerate,
    val emphasizedCubic: Easing = FinTrackMotion.EmphasizedCubic,
    val expressiveEasing: Easing = FinTrackMotion.ExpressiveEasing,
    val pressScaleTarget: Float = FinTrackMotion.PressScaleTarget
)

val LocalFinTrackMotion = staticCompositionLocalOf { FinTrackMotionTokens() }

/**
 * Checks system accessibility settings to determine if transition animations
 * are disabled (Scale = 0). Allows graceful degradation for motion sensitivity.
 */
@Composable
fun isReducedMotionEnabled(): Boolean {
    val context = LocalContext.current
    return remember(context) {
        try {
            val resolver = context.contentResolver
            val transitionScale = Settings.Global.getFloat(
                resolver,
                Settings.Global.TRANSITION_ANIMATION_SCALE,
                1.0f
            )
            val animatorScale = Settings.Global.getFloat(
                resolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1.0f
            )
            transitionScale == 0f || animatorScale == 0f
        } catch (_: Throwable) {
            false
        }
    }
}

/**
 * Applies subtle spring-based tactile press feedback (scaling down to target scale on press)
 * adhering to Material 3 Expressive family finance interaction principles.
 * Respects reduced-motion accessibility settings.
 */
fun Modifier.tactilePress(
    interactionSource: MutableInteractionSource? = null,
    targetScale: Float = FinTrackMotion.PressScaleTarget,
    enabled: Boolean = true
): Modifier = composed {
    if (!enabled || isReducedMotionEnabled()) return@composed this
    val source = interactionSource ?: remember { MutableInteractionSource() }
    val isSourcePressed by source.collectIsPressedAsState()
    var isPointerPressed by remember { mutableStateOf(false) }
    val isPressed = isSourcePressed || isPointerPressed
    val scale by animateFloatAsState(
        targetValue = if (isPressed) targetScale else 1.0f,
        animationSpec = FinTrackMotion.InteractiveSpring,
        label = "tactilePressScale"
    )
    this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .then(
            if (interactionSource == null && enabled) {
                Modifier.pointerInput(Unit) {
                    awaitEachGesture {
                        awaitFirstDown(requireUnconsumed = false)
                        isPointerPressed = true
                        waitForUpOrCancellation()
                        isPointerPressed = false
                    }
                }
            } else {
                Modifier
            }
        )
}
