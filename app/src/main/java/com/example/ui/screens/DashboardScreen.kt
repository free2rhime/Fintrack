package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.snap
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.data.model.FilterSettings
import com.example.data.model.TransactionEntity
import com.example.data.repository.SyncStatus
import com.example.domain.analytics.CategoryExpenseShare
import com.example.domain.analytics.DashboardMetrics
import com.example.domain.analytics.MonthlyDataPoint
import com.example.domain.analytics.SmartFinancialInsights
import com.example.ui.components.CategoryDistributionChart
import com.example.ui.components.CurrencyToggle
import com.example.ui.components.FinTrackPeriodDropdown
import com.example.ui.components.FinancialPulseCard
import com.example.ui.components.MonthlyCashFlowSplineChart
import com.example.ui.components.RecentActivitySection
import com.example.ui.components.UnifiedHeroCanvas
import com.example.ui.theme.FinTrackMotion
import com.example.ui.theme.FinTrackTheme
import com.example.ui.theme.MaxContentWidthTablet
import com.example.ui.theme.MicroMetadata
import com.example.ui.theme.RadiusLarge
import com.example.ui.theme.RadiusMedium
import com.example.ui.theme.SectionHeadline
import com.example.ui.theme.ShapeGroupedContainer
import com.example.ui.theme.Space12
import com.example.ui.theme.Space16
import com.example.ui.theme.Space20
import com.example.ui.theme.Space24
import com.example.ui.theme.Space4
import com.example.ui.theme.isReducedMotionEnabled
import com.example.ui.theme.Space8

