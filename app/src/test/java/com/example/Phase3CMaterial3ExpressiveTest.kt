package com.example

import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.domain.analytics.DashboardMetrics
import com.example.domain.analytics.SmartFinancialInsights
import com.example.ui.components.FinancialPulseCard
import com.example.ui.components.UnifiedHeroCanvas
import com.example.ui.components.formatHeroCashflowDigits
import com.example.ui.components.formatHeroNetBalanceDigits
import com.example.ui.theme.DarkFinTrackColors
import com.example.ui.theme.FinTrackTheme
import com.example.ui.theme.LightFinTrackColors
import com.example.ui.theme.SurfaceHeroDarkMidnight
import com.example.ui.theme.SurfaceHeroLight
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class Phase3CMaterial3ExpressiveTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun test01_HeroCanvasLightModeAdaptiveSurfaceTokens() {
        // Light Mode surface must be crisp, airy and luminous (not hardcoded dark midnight)
        assertEquals(SurfaceHeroLight, LightFinTrackColors.surfaceHero)
        assertNotEquals(SurfaceHeroDarkMidnight, LightFinTrackColors.surfaceHero)
        assertTrue("Light hero surface must have high luminance", LightFinTrackColors.surfaceHero.luminance() > 0.9f)

        // Dark Mode surface must maintain high contrast midnight surface
        assertEquals(SurfaceHeroDarkMidnight, DarkFinTrackColors.surfaceHero)
        assertTrue("Dark hero surface must have low luminance", DarkFinTrackColors.surfaceHero.luminance() < 0.1f)

        // Light mode text contrast verification (WCAG AA: text primary on surface hero)
        val lightSurfaceLum = LightFinTrackColors.surfaceHero.luminance()
        val lightTextLum = LightFinTrackColors.textPrimary.luminance()
        val contrastRatio = (lightSurfaceLum + 0.05f) / (lightTextLum + 0.05f)
        assertTrue("Light theme hero text contrast must satisfy WCAG AA (>4.5:1)", contrastRatio >= 4.5f)
    }

    @Test
    fun test02_HeroCanvasRendersInLightModeWithAdaptiveStyling() {
        val sampleMetrics = DashboardMetrics(
            balance = 12500.0,
            currency = "RON",
            secondaryCurrency = "EUR",
            secondaryCurrencyBalance = 2515.0,
            totalIncome = 20000.0,
            totalExpense = 7500.0,
            periodLabel = "This Month",
            latestBnrRate = 4.97,
            effectiveBnrDate = "2026-03-09"
        )

        composeTestRule.setContent {
            FinTrackTheme(darkTheme = false) {
                UnifiedHeroCanvas(metrics = sampleMetrics)
            }
        }

        composeTestRule.onNodeWithTag("dashboard_top_card").assertIsDisplayed()
        composeTestRule.onNodeWithTag("hero_primary_balance").assertIsDisplayed()
        composeTestRule.onNodeWithText("12 500 RON").assertIsDisplayed()
        composeTestRule.onNodeWithText("+20 000 RON").assertIsDisplayed()
        composeTestRule.onNodeWithText("-7 500 RON").assertIsDisplayed()
        composeTestRule.onNodeWithTag("hero_bnr_parity").assertIsDisplayed()
    }

    @Test
    fun test03_HeroCanvasDecoupledFormattersForRonAndEur() {
        val ronBalance = 85200.0
        val eurBalance = 17142.85

        val formattedRon = formatHeroNetBalanceDigits(ronBalance)
        val formattedEur = formatHeroNetBalanceDigits(eurBalance)
        val formattedCashflow = formatHeroCashflowDigits(12345.0)

        assertEquals("85 200", formattedRon)
        assertEquals("17 143", formattedEur)
        assertEquals("12 345", formattedCashflow)
    }

    @Test
    fun test04_FinancialPulseMetricInfoDialogAffordances() {
        val sampleInsights = SmartFinancialInsights(
            savingsTrendText = "Positive Savings Rate",
            monthOverMonthExpenseChangePercent = 3.5,
            avgMonthlyExpense = 4000.0,
            avgMonthlyIncome = 9000.0
        )

        composeTestRule.setContent {
            FinTrackTheme {
                FinancialPulseCard(
                    insights = sampleInsights,
                    savingsRate = 55.5,
                    expenseVelocity = 44.5,
                    periodLabel = "This Month"
                )
            }
        }

        // 1. Check Savings Ratio Info Affordance
        composeTestRule.onNodeWithTag("pulse_savings_ratio_info").assertIsDisplayed()
        composeTestRule.onNodeWithTag("pulse_savings_ratio_info").performClick()

        // Dialog should appear with title and formula
        composeTestRule.onNodeWithTag("pulse_metric_dialog").assertIsDisplayed()
        composeTestRule.onNodeWithText("Formula: (Net Surplus ÷ Total Income) × 100").assertIsDisplayed()

        // Dismiss dialog
        composeTestRule.onNodeWithTag("pulse_info_dialog_dismiss").performClick()
        composeTestRule.onNodeWithTag("pulse_metric_dialog").assertDoesNotExist()

        // 2. Check Expense Velocity Info Affordance
        composeTestRule.onNodeWithTag("pulse_expense_velocity_info").assertIsDisplayed()
        composeTestRule.onNodeWithTag("pulse_expense_velocity_info").performClick()

        composeTestRule.onNodeWithTag("pulse_metric_dialog").assertIsDisplayed()
        composeTestRule.onNodeWithText("Formula: (Total Expenses ÷ Total Income) × 100").assertIsDisplayed()

        // Dismiss dialog
        composeTestRule.onNodeWithTag("pulse_info_dialog_dismiss").performClick()
        composeTestRule.onNodeWithTag("pulse_metric_dialog").assertDoesNotExist()
    }

    @Test
    fun test05_FinancialPulsePeriodAwareNarrativeMultiMonth() {
        val multiMonthInsights = SmartFinancialInsights(
            savingsTrendText = "Strong Capital Growth",
            monthOverMonthExpenseChangePercent = 8.5 // Should NOT be cited as 'vs previous month' for Year to Date
        )

        composeTestRule.setContent {
            FinTrackTheme {
                FinancialPulseCard(
                    insights = multiMonthInsights,
                    savingsRate = 60.0,
                    expenseVelocity = 40.0,
                    periodLabel = "Year to Date"
                )
            }
        }

        composeTestRule.onNodeWithTag("financial_pulse_narrative").assertIsDisplayed()
        // Narrative must not assert 'vs previous month' when period is multi-month (Year to Date)
        composeTestRule.onNodeWithText("vs previous month", substring = true).assertDoesNotExist()
        // Truthful narrative for selected multi-month period
        composeTestRule.onNodeWithText("Expense trend is available for the selected period.", substring = true).assertIsDisplayed()
    }

    @Test
    fun test06_FinancialPulseCardRemovesRedundantStatusBadge() {
        val sampleInsights = SmartFinancialInsights(
            savingsTrendText = "Strong Capital Growth",
            avgMonthlyExpense = 3000.0,
            avgMonthlyIncome = 8000.0
        )

        composeTestRule.setContent {
            FinTrackTheme {
                FinancialPulseCard(
                    insights = sampleInsights,
                    savingsRate = 62.5,
                    expenseVelocity = 37.5,
                    periodLabel = "This Month"
                )
            }
        }

        // Header must be clean: "Financial Pulse" is present, but "Strong Capital Growth" badge is absent
        composeTestRule.onNodeWithText("Financial Pulse").assertIsDisplayed()
        composeTestRule.onNodeWithTag("status_badge_success").assertDoesNotExist()
    }
}
