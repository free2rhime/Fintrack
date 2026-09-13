package com.example

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasNoClickAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.data.model.FilterSettings
import com.example.ui.AnalyticsUiState
import com.example.ui.screens.AnalyticsScreen
import com.example.ui.theme.FinTrackTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Verification tests for the Analytics Screen Header UI Refinement:
 * 1. Analytics icon is completely removed.
 * 2. Subtitle "Financial flow & category distribution" is completely removed.
 * 3. "Analytics" title is present as the dominant headline.
 * 4. Date-filter indicator ("analytics_active_period_badge") is present and display-only (not clickable).
 * 5. Changes to Dashboard date filter state are immediately reflected in the Analytics badge.
 * 6. Currency toggle (RON/EUR) remains fully functional.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AnalyticsHeaderRefinementTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun test01_analyticsIconAndSubtitleAreCompletelyRemoved() {
        composeTestRule.setContent {
            FinTrackTheme {
                AnalyticsScreen(
                    analyticsUiState = AnalyticsUiState(),
                    filterSettings = FilterSettings(selectedPeriod = "Year To Date", selectedCurrency = "RON")
                )
            }
        }

        // Subtitle must NOT exist
        composeTestRule.onNodeWithText("Financial flow & category distribution").assertDoesNotExist()

        // Hero canvas must exist
        composeTestRule.onNodeWithTag("analytics_hero_canvas").assertIsDisplayed()

        // Title and badge must exist
        composeTestRule.onNodeWithText("Analytics").assertIsDisplayed()
        composeTestRule.onNodeWithTag("analytics_active_period_badge").assertIsDisplayed()
    }

    @Test
    fun test02_analyticsTitleIsPresentAsDominantHeadline() {
        composeTestRule.setContent {
            FinTrackTheme {
                AnalyticsScreen(
                    analyticsUiState = AnalyticsUiState(),
                    filterSettings = FilterSettings(selectedPeriod = "This Month", selectedCurrency = "RON")
                )
            }
        }

        composeTestRule.onNodeWithText("Analytics").assertIsDisplayed()
    }

    @Test
    fun test03_activePeriodBadgeIsPresentAndReflectsSelectedPeriod() {
        composeTestRule.setContent {
            FinTrackTheme {
                AnalyticsScreen(
                    analyticsUiState = AnalyticsUiState(),
                    filterSettings = FilterSettings(selectedPeriod = "Year To Date", selectedCurrency = "RON")
                )
            }
        }

        composeTestRule.onNodeWithTag("analytics_active_period_badge").assertIsDisplayed()
        composeTestRule.onNodeWithText("Year To Date").assertIsDisplayed()
    }

    @Test
    fun test04_activePeriodBadgeIsDisplayOnlyAndNotClickable() {
        composeTestRule.setContent {
            FinTrackTheme {
                AnalyticsScreen(
                    analyticsUiState = AnalyticsUiState(),
                    filterSettings = FilterSettings(selectedPeriod = "Year To Date", selectedCurrency = "RON")
                )
            }
        }

        // Badge must be display-only without click semantics
        composeTestRule.onNodeWithTag("analytics_active_period_badge").assert(hasNoClickAction())

        // No dropdown or date dialog should be opened or present
        composeTestRule.onNodeWithTag("period_selector_dropdown").assertDoesNotExist()
    }

    @Test
    fun test05_dashboardPeriodChangeIsImmediatelyReflectedInAnalyticsHeader() {
        val filterSettingsState = mutableStateOf(FilterSettings(selectedPeriod = "Year To Date", selectedCurrency = "RON"))

        composeTestRule.setContent {
            FinTrackTheme {
                AnalyticsScreen(
                    analyticsUiState = AnalyticsUiState(),
                    filterSettings = filterSettingsState.value
                )
            }
        }

        // Initial period displayed
        composeTestRule.onNodeWithText("Year To Date").assertIsDisplayed()

        // Dashboard filter updates to "This Month"
        filterSettingsState.value = filterSettingsState.value.copy(selectedPeriod = "This Month")
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("This Month").assertIsDisplayed()
        composeTestRule.onNodeWithText("Year To Date").assertDoesNotExist()

        // Dashboard filter updates to "Last 30 Days"
        filterSettingsState.value = filterSettingsState.value.copy(selectedPeriod = "Last 30 Days")
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Last 30 Days").assertIsDisplayed()
        composeTestRule.onNodeWithText("This Month").assertDoesNotExist()
    }

    @Test
    fun test06_currencyToggleWorksAsExpected() {
        var selectedCurrency: String? = null

        composeTestRule.setContent {
            FinTrackTheme {
                AnalyticsScreen(
                    analyticsUiState = AnalyticsUiState(),
                    filterSettings = FilterSettings(selectedPeriod = "Year To Date", selectedCurrency = "RON"),
                    onCurrencyChanged = { selectedCurrency = it }
                )
            }
        }

        composeTestRule.onNodeWithTag("currency_toggle_RON").assertIsDisplayed()
        composeTestRule.onNodeWithTag("currency_toggle_EUR").assertIsDisplayed()

        composeTestRule.onNodeWithTag("currency_toggle_EUR").performClick()
        assertEquals("EUR", selectedCurrency)
    }
}
