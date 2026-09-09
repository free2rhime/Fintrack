package com.example

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.ui.navigation.BottomNavItem
import com.example.ui.navigation.FinTrackBottomNavigation
import com.example.ui.theme.FinTrackTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class FloatingNavigationTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testAllFiveDestinationsPresent() {
        var selectedTab = 0

        composeTestRule.setContent {
            FinTrackTheme {
                FinTrackBottomNavigation(
                    selectedTabIndex = selectedTab,
                    onTabSelected = { selectedTab = it }
                )
            }
        }

        // Test tag verification
        composeTestRule.onNodeWithTag("bottom_navigation_bar").assertIsDisplayed()
        composeTestRule.onNodeWithTag("bottom_nav_dashboard").assertIsDisplayed()
        composeTestRule.onNodeWithTag("bottom_nav_transactions").assertIsDisplayed()
        composeTestRule.onNodeWithTag("bottom_nav_analytics").assertIsDisplayed()
        composeTestRule.onNodeWithTag("bottom_nav_categories").assertIsDisplayed()
        composeTestRule.onNodeWithTag("bottom_nav_settings").assertIsDisplayed()

        // Tab selection behavior
        composeTestRule.onNodeWithTag("bottom_nav_dashboard").assertIsSelected()
        composeTestRule.onNodeWithTag("bottom_nav_transactions").performClick()
        assertEquals(1, selectedTab)
    }

    @Test
    @Config(qualifiers = "w360dp-h800dp")
    fun testFloatingNavOnCompact360dp() {
        composeTestRule.setContent {
            FinTrackTheme {
                FinTrackBottomNavigation(
                    selectedTabIndex = 1, // "Transactions" is active
                    onTabSelected = {}
                )
            }
        }

        composeTestRule.onNodeWithTag("bottom_navigation_bar").assertIsDisplayed()
        // Active destination has full label displayed ("Transactions" is never shortened)
        composeTestRule.onNodeWithText("Transactions").assertIsDisplayed()
        composeTestRule.onNodeWithTag("bottom_nav_transactions").assertIsSelected()

        // Inactive destinations remain accessible
        composeTestRule.onNodeWithTag("bottom_nav_dashboard").assertIsDisplayed()
        composeTestRule.onNodeWithTag("bottom_nav_analytics").assertIsDisplayed()
        composeTestRule.onNodeWithTag("bottom_nav_categories").assertIsDisplayed()
        composeTestRule.onNodeWithTag("bottom_nav_settings").assertIsDisplayed()
    }

    @Test
    @Config(qualifiers = "w390dp-h844dp")
    fun testFloatingNavOnMedium390dp() {
        var activeTab = 2 // Analytics active

        composeTestRule.setContent {
            FinTrackTheme {
                FinTrackBottomNavigation(
                    selectedTabIndex = activeTab,
                    onTabSelected = { activeTab = it }
                )
            }
        }

        composeTestRule.onNodeWithTag("bottom_navigation_bar").assertIsDisplayed()
        composeTestRule.onNodeWithText("Analytics").assertIsDisplayed()
        composeTestRule.onNodeWithTag("bottom_nav_analytics").assertIsSelected()

        composeTestRule.onNodeWithTag("bottom_nav_categories").performClick()
        assertEquals(3, activeTab)
    }

    @Test
    @Config(qualifiers = "w412dp-h915dp")
    fun testFloatingNavOnExpanded412dp() {
        composeTestRule.setContent {
            FinTrackTheme {
                FinTrackBottomNavigation(
                    selectedTabIndex = 4, // Settings active
                    onTabSelected = {}
                )
            }
        }

        composeTestRule.onNodeWithTag("bottom_navigation_bar").assertIsDisplayed()
        composeTestRule.onNodeWithText("Settings").assertIsDisplayed()
        composeTestRule.onNodeWithTag("bottom_nav_settings").assertIsSelected()
    }

    @Test
    fun testDestinationsEnumContract() {
        val items = BottomNavItem.values()
        assertEquals(5, items.size)
        assertEquals("Dashboard", items[0].title)
        assertEquals(0, items[0].tabIndex)
        assertEquals("Transactions", items[1].title)
        assertEquals(1, items[1].tabIndex)
        assertEquals("Analytics", items[2].title)
        assertEquals(2, items[2].tabIndex)
        assertEquals("Categories", items[3].title)
        assertEquals(3, items[3].tabIndex)
        assertEquals("Settings", items[4].title)
        assertEquals(4, items[4].tabIndex)
    }
}
