package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.ui.theme.BodyRegular
import com.example.ui.theme.FinTrackMotion
import com.example.ui.theme.SectionHeadline
import com.example.ui.theme.ShapeSquircleIcon
import com.example.ui.theme.Space12
import com.example.ui.theme.Space16
import com.example.ui.theme.Space24
import com.example.ui.theme.Space4
import com.example.ui.theme.Space8
import com.example.ui.theme.TitleCard
import com.example.ui.theme.isReducedMotionEnabled
import kotlinx.coroutines.launch

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
    iconTint: Color = Color.Unspecified,
    iconContainerColor: Color = Color.Unspecified,
    compact: Boolean = false,
    actionLabel: String? = null,
    onActionClick: (() -> Unit)? = null
) {
    val resolvedIconTint = if (iconTint != Color.Unspecified) iconTint else MaterialTheme.colorScheme.onSurfaceVariant
    val resolvedIconContainerColor = if (iconContainerColor != Color.Unspecified) iconContainerColor else MaterialTheme.colorScheme.surfaceContainerHigh

    val containerPadding = if (compact) Space12 else Space24
    val iconBoxSize = if (compact) 44.dp else 64.dp
    val iconSize = if (compact) 22.dp else 32.dp
    val spacingAfterIcon = if (compact) Space8 else Space16
    val spacingAfterTitle = if (compact) Space4 else Space8
    val spacingBeforeAction = if (compact) Space12 else Space24
    val titleStyle = if (compact) TitleCard else SectionHeadline

    val isReducedMotion = isReducedMotionEnabled()
    val entranceAlpha = remember { Animatable(if (isReducedMotion) 1f else 0f) }
    val entranceScale = remember { Animatable(if (isReducedMotion) 1f else 0.85f) }

    LaunchedEffect(Unit) {
        if (!isReducedMotion) {
            launch {
                entranceAlpha.animateTo(
                    targetValue = 1f,
                    animationSpec = FinTrackMotion.fastTween()
                )
            }
            launch {
                entranceScale.animateTo(
                    targetValue = 1f,
                    animationSpec = FinTrackMotion.contentEntranceSpec()
                )
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                alpha = entranceAlpha.value
                scaleX = entranceScale.value
                scaleY = entranceScale.value
            }
            .padding(containerPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (icon != null) {
            Box(
                modifier = Modifier
                    .size(iconBoxSize)
                    .background(resolvedIconContainerColor, ShapeSquircleIcon),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = resolvedIconTint,
                    modifier = Modifier.size(iconSize)
                )
            }
            Spacer(modifier = Modifier.height(spacingAfterIcon))
        }

        Text(
            text = title,
            style = titleStyle,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(spacingAfterTitle))

        Text(
            text = description,
            style = BodyRegular,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
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
    indicatorColor: Color = MaterialTheme.colorScheme.primary,
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
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}
