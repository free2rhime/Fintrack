package com.example

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.data.model.FilterSettings
import com.example.domain.analytics.DashboardMetrics
import com.example.domain.analytics.SmartFinancialInsights
import com.example.ui.components.UnifiedHeroCanvas
import com.example.ui.screens.DashboardScreen
import com.example.ui.theme.FinTrackTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class Checkpoint41UnifiedHeroCanvasTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    // 1 & 2: Primary balance and currency render from metrics.balance and metrics.currency
    @Test
    fun testPrimaryBalanceAndCurrencyRender() {
        val metrics = DashboardMetrics(
            balance = 85200.0,
            currency = "RON",
            totalIncome = 120000.0,
            totalExpense = 34800.0,
            periodLabel = "This Month"
        )

        composeTestRule.setContent {
            FinTrackTheme {
                UnifiedHeroCanvas(metrics = metrics)
            }
        }

        composeTestRule.onNodeWithTag("unified_hero_canvas").assertIsDisplayed()
        composeTestRule.onNodeWithTag("dashboard_top_card").assertIsDisplayed()
        composeTestRule.onNodeWithText("NET BALANCE").assertIsDisplayed()
        composeTestRule.onNodeWithText("85 200 RON").assertIsDisplayed()
    }

    // 3: Secondary currency renders when available
    @Test
    fun testSecondaryCurrencyRendersWhenAvailable() {
        val metrics = DashboardMetrics(
            balance = 131556.0,
            currency = "RON",
            secondaryCurrency = "EUR",
            secondaryCurrencyBalance = 25900.0,
            totalIncome = 131556.0,
            totalExpense = 0.0
        )

        composeTestRule.setContent {
            FinTrackTheme {
                UnifiedHeroCanvas(metrics = metrics)
            }
        }

        composeTestRule.onNodeWithTag("hero_secondary_currency").assertIsDisplayed()
        composeTestRule.onNodeWithText("≈ 25 900 EUR").assertIsDisplayed()
    }

    // 4: Secondary currency handles null gracefully without fabricating values
    @Test
    fun testSecondaryCurrencyAbsentGracefullyWhenNull() {
        val metrics = DashboardMetrics(
            balance = 10000.0,
            currency = "RON",
            secondaryCurrency = "EUR",
            secondaryCurrencyBalance = null,
            totalIncome = 10000.0,
            totalExpense = 0.0
        )

        composeTestRule.setContent {
            FinTrackTheme {
                UnifiedHeroCanvas(metrics = metrics)
            }
        }

        composeTestRule.onNodeWithTag("hero_secondary_currency").assertIsDisplayed()
        composeTestRule.onNodeWithText("≈ EUR unavailable").assertIsDisplayed()
    }

    // 5: BNR parity renders when latestBnrRate exists
    @Test
    fun testBnrParityRendersWhenAvailable() {
        val metrics = DashboardMetrics(
            balance = 50000.0,
            currency = "RON",
            latestBnrRate = 4.9765,
            effectiveBnrDate = "2026-03-09",
            bnrStatus = "OFFICIAL"
        )

        composeTestRule.setContent {
            FinTrackTheme {
                UnifiedHeroCanvas(metrics = metrics)
            }
        }

        composeTestRule.onNodeWithTag("hero_bnr_parity").assertIsDisplayed()
        composeTestRule.onNodeWithText("BNR • 1 EUR = 4.9765 RON • 2026-03-09").assertIsDisplayed()
    }

    // 6: BNR unavailable state does not fabricate a value
    @Test
    fun testBnrUnavailableStateDoesNotFabricateValue() {
        val metrics = DashboardMetrics(
            balance = 50000.0,
            currency = "RON",
            latestBnrRate = null,
            effectiveBnrDate = null,
            bnrStatus = "UNAVAILABLE"
        )

        composeTestRule.setContent {
            FinTrackTheme {
                UnifiedHeroCanvas(metrics = metrics)
            }
        }

        composeTestRule.onNodeWithTag("hero_bnr_parity").assertIsDisplayed()
        composeTestRule.onNodeWithText("BNR unavailable").assertIsDisplayed()
    }

    // 7 & 8: Income and Expense render correctly
    @Test
    fun testIncomeAndExpenseRenderCorrectly() {
        val metrics = DashboardMetrics(
            balance = 44911.0,
            currency = "RON",
            totalIncome = 131556.0,
            totalExpense = 86645.0
        )

        composeTestRule.setContent {
            FinTrackTheme {
                UnifiedHeroCanvas(metrics = metrics)
            }
        }

        composeTestRule.onNodeWithText("Income").assertIsDisplayed()
        composeTestRule.onNodeWithText("Expense").assertIsDisplayed()
        composeTestRule.onNodeWithText("+131 556 RON").assertIsDisplayed()
        composeTestRule.onNodeWithText("-86 645 RON").assertIsDisplayed()
    }

    // 9: Period label remains correct
    @Test
    fun testPeriodLabelRendersInHero() {
        val metrics = DashboardMetrics(
            balance = 10000.0,
            currency = "RON",
            periodLabel = "This Year"
        )

        composeTestRule.setContent {
            FinTrackTheme {
                UnifiedHeroCanvas(metrics = metrics)
            }
        }

        composeTestRule.onNodeWithTag("hero_period_badge").assertIsDisplayed()
        composeTestRule.onNodeWithText("This Year").assertIsDisplayed()
    }

    // 10: 360dp layout does not truncate primary balance or cash flow
    @Test
    @Config(qualifiers = "w360dp-h800dp")
    fun testHeroCanvasResponsiveAt360dp() {
        val metrics = DashboardMetrics(
            balance = 1250000.0,
            currency = "RON",
            totalIncome = 2500000.0,
            totalExpense = 1250000.0,
            secondaryCurrency = "EUR",
            secondaryCurrencyBalance = 250000.0,
            latestBnrRate = 5.0000,
            effectiveBnrDate = "2026-03-09",
            periodLabel = "All Time"
        )

        composeTestRule.setContent {
            FinTrackTheme {
                UnifiedHeroCanvas(metrics = metrics)
            }
        }

        composeTestRule.onNodeWithTag("unified_hero_canvas").assertIsDisplayed()
        composeTestRule.onNodeWithText("1 250 000 RON").assertIsDisplayed()
        composeTestRule.onNodeWithText("≈ 250 000 EUR").assertIsDisplayed()
        composeTestRule.onNodeWithText("+2 500 000 RON").assertIsDisplayed()
        composeTestRule.onNodeWithText("-1 250 000 RON").assertIsDisplayed()
    }

    // 11: 390dp layout remains stable
    @Test
    @Config(qualifiers = "w390dp-h844dp")
    fun testHeroCanvasStableAt390dp() {
        val metrics = DashboardMetrics(
            balance = -45200.0,
            currency = "EUR",
            totalIncome = 50000.0,
            totalExpense = 95200.0,
            secondaryCurrency = "RON",
            secondaryCurrencyBalance = -226000.0,
            periodLabel = "Last Month"
        )

        composeTestRule.setContent {
            FinTrackTheme {
                UnifiedHeroCanvas(metrics = metrics)
            }
        }

        composeTestRule.onNodeWithTag("unified_hero_canvas").assertIsDisplayed()
        composeTestRule.onNodeWithText("-45 200 EUR").assertIsDisplayed()
        composeTestRule.onNodeWithText("≈ -226 000 RON").assertIsDisplayed()
    }

    // 12: 412dp+ layout remains stable
    @Test
    @Config(qualifiers = "w412dp-h915dp")
    fun testHeroCanvasStableAt412dp() {
        val metrics = DashboardMetrics(
            balance = 12345678.0,
            currency = "RON",
            totalIncome = 15000000.0,
            totalExpense = 2654322.0,
            secondaryCurrency = "EUR",
            secondaryCurrencyBalance = 2469135.0,
            latestBnrRate = 5.0000,
            effectiveBnrDate = "2026-03-09",
            periodLabel = "All Time"
        )

        composeTestRule.setContent {
            FinTrackTheme {
                UnifiedHeroCanvas(metrics = metrics)
            }
        }

        composeTestRule.onNodeWithTag("unified_hero_canvas").assertIsDisplayed()
        composeTestRule.onNodeWithText("12 345 678 RON").assertIsDisplayed()
    }

    // 13: Reduced motion behavior is respected
    @Test
    fun testReducedMotionRespectedInHero() {
        val context = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>()
        android.provider.Settings.Global.putFloat(
            context.contentResolver,
            android.provider.Settings.Global.TRANSITION_ANIMATION_SCALE,
            0f
        )
        android.provider.Settings.Global.putFloat(
            context.contentResolver,
            android.provider.Settings.Global.ANIMATOR_DURATION_SCALE,
            0f
        )

        val metrics = DashboardMetrics(
            balance = 5000.0,
            currency = "RON",
            totalIncome = 10000.0,
            totalExpense = 5000.0
        )

        composeTestRule.setContent {
            FinTrackTheme {
                UnifiedHeroCanvas(metrics = metrics)
            }
        }

        composeTestRule.onNodeWithTag("unified_hero_canvas").assertIsDisplayed()
        composeTestRule.onNodeWithText("5 000 RON").assertIsDisplayed()
    }

    // 14: Existing currency switching behavior remains intact in DashboardScreen
    @Test
    fun testCurrencySwitchingBehaviorRemainsIntact() {
        var switchedCurrency = ""
        val metrics = DashboardMetrics(
            balance = 1000.0,
            currency = "RON"
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
                    onCurrencyChanged = { switchedCurrency = it }
                )
            }
        }

        composeTestRule.onNodeWithTag("currency_toggle_EUR").performClick()
        assertEquals("EUR", switchedCurrency)
    }

    // 15: Existing Dashboard components and controls remain intact
    @Test
    fun testDashboardControlsAndStructureRemainIntact() {
        val metrics = DashboardMetrics(
            balance = 2000.0,
            currency = "RON"
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

        composeTestRule.onNodeWithTag("period_selector_dropdown").assertIsDisplayed()
        composeTestRule.onNodeWithTag("currency_toggle_RON").assertIsDisplayed()
        composeTestRule.onNodeWithTag("currency_toggle_EUR").assertIsDisplayed()
        composeTestRule.onNodeWithTag("dashboard_top_card").assertIsDisplayed()
        composeTestRule.onNodeWithTag("unified_hero_canvas").assertIsDisplayed()
    }
}
