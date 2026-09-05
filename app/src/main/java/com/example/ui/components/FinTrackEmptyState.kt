package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.ui.theme.BodyRegular
import com.example.ui.theme.CardTitleAmount
import com.example.ui.theme.CobaltBlue
import com.example.ui.theme.SectionHeadline
import com.example.ui.theme.Space12
import com.example.ui.theme.Space16
import com.example.ui.theme.Space24
import com.example.ui.theme.Space4
import com.example.ui.theme.Space8
import com.example.ui.theme.SurfaceContainerDark
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

/**
 * Reusable empty state primitive for FinTrack Design System v1.
 * Provides a tonal icon well, clear typographic hierarchy, and an optional action trigger.
 * Supports compact display for inline containers such as chart cards.
 */
@Composable
fun FinTrackEmptyState(
    title: String,
    description: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    iconTint: Color = TextSecondary,
    iconContainerColor: Color = SurfaceContainerDark,
    compact: Boolean = false,
    actionLabel: String? = null,
    onActionClick: (() -> Unit)? = null
) {
    val containerPadding = if (compact) Space12 else Space24
    val iconBoxSize = if (compact) 44.dp else 64.dp
    val iconSize = if (compact) 22.dp else 32.dp
    val spacingAfterIcon = if (compact) Space8 else Space16
    val spacingAfterTitle = if (compact) Space4 else Space8
    val spacingBeforeAction = if (compact) Space12 else Space24
    val titleStyle = if (compact) CardTitleAmount else SectionHeadline

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(containerPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (icon != null) {
            Box(
                modifier = Modifier
                    .size(iconBoxSize)
                    .background(iconContainerColor, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(iconSize)
                )
            }
            Spacer(modifier = Modifier.height(spacingAfterIcon))
        }

        Text(
            text = title,
            style = titleStyle,
            color = TextPrimary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(spacingAfterTitle))

        Text(
            text = description,
            style = BodyRegular,
            color = TextSecondary,
            textAlign = TextAlign.Center
        )

        if (actionLabel != null && onActionClick != null) {
            Spacer(modifier = Modifier.height(spacingBeforeAction))
            FinTrackButton(
                text = actionLabel,
                onClick = onActionClick,
                variant = ButtonVariant.PRIMARY
            )
        }
    }
}

/**
 * Reusable loading state primitive for FinTrack Design System v1.
 */
@Composable
fun FinTrackLoadingState(
    message: String = "Loading...",
    modifier: Modifier = Modifier,
    indicatorColor: Color = CobaltBlue,
    testTag: String = "fintrack_loading_indicator"
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(Space24),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator(
            modifier = Modifier
                .size(36.dp)
                .testTag(testTag),
            color = indicatorColor,
            strokeWidth = 3.dp
        )
        Spacer(modifier = Modifier.height(Space12))
        Text(
            text = message,
            style = BodyRegular,
            color = TextSecondary,
            textAlign = TextAlign.Center
        )
    }
}

