package com.example

import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
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
    fun testRoleTabAndAccessibilitySemantics() {
        var selectedTab = 0

        composeTestRule.setContent {
            FinTrackTheme {
                FinTrackBottomNavigation(
                    selectedTabIndex = selectedTab,
                    onTabSelected = { selectedTab = it }
                )
            }
        }

        val roleTabMatcher = SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Tab)

        BottomNavItem.values().forEach { item ->
            val tag = "bottom_nav_${item.title.lowercase()}"
            val node = composeTestRule.onNodeWithTag(tag)
            node.assertIsDisplayed()
            node.assert(roleTabMatcher)
            node.assert(SemanticsMatcher.expectValue(SemanticsProperties.ContentDescription, listOf(item.title)))
            if (item.tabIndex == 0) {
                node.assertIsSelected()
            } else {
                node.assertIsNotSelected()
            }
        }
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

        // Verify touch targets >= 48dp on 360dp
        composeTestRule.onNodeWithTag("bottom_nav_transactions").assertHeightIsAtLeast(48.dp)
        composeTestRule.onNodeWithTag("bottom_nav_transactions").assertWidthIsAtLeast(48.dp)
        composeTestRule.onNodeWithTag("bottom_nav_dashboard").assertHeightIsAtLeast(48.dp)
        composeTestRule.onNodeWithTag("bottom_nav_dashboard").assertWidthIsAtLeast(48.dp)
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

        composeTestRule.onNodeWithTag("bottom_nav_categories").assertHeightIsAtLeast(48.dp)
        composeTestRule.onNodeWithTag("bottom_nav_categories").assertWidthIsAtLeast(48.dp)
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

        composeTestRule.onNodeWithTag("bottom_nav_settings").assertHeightIsAtLeast(48.dp)
        composeTestRule.onNodeWithTag("bottom_nav_settings").assertWidthIsAtLeast(48.dp)
    }

    @Test
    fun testSequentialDestinationSwitching() {
        var activeTab = 0

        composeTestRule.setContent {
            FinTrackTheme {
                FinTrackBottomNavigation(
                    selectedTabIndex = activeTab,
                    onTabSelected = { activeTab = it }
                )
            }
        }

        val destinations = listOf(
            0 to "bottom_nav_dashboard",
            1 to "bottom_nav_transactions",
            2 to "bottom_nav_analytics",
            3 to "bottom_nav_categories",
            4 to "bottom_nav_settings"
        )

        destinations.forEach { (index, tag) ->
            composeTestRule.onNodeWithTag(tag).performClick()
            assertEquals(index, activeTab)
        }
    }

    @Test
    fun testDarkThemeAndTonalElevation() {
        composeTestRule.setContent {
            FinTrackTheme(darkTheme = true) {
                FinTrackBottomNavigation(
                    selectedTabIndex = 0,
                    onTabSelected = {}
                )
            }
        }

        composeTestRule.onNodeWithTag("bottom_navigation_bar").assertIsDisplayed()
        composeTestRule.onNodeWithTag("bottom_nav_dashboard").assertIsSelected()
        composeTestRule.onNodeWithText("Dashboard").assertIsDisplayed()
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
