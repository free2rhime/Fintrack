package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.snap
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.theme.FinTrackMotion
import com.example.ui.theme.LabelBadgeMedium
import com.example.ui.theme.RadiusMedium
import com.example.ui.theme.RadiusSmall
import com.example.ui.theme.Space2
import com.example.ui.theme.Space4
import com.example.ui.theme.isReducedMotionEnabled
import com.example.ui.theme.tactilePress

/**
 * Reusable segmented control for mutually exclusive choices in FinTrack Design System v1.
 * Provides a tonal container, primary selected state with subtle spring color transition,
 * 48dp minimum touch target for accessibility, and accessible selected semantics.
 *
 * @param isCompact When true, renders a sleek 32dp visual pill track while preserving the
 *                  full 48dp interactive touch target for accessibility.
 * @param fillMaxWidth When true, the control fills the available width and expands items equally.
 * @param itemTestTag Optional lambda to supply a testTag per item.
 */
@Composable
fun FinTrackSegmentedControl(
    items: List<String>,
    selectedIndex: Int,
    onItemSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    isCompact: Boolean = false,
    fillMaxWidth: Boolean = true,
    itemTestTag: ((index: Int, item: String) -> String)? = null
) {
    val isReducedMotion = isReducedMotionEnabled()
    val selectionSpringSpec: AnimationSpec<Color> = if (isReducedMotion) snap() else FinTrackMotion.selectionSpring()

    if (isCompact) {
        Box(
            modifier = modifier,
            contentAlignment = Alignment.Center
        ) {
            // Visual track background (32dp)
            Box(
                modifier = Modifier
                    .then(if (fillMaxWidth) Modifier.fillMaxWidth() else Modifier)
                    .height(32.dp)
                    .clip(RoundedCornerShape(RadiusMedium))
                    .background(MaterialTheme.colorScheme.surfaceContainer)
                    .padding(Space2)
            )

            // Transparent backing for sizing
            Box(
                modifier = Modifier
                    .then(if (fillMaxWidth) Modifier.fillMaxWidth() else Modifier)
                    .height(32.dp)
            ) {}

            // Interactive Row with accessible >= 48dp touch targets and selectableGroup
            Row(
                modifier = Modifier
                    .then(if (fillMaxWidth) Modifier.fillMaxWidth() else Modifier)
                    .selectableGroup(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                items.forEachIndexed { index, item ->
                    val isSelected = index == selectedIndex

                    val pillBgColor by animateColorAsState(
                        targetValue = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                        animationSpec = selectionSpringSpec,
                        label = "segmented_control_compact_pill_bg"
                    )

                    val textColor by animateColorAsState(
                        targetValue = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        animationSpec = selectionSpringSpec,
                        label = "segmented_control_compact_text_color"
                    )

                    val tagModifier = if (itemTestTag != null) {
                        Modifier.testTag(itemTestTag(index, item))
                    } else Modifier

                    Box(
                        modifier = Modifier
                            .then(if (fillMaxWidth) Modifier.weight(1f) else Modifier.defaultMinSize(minWidth = 56.dp))
                            .defaultMinSize(minHeight = 48.dp)
                            .tactilePress()
                            .clickable(role = Role.Tab) { onItemSelected(index) }
                            .semantics {
                                this.selected = isSelected
                                this.role = Role.Tab
                            }
                            .then(tagModifier)
                            .padding(horizontal = Space2),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .then(if (fillMaxWidth) Modifier.fillMaxWidth() else Modifier.defaultMinSize(minWidth = 48.dp))
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
                .then(if (fillMaxWidth) Modifier.fillMaxWidth() else Modifier)
                .clip(RoundedCornerShape(RadiusMedium))
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .selectableGroup()
                .padding(Space4),
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEachIndexed { index, item ->
                val isSelected = index == selectedIndex

                val pillBgColor by animateColorAsState(
                    targetValue = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                    animationSpec = selectionSpringSpec,
                    label = "segmented_control_pill_bg"
                )

                val textColor by animateColorAsState(
                    targetValue = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                    animationSpec = selectionSpringSpec,
                    label = "segmented_control_text_color"
                )

                val tagModifier = if (itemTestTag != null) {
                    Modifier.testTag(itemTestTag(index, item))
                } else Modifier

                Box(
                    modifier = Modifier
                        .then(if (fillMaxWidth) Modifier.weight(1f) else Modifier.defaultMinSize(minWidth = 56.dp))
                        .defaultMinSize(minHeight = 48.dp)
                        .clip(RoundedCornerShape(RadiusSmall))
                        .background(pillBgColor)
                        .tactilePress()
                        .clickable(role = Role.Tab) { onItemSelected(index) }
                        .semantics {
                            this.selected = isSelected
                            this.role = Role.Tab
                        }
                        .then(tagModifier)
                        .padding(horizontal = if (fillMaxWidth) Space4 else 14.dp, vertical = Space2),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = item,
                        style = LabelBadgeMedium,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = textColor
                    )
                }
            }
        }
    }
}
