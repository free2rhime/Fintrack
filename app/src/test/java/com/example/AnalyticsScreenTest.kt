package com.example

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.data.model.FilterSettings
import com.example.domain.analytics.CategoryRankingItem
import com.example.domain.analytics.SingleSeriesAnalyticsResult
import com.example.domain.analytics.SingleSeriesDataPoint
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
class AnalyticsScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testAnalyticsScreenRendersAllThreeSections() {
        val sampleDataPoints = listOf(
            SingleSeriesDataPoint(yearMonth = "2026-06", monthYearLabel = "Jun 26", value = 1000.0),
            SingleSeriesDataPoint(yearMonth = "2026-07", monthYearLabel = "Jul 26", value = 1500.0),
            SingleSeriesDataPoint(yearMonth = "2026-08", monthYearLabel = "Aug 26", value = 2000.0)
        )

        val expRankings = listOf(
            CategoryRankingItem(categoryName = "Groceries", totalAmount = 3420.0, percentage = 60.0, transactionCount = 10),
            CategoryRankingItem(categoryName = "Utilities", totalAmount = 2280.0, percentage = 40.0, transactionCount = 4)
        )

        val incRankings = listOf(
            CategoryRankingItem(categoryName = "Salary", totalAmount = 24000.0, percentage = 80.0, transactionCount = 6),
            CategoryRankingItem(categoryName = "Freelance", totalAmount = 6000.0, percentage = 20.0, transactionCount = 2)
        )

        val state = AnalyticsUiState(
            incomeExpenseSelection = "Expense",
            incomeExpenseResult = SingleSeriesAnalyticsResult(
                dataPoints = sampleDataPoints,
                currency = "RON",
                monthlyAverage = 1500.0,
                total = 4500.0,
                monthCount = 3
            ),
            expenseCategoryRankings = expRankings,
            selectedExpenseCategory = "Groceries",
            expenseCategoryResult = SingleSeriesAnalyticsResult(
                dataPoints = sampleDataPoints,
                currency = "RON",
                monthlyAverage = 570.0,
                total = 3420.0,
                monthCount = 6
            ),
            incomeSourceRankings = incRankings,
            selectedIncomeSource = "Salary",
            incomeSourceResult = SingleSeriesAnalyticsResult(
                dataPoints = sampleDataPoints,
                currency = "RON",
                monthlyAverage = 4000.0,
                total = 24000.0,
                monthCount = 6
            )
        )

        var selectedCurrency: String? = null
        var selectedIncExp: String? = null
        var selectedExpCat: String? = null
        var selectedIncSrc: String? = null

        composeTestRule.setContent {
            FinTrackTheme {
                AnalyticsScreen(
                    analyticsUiState = state,
                    filterSettings = FilterSettings(selectedCurrency = "RON", selectedPeriod = "Last 6 Months"),
                    hasIncompleteEurData = true,
                    excludedNonOfficialCount = 2,
                    onCurrencyChanged = { selectedCurrency = it },
                    onIncomeExpenseSelectionChanged = { selectedIncExp = it },
                    onExpenseCategorySelectionChanged = { selectedExpCat = it },
                    onIncomeSourceSelectionChanged = { selectedIncSrc = it }
                )
            }
        }

        // 1. Header & Warning
        composeTestRule.onNodeWithText("Analytics").assertExists()
        composeTestRule.onNodeWithTag("currency_toggle_RON").assertExists()
        composeTestRule.onNodeWithTag("currency_toggle_EUR").assertExists()
        composeTestRule.onNodeWithTag("analytics_eur_incomplete_warning_card").assertExists()

        // 2. Section 1: Income & Expense
        composeTestRule.onNodeWithTag("analytics_income_expense_card").assertExists()
        composeTestRule.onNodeWithText("Income & Expense").assertExists()
        composeTestRule.onNodeWithTag("analytics_income_expense_selector").assertExists()
        composeTestRule.onNodeWithText("1 500.00 RON / month").assertExists()

        // 3. Section 2: Spending by Category
        composeTestRule.onNodeWithTag("analytics_spending_category_card").assertExists()
        composeTestRule.onNodeWithText("Spending by Category").assertExists()
        composeTestRule.onNodeWithTag("analytics_spending_category_selector").assertExists()
        composeTestRule.onNodeWithText("3 420.00 RON · Groceries · 6 months").assertExists()
        composeTestRule.onNodeWithText("570.00 RON / month · Groceries").assertExists()

        // 4. Section 3: Income by Source
        composeTestRule.onNodeWithTag("analytics_income_source_card").assertExists()
        composeTestRule.onNodeWithText("Income by Source").assertExists()
        composeTestRule.onNodeWithTag("analytics_income_source_selector").assertExists()
        composeTestRule.onNodeWithText("24 000.00 RON · Salary · 6 months").assertExists()
        composeTestRule.onNodeWithText("4 000.00 RON / month · Salary").assertExists()

        // 5. Test Currency toggle click
        composeTestRule.onNodeWithTag("currency_toggle_EUR").performClick()
        assertEquals("EUR", selectedCurrency)
    }

    @Test
    fun testAnalyticsScreenEmptyStates() {
        val emptyState = AnalyticsUiState(
            incomeExpenseSelection = "Expense",
            incomeExpenseResult = SingleSeriesAnalyticsResult(
                dataPoints = emptyList(),
                currency = "RON",
                monthlyAverage = 0.0,
                total = 0.0,
                monthCount = 0
            ),
            expenseCategoryRankings = emptyList(),
            selectedExpenseCategory = null,
            expenseCategoryResult = SingleSeriesAnalyticsResult(
                dataPoints = emptyList(),
                currency = "RON",
                monthlyAverage = 0.0,
                total = 0.0,
                monthCount = 0
            ),
            incomeSourceRankings = emptyList(),
            selectedIncomeSource = null,
            incomeSourceResult = SingleSeriesAnalyticsResult(
                dataPoints = emptyList(),
                currency = "RON",
                monthlyAverage = 0.0,
                total = 0.0,
                monthCount = 0
            )
        )

        composeTestRule.setContent {
            FinTrackTheme {
                AnalyticsScreen(
                    analyticsUiState = emptyState,
                    filterSettings = FilterSettings(selectedCurrency = "RON", selectedPeriod = "Last Month"),
                    hasIncompleteEurData = false
                )
            }
        }

        // Section 1 has chart empty state
        composeTestRule.onNodeWithText("0.00 RON / month").assertExists()

        // Section 2 has empty state
        composeTestRule.onNodeWithText("No Expense Data").assertExists()
        composeTestRule.onNodeWithText("No expense categories recorded for this period").assertExists()

        // Section 3 has empty state
        composeTestRule.onNodeWithText("No Income Data").assertExists()
        composeTestRule.onNodeWithText("No income sources recorded for this period").assertExists()

        // Warning card should NOT be displayed when hasIncompleteEurData = false
        composeTestRule.onNodeWithTag("analytics_eur_incomplete_warning_card").assertDoesNotExist()
    }
}
