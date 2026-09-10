package com.example

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.example.data.model.FilterSettings
import com.example.domain.analytics.DashboardMetrics
import com.example.domain.analytics.SmartFinancialInsights
import com.example.ui.components.FinancialPulseCard
import com.example.ui.components.UnifiedHeroCanvas
import com.example.ui.screens.DashboardScreen
import com.example.ui.theme.FinTrackTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class M3HeroExpressiveMigrationTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testUnifiedHeroCanvasExpressiveRendering() {
        val metrics = DashboardMetrics(
            balance = 48500.0,
            currency = "RON",
            secondaryCurrency = "EUR",
            secondaryCurrencyBalance = 9750.0,
            latestBnrRate = 4.9745,
            effectiveBnrDate = "2026-03-09",
            totalIncome = 65000.0,
            totalExpense = 16500.0,
            periodLabel = "March 2026"
        )

        composeTestRule.setContent {
            FinTrackTheme {
                UnifiedHeroCanvas(metrics = metrics)
            }
        }

        // Verify top-level surface tags
        composeTestRule.onNodeWithTag("dashboard_top_card").assertIsDisplayed()
        composeTestRule.onNodeWithTag("unified_hero_canvas").assertIsDisplayed()

        // Verify Header and period badge
        composeTestRule.onNodeWithText("NET BALANCE").assertIsDisplayed()
        composeTestRule.onNodeWithTag("hero_period_badge").assertIsDisplayed()
        composeTestRule.onNodeWithText("March 2026").assertIsDisplayed()

        // Verify Primary net balance
        composeTestRule.onNodeWithTag("hero_primary_balance").assertIsDisplayed()
        composeTestRule.onNodeWithText("48 500 RON").assertIsDisplayed()

        // Verify Secondary currency equivalence
        composeTestRule.onNodeWithTag("hero_secondary_currency").assertIsDisplayed()
        composeTestRule.onNodeWithText("≈ 9 750 EUR").assertIsDisplayed()

        // Verify Narrative
        composeTestRule.onNodeWithTag("hero_wealth_narrative").assertIsDisplayed()
        composeTestRule.onNodeWithText("Net surplus this period").assertIsDisplayed()

        // Verify BNR parity
        composeTestRule.onNodeWithTag("hero_bnr_parity").assertIsDisplayed()

        // Verify Cashflow row
        composeTestRule.onNodeWithTag("hero_cashflow_row").assertIsDisplayed()
        composeTestRule.onNodeWithText("Income").assertIsDisplayed()
        composeTestRule.onNodeWithText("Expense").assertIsDisplayed()
        composeTestRule.onNodeWithText("+65 000 RON").assertIsDisplayed()
        composeTestRule.onNodeWithText("-16 500 RON").assertIsDisplayed()
    }

    @Test
    fun testUnifiedHeroCanvasDeficitAndDegradedState() {
        val metrics = DashboardMetrics(
            balance = -3200.0,
            currency = "RON",
            secondaryCurrency = "EUR",
            secondaryCurrencyBalance = null,
            latestBnrRate = null,
            totalIncome = 5000.0,
            totalExpense = 8200.0,
            periodLabel = ""
        )

        composeTestRule.setContent {
            FinTrackTheme {
                UnifiedHeroCanvas(metrics = metrics)
            }
        }

        composeTestRule.onNodeWithTag("hero_primary_balance").assertIsDisplayed()
        composeTestRule.onNodeWithText("-3 200 RON").assertIsDisplayed()
        composeTestRule.onNodeWithText("≈ EUR unavailable").assertIsDisplayed()
        composeTestRule.onNodeWithText("Net deficit this period").assertIsDisplayed()
        composeTestRule.onNodeWithText("BNR unavailable").assertIsDisplayed()
    }

    @Test
    fun testFinancialPulseCardExpressiveGroupedPanel() {
        val insights = SmartFinancialInsights(
            savingsTrendText = "Strong Capital Growth",
            monthOverMonthExpenseChangePercent = 4.5,
            avgMonthlyExpense = 3000.0,
            avgMonthlyIncome = 8000.0
        )

        composeTestRule.setContent {
            FinTrackTheme {
                FinancialPulseCard(
                    insights = insights,
                    savingsRate = 62.5,
                    expenseVelocity = 37.5,
                    periodLabel = "This Month"
                )
            }
        }

        // Top level panel
        composeTestRule.onNodeWithTag("financial_pulse_card").assertIsDisplayed()
        composeTestRule.onNodeWithText("Financial Pulse").assertIsDisplayed()
        composeTestRule.onNodeWithTag("financial_pulse_narrative").assertIsDisplayed()

        // Dual metrics
        composeTestRule.onNodeWithTag("financial_pulse_savings_ratio").assertIsDisplayed()
        composeTestRule.onNodeWithText("62.5%").assertIsDisplayed()
        composeTestRule.onNodeWithTag("financial_pulse_expense_velocity").assertIsDisplayed()
        composeTestRule.onNodeWithText("+37.5%").assertIsDisplayed()

        composeTestRule.onNodeWithTag("pulse_savings_progress").assertIsDisplayed()
        composeTestRule.onNodeWithTag("pulse_velocity_progress").assertIsDisplayed()

        // Affordance dialogs
        composeTestRule.onNodeWithTag("pulse_savings_ratio_info").assertIsDisplayed().performClick()
        composeTestRule.onNodeWithTag("pulse_metric_dialog").assertIsDisplayed()
        composeTestRule.onNodeWithTag("pulse_info_dialog_dismiss").assertIsDisplayed().performClick()
    }

    @Test
    fun testFinancialPulseCardDegradedState() {
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
    fun testDashboardScreenExpressiveIntegration() {
        val metrics = DashboardMetrics(
            balance = 10000.0,
            currency = "RON",
            totalIncome = 15000.0,
            totalExpense = 5000.0,
            periodLabel = "This Month"
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

        composeTestRule.onNodeWithTag("unified_hero_canvas").assertIsDisplayed()
        composeTestRule.onNodeWithTag("financial_pulse_card").performScrollTo().assertIsDisplayed()
    }
}
