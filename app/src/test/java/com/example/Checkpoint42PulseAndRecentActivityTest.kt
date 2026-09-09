package com.example

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.example.data.model.FilterSettings
import com.example.data.model.TransactionEntity
import com.example.domain.analytics.DashboardMetrics
import com.example.domain.analytics.SmartFinancialInsights
import com.example.ui.components.FinancialPulseCard
import com.example.ui.components.RecentActivitySection
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
class Checkpoint42PulseAndRecentActivityTest {

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
        householdId = "test-household",
        description = desc,
        amountRON = amountRON,
        amountEUR = amountEUR,
        type = type,
        category = category,
        subCategory = "",
        date = date,
        account = "Checking",
        exchangeRate = 4.97,
        exchangeRateDate = "2026-03-09",
        conversionStatus = "OFFICIAL",
        exchangeRateSource = "BNR_OFFICIAL"
    )

    // =========================================================================
    // 1. FINANCIAL PULSE CARD TESTS
    // =========================================================================

    @Test
    fun test01_FinancialPulseCardRendersWithValidInsights() {
        val insights = SmartFinancialInsights(
            savingsTrendText = "Strong Capital Growth",
            monthOverMonthExpenseChangePercent = 5.2,
            avgMonthlyExpense = 3200.0,
            avgMonthlyIncome = 7500.0
        )

        composeTestRule.setContent {
            FinTrackTheme {
                FinancialPulseCard(
                    insights = insights,
                    savingsRate = 57.3,
                    expenseVelocity = 42.7
                )
            }
        }

        composeTestRule.onNodeWithTag("financial_pulse_card").assertIsDisplayed()
        composeTestRule.onNodeWithText("Financial Pulse").assertIsDisplayed()
        composeTestRule.onNodeWithTag("financial_pulse_narrative").assertIsDisplayed()
        composeTestRule.onNodeWithTag("financial_pulse_savings_ratio").assertIsDisplayed()
        composeTestRule.onNodeWithTag("financial_pulse_expense_velocity").assertIsDisplayed()
        composeTestRule.onNodeWithTag("pulse_savings_progress").assertIsDisplayed()
        composeTestRule.onNodeWithTag("pulse_velocity_progress").assertIsDisplayed()
    }

    @Test
    fun test02_FinancialPulseCardGracefulDegradedStateOnEmptyInsights() {
        val emptyInsights = SmartFinancialInsights()

        composeTestRule.setContent {
            FinTrackTheme {
                FinancialPulseCard(insights = emptyInsights)
            }
        }

        composeTestRule.onNodeWithTag("financial_pulse_card").assertIsDisplayed()
        composeTestRule.onNodeWithTag("financial_pulse_empty_state").assertIsDisplayed()
        composeTestRule.onNodeWithText("Financial pulse will calibrate as activity accumulates.").assertIsDisplayed()
    }

    @Test
    fun test03_FinancialPulseCardEditorialProseConstructedAccurately() {
        val insights = SmartFinancialInsights(
            savingsTrendText = "Positive Savings Rate",
            monthOverMonthExpenseChangePercent = -12.4
        )

        composeTestRule.setContent {
            FinTrackTheme {
                FinancialPulseCard(
                    insights = insights,
                    savingsRate = 28.5,
                    expenseVelocity = 71.5
                )
            }
        }

        composeTestRule.onNodeWithTag("financial_pulse_card").assertIsDisplayed()
        composeTestRule.onNodeWithTag("financial_pulse_narrative").assertIsDisplayed()
        composeTestRule.onNodeWithText("28.5%").assertIsDisplayed()
        composeTestRule.onNodeWithText("+71.5%").assertIsDisplayed()
    }

    // =========================================================================
    // 2. RECENT ACTIVITY SECTION TESTS
    // =========================================================================

    @Test
    fun test04_RecentActivitySectionRendersQuietEmptyStateWhenZeroTransactions() {
        var viewAllClicked = false

        composeTestRule.setContent {
            FinTrackTheme {
                RecentActivitySection(
                    transactions = emptyList(),
                    selectedCurrency = "RON",
                    onViewAllClicked = { viewAllClicked = true }
                )
            }
        }

        composeTestRule.onNodeWithTag("recent_activity_section").assertIsDisplayed()
        composeTestRule.onNodeWithText("Recent Activity").assertIsDisplayed()
        composeTestRule.onNodeWithTag("recent_activity_empty_state").assertIsDisplayed()
        composeTestRule.onNodeWithText("No Recent Activity").assertIsDisplayed()

        // "View All Activity" button is still accessible
        composeTestRule.onNodeWithTag("view_all_activity_button").performClick()
        assertTrue(viewAllClicked)
    }

    @Test
    fun test05_RecentActivitySectionRendersSingleTransactionCleanly() {
        val tx = createSampleTransaction(
            id = "tx1",
            desc = "Organic Groceries",
            amountRON = 120.50,
            amountEUR = 24.25,
            category = "Food & Dining",
            type = "Expense"
        )

        composeTestRule.setContent {
            FinTrackTheme {
                RecentActivitySection(
                    transactions = listOf(tx),
                    selectedCurrency = "RON",
                    onViewAllClicked = {}
                )
            }
        }

        composeTestRule.onNodeWithTag("recent_activity_section").assertIsDisplayed()
        composeTestRule.onNodeWithTag("recent_tx_item_tx1").assertIsDisplayed()
        composeTestRule.onNodeWithTag("recent_tx_desc_tx1", useUnmergedTree = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("Organic Groceries").assertIsDisplayed()
        composeTestRule.onNodeWithText("-120.50 RON").assertIsDisplayed()
    }

    @Test
    fun test06_RecentActivitySectionRendersTwoTransactionsCleanly() {
        val tx1 = createSampleTransaction("tx1", "Gym Membership", 150.0, 30.0, "Health", "Expense")
        val tx2 = createSampleTransaction("tx2", "Client Payment", 3500.0, 704.0, "Salary", "Income")

        composeTestRule.setContent {
            FinTrackTheme {
                RecentActivitySection(
                    transactions = listOf(tx1, tx2),
                    selectedCurrency = "RON",
                    onViewAllClicked = {}
                )
            }
        }

        composeTestRule.onNodeWithTag("recent_tx_item_tx1").assertIsDisplayed()
        composeTestRule.onNodeWithTag("recent_tx_item_tx2").assertIsDisplayed()
        composeTestRule.onNodeWithText("-150.00 RON").assertIsDisplayed()
        composeTestRule.onNodeWithText("+3 500.00 RON").assertIsDisplayed()
    }

    @Test
    fun test07_RecentActivitySectionConstrainsToThreeTransactionsWhenMoreExist() {
        val txs = listOf(
            createSampleTransaction("tx1", "Coffee", 15.0, 3.0, "Food & Dining", "Expense"),
            createSampleTransaction("tx2", "Gasoline", 200.0, 40.0, "Transportation", "Expense"),
            createSampleTransaction("tx3", "Freelance Work", 1200.0, 241.0, "Income", "Income"),
            createSampleTransaction("tx4", "Pharmacy", 45.0, 9.0, "Health", "Expense"),
            createSampleTransaction("tx5", "Books", 80.0, 16.0, "Entertainment", "Expense")
        )

        composeTestRule.setContent {
            FinTrackTheme {
                RecentActivitySection(
                    transactions = txs,
                    selectedCurrency = "RON",
                    onViewAllClicked = {}
                )
            }
        }

        composeTestRule.onNodeWithTag("recent_tx_item_tx1").assertIsDisplayed()
        composeTestRule.onNodeWithTag("recent_tx_item_tx2").assertIsDisplayed()
        composeTestRule.onNodeWithTag("recent_tx_item_tx3").assertIsDisplayed()
        // 4th and 5th items should not be present in preview
        composeTestRule.onNodeWithTag("recent_tx_item_tx4").assertDoesNotExist()
        composeTestRule.onNodeWithTag("recent_tx_item_tx5").assertDoesNotExist()
    }

    @Test
    fun test08_RecentActivitySectionTransactionClickTriggersCallback() {
        val tx = createSampleTransaction("tx1", "Dinner", 85.0, 17.0, "Food & Dining", "Expense")
        var clickedTx: TransactionEntity? = null

        composeTestRule.setContent {
            FinTrackTheme {
                RecentActivitySection(
                    transactions = listOf(tx),
                    selectedCurrency = "RON",
                    onViewAllClicked = {},
                    onTransactionClicked = { clickedTx = it }
                )
            }
        }

        composeTestRule.onNodeWithTag("recent_tx_item_tx1").performClick()
        assertEquals("tx1", clickedTx?.id)
        assertEquals("Dinner", clickedTx?.description)
    }

    @Test
    fun test09_RecentActivitySectionCurrencyParityInEUR() {
        val tx = createSampleTransaction(
            id = "tx1",
            desc = "Euro Software License",
            amountRON = 497.0,
            amountEUR = 100.0,
            category = "Services",
            type = "Expense"
        )

        composeTestRule.setContent {
            FinTrackTheme {
                RecentActivitySection(
                    transactions = listOf(tx),
                    selectedCurrency = "EUR",
                    onViewAllClicked = {}
                )
            }
        }

        composeTestRule.onNodeWithTag("recent_tx_item_tx1").assertIsDisplayed()
        composeTestRule.onNodeWithText("-100.00 EUR").assertIsDisplayed()
    }

    // =========================================================================
    // 3. DASHBOARD SCREEN INTEGRATION TESTS
    // =========================================================================

    @Test
    fun test10_DashboardScreenIntegratesBothPulseCardAndRecentActivitySection() {
        val metrics = DashboardMetrics(
            balance = 45000.0,
            currency = "RON",
            totalIncome = 60000.0,
            totalExpense = 15000.0,
            savingsRate = 75.0,
            expensePressure = 25.0
        )
        val insights = SmartFinancialInsights(
            savingsTrendText = "Strong Capital Growth",
            avgMonthlyExpense = 15000.0,
            avgMonthlyIncome = 60000.0
        )
        val recentTxs = listOf(
            createSampleTransaction("tx1", "Salary Advance", 5000.0, 1006.0, "Salary", "Income")
        )

        var viewAllInvoked = false

        composeTestRule.setContent {
            FinTrackTheme {
                DashboardScreen(
                    metrics = metrics,
                    filterSettings = FilterSettings(selectedCurrency = "RON", selectedPeriod = "This Month"),
                    monthlyDataPoints = emptyList(),
                    categoryShares = emptyList(),
                    smartInsights = insights,
                    recentTransactions = recentTxs,
                    onPeriodSelected = {},
                    onCurrencyChanged = {},
                    onViewAllActivity = { viewAllInvoked = true }
                )
            }
        }

        // Structural invariants verified
        composeTestRule.onNodeWithTag("unified_hero_canvas").assertIsDisplayed()
        composeTestRule.onNodeWithTag("period_selector_dropdown").assertIsDisplayed()
        composeTestRule.onNodeWithTag("financial_pulse_card").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("recent_activity_section").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("recent_tx_item_tx1").performScrollTo().assertIsDisplayed()

        // Test interaction
        composeTestRule.onNodeWithTag("view_all_activity_button").performScrollTo().performClick()
        assertTrue(viewAllInvoked)
    }

    @Test
    fun test11_DashboardScreenEmptyScenarioRendersCleanlyWithoutCrashing() {
        composeTestRule.setContent {
            FinTrackTheme {
                DashboardScreen(
                    metrics = DashboardMetrics(),
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
        composeTestRule.onNodeWithTag("financial_pulse_card").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("financial_pulse_empty_state").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("recent_activity_section").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("recent_activity_empty_state").performScrollTo().assertIsDisplayed()
    }
}
