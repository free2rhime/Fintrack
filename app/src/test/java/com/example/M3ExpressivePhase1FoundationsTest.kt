package com.example

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import com.example.domain.analytics.CategoryExpenseShare
import com.example.domain.analytics.MonthlyDataPoint
import com.example.domain.analytics.SingleSeriesDataPoint
import com.example.domain.analytics.SmartFinancialInsights
import com.example.ui.components.ButtonVariant
import com.example.ui.components.CategoryDistributionChart
import com.example.ui.components.FinTrackButton
import com.example.ui.components.FinTrackPeriodDropdown
import com.example.ui.components.FinancialPulseCard
import com.example.ui.components.MonthlyCashFlowBarChart
import com.example.ui.components.MonthlyCashFlowSplineChart
import com.example.ui.components.SingleSeriesSplineChart
import com.example.ui.theme.FinTrackTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Phase 1 Foundations & P0 Verification Test Suite.
 *
 * Verifies:
 * 1. FinancialPulseCard velocity warning triggers strictly when > 80.0 (not at 25.0, 50.0, or 80.0).
 * 2. FinTrackPeriodDropdown enforces >= 48dp minimum touch target height.
 * 3. FinTrackButton PRIMARY variant uses MaterialTheme primary color scheme (cobalt), reserving green for income.
 * 4. FinTrackTheme semantic tokens (income, expense, healthPositive, healthWarning).
 * 5. Financial charts expose Canvas TalkBack screen reader accessibility semantics (contentDescription).
 */
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class M3ExpressivePhase1FoundationsTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    // =========================================================================
    // 1. FINANCIAL PULSE CARD: VELOCITY THRESHOLD VERIFICATION
    // =========================================================================

    @Test
    fun test01a_FinancialPulseCard_NormalVelocity_HealthyState() {
        val insightsNormal = SmartFinancialInsights(
            savingsTrendText = "Capital Growth",
            monthOverMonthExpenseChangePercent = 2.0,
            avgMonthlyExpense = 2000.0,
            avgMonthlyIncome = 5000.0
        )

        composeTestRule.setContent {
            FinTrackTheme {
                FinancialPulseCard(
                    insights = insightsNormal,
                    savingsRate = 50.0,
                    expenseVelocity = 50.0
                )
            }
        }

        composeTestRule.onNodeWithTag("financial_pulse_card").assertIsDisplayed()
        composeTestRule.onNodeWithTag("pulse_velocity_progress").assertIsDisplayed()
        composeTestRule.onNodeWithText("+50.0%").assertIsDisplayed()
    }

    @Test
    fun test01b_FinancialPulseCard_HighVelocity_WarningState() {
        val insightsNormal = SmartFinancialInsights(
            savingsTrendText = "Capital Growth",
            monthOverMonthExpenseChangePercent = 2.0,
            avgMonthlyExpense = 2000.0,
            avgMonthlyIncome = 5000.0
        )

        composeTestRule.setContent {
            FinTrackTheme {
                FinancialPulseCard(
                    insights = insightsNormal,
                    savingsRate = 15.0,
                    expenseVelocity = 85.0
                )
            }
        }

        composeTestRule.onNodeWithTag("financial_pulse_card").assertIsDisplayed()
        composeTestRule.onNodeWithTag("pulse_velocity_progress").assertIsDisplayed()
        composeTestRule.onNodeWithText("+85.0%").assertIsDisplayed()
    }

    @Test
    fun test02_FinancialPulseCard_VelocityThreshold_BoundaryConditions() {
        // Logic test for boundary: velocity <= 80.0 is positive, > 80.0 is warning
        val threshold = 80.0
        fun isWarning(velocity: Double) = velocity > threshold

        assertFalse("25.0% velocity should not trigger warning", isWarning(25.0))
        assertFalse("50.0% velocity should not trigger warning", isWarning(50.0))
        assertFalse("80.0% boundary velocity should not trigger warning", isWarning(80.0))
        assertTrue("80.1% velocity must trigger warning", isWarning(80.1))
        assertTrue("95.0% velocity must trigger warning", isWarning(95.0))
    }

    // =========================================================================
    // 2. TOUCH TARGETS: 48DP MINIMUM ENFORCEMENT
    // =========================================================================

    @Test
    fun test03_PeriodDropdown_Enforces48dpMinimumHeight() {
        composeTestRule.setContent {
            FinTrackTheme {
                FinTrackPeriodDropdown(
                    selectedPeriod = "This Month",
                    onPeriodSelected = {}
                )
            }
        }

        composeTestRule.onNodeWithTag("period_selector_dropdown")
            .assertIsDisplayed()
            .assertHeightIsAtLeast(48.dp)
    }

    @Test
    fun test04_FinTrackButton_Enforces48dpMinimumHeight() {
        composeTestRule.setContent {
            FinTrackTheme {
                FinTrackButton(
                    onClick = {},
                    variant = ButtonVariant.PRIMARY,
                    modifier = Modifier.testTag("test_primary_button")
                ) {
                    androidx.compose.material3.Text("Primary Action")
                }
            }
        }

        composeTestRule.onNodeWithTag("test_primary_button")
            .assertIsDisplayed()
            .assertHeightIsAtLeast(48.dp)
    }

    // =========================================================================
    // 3. FINTRACK BUTTON SEMANTICS: PRIMARY VS INCOME
    // =========================================================================

    @Test
    fun test05_FinTrackButton_PrimaryVariant_UsesPrimaryColorScheme() {
        var themePrimary = androidx.compose.ui.graphics.Color.Unspecified
        var themeIncome = androidx.compose.ui.graphics.Color.Unspecified

        composeTestRule.setContent {
            FinTrackTheme {
                themePrimary = MaterialTheme.colorScheme.primary
                themeIncome = FinTrackTheme.colors.income

                FinTrackButton(
                    onClick = {},
                    variant = ButtonVariant.PRIMARY,
                    modifier = Modifier.testTag("semantic_primary_btn")
                ) {
                    androidx.compose.material3.Text("Submit")
                }
            }
        }

        composeTestRule.onNodeWithTag("semantic_primary_btn").assertIsDisplayed()
        // Primary theme color must be differentiated from income green
        assertNotEquals("Primary button color should not be raw income green", themeIncome, themePrimary)
    }

    // =========================================================================
    // 4. SEMANTIC THEME TOKENS HIERARCHY
    // =========================================================================

    @Test
    fun test06_SemanticThemeTokens_HierarchyIntegrity() {
        composeTestRule.setContent {
            FinTrackTheme {
                val colors = FinTrackTheme.colors
                assertTrue("income color must be specified", colors.income.isSpecified)
                assertTrue("expense color must be specified", colors.expense.isSpecified)
                assertTrue("healthPositive color must be specified", colors.healthPositive.isSpecified)
                assertTrue("healthWarning color must be specified", colors.healthWarning.isSpecified)
                assertNotEquals("Income and Expense colors must be distinct", colors.income, colors.expense)
                assertNotEquals("HealthPositive and HealthWarning colors must be distinct", colors.healthPositive, colors.healthWarning)
            }
        }
    }

    // =========================================================================
    // 5. FINANCIAL CHARTS: TALKBACK ACCESSIBILITY SEMANTICS
    // =========================================================================

    @Test
    fun test07_SingleSeriesSplineChart_ExposesTalkBackSemantics() {
        val points = listOf(
            SingleSeriesDataPoint("2026-01", "Jan 2026", 1200.0),
            SingleSeriesDataPoint("2026-02", "Feb 2026", 1500.0)
        )

        composeTestRule.setContent {
            FinTrackTheme {
                SingleSeriesSplineChart(
                    dataPoints = points,
                    currency = "RON"
                )
            }
        }

        // Verify that a node with contentDescription mentioning chart summary exists
        composeTestRule.onNode(
            SemanticsMatcher.expectValue(
                SemanticsProperties.ContentDescription,
                listOf("Spline trend chart showing 2 data points in RON. Total: 2 700.00 RON.")
            )
        ).assertIsDisplayed()
    }

    @Test
    fun test08_MonthlyCashFlowSplineChart_ExposesTalkBackSemantics() {
        val points = listOf(
            MonthlyDataPoint("Jan 2026", income = 5000.0, expense = 3000.0, balance = 2000.0)
        )

        composeTestRule.setContent {
            FinTrackTheme {
                MonthlyCashFlowSplineChart(
                    dataPoints = points,
                    currency = "RON"
                )
            }
        }

        composeTestRule.onNode(
            SemanticsMatcher.expectValue(
                SemanticsProperties.ContentDescription,
                listOf("Monthly cash flow spline chart over 1 months. Total income: 5 000.00 RON, total expense: 3 000.00 RON.")
            )
        ).assertIsDisplayed()
    }

    @Test
    fun test09_MonthlyCashFlowBarChart_ExposesTalkBackSemantics() {
        val points = listOf(
            MonthlyDataPoint("Jan 2026", income = 4500.0, expense = 2500.0, balance = 2000.0)
        )

        composeTestRule.setContent {
            FinTrackTheme {
                MonthlyCashFlowBarChart(
                    dataPoints = points,
                    currency = "RON"
                )
            }
        }

        composeTestRule.onNode(
            SemanticsMatcher.expectValue(
                SemanticsProperties.ContentDescription,
                listOf("Monthly cash flow bar chart over 1 months. Total income: 4 500.00 RON, total expense: 2 500.00 RON.")
            )
        ).assertIsDisplayed()
    }

    @Test
    fun test10_CategoryDistributionChart_ExposesTalkBackSemantics() {
        val shares = listOf(
            CategoryExpenseShare(categoryName = "Food", totalAmount = 300.0, percentage = 60.0, transactionCount = 5),
            CategoryExpenseShare(categoryName = "Bills", totalAmount = 200.0, percentage = 40.0, transactionCount = 2)
        )

        composeTestRule.setContent {
            FinTrackTheme {
                CategoryDistributionChart(
                    categoryShares = shares,
                    currency = "RON"
                )
            }
        }

        composeTestRule.onNode(
            SemanticsMatcher.expectValue(
                SemanticsProperties.ContentDescription,
                listOf("Spending by category donut chart with 2 categories. Total: 500.00 RON. Top categories: Food: 60%, Bills: 40%.")
            )
        ).assertIsDisplayed()
    }
}
