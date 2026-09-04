package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.theme.CobaltBlue
import com.example.ui.theme.LabelBadgeMedium
import com.example.ui.theme.RadiusMedium
import com.example.ui.theme.RadiusSmall
import com.example.ui.theme.Space2
import com.example.ui.theme.Space4
import com.example.ui.theme.SurfaceContainerDark
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

/**
 * Reusable segmented control for mutually exclusive choices in FinTrack Design System v1.
 * Provides a tonal container, CobaltBlue selected state with subtle 200ms color transition,
 * 48dp minimum touch target, and accessible selected semantics.
 */
@Composable
fun FinTrackSegmentedControl(
    items: List<String>,
    selectedIndex: Int,
    onItemSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(RadiusMedium))
            .background(SurfaceContainerDark)
            .padding(Space4),
        verticalAlignment = Alignment.CenterVertically
    ) {
        items.forEachIndexed { index, item ->
            val isSelected = index == selectedIndex

            val pillBgColor by animateColorAsState(
                targetValue = if (isSelected) CobaltBlue else Color.Transparent,
                animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
                label = "segmented_control_pill_bg"
            )

            val textColor by animateColorAsState(
                targetValue = if (isSelected) Color.White else TextSecondary,
                animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
                label = "segmented_control_text_color"
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .defaultMinSize(minHeight = 48.dp)
                    .clip(RoundedCornerShape(RadiusSmall))
                    .background(pillBgColor)
                    .clickable(role = Role.Tab) { onItemSelected(index) }
                    .semantics {
                        this.selected = isSelected
                        this.role = Role.Tab
                    }
                    .padding(horizontal = Space4, vertical = Space2),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = item,
                    style = LabelBadgeMedium,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    color = textColor
                )
            }
        }
    }
}
