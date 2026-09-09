package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.example.ui.theme.RadiusLarge
import com.example.ui.theme.RadiusMedium
import com.example.ui.theme.RadiusSmall
import com.example.ui.theme.SectionHeadline
import com.example.ui.theme.Space12
import com.example.ui.theme.Space16
import com.example.ui.theme.Space20
import com.example.ui.theme.Space4
import com.example.ui.theme.Space8
import com.example.ui.theme.isReducedMotionEnabled
import java.util.Locale
import kotlin.math.abs

/**
 * FinancialPulseCard — Precision Fintech + Editorial Wealth Narrative.
 *
 * Stateless component consuming authoritative SmartFinancialInsights data.
 * Displays:
 * 1. Editorial narrative prose derived from SmartFinancialInsights
 * 2. Dual progress presentation: Savings Ratio vs Expense Velocity
 * 3. Graceful empty/degraded state when insight data is newly initialized or missing
 */
@Composable
fun FinancialPulseCard(
    insights: SmartFinancialInsights,
    savingsRate: Double? = null,
    expenseVelocity: Double? = null,
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

    FinTrackCard(
        modifier = modifier
            .fillMaxWidth()
            .testTag("financial_pulse_card"),
        containerColor = FinTrackTheme.colors.surfacePrimary,
        border = BorderStroke(1.dp, FinTrackTheme.colors.borderSubtle),
        shape = RoundedCornerShape(RadiusLarge),
        contentPadding = Space16
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header: Icon + Title + Trend Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(FinTrackTheme.colors.brandAccent.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoGraph,
                            contentDescription = "Financial Pulse",
                            tint = FinTrackTheme.colors.brandAccent,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(Space8))
                    Text(
                        text = "Financial Pulse",
                        style = SectionHeadline,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                if (!isNewlyInitialized && insights.savingsTrendText.isNotBlank() && insights.savingsTrendText != "N/A") {
                    val badgeVariant = when (insights.savingsTrendText) {
                        "Strong Capital Growth" -> BadgeVariant.SUCCESS
                        "Positive Savings Rate" -> BadgeVariant.SUCCESS
                        "High Expense Ratio" -> BadgeVariant.WARNING
                        else -> BadgeVariant.NEUTRAL
                    }
                    FinTrackStatusBadge(
                        label = insights.savingsTrendText,
                        variant = badgeVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(Space12))

            if (isNewlyInitialized) {
                // Graceful Degraded / Empty State
                Text(
                    text = "Financial pulse will calibrate as activity accumulates.",
                    style = BodyRegular,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.testTag("financial_pulse_empty_state")
                )
            } else {
                // Editorial Narrative Prose derived authoritatively from SmartFinancialInsights
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

                if (insights.monthOverMonthExpenseChangePercent != 0.0) {
                    val momChange = insights.monthOverMonthExpenseChangePercent
                    if (momChange > 0.0) {
                        narrativeBuilder.append(" Expenses are running +${formatOneDecimal(momChange)}% vs previous month.")
                    } else {
                        narrativeBuilder.append(" Expenses decreased by ${formatOneDecimal(abs(momChange))}% vs previous month.")
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
                        // 1. Savings Ratio Bar
                        if (hasSavings && effectiveSavingsRatio != null) {
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
                                accessibilityDesc = "Savings Ratio: ${formatOneDecimal(ratioValue)} percent"
                            )
                        }

                        // 2. Expense Velocity Bar
                        if (hasVelocity && effectiveExpenseVelocity != null) {
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
    accessibilityDesc: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = LabelBadgeMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = formattedValue,
                style = CardTitleAmount,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.testTag(valueTestTag)
            )
        }

        Spacer(modifier = Modifier.height(Space4))

        // Restrained horizontal progress indicator
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(RadiusSmall))
                .background(FinTrackTheme.colors.surfaceSecondary)
                .semantics {
                    contentDescription = accessibilityDesc
                }
                .testTag(barTestTag)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress)
                    .height(6.dp)
                    .clip(RoundedCornerShape(RadiusSmall))
                    .background(progressColor)
            )
        }
    }
}

private fun formatOneDecimal(value: Double): String {
    return String.format(Locale.US, "%.1f", value)
}
