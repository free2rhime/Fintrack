package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.TrendingUp
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.FilterSettings
import com.example.data.repository.SyncStatus
import com.example.data.util.NumberFormatter
import com.example.domain.analytics.CategoryExpenseShare
import com.example.domain.analytics.DashboardMetrics
import com.example.domain.analytics.MonthlyDataPoint
import com.example.domain.analytics.SmartFinancialInsights
import com.example.ui.components.BadgeVariant
import com.example.ui.components.CategoryDistributionChart
import com.example.ui.components.CurrencyToggle
import com.example.ui.components.FinTrackCard
import com.example.ui.components.FinTrackStatusBadge
import com.example.ui.components.FinTrackSyncStatus
import com.example.ui.components.MonthlyCashFlowBarChart
import com.example.ui.components.MonthlyCashFlowSplineChart
import com.example.ui.components.PeriodSelectorChipRow
import com.example.ui.theme.CardTitleAmount
import com.example.ui.theme.CobaltBlue
import com.example.ui.theme.ExpenseContainer
import com.example.ui.theme.ExpenseCoral
import com.example.ui.theme.FinTrackMotion
import com.example.ui.theme.HeroFinancialDisplay
import com.example.ui.theme.IncomeContainer
import com.example.ui.theme.IncomeEmerald
import com.example.ui.theme.LabelBadgeMedium
import com.example.ui.theme.MicroMetadata
import com.example.ui.theme.RadiusLarge
import com.example.ui.theme.RadiusMedium
import com.example.ui.theme.RadiusSmall
import com.example.ui.theme.RadiusXLarge
import com.example.ui.theme.SectionHeadline
import com.example.ui.theme.Space12
import com.example.ui.theme.Space16
import com.example.ui.theme.Space2
import com.example.ui.theme.Space20
import com.example.ui.theme.Space24
import com.example.ui.theme.Space4
import com.example.ui.theme.Space8
import com.example.ui.theme.SurfaceContainerDark
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WarningAmber

