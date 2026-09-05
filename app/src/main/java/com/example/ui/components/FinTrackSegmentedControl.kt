package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
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
import com.example.ui.theme.FinTrackMotion
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
 * 48dp minimum touch target for accessibility, and accessible selected semantics.
 *
 * @param isCompact When true, renders a sleek 32dp visual pill track while preserving the
 *                  full 48dp interactive touch target for accessibility.
 */
@Composable
fun FinTrackSegmentedControl(
    items: List<String>,
    selectedIndex: Int,
    onItemSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    isCompact: Boolean = false
) {
    if (isCompact) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 48.dp),
            contentAlignment = Alignment.Center
        ) {
            // Compact visual track (32dp height, subtle rounded corners)
            Surface(
                shape = RoundedCornerShape(RadiusMedium),
                color = SurfaceContainerDark,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(32.dp)
            ) {}

            // Interactive Row with accessible >= 48dp touch targets
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                items.forEachIndexed { index, item ->
                    val isSelected = index == selectedIndex

                    val pillBgColor by animateColorAsState(
                        targetValue = if (isSelected) CobaltBlue else Color.Transparent,
                        animationSpec = FinTrackMotion.standardTween(),
                        label = "segmented_control_compact_pill_bg"
                    )

                    val textColor by animateColorAsState(
                        targetValue = if (isSelected) TextPrimary else TextSecondary,
                        animationSpec = FinTrackMotion.standardTween(),
                        label = "segmented_control_compact_text_color"
                    )

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .defaultMinSize(minHeight = 48.dp)
                            .clickable(role = Role.Tab) { onItemSelected(index) }
                            .semantics {
                                this.selected = isSelected
                                this.role = Role.Tab
                            }
                            .padding(horizontal = Space2),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(28.dp)
                                .clip(RoundedCornerShape(RadiusSmall))
                                .background(pillBgColor),
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
        }
    } else {
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
                    animationSpec = FinTrackMotion.standardTween(),
                    label = "segmented_control_pill_bg"
                )

                val textColor by animateColorAsState(
                    targetValue = if (isSelected) Color.White else TextSecondary,
                    animationSpec = FinTrackMotion.standardTween(),
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
}
