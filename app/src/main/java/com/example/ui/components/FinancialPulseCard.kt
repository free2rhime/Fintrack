package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoGraph
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.analytics.SmartFinancialInsights
import com.example.ui.theme.BodyRegular
import com.example.ui.theme.CardTitleAmount
import com.example.ui.theme.FinTrackMotion
import com.example.ui.theme.FinTrackTheme
import com.example.ui.theme.LabelBadgeMedium
import com.example.ui.theme.MicroMetadata
import com.example.ui.theme.RadiusSmall
import com.example.ui.theme.SectionHeadline
import com.example.ui.theme.ShapePill
import com.example.ui.theme.Space12
import com.example.ui.theme.Space16
import com.example.ui.theme.Space20
import com.example.ui.theme.Space4
import com.example.ui.theme.Space8
import com.example.ui.theme.isReducedMotionEnabled
import java.util.Locale
import kotlin.math.abs

private enum class PulseMetricType {
    SAVINGS_RATIO,
    EXPENSE_VELOCITY
}

/**
 * FinancialPulseCard — Material 3 Expressive Grouped Tonal Panel (Phase 3C / M3-3).
 *
 * Stateless component consuming authoritative SmartFinancialInsights data.
 * Displays:
 * 1. Editorial narrative prose derived from SmartFinancialInsights (period-aware)
 * 2. Dual progress presentation: Savings Ratio vs Expense Velocity in expressive tonal wells
 * 3. Explainable metric affordances (accessible 48dp touch target dialog with formula and business context)
 * 4. Graceful empty/degraded state when insight data is newly initialized or missing
 *
 * Container: ShapeGroupedContainer (24dp) with surfaceContainer elevation and zero hard borders.
 */
