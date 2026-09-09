package com.example

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.domain.analytics.CategoryExpenseShare
import com.example.domain.analytics.MonthlyDataPoint
import com.example.domain.analytics.SingleSeriesDataPoint
import com.example.ui.components.CategoryDistributionChart
import com.example.ui.components.FinTrackDropdownSelector
import com.example.ui.components.MonthlyCashFlowSplineChart
import com.example.ui.components.SingleSeriesSplineChart
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class FinancialChartComponentsTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testSingleSeriesSplineChartRendersEmptyStateWhenNoData() {
        composeTestRule.setContent {
            SingleSeriesSplineChart(
                dataPoints = emptyList(),
                currency = "RON",
                lineColor = Color.Green
            )
        }

        composeTestRule.onNodeWithText("No Activity").assertIsDisplayed()
        composeTestRule.onNodeWithText("No data recorded for this period").assertIsDisplayed()
    }

    @Test
    fun testSingleSeriesSplineChartRendersDataPointsAndActiveIndicator() {
        val points = listOf(
            SingleSeriesDataPoint("2026-01", "Jan 2026", 1500.0),
            SingleSeriesDataPoint("2026-02", "Feb 2026", 0.0),
            SingleSeriesDataPoint("2026-03", "Mar 2026", 1200.0)
        )

        composeTestRule.setContent {
            SingleSeriesSplineChart(
                dataPoints = points,
                currency = "RON",
                lineColor = Color(0xFF22C55E)
            )
        }

        // Active point defaults to the last item ("Mar 2026" with 1 200.00 RON)
        composeTestRule.onNodeWithText("Mar 2026").assertIsDisplayed()
        composeTestRule.onNodeWithText("1 200.00 RON").assertIsDisplayed()

        // Clicking on "Jan" switches active indicator
        composeTestRule.onNodeWithText("Jan").performClick()
        composeTestRule.onNodeWithText("1 500.00 RON").assertIsDisplayed()

        // Clicking on "Feb" shows 0.00 RON (zero value handled properly)
        composeTestRule.onNodeWithText("Feb").performClick()
        composeTestRule.onNodeWithText("0.00 RON").assertIsDisplayed()
    }

    @Test
    fun testMonthlyCashFlowSplineChartRendersHudAndSwitchesMonths() {
        val points = listOf(
            MonthlyDataPoint("Jan 2026", income = 5000.0, expense = 2500.0, balance = 2500.0),
            MonthlyDataPoint("Feb 2026", income = 6000.0, expense = 3000.0, balance = 3000.0),
            MonthlyDataPoint("Mar 2026", income = 4500.0, expense = 4000.0, balance = 500.0)
        )

        composeTestRule.setContent {
            MonthlyCashFlowSplineChart(
                dataPoints = points,
                currency = "RON"
            )
        }

        // Canvas and HUD are present
        composeTestRule.onNodeWithTag("cash_flow_spline_canvas").assertIsDisplayed()
        composeTestRule.onNodeWithTag("cash_flow_hud").assertIsDisplayed()

        // Default active point is Mar 2026 (last point)
        composeTestRule.onNodeWithText("Mar 2026").assertIsDisplayed()
        composeTestRule.onNodeWithText("Net: +500.00 RON").assertIsDisplayed()

        // Click on Jan 2026
        composeTestRule.onNodeWithTag("month_x_label_0").performClick()
        composeTestRule.onNodeWithText("Jan 2026").assertIsDisplayed()
        composeTestRule.onNodeWithText("Net: +2 500.00 RON").assertIsDisplayed()
    }

    @Test
    fun testCategoryDistributionChartRendersDonutHudAndItems() {
        val shares = listOf(
            CategoryExpenseShare(categoryName = "Groceries", totalAmount = 3000.0, percentage = 60.0, transactionCount = 12),
            CategoryExpenseShare(categoryName = "Utilities", totalAmount = 2000.0, percentage = 40.0, transactionCount = 4)
        )

        composeTestRule.setContent {
            CategoryDistributionChart(
                categoryShares = shares,
                currency = "RON"
            )
        }

        // Donut box, canvas, HUD, and share items are rendered
        composeTestRule.onNodeWithTag("category_donut_box").assertIsDisplayed()
        composeTestRule.onNodeWithTag("category_donut_canvas").assertIsDisplayed()
        composeTestRule.onNodeWithTag("category_donut_hud").assertIsDisplayed()
        composeTestRule.onNodeWithTag("category_share_item_0").assertIsDisplayed()
        composeTestRule.onNodeWithTag("category_share_item_1").assertIsDisplayed()

        // Default selected category is top item (Groceries, 60%)
        composeTestRule.onNodeWithText("60%").assertIsDisplayed()

        // Clicking on Utilities selects it
        composeTestRule.onNodeWithTag("category_share_item_1").performClick()
        composeTestRule.onNodeWithText("40%").assertIsDisplayed()
    }

    @Test
    fun testFinTrackDropdownSelectorSelectionFlow() {
        data class TestItem(val id: String, val name: String, val share: Double)

        val items = listOf(
            TestItem("1", "Groceries", 32.4),
            TestItem("2", "Restaurants", 18.7),
            TestItem("3", "Transport", 14.2)
        )

        var selected: TestItem? = items[0]

        composeTestRule.setContent {
            FinTrackDropdownSelector(
                selectedItem = selected,
                items = items,
                onItemSelected = { selected = it },
                itemLabel = { it.name },
                itemDropdownLabel = { "${it.name} · ${it.share}%" },
                testTag = "test_selector"
            )
        }

        // Selected button displays just name "Groceries"
        composeTestRule.onNodeWithText("Groceries").assertIsDisplayed()

        // Open dropdown
        composeTestRule.onNodeWithTag("test_selector").performClick()

        // Items in dropdown show custom format "Groceries · 32.4%"
        composeTestRule.onNodeWithText("Groceries · 32.4%").assertIsDisplayed()
        composeTestRule.onNodeWithText("Restaurants · 18.7%").assertIsDisplayed()
        composeTestRule.onNodeWithText("Transport · 14.2%").assertIsDisplayed()

        // Select "Restaurants"
        composeTestRule.onNodeWithTag("test_selector_item_Restaurants").performClick()
        assertEquals(items[1], selected)
    }
}
