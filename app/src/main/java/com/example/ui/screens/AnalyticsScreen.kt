package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.snap
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.TrendingUp
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.FilterSettings
import com.example.data.util.NumberFormatter
import com.example.ui.AnalyticsUiState
import com.example.ui.components.CurrencyToggle
import com.example.ui.components.FinTrackDropdownSelector
import com.example.ui.components.FinTrackEmptyState
import com.example.ui.components.SingleSeriesSplineChart
import com.example.ui.theme.CardTitleAmount
import com.example.ui.theme.FinTrackMotion
import com.example.ui.theme.FinTrackTheme
import com.example.ui.theme.LabelBadge
import com.example.ui.theme.MaxContentWidthTablet
import com.example.ui.theme.MicroMetadata
import com.example.ui.theme.SectionHeadline
import com.example.ui.theme.ShapeBadgeOrganic
import com.example.ui.theme.ShapeGroupedContainer
import com.example.ui.theme.ShapeGroupedItemBottom
import com.example.ui.theme.ShapeGroupedItemSingle
import com.example.ui.theme.ShapeGroupedItemTop
import com.example.ui.theme.ShapeSquircleIcon
import com.example.ui.theme.Space12
import com.example.ui.theme.Space16
import com.example.ui.theme.Space20
import com.example.ui.theme.Space4
import com.example.ui.theme.Space8
import com.example.ui.theme.isReducedMotionEnabled
import java.util.Locale

/**
 * Material 3 Expressive Exploration Surface for FinTrack Analytics.
 *
 * Designed as a tactile, organic, and visually engaging Material 3 Expressive
 * experience for private family finance:
 * - Expressive Analytics Hero Canvas ([ShapeGroupedContainer]) with organic period badge and CurrencyToggle.
 * - Tonal grouped exploration containers replacing generic bordered cards.
 * - Dynamic interactive spline charts with spring-animated transitions.
 * - Content-first financial metric clusters with tabular numerals.
 * - Strict financial calculation authority: zero UI-level arithmetic.
 * - Complete removal of legacy Smart Financial Insights from the Analytics UI.
 * - Global period filter invariant preserved: no local period chips/dropdowns on AnalyticsScreen.
 */
