package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.ui.theme.FinTrackMotion
import com.example.ui.theme.FinTrackTheme
import com.example.ui.theme.LabelBadgeMedium
import com.example.ui.theme.ShapeBadgeOrganic
import com.example.ui.theme.Space4
import com.example.ui.theme.Space8
import com.example.ui.theme.isReducedMotionEnabled

enum class BadgeVariant {
    SUCCESS,
    WARNING,
    ERROR,
    INFORMATIONAL,
    NEUTRAL,
    SYNCING
}

/**
 * Reusable semantic status badge for FinTrack Design System v1.
 * Combines a vector icon, text label, and tonal container with full accessibility semantics.
 * Never uses emoji characters.
 */
@Composable
fun FinTrackStatusBadge(
    label: String,
    modifier: Modifier = Modifier,
    variant: BadgeVariant = BadgeVariant.NEUTRAL,
    icon: ImageVector? = null,
    contentDescription: String? = null
) {
    val (defaultIcon, iconColor, containerColor, textColor) = when (variant) {
        BadgeVariant.SUCCESS -> Quad(
            Icons.Default.CheckCircle,
            FinTrackTheme.colors.income,
            FinTrackTheme.colors.incomeContainer,
            FinTrackTheme.colors.income
        )
        BadgeVariant.WARNING -> Quad(
            Icons.Default.Warning,
            FinTrackTheme.colors.warning,
            FinTrackTheme.colors.warningContainer,
            FinTrackTheme.colors.warning
        )
        BadgeVariant.ERROR -> Quad(
            Icons.Default.Error,
            MaterialTheme.colorScheme.error,
            MaterialTheme.colorScheme.errorContainer,
            MaterialTheme.colorScheme.error
        )
        BadgeVariant.INFORMATIONAL -> Quad(
            Icons.Default.Info,
            FinTrackTheme.colors.info,
            FinTrackTheme.colors.infoContainer,
            FinTrackTheme.colors.info
        )
        BadgeVariant.NEUTRAL -> Quad(
            Icons.Default.Info,
            MaterialTheme.colorScheme.onSurfaceVariant,
            MaterialTheme.colorScheme.surfaceContainerHigh,
            MaterialTheme.colorScheme.onSurface
        )
        BadgeVariant.SYNCING -> Quad(
            Icons.Default.Sync,
            MaterialTheme.colorScheme.primary,
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.primary
        )
    }

    val isReducedMotion = isReducedMotionEnabled()
    val colorSpec: AnimationSpec<Color> = if (isReducedMotion) snap() else FinTrackMotion.fastTween()

    val animatedContainerColor by animateColorAsState(
        targetValue = containerColor,
        animationSpec = colorSpec,
        label = "badge_container_color"
    )
    val animatedTextColor by animateColorAsState(
        targetValue = textColor,
        animationSpec = colorSpec,
        label = "badge_text_color"
    )
    val animatedIconColor by animateColorAsState(
        targetValue = iconColor,
        animationSpec = colorSpec,
        label = "badge_icon_color"
    )

    val scale = remember { Animatable(1f) }
    LaunchedEffect(variant) {
        if (!isReducedMotion && variant == BadgeVariant.SUCCESS) {
            scale.animateTo(1.08f, animationSpec = FinTrackMotion.microTween())
            scale.animateTo(1f, animationSpec = FinTrackMotion.interactiveSpring())
        }
    }

    val displayIcon = icon ?: defaultIcon

    // Subtle 1000ms functional rotation for syncing status
    val iconModifier = if (variant == BadgeVariant.SYNCING) {
        val infiniteTransition = rememberInfiniteTransition(label = "badge_sync_spin")
        val rotation by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(
                    durationMillis = FinTrackMotion.DurationSyncSpin,
                    easing = FinTrackMotion.LinearCurve
                ),
                repeatMode = RepeatMode.Restart
            ),
            label = "badge_sync_rotation"
        )
        Modifier.size(14.dp).rotate(rotation)
    } else {
        Modifier.size(14.dp)
    }

    val badgeSemantics = contentDescription ?: "$label status: ${variant.name.lowercase()}"

    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale.value
                scaleY = scale.value
            }
            .clip(ShapeBadgeOrganic)
            .background(animatedContainerColor)
            .padding(horizontal = Space8, vertical = Space4)
            .semantics { this.contentDescription = badgeSemantics },
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = displayIcon,
                contentDescription = null, // decorative within semantic container
                tint = animatedIconColor,
                modifier = iconModifier
            )
            Spacer(modifier = Modifier.width(Space4))
            Text(
                text = label,
                style = LabelBadgeMedium,
                color = animatedTextColor
            )
        }
    }
}

private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
