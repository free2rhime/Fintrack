package com.example

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import com.example.data.model.FilterSettings
import com.example.domain.analytics.DashboardMetrics
import com.example.domain.analytics.SmartFinancialInsights
import com.example.ui.navigation.FinTrackBottomNavigation
import com.example.ui.screens.DashboardScreen
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class Checkpoint1P0CorrectnessTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    // -------------------------------------------------------------
    // P0.3: Bottom Navigation Truncation Verification
    // -------------------------------------------------------------

    @Test
    @Config(qualifiers = "w360dp-h800dp")
    fun testBottomNavLabelsDisplayedOnCompact360dp() {
        composeTestRule.setContent {
            FinTrackBottomNavigation(
                selectedTabIndex = 1, // "Transactions" is active
                onTabSelected = {}
            )
        }

        composeTestRule.onNodeWithTag("bottom_navigation_bar").assertIsDisplayed()
        composeTestRule.onNodeWithText("Dashboard").assertIsDisplayed()
        composeTestRule.onNodeWithText("Transactions").assertIsDisplayed()
        composeTestRule.onNodeWithText("Analytics").assertIsDisplayed()
        composeTestRule.onNodeWithText("Categories").assertIsDisplayed()
        composeTestRule.onNodeWithText("Settings").assertIsDisplayed()
    }

    @Test
    @Config(qualifiers = "w390dp-h844dp")
    fun testBottomNavLabelsDisplayedOnMedium390dp() {
        composeTestRule.setContent {
            FinTrackBottomNavigation(
                selectedTabIndex = 0,
                onTabSelected = {}
            )
        }

        composeTestRule.onNodeWithTag("bottom_navigation_bar").assertIsDisplayed()
        composeTestRule.onNodeWithText("Transactions").assertIsDisplayed()
    }

    @Test
    @Config(qualifiers = "w412dp-h915dp")
    fun testBottomNavLabelsDisplayedOnExpanded412dp() {
        composeTestRule.setContent {
            FinTrackBottomNavigation(
                selectedTabIndex = 1,
                onTabSelected = {}
            )
        }

        composeTestRule.onNodeWithTag("bottom_navigation_bar").assertIsDisplayed()
        composeTestRule.onNodeWithText("Transactions").assertIsDisplayed()
    }

    // -------------------------------------------------------------
    // P0.2: Hero Amount Truncation & Responsiveness Verification
    // -------------------------------------------------------------

    @Test
    @Config(qualifiers = "w360dp-h800dp")
    fun testHeroBalanceCardSixDigitValuesAt360dp() {
        val metrics = DashboardMetrics(
            balance = 131556.0,
            totalIncome = 131556.0,
            totalExpense = 86645.0,
            currency = "RON",
            savingsRate = 34.1,
            expensePressure = 65.9
        )

        composeTestRule.setContent {
            DashboardScreen(
                metrics = metrics,
                filterSettings = FilterSettings(selectedPeriod = "This Month", selectedCurrency = "RON"),
                monthlyDataPoints = emptyList(),
                categoryShares = emptyList(),
                smartInsights = SmartFinancialInsights(),
                onPeriodSelected = {},
                onCurrencyChanged = {}
            )
        }

        composeTestRule.onNodeWithTag("dashboard_top_card").assertIsDisplayed()
        composeTestRule.onNodeWithText("NET BALANCE").assertIsDisplayed()
        composeTestRule.onNodeWithText("Income").assertIsDisplayed()
        composeTestRule.onNodeWithText("Expense").assertIsDisplayed()
        composeTestRule.onNodeWithText("131 556 RON").assertIsDisplayed()
        composeTestRule.onNodeWithText("+131 556 RON").assertIsDisplayed()
        composeTestRule.onNodeWithText("-86 645 RON").assertIsDisplayed()
    }

    @Test
    @Config(qualifiers = "w360dp-h800dp")
    fun testHeroBalanceCardSevenDigitAndNegativeValuesAt360dp() {
        val metrics = DashboardMetrics(
            balance = -1250000.0,
            totalIncome = 2500000.0,
            totalExpense = 3750000.0,
            currency = "EUR",
            savingsRate = -50.0,
            expensePressure = 150.0
        )

        composeTestRule.setContent {
            DashboardScreen(
                metrics = metrics,
                filterSettings = FilterSettings(selectedPeriod = "All Time", selectedCurrency = "EUR"),
                monthlyDataPoints = emptyList(),
                categoryShares = emptyList(),
                smartInsights = SmartFinancialInsights(),
                onPeriodSelected = {},
                onCurrencyChanged = {}
            )
        }

        composeTestRule.onNodeWithTag("dashboard_top_card").assertIsDisplayed()
        composeTestRule.onNodeWithText("NET BALANCE").assertIsDisplayed()
        composeTestRule.onNodeWithText("-1 250 000 EUR").assertIsDisplayed()
        composeTestRule.onNodeWithText("+2 500 000 EUR").assertIsDisplayed()
        composeTestRule.onNodeWithText("-3 750 000 EUR").assertIsDisplayed()
    }

    @Test
    @Config(qualifiers = "w412dp-h915dp")
    fun testHeroBalanceCardLargeValuesAt412dp() {
        val metrics = DashboardMetrics(
            balance = 5432100.0,
            totalIncome = 6000000.0,
            totalExpense = 567900.0,
            currency = "RON",
            savingsRate = 90.5,
            expensePressure = 9.5
        )

        composeTestRule.setContent {
            DashboardScreen(
                metrics = metrics,
                filterSettings = FilterSettings(selectedPeriod = "This Year", selectedCurrency = "RON"),
                monthlyDataPoints = emptyList(),
                categoryShares = emptyList(),
                smartInsights = SmartFinancialInsights(),
                onPeriodSelected = {},
                onCurrencyChanged = {}
            )
        }

        composeTestRule.onNodeWithTag("dashboard_top_card").assertIsDisplayed()
        composeTestRule.onNodeWithText("5 432 100 RON").assertIsDisplayed()
        composeTestRule.onNodeWithText("+6 000 000 RON").assertIsDisplayed()
        composeTestRule.onNodeWithText("-567 900 RON").assertIsDisplayed()
    }
}
