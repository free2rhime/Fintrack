package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.ui.theme.BodyRegular
import com.example.ui.theme.SectionHeadline
import com.example.ui.theme.Space16
import com.example.ui.theme.Space24
import com.example.ui.theme.Space8
import com.example.ui.theme.SurfaceContainerDark
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

/**
 * Reusable empty state primitive for FinTrack Design System v1.
 * Provides a tonal icon well, clear typographic hierarchy, and an optional action trigger.
 */
@Composable
fun FinTrackEmptyState(
    title: String,
    description: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    iconTint: Color = TextSecondary,
    actionLabel: String? = null,
    onActionClick: (() -> Unit)? = null
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(Space24),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (icon != null) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .background(SurfaceContainerDark, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(32.dp)
                )
            }
            Spacer(modifier = Modifier.height(Space16))
        }

        Text(
            text = title,
            style = SectionHeadline,
            color = TextPrimary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(Space8))

        Text(
            text = description,
            style = BodyRegular,
            color = TextSecondary,
            textAlign = TextAlign.Center
        )

        if (actionLabel != null && onActionClick != null) {
            Spacer(modifier = Modifier.height(Space24))
            FinTrackButton(
                text = actionLabel,
                onClick = onActionClick,
                variant = ButtonVariant.PRIMARY
            )
        }
    }
}
