package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.domain.analytics.FinancialAnalyticsEngine
import com.example.ui.theme.CobaltBlue
import com.example.ui.theme.LabelBadgeMedium
import com.example.ui.theme.RadiusMedium
import com.example.ui.theme.RadiusSmall
import com.example.ui.theme.Space12
import com.example.ui.theme.Space16
import com.example.ui.theme.Space8
import com.example.ui.theme.SurfaceContainerDark
import com.example.ui.theme.SurfaceContainerHighDark
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

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

    Box(modifier = modifier) {
        Surface(
            onClick = { expanded = !expanded },
            shape = RoundedCornerShape(RadiusMedium),
            color = SurfaceContainerDark,
            border = BorderStroke(1.dp, SurfaceContainerHighDark),
            modifier = Modifier
                .defaultMinSize(minHeight = 40.dp)
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
                    tint = CobaltBlue,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(Space8))
                Text(
                    text = currentDisplayName,
                    style = LabelBadgeMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.width(Space8))
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = "Select period",
                    tint = TextSecondary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier
                .background(SurfaceDark)
                .border(1.dp, SurfaceContainerHighDark, RoundedCornerShape(RadiusSmall))
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
                                color = if (isSelected) CobaltBlue else TextPrimary,
                                modifier = Modifier.weight(1f)
                            )
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = CobaltBlue,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    },
                    onClick = {
                        onPeriodSelected(period)
                        expanded = false
                    },
                    modifier = Modifier
                        .testTag("period_chip_$period")
                        .testTag("period_chip_$normalizedTag")
                        .defaultMinSize(minHeight = 44.dp)
                )
            }
        }
    }
}

/**
 * Reusable period selector row for FinTrack Design System v1.
 * Provides a horizontally scrollable chip row with CobaltBlue selection,
 * 12dp rounded corners, minimum 48dp touch targets, and full accessibility semantics.
 */
@Composable
fun FinTrackPeriodSelector(
    selectedPeriod: String,
    onPeriodSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
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

    Row(
        modifier = modifier
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = Space16),
        horizontalArrangement = Arrangement.spacedBy(Space8)
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
                    onClick = { onPeriodSelected(period) },
                    shape = RoundedCornerShape(RadiusMedium),
                    label = {
                        Text(
                            text = displayName,
                            style = LabelBadgeMedium,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = CobaltBlue,
                        selectedLabelColor = Color.White,
                        containerColor = SurfaceContainerDark,
                        labelColor = TextSecondary
                    ),
                    modifier = Modifier
                        .defaultMinSize(minHeight = 48.dp)
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
