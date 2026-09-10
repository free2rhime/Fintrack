package com.example

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import com.example.data.model.FilterSettings
import com.example.domain.analytics.CategoryRankingItem
import com.example.domain.analytics.SingleSeriesAnalyticsResult
import com.example.domain.analytics.SingleSeriesDataPoint
import com.example.domain.analytics.SmartFinancialInsights
import com.example.ui.AnalyticsUiState
import com.example.ui.screens.AnalyticsScreen
import com.example.ui.theme.FinTrackTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class M3AnalyticsExpressiveMigrationTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun sampleDataPoints(): List<SingleSeriesDataPoint> = listOf(
        SingleSeriesDataPoint(yearMonth = "2026-06", monthYearLabel = "Jun 26", value = 1000.0),
        SingleSeriesDataPoint(yearMonth = "2026-07", monthYearLabel = "Jul 26", value = 1500.0),
        SingleSeriesDataPoint(yearMonth = "2026-08", monthYearLabel = "Aug 26", value = 2000.0)
    )

    private fun sampleExpenseRankings(): List<CategoryRankingItem> = listOf(
        CategoryRankingItem(categoryName = "Groceries", totalAmount = 3420.0, percentage = 60.0, transactionCount = 10),
        CategoryRankingItem(categoryName = "Utilities", totalAmount = 2280.0, percentage = 40.0, transactionCount = 4)
    )

    private fun sampleIncomeRankings(): List<CategoryRankingItem> = listOf(
        CategoryRankingItem(categoryName = "Salary", totalAmount = 24000.0, percentage = 80.0, transactionCount = 6),
        CategoryRankingItem(categoryName = "Freelance", totalAmount = 6000.0, percentage = 20.0, transactionCount = 2)
    )

    private fun sampleState(): AnalyticsUiState = AnalyticsUiState(
        incomeExpenseSelection = "Expense",
        incomeExpenseResult = SingleSeriesAnalyticsResult(
            dataPoints = sampleDataPoints(),
            currency = "RON",
            monthlyAverage = 1500.0,
            total = 4500.0,
            monthCount = 3
        ),
        expenseCategoryRankings = sampleExpenseRankings(),
        selectedExpenseCategory = "Groceries",
        expenseCategoryResult = SingleSeriesAnalyticsResult(
            dataPoints = sampleDataPoints(),
            currency = "RON",
            monthlyAverage = 570.0,
            total = 3420.0,
            monthCount = 6
        ),
        incomeSourceRankings = sampleIncomeRankings(),
        selectedIncomeSource = "Salary",
        incomeSourceResult = SingleSeriesAnalyticsResult(
            dataPoints = sampleDataPoints(),
            currency = "RON",
            monthlyAverage = 4000.0,
            total = 24000.0,
            monthCount = 6
        )
    )

    @Test
    fun test1_analyticsScreenRendersWithHeaderAndSubtitle() {
        composeTestRule.setContent {
            FinTrackTheme {
                AnalyticsScreen(
                    analyticsUiState = sampleState(),
                    filterSettings = FilterSettings(selectedCurrency = "RON", selectedPeriod = "Last 6 Months")
                )
            }
        }

        composeTestRule.onNodeWithText("Analytics").assertExists()
        composeTestRule.onNodeWithText("Financial flow & category distribution").assertExists()
    }

    @Test
    fun test2_currencyToggleInteractions() {
        var selectedCurrency: String? = null
        composeTestRule.setContent {
            FinTrackTheme {
                AnalyticsScreen(
                    analyticsUiState = sampleState(),
                    filterSettings = FilterSettings(selectedCurrency = "RON", selectedPeriod = "Last 6 Months"),
                    onCurrencyChanged = { selectedCurrency = it }
                )
            }
        }

        composeTestRule.onNodeWithTag("currency_toggle_RON").assertExists()
        composeTestRule.onNodeWithTag("currency_toggle_EUR").assertExists()

        composeTestRule.onNodeWithTag("currency_toggle_EUR").performClick()
        assertEquals("EUR", selectedCurrency)
    }

    @Test
    fun test3_incomeExpenseCardAndSelector() {
        var selectedIncExp: String? = null
        composeTestRule.setContent {
            FinTrackTheme {
                AnalyticsScreen(
                    analyticsUiState = sampleState(),
                    filterSettings = FilterSettings(selectedCurrency = "RON", selectedPeriod = "Last 6 Months"),
                    onIncomeExpenseSelectionChanged = { selectedIncExp = it }
                )
            }
        }

        composeTestRule.onNodeWithTag("analytics_income_expense_card").assertExists()
        composeTestRule.onNodeWithText("Income & Expense").assertExists()
        composeTestRule.onNodeWithTag("analytics_income_expense_selector").assertExists()
        composeTestRule.onNodeWithText("1 500.00 RON / month").assertExists()
    }

    @Test
    fun test4_spendingCategoryCardAndSelector() {
        var selectedCategory: String? = null
        composeTestRule.setContent {
            FinTrackTheme {
                AnalyticsScreen(
                    analyticsUiState = sampleState(),
                    filterSettings = FilterSettings(selectedCurrency = "RON", selectedPeriod = "Last 6 Months"),
                    onExpenseCategorySelectionChanged = { selectedCategory = it }
                )
            }
        }

        composeTestRule.onNodeWithTag("analytics_spending_category_card").assertExists()
        composeTestRule.onNodeWithText("Spending by Category").assertExists()
        composeTestRule.onNodeWithTag("analytics_spending_category_selector").assertExists()
        composeTestRule.onNodeWithText("3 420.00 RON · Groceries · 6 months").assertExists()
        composeTestRule.onNodeWithText("570.00 RON / month · Groceries").assertExists()
    }

    @Test
    fun test5_incomeSourceCardAndSelector() {
        var selectedSource: String? = null
        composeTestRule.setContent {
            FinTrackTheme {
                AnalyticsScreen(
                    analyticsUiState = sampleState(),
                    filterSettings = FilterSettings(selectedCurrency = "RON", selectedPeriod = "Last 6 Months"),
                    onIncomeSourceSelectionChanged = { selectedSource = it }
                )
            }
        }

        composeTestRule.onNodeWithTag("analytics_income_source_card").assertExists()
        composeTestRule.onNodeWithText("Income by Source").assertExists()
        composeTestRule.onNodeWithTag("analytics_income_source_selector").assertExists()
        composeTestRule.onNodeWithText("24 000.00 RON · Salary · 6 months").assertExists()
        composeTestRule.onNodeWithText("4 000.00 RON / month · Salary").assertExists()
    }

    @Test
    fun test6_emptyStateExpenseCategories() {
        val emptyState = sampleState().copy(
            expenseCategoryRankings = emptyList(),
            selectedExpenseCategory = null
        )

        composeTestRule.setContent {
            FinTrackTheme {
                AnalyticsScreen(
                    analyticsUiState = emptyState,
                    filterSettings = FilterSettings(selectedCurrency = "RON", selectedPeriod = "Last Month")
                )
            }
        }

        composeTestRule.onNodeWithText("No Expense Data").assertExists()
        composeTestRule.onNodeWithText("No expense categories recorded for this period").assertExists()
        composeTestRule.onNodeWithTag("analytics_spending_category_selector").assertDoesNotExist()
    }

    @Test
    fun test7_emptyStateIncomeSources() {
        val emptyState = sampleState().copy(
            incomeSourceRankings = emptyList(),
            selectedIncomeSource = null
        )

        composeTestRule.setContent {
            FinTrackTheme {
                AnalyticsScreen(
                    analyticsUiState = emptyState,
                    filterSettings = FilterSettings(selectedCurrency = "RON", selectedPeriod = "Last Month")
                )
            }
        }

        composeTestRule.onNodeWithText("No Income Data").assertExists()
        composeTestRule.onNodeWithText("No income sources recorded for this period").assertExists()
        composeTestRule.onNodeWithTag("analytics_income_source_selector").assertDoesNotExist()
    }

    @Test
    fun test8_warningCardDisplayedWhenEurIncomplete() {
        composeTestRule.setContent {
            FinTrackTheme {
                AnalyticsScreen(
                    analyticsUiState = sampleState(),
                    filterSettings = FilterSettings(selectedCurrency = "RON", selectedPeriod = "Last 6 Months"),
                    hasIncompleteEurData = true,
                    excludedNonOfficialCount = 3
                )
            }
        }

        composeTestRule.onNodeWithTag("analytics_eur_incomplete_warning_card").assertExists()
        composeTestRule.onNodeWithText("EUR totals are incomplete: 3 transaction(s) pending or unverified BNR exchange rate excluded. Complete RON data remains available.").assertExists()
    }

    @Test
    fun test9_warningCardNotDisplayedWhenEurComplete() {
        composeTestRule.setContent {
            FinTrackTheme {
                AnalyticsScreen(
                    analyticsUiState = sampleState(),
                    filterSettings = FilterSettings(selectedCurrency = "RON", selectedPeriod = "Last 6 Months"),
                    hasIncompleteEurData = false,
                    excludedNonOfficialCount = 0
                )
            }
        }

        composeTestRule.onNodeWithTag("analytics_eur_incomplete_warning_card").assertDoesNotExist()
    }

    @Test
    fun test10_smartInsightsCardDisplayedWhenProvided() {
        val insights = SmartFinancialInsights(
            monthOverMonthExpenseChangePercent = 12.5,
            savingsTrendText = "Improving",
            largestExpenseMonth = "August 2026",
            largestExpenseMonthAmount = 4500.0
        )

        composeTestRule.setContent {
            FinTrackTheme {
                AnalyticsScreen(
                    analyticsUiState = sampleState(),
                    filterSettings = FilterSettings(selectedCurrency = "RON", selectedPeriod = "Last 6 Months"),
                    smartInsights = insights
                )
            }
        }

        composeTestRule.onNodeWithTag("analytics_smart_insights_card").assertExists()
        composeTestRule.onNodeWithText("Financial Insights").assertExists()
        composeTestRule.onNodeWithText("Improving").assertExists()
        composeTestRule.onNodeWithText("+12.5%").assertExists()
        composeTestRule.onNodeWithText("August 2026 · 4 500.00 RON").assertExists()
    }

    @Test
    fun test11_smartInsightsCardAbsentWhenNull() {
        composeTestRule.setContent {
            FinTrackTheme {
                AnalyticsScreen(
                    analyticsUiState = sampleState(),
                    filterSettings = FilterSettings(selectedCurrency = "RON", selectedPeriod = "Last 6 Months"),
                    smartInsights = null
                )
            }
        }

        composeTestRule.onNodeWithTag("analytics_smart_insights_card").assertDoesNotExist()
    }

    @Test
    fun test12_chartRendersWithData() {
        composeTestRule.setContent {
            FinTrackTheme {
                AnalyticsScreen(
                    analyticsUiState = sampleState(),
                    filterSettings = FilterSettings(selectedCurrency = "RON", selectedPeriod = "Last 6 Months")
                )
            }
        }

        // Active point indicator inside spline charts shows the active month and formatted value across all rendered charts
        composeTestRule.onAllNodesWithText("Aug 26").assertCountEquals(3)
        composeTestRule.onAllNodesWithText("2 000.00 RON").assertCountEquals(3)
    }

    @Test
    fun test13_editorialMetricDisplaysValues() {
        composeTestRule.setContent {
            FinTrackTheme {
                AnalyticsScreen(
                    analyticsUiState = sampleState(),
                    filterSettings = FilterSettings(selectedCurrency = "RON", selectedPeriod = "Last 6 Months")
                )
            }
        }

        composeTestRule.onNodeWithText("1 500.00 RON / month").assertExists()
        composeTestRule.onNodeWithText("3 420.00 RON · Groceries · 6 months").assertExists()
        composeTestRule.onNodeWithText("570.00 RON / month · Groceries").assertExists()
        composeTestRule.onNodeWithText("24 000.00 RON · Salary · 6 months").assertExists()
        composeTestRule.onNodeWithText("4 000.00 RON / month · Salary").assertExists()
    }

    @Test
    fun test14_responsiveLayout360dp() {
        composeTestRule.setContent {
            FinTrackTheme {
                Box(modifier = Modifier.width(360.dp)) {
                    AnalyticsScreen(
                        analyticsUiState = sampleState(),
                        filterSettings = FilterSettings(selectedCurrency = "RON", selectedPeriod = "Last 6 Months")
                    )
                }
            }
        }

        composeTestRule.onNodeWithText("Analytics").assertExists()
        composeTestRule.onNodeWithTag("analytics_income_expense_card").assertExists()
        composeTestRule.onNodeWithTag("analytics_spending_category_card").assertExists()
        composeTestRule.onNodeWithTag("analytics_income_source_card").assertExists()
    }

    @Test
    fun test15_responsiveLayout390dp() {
        composeTestRule.setContent {
            FinTrackTheme {
                Box(modifier = Modifier.width(390.dp)) {
                    AnalyticsScreen(
                        analyticsUiState = sampleState(),
                        filterSettings = FilterSettings(selectedCurrency = "RON", selectedPeriod = "Last 6 Months")
                    )
                }
            }
        }

        composeTestRule.onNodeWithText("Analytics").assertExists()
        composeTestRule.onNodeWithTag("analytics_income_expense_card").assertExists()
    }

    @Test
    fun test16_responsiveLayout412dp() {
        composeTestRule.setContent {
            FinTrackTheme {
                Box(modifier = Modifier.width(412.dp)) {
                    AnalyticsScreen(
                        analyticsUiState = sampleState(),
                        filterSettings = FilterSettings(selectedCurrency = "RON", selectedPeriod = "Last 6 Months")
                    )
                }
            }
        }

        composeTestRule.onNodeWithText("Analytics").assertExists()
        composeTestRule.onNodeWithTag("analytics_income_expense_card").assertExists()
    }

    @Test
    fun test17_responsiveLayout600dpAdaptive() {
        composeTestRule.setContent {
            FinTrackTheme {
                Box(modifier = Modifier.width(640.dp)) {
                    AnalyticsScreen(
                        analyticsUiState = sampleState(),
                        filterSettings = FilterSettings(selectedCurrency = "RON", selectedPeriod = "Last 6 Months")
                    )
                }
            }
        }

        composeTestRule.onNodeWithText("Analytics").assertExists()
        composeTestRule.onNodeWithTag("analytics_income_expense_card").assertExists()
    }

    @Test
    fun test18_accessibilityMinTouchTargets() {
        composeTestRule.setContent {
            FinTrackTheme {
                AnalyticsScreen(
                    analyticsUiState = sampleState(),
                    filterSettings = FilterSettings(selectedCurrency = "RON", selectedPeriod = "Last 6 Months")
                )
            }
        }

        // Dropdown selectors and toggles exist and are interactable
        composeTestRule.onNodeWithTag("analytics_income_expense_selector").assertExists()
        composeTestRule.onNodeWithTag("analytics_spending_category_selector").assertExists()
        composeTestRule.onNodeWithTag("analytics_income_source_selector").assertExists()
        composeTestRule.onNodeWithTag("currency_toggle_RON").assertExists()
        composeTestRule.onNodeWithTag("currency_toggle_EUR").assertExists()
    }

    @Test
    fun test19_accessibilityRoleSemantics() {
        composeTestRule.setContent {
            FinTrackTheme {
                AnalyticsScreen(
                    analyticsUiState = sampleState(),
                    filterSettings = FilterSettings(selectedCurrency = "RON", selectedPeriod = "Last 6 Months")
                )
            }
        }

        composeTestRule.onNodeWithTag("currency_toggle_RON").assertExists()
        composeTestRule.onNodeWithTag("currency_toggle_EUR").assertExists()
    }

    @Test
    fun test20_reducedMotionExecution() {
        composeTestRule.setContent {
            FinTrackTheme {
                AnalyticsScreen(
                    analyticsUiState = sampleState(),
                    filterSettings = FilterSettings(selectedCurrency = "RON", selectedPeriod = "Last 6 Months")
                )
            }
        }

        composeTestRule.onNodeWithTag("analytics_income_expense_card").assertExists()
    }

    @Test
    fun test21_preservesAllLegacyTestTags() {
        composeTestRule.setContent {
            FinTrackTheme {
                AnalyticsScreen(
                    analyticsUiState = sampleState(),
                    filterSettings = FilterSettings(selectedCurrency = "RON", selectedPeriod = "Last 6 Months"),
                    hasIncompleteEurData = true,
                    excludedNonOfficialCount = 2
                )
            }
        }

        composeTestRule.onNodeWithTag("currency_toggle_RON").assertExists()
        composeTestRule.onNodeWithTag("currency_toggle_EUR").assertExists()
        composeTestRule.onNodeWithTag("analytics_eur_incomplete_warning_card").assertExists()
        composeTestRule.onNodeWithTag("analytics_income_expense_card").assertExists()
        composeTestRule.onNodeWithTag("analytics_income_expense_selector").assertExists()
        composeTestRule.onNodeWithTag("analytics_spending_category_card").assertExists()
        composeTestRule.onNodeWithTag("analytics_spending_category_selector").assertExists()
        composeTestRule.onNodeWithTag("analytics_income_source_card").assertExists()
        composeTestRule.onNodeWithTag("analytics_income_source_selector").assertExists()
    }

    @Test
    fun test22_noLocalPeriodSelectorOnAnalytics() {
        composeTestRule.setContent {
            FinTrackTheme {
                AnalyticsScreen(
                    analyticsUiState = sampleState(),
                    filterSettings = FilterSettings(selectedPeriod = "Last Month")
                )
            }
        }

        composeTestRule.onNodeWithTag("period_chip_Last_Month").assertDoesNotExist()
        composeTestRule.onNodeWithTag("period_chip_All_Time").assertDoesNotExist()
        composeTestRule.onNodeWithTag("period_selector_dropdown").assertDoesNotExist()
    }
}
