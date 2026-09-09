package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.analytics.DashboardMetrics
import com.example.ui.screens.formatCeilDigits
import com.example.ui.theme.BreakpointCompactWidth
import com.example.ui.theme.CurrencyUnitDecoupled
import com.example.ui.theme.ExpenseContainerDark
import com.example.ui.theme.ExpenseDark
import com.example.ui.theme.FinTrackMotion
import com.example.ui.theme.FinTrackTheme
import com.example.ui.theme.HealthCriticalDark
import com.example.ui.theme.HealthPositiveDark
import com.example.ui.theme.HeroFinancialDisplay
import com.example.ui.theme.HeroFinancialDisplayCompact
import com.example.ui.theme.IncomeContainerDark
import com.example.ui.theme.IncomeDark
import com.example.ui.theme.LabelBadge
import com.example.ui.theme.LabelMicro
import com.example.ui.theme.MetricFinancialMedium
import com.example.ui.theme.MetricFinancialRegular
import com.example.ui.theme.ShapeHero
import com.example.ui.theme.ShapeInset
import com.example.ui.theme.ShapePill
import com.example.ui.theme.Space12
import com.example.ui.theme.Space16
import com.example.ui.theme.Space2
import com.example.ui.theme.Space20
import com.example.ui.theme.Space4
import com.example.ui.theme.Space8
import com.example.ui.theme.SurfaceHeroBorder
import com.example.ui.theme.TextMutedDark
import com.example.ui.theme.TextPrimaryDark
import com.example.ui.theme.TextSecondaryDark
import com.example.ui.theme.isReducedMotionEnabled

/**
 * Unified Hero Canvas — FinTrack Phase 3B Checkpoint 4.1
 * Visual Direction: Precision Fintech + Editorial Wealth Narrative
 *
 * Visually unifies:
 * 1. Financial Apex / Net Balance (prominent tabular figures, decoupled currency)
 * 2. Secondary currency equivalence (e.g., ≈ 25 900 EUR or restrained unavailable state)
 * 3. Authoritative BNR Parity reference (1 EUR = X.XXXX RON or BNR unavailable)
 * 4. Income & Expense cash flow well (Emerald / Coral indicators on dark canvas)
 * 5. Period context badge
 * 6. Editorial wealth narrative layer
 *
 * Presentation-only component: consumes authoritative DashboardMetrics.
 * Strict non-truncation guarantee across 360dp, 390dp, and 412dp+ viewports.
 */
