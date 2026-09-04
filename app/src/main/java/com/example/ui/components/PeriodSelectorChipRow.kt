package com.example.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.example.ui.theme.Space16
import com.example.ui.theme.Space8
import com.example.ui.theme.SurfaceContainerDark
import com.example.ui.theme.TextSecondary

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