@Composable
fun FinancialPulseCard(
    insights: SmartFinancialInsights,
    savingsRate: Double? = null,
    expenseVelocity: Double? = null,
    periodLabel: String = "This Month",
    modifier: Modifier = Modifier
) {
    val effectiveSavingsRatio: Double? = savingsRate ?: insights.savingsRatio
    val effectiveExpenseVelocity: Double? = expenseVelocity ?: insights.expenseVelocity

    val isNewlyInitialized = insights.avgMonthlyExpense == 0.0 &&
            insights.avgMonthlyIncome == 0.0 &&
            (insights.savingsTrendText == "Stable" || insights.savingsTrendText.isBlank()) &&
            effectiveSavingsRatio == null &&
            effectiveExpenseVelocity == null

    val isReducedMotion = isReducedMotionEnabled()
    var activeMetricInfo by remember { mutableStateOf<PulseMetricType?>(null) }

    // Contextual explanation dialog for metrics
    val currentMetricInfo = activeMetricInfo
    if (currentMetricInfo != null) {
        val (title, description, formula) = when (currentMetricInfo) {
            PulseMetricType.SAVINGS_RATIO -> Triple(
                "Savings Ratio",
                "Indicates the percentage of household cash inflow retained after all expenses are deducted during the active period.",
                "Formula: (Net Surplus ÷ Total Income) × 100"
            )
            PulseMetricType.EXPENSE_VELOCITY -> Triple(
                "Expense Velocity",
                "Indicates the burn rate of your income. High velocity (>80%) suggests cashflow pressure, while balanced velocity ensures capital accumulation.",
                "Formula: (Total Expenses ÷ Total Income) × 100"
            )
        }

        AlertDialog(
            onDismissRequest = { activeMetricInfo = null },
            shape = FinTrackTheme.shapes.dialog,
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            title = {
                Text(
                    text = title,
                    style = SectionHeadline,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(Space8)) {
                    Text(
                        text = description,
                        style = BodyRegular,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(RadiusSmall))
                            .background(MaterialTheme.colorScheme.surfaceContainer)
                            .padding(Space8)
                    ) {
                        Text(
                            text = formula,
                            style = MicroMetadata,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { activeMetricInfo = null },
                    modifier = Modifier.testTag("pulse_info_dialog_dismiss")
                ) {
                    Text("Got it")
                }
            },
            modifier = Modifier.testTag("pulse_metric_dialog")
        )
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("financial_pulse_card"),
        shape = FinTrackTheme.shapes.listContainer,
        color = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 1.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Space20)
        ) {
            // Header: Icon + Title
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(FinTrackTheme.colors.brandAccent.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoGraph,
                        contentDescription = "Financial Pulse",
                        tint = FinTrackTheme.colors.brandAccent,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(Space12))
                Text(
                    text = "Financial Pulse",
                    style = SectionHeadline,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(Space16))

            if (isNewlyInitialized) {
                // Graceful Degraded / Empty State
                Text(
                    text = "Financial pulse will calibrate as activity accumulates.",
                    style = BodyRegular,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.testTag("financial_pulse_empty_state")
                )
            } else {
                // Editorial Narrative Prose derived authoritatively and period-aware from SmartFinancialInsights
                val isMonthPeriod = periodLabel.equals("This Month", ignoreCase = true) ||
                        periodLabel.equals("Last Month", ignoreCase = true) ||
                        periodLabel.equals("Previous Month", ignoreCase = true)

                val narrativeBuilder = StringBuilder()
                if (effectiveSavingsRatio != null && effectiveSavingsRatio > 0.0) {
                    narrativeBuilder.append("You're saving ${formatOneDecimal(effectiveSavingsRatio)}% of household income this period.")
                } else if (effectiveSavingsRatio != null && effectiveSavingsRatio <= 0.0) {
                    narrativeBuilder.append("Spending is outpacing income in this period.")
                } else {
                    when (insights.savingsTrendText) {
                        "Strong Capital Growth" -> narrativeBuilder.append("Strong capital growth with healthy reserve allocation.")
                        "Positive Savings Rate" -> narrativeBuilder.append("Positive savings rate maintained across this period.")
                        "High Expense Ratio" -> narrativeBuilder.append("Expenses are running high relative to periodic cash inflow.")
                        else -> narrativeBuilder.append("Spending and savings are tracking stably across periods.")
                    }
                }

                if (isMonthPeriod && insights.monthOverMonthExpenseChangePercent != 0.0) {
                    val momChange = insights.monthOverMonthExpenseChangePercent
                    if (momChange > 0.0) {
                        narrativeBuilder.append(" Expenses are running +${formatOneDecimal(momChange)}% vs previous month.")
                    } else {
                        narrativeBuilder.append(" Expenses decreased by ${formatOneDecimal(abs(momChange))}% vs previous month.")
                    }
                } else if (!isMonthPeriod) {
                    if (insights.largestExpenseMonth != "N/A" && insights.largestExpenseMonth.isNotBlank()) {
                        narrativeBuilder.append(" Peak monthly outflow observed in ${insights.largestExpenseMonth}.")
                    } else {
                        narrativeBuilder.append(" Expense trend is available for the selected period.")
                    }
                } else if (insights.largestExpenseMonth != "N/A" && insights.largestExpenseMonth.isNotBlank()) {
                    narrativeBuilder.append(" Peak monthly outflow observed in ${insights.largestExpenseMonth}.")
                }

                val narrativeText = narrativeBuilder.toString().trim()

                Text(
                    text = narrativeText,
                    style = BodyRegular,
                    color = MaterialTheme.colorScheme.onSurface,
                    lineHeight = 22.sp,
                    modifier = Modifier.testTag("financial_pulse_narrative")
                )

                // Dual Progress Presentation (Savings Ratio vs Expense Velocity)
                val hasSavings = effectiveSavingsRatio != null
                val hasVelocity = effectiveExpenseVelocity != null

                if (hasSavings || hasVelocity) {
                    Spacer(modifier = Modifier.height(Space16))

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(Space12)
                    ) {
                        // 1. Savings Ratio Bar with Info Affordance
                        if (effectiveSavingsRatio != null) {
                            val ratioValue = effectiveSavingsRatio
                            val clampedSavings = (ratioValue / 100.0).coerceIn(0.0, 1.0).toFloat()
                            val animatedSavings by animateFloatAsState(
                                targetValue = clampedSavings,
                                animationSpec = if (isReducedMotion) snap() else FinTrackMotion.ContentSpring,
                                label = "savings_progress"
                            )

                            PulseMetricBar(
                                label = "Savings Ratio",
                                formattedValue = "${formatOneDecimal(ratioValue)}%",
                                progress = animatedSavings,
                                progressColor = FinTrackTheme.colors.income,
                                valueTestTag = "financial_pulse_savings_ratio",
                                barTestTag = "pulse_savings_progress",
                                infoTestTag = "pulse_savings_ratio_info",
                                onInfoClick = { activeMetricInfo = PulseMetricType.SAVINGS_RATIO },
                                accessibilityDesc = "Savings Ratio: ${formatOneDecimal(ratioValue)} percent"
                            )
                        }

                        // 2. Expense Velocity Bar with Info Affordance
                        if (effectiveExpenseVelocity != null) {
                            val velocityValue = effectiveExpenseVelocity
                            val clampedVelocity = (abs(velocityValue) / 100.0).coerceIn(0.0, 1.0).toFloat()
                            val animatedVelocity by animateFloatAsState(
                                targetValue = clampedVelocity,
                                animationSpec = if (isReducedMotion) snap() else FinTrackMotion.ContentSpring,
                                label = "velocity_progress"
                            )

                            val velocityColor = if (velocityValue > 80.0 || velocityValue > 20.0) {
                                FinTrackTheme.colors.healthWarning
                            } else {
                                FinTrackTheme.colors.healthPositive
                            }

                            val formattedVelocityStr = if (velocityValue > 0.0) "+${formatOneDecimal(velocityValue)}%" else "${formatOneDecimal(velocityValue)}%"

                            PulseMetricBar(
                                label = "Expense Velocity",
                                formattedValue = formattedVelocityStr,
                                progress = animatedVelocity,
                                progressColor = velocityColor,
                                valueTestTag = "financial_pulse_expense_velocity",
                                barTestTag = "pulse_velocity_progress",
                                infoTestTag = "pulse_expense_velocity_info",
                                onInfoClick = { activeMetricInfo = PulseMetricType.EXPENSE_VELOCITY },
                                accessibilityDesc = "Expense Velocity: $formattedVelocityStr"
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PulseMetricBar(
    label: String,
    formattedValue: String,
    progress: Float,
    progressColor: Color,
    valueTestTag: String,
    barTestTag: String,
    infoTestTag: String,
    onInfoClick: () -> Unit,
    accessibilityDesc: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .padding(horizontal = Space16, vertical = Space12)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = label,
                        style = LabelBadgeMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    IconButton(
                        onClick = onInfoClick,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag(infoTestTag)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Explain $label",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Text(
                    text = formattedValue,
                    style = CardTitleAmount,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.testTag(valueTestTag)
                )
            }

            Spacer(modifier = Modifier.height(Space4))

            // Material 3 Expressive pill progress indicator
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(ShapePill)
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                    .semantics {
                        contentDescription = accessibilityDesc
                    }
                    .testTag(barTestTag)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress)
                        .height(8.dp)
                        .clip(ShapePill)
                        .background(progressColor)
                )
            }
        }
    }
}

private fun formatOneDecimal(value: Double): String {
    return String.format(Locale.US, "%.1f", value)
}

