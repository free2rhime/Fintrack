package com.example

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.example.data.model.FilterSettings
import com.example.data.model.TransactionEntity
import com.example.domain.analytics.CategoryExpenseShare
import com.example.domain.analytics.DashboardMetrics
import com.example.domain.analytics.MonthlyDataPoint
import com.example.domain.analytics.SmartFinancialInsights
import com.example.ui.screens.DashboardScreen
import com.example.ui.theme.FinTrackTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class Checkpoint6DashboardAssemblyTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun createSampleTransaction(
        id: String,
        desc: String,
        amountRON: Double,
        amountEUR: Double,
        category: String,
        type: String,
        date: String = "2026-03-09"
    ) = TransactionEntity(
        id = id,
        householdId = "household-alpha",
        description = desc,
        amountRON = amountRON,
        amountEUR = amountEUR,
        type = type,
        category = category,
        subCategory = "",
        date = date,
        account = "Checking",
        exchangeRate = 4.9765,
        exchangeRateDate = "2026-03-09",
        conversionStatus = "OFFICIAL",
        exchangeRateSource = "BNR_OFFICIAL"
    )

    private fun createStandardDashboardMetrics(
        balance: Double = 45200.0,
        currency: String = "RON",
        secondaryBalance: Double? = 9082.0,
        hasIncompleteEur: Boolean = false
    ) = DashboardMetrics(
        balance = balance,
        currency = currency,
        totalIncome = 62000.0,
        totalExpense = 16800.0,
        savingsRate = 72.9,
        expensePressure = 27.1,
        secondaryCurrency = if (currency == "RON") "EUR" else "RON",
        secondaryCurrencyBalance = secondaryBalance,
        latestBnrRate = 4.9765,
        effectiveBnrDate = "2026-03-09",
        bnrStatus = "OFFICIAL",
        periodLabel = "This Month",
        hasIncompleteEurData = hasIncompleteEur,
        excludedNonOfficialCount = if (hasIncompleteEur) 3 else 0
    )

    // =========================================================================
    // 1. COMPLETE DASHBOARD HIERARCHY & ASSEMBLY VERIFICATION
    // =========================================================================

    @Test
    fun test01_CompleteDashboardHierarchyAssembledInCorrectSequence() {
        val metrics = createStandardDashboardMetrics()
        val insights = SmartFinancialInsights(
            savingsTrendText = "Strong Capital Growth",
            monthOverMonthExpenseChangePercent = -8.5,
            avgMonthlyExpense = 16800.0,
            avgMonthlyIncome = 62000.0
        )
        val dataPoints = listOf(
            MonthlyDataPoint("Jan 2026", income = 50000.0, expense = 15000.0, balance = 35000.0),
            MonthlyDataPoint("Feb 2026", income = 55000.0, expense = 14000.0, balance = 41000.0),
            MonthlyDataPoint("Mar 2026", income = 62000.0, expense = 16800.0, balance = 45200.0)
        )
        val shares = listOf(
            CategoryExpenseShare(categoryName = "Housing", totalAmount = 8000.0, percentage = 47.6, transactionCount = 2),
            CategoryExpenseShare(categoryName = "Food & Dining", totalAmount = 5000.0, percentage = 29.8, transactionCount = 10)
        )
        val recentTxs = listOf(
            createSampleTransaction("tx1", "Supermarket Restock", 350.0, 70.33, "Food & Dining", "Expense"),
            createSampleTransaction("tx2", "Client Invoice", 12000.0, 2411.33, "Salary", "Income")
        )

        composeTestRule.setContent {
            FinTrackTheme {
                DashboardScreen(
                    metrics = metrics,
                    filterSettings = FilterSettings(selectedCurrency = "RON", selectedPeriod = "This Month"),
                    monthlyDataPoints = dataPoints,
                    categoryShares = shares,
                    smartInsights = insights,
                    recentTransactions = recentTxs,
                    onPeriodSelected = {},
                    onCurrencyChanged = {}
                )
            }
        }

        // 1. Header: Title & Currency Toggle
        composeTestRule.onNodeWithText("Dashboard").assertIsDisplayed()
        composeTestRule.onNodeWithTag("currency_toggle_RON").assertIsDisplayed()
        composeTestRule.onNodeWithTag("currency_toggle_EUR").assertIsDisplayed()

        // 2. Unified Hero Canvas
        composeTestRule.onNodeWithTag("unified_hero_canvas").assertIsDisplayed()
        composeTestRule.onNodeWithTag("dashboard_top_card").assertIsDisplayed()
        composeTestRule.onNodeWithText("NET BALANCE").assertIsDisplayed()
        composeTestRule.onNodeWithText("45 200 RON").assertIsDisplayed()
        composeTestRule.onNodeWithTag("hero_secondary_currency").assertIsDisplayed()
        composeTestRule.onNodeWithTag("hero_bnr_parity").assertIsDisplayed()

        // 3. Period Selector
        composeTestRule.onNodeWithTag("period_selector_dropdown").assertIsDisplayed()

        // 4. Financial Pulse Card
        composeTestRule.onNodeWithTag("financial_pulse_card").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Financial Pulse").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("financial_pulse_narrative").performScrollTo().assertIsDisplayed()

        // 5. Monthly Cash Flow Spline Chart
        composeTestRule.onNodeWithText("Monthly Cash Flow").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("cash_flow_spline_canvas").performScrollTo().assertIsDisplayed()

        // 6. Spending by Category Donut Chart
        composeTestRule.onNodeWithText("Spending by Category").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("category_donut_canvas").performScrollTo().assertIsDisplayed()

        // 7. Recent Activity Stream
        composeTestRule.onNodeWithTag("recent_activity_section").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Recent Activity").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("recent_tx_item_tx1").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("recent_tx_item_tx2").performScrollTo().assertIsDisplayed()
    }

    // =========================================================================
    // 2. USER INTERACTIONS & NAVIGATION CALLBACKS
    // =========================================================================

    @Test
    fun test02_CurrencyToggleCallbackInvokedCorrectly() {
        var newCurrency = ""
        val metrics = createStandardDashboardMetrics()

        composeTestRule.setContent {
            FinTrackTheme {
                DashboardScreen(
                    metrics = metrics,
                    filterSettings = FilterSettings(selectedCurrency = "RON", selectedPeriod = "This Month"),
                    monthlyDataPoints = emptyList(),
                    categoryShares = emptyList(),
                    smartInsights = SmartFinancialInsights(),
                    recentTransactions = emptyList(),
                    onPeriodSelected = {},
                    onCurrencyChanged = { newCurrency = it }
                )
            }
        }

        composeTestRule.onNodeWithTag("currency_toggle_EUR").performClick()
        assertEquals("EUR", newCurrency)
    }

    @Test
    fun test03_PeriodSelectionCallbackInvokedCorrectly() {
        var selectedPeriod = ""
        val metrics = createStandardDashboardMetrics()

        composeTestRule.setContent {
            FinTrackTheme {
                DashboardScreen(
                    metrics = metrics,
                    filterSettings = FilterSettings(selectedCurrency = "RON", selectedPeriod = "This Month"),
                    monthlyDataPoints = emptyList(),
                    categoryShares = emptyList(),
                    smartInsights = SmartFinancialInsights(),
                    recentTransactions = emptyList(),
                    onPeriodSelected = { selectedPeriod = it },
                    onCurrencyChanged = {}
                )
            }
        }

        // Scroll to and open dropdown
        composeTestRule.onNodeWithTag("period_selector_dropdown").performScrollTo().performClick()
        composeTestRule.waitForIdle()
        // Select "All Time"
        composeTestRule.onNodeWithText("All Time", useUnmergedTree = true).performClick()
        assertEquals("All Time", selectedPeriod)
    }

    @Test
    fun test04_ViewAllActivityCallbackNavigatesToTransactionsTab() {
        var viewAllTriggered = false
        val metrics = createStandardDashboardMetrics()

        composeTestRule.setContent {
            FinTrackTheme {
                DashboardScreen(
                    metrics = metrics,
                    filterSettings = FilterSettings(selectedCurrency = "RON", selectedPeriod = "This Month"),
                    monthlyDataPoints = emptyList(),
                    categoryShares = emptyList(),
                    smartInsights = SmartFinancialInsights(),
                    recentTransactions = emptyList(),
                    onPeriodSelected = {},
                    onCurrencyChanged = {},
                    onViewAllActivity = { viewAllTriggered = true }
                )
            }
        }

        composeTestRule.onNodeWithTag("view_all_activity_button").performScrollTo().performClick()
        assertTrue(viewAllTriggered)
    }

    @Test
    fun test05_TransactionPreviewItemClickTriggersEditCallback() {
        var clickedTx: TransactionEntity? = null
        val tx = createSampleTransaction("tx101", "Coffee Bean Order", 45.0, 9.04, "Food & Dining", "Expense")
        val metrics = createStandardDashboardMetrics()

        composeTestRule.setContent {
            FinTrackTheme {
                DashboardScreen(
                    metrics = metrics,
                    filterSettings = FilterSettings(selectedCurrency = "RON", selectedPeriod = "This Month"),
                    monthlyDataPoints = emptyList(),
                    categoryShares = emptyList(),
                    smartInsights = SmartFinancialInsights(),
                    recentTransactions = listOf(tx),
                    onPeriodSelected = {},
                    onCurrencyChanged = {},
                    onTransactionClicked = { clickedTx = it }
                )
            }
        }

        composeTestRule.onNodeWithTag("recent_tx_item_tx101").performScrollTo().performClick()
        assertEquals("tx101", clickedTx?.id)
        assertEquals("Coffee Bean Order", clickedTx?.description)
    }

    // =========================================================================
    // 3. INCOMPLETE EUR DATA WARNING CARD
    // =========================================================================

    @Test
    fun test06_IncompleteEurDataWarningCardRendersWhenFlagged() {
        val metricsWithWarning = createStandardDashboardMetrics(hasIncompleteEur = true)

        composeTestRule.setContent {
            FinTrackTheme {
                DashboardScreen(
                    metrics = metricsWithWarning,
                    filterSettings = FilterSettings(selectedCurrency = "EUR", selectedPeriod = "This Month"),
                    monthlyDataPoints = emptyList(),
                    categoryShares = emptyList(),
                    smartInsights = SmartFinancialInsights(),
                    recentTransactions = emptyList(),
                    onPeriodSelected = {},
                    onCurrencyChanged = {}
                )
            }
        }

        composeTestRule.onNodeWithTag("eur_incomplete_warning_card").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("EUR totals are incomplete: 3 transaction(s) pending or unverified BNR exchange rate excluded. Complete RON data remains available.").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun test07_IncompleteEurDataWarningCardAbsentWhenDataComplete() {
        val metricsComplete = createStandardDashboardMetrics(hasIncompleteEur = false)

        composeTestRule.setContent {
            FinTrackTheme {
                DashboardScreen(
                    metrics = metricsComplete,
                    filterSettings = FilterSettings(selectedCurrency = "RON", selectedPeriod = "This Month"),
                    monthlyDataPoints = emptyList(),
                    categoryShares = emptyList(),
                    smartInsights = SmartFinancialInsights(),
                    recentTransactions = emptyList(),
                    onPeriodSelected = {},
                    onCurrencyChanged = {}
                )
            }
        }

        composeTestRule.onNodeWithTag("eur_incomplete_warning_card").assertDoesNotExist()
    }

    // =========================================================================
    // 4. EMPTY DATASET STABILITY & CALM DEGRADED STATES
    // =========================================================================

    @Test
    fun test08_EmptyDatasetRendersCleanlyWithoutCrashing() {
        val emptyMetrics = DashboardMetrics()
        val emptyInsights = SmartFinancialInsights()

        composeTestRule.setContent {
            FinTrackTheme {
                DashboardScreen(
                    metrics = emptyMetrics,
                    filterSettings = FilterSettings(),
                    monthlyDataPoints = emptyList(),
                    categoryShares = emptyList(),
                    smartInsights = emptyInsights,
                    recentTransactions = emptyList(),
                    onPeriodSelected = {},
                    onCurrencyChanged = {}
                )
            }
        }

        // Hero renders 0 RON
        composeTestRule.onNodeWithTag("unified_hero_canvas").assertIsDisplayed()
        composeTestRule.onNodeWithText("0 RON").assertIsDisplayed()

        // Pulse card degraded state
        composeTestRule.onNodeWithTag("financial_pulse_card").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("financial_pulse_empty_state").performScrollTo().assertIsDisplayed()

        // Spline empty state
        composeTestRule.onNodeWithText("Monthly Cash Flow").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("No Cash Flow Data").performScrollTo().assertIsDisplayed()

        // Category empty state
        composeTestRule.onNodeWithText("Spending by Category").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("No Category Spending").performScrollTo().assertIsDisplayed()

        // Recent activity empty state
        composeTestRule.onNodeWithTag("recent_activity_section").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("recent_activity_empty_state").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("No Recent Activity").performScrollTo().assertIsDisplayed()
    }

    // =========================================================================
    // 5. RESPONSIVE BREAKPOINTS (360dp, 390dp, 412dp+)
    // =========================================================================

    @Test
    @Config(qualifiers = "w360dp-h800dp")
    fun test09_ResponsiveLayoutAt360dpViewport() {
        val metrics = createStandardDashboardMetrics(balance = 987654.0)

        composeTestRule.setContent {
            FinTrackTheme {
                DashboardScreen(
                    metrics = metrics,
                    filterSettings = FilterSettings(),
                    monthlyDataPoints = emptyList(),
                    categoryShares = emptyList(),
                    smartInsights = SmartFinancialInsights(),
                    recentTransactions = emptyList(),
                    onPeriodSelected = {},
                    onCurrencyChanged = {}
                )
            }
        }

        composeTestRule.onNodeWithTag("unified_hero_canvas").assertIsDisplayed()
        composeTestRule.onNodeWithText("987 654 RON").assertIsDisplayed()
        composeTestRule.onNodeWithTag("period_selector_dropdown").assertIsDisplayed()
        composeTestRule.onNodeWithTag("financial_pulse_card").performScrollTo().assertIsDisplayed()
    }

    @Test
    @Config(qualifiers = "w412dp-h915dp")
    fun test10_ResponsiveLayoutAt412dpViewport() {
        val metrics = createStandardDashboardMetrics(balance = 12500000.0)

        composeTestRule.setContent {
            FinTrackTheme {
                DashboardScreen(
                    metrics = metrics,
                    filterSettings = FilterSettings(),
                    monthlyDataPoints = emptyList(),
                    categoryShares = emptyList(),
                    smartInsights = SmartFinancialInsights(),
                    recentTransactions = emptyList(),
                    onPeriodSelected = {},
                    onCurrencyChanged = {}
                )
            }
        }

        composeTestRule.onNodeWithTag("unified_hero_canvas").assertIsDisplayed()
        composeTestRule.onNodeWithText("12 500 000 RON").assertIsDisplayed()
        composeTestRule.onNodeWithTag("financial_pulse_card").performScrollTo().assertIsDisplayed()
    }
}
