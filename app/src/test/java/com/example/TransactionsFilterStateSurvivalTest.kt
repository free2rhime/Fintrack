package com.example

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.data.model.CategoryEntity
import com.example.data.model.FilterSettings
import com.example.data.model.TransactionEntity
import com.example.ui.MainUiState
import com.example.ui.screens.TransactionsScreen
import com.example.ui.theme.FinTrackTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Focused regression test proving:
 * 1. Initial filter visibility is expanded.
 * 2. User toggles filters to hidden.
 * 3. Navigate away from Transactions (tab switch).
 * 4. Return to Transactions.
 * 5. Filter visibility is still hidden.
 * 6. Toggle again and verify it becomes expanded.
 * 7. Active filter count/badge behavior remains correct across navigation.
 * 8. Search and currency selector remain unaffected.
 * 9. State is owned by MainUiState (in-memory UI/session state) and NOT persisted to DataStore/Room/Firestore.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class TransactionsFilterStateSurvivalTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun sampleCategories() = listOf(
        CategoryEntity(id = "c1", householdId = "hh_test", name = "Housing", type = "Expense", subCategory = "Rent"),
        CategoryEntity(id = "c2", householdId = "hh_test", name = "Food & Dining", type = "Expense", subCategory = "Groceries")
    )

    private fun sampleTransactions() = listOf(
        TransactionEntity(
            id = "tx_1",
            householdId = "hh_test",
            description = "Groceries",
            amountRON = 150.0,
            amountEUR = 30.0,
            type = "Expense",
            category = "Food & Dining",
            subCategory = "Groceries",
            date = "2026-03-10",
            account = "Card",
            exchangeRate = 5.0,
            exchangeRateDate = "2026-03-10",
            conversionStatus = "OFFICIAL",
            exchangeRateSource = "BNR_OFFICIAL"
        )
    )

    @Test
    fun test01_filterVisibilitySurvivesTabNavigationLifecycle() {
        var uiState by mutableStateOf(MainUiState(selectedTab = 1, showTransactionFilters = true))

        composeTestRule.setContent {
            FinTrackTheme {
                when (uiState.selectedTab) {
                    0 -> {
                        // Dashboard placeholder
                        androidx.compose.material3.Text("Dashboard Screen")
                    }
                    1 -> {
                        TransactionsScreen(
                            transactions = sampleTransactions(),
                            categories = sampleCategories(),
                            filterSettings = FilterSettings(),
                            onCurrencyChanged = {},
                            onCategoryFilterSelected = { _, _ -> },
                            onSearchQueryChanged = {},
                            onAddTransactionClicked = {},
                            onDuplicateClicked = {},
                            onEditClicked = {},
                            onDeleteClicked = {},
                            showFilters = uiState.showTransactionFilters,
                            onToggleFilters = {
                                uiState = uiState.copy(showTransactionFilters = !uiState.showTransactionFilters)
                            }
                        )
                    }
                    2 -> {
                        androidx.compose.material3.Text("Analytics Screen")
                    }
                    3 -> {
                        androidx.compose.material3.Text("Settings Screen")
                    }
                }
            }
        }

        // 1. Initial filter visibility is expanded: button says "Hide filters", chips visible
        composeTestRule.onNodeWithTag("toggle_filters_button")
            .assertIsDisplayed()
            .assertTextContains("Hide filters")
        composeTestRule.onNodeWithText("All Categories").assertIsDisplayed()

        // 2. Search bar and currency toggle are displayed and unaffected
        composeTestRule.onNodeWithTag("search_transactions_input").assertIsDisplayed()
        composeTestRule.onNodeWithTag("currency_toggle_RON").assertIsDisplayed()

        // 3. User toggles filters to hidden
        composeTestRule.onNodeWithTag("toggle_filters_button").performClick()
        composeTestRule.waitForIdle()

        // Verify collapsed state in Transactions
        assertFalse(uiState.showTransactionFilters)
        composeTestRule.onNodeWithTag("toggle_filters_button").assertTextContains("Filters")
        composeTestRule.onNodeWithText("All Categories").assertDoesNotExist()

        // 4. Navigate away from Transactions to Dashboard (Tab 0)
        uiState = uiState.copy(selectedTab = 0)
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Dashboard Screen").assertIsDisplayed()
        composeTestRule.onNodeWithTag("toggle_filters_button").assertDoesNotExist()

        // 5. Navigate to Analytics (Tab 2)
        uiState = uiState.copy(selectedTab = 2)
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Analytics Screen").assertIsDisplayed()

        // 6. Return to Transactions (Tab 1)
        uiState = uiState.copy(selectedTab = 1)
        composeTestRule.waitForIdle()

        // 7. Filters MUST STILL BE COLLAPSED!
        composeTestRule.onNodeWithTag("toggle_filters_button")
            .assertIsDisplayed()
            .assertTextContains("Filters")
        composeTestRule.onNodeWithText("All Categories").assertDoesNotExist()

        // Search bar and currency toggle remain intact
        composeTestRule.onNodeWithTag("search_transactions_input").assertIsDisplayed()
        composeTestRule.onNodeWithTag("currency_toggle_RON").assertIsDisplayed()

        // 8. Toggle again and verify it becomes expanded
        composeTestRule.onNodeWithTag("toggle_filters_button").performClick()
        composeTestRule.waitForIdle()

        assertTrue(uiState.showTransactionFilters)
        composeTestRule.onNodeWithTag("toggle_filters_button").assertTextContains("Hide filters")
        composeTestRule.onNodeWithText("All Categories").assertIsDisplayed()
    }

    @Test
    fun test02_activeFilterCountBadgeSurvivesNavigationWhenCollapsed() {
        val activeFilters = FilterSettings(
            selectedType = "Expense",
            selectedExpenseCategory = "Food & Dining"
        )
        var uiState by mutableStateOf(MainUiState(selectedTab = 1, showTransactionFilters = true))

        composeTestRule.setContent {
            FinTrackTheme {
                when (uiState.selectedTab) {
                    0 -> androidx.compose.material3.Text("Dashboard Screen")
                    1 -> TransactionsScreen(
                        transactions = sampleTransactions(),
                        categories = sampleCategories(),
                        filterSettings = activeFilters,
                        onCurrencyChanged = {},
                        onCategoryFilterSelected = { _, _ -> },
                        onSearchQueryChanged = {},
                        onAddTransactionClicked = {},
                        onDuplicateClicked = {},
                        onEditClicked = {},
                        onDeleteClicked = {},
                        showFilters = uiState.showTransactionFilters,
                        onToggleFilters = {
                            uiState = uiState.copy(showTransactionFilters = !uiState.showTransactionFilters)
                        }
                    )
                }
            }
        }

        // Collapse filters
        composeTestRule.onNodeWithTag("toggle_filters_button").performClick()
        composeTestRule.waitForIdle()

        // Shows count badge: Filters • 2
        composeTestRule.onNodeWithTag("toggle_filters_button").assertTextContains("Filters • 2")

        // Navigate away to Dashboard
        uiState = uiState.copy(selectedTab = 0)
        composeTestRule.waitForIdle()

        // Return to Transactions
        uiState = uiState.copy(selectedTab = 1)
        composeTestRule.waitForIdle()

        // Still collapsed with exact count badge "Filters • 2"
        composeTestRule.onNodeWithTag("toggle_filters_button")
            .assertIsDisplayed()
            .assertTextContains("Filters • 2")
    }

    @Test
    fun test03_mainUiStateDefaultsAndSessionScopeVerification() {
        val defaultState = MainUiState()
        // Default must be true (expanded)
        assertTrue(defaultState.showTransactionFilters)

        val toggledState = defaultState.copy(showTransactionFilters = false)
        assertFalse(toggledState.showTransactionFilters)

        // Ensure showTransactionFilters is pure UI state and not in persistence schemas
        val hasDataStoreProperty = FilterSettings::class.java.declaredFields.any { it.name == "showTransactionFilters" }
        assertFalse("showTransactionFilters must NOT be in FilterSettings / persistent settings", hasDataStoreProperty)
    }
}