@Composable
fun DashboardScreen(
    metrics: DashboardMetrics,
    filterSettings: FilterSettings,
    monthlyDataPoints: List<MonthlyDataPoint>,
    categoryShares: List<CategoryExpenseShare>,
    smartInsights: SmartFinancialInsights,
    onPeriodSelected: (String) -> Unit,
    onCurrencyChanged: (String) -> Unit,
    recentTransactions: List<TransactionEntity> = emptyList(),
    onViewAllActivity: () -> Unit = {},
    onTransactionClicked: (TransactionEntity) -> Unit = {},
    syncStatus: SyncStatus = SyncStatus.SignedOut,
    modifier: Modifier = Modifier
) {
    val isReducedMotion = isReducedMotionEnabled()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("dashboard_screen_root"),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = MaxContentWidthTablet)
                .verticalScroll(rememberScrollState())
                .padding(bottom = Space24)
        ) {
            // Dashboard Header Region: Title + Currency Toggle ([ RON ] [ EUR ])
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
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.semantics { heading() }
                )
                CurrencyToggle(
                    selectedCurrency = filterSettings.selectedCurrency,
                    onCurrencyChanged = onCurrencyChanged
                )
            }

            // UNIFIED HERO CANVAS — Material 3 Expressive (Phase 3C / M3-3)
            UnifiedHeroCanvas(
                metrics = metrics,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Space16)
            )

            Spacer(modifier = Modifier.height(Space12))

            // Context Controls: Compact Period Selector Dropdown
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Space16, vertical = Space4),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FinTrackPeriodDropdown(
                    selectedPeriod = filterSettings.selectedPeriod,
                    onPeriodSelected = onPeriodSelected
                )
            }

            // Optional EUR / BNR Incomplete Warning
            AnimatedVisibility(
                visible = metrics.hasIncompleteEurData,
                enter = if (isReducedMotion) fadeIn(snap()) else fadeIn(FinTrackMotion.contentEntranceSpec()) + expandVertically(FinTrackMotion.contentEntranceSpec()),
                exit = if (isReducedMotion) fadeOut(snap()) else fadeOut(FinTrackMotion.contentChangeSpec()) + shrinkVertically(FinTrackMotion.contentChangeSpec())
            ) {
                Column {
                    Spacer(modifier = Modifier.height(Space12))
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = Space16)
                            .testTag("eur_incomplete_warning_card"),
                        shape = RoundedCornerShape(RadiusMedium),
                        color = MaterialTheme.colorScheme.surfaceContainer,
                        border = BorderStroke(1.dp, FinTrackTheme.colors.healthWarning.copy(alpha = 0.3f))
                    ) {
                        Row(
                            modifier = Modifier.padding(Space12),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(FinTrackTheme.colors.healthWarning.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = "Warning",
                                    tint = FinTrackTheme.colors.healthWarning,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(Space12))
                            Text(
                                text = "EUR totals are incomplete: ${metrics.excludedNonOfficialCount} transaction(s) pending or unverified BNR exchange rate excluded. Complete RON data remains available.",
                                style = MicroMetadata,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(Space16))

            // FINANCIAL PULSE CARD (Material 3 Expressive Grouped Tonal Panel - Phase 3C / M3-3)
            val isZeroIncome = metrics.totalIncome <= 0.0
            FinancialPulseCard(
                insights = smartInsights,
                savingsRate = if (isZeroIncome) null else metrics.savingsRate,
                expenseVelocity = if (isZeroIncome) null else metrics.expensePressure,
                periodLabel = metrics.periodLabel,
                modifier = Modifier.padding(horizontal = Space16)
            )

            Spacer(modifier = Modifier.height(Space20))

            // MONTHLY CASH FLOW CHART (Material 3 Expressive Tonal Grouped Panel)
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Space16)
                    .testTag("dashboard_cash_flow_container"),
                shape = ShapeGroupedContainer,
                color = MaterialTheme.colorScheme.surfaceContainerLow
            ) {
                Column(modifier = Modifier.padding(Space16)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(FinTrackTheme.colors.brandAccent.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                                contentDescription = null,
                                tint = FinTrackTheme.colors.brandAccent,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(Space8))
                        Text(
                            text = "Monthly Cash Flow",
                            style = SectionHeadline,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.semantics { heading() }
                        )
                    }

                    Spacer(modifier = Modifier.height(Space16))

                    MonthlyCashFlowSplineChart(
                        dataPoints = monthlyDataPoints,
                        currency = metrics.currency
                    )
                }
            }

            Spacer(modifier = Modifier.height(Space20))

            // CATEGORY BREAKDOWN CHART (Material 3 Expressive Tonal Grouped Panel)
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Space16)
                    .testTag("dashboard_category_distribution_container"),
                shape = ShapeGroupedContainer,
                color = MaterialTheme.colorScheme.surfaceContainerLow
            ) {
                Column(modifier = Modifier.padding(Space16)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(FinTrackTheme.colors.brandAccent.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PieChart,
                                contentDescription = null,
                                tint = FinTrackTheme.colors.brandAccent,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(Space8))
                        Text(
                            text = "Spending by Category",
                            style = SectionHeadline,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.semantics { heading() }
                        )
                    }

                    Spacer(modifier = Modifier.height(Space16))

                    CategoryDistributionChart(
                        categoryShares = categoryShares,
                        currency = metrics.currency
                    )
                }
            }

            Spacer(modifier = Modifier.height(Space20))

            // RECENT ACTIVITY STREAM (Checkpoint 4.2)
            RecentActivitySection(
                transactions = recentTransactions,
                selectedCurrency = filterSettings.selectedCurrency,
                onViewAllClicked = onViewAllActivity,
                onTransactionClicked = onTransactionClicked,
                modifier = Modifier.padding(horizontal = Space16)
            )
        }
    }
}

internal fun formatCeilNetBalance(balance: Double, currency: String): String {
    val rounded = if (balance >= 0.0) {
        kotlin.math.ceil(balance).toLong()
    } else {
        -kotlin.math.ceil(kotlin.math.abs(balance)).toLong()
    }
    val symbols = java.text.DecimalFormatSymbols(java.util.Locale.US).apply {
        groupingSeparator = ' '
    }
    val df = java.text.DecimalFormat("#,##0", symbols)
    return "${df.format(rounded)} $currency"
}

internal fun formatCeilAmount(amount: Double, currency: String): String {
    val rounded = kotlin.math.ceil(kotlin.math.abs(amount)).toLong()
    val symbols = java.text.DecimalFormatSymbols(java.util.Locale.US).apply {
        groupingSeparator = ' '
    }
    val df = java.text.DecimalFormat("#,##0", symbols)
    return "${df.format(rounded)} $currency"
}

internal fun formatCeilDigits(amount: Double): String {
    val rounded = kotlin.math.ceil(kotlin.math.abs(amount)).toLong()
    val symbols = java.text.DecimalFormatSymbols(java.util.Locale.US).apply {
        groupingSeparator = ' '
    }
    val df = java.text.DecimalFormat("#,##0", symbols)
    return df.format(rounded)
}