@Composable
fun AnalyticsScreen(
    analyticsUiState: AnalyticsUiState,
    filterSettings: FilterSettings,
    hasIncompleteEurData: Boolean = false,
    excludedNonOfficialCount: Int = 0,
    onCurrencyChanged: (String) -> Unit = {},
    onIncomeExpenseSelectionChanged: (String) -> Unit = {},
    onExpenseCategorySelectionChanged: (String) -> Unit = {},
    onIncomeSourceSelectionChanged: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val isReducedMotion = isReducedMotionEnabled()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = MaxContentWidthTablet)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 96.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ==========================================
            // [HERO REGION] Material 3 Expressive Analytics Hero
            // ==========================================
            Spacer(modifier = Modifier.height(Space8))
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Space16, vertical = Space8)
                    .testTag("analytics_hero_canvas"),
                shape = ShapeGroupedContainer,
                color = MaterialTheme.colorScheme.surfaceContainerLow
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(Space20)
                ) {
                    // Top Row: Large Expressive Squircle Icon & CurrencyToggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(ShapeSquircleIcon)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Analytics,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        CurrencyToggle(
                            selectedCurrency = filterSettings.selectedCurrency,
                            onCurrencyChanged = onCurrencyChanged
                        )
                    }

                    Spacer(modifier = Modifier.height(Space16))

                    // Title & Contextual Subtitle
                    Text(
                        text = "Analytics",
                        style = SectionHeadline,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.semantics { heading() }
                    )
                    Spacer(modifier = Modifier.height(Space4))
                    Text(
                        text = "Financial flow & category distribution",
                        style = MicroMetadata,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(Space12))

                    // Compact visual summary of the currently selected period (Organic badge)
                    Surface(
                        shape = ShapeBadgeOrganic,
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        modifier = Modifier.testTag("analytics_active_period_badge")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = Space12, vertical = Space4)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary)
                            )
                            Spacer(modifier = Modifier.width(Space8))
                            Text(
                                text = filterSettings.selectedPeriod,
                                style = LabelBadge,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            // Incomplete EUR warning if applicable
            if (hasIncompleteEurData) {
                Spacer(modifier = Modifier.height(Space12))
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Space16)
                        .testTag("analytics_eur_incomplete_warning_card"),
                    shape = ShapeGroupedItemSingle,
                    color = FinTrackTheme.colors.warning.copy(alpha = 0.10f),
                    border = BorderStroke(1.dp, FinTrackTheme.colors.warning.copy(alpha = 0.25f))
                ) {
                    Row(
                        modifier = Modifier.padding(Space16),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(ShapeSquircleIcon)
                                .background(FinTrackTheme.colors.warning.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "Warning",
                                tint = FinTrackTheme.colors.warning,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(Space12))
                        Text(
                            text = "EUR totals are incomplete: $excludedNonOfficialCount transaction(s) pending or unverified BNR exchange rate excluded. Complete RON data remains available.",
                            style = MicroMetadata,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(Space16))

            // ==========================================
            // [SECTION 1] Explore Financial Flow (Income & Expense)
            // ==========================================
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Space16)
                    .testTag("analytics_income_expense_card"),
                shape = ShapeGroupedContainer,
                color = MaterialTheme.colorScheme.surfaceContainerLow
            ) {
                Column(modifier = Modifier.padding(Space16)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f, fill = false)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(ShapeSquircleIcon)
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ShowChart,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(Space8))
                            Text(
                                text = "Income & Expense",
                                style = SectionHeadline,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.semantics { heading() }
                            )
                        }

                        Spacer(modifier = Modifier.width(Space8))

                        FinTrackDropdownSelector(
                            selectedItem = analyticsUiState.incomeExpenseSelection,
                            items = listOf("Income", "Expense"),
                            onItemSelected = onIncomeExpenseSelectionChanged,
                            itemLabel = { it },
                            testTag = "analytics_income_expense_selector"
                        )
                    }

                    Spacer(modifier = Modifier.height(Space16))

                    val isIncome = analyticsUiState.incomeExpenseSelection.equals("Income", ignoreCase = true)
                    val lineColor = if (isIncome) FinTrackTheme.colors.income else FinTrackTheme.colors.expense

                    AnimatedContent(
                        targetState = analyticsUiState.incomeExpenseSelection,
                        transitionSpec = {
                            if (isReducedMotion) {
                                fadeIn(animationSpec = snap()) togetherWith fadeOut(animationSpec = snap())
                            } else {
                                fadeIn(animationSpec = FinTrackMotion.selectionSpring()) togetherWith
                                        fadeOut(animationSpec = FinTrackMotion.selectionSpring())
                            }
                        },
                        label = "incomeExpenseChartContent"
                    ) { _ ->
                        Column {
                            SingleSeriesSplineChart(
                                dataPoints = analyticsUiState.incomeExpenseResult.dataPoints,
                                currency = analyticsUiState.incomeExpenseResult.currency,
                                lineColor = lineColor
                            )

                            Spacer(modifier = Modifier.height(Space16))

                            // Integrated Monthly Average Metric Surface
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = ShapeGroupedItemSingle,
                                color = MaterialTheme.colorScheme.surfaceContainer
                            ) {
                                Column(modifier = Modifier.padding(horizontal = Space16, vertical = Space12)) {
                                    Text(
                                        text = "Monthly Average",
                                        style = MicroMetadata,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(Space4))
                                    Text(
                                        text = "${NumberFormatter.formatAmount(analyticsUiState.incomeExpenseResult.monthlyAverage)} ${analyticsUiState.incomeExpenseResult.currency} / month",
                                        style = CardTitleAmount,
                                        color = lineColor,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(Space20))

            // ==========================================
            // [SECTION 2] Spending by Category Exploration
            // ==========================================
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Space16)
                    .testTag("analytics_spending_category_card"),
                shape = ShapeGroupedContainer,
                color = MaterialTheme.colorScheme.surfaceContainerLow
            ) {
                Column(modifier = Modifier.padding(Space16)) {
                    val expRankings = analyticsUiState.expenseCategoryRankings
                    val selectedRankingItem = expRankings.find {
                        it.categoryName == analyticsUiState.selectedExpenseCategory
                    } ?: expRankings.firstOrNull()

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f, fill = false)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(ShapeSquircleIcon)
                                    .background(FinTrackTheme.colors.expense.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PieChart,
                                    contentDescription = null,
                                    tint = FinTrackTheme.colors.expense,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(Space8))
                            Text(
                                text = "Spending by Category",
                                style = SectionHeadline,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.semantics { heading() }
                            )
                        }

                        if (expRankings.isNotEmpty()) {
                            Spacer(modifier = Modifier.width(Space8))
                            FinTrackDropdownSelector(
                                selectedItem = selectedRankingItem,
                                items = expRankings,
                                onItemSelected = { onExpenseCategorySelectionChanged(it.categoryName) },
                                itemLabel = { it.categoryName },
                                itemDropdownLabel = { "${it.categoryName} · ${String.format(Locale.US, "%.1f%%", it.percentage)}" },
                                testTag = "analytics_spending_category_selector"
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(Space16))

                    if (expRankings.isEmpty()) {
                        FinTrackEmptyState(
                            title = "No Expense Data",
                            description = "No expense categories recorded for this period",
                            icon = Icons.Default.PieChart,
                            iconTint = MaterialTheme.colorScheme.onSurfaceVariant,
                            compact = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp)
                        )
                    } else {
                        val expResult = analyticsUiState.expenseCategoryResult
                        val selectedCategoryName = selectedRankingItem?.categoryName ?: ""
                        val monthCountText = if (expResult.monthCount == 1) "1 month" else "${expResult.monthCount} months"

                        AnimatedContent(
                            targetState = selectedCategoryName,
                            transitionSpec = {
                                if (isReducedMotion) {
                                    fadeIn(animationSpec = snap()) togetherWith fadeOut(animationSpec = snap())
                                } else {
                                    fadeIn(animationSpec = FinTrackMotion.selectionSpring()) togetherWith
                                            fadeOut(animationSpec = FinTrackMotion.selectionSpring())
                                }
                            },
                            label = "spendingCategoryChartContent"
                        ) { _ ->
                            Column {
                                SingleSeriesSplineChart(
                                    dataPoints = expResult.dataPoints,
                                    currency = expResult.currency,
                                    lineColor = FinTrackTheme.colors.expense
                                )

                                Spacer(modifier = Modifier.height(Space16))

                                // Integrated Metric Cluster (Top & Bottom Grouped Surfaces)
                                Column(verticalArrangement = Arrangement.spacedBy(Space8)) {
                                    // Total Metric
                                    Surface(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = ShapeGroupedItemTop,
                                        color = MaterialTheme.colorScheme.surfaceContainer
                                    ) {
                                        Column(modifier = Modifier.padding(horizontal = Space16, vertical = Space12)) {
                                            Text(
                                                text = "Total",
                                                style = MicroMetadata,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Spacer(modifier = Modifier.height(Space4))
                                            Text(
                                                text = "${NumberFormatter.formatAmount(expResult.total)} ${expResult.currency} · $selectedCategoryName · $monthCountText",
                                                style = CardTitleAmount,
                                                color = FinTrackTheme.colors.expense,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }

                                    // Monthly Average Metric
                                    Surface(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = ShapeGroupedItemBottom,
                                        color = MaterialTheme.colorScheme.surfaceContainer
                                    ) {
                                        Column(modifier = Modifier.padding(horizontal = Space16, vertical = Space12)) {
                                            Text(
                                                text = "Monthly Average",
                                                style = MicroMetadata,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Spacer(modifier = Modifier.height(Space4))
                                            Text(
                                                text = "${NumberFormatter.formatAmount(expResult.monthlyAverage)} ${expResult.currency} / month · $selectedCategoryName",
                                                style = CardTitleAmount,
                                                color = FinTrackTheme.colors.expense,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(Space20))

            // ==========================================
            // [SECTION 3] Income by Source Exploration
            // ==========================================
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Space16)
                    .testTag("analytics_income_source_card"),
                shape = ShapeGroupedContainer,
                color = MaterialTheme.colorScheme.surfaceContainerLow
            ) {
                Column(modifier = Modifier.padding(Space16)) {
                    val incRankings = analyticsUiState.incomeSourceRankings
                    val selectedRankingItem = incRankings.find {
                        it.categoryName == analyticsUiState.selectedIncomeSource
                    } ?: incRankings.firstOrNull()

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f, fill = false)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(ShapeSquircleIcon)
                                    .background(FinTrackTheme.colors.income.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.TrendingUp,
                                    contentDescription = null,
                                    tint = FinTrackTheme.colors.income,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(Space8))
                            Text(
                                text = "Income by Source",
                                style = SectionHeadline,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.semantics { heading() }
                            )
                        }

                        if (incRankings.isNotEmpty()) {
                            Spacer(modifier = Modifier.width(Space8))
                            FinTrackDropdownSelector(
                                selectedItem = selectedRankingItem,
                                items = incRankings,
                                onItemSelected = { onIncomeSourceSelectionChanged(it.categoryName) },
                                itemLabel = { it.categoryName },
                                itemDropdownLabel = { "${it.categoryName} · ${String.format(Locale.US, "%.1f%%", it.percentage)}" },
                                testTag = "analytics_income_source_selector"
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(Space16))

                    if (incRankings.isEmpty()) {
                        FinTrackEmptyState(
                            title = "No Income Data",
                            description = "No income sources recorded for this period",
                            icon = Icons.Default.TrendingUp,
                            iconTint = MaterialTheme.colorScheme.onSurfaceVariant,
                            compact = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp)
                        )
                    } else {
                        val incResult = analyticsUiState.incomeSourceResult
                        val selectedSourceName = selectedRankingItem?.categoryName ?: ""
                        val monthCountText = if (incResult.monthCount == 1) "1 month" else "${incResult.monthCount} months"

                        AnimatedContent(
                            targetState = selectedSourceName,
                            transitionSpec = {
                                if (isReducedMotion) {
                                    fadeIn(animationSpec = snap()) togetherWith fadeOut(animationSpec = snap())
                                } else {
                                    fadeIn(animationSpec = FinTrackMotion.selectionSpring()) togetherWith
                                            fadeOut(animationSpec = FinTrackMotion.selectionSpring())
                                }
                            },
                            label = "incomeSourceChartContent"
                        ) { _ ->
                            Column {
                                SingleSeriesSplineChart(
                                    dataPoints = incResult.dataPoints,
                                    currency = incResult.currency,
                                    lineColor = FinTrackTheme.colors.income
                                )

                                Spacer(modifier = Modifier.height(Space16))

                                // Integrated Metric Cluster (Top & Bottom Grouped Surfaces)
                                Column(verticalArrangement = Arrangement.spacedBy(Space8)) {
                                    // Total Metric
                                    Surface(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = ShapeGroupedItemTop,
                                        color = MaterialTheme.colorScheme.surfaceContainer
                                    ) {
                                        Column(modifier = Modifier.padding(horizontal = Space16, vertical = Space12)) {
                                            Text(
                                                text = "Total",
                                                style = MicroMetadata,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Spacer(modifier = Modifier.height(Space4))
                                            Text(
                                                text = "${NumberFormatter.formatAmount(incResult.total)} ${incResult.currency} · $selectedSourceName · $monthCountText",
                                                style = CardTitleAmount,
                                                color = FinTrackTheme.colors.income,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }

                                    // Monthly Average Metric
                                    Surface(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = ShapeGroupedItemBottom,
                                        color = MaterialTheme.colorScheme.surfaceContainer
                                    ) {
                                        Column(modifier = Modifier.padding(horizontal = Space16, vertical = Space12)) {
                                            Text(
                                                text = "Monthly Average",
                                                style = MicroMetadata,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Spacer(modifier = Modifier.height(Space4))
                                            Text(
                                                text = "${NumberFormatter.formatAmount(incResult.monthlyAverage)} ${incResult.currency} / month · $selectedSourceName",
                                                style = CardTitleAmount,
                                                color = FinTrackTheme.colors.income,
                                                fontWeight = FontWeight.Bold
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
    }
}
