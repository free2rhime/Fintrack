package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.FinTrackTheme
import com.example.ui.theme.RadiusCard
import com.example.ui.theme.Space16

/**
 * Reusable surface card primitive for FinTrack Design System v2.
 * Adheres strictly to Material 3 surface elevation, 1dp subtle micro-border,
 * and 8dp grid spacing discipline.
 */
@Composable
fun FinTrackCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(RadiusCard),
    containerColor: Color = Color.Unspecified,
    contentColor: Color = Color.Unspecified,
    border: BorderStroke? = null,
    tonalElevation: Dp = 0.dp,
    contentPadding: Dp = Space16,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val resolvedContainerColor = if (containerColor != Color.Unspecified) {
        containerColor
    } else {
        FinTrackTheme.colors.surfacePrimary
    }

    val resolvedBorder = border ?: BorderStroke(1.dp, FinTrackTheme.colors.borderSubtle)

    Surface(
        modifier = if (onClick != null) {
            modifier.clickable(onClick = onClick)
        } else {
            modifier
        },
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

