package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.model.FilterSettings
import com.example.data.util.NumberFormatter
import com.example.domain.analytics.CategoryExpenseShare
import com.example.domain.analytics.DashboardMetrics
import com.example.domain.analytics.MonthlyDataPoint
import com.example.domain.analytics.SmartFinancialInsights
import com.example.ui.components.BadgeVariant
import com.example.ui.components.CategoryDistributionChart
import com.example.ui.components.CurrencyToggle
import com.example.ui.components.FinTrackCard
import com.example.ui.components.FinTrackSegmentedControl
import com.example.ui.components.FinTrackStatusBadge
import com.example.ui.components.MonthlyCashFlowBarChart
import com.example.ui.components.MonthlyCashFlowSplineChart
import com.example.ui.components.PeriodSelectorChipRow
import com.example.ui.components.SavingsTrendLineChart
import com.example.ui.theme.BodyRegular
import com.example.ui.theme.CanvasDark
import com.example.ui.theme.CardTitleAmount
import com.example.ui.theme.CobaltBlue
import com.example.ui.theme.ExpenseCoral
import com.example.ui.theme.IncomeEmerald
import com.example.ui.theme.MicroMetadata
import com.example.ui.theme.RadiusLarge
import com.example.ui.theme.RadiusMedium
import com.example.ui.theme.SectionHeadline
import com.example.ui.theme.Space12
import com.example.ui.theme.Space16
import com.example.ui.theme.Space20
import com.example.ui.theme.Space4
import com.example.ui.theme.Space8
import com.example.ui.theme.SurfaceContainerDark
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WarningAmber

