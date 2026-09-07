package com.example.ui.screens

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
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.FilterSettings
import com.example.data.util.NumberFormatter
import com.example.ui.AnalyticsUiState
import com.example.ui.components.CurrencyToggle
import com.example.ui.components.FinTrackCard
import com.example.ui.components.FinTrackDropdownSelector
import com.example.ui.components.FinTrackEmptyState
import com.example.ui.components.SingleSeriesSplineChart
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
import com.example.ui.theme.WarningAmber
import java.util.Locale

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
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
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
            // Header Region: Title + Currency Toggle
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
                        text = "Analytics",
                        style = SectionHeadline,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                CurrencyToggle(
                    selectedCurrency = filterSettings.selectedCurrency,
                    onCurrencyChanged = onCurrencyChanged
                )
            }

            // Incomplete EUR warning if applicable
            if (hasIncompleteEurData) {
                Spacer(modifier = Modifier.height(Space12))
                FinTrackCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Space16)
                        .testTag("analytics_eur_incomplete_warning_card"),
                    shape = RoundedCornerShape(RadiusMedium),
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
                            text = "EUR totals are incomplete: $excludedNonOfficialCount transaction(s) pending or unverified BNR exchange rate excluded. Complete RON data remains available.",
                            style = MicroMetadata,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(Space16))

            // ==========================================
            // [SECTION 1] Income & Expense
            // ==========================================
            FinTrackCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Space16)
                    .testTag("analytics_income_expense_card"),
                shape = RoundedCornerShape(RadiusLarge),
                contentPadding = Space16
            ) {
                Column {
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
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(CobaltBlue.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ShowChart,
                                    contentDescription = null,
                                    tint = CobaltBlue,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(Space8))
                            Text(
                                text = "Income & Expense",
                                style = SectionHeadline,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
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
                    val lineColor = if (isIncome) IncomeEmerald else ExpenseCoral

                    SingleSeriesSplineChart(
                        dataPoints = analyticsUiState.incomeExpenseResult.dataPoints,
                        currency = analyticsUiState.incomeExpenseResult.currency,
                        lineColor = lineColor
                    )

                    Spacer(modifier = Modifier.height(Space16))

                    // Monthly Average KPI
                    FinTrackCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(RadiusMedium),
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        contentPadding = Space12
                    ) {
                        Column {
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

            Spacer(modifier = Modifier.height(Space20))

            // ==========================================
            // [SECTION 2] Spending by Category
            // ==========================================
            FinTrackCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Space16)
                    .testTag("analytics_spending_category_card"),
                shape = RoundedCornerShape(RadiusLarge),
                contentPadding = Space16
            ) {
                Column {
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
                                text = "Spending by Category",
                                style = SectionHeadline,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
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

                        SingleSeriesSplineChart(
                            dataPoints = expResult.dataPoints,
                            currency = expResult.currency,
                            lineColor = ExpenseCoral
                        )

                        Spacer(modifier = Modifier.height(Space16))

                        Column(verticalArrangement = Arrangement.spacedBy(Space8)) {
                            // Total KPI
                            FinTrackCard(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(RadiusMedium),
                                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                contentPadding = Space12
                            ) {
                                Column {
                                    Text(
                                        text = "Total",
                                        style = MicroMetadata,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(Space4))
                                    Text(
                                        text = "${NumberFormatter.formatAmount(expResult.total)} ${expResult.currency} · $selectedCategoryName · $monthCountText",
                                        style = CardTitleAmount,
                                        color = ExpenseCoral,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            // Monthly Average KPI
                            FinTrackCard(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(RadiusMedium),
                                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                contentPadding = Space12
                            ) {
                                Column {
                                    Text(
                                        text = "Monthly Average",
                                        style = MicroMetadata,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(Space4))
                                    Text(
                                        text = "${NumberFormatter.formatAmount(expResult.monthlyAverage)} ${expResult.currency} / month · $selectedCategoryName",
                                        style = CardTitleAmount,
                                        color = ExpenseCoral,
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
            // [SECTION 3] Income by Source
            // ==========================================
            FinTrackCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Space16)
                    .testTag("analytics_income_source_card"),
                shape = RoundedCornerShape(RadiusLarge),
                contentPadding = Space16
            ) {
                Column {
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
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(IncomeEmerald.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.TrendingUp,
                                    contentDescription = null,
                                    tint = IncomeEmerald,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(Space8))
                            Text(
                                text = "Income by Source",
                                style = SectionHeadline,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
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

                        SingleSeriesSplineChart(
                            dataPoints = incResult.dataPoints,
                            currency = incResult.currency,
                            lineColor = IncomeEmerald
                        )

                        Spacer(modifier = Modifier.height(Space16))

                        Column(verticalArrangement = Arrangement.spacedBy(Space8)) {
                            // Total KPI
                            FinTrackCard(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(RadiusMedium),
                                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                contentPadding = Space12
                            ) {
                                Column {
                                    Text(
                                        text = "Total",
                                        style = MicroMetadata,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(Space4))
                                    Text(
                                        text = "${NumberFormatter.formatAmount(incResult.total)} ${incResult.currency} · $selectedSourceName · $monthCountText",
                                        style = CardTitleAmount,
                                        color = IncomeEmerald,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            // Monthly Average KPI
                            FinTrackCard(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(RadiusMedium),
                                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                contentPadding = Space12
                            ) {
                                Column {
                                    Text(
                                        text = "Monthly Average",
                                        style = MicroMetadata,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(Space4))
                                    Text(
                                        text = "${NumberFormatter.formatAmount(incResult.monthlyAverage)} ${incResult.currency} / month · $selectedSourceName",
                                        style = CardTitleAmount,
                                        color = IncomeEmerald,
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
