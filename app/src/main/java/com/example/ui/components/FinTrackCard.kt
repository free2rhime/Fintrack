package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.RadiusLarge
import com.example.ui.theme.Space16
import com.example.ui.theme.SurfaceContainerDark
import com.example.ui.theme.SurfaceDark

/**
 * Reusable tonal card primitive for FinTrack Design System v1.
 * Adheres strictly to Material 3 tonal surface elevation and 8dp grid spacing.
 */
@Composable
fun FinTrackCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(RadiusLarge),
    containerColor: Color = SurfaceDark,
    contentColor: Color = Color.Unspecified,
    border: BorderStroke? = null,
    tonalElevation: Dp = 0.dp,
    contentPadding: Dp = Space16,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = if (onClick != null) {
            modifier.clickable(onClick = onClick)
        } else {
            modifier
        },
        shape = shape,
        color = containerColor,
        contentColor = contentColor,
        tonalElevation = tonalElevation,
        border = border
    ) {
        Box(modifier = Modifier.padding(contentPadding)) {
            content()
        }
    }
}
