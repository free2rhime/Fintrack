package com.example

import android.content.Context
import android.provider.Settings
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.example.domain.analytics.CategoryExpenseShare
import com.example.domain.analytics.MonthlyDataPoint
import com.example.ui.components.CategoryDistributionChart
import com.example.ui.components.MonthlyCashFlowSplineChart
import com.example.ui.navigation.FinTrackBottomNavigation
import com.example.ui.theme.FinTrackTheme
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class Phase3CCheckpoint2MotionPolishTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @After
    fun tearDown() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        Settings.Global.putFloat(context.contentResolver, Settings.Global.TRANSITION_ANIMATION_SCALE, 1.0f)
        Settings.Global.putFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1.0f)
    }

    @Test
    fun testFloatingNavExpressiveSelectionAndSwitching() {
        var selectedTabIndex by mutableStateOf(0)

        composeTestRule.setContent {
            FinTrackTheme {
                FinTrackBottomNavigation(
                    selectedTabIndex = selectedTabIndex,
                    onTabSelected = { selectedTabIndex = it }
                )
            }
        }

        // Initially dashboard is selected
        composeTestRule.onNodeWithTag("bottom_nav_dashboard").assertIsSelected()
        composeTestRule.onNodeWithText("Dashboard").assertIsDisplayed()

        // Switch to Analytics
        composeTestRule.onNodeWithTag("bottom_nav_analytics").performClick()
        assertEquals(2, selectedTabIndex)

        composeTestRule.onNodeWithTag("bottom_nav_analytics").assertIsSelected()
        composeTestRule.onNodeWithText("Analytics").assertIsDisplayed()
    }

    @Test
    @Config(qualifiers = "w360dp-h800dp")
    fun testFloatingNavOnCompact360dpViewport() {
        var selectedTabIndex by mutableStateOf(1)

        composeTestRule.setContent {
            FinTrackTheme {
                FinTrackBottomNavigation(
                    selectedTabIndex = selectedTabIndex,
                    onTabSelected = { selectedTabIndex = it }
                )
            }
        }

        composeTestRule.onNodeWithTag("bottom_navigation_bar").assertIsDisplayed()
        composeTestRule.onNodeWithTag("bottom_nav_transactions").assertIsSelected()
        composeTestRule.onNodeWithText("Transactions").assertIsDisplayed()
    }

    @Test
    @Config(qualifiers = "w412dp-h915dp")
    fun testFloatingNavOnExpanded412dpViewport() {
        var selectedTabIndex by mutableStateOf(3)

        composeTestRule.setContent {
            FinTrackTheme {
                FinTrackBottomNavigation(
                    selectedTabIndex = selectedTabIndex,
                    onTabSelected = { selectedTabIndex = it }
                )
            }
        }

        composeTestRule.onNodeWithTag("bottom_navigation_bar").assertIsDisplayed()
        composeTestRule.onNodeWithTag("bottom_nav_categories").assertIsSelected()
        composeTestRule.onNodeWithText("Categories").assertIsDisplayed()
    }

    @Test
    fun testMonthlyCashFlowSplineChartInteractiveScrubbing() {
        val testData = listOf(
            MonthlyDataPoint(monthYearLabel = "Jan 2026", income = 4500.0, expense = 2800.0, balance = 1700.0),
            MonthlyDataPoint(monthYearLabel = "Feb 2026", income = 5200.0, expense = 3100.0, balance = 2100.0),
            MonthlyDataPoint(monthYearLabel = "Mar 2026", income = 4800.0, expense = 2600.0, balance = 2200.0)
        )

        composeTestRule.setContent {
            FinTrackTheme {
                MonthlyCashFlowSplineChart(
                    dataPoints = testData,
                    currency = "RON"
                )
            }
        }

        // Canvas & HUD are present
        composeTestRule.onNodeWithTag("cash_flow_spline_canvas").assertIsDisplayed()
        composeTestRule.onNodeWithTag("cash_flow_hud").assertIsDisplayed()

        // By default, last month (Mar 2026) is active
        composeTestRule.onNodeWithText("Mar 2026").assertIsDisplayed()
        composeTestRule.onNodeWithText("Net: +2 200.00 RON").assertIsDisplayed()

        // Click Jan 2026 to switch crosshair & HUD
        composeTestRule.onNodeWithTag("month_x_label_0").performClick()
        composeTestRule.onNodeWithText("Jan 2026").assertIsDisplayed()
        composeTestRule.onNodeWithText("Net: +1 700.00 RON").assertIsDisplayed()
    }

    @Test
    fun testCategoryDistributionChartDonutAndHudSelection() {
        val testShares = listOf(
            CategoryExpenseShare(categoryName = "Housing", totalAmount = 1500.0, percentage = 50.0, transactionCount = 1),
            CategoryExpenseShare(categoryName = "Groceries", totalAmount = 900.0, percentage = 30.0, transactionCount = 5),
            CategoryExpenseShare(categoryName = "Utilities", totalAmount = 600.0, percentage = 20.0, transactionCount = 2)
        )

        composeTestRule.setContent {
            FinTrackTheme {
                CategoryDistributionChart(
                    categoryShares = testShares,
                    currency = "RON"
                )
            }
        }

        // Verify Donut box and HUD
        composeTestRule.onNodeWithTag("category_donut_box").assertIsDisplayed()
        composeTestRule.onNodeWithTag("category_donut_canvas").assertIsDisplayed()
        composeTestRule.onNodeWithTag("category_donut_hud").assertIsDisplayed()

        // Initially "Housing" (first item) is focused in HUD
        composeTestRule.onNodeWithText("50%").assertIsDisplayed()
        composeTestRule.onNodeWithText("1 500.00 RON").assertIsDisplayed()

        // Click Groceries breakdown item
        composeTestRule.onNodeWithTag("category_share_item_1").performClick()
        composeTestRule.onNodeWithText("30%").assertIsDisplayed()
        composeTestRule.onNodeWithText("900.00 RON").assertIsDisplayed()

        // Click Groceries again to toggle/deselect
        composeTestRule.onNodeWithTag("category_share_item_1").performClick()
        composeTestRule.onNodeWithText("All Categories").assertIsDisplayed()
        composeTestRule.onNodeWithText("100%").assertIsDisplayed()
        composeTestRule.onNodeWithText("3 000.00 RON").assertIsDisplayed()
    }

    @Test
    fun testExpressiveComponentsUnderReducedMotion() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        Settings.Global.putFloat(context.contentResolver, Settings.Global.TRANSITION_ANIMATION_SCALE, 0f)
        Settings.Global.putFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 0f)

        val testData = listOf(
            MonthlyDataPoint(monthYearLabel = "Jan 2026", income = 4500.0, expense = 2800.0, balance = 1700.0)
        )
        val testShares = listOf(
            CategoryExpenseShare(categoryName = "Housing", totalAmount = 1500.0, percentage = 100.0, transactionCount = 1)
        )

        composeTestRule.setContent {
            FinTrackTheme {
                FinTrackBottomNavigation(
                    selectedTabIndex = 0,
                    onTabSelected = {}
                )
                MonthlyCashFlowSplineChart(
                    dataPoints = testData,
                    currency = "EUR"
                )
                CategoryDistributionChart(
                    categoryShares = testShares,
                    currency = "EUR"
                )
            }
        }

        // Assert all components render correctly and without errors when reduced motion is forced
        composeTestRule.onNodeWithTag("bottom_navigation_bar").assertIsDisplayed()
        composeTestRule.onNodeWithTag("cash_flow_spline_canvas").assertIsDisplayed()
        composeTestRule.onNodeWithTag("category_donut_canvas").assertIsDisplayed()
    }
}
