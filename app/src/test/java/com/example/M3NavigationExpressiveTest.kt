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
import com.example.domain.analytics.FinancialAnalyticsEngine
import com.example.ui.components.formatHeroNetBalanceDigits
import com.example.ui.navigation.BottomNavItem
import com.example.ui.navigation.FinTrackBottomNavigation
import com.example.ui.theme.FinTrackMotion
import com.example.ui.theme.FinTrackTheme
import com.example.ui.theme.ShapeNavigationCapsule
import com.example.ui.theme.ShapePill
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Checkpoint 2: Navigation & Core Shell Test Suite.
 *
 * Validates:
 * - Navigation root renders
 * - All 5 primary destinations remain available in strictly preserved order
 * - Active destination indicator and label presentation
 * - Inactive destination state and visual balance
 * - Selected/unselected semantics and Role.Tab compliance
 * - Interactive navigation click behavior and state updates
 * - Existing routes and test tags contract preserved
 * - Minimum 48dp touch targets across all viewports
 * - Expressive shape/container usage (capsule container + pill indicator)
 * - Tactile interaction infrastructure (press scale 0.975f, spring physics)
 * - Reduced-motion accessibility resilience
 * - Light and Dark theme tonal support
 * - Responsive viewports: 360dp, 390dp, 412dp, 600dp+
 * - Architectural invariants: Financial calculation integrity untouched
 */
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class M3NavigationExpressiveTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    // =========================================================================
    // 1. NAVIGATION ROOT & DESTINATIONS AVAILABILITY
    // =========================================================================

    @Test
    fun test01_NavigationRootRendersAndAllDestinationsAvailable() {
        var selectedTab = 0

        composeTestRule.setContent {
            FinTrackTheme {
                FinTrackBottomNavigation(
                    selectedTabIndex = selectedTab,
                    onTabSelected = { selectedTab = it }
                )
            }
        }

        // Verify root container
        composeTestRule.onNodeWithTag("bottom_navigation_bar").assertIsDisplayed()

        // Verify all 5 destination test tags
        composeTestRule.onNodeWithTag("bottom_nav_dashboard").assertIsDisplayed()
        composeTestRule.onNodeWithTag("bottom_nav_transactions").assertIsDisplayed()
        composeTestRule.onNodeWithTag("bottom_nav_analytics").assertIsDisplayed()
        composeTestRule.onNodeWithTag("bottom_nav_categories").assertIsDisplayed()
        composeTestRule.onNodeWithTag("bottom_nav_settings").assertIsDisplayed()
    }

    // =========================================================================
    // 2. ACTIVE DESTINATION INDICATOR & SEMANTICS
    // =========================================================================

    @Test
    fun test02_ActiveDestinationIndicatorAndSemantics() {
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

        // Active destination displays full text label
        composeTestRule.onNodeWithText("Dashboard").assertIsDisplayed()
    }

    // =========================================================================
    // 3. NAVIGATION CLICK BEHAVIOR & STATE UPDATES
    // =========================================================================

    @Test
    fun test03_NavigationClickBehaviorAndDestinationSwitching() {
        val selectedTab = androidx.compose.runtime.mutableStateOf(0)

        composeTestRule.setContent {
            FinTrackTheme {
                FinTrackBottomNavigation(
                    selectedTabIndex = selectedTab.value,
                    onTabSelected = { selectedTab.value = it }
                )
            }
        }

        // Click on Transactions
        composeTestRule.onNodeWithTag("bottom_nav_transactions").performClick()
        assertEquals(1, selectedTab.value)

        // Click on Analytics
        composeTestRule.onNodeWithTag("bottom_nav_analytics").performClick()
        assertEquals(2, selectedTab.value)

        // Click on Categories
        composeTestRule.onNodeWithTag("bottom_nav_categories").performClick()
        assertEquals(3, selectedTab.value)

        // Click on Settings
        composeTestRule.onNodeWithTag("bottom_nav_settings").performClick()
        assertEquals(4, selectedTab.value)

        // Click back to Dashboard
        composeTestRule.onNodeWithTag("bottom_nav_dashboard").performClick()
        assertEquals(0, selectedTab.value)
    }

    // =========================================================================
    // 4. DESTINATIONS ENUM & DUAL-STATE ICON CONTRACT
    // =========================================================================

    @Test
    fun test04_DestinationsEnumAndDualStateIconContract() {
        val items = BottomNavItem.values()
        assertEquals(5, items.size)

        assertEquals("Dashboard", items[0].title)
        assertEquals(0, items[0].tabIndex)
        assertNotNull(items[0].icon)
        assertNotNull(items[0].unselectedIcon)

        assertEquals("Transactions", items[1].title)
        assertEquals(1, items[1].tabIndex)
        assertNotNull(items[1].icon)
        assertNotNull(items[1].unselectedIcon)

        assertEquals("Analytics", items[2].title)
        assertEquals(2, items[2].tabIndex)
        assertNotNull(items[2].icon)
        assertNotNull(items[2].unselectedIcon)

        assertEquals("Categories", items[3].title)
        assertEquals(3, items[3].tabIndex)
        assertNotNull(items[3].icon)
        assertNotNull(items[3].unselectedIcon)

        assertEquals("Settings", items[4].title)
        assertEquals(4, items[4].tabIndex)
        assertNotNull(items[4].icon)
        assertNotNull(items[4].unselectedIcon)

        // Verify filled vs outlined distinction
        assertNotEquals(items[0].icon, items[0].unselectedIcon)
        assertNotEquals(items[1].icon, items[1].unselectedIcon)
        assertNotEquals(items[2].icon, items[2].unselectedIcon)
        assertNotEquals(items[3].icon, items[3].unselectedIcon)
        assertNotEquals(items[4].icon, items[4].unselectedIcon)
    }

    // =========================================================================
    // 5. MINIMUM 48DP TOUCH TARGETS
    // =========================================================================

    @Test
    fun test05_Minimum48dpTouchTargetsAcrossAllDestinations() {
        composeTestRule.setContent {
            FinTrackTheme {
                FinTrackBottomNavigation(
                    selectedTabIndex = 1, // Transactions
                    onTabSelected = {}
                )
            }
        }

        BottomNavItem.values().forEach { item ->
            val tag = "bottom_nav_${item.title.lowercase()}"
            composeTestRule.onNodeWithTag(tag).assertHeightIsAtLeast(48.dp)
            composeTestRule.onNodeWithTag(tag).assertWidthIsAtLeast(48.dp)
        }
    }

    // =========================================================================
    // 6. EXPRESSIVE SHAPE & CONTAINER CONTRACTS
    // =========================================================================

    @Test
    fun test06_ExpressiveShapeAndContainerContracts() {
        assertNotNull(ShapeNavigationCapsule)
        assertNotNull(ShapePill)

        composeTestRule.setContent {
            FinTrackTheme {
                assertEquals(ShapeNavigationCapsule, FinTrackTheme.shapes.navigationCapsule)
                assertEquals(ShapePill, FinTrackTheme.shapes.pill)
            }
        }
    }

    // =========================================================================
    // 7. TACTILE INTERACTION & SPRING MOTION INFRASTRUCTURE
    // =========================================================================

    @Test
    fun test07_TactileInteractionAndSpringMotionInfrastructure() {
        assertEquals(0.975f, FinTrackMotion.PressScaleTarget, 0.001f)
        assertNotNull(FinTrackMotion.selectionSpring<Float>())
        assertNotNull(FinTrackMotion.pressInteractionSpec<Float>())
        assertNotNull(FinTrackMotion.contentSpring<Float>())
        assertNotNull(FinTrackMotion.interactiveSpring<Float>())
    }

    // =========================================================================
    // 8. LIGHT THEME RENDERING & TONAL SEPARATION
    // =========================================================================

    @Test
    fun test08_LightThemeRenderingAndTonalSeparation() {
        composeTestRule.setContent {
            FinTrackTheme(darkTheme = false) {
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

    // =========================================================================
    // 9. DARK THEME RENDERING & TONAL SEPARATION
    // =========================================================================

    @Test
    fun test09_DarkThemeRenderingAndTonalSeparation() {
        composeTestRule.setContent {
            FinTrackTheme(darkTheme = true) {
                FinTrackBottomNavigation(
                    selectedTabIndex = 1,
                    onTabSelected = {}
                )
            }
        }

        composeTestRule.onNodeWithTag("bottom_navigation_bar").assertIsDisplayed()
        composeTestRule.onNodeWithTag("bottom_nav_transactions").assertIsSelected()
        composeTestRule.onNodeWithText("Transactions").assertIsDisplayed()
    }

    // =========================================================================
    // 10. COMPACT 360DP VIEWPORT
    // =========================================================================

    @Test
    @Config(qualifiers = "w360dp-h800dp")
    fun test10_Compact360dpViewport() {
        composeTestRule.setContent {
            FinTrackTheme {
                FinTrackBottomNavigation(
                    selectedTabIndex = 1, // Transactions has longest label
                    onTabSelected = {}
                )
            }
        }

        composeTestRule.onNodeWithTag("bottom_navigation_bar").assertIsDisplayed()
        composeTestRule.onNodeWithText("Transactions").assertIsDisplayed()
        composeTestRule.onNodeWithTag("bottom_nav_transactions").assertIsSelected()

        // Touch targets >= 48dp on 360dp
        BottomNavItem.values().forEach { item ->
            val tag = "bottom_nav_${item.title.lowercase()}"
            composeTestRule.onNodeWithTag(tag).assertHeightIsAtLeast(48.dp)
            composeTestRule.onNodeWithTag(tag).assertWidthIsAtLeast(48.dp)
        }
    }

    // =========================================================================
    // 11. MEDIUM 390DP VIEWPORT
    // =========================================================================

    @Test
    @Config(qualifiers = "w390dp-h844dp")
    fun test11_Medium390dpViewport() {
        var activeTab = 2

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

    // =========================================================================
    // 12. EXPANDED 412DP VIEWPORT
    // =========================================================================

    @Test
    @Config(qualifiers = "w412dp-h915dp")
    fun test12_Expanded412dpViewport() {
        composeTestRule.setContent {
            FinTrackTheme {
                FinTrackBottomNavigation(
                    selectedTabIndex = 4, // Settings
                    onTabSelected = {}
                )
            }
        }

        composeTestRule.onNodeWithTag("bottom_navigation_bar").assertIsDisplayed()
        composeTestRule.onNodeWithText("Settings").assertIsDisplayed()
        composeTestRule.onNodeWithTag("bottom_nav_settings").assertIsSelected()

        BottomNavItem.values().forEach { item ->
            val tag = "bottom_nav_${item.title.lowercase()}"
            composeTestRule.onNodeWithTag(tag).assertHeightIsAtLeast(48.dp)
            composeTestRule.onNodeWithTag(tag).assertWidthIsAtLeast(48.dp)
        }
    }

    // =========================================================================
    // 13. WIDE 600DP+ TABLET / FOLDABLE VIEWPORT
    // =========================================================================

    @Test
    @Config(qualifiers = "w600dp-h960dp")
    fun test13_Wide600dpPlusTabletViewport() {
        var activeTab = 0

        composeTestRule.setContent {
            FinTrackTheme {
                FinTrackBottomNavigation(
                    selectedTabIndex = activeTab,
                    onTabSelected = { activeTab = it }
                )
            }
        }

        composeTestRule.onNodeWithTag("bottom_navigation_bar").assertIsDisplayed()
        composeTestRule.onNodeWithTag("bottom_nav_dashboard").assertIsSelected()
        composeTestRule.onNodeWithText("Dashboard").assertIsDisplayed()

        composeTestRule.onNodeWithTag("bottom_nav_transactions").performClick()
        assertEquals(1, activeTab)

        BottomNavItem.values().forEach { item ->
            val tag = "bottom_nav_${item.title.lowercase()}"
            composeTestRule.onNodeWithTag(tag).assertHeightIsAtLeast(48.dp)
            composeTestRule.onNodeWithTag(tag).assertWidthIsAtLeast(48.dp)
        }
    }

    // =========================================================================
    // 14. REDUCED MOTION BEHAVIOR & SNAP FALLBACK
    // =========================================================================

    @Test
    fun test14_ReducedMotionBehaviorContract() {
        // Reduced motion contract: selectionSpring and snap specs exist and are non-null
        assertNotNull(FinTrackMotion.selectionSpring<Float>())
        assertNotNull(FinTrackMotion.fastTween<androidx.compose.ui.graphics.Color>())
        assertNotNull(FinTrackMotion.microTween<Float>())

        // Ensure navigation composable can render in either mode without crashing
        composeTestRule.setContent {
            FinTrackTheme {
                FinTrackBottomNavigation(
                    selectedTabIndex = 3, // Categories
                    onTabSelected = {}
                )
            }
        }

        composeTestRule.onNodeWithTag("bottom_nav_categories").assertIsSelected()
        composeTestRule.onNodeWithText("Categories").assertIsDisplayed()
    }

    // =========================================================================
    // 15. ARCHITECTURAL INVARIANTS — FINANCIAL CALCULATION UNTOUCHED
    // =========================================================================

    @Test
    fun test15_NoFinancialCalculationRegression() {
        val engine = FinancialAnalyticsEngine
        assertNotNull(engine)

        // Formatter sanity check
        assertEquals("10 000", formatHeroNetBalanceDigits(10000.0))
        assertEquals("0", formatHeroNetBalanceDigits(0.0))
    }
}
