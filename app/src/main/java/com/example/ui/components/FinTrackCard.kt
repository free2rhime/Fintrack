package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.FinTrackTheme
import com.example.ui.theme.RadiusCard
import com.example.ui.theme.Space16
import com.example.ui.theme.tactilePress

/**
 * Reusable surface card primitive for FinTrack Design System v2.
 * Adheres strictly to Material 3 surface elevation, 1dp subtle micro-border,
 * spring-based tactile press feedback, and 8dp grid spacing discipline.
 */
@Composable
fun FinTrackCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(RadiusCard),
    containerColor: Color = Color.Unspecified,
    contentColor: Color = Color.Unspecified,
    border: BorderStroke? = null,
    showBorder: Boolean = true,
    tonalElevation: Dp = 0.dp,
    contentPadding: Dp = Space16,
    onClick: (() -> Unit)? = null,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    content: @Composable () -> Unit
) {
    val resolvedContainerColor = if (containerColor != Color.Unspecified) {
        containerColor
    } else {
        FinTrackTheme.colors.surfacePrimary
    }

    val resolvedBorder = if (showBorder) {
        border ?: BorderStroke(1.dp, FinTrackTheme.colors.borderSubtle)
    } else {
        border
    }

    if (onClick != null) {
        Surface(
            onClick = onClick,
            modifier = modifier.tactilePress(interactionSource = interactionSource, enabled = true),
            shape = shape,
            color = resolvedContainerColor,
            contentColor = if (contentColor != Color.Unspecified) contentColor else MaterialTheme.colorScheme.onSurface,
            tonalElevation = tonalElevation,
            border = resolvedBorder,
            interactionSource = interactionSource
        ) {
            Box(modifier = Modifier.padding(contentPadding)) {
                content()
            }
        }
    } else {
        Surface(
            modifier = modifier,
            shape = shape,
            color = resolvedContainerColor,
            contentColor = if (contentColor != Color.Unspecified) contentColor else MaterialTheme.colorScheme.onSurface,
            tonalElevation = tonalElevation,
            border = resolvedBorder
        ) {
            Box(modifier = Modifier.padding(contentPadding)) {
                content()
            }
        }
    }
}

