package com.example.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.ui.theme.CobaltBlue
import com.example.ui.theme.ExpenseContainer
import com.example.ui.theme.ExpenseCoral
import com.example.ui.theme.FinTrackMotion
import com.example.ui.theme.IncomeContainer
import com.example.ui.theme.IncomeEmerald
import com.example.ui.theme.LabelBadgeMedium
import com.example.ui.theme.RadiusMedium
import com.example.ui.theme.Space4
import com.example.ui.theme.Space8
import com.example.ui.theme.SurfaceContainerHighDark
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WarningAmber

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
            IncomeEmerald,
            IncomeContainer,
            IncomeEmerald
        )
        BadgeVariant.WARNING -> Quad(
            Icons.Default.Warning,
            WarningAmber,
            WarningAmber.copy(alpha = 0.15f),
            WarningAmber
        )
        BadgeVariant.ERROR -> Quad(
            Icons.Default.Error,
            ExpenseCoral,
            ExpenseContainer,
            ExpenseCoral
        )
        BadgeVariant.INFORMATIONAL -> Quad(
            Icons.Default.Info,
            CobaltBlue,
            CobaltBlue.copy(alpha = 0.15f),
            CobaltBlue
        )
        BadgeVariant.NEUTRAL -> Quad(
            Icons.Default.Info,
            TextSecondary,
            SurfaceContainerHighDark,
            TextPrimary
        )
        BadgeVariant.SYNCING -> Quad(
            Icons.Default.Sync,
            CobaltBlue,
            CobaltBlue.copy(alpha = 0.15f),
            CobaltBlue
        )
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
            .clip(RoundedCornerShape(RadiusMedium))
            .background(containerColor)
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
                tint = iconColor,
                modifier = iconModifier
            )
            Spacer(modifier = Modifier.width(Space4))
            Text(
                text = label,
                style = LabelBadgeMedium,
                color = textColor
            )
        }
    }
}

private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