@Composable
fun UnifiedHeroCanvas(
    metrics: DashboardMetrics,
    modifier: Modifier = Modifier
) {
    val reducedMotion = isReducedMotionEnabled()

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("dashboard_top_card"),
        shape = ShapeHero,
        color = FinTrackTheme.colors.surfaceHero,
        border = BorderStroke(1.dp, SurfaceHeroBorder)
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Space20)
                .testTag("unified_hero_canvas")
        ) {
            val availableWidth = maxWidth
            val isCompactWidth = availableWidth < BreakpointCompactWidth

            Column(modifier = Modifier.fillMaxWidth()) {
                // Header Row: "NET BALANCE" + Period Context Badge
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "NET BALANCE",
                        style = LabelBadge,
                        color = TextSecondaryDark,
                        letterSpacing = 1.sp
                    )

                    if (metrics.periodLabel.isNotBlank()) {
                        Box(
                            modifier = Modifier
                                .clip(ShapePill)
                                .background(Color(0xFF1E293B))
                                .padding(horizontal = Space8, vertical = Space2)
                                .testTag("hero_period_badge"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = metrics.periodLabel,
                                style = LabelMicro,
                                fontWeight = FontWeight.SemiBold,
                                color = TextSecondaryDark
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(Space8))

                // Primary Net Balance (Financial Apex)
                val isNegative = metrics.balance < 0.0
                val balancePrefix = if (isNegative) "-" else ""
                val balanceDigits = formatCeilDigits(metrics.balance)
                val balanceText = buildAnnotatedString {
                    append(balancePrefix)
                    append(balanceDigits)
                    append(" ")
                    withStyle(
                        SpanStyle(
                            fontSize = CurrencyUnitDecoupled.fontSize,
                            fontWeight = CurrencyUnitDecoupled.fontWeight,
                            letterSpacing = CurrencyUnitDecoupled.letterSpacing,
                            color = TextSecondaryDark
                        )
                    ) {
                        append(metrics.currency)
                    }
                }

                // Choose display style adaptively to prevent any overflow at 360dp
                val displayStyle = if (isCompactWidth || balanceDigits.length > 8) {
                    HeroFinancialDisplayCompact
                } else {
                    HeroFinancialDisplay
                }

                AnimatedContent(
                    targetState = "$balancePrefix$balanceDigits ${metrics.currency}",
                    transitionSpec = {
                        if (reducedMotion) {
                            ContentTransform(EnterTransition.None, ExitTransition.None)
                        } else {
                            FinTrackMotion.contentFade()
                        }
                    },
                    label = "hero_net_balance_animation"
                ) { _ ->
                    Text(
                        text = balanceText,
                        style = displayStyle,
                        color = TextPrimaryDark,
                        softWrap = true,
                        maxLines = 2,
                        overflow = TextOverflow.Clip,
                        modifier = Modifier.testTag("hero_primary_balance")
                    )
                }

                Spacer(modifier = Modifier.height(Space4))

                // Secondary Currency Equivalence Row
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.testTag("hero_secondary_currency")
                ) {
                    if (metrics.secondaryCurrencyBalance != null) {
                        val secNegative = metrics.secondaryCurrencyBalance!! < 0.0
                        val secPrefix = if (secNegative) "-" else ""
                        val secDigits = formatCeilDigits(metrics.secondaryCurrencyBalance!!)
                        val secText = buildAnnotatedString {
                            append("≈ ")
                            append(secPrefix)
                            append(secDigits)
                            append(" ")
                            withStyle(
                                SpanStyle(
                                    fontSize = CurrencyUnitDecoupled.fontSize,
                                    fontWeight = CurrencyUnitDecoupled.fontWeight,
                                    color = TextMutedDark
                                )
                            ) {
                                append(metrics.secondaryCurrency)
                            }
                        }
                        Text(
                            text = secText,
                            style = MetricFinancialRegular,
                            color = TextSecondaryDark
                        )
                    } else {
                        Text(
                            text = "≈ ${metrics.secondaryCurrency} unavailable",
                            style = LabelMicro,
                            color = TextMutedDark
                        )
                    }
                }

                Spacer(modifier = Modifier.height(Space4))

                // Editorial Wealth Narrative Row
                val narrativeText = when {
                    metrics.balance > 0.0 -> "Net surplus this period"
                    metrics.balance < 0.0 -> "Net deficit this period"
                    else -> "Balanced net position"
                }
                Text(
                    text = narrativeText,
                    style = LabelMicro,
                    color = if (metrics.balance >= 0.0) HealthPositiveDark else HealthCriticalDark,
                    modifier = Modifier.testTag("hero_wealth_narrative")
                )

                Spacer(modifier = Modifier.height(Space8))

                // BNR Parity Informational Row
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(ShapeInset)
                        .background(Color(0xFF1E293B).copy(alpha = 0.6f))
                        .padding(horizontal = Space8, vertical = Space4)
                        .testTag("hero_bnr_parity")
                ) {
                    if (metrics.latestBnrRate != null) {
                        val rateFormatted = String.format(java.util.Locale.US, "%.4f", metrics.latestBnrRate)
                        val dateSuffix = if (!metrics.effectiveBnrDate.isNullOrBlank()) " • ${metrics.effectiveBnrDate}" else ""
                        Text(
                            text = "BNR • 1 EUR = $rateFormatted RON$dateSuffix",
                            style = LabelMicro,
                            color = TextSecondaryDark
                        )
                    } else {
                        Text(
                            text = "BNR unavailable",
                            style = LabelMicro,
                            color = TextMutedDark
                        )
                    }
                }

                Spacer(modifier = Modifier.height(Space16))

                // Income / Expense Unified Well
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(ShapeInset)
                        .background(Color(0xFF131B2E))
                        .padding(horizontal = Space12, vertical = Space12)
                        .testTag("hero_cashflow_row")
                ) {
                    val useStackedCashflow = availableWidth < 300.dp || (availableWidth < 350.dp && (metrics.totalIncome >= 100_000.0 || metrics.totalExpense >= 100_000.0))

                    if (useStackedCashflow) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(Space8)
                        ) {
                            // Income Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f, fill = false)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(CircleShape)
                                            .background(IncomeContainerDark),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ArrowDownward,
                                            contentDescription = "Income",
                                            tint = IncomeDark,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(Space8))
                                    Text(
                                        text = "Income",
                                        style = LabelBadge,
                                        color = TextSecondaryDark
                                    )
                                }
                                Spacer(modifier = Modifier.width(Space8))
                                HeroCashflowAmount(
                                    prefix = "+",
                                    amount = metrics.totalIncome,
                                    currency = metrics.currency,
                                    color = IncomeDark,
                                    reducedMotion = reducedMotion
                                )
                            }

                            // Expense Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f, fill = false)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(CircleShape)
                                            .background(ExpenseContainerDark),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ArrowUpward,
                                            contentDescription = "Expense",
                                            tint = ExpenseDark,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(Space8))
                                    Text(
                                        text = "Expense",
                                        style = LabelBadge,
                                        color = TextSecondaryDark
                                    )
                                }
                                Spacer(modifier = Modifier.width(Space8))
                                HeroCashflowAmount(
                                    prefix = "-",
                                    amount = metrics.totalExpense,
                                    currency = metrics.currency,
                                    color = ExpenseDark,
                                    reducedMotion = reducedMotion
                                )
                            }
                        }
                    } else {
                        // Side-by-Side Dual Column
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Left: Income
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(IncomeContainerDark),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ArrowDownward,
                                        contentDescription = "Income",
                                        tint = IncomeDark,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(Space8))
                                Column(modifier = Modifier.weight(1f, fill = false)) {
                                    Text(
                                        text = "Income",
                                        style = LabelBadge,
                                        color = TextSecondaryDark
                                    )
                                    Spacer(modifier = Modifier.height(Space2))
                                    HeroCashflowAmount(
                                        prefix = "+",
                                        amount = metrics.totalIncome,
                                        currency = metrics.currency,
                                        color = IncomeDark,
                                        reducedMotion = reducedMotion
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(Space12))

                            // Right: Expense
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.End
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(ExpenseContainerDark),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ArrowUpward,
                                        contentDescription = "Expense",
                                        tint = ExpenseDark,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(Space8))
                                Column(
                                    modifier = Modifier.weight(1f, fill = false),
                                    horizontalAlignment = Alignment.End
                                ) {
                                    Text(
                                        text = "Expense",
                                        style = LabelBadge,
                                        color = TextSecondaryDark
                                    )
                                    Spacer(modifier = Modifier.height(Space2))
                                    HeroCashflowAmount(
                                        prefix = "-",
                                        amount = metrics.totalExpense,
                                        currency = metrics.currency,
                                        color = ExpenseDark,
                                        reducedMotion = reducedMotion
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HeroCashflowAmount(
    prefix: String,
    amount: Double,
    currency: String,
    color: Color,
    reducedMotion: Boolean,
    modifier: Modifier = Modifier
) {
    val digits = formatCeilDigits(amount)
    val annotatedText = buildAnnotatedString {
        append(prefix)
        append(digits)
        append(" ")
        withStyle(
            SpanStyle(
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = TextSecondaryDark
            )
        ) {
            append(currency)
        }
    }

    // Adaptive style: if figure has 7+ digits, use MetricFinancialRegular (15sp), else MetricFinancialMedium (20sp)
    val style = if (digits.length > 8) MetricFinancialRegular else MetricFinancialMedium

    AnimatedContent(
        targetState = "$prefix$digits $currency",
        transitionSpec = {
            if (reducedMotion) {
                ContentTransform(EnterTransition.None, ExitTransition.None)
            } else {
                FinTrackMotion.contentFade()
            }
        },
        label = "hero_cashflow_${prefix}_anim"
    ) { _ ->
        Text(
            text = annotatedText,
            style = style,
            color = color,
            softWrap = true,
            maxLines = 2,
            overflow = TextOverflow.Clip,
            modifier = modifier
        )
    }
}
