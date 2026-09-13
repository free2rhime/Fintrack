package com.example

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import com.example.data.model.CategoryEntity
import com.example.data.model.FilterSettings
import com.example.data.model.HouseholdDto
import com.example.data.model.HouseholdMemberDto
import com.example.data.model.TransactionEntity
import com.example.ui.components.SubcategorySemanticComparator
import com.example.ui.components.TransactionFormDialog
import com.example.ui.components.extractSubcategorySemanticKey
import com.example.ui.navigation.BottomNavItem
import com.example.ui.navigation.FinTrackBottomNavigation
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SettingsSubView
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
 * Milestone Verification Test Suite: FinTrack UI/UX Refinement
 *
 * Verifies the 8 core features:
 * 1. Subcategory alphabetical sorting in TransactionFormDialog (case-insensitive)
 * 2. Floating "Jump to latest" pill appearance on scroll and return to top
 * 3. Collapsible filter chips section with dynamic button label and active filter count badge
 * 4. Row-level transaction delete confirmation dialog (Cancel vs Confirm)
 * 5. Absence of Currency selector in SettingsScreen
 * 6. Categories entry in SettingsScreen: OWNER access vs MEMBER restriction (UI + entry guard)
 * 7. Developer entry in SettingsScreen: OWNER access vs MEMBER restriction (UI + entry guard)
 * 8. 4-destination bottom navigation contract (0: Dashboard, 1: Transactions, 2: Analytics, 3: Settings)
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class UxRefinementMilestoneTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun sampleHousehold(ownerUid: String = "owner_123") = HouseholdDto(
        householdId = "hh_test",
        name = "Test Household",
        createdByUid = ownerUid,
        createdAt = 1700000000000L,
        updatedAt = 1700000000000L
    )

    private fun sampleOwnerMembership(uid: String = "owner_123") = HouseholdMemberDto(
        uid = uid,
        displayName = "Owner User",
        email = "owner@test.com",
        role = "OWNER",
        status = "ACTIVE",
        joinedAt = 1700000000000L
    )

    private fun sampleMemberMembership(uid: String = "member_456") = HouseholdMemberDto(
        uid = uid,
        displayName = "Member User",
        email = "member@test.com",
        role = "MEMBER",
        status = "ACTIVE",
        joinedAt = 1700001000000L
    )

    private fun sampleCategories() = listOf(
        CategoryEntity(id = "c1", householdId = "hh_test", name = "Housing", type = "Expense", subCategory = "Rent"),
        CategoryEntity(id = "c2", householdId = "hh_test", name = "Food & Dining", type = "Expense", subCategory = "Groceries"),
        CategoryEntity(id = "c3", householdId = "hh_test", name = "Auto & Transport", type = "Expense", subCategory = "Fuel"),
        CategoryEntity(id = "c4", householdId = "hh_test", name = "Utilities", type = "Expense", subCategory = "Electricity"),
        CategoryEntity(id = "c5", householdId = "hh_test", name = "Entertainment", type = "Expense", subCategory = "Cinema")
    )

    private fun generateTransactions(count: Int): List<TransactionEntity> {
        return (1..count).map { i ->
            TransactionEntity(
                id = "tx_$i",
                householdId = "hh_test",
                description = "Transaction $i",
                amountRON = 100.0 * i,
                amountEUR = 20.0 * i,
                type = "Expense",
                category = "Food & Dining",
                subCategory = "Groceries",
                date = "2026-03-09",
                account = "Checking",
                exchangeRate = 5.0,
                exchangeRateDate = "2026-03-09",
                conversionStatus = "OFFICIAL",
                exchangeRateSource = "BNR_OFFICIAL"
            )
        }
    }

    @Composable
    private fun RenderTransactionsScreen(
        transactions: List<TransactionEntity> = emptyList(),
        categories: List<CategoryEntity> = sampleCategories(),
        filterSettings: FilterSettings = FilterSettings(),
        onCurrencyChanged: (String) -> Unit = {},
        onCategoryFilterSelected: (String, String?) -> Unit = { _, _ -> },
        onSearchQueryChanged: (String) -> Unit = {},
        onAddTransactionClicked: () -> Unit = {},
        onDuplicateClicked: (TransactionEntity) -> Unit = {},
        onEditClicked: (TransactionEntity) -> Unit = {},
        onDeleteClicked: (TransactionEntity) -> Unit = {}
    ) {
        TransactionsScreen(
            transactions = transactions,
            categories = categories,
            filterSettings = filterSettings,
            onCurrencyChanged = onCurrencyChanged,
            onCategoryFilterSelected = onCategoryFilterSelected,
            onSearchQueryChanged = onSearchQueryChanged,
            onAddTransactionClicked = onAddTransactionClicked,
            onDuplicateClicked = onDuplicateClicked,
            onEditClicked = onEditClicked,
            onDeleteClicked = onDeleteClicked
        )
    }

    @Composable
    private fun RenderSettingsScreen(
        filterSettings: FilterSettings = FilterSettings(),
        themeMode: String = "system",
        currentUid: String = "owner_123",
        currentUserEmail: String = "owner@test.com",
        currentHousehold: HouseholdDto? = null,
        currentUserMembership: HouseholdMemberDto? = null,
        categories: List<CategoryEntity> = emptyList(),
        initialSubView: SettingsSubView = SettingsSubView.ROOT,
        onExportCsv: () -> Unit = {},
        onThemeModeChanged: (String) -> Unit = {}
    ) {
        SettingsScreen(
            filterSettings = filterSettings,
            themeMode = themeMode,
            currentUid = currentUid,
            currentUserEmail = currentUserEmail,
            currentHousehold = currentHousehold,
            currentUserMembership = currentUserMembership,
            categories = categories,
            initialSubView = initialSubView,
            onThemeModeChanged = onThemeModeChanged,
            onExportCsv = onExportCsv
        )
    }

    // =========================================================================
    // STEP 1: SUBCATEGORY ALPHABETICAL SORTING
    // =========================================================================

    @Test
    fun test01_subcategoriesAreSortedAlphabeticallyInFormDialog() {
        val unsortedCategories = listOf(
            CategoryEntity(id = "c1", householdId = "hh_test", name = "Misc", type = "Expense", subCategory = "🌴 Vacanta"),
            CategoryEntity(id = "c2", householdId = "hh_test", name = "Misc", type = "Expense", subCategory = "⚡ Electricitate"),
            CategoryEntity(id = "c3", householdId = "hh_test", name = "Misc", type = "Expense", subCategory = "🍉 Alimente"),
            CategoryEntity(id = "c4", householdId = "hh_test", name = "Misc", type = "Expense", subCategory = "🛠️ Altele home"),
            CategoryEntity(id = "c5", householdId = "hh_test", name = "Misc", type = "Expense", subCategory = "🅿️ Parcari"),
            CategoryEntity(id = "c6", householdId = "hh_test", name = "Misc", type = "Expense", subCategory = "⛽ Gaz")
        )

        composeTestRule.setContent {
            FinTrackTheme {
                TransactionFormDialog(
                    initialTransaction = null,
                    isDuplicateMode = false,
                    categories = unsortedCategories,
                    onDismiss = {},
                    onSave = { _, _, _, _, _, _, _, _, _ -> }
                )
            }
        }

        // Open subcategory dropdown
        composeTestRule.onNodeWithText("Subcategory (Select First)")
            .performScrollTo()
            .performClick()
        composeTestRule.waitForIdle()

        // All items should be present with emoji displayed
        composeTestRule.onNodeWithText("🍉 Alimente").assertIsDisplayed()
        composeTestRule.onNodeWithText("🛠️ Altele home").assertIsDisplayed()
        composeTestRule.onNodeWithText("⚡ Electricitate").assertIsDisplayed()
        composeTestRule.onNodeWithText("⛽ Gaz").assertIsDisplayed()
        composeTestRule.onNodeWithText("🅿️ Parcari").assertIsDisplayed()
        composeTestRule.onNodeWithText("🌴 Vacanta").assertIsDisplayed()

        // Selection works and keeps stored value intact
        composeTestRule.onNodeWithText("🍉 Alimente").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("🍉 Alimente").assertIsDisplayed()
    }

    @Test
    fun test01b_emojiSubcategorySemanticSortingExpense() {
        val unsortedExpenseSubcategories = listOf(
            "🌴 Vacanta",
            "⚡ Electricitate",
            "🍉 Alimente",
            "🛠️ Altele home",
            "🅿️ Parcari",
            "⛽ Gaz"
        )

        val sorted = unsortedExpenseSubcategories.sortedWith(SubcategorySemanticComparator)

        val expectedExpenseOrder = listOf(
            "🍉 Alimente",
            "🛠️ Altele home",
            "⚡ Electricitate",
            "⛽ Gaz",
            "🅿️ Parcari",
            "🌴 Vacanta"
        )

        assertEquals(expectedExpenseOrder, sorted)
    }

    @Test
    fun test01c_emojiSubcategorySemanticSortingIncome() {
        val unsortedIncomeSubcategories = listOf(
            "🎂 Zi de nastere/Altele",
            "💵 Venit Lunar",
            "💵 Prime",
            "💵 Tichete de masa"
        )

        val sorted = unsortedIncomeSubcategories.sortedWith(SubcategorySemanticComparator)

        val expectedIncomeOrder = listOf(
            "💵 Prime",
            "💵 Tichete de masa",
            "💵 Venit Lunar",
            "🎂 Zi de nastere/Altele"
        )

        assertEquals(expectedIncomeOrder, sorted)
    }

    @Test
    fun test01d_emojiAndPlainTextMixedCaseInsensitiveSorting() {
        val mixed = listOf(
            "groceries",
            "🍉 Alimente",
            "Bakery",
            "⚡ Electricitate"
        )

        val sorted = mixed.sortedWith(SubcategorySemanticComparator)

        val expected = listOf(
            "🍉 Alimente",
            "Bakery",
            "⚡ Electricitate",
            "groceries"
        )

        assertEquals(expected, sorted)
    }

    // =========================================================================
    // STEP 2: JUMP-TO-LATEST FLOATING PILL
    // =========================================================================

    @Test
    fun test02_jumpToLatestPillAppearsOnScrollAndScrollsToTop() {
        val transactions = generateTransactions(25)

        composeTestRule.setContent {
            FinTrackTheme {
                RenderTransactionsScreen(
                    transactions = transactions,
                    categories = sampleCategories(),
                    filterSettings = FilterSettings()
                )
            }
        }

        // Initially at top: pill should NOT exist
        composeTestRule.onNodeWithTag("btn_jump_to_latest").assertDoesNotExist()

        // Scroll down to an item past index 3
        composeTestRule.onNodeWithTag("transaction_item_tx_12").performScrollTo()
        composeTestRule.waitForIdle()

        // Pill should now be visible and meet touch target
        composeTestRule.onNodeWithTag("btn_jump_to_latest")
            .assertIsDisplayed()
            .assertHeightIsAtLeast(48.dp)
            .assert(hasClickAction())

        // Click the jump to latest pill
        composeTestRule.onNodeWithTag("btn_jump_to_latest").performClick()
        composeTestRule.waitForIdle()

        // First item should be visible again
        composeTestRule.onNodeWithTag("transaction_item_tx_1").assertIsDisplayed()
    }

    // =========================================================================
    // STEP 3: COLLAPSIBLE FILTER CHIPS & ACTIVE COUNT BADGE
    // =========================================================================

    @Test
    fun test03_filterToggleCollapseExpandAndBadgeCount() {
        composeTestRule.setContent {
            FinTrackTheme {
                RenderTransactionsScreen(
                    transactions = generateTransactions(5),
                    categories = sampleCategories(),
                    filterSettings = FilterSettings()
                )
            }
        }

        // Initially, filters are expanded: button shows "Hide filters"
        composeTestRule.onNodeWithTag("toggle_filters_button")
            .assertIsDisplayed()
            .assertHeightIsAtLeast(48.dp)
            .assertTextContains("Hide filters")

        // Filter chips container is displayed
        composeTestRule.onNodeWithText("All Categories").assertIsDisplayed()

        // Collapse filters
        composeTestRule.onNodeWithTag("toggle_filters_button").performClick()
        composeTestRule.waitForIdle()

        // Button text should now be "Filters" (no active filters)
        composeTestRule.onNodeWithTag("toggle_filters_button")
            .assertTextContains("Filters")

        // Filter chips are collapsed / not visible
        composeTestRule.onNodeWithText("All Categories").assertDoesNotExist()

        // Re-expand filters
        composeTestRule.onNodeWithTag("toggle_filters_button").performClick()
        composeTestRule.waitForIdle()

        // Button text is back to "Hide filters" and chips are displayed
        composeTestRule.onNodeWithTag("toggle_filters_button").assertTextContains("Hide filters")
        composeTestRule.onNodeWithText("All Categories").assertIsDisplayed()
    }

    @Test
    fun test03b_filterToggleWithActiveFiltersShowsCountBadge() {
        val activeFilterSettings = FilterSettings(
            selectedType = "Expense",
            selectedExpenseCategory = "Food & Dining"
        )

        composeTestRule.setContent {
            FinTrackTheme {
                RenderTransactionsScreen(
                    transactions = generateTransactions(5),
                    categories = sampleCategories(),
                    filterSettings = activeFilterSettings
                )
            }
        }

        // Collapse filters
        composeTestRule.onNodeWithTag("toggle_filters_button").performClick()
        composeTestRule.waitForIdle()

        // Button should show "Filters • 2" badge
        composeTestRule.onNodeWithTag("toggle_filters_button")
            .assertTextContains("Filters • 2")
    }

    // =========================================================================
    // STEP 4: ROW-LEVEL DELETE CONFIRMATION DIALOG
    // =========================================================================

    @Test
    fun test04_deleteConfirmationDialogCancelDismissesWithoutDeleting() {
        var deletedTx: TransactionEntity? = null
        val transactions = generateTransactions(3)

        composeTestRule.setContent {
            FinTrackTheme {
                RenderTransactionsScreen(
                    transactions = transactions,
                    categories = sampleCategories(),
                    filterSettings = FilterSettings(),
                    onDeleteClicked = { deletedTx = it }
                )
            }
        }

        // Dialog should not be visible initially
        composeTestRule.onNodeWithTag("delete_transaction_dialog").assertDoesNotExist()

        // Click delete on first transaction
        composeTestRule.onNodeWithTag("tx_delete_tx_1").performClick()
        composeTestRule.waitForIdle()

        // Dialog is displayed with confirm and cancel buttons
        composeTestRule.onNodeWithTag("delete_transaction_dialog").assertIsDisplayed()
        composeTestRule.onNodeWithTag("cancel_delete_transaction_button")
            .assertIsDisplayed()
            .assertHeightIsAtLeast(48.dp)
        composeTestRule.onNodeWithTag("confirm_delete_transaction_button")
            .assertIsDisplayed()
            .assertHeightIsAtLeast(48.dp)

        // Click Cancel
        composeTestRule.onNodeWithTag("cancel_delete_transaction_button").performClick()
        composeTestRule.waitForIdle()

        // Dialog is dismissed, onDeleteClicked was NOT called
        composeTestRule.onNodeWithTag("delete_transaction_dialog").assertDoesNotExist()
        assertEquals(null, deletedTx)
    }

    @Test
    fun test04b_deleteConfirmationDialogConfirmInvokesCallback() {
        var deletedTx: TransactionEntity? = null
        val transactions = generateTransactions(3)

        composeTestRule.setContent {
            FinTrackTheme {
                RenderTransactionsScreen(
                    transactions = transactions,
                    categories = sampleCategories(),
                    filterSettings = FilterSettings(),
                    onDeleteClicked = { deletedTx = it }
                )
            }
        }

        // Click delete on tx_1
        composeTestRule.onNodeWithTag("tx_delete_tx_1").performClick()
        composeTestRule.waitForIdle()

        // Click Confirm Delete
        composeTestRule.onNodeWithTag("confirm_delete_transaction_button").performClick()
        composeTestRule.waitForIdle()

        // Dialog dismissed and callback invoked with tx_1
        composeTestRule.onNodeWithTag("delete_transaction_dialog").assertDoesNotExist()
        assertEquals("tx_1", deletedTx?.id)
    }

    // =========================================================================
    // STEP 5: SETTINGS CURRENCY REMOVAL
    // =========================================================================

    @Test
    fun test05_settingsScreenDoesNotContainDisplayCurrencySelector() {
        composeTestRule.setContent {
            FinTrackTheme {
                RenderSettingsScreen(
                    filterSettings = FilterSettings(selectedCurrency = "RON"),
                    themeMode = "system",
                    currentUid = "owner_123",
                    currentUserEmail = "owner@test.com"
                )
            }
        }

        // "Display Currency" must not exist anywhere on the screen
        composeTestRule.onNodeWithText("Display Currency").assertDoesNotExist()
        composeTestRule.onNodeWithTag("currency_selector").assertDoesNotExist()
    }

    // =========================================================================
    // STEP 6: CATEGORIES SUB-VIEW & OWNER PROTECTION
    // =========================================================================

    @Test
    fun test06_categoriesAccessibleForOwnerAndNavigatesBack() {
        composeTestRule.setContent {
            FinTrackTheme {
                RenderSettingsScreen(
                    filterSettings = FilterSettings(),
                    currentUid = "owner_123",
                    currentUserEmail = "owner@test.com",
                    currentHousehold = sampleHousehold("owner_123"),
                    currentUserMembership = sampleOwnerMembership("owner_123"),
                    categories = sampleCategories()
                )
            }
        }

        // Categories management entry is displayed for OWNER
        composeTestRule.onNodeWithTag("settings_item_categories")
            .performScrollTo()
            .assertIsDisplayed()
            .assertHeightIsAtLeast(48.dp)

        // Click to enter Categories subview
        composeTestRule.onNodeWithTag("settings_item_categories").performClick()
        composeTestRule.waitForIdle()

        // Header back button is displayed in Categories subview
        composeTestRule.onNodeWithTag("btn_categories_back")
            .assertIsDisplayed()
            .assertHeightIsAtLeast(48.dp)
        composeTestRule.onNodeWithText("Categories & Subcategories").assertIsDisplayed()

        // Click back button
        composeTestRule.onNodeWithTag("btn_categories_back").performClick()
        composeTestRule.waitForIdle()

        // Returned to Settings ROOT
        composeTestRule.onNodeWithTag("settings_item_categories").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun test06b_categoriesHiddenAndBlockedForMember() {
        composeTestRule.setContent {
            FinTrackTheme {
                RenderSettingsScreen(
                    filterSettings = FilterSettings(),
                    currentUid = "member_456",
                    currentUserEmail = "member@test.com",
                    currentHousehold = sampleHousehold("owner_123"),
                    currentUserMembership = sampleMemberMembership("member_456"),
                    initialSubView = SettingsSubView.CATEGORIES // Attempt direct entry
                )
            }
        }

        // Categories item must NOT exist for Member
        composeTestRule.onNodeWithTag("settings_item_categories").assertDoesNotExist()

        // Entry guard forces currentSubView back to ROOT immediately
        composeTestRule.onNodeWithTag("btn_categories_back").assertDoesNotExist()
        composeTestRule.onNodeWithTag("account_info_card").assertIsDisplayed()
    }

    // =========================================================================
    // STEP 7: DEVELOPER SETTINGS & OWNER PROTECTION
    // =========================================================================

    @Test
    fun test07_developerSettingsAccessibleForOwnerAndNavigatesBack() {
        var exportTriggered = false

        composeTestRule.setContent {
            FinTrackTheme {
                RenderSettingsScreen(
                    filterSettings = FilterSettings(),
                    currentUid = "owner_123",
                    currentUserEmail = "owner@test.com",
                    currentHousehold = sampleHousehold("owner_123"),
                    currentUserMembership = sampleOwnerMembership("owner_123"),
                    onExportCsv = { exportTriggered = true }
                )
            }
        }

        // Developer item is displayed for OWNER
        composeTestRule.onNodeWithTag("settings_item_developer")
            .performScrollTo()
            .assertIsDisplayed()
            .assertHeightIsAtLeast(48.dp)

        // Click to enter Developer subview
        composeTestRule.onNodeWithTag("settings_item_developer").performClick()
        composeTestRule.waitForIdle()

        // Developer header and back button are displayed
        composeTestRule.onNodeWithTag("btn_developer_back")
            .assertIsDisplayed()
            .assertHeightIsAtLeast(48.dp)
        composeTestRule.onNodeWithText("Developer Settings").assertIsDisplayed()

        // Developer features are accessible
        composeTestRule.onNodeWithTag("export_csv_button").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("import_csv_button").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("retry_eur_conversions_button").performScrollTo().assertIsDisplayed()

        // Action executes
        composeTestRule.onNodeWithTag("export_csv_button").performClick()
        assertTrue(exportTriggered)

        // Click back button
        composeTestRule.onNodeWithTag("btn_developer_back").performScrollTo().performClick()
        composeTestRule.waitForIdle()

        // Returned to Settings ROOT
        composeTestRule.onNodeWithTag("settings_item_developer").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun test07b_developerSettingsHiddenAndBlockedForMember() {
        composeTestRule.setContent {
            FinTrackTheme {
                RenderSettingsScreen(
                    filterSettings = FilterSettings(),
                    currentUid = "member_456",
                    currentUserEmail = "member@test.com",
                    currentHousehold = sampleHousehold("owner_123"),
                    currentUserMembership = sampleMemberMembership("member_456"),
                    initialSubView = SettingsSubView.DEVELOPER // Attempt direct entry
                )
            }
        }

        // Developer item must NOT exist for Member
        composeTestRule.onNodeWithTag("settings_item_developer").assertDoesNotExist()

        // Entry guard forces currentSubView back to ROOT immediately
        composeTestRule.onNodeWithTag("btn_developer_back").assertDoesNotExist()
        composeTestRule.onNodeWithTag("account_info_card").assertIsDisplayed()
    }

    // =========================================================================
    // STEP 8: BOTTOM NAVIGATION 4-DESTINATION CONTRACT
    // =========================================================================

    @Test
    fun test08_bottomNavigationContainsExactlyFourDestinations() {
        val items = BottomNavItem.values()
        assertEquals(4, items.size)
        assertEquals("Dashboard", items[0].title)
        assertEquals("Transactions", items[1].title)
        assertEquals("Analytics", items[2].title)
        assertEquals("Settings", items[3].title)

        var selectedTab = 0
        composeTestRule.setContent {
            FinTrackTheme {
                FinTrackBottomNavigation(
                    selectedTabIndex = selectedTab,
                    onTabSelected = { selectedTab = it }
                )
            }
        }

        // Verify the 4 tags exist and Categories does NOT exist
        composeTestRule.onNodeWithTag("bottom_nav_dashboard").assertIsDisplayed()
        composeTestRule.onNodeWithTag("bottom_nav_transactions").assertIsDisplayed()
        composeTestRule.onNodeWithTag("bottom_nav_analytics").assertIsDisplayed()
        composeTestRule.onNodeWithTag("bottom_nav_settings").assertIsDisplayed()
        composeTestRule.onNodeWithTag("bottom_nav_categories").assertDoesNotExist()

        // Tab selection works
        composeTestRule.onNodeWithTag("bottom_nav_settings").performClick()
        assertEquals(3, selectedTab)
    }
}
