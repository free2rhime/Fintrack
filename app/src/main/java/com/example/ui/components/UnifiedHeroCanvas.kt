package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.analytics.DashboardMetrics
import com.example.ui.theme.BreakpointCompactWidth
import com.example.ui.theme.CurrencyUnitDecoupled
import com.example.ui.theme.FinTrackMotion
import com.example.ui.theme.FinTrackTheme
import com.example.ui.theme.HeroFinancialDisplay
import com.example.ui.theme.HeroFinancialDisplayCompact
import com.example.ui.theme.LabelBadge
import com.example.ui.theme.LabelMicro
import com.example.ui.theme.MetricFinancialMedium
import com.example.ui.theme.MetricFinancialRegular
import com.example.ui.theme.ShapePill
import com.example.ui.theme.Space12
import com.example.ui.theme.Space16
import com.example.ui.theme.Space2
import com.example.ui.theme.Space20
import com.example.ui.theme.Space24
import com.example.ui.theme.Space4
import com.example.ui.theme.Space8
import com.example.ui.theme.isReducedMotionEnabled

/**
 * Unified Hero Canvas — Material 3 Expressive (Phase 3C / M3-3)
 * Visual Direction: Material 3 Expressive + Editorial Wealth Narrative
 *
 * Visually unifies:
 * 1. Financial Apex / Net Balance (prominent tabular figures, decoupled currency)
 * 2. Secondary currency equivalence (e.g., ≈ 25 900 EUR or restrained unavailable state)
 * 3. Authoritative BNR Parity reference (1 EUR = X.XXXX RON or BNR unavailable)
 * 4. Income & Expense cash flow well (Emerald / Coral indicators on expressive tonal well)
 * 5. Period context badge (tactile pill)
 * 6. Editorial wealth narrative layer
 *
 * Presentation-only component: consumes authoritative DashboardMetrics.
 * Expressive container hierarchy with ShapeHeroCanvas (32dp bottom curve),
 * surfaceContainerLow tonal elevation, zero hard border dependency, and strict
 * non-truncation guarantees across 360dp, 390dp, and 412dp+ viewports.
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
        shape = FinTrackTheme.shapes.hero,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = 1.dp
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Space16, vertical = Space12)
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
                        color = FinTrackTheme.colors.textSecondary,
                        letterSpacing = 1.sp
                    )

                    if (metrics.periodLabel.isNotBlank()) {
                        Box(
                            modifier = Modifier
                                .clip(ShapePill)
                                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                                .padding(horizontal = Space12, vertical = Space4)
                                .testTag("hero_period_badge"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = metrics.periodLabel,
                                style = LabelMicro,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(Space4))

                // Primary Net Balance (Financial Apex)
                val isNegative = metrics.balance < 0.0
                val balancePrefix = if (isNegative) "-" else ""
                val balanceDigits = formatHeroNetBalanceDigits(metrics.balance)
                val balanceText = buildAnnotatedString {
                    append(balancePrefix)
                    append(balanceDigits)
                    append(" ")
                    withStyle(
                        SpanStyle(
                            fontSize = CurrencyUnitDecoupled.fontSize,
                            fontWeight = CurrencyUnitDecoupled.fontWeight,
                            letterSpacing = CurrencyUnitDecoupled.letterSpacing,
                            color = FinTrackTheme.colors.textSecondary
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
                        color = MaterialTheme.colorScheme.onSurface,
                        softWrap = true,
                        maxLines = 2,
                        overflow = TextOverflow.Clip,
                        modifier = Modifier
                            .testTag("hero_primary_balance")
                            .semantics(mergeDescendants = true) { }
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
                        val secDigits = formatHeroNetBalanceDigits(metrics.secondaryCurrencyBalance!!)
                        val secText = buildAnnotatedString {
                            append("≈ ")
                            append(secPrefix)
                            append(secDigits)
                            append(" ")
                            withStyle(
                                SpanStyle(
                                    fontSize = CurrencyUnitDecoupled.fontSize,
                                    fontWeight = CurrencyUnitDecoupled.fontWeight,
                                    color = FinTrackTheme.colors.textMuted
                                )
                            ) {
                                append(metrics.secondaryCurrency)
                            }
                        }
                        Text(
                            text = secText,
                            style = MetricFinancialRegular,
                            color = FinTrackTheme.colors.textSecondary
                        )
                    } else {
                        Text(
                            text = "≈ ${metrics.secondaryCurrency} unavailable",
                            style = LabelMicro,
                            color = FinTrackTheme.colors.textMuted
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
                    color = if (metrics.balance >= 0.0) FinTrackTheme.colors.healthPositive else FinTrackTheme.colors.healthCritical,
                    modifier = Modifier.testTag("hero_wealth_narrative")
                )

                Spacer(modifier = Modifier.height(Space4))

                // BNR Parity Informational Row
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(ShapePill)
                        .background(MaterialTheme.colorScheme.surfaceContainer)
                        .padding(horizontal = Space12, vertical = Space4)
                        .testTag("hero_bnr_parity")
                ) {
                    if (metrics.latestBnrRate != null) {
                        val rateFormatted = String.format(java.util.Locale.US, "%.4f", metrics.latestBnrRate)
                        val dateSuffix = if (!metrics.effectiveBnrDate.isNullOrBlank()) " • ${metrics.effectiveBnrDate}" else ""
                        Text(
                            text = "BNR • 1 EUR = $rateFormatted RON$dateSuffix",
                            style = LabelMicro,
                            color = FinTrackTheme.colors.textSecondary
                        )
                    } else {
                        Text(
                            text = "BNR unavailable",
                            style = LabelMicro,
                            color = FinTrackTheme.colors.textMuted
                        )
                    }
                }

                Spacer(modifier = Modifier.height(Space8))

                // Income / Expense Expressive Tonal Well
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainer)
                        .padding(horizontal = Space12, vertical = Space8)
                        .testTag("hero_cashflow_row")
                ) {
                    val useStackedCashflow = availableWidth < 300.dp || (availableWidth < 350.dp && (metrics.totalIncome >= 100_000.0 || metrics.totalExpense >= 100_000.0))

                    if (useStackedCashflow) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(Space12)
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
                                            .size(32.dp)
                                            .clip(CircleShape)
                                            .background(FinTrackTheme.colors.incomeContainer),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ArrowDownward,
                                            contentDescription = "Income",
                                            tint = FinTrackTheme.colors.income,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(Space8))
                                    Text(
                                        text = "Income",
                                        style = LabelBadge,
                                        color = FinTrackTheme.colors.textSecondary
                                    )
                                }
                                Spacer(modifier = Modifier.width(Space8))
                                HeroCashflowAmount(
                                    prefix = "+",
                                    amount = metrics.totalIncome,
                                    currency = metrics.currency,
                                    color = FinTrackTheme.colors.income,
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
                                            .size(32.dp)
                                            .clip(CircleShape)
                                            .background(FinTrackTheme.colors.expenseContainer),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ArrowUpward,
                                            contentDescription = "Expense",
                                            tint = FinTrackTheme.colors.expense,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(Space8))
                                    Text(
                                        text = "Expense",
                                        style = LabelBadge,
                                        color = FinTrackTheme.colors.textSecondary
                                    )
                                }
                                Spacer(modifier = Modifier.width(Space8))
                                HeroCashflowAmount(
                                    prefix = "-",
                                    amount = metrics.totalExpense,
                                    currency = metrics.currency,
                                    color = FinTrackTheme.colors.expense,
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
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(FinTrackTheme.colors.incomeContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ArrowDownward,
                                        contentDescription = "Income",
                                        tint = FinTrackTheme.colors.income,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(Space12))
                                Column(modifier = Modifier.weight(1f, fill = false)) {
                                    Text(
                                        text = "Income",
                                        style = LabelBadge,
                                        color = FinTrackTheme.colors.textSecondary
                                    )
                                    Spacer(modifier = Modifier.height(Space2))
                                    HeroCashflowAmount(
                                        prefix = "+",
                                        amount = metrics.totalIncome,
                                        currency = metrics.currency,
                                        color = FinTrackTheme.colors.income,
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
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(FinTrackTheme.colors.expenseContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ArrowUpward,
                                        contentDescription = "Expense",
                                        tint = FinTrackTheme.colors.expense,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(Space12))
                                Column(
                                    modifier = Modifier.weight(1f, fill = false),
                                    horizontalAlignment = Alignment.End
                                ) {
                                    Text(
                                        text = "Expense",
                                        style = LabelBadge,
                                        color = FinTrackTheme.colors.textSecondary
                                    )
                                    Spacer(modifier = Modifier.height(Space2))
                                    HeroCashflowAmount(
                                        prefix = "-",
                                        amount = metrics.totalExpense,
                                        currency = metrics.currency,
                                        color = FinTrackTheme.colors.expense,
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
    val digits = formatHeroCashflowDigits(amount)
    val annotatedText = buildAnnotatedString {
        append(prefix)
        append(digits)
        append(" ")
        withStyle(
            SpanStyle(
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = FinTrackTheme.colors.textSecondary
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

/**
 * Strict decoupled formatter for Hero Net Balance.
 * Preserves tabular grouping and guarantees independent presentation formatting.
 */
internal fun formatHeroNetBalanceDigits(amount: Double): String {
    val rounded = kotlin.math.ceil(kotlin.math.abs(amount)).toLong()
    val symbols = java.text.DecimalFormatSymbols(java.util.Locale.US).apply {
        groupingSeparator = ' '
    }
    val df = java.text.DecimalFormat("#,##0", symbols)
    return df.format(rounded)
}

/**
 * Strict decoupled formatter for Hero Cashflow figures (Income / Expense).
 * Completely isolated from Net Balance representation to prevent formatting inheritance.
 */
internal fun formatHeroCashflowDigits(amount: Double): String {
    val rounded = kotlin.math.ceil(kotlin.math.abs(amount)).toLong()
    val symbols = java.text.DecimalFormatSymbols(java.util.Locale.US).apply {
        groupingSeparator = ' '
    }
    val df = java.text.DecimalFormat("#,##0", symbols)
    return df.format(rounded)
}