@Composable
fun AnalyticsScreen(
    metrics: DashboardMetrics,
    filterSettings: FilterSettings,
    categoryExpenseShares: List<CategoryExpenseShare>,
    categoryIncomeShares: List<CategoryExpenseShare>,
    monthlyDataPoints: List<MonthlyDataPoint>,
    insights: SmartFinancialInsights,
    onPeriodSelected: (String) -> Unit,
    onCurrencyChanged: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var isSplineChart by remember { mutableStateOf(true) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(CanvasDark),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 680.dp)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 96.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header Region: Title + Currency Selector
            Spacer(modifier = Modifier.height(Space8))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Space16, vertical = Space8),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
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
                            imageVector = Icons.Default.Analytics,
                            contentDescription = null,
                            tint = CobaltBlue,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(Space8))
                    Text(
                        text = "Analytics & Ratios",
                        style = SectionHeadline,
                        color = TextPrimary
                    )
                }

                CurrencyToggle(
                    selectedCurrency = filterSettings.selectedCurrency,
                    onCurrencyChanged = onCurrencyChanged
                )
            }

            Spacer(modifier = Modifier.height(Space8))

            // Period Selector Chip Row
            PeriodSelectorChipRow(
                selectedPeriod = filterSettings.selectedPeriod,
                onPeriodSelected = onPeriodSelected
            )

            // Incomplete EUR warning if applicable
            if (metrics.hasIncompleteEurData) {
                Spacer(modifier = Modifier.height(Space12))
                FinTrackCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Space16)
                        .testTag("analytics_eur_incomplete_warning_card"),
                    shape = RoundedCornerShape(RadiusMedium),
                    containerColor = SurfaceContainerDark,
                    border = BorderStroke(1.dp, WarningAmber.copy(alpha = 0.3f)),
                    contentPadding = Space12
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
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

            // Financial Health Assessment Card
            FinTrackCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Space16)
                    .testTag("analytics_smart_insights_card"),
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
                                imageVector = Icons.Default.TrendingUp,
                                contentDescription = null,
                                tint = CobaltBlue,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(Space8))
                        Text(
                            text = "Financial Health Assessment",
                            style = SectionHeadline,
                            color = TextPrimary
                        )
                    }

                    Spacer(modifier = Modifier.height(Space12))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Space8)
                    ) {
                        Text(
                            text = "Status:",
                            style = BodyRegular,
                            color = TextSecondary
                        )

                        val healthBadgeVariant = when {
                            insights.savingsTrendText.contains("Growth", ignoreCase = true) ||
                            insights.savingsTrendText.contains("Strong", ignoreCase = true) -> BadgeVariant.SUCCESS
                            insights.savingsTrendText.contains("Positive", ignoreCase = true) -> BadgeVariant.SUCCESS
                            insights.savingsTrendText.contains("High Expense", ignoreCase = true) ||
                            insights.savingsTrendText.contains("Ratio", ignoreCase = true) -> BadgeVariant.WARNING
                            else -> BadgeVariant.NEUTRAL
                        }

                        FinTrackStatusBadge(
                            label = insights.savingsTrendText,
                            variant = healthBadgeVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(Space16))

                    // Average Monthly Metrics
                    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                        if (maxWidth < 340.dp) {
                            Column(verticalArrangement = Arrangement.spacedBy(Space8)) {
                                FinTrackCard(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(RadiusMedium),
                                    containerColor = SurfaceContainerDark,
                                    contentPadding = Space12
                                ) {
                                    Column {
                                        Text(
                                            text = "Avg Monthly Expense",
                                            style = MicroMetadata,
                                            color = TextSecondary
                                        )
                                        Spacer(modifier = Modifier.height(Space4))
                                        Text(
                                            text = NumberFormatter.formatCurrency(insights.avgMonthlyExpense, metrics.currency),
                                            style = CardTitleAmount,
                                            color = ExpenseCoral
                                        )
                                    }
                                }

                                FinTrackCard(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(RadiusMedium),
                                    containerColor = SurfaceContainerDark,
                                    contentPadding = Space12
                                ) {
                                    Column {
                                        Text(
                                            text = "Avg Monthly Income",
                                            style = MicroMetadata,
                                            color = TextSecondary
                                        )
                                        Spacer(modifier = Modifier.height(Space4))
                                        Text(
                                            text = NumberFormatter.formatCurrency(insights.avgMonthlyIncome, metrics.currency),
                                            style = CardTitleAmount,
                                            color = IncomeEmerald
                                        )
                                    }
                                }
                            }
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(Space12)
                            ) {
                                FinTrackCard(
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(RadiusMedium),
                                    containerColor = SurfaceContainerDark,
                                    contentPadding = Space12
                                ) {
                                    Column {
                                        Text(
                                            text = "Avg Monthly Expense",
                                            style = MicroMetadata,
                                            color = TextSecondary
                                        )
                                        Spacer(modifier = Modifier.height(Space4))
                                        Text(
                                            text = NumberFormatter.formatCurrency(insights.avgMonthlyExpense, metrics.currency),
                                            style = CardTitleAmount,
                                            color = ExpenseCoral
                                        )
                                    }
                                }

                                FinTrackCard(
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(RadiusMedium),
                                    containerColor = SurfaceContainerDark,
                                    contentPadding = Space12
                                ) {
                                    Column {
                                        Text(
                                            text = "Avg Monthly Income",
                                            style = MicroMetadata,
                                            color = TextSecondary
                                        )
                                        Spacer(modifier = Modifier.height(Space4))
                                        Text(
                                            text = NumberFormatter.formatCurrency(insights.avgMonthlyIncome, metrics.currency),
                                            style = CardTitleAmount,
                                            color = IncomeEmerald
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(Space16))

            // Period Totals Card
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
                                imageVector = Icons.Default.ReceiptLong,
                                contentDescription = null,
                                tint = CobaltBlue,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(Space8))
                        Text(
                            text = "Period Totals",
                            style = SectionHeadline,
                            color = TextPrimary
                        )
                    }

                    Spacer(modifier = Modifier.height(Space16))

                    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                        if (maxWidth < 360.dp) {
                            Column(verticalArrangement = Arrangement.spacedBy(Space8)) {
                                FinTrackCard(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(RadiusMedium),
                                    containerColor = SurfaceContainerDark,
                                    contentPadding = Space12
                                ) {
                                    Column {
                                        Text(
                                            text = "Total Income",
                                            style = MicroMetadata,
                                            color = TextSecondary
                                        )
                                        Spacer(modifier = Modifier.height(Space4))
                                        Text(
                                            text = NumberFormatter.formatCurrency(metrics.totalIncome, metrics.currency),
                                            style = CardTitleAmount,
                                            color = IncomeEmerald
                                        )
                                    }
                                }

                                FinTrackCard(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(RadiusMedium),
                                    containerColor = SurfaceContainerDark,
                                    contentPadding = Space12
                                ) {
                                    Column {
                                        Text(
                                            text = "Total Expense",
                                            style = MicroMetadata,
                                            color = TextSecondary
                                        )
                                        Spacer(modifier = Modifier.height(Space4))
                                        Text(
                                            text = NumberFormatter.formatCurrency(metrics.totalExpense, metrics.currency),
                                            style = CardTitleAmount,
                                            color = ExpenseCoral
                                        )
                                    }
                                }

                                FinTrackCard(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(RadiusMedium),
                                    containerColor = SurfaceContainerDark,
                                    contentPadding = Space12
                                ) {
                                    Column {
                                        Text(
                                            text = "Net Balance",
                                            style = MicroMetadata,
                                            color = TextSecondary
                                        )
                                        Spacer(modifier = Modifier.height(Space4))
                                        Text(
                                            text = NumberFormatter.formatCurrency(metrics.balance, metrics.currency),
                                            style = CardTitleAmount,
                                            color = if (metrics.balance > 0) IncomeEmerald else if (metrics.balance < 0) ExpenseCoral else TextPrimary
                                        )
                                    }
                                }
                            }
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(Space8)
                            ) {
                                FinTrackCard(
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(RadiusMedium),
                                    containerColor = SurfaceContainerDark,
                                    contentPadding = Space12
                                ) {
                                    Column {
                                        Text(
                                            text = "Total Income",
                                            style = MicroMetadata,
                                            color = TextSecondary
                                        )
                                        Spacer(modifier = Modifier.height(Space4))
                                        Text(
                                            text = NumberFormatter.formatCurrency(metrics.totalIncome, metrics.currency),
                                            style = CardTitleAmount,
                                            color = IncomeEmerald
                                        )
                                    }
                                }

                                FinTrackCard(
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(RadiusMedium),
                                    containerColor = SurfaceContainerDark,
                                    contentPadding = Space12
                                ) {
                                    Column {
                                        Text(
                                            text = "Total Expense",
                                            style = MicroMetadata,
                                            color = TextSecondary
                                        )
                                        Spacer(modifier = Modifier.height(Space4))
                                        Text(
                                            text = NumberFormatter.formatCurrency(metrics.totalExpense, metrics.currency),
                                            style = CardTitleAmount,
                                            color = ExpenseCoral
                                        )
                                    }
                                }

                                FinTrackCard(
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(RadiusMedium),
                                    containerColor = SurfaceContainerDark,
                                    contentPadding = Space12
                                ) {
                                    Column {
                                        Text(
                                            text = "Net Balance",
                                            style = MicroMetadata,
                                            color = TextSecondary
                                        )
                                        Spacer(modifier = Modifier.height(Space4))
                                        Text(
                                            text = NumberFormatter.formatCurrency(metrics.balance, metrics.currency),
                                            style = CardTitleAmount,
                                            color = if (metrics.balance > 0) IncomeEmerald else if (metrics.balance < 0) ExpenseCoral else TextPrimary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(Space20))

            // Net Savings Trend Card
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
                                .background(IncomeEmerald.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.ShowChart,
                                contentDescription = null,
                                tint = IncomeEmerald,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(Space8))
                        Text(
                            text = "Net Savings Trend",
                            style = SectionHeadline,
                            color = TextPrimary
                        )
                    }

                    Spacer(modifier = Modifier.height(Space16))

                    SavingsTrendLineChart(dataPoints = monthlyDataPoints, currency = metrics.currency)
                }
            }

            Spacer(modifier = Modifier.height(Space20))

            // Income vs Expense Trend Card
            FinTrackCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Space16),
                shape = RoundedCornerShape(RadiusLarge),
                containerColor = SurfaceDark,
                contentPadding = Space16
            ) {
                Column {
                    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                        if (maxWidth < 360.dp) {
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
                                            imageVector = Icons.Default.BarChart,
                                            contentDescription = null,
                                            tint = CobaltBlue,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(Space8))
                                    Text(
                                        text = "Income vs Expense Trend",
                                        style = SectionHeadline,
                                        color = TextPrimary
                                    )
                                }
                                Spacer(modifier = Modifier.height(Space12))
                                FinTrackSegmentedControl(
                                    items = listOf("Spline", "Bars"),
                                    selectedIndex = if (isSplineChart) 0 else 1,
                                    onItemSelected = { isSplineChart = (it == 0) },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        } else {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f, fill = false)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(CircleShape)
                                            .background(CobaltBlue.copy(alpha = 0.15f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.BarChart,
                                            contentDescription = null,
                                            tint = CobaltBlue,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(Space8))
                                    Text(
                                        text = "Income vs Expense Trend",
                                        style = SectionHeadline,
                                        color = TextPrimary
                                    )
                                }
                                Spacer(modifier = Modifier.width(Space8))
                                FinTrackSegmentedControl(
                                    items = listOf("Spline", "Bars"),
                                    selectedIndex = if (isSplineChart) 0 else 1,
                                    onItemSelected = { isSplineChart = (it == 0) },
                                    modifier = Modifier.width(150.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(Space16))

                    if (isSplineChart) {
                        MonthlyCashFlowSplineChart(dataPoints = monthlyDataPoints, currency = metrics.currency)
                    } else {
                        MonthlyCashFlowBarChart(dataPoints = monthlyDataPoints, currency = metrics.currency)
                    }
                }
            }

            Spacer(modifier = Modifier.height(Space20))

            // Expense Category Breakdown Card
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
                                .background(ExpenseCoral.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PieChart,
                                contentDescription = null,
                                tint = ExpenseCoral,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(Space8))
                        Text(
                            text = "Expense Category Breakdown",
                            style = SectionHeadline,
                            color = TextPrimary
                        )
                    }

                    Spacer(modifier = Modifier.height(Space16))

                    CategoryDistributionChart(categoryShares = categoryExpenseShares, currency = metrics.currency)
                }
            }

            // Optional Income Source Distribution Card
            if (categoryIncomeShares.isNotEmpty()) {
                Spacer(modifier = Modifier.height(Space20))

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
                                    .background(IncomeEmerald.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PieChart,
                                    contentDescription = null,
                                    tint = IncomeEmerald,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(Space8))
                            Text(
                                text = "Income Source Distribution",
                                style = SectionHeadline,
                                color = TextPrimary
                            )
                        }

                        Spacer(modifier = Modifier.height(Space16))

                        CategoryDistributionChart(categoryShares = categoryIncomeShares, currency = metrics.currency)
                    }
                }
            }
        }
    }
}