@Composable
fun DashboardScreen(
    metrics: DashboardMetrics,
    filterSettings: FilterSettings,
    monthlyDataPoints: List<MonthlyDataPoint>,
    categoryShares: List<CategoryExpenseShare>,
    smartInsights: SmartFinancialInsights,
    onPeriodSelected: (String) -> Unit,
    onCurrencyChanged: (String) -> Unit,
    syncStatus: SyncStatus = SyncStatus.SignedOut,
    modifier: Modifier = Modifier
) {
    var isSplineChart by remember { mutableStateOf(true) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 80.dp)
    ) {
        // Dashboard Header Region: Title + Sync Status
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Space16, vertical = Space8),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Dashboard",
                style = SectionHeadline,
                color = TextPrimary
            )
            FinTrackSyncStatus(
                syncStatus = syncStatus,
                modifier = Modifier.testTag("sync_status_indicator")
            )
        }

        // Context Controls: Period Selector + Currency Selector
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = Space4),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.weight(1f)) {
                PeriodSelectorChipRow(
                    selectedPeriod = filterSettings.selectedPeriod,
                    onPeriodSelected = onPeriodSelected
                )
            }
            Box(modifier = Modifier.padding(end = Space16)) {
                CurrencyToggle(
                    selectedCurrency = filterSettings.selectedCurrency,
                    onCurrencyChanged = onCurrencyChanged
                )
            }
        }

        // Optional EUR / BNR Incomplete Warning
        if (metrics.hasIncompleteEurData) {
            Spacer(modifier = Modifier.height(Space12))
            FinTrackCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Space16)
                    .testTag("eur_incomplete_warning_card"),
                shape = RoundedCornerShape(RadiusMedium),
                containerColor = SurfaceContainerDark,
                border = BorderStroke(1.dp, WarningAmber.copy(alpha = 0.3f)),
                contentPadding = Space12
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(WarningAmber.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Warning",
                            tint = WarningAmber,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(Space12))
                    Text(
                        text = "EUR totals are incomplete: ${metrics.excludedNonOfficialCount} transaction(s) pending or unverified BNR exchange rate excluded. Complete RON data remains available.",
                        style = MicroMetadata,
                        color = TextPrimary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(Space16))

        // HERO BALANCE CARD (Tonal Surface Hierarchy, RadiusXLarge = 24dp)
        FinTrackCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Space16)
                .testTag("dashboard_top_card"),
            shape = RoundedCornerShape(RadiusXLarge),
            containerColor = SurfaceDark,
            tonalElevation = 2.dp,
            contentPadding = Space24
        ) {
            Column {
                Text(
                    text = "NET BALANCE",
                    style = LabelBadgeMedium,
                    color = TextSecondary,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(Space8))
                AnimatedContent(
                    targetState = NumberFormatter.formatCurrency(metrics.balance, metrics.currency),
                    transitionSpec = { FinTrackMotion.contentFade() },
                    label = "dashboard_balance_amount"
                ) { formattedBalance ->
                    Text(
                        text = formattedBalance,
                        style = HeroFinancialDisplay,
                        color = TextPrimary
                    )
                }

                Spacer(modifier = Modifier.height(Space24))

                // Income & Expense Split
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Total Income
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(IncomeContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowDownward,
                                contentDescription = "Income",
                                tint = IncomeEmerald,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(Space12))
                        Column {
                            Text(
                                text = "Income",
                                style = LabelBadgeMedium,
                                color = TextSecondary
                            )
                            Spacer(modifier = Modifier.height(Space2))
                            Text(
                                text = "+${NumberFormatter.formatCurrency(metrics.totalIncome, metrics.currency)}",
                                style = CardTitleAmount,
                                color = IncomeEmerald
                            )
                        }
                    }

                    // Total Expense
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(ExpenseContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowUpward,
                                contentDescription = "Expense",
                                tint = ExpenseCoral,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(Space12))
                        Column {
                            Text(
                                text = "Expense",
                                style = LabelBadgeMedium,
                                color = TextSecondary
                            )
                            Spacer(modifier = Modifier.height(Space2))
                            Text(
                                text = "-${NumberFormatter.formatCurrency(metrics.totalExpense, metrics.currency)}",
                                style = CardTitleAmount,
                                color = ExpenseCoral
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(Space20))

        // FINANCIAL RATIO CARDS (Row 1: Savings Rate & Expense Pressure)
        // Notice: "KEY FINANCIAL RATIOS" visible heading is intentionally removed.
        val isZeroIncome = metrics.totalIncome <= 0.0

        val savingsVariant = when {
            isZeroIncome -> BadgeVariant.NEUTRAL
            metrics.savingsRate >= 20.0 -> BadgeVariant.SUCCESS
            metrics.savingsRate >= 0.0 -> BadgeVariant.WARNING
            else -> BadgeVariant.ERROR
        }

        val pressureVariant = when {
            isZeroIncome -> BadgeVariant.NEUTRAL
            metrics.expensePressure < 60.0 -> BadgeVariant.SUCCESS
            metrics.expensePressure <= 80.0 -> BadgeVariant.WARNING
            else -> BadgeVariant.ERROR
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Space16),
            horizontalArrangement = Arrangement.spacedBy(Space12)
        ) {
            // Savings Rate Card
            FinTrackCard(
                modifier = Modifier
                    .weight(1f)
                    .testTag("card_savings_rate"),
                shape = RoundedCornerShape(RadiusLarge),
                containerColor = SurfaceDark,
                contentPadding = Space16
            ) {
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Space8)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(SurfaceContainerDark),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Savings,
                                contentDescription = "Savings",
                                tint = TextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Text(
                            text = "Savings Rate",
                            style = LabelBadgeMedium,
                            color = TextSecondary
                        )
                    }

                    Spacer(modifier = Modifier.height(Space12))

                    FinTrackStatusBadge(
                        label = if (isZeroIncome) "N/A" else "${metrics.savingsRate}% saved",
                        variant = savingsVariant
                    )
                }
            }

            // Expense Pressure Card
            FinTrackCard(
                modifier = Modifier
                    .weight(1f)
                    .testTag("card_expense_pressure"),
                shape = RoundedCornerShape(RadiusLarge),
                containerColor = SurfaceDark,
                contentPadding = Space16
            ) {
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Space8)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(SurfaceContainerDark),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Speed,
                                contentDescription = "Pressure",
                                tint = TextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Text(
                            text = "Expense Pressure",
                            style = LabelBadgeMedium,
                            color = TextSecondary
                        )
                    }

                    Spacer(modifier = Modifier.height(Space12))

                    FinTrackStatusBadge(
                        label = if (isZeroIncome) "N/A" else "${metrics.expensePressure}% used",
                        variant = pressureVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(Space12))

        // FINANCIAL RATIO CARDS (Row 2: Top Expense Category & Concentration)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Space16),
            horizontalArrangement = Arrangement.spacedBy(Space12)
        ) {
            // Top Expense Category
            FinTrackCard(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(RadiusLarge),
                containerColor = SurfaceDark,
                contentPadding = Space16
            ) {
                Column {
                    Text(
                        text = "Top Category",
                        style = LabelBadgeMedium,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(Space8))
                    Text(
                        text = metrics.topExpenseCategory.ifBlank { "None" },
                        style = CardTitleAmount,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(Space4))
                    Text(
                        text = NumberFormatter.formatCurrency(metrics.topExpenseCategoryAmount, metrics.currency),
                        style = MicroMetadata,
                        color = CobaltBlue
                    )
                }
            }

            // Concentration Share
            FinTrackCard(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(RadiusLarge),
                containerColor = SurfaceDark,
                contentPadding = Space16
            ) {
                Column {
                    Text(
                        text = "Concentration",
                        style = LabelBadgeMedium,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(Space8))
                    Text(
                        text = "${metrics.categoryConcentrationPercent}%",
                        style = CardTitleAmount,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(Space4))
                    Text(
                        text = "of total spending",
                        style = MicroMetadata,
                        color = TextSecondary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(Space24))

        // MONTHLY CASH FLOW CHART (Tonal Container, Spline & Bar Chart modes)
        FinTrackCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Space16),
            shape = RoundedCornerShape(RadiusLarge),
            containerColor = SurfaceDark,
            contentPadding = Space16
        ) {
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(CobaltBlue.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.TrendingUp,
                                contentDescription = null,
                                tint = CobaltBlue,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(Space8))
                        Text(
                            text = "Monthly Cash Flow",
                            style = SectionHeadline,
                            color = TextPrimary
                        )
                    }

                    // Chart Type Segmented Pills
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(RadiusMedium))
                            .background(SurfaceContainerDark)
                            .padding(Space2)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(RadiusSmall),
                            color = if (isSplineChart) CobaltBlue else Color.Transparent,
                            modifier = Modifier
                                .clip(RoundedCornerShape(RadiusSmall))
                                .clickable { isSplineChart = true }
                                .padding(horizontal = Space12, vertical = Space4)
                        ) {
                            Text(
                                text = "Spline",
                                style = LabelBadgeMedium,
                                fontWeight = if (isSplineChart) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSplineChart) Color.White else TextSecondary
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(RadiusSmall),
                            color = if (!isSplineChart) CobaltBlue else Color.Transparent,
                            modifier = Modifier
                                .clip(RoundedCornerShape(RadiusSmall))
                                .clickable { isSplineChart = false }
                                .padding(horizontal = Space12, vertical = Space4)
                        ) {
                            Text(
                                text = "Bars",
                                style = LabelBadgeMedium,
                                fontWeight = if (!isSplineChart) FontWeight.Bold else FontWeight.Medium,
                                color = if (!isSplineChart) Color.White else TextSecondary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(Space16))

                if (isSplineChart) {
                    MonthlyCashFlowSplineChart(
                        dataPoints = monthlyDataPoints,
                        currency = metrics.currency
                    )
                } else {
                    MonthlyCashFlowBarChart(
                        dataPoints = monthlyDataPoints,
                        currency = metrics.currency
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(Space20))

        // CATEGORY BREAKDOWN CHART (Tonal Container)
        FinTrackCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Space16),
            shape = RoundedCornerShape(RadiusLarge),
            containerColor = SurfaceDark,
            contentPadding = Space16
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(CobaltBlue.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PieChart,
                            contentDescription = null,
                            tint = CobaltBlue,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(Space8))
                    Text(
                        text = "Spending by Category",
                        style = SectionHeadline,
                        color = TextPrimary
                    )
                }

                Spacer(modifier = Modifier.height(Space16))

                CategoryDistributionChart(
                    categoryShares = categoryShares,
                    currency = metrics.currency
                )
            }
        }
    }
}
