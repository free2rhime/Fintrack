package com.example

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.domain.analytics.SingleSeriesDataPoint
import com.example.ui.components.FinTrackDropdownSelector
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
