package com.example

import android.content.Context
import android.provider.Settings
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
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
class M3DashboardExpressiveMigrationTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun createStandardMetrics(
        balance: Double = 45200.0,
        currency: String = "RON",
        totalIncome: Double = 60000.0,
        totalExpense: Double = 14800.0,
        hasIncompleteEur: Boolean = false
    ): DashboardMetrics {
        return DashboardMetrics(
            balance = balance,
            currency = currency,
            secondaryCurrency = if (currency == "RON") "EUR" else "RON",
            secondaryCurrencyBalance = if (currency == "RON") balance / 4.9745 else balance * 4.9745,
            latestBnrRate = 4.9745,
            effectiveBnrDate = "2026-03-09",
            totalIncome = totalIncome,
            totalExpense = totalExpense,
            savingsRate = if (totalIncome > 0) (totalIncome - totalExpense) / totalIncome else 0.0,
            expensePressure = if (totalIncome > 0) totalExpense / totalIncome else 0.0,
            periodLabel = "March 2026",
            hasIncompleteEurData = hasIncompleteEur,
            excludedNonOfficialCount = if (hasIncompleteEur) 3 else 0
        )
    }

    private fun createSampleTransaction(
        id: String,
        desc: String,
        amountRON: Double,
        amountEUR: Double = amountRON / 4.9745,
        category: String = "Food & Dining",
        type: String = "Expense",
        date: String = "2026-03-09"
    ): TransactionEntity {
        return TransactionEntity(
            id = id,
            householdId = "h_test_family",
            description = desc,
            amountRON = amountRON,
            amountEUR = amountEUR,
            type = type,
            category = category,
            subCategory = "",
            date = date,
            account = "Checking",
            exchangeRate = 4.9745,
            exchangeRateDate = "2026-03-09",
            conversionStatus = "OFFICIAL",
            exchangeRateSource = "BNR_OFFICIAL"
        )
    }

    // =========================================================================
    // 1. DASHBOARD ROOT & STRUCTURAL HIERARCHY
    // =========================================================================

    @Test
    fun test01_DashboardRootAndHierarchyDisplayed() {
        val metrics = createStandardMetrics()
        val dataPoints = listOf(
            MonthlyDataPoint("2026-01", 10000.0, 4000.0, 6000.0),
            MonthlyDataPoint("2026-02", 12000.0, 5000.0, 7000.0)
        )
        val shares = listOf(
            CategoryExpenseShare(categoryName = "Food & Dining", totalAmount = 3500.0, percentage = 45.0, transactionCount = 5),
            CategoryExpenseShare(categoryName = "Utilities", totalAmount = 2500.0, percentage = 35.0, transactionCount = 2)
        )
        val recentTxs = listOf(
            createSampleTransaction("tx1", "Supermarket", 350.0),
            createSampleTransaction("tx2", "Monthly Salary", 12000.0, category = "Salary", type = "Income")
        )

        composeTestRule.setContent {
            FinTrackTheme {
                DashboardScreen(
                    metrics = metrics,
                    filterSettings = FilterSettings(selectedCurrency = "RON", selectedPeriod = "This Month"),
                    monthlyDataPoints = dataPoints,
                    categoryShares = shares,
                    smartInsights = SmartFinancialInsights(savingsTrendText = "Healthy finances"),
                    recentTransactions = recentTxs,
                    onPeriodSelected = {},
                    onCurrencyChanged = {}
                )
            }
        }

        // 1. Dashboard Root & Header
        composeTestRule.onNodeWithTag("dashboard_screen_root").assertIsDisplayed()
        composeTestRule.onNodeWithText("Dashboard").assertIsDisplayed()
        composeTestRule.onNodeWithTag("currency_toggle_RON").assertIsDisplayed()
        composeTestRule.onNodeWithTag("currency_toggle_EUR").assertIsDisplayed()

        // 2. Net Balance Hero
        composeTestRule.onNodeWithTag("dashboard_top_card").assertIsDisplayed()
        composeTestRule.onNodeWithTag("unified_hero_canvas").assertIsDisplayed()
        composeTestRule.onNodeWithText("NET BALANCE").assertIsDisplayed()
        composeTestRule.onNodeWithTag("hero_period_badge").assertIsDisplayed()

        // 3. Period Selector
        composeTestRule.onNodeWithTag("period_selector_dropdown").assertIsDisplayed()

        // 4. Financial Pulse Panel
        composeTestRule.onNodeWithTag("financial_pulse_card").performScrollTo().assertIsDisplayed()

        // 5. Monthly Cash Flow Tonal Surface
        composeTestRule.onNodeWithTag("dashboard_cash_flow_container").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("cash_flow_spline_canvas").performScrollTo().assertIsDisplayed()

        // 6. Category Breakdown Tonal Surface
        composeTestRule.onNodeWithTag("dashboard_category_distribution_container").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("category_donut_canvas").performScrollTo().assertIsDisplayed()

        // 7. Recent Activity Stream
        composeTestRule.onNodeWithTag("recent_activity_section").performScrollTo().assertIsDisplayed()
    }

    // =========================================================================
    // 2. NET BALANCE HERO & FINANCIAL DISPLAY TYPOGRAPHY
    // =========================================================================

    @Test
    fun test02_NetBalanceHeroDisplayAndFormatting() {
        val metrics = createStandardMetrics(balance = 45200.0)

        composeTestRule.setContent {
            FinTrackTheme {
                DashboardScreen(
                    metrics = metrics,
                    filterSettings = FilterSettings(selectedCurrency = "RON", selectedPeriod = "This Month"),
                    monthlyDataPoints = emptyList(),
                    categoryShares = emptyList(),
                    smartInsights = SmartFinancialInsights(),
                    onPeriodSelected = {},
                    onCurrencyChanged = {}
                )
            }
        }

        composeTestRule.onNodeWithTag("hero_primary_balance").assertIsDisplayed()
        composeTestRule.onNodeWithText("45 200 RON").assertIsDisplayed()
        composeTestRule.onNodeWithTag("hero_secondary_currency").assertIsDisplayed()
        composeTestRule.onNodeWithTag("hero_bnr_parity").assertIsDisplayed()
        composeTestRule.onNodeWithTag("hero_wealth_narrative").assertIsDisplayed()
        composeTestRule.onNodeWithText("Net surplus this period").assertIsDisplayed()
    }

    @Test
    fun test03_NetBalanceHeroDeficitAndZeroIncomeStates() {
        val metrics = createStandardMetrics(
            balance = -4500.0,
            totalIncome = 2000.0,
            totalExpense = 6500.0
        )

        composeTestRule.setContent {
            FinTrackTheme {
                DashboardScreen(
                    metrics = metrics,
                    filterSettings = FilterSettings(selectedCurrency = "RON", selectedPeriod = "This Month"),
                    monthlyDataPoints = emptyList(),
                    categoryShares = emptyList(),
                    smartInsights = SmartFinancialInsights(),
                    onPeriodSelected = {},
                    onCurrencyChanged = {}
                )
            }
        }

        composeTestRule.onNodeWithTag("hero_primary_balance").assertIsDisplayed()
        composeTestRule.onNodeWithText("-4 500 RON").assertIsDisplayed()
        composeTestRule.onNodeWithTag("hero_wealth_narrative").assertIsDisplayed()
        composeTestRule.onNodeWithText("Net deficit this period").assertIsDisplayed()
    }

    // =========================================================================
    // 3. SUPPORTING METRICS (CASHFLOW ROW)
    // =========================================================================

    @Test
    fun test04_SupportingMetricsCashflowRow() {
        val metrics = createStandardMetrics(
            totalIncome = 75000.0,
            totalExpense = 25000.0
        )

        composeTestRule.setContent {
            FinTrackTheme {
                DashboardScreen(
                    metrics = metrics,
                    filterSettings = FilterSettings(selectedCurrency = "RON", selectedPeriod = "This Month"),
                    monthlyDataPoints = emptyList(),
                    categoryShares = emptyList(),
                    smartInsights = SmartFinancialInsights(),
                    onPeriodSelected = {},
                    onCurrencyChanged = {}
                )
            }
        }

        composeTestRule.onNodeWithTag("hero_cashflow_row").assertIsDisplayed()
        composeTestRule.onNodeWithText("Income").assertIsDisplayed()
        composeTestRule.onNodeWithText("+75 000 RON").assertIsDisplayed()
        composeTestRule.onNodeWithText("Expense").assertIsDisplayed()
        composeTestRule.onNodeWithText("-25 000 RON").assertIsDisplayed()
    }

    // =========================================================================
    // 4. TONAL SURFACES & WARNING SURFACE
    // =========================================================================

    @Test
    fun test05_TonalSurfacesAndIncompleteEurWarning() {
        val metricsWithWarning = createStandardMetrics(hasIncompleteEur = true)

        composeTestRule.setContent {
            FinTrackTheme {
                DashboardScreen(
                    metrics = metricsWithWarning,
                    filterSettings = FilterSettings(selectedCurrency = "RON", selectedPeriod = "This Month"),
                    monthlyDataPoints = emptyList(),
                    categoryShares = emptyList(),
                    smartInsights = SmartFinancialInsights(),
                    onPeriodSelected = {},
                    onCurrencyChanged = {}
                )
            }
        }

        // Warning surface must be displayed when hasIncompleteEurData = true
        composeTestRule.onNodeWithTag("eur_incomplete_warning_card").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("EUR totals are incomplete: 3 transaction(s) pending or unverified BNR exchange rate excluded. Complete RON data remains available.").performScrollTo().assertIsDisplayed()
    }

    // =========================================================================
    // 5. RECENT TRANSACTIONS (EMPTY & POPULATED STATES)
    // =========================================================================

    @Test
    fun test06_RecentTransactionsEmptyState() {
        val metrics = createStandardMetrics()

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
                    onCurrencyChanged = {}
                )
            }
        }

        composeTestRule.onNodeWithTag("recent_activity_section").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("recent_activity_empty_state").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("No Recent Activity").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun test07_RecentTransactionsPopulatedAndNavigationCallbacks() {
        var viewAllClicked = false
        var clickedTx: TransactionEntity? = null

        val tx1 = createSampleTransaction("tx1", "Supermarket Market", 240.0)
        val tx2 = createSampleTransaction("tx2", "Pharmacy Plus", 85.0)
        val metrics = createStandardMetrics()

        composeTestRule.setContent {
            FinTrackTheme {
                DashboardScreen(
                    metrics = metrics,
                    filterSettings = FilterSettings(selectedCurrency = "RON", selectedPeriod = "This Month"),
                    monthlyDataPoints = emptyList(),
                    categoryShares = emptyList(),
                    smartInsights = SmartFinancialInsights(),
                    recentTransactions = listOf(tx1, tx2),
                    onViewAllActivity = { viewAllClicked = true },
                    onTransactionClicked = { clickedTx = it },
                    onPeriodSelected = {},
                    onCurrencyChanged = {}
                )
            }
        }

        // Transactions preview items are displayed
        composeTestRule.onNodeWithTag("recent_tx_item_tx1").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("recent_tx_desc_tx1", useUnmergedTree = true).performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("recent_tx_amount_tx1", useUnmergedTree = true).performScrollTo().assertIsDisplayed()

        composeTestRule.onNodeWithTag("recent_tx_item_tx2").performScrollTo().assertIsDisplayed()

        // Click item triggers callback
        composeTestRule.onNodeWithTag("recent_tx_item_tx1").performScrollTo().performClick()
        assertEquals("tx1", clickedTx?.id)

        // Click "View All" triggers callback
        composeTestRule.onNodeWithTag("view_all_activity_button").performScrollTo().performClick()
        assertTrue(viewAllClicked)
    }

    // =========================================================================
    // 6. GLOBAL PERIOD & CURRENCY TOGGLE INTEGRATION
    // =========================================================================

    @Test
    fun test08_PeriodSelectorAndCurrencyToggleInteractions() {
        var selectedPeriodResult = ""
        var selectedCurrencyResult = ""
        val metrics = createStandardMetrics()

        composeTestRule.setContent {
            FinTrackTheme {
                DashboardScreen(
                    metrics = metrics,
                    filterSettings = FilterSettings(selectedCurrency = "RON", selectedPeriod = "This Month"),
                    monthlyDataPoints = emptyList(),
                    categoryShares = emptyList(),
                    smartInsights = SmartFinancialInsights(),
                    onPeriodSelected = { selectedPeriodResult = it },
                    onCurrencyChanged = { selectedCurrencyResult = it }
                )
            }
        }

        // Currency Toggle interaction
        composeTestRule.onNodeWithTag("currency_toggle_EUR").performClick()
        assertEquals("EUR", selectedCurrencyResult)

        // Period Dropdown interaction
        composeTestRule.onNodeWithTag("period_selector_dropdown").performScrollTo().performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("All Time", useUnmergedTree = true).performClick()
        assertEquals("All Time", selectedPeriodResult)
    }

    // =========================================================================
    // 7. LIGHT & DARK THEMES
    // =========================================================================

    @Test
    fun test09a_LightThemeRendering() {
        val metrics = createStandardMetrics()

        composeTestRule.setContent {
            FinTrackTheme(darkTheme = false) {
                DashboardScreen(
                    metrics = metrics,
                    filterSettings = FilterSettings(selectedCurrency = "RON", selectedPeriod = "This Month"),
                    monthlyDataPoints = emptyList(),
                    categoryShares = emptyList(),
                    smartInsights = SmartFinancialInsights(),
                    onPeriodSelected = {},
                    onCurrencyChanged = {}
                )
            }
        }
        composeTestRule.onNodeWithTag("dashboard_screen_root").assertIsDisplayed()
    }

    @Test
    fun test09b_DarkThemeRendering() {
        val metrics = createStandardMetrics()

        composeTestRule.setContent {
            FinTrackTheme(darkTheme = true) {
                DashboardScreen(
                    metrics = metrics,
                    filterSettings = FilterSettings(selectedCurrency = "RON", selectedPeriod = "This Month"),
                    monthlyDataPoints = emptyList(),
                    categoryShares = emptyList(),
                    smartInsights = SmartFinancialInsights(),
                    onPeriodSelected = {},
                    onCurrencyChanged = {}
                )
            }
        }
        composeTestRule.onNodeWithTag("dashboard_screen_root").assertIsDisplayed()
    }

    // =========================================================================
    // 8. RESPONSIVE VIEWPORTS (360dp, 390dp, 412dp, 600dp+)
    // =========================================================================

    @Test
    @Config(qualifiers = "w360dp-h800dp")
    fun test10a_ResponsiveViewport360dp() {
        val metrics = createStandardMetrics()
        composeTestRule.setContent {
            FinTrackTheme {
                DashboardScreen(
                    metrics = metrics,
                    filterSettings = FilterSettings(selectedCurrency = "RON", selectedPeriod = "This Month"),
                    monthlyDataPoints = emptyList(),
                    categoryShares = emptyList(),
                    smartInsights = SmartFinancialInsights(),
                    onPeriodSelected = {},
                    onCurrencyChanged = {}
                )
            }
        }
        composeTestRule.onNodeWithTag("dashboard_screen_root").assertIsDisplayed()
        composeTestRule.onNodeWithTag("unified_hero_canvas").assertIsDisplayed()
        composeTestRule.onNodeWithTag("hero_primary_balance").assertIsDisplayed()
    }

    @Test
    @Config(qualifiers = "w390dp-h844dp")
    fun test10b_ResponsiveViewport390dp() {
        val metrics = createStandardMetrics()
        composeTestRule.setContent {
            FinTrackTheme {
                DashboardScreen(
                    metrics = metrics,
                    filterSettings = FilterSettings(selectedCurrency = "RON", selectedPeriod = "This Month"),
                    monthlyDataPoints = emptyList(),
                    categoryShares = emptyList(),
                    smartInsights = SmartFinancialInsights(),
                    onPeriodSelected = {},
                    onCurrencyChanged = {}
                )
            }
        }
        composeTestRule.onNodeWithTag("dashboard_screen_root").assertIsDisplayed()
        composeTestRule.onNodeWithTag("unified_hero_canvas").assertIsDisplayed()
        composeTestRule.onNodeWithTag("hero_primary_balance").assertIsDisplayed()
    }

    @Test
    @Config(qualifiers = "w412dp-h915dp")
    fun test10c_ResponsiveViewport412dp() {
        val metrics = createStandardMetrics()
        composeTestRule.setContent {
            FinTrackTheme {
                DashboardScreen(
                    metrics = metrics,
                    filterSettings = FilterSettings(selectedCurrency = "RON", selectedPeriod = "This Month"),
                    monthlyDataPoints = emptyList(),
                    categoryShares = emptyList(),
                    smartInsights = SmartFinancialInsights(),
                    onPeriodSelected = {},
                    onCurrencyChanged = {}
                )
            }
        }
        composeTestRule.onNodeWithTag("dashboard_screen_root").assertIsDisplayed()
        composeTestRule.onNodeWithTag("unified_hero_canvas").assertIsDisplayed()
        composeTestRule.onNodeWithTag("hero_primary_balance").assertIsDisplayed()
    }

    @Test
    @Config(qualifiers = "w600dp-h960dp")
    fun test10d_ResponsiveViewport600dpPlus() {
        val metrics = createStandardMetrics()
        composeTestRule.setContent {
            FinTrackTheme {
                DashboardScreen(
                    metrics = metrics,
                    filterSettings = FilterSettings(selectedCurrency = "RON", selectedPeriod = "This Month"),
                    monthlyDataPoints = emptyList(),
                    categoryShares = emptyList(),
                    smartInsights = SmartFinancialInsights(),
                    onPeriodSelected = {},
                    onCurrencyChanged = {}
                )
            }
        }
        composeTestRule.onNodeWithTag("dashboard_screen_root").assertIsDisplayed()
        composeTestRule.onNodeWithTag("unified_hero_canvas").assertIsDisplayed()
        composeTestRule.onNodeWithTag("hero_primary_balance").assertIsDisplayed()
    }

    // =========================================================================
    // 9. ACCESSIBILITY & TOUCH TARGETS (>= 48dp)
    // =========================================================================

    @Test
    fun test11_TouchTargetsAccessibilityStandards() {
        val metrics = createStandardMetrics()
        val tx = createSampleTransaction("tx1", "Grocery", 120.0)

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
                    onCurrencyChanged = {}
                )
            }
        }

        // Currency toggle tabs: min 48dp
        composeTestRule.onNodeWithTag("currency_toggle_RON")
            .assertWidthIsAtLeast(48.dp)
            .assertHeightIsAtLeast(48.dp)
        composeTestRule.onNodeWithTag("currency_toggle_EUR")
            .assertWidthIsAtLeast(48.dp)
            .assertHeightIsAtLeast(48.dp)

        // Period dropdown: min 48dp height
        composeTestRule.onNodeWithTag("period_selector_dropdown")
            .performScrollTo()
            .assertHeightIsAtLeast(48.dp)

        // View All button: min 48dp height
        composeTestRule.onNodeWithTag("view_all_activity_button")
            .performScrollTo()
            .assertHeightIsAtLeast(48.dp)

        // Transaction list item: min 48dp height
        composeTestRule.onNodeWithTag("recent_tx_item_tx1")
            .performScrollTo()
            .assertHeightIsAtLeast(48.dp)
    }

    // =========================================================================
    // 10. REDUCED MOTION BEHAVIOR
    // =========================================================================

    @Test
    fun test12_ReducedMotionAccessibility() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        Settings.Global.putFloat(context.contentResolver, Settings.Global.TRANSITION_ANIMATION_SCALE, 0f)
        Settings.Global.putFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 0f)

        try {
            val metrics = createStandardMetrics()
            val tx = createSampleTransaction("tx1", "Reduced Motion Coffee", 15.0)

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
                        onCurrencyChanged = {}
                    )
                }
            }

            // Screen renders cleanly with reduced-motion mode active
            composeTestRule.onNodeWithTag("dashboard_screen_root").assertIsDisplayed()
            composeTestRule.onNodeWithTag("hero_primary_balance").assertIsDisplayed()
            composeTestRule.onNodeWithTag("recent_tx_item_tx1").performScrollTo().assertIsDisplayed()
        } finally {
            // Restore default scales
            Settings.Global.putFloat(context.contentResolver, Settings.Global.TRANSITION_ANIMATION_SCALE, 1f)
            Settings.Global.putFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f)
        }
    }

    // =========================================================================
    // 11. ARCHITECTURAL INVARIANTS: ZERO FINANCIAL CALCULATION DUPLICATION
    // =========================================================================

    @Test
    fun test13_ZeroFinancialCalculationDuplicationInvariant() {
        // Confirms UI displays authoritative metrics values verbatim
        val authoritativeMetrics = DashboardMetrics(
            balance = 99999.0,
            currency = "RON",
            secondaryCurrency = "EUR",
            secondaryCurrencyBalance = 20102.32,
            latestBnrRate = 4.9745,
            effectiveBnrDate = "2026-03-09",
            totalIncome = 120000.0,
            totalExpense = 20001.0,
            periodLabel = "Custom Year"
        )

        composeTestRule.setContent {
            FinTrackTheme {
                DashboardScreen(
                    metrics = authoritativeMetrics,
                    filterSettings = FilterSettings(selectedCurrency = "RON", selectedPeriod = "Custom Year"),
                    monthlyDataPoints = emptyList(),
                    categoryShares = emptyList(),
                    smartInsights = SmartFinancialInsights(),
                    onPeriodSelected = {},
                    onCurrencyChanged = {}
                )
            }
        }

        // The UI must reflect authoritative values directly
        composeTestRule.onNodeWithText("99 999 RON").assertIsDisplayed()
        composeTestRule.onNodeWithTag("hero_period_badge").assertIsDisplayed()
        composeTestRule.onNodeWithText("+120 000 RON").assertIsDisplayed()
        composeTestRule.onNodeWithText("-20 001 RON").assertIsDisplayed()
    }
}
