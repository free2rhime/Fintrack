package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.unit.dp
import com.example.domain.analytics.FinancialAnalyticsEngine
import com.example.ui.theme.FinTrackMotion
import com.example.ui.theme.LabelBadgeMedium
import com.example.ui.theme.RadiusMedium
import com.example.ui.theme.RadiusSmall
import com.example.ui.theme.Space12
import com.example.ui.theme.Space16
import com.example.ui.theme.Space8
import com.example.ui.theme.isReducedMotionEnabled
import com.example.ui.theme.tactilePress

/**
 * Compact Material 3 Period Dropdown Selector for FinTrack Design System v1.
 * Shows currently selected period with calendar icon and dropdown arrow.
 * Opens an anchored DropdownMenu with all time ranges.
 */
@Composable
fun FinTrackPeriodDropdown(
    selectedPeriod: String,
    onPeriodSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val periods = listOf(
        "Last Month",
        "Previous Month",
        "Last 3 Months",
        "Last 6 Months",
        "Last 12 Months",
        "Year To Date",
        "All Time",
        "Custom Range"
    )
    var expanded by remember { mutableStateOf(false) }
    val currentDisplayName = FinancialAnalyticsEngine.getPeriodDisplayName(selectedPeriod)

    val reducedMotion = isReducedMotionEnabled()
    val arrowRotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = if (reducedMotion) snap() else FinTrackMotion.fastTween(),
        label = "period_dropdown_arrow_rotation"
    )

    Box(modifier = modifier) {
        Surface(
            onClick = { expanded = !expanded },
            shape = RoundedCornerShape(RadiusMedium),
            color = MaterialTheme.colorScheme.surfaceContainer,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier
                .defaultMinSize(minHeight = 48.dp)
                .tactilePress()
                .testTag("period_selector_dropdown")
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.padding(horizontal = Space12, vertical = Space8)
            ) {
                Icon(
                    imageVector = Icons.Default.CalendarToday,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(Space8))
                Text(
                    text = currentDisplayName,
                    style = LabelBadgeMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.width(Space8))
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = "Select period",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .size(18.dp)
                        .rotate(arrowRotation)
                )
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(RadiusSmall))
        ) {
            periods.forEach { period ->
                val isSelected = period == selectedPeriod
                val displayName = FinancialAnalyticsEngine.getPeriodDisplayName(period)
                val normalizedTag = period.replace(" ", "_")

                DropdownMenuItem(
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = displayName,
                                style = LabelBadgeMedium,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    },
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onPeriodSelected(period)
                        expanded = false
                    },
                    modifier = Modifier
                        .testTag("period_chip_$period")
                        .testTag("period_chip_$normalizedTag")
                        .defaultMinSize(minHeight = 48.dp)
                )
            }
        }
    }
}

/**
 * Reusable period selector row for FinTrack Design System v1.
 * Provides a horizontally scrollable chip row with primary theme selection,
 * 12dp rounded corners, minimum 48dp touch targets, tactile press feedback,
 * and full accessibility semantics.
 */
@Composable
fun FinTrackPeriodSelector(
    selectedPeriod: String,
    onPeriodSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val periods = listOf(
        "Last Month",
        "Previous Month",
        "Last 3 Months",
        "Last 6 Months",
        "Last 12 Months",
        "Year To Date",
        "All Time",
        "Custom Range"
    )

    BoxWithConstraints(
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .widthIn(min = maxWidth)
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = Space16)
                .selectableGroup(),
            horizontalArrangement = Arrangement.spacedBy(Space8, Alignment.CenterHorizontally)
        ) {
            periods.forEach { period ->
                val isSelected = period == selectedPeriod
                val displayName = FinancialAnalyticsEngine.getPeriodDisplayName(period)
                val normalizedTag = period.replace(" ", "_")

                // Outer box tagged with literal period name format "period_chip_<period>"
                Box(
                    modifier = Modifier.testTag("period_chip_$period")
                ) {
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            if (!isSelected) {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            }
                            onPeriodSelected(period)
                        },
                        shape = RoundedCornerShape(RadiusMedium),
                        label = {
                            Text(
                                text = displayName,
                                style = LabelBadgeMedium,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                            containerColor = MaterialTheme.colorScheme.surfaceContainer,
                            labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        modifier = Modifier
                            .defaultMinSize(minHeight = 48.dp)
                            .tactilePress()
                            .semantics {
                                this.selected = isSelected
                                this.role = Role.Tab
                            }
                            .testTag("period_chip_$normalizedTag")
                    )
                }
            }
        }
    }
}

/**
 * Backward-compatibility wrapper for existing callers.
 */
@Composable
fun PeriodSelectorChipRow(
    selectedPeriod: String,
    onPeriodSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    FinTrackPeriodSelector(
        selectedPeriod = selectedPeriod,
        onPeriodSelected = onPeriodSelected,
        modifier = modifier
    )
}
