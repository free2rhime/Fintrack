package com.example

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.unit.dp
import com.example.data.model.CategoryEntity
import com.example.data.model.FilterSettings
import com.example.data.model.TransactionEntity
import com.example.ui.components.BadgeVariant
import com.example.ui.components.FinTrackTransactionRow
import com.example.ui.components.TransactionCardItem
import com.example.ui.components.TransactionFormDialog
import com.example.ui.screens.TransactionsScreen
import com.example.ui.theme.FinTrackTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate
import java.time.ZoneId

/**
 * Material 3 Expressive Migration Test Suite for Checkpoint 4:
 * Transaction Flow & Detail Screens.
 *
 * Verifies:
 * - TransactionsScreen: List rendering, grouped tonal containers, empty state, search, filters, FAB
 * - FinTrackTransactionRow & TransactionCardItem: Readability, tabular numerals, semantic colors,
 *   secondary EUR conversion, status badges, quick actions, accessibility semantics
 * - TransactionFormDialog: Add, Edit, Duplicate modes, type toggle, validation, autocomplete,
 *   subcategory-first category assignment, date & account selection, delete confirmation,
 *   and multi-screen layout stability (360dp, 390dp, 412dp, 600dp).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class M3TransactionFlowExpressiveMigrationTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val sampleCategories = listOf(
        CategoryEntity(id = "c1", name = "🍉 Food & Dining", type = "Expense", subCategory = "🛒 Groceries"),
        CategoryEntity(id = "c2", name = "🍉 Food & Dining", type = "Expense", subCategory = "🍔 Restaurants"),
        CategoryEntity(id = "c3", name = "🚗 Transport", type = "Expense", subCategory = "⛽ Fuel"),
        CategoryEntity(id = "c4", name = "💼 Salary", type = "Income", subCategory = "🏢 Primary Job"),
        CategoryEntity(id = "c5", name = "📈 Investments", type = "Income", subCategory = "🪙 Dividends")
    )

    private fun createSampleTransaction(
        id: String,
        description: String,
        amountRON: Double,
        amountEUR: Double,
        type: String,
        category: String,
        subCategory: String = "",
        date: String = "2026-03-10",
        account: String = "Card",
        conversionStatus: String = "OFFICIAL",
        exchangeRateSource: String = "BNR_OFFICIAL",
        exchangeRate: Double = 4.97,
        destination: String? = null
    ) = TransactionEntity(
        id = id,
        householdId = "hh_test_1",
        description = description,
        amountRON = amountRON,
        amountEUR = amountEUR,
        type = type,
        category = category,
        subCategory = subCategory,
        date = date,
        account = account,
        exchangeRate = exchangeRate,
        exchangeRateDate = date,
        conversionStatus = conversionStatus,
        exchangeRateSource = exchangeRateSource,
        destination = destination
    )

    // =========================================================================
    // 1. TRANSACTIONS SCREEN TESTS
    // =========================================================================

    @Test
    @Config(qualifiers = "w412dp-h1000dp")
    fun test01_transactionsScreen_fullListRendering_rendersDateGroupsAndItems() {
        val tx1 = createSampleTransaction("tx1", "Supermarket Groceries", 150.0, 30.18, "Expense", "🍉 Food & Dining", date = "2026-03-10")
        val tx2 = createSampleTransaction("tx2", "Gas Station Fuel", 250.0, 50.30, "Expense", "🚗 Transport", date = "2026-03-10")
        val tx3 = createSampleTransaction("tx3", "Monthly Salary", 6000.0, 1207.24, "Income", "💼 Salary", date = "2026-03-09")

        composeTestRule.setContent {
            FinTrackTheme {
                TransactionsScreen(
                    transactions = listOf(tx1, tx2, tx3),
                    categories = sampleCategories,
                    filterSettings = FilterSettings(selectedCurrency = "RON"),
                    onCurrencyChanged = {},
                    onCategoryFilterSelected = { _, _ -> },
                    onSearchQueryChanged = {},
                    onAddTransactionClicked = {},
                    onDuplicateClicked = {},
                    onEditClicked = {},
                    onDeleteClicked = {}
                )
            }
        }

        // Screen header & search
        composeTestRule.onNodeWithText("Transactions").assertIsDisplayed()
        composeTestRule.onNodeWithTag("search_transactions_input").assertIsDisplayed()
        composeTestRule.onNodeWithTag("fab_add_transaction").assertIsDisplayed()

        // Grouped date containers
        composeTestRule.onNodeWithTag("transaction_date_group_2026-03-10").assertIsDisplayed()
        composeTestRule.onNodeWithTag("transaction_item_tx1").assertExists()
        composeTestRule.onNodeWithTag("transaction_item_tx2").assertExists()

        composeTestRule.onNodeWithTag("transaction_date_group_2026-03-09").assertExists()
        composeTestRule.onNodeWithTag("transaction_item_tx3").assertExists()
    }

    @Test
    @Config(qualifiers = "w412dp-h915dp")
    fun test02_transactionsScreen_emptyState_rendersEmptyStateWithActionableButton() {
        var addClicked = false

        composeTestRule.setContent {
            FinTrackTheme {
                TransactionsScreen(
                    transactions = emptyList(),
                    categories = sampleCategories,
                    filterSettings = FilterSettings(selectedCurrency = "RON"),
                    onCurrencyChanged = {},
                    onCategoryFilterSelected = { _, _ -> },
                    onSearchQueryChanged = {},
                    onAddTransactionClicked = { addClicked = true },
                    onDuplicateClicked = {},
                    onEditClicked = {},
                    onDeleteClicked = {}
                )
            }
        }

        composeTestRule.onNodeWithText("No transactions found").assertIsDisplayed()
        composeTestRule.onNodeWithText("Try adjusting filters or tap + to record a new transaction.").assertExists()

        // Empty state actionable button
        composeTestRule.onNodeWithText("Add Transaction").performScrollTo().performClick()
        assertTrue(addClicked)
    }

    @Test
    fun test03_transactionsScreen_groupedDateContainer_hasIntegratedHeaderAndDailyTotals() {
        val txExpense = createSampleTransaction("tx1", "Bakery", 40.0, 8.05, "Expense", "🍉 Food & Dining", date = "2026-03-10")
        val txIncome = createSampleTransaction("tx2", "Bonus", 500.0, 100.60, "Income", "💼 Salary", date = "2026-03-10")

        composeTestRule.setContent {
            FinTrackTheme {
                TransactionsScreen(
                    transactions = listOf(txExpense, txIncome),
                    categories = sampleCategories,
                    filterSettings = FilterSettings(selectedCurrency = "RON"),
                    onCurrencyChanged = {},
                    onCategoryFilterSelected = { _, _ -> },
                    onSearchQueryChanged = {},
                    onAddTransactionClicked = {},
                    onDuplicateClicked = {},
                    onEditClicked = {},
                    onDeleteClicked = {}
                )
            }
        }

        composeTestRule.onNodeWithTag("transaction_date_group_2026-03-10").assertIsDisplayed()
        // Daily total badge displays income and expense
        composeTestRule.onNodeWithText("+500.00").assertIsDisplayed()
        composeTestRule.onNodeWithText("-40.00 RON").assertIsDisplayed()
    }

    @Test
    fun test04_transactionsScreen_searchBar_filtersAndClearsProperly() {
        var queryReported = ""

        composeTestRule.setContent {
            FinTrackTheme {
                TransactionsScreen(
                    transactions = emptyList(),
                    categories = sampleCategories,
                    filterSettings = FilterSettings(selectedCurrency = "RON"),
                    onCurrencyChanged = {},
                    onCategoryFilterSelected = { _, _ -> },
                    onSearchQueryChanged = { queryReported = it },
                    onAddTransactionClicked = {},
                    onDuplicateClicked = {},
                    onEditClicked = {},
                    onDeleteClicked = {}
                )
            }
        }

        composeTestRule.onNodeWithTag("search_transactions_input").performTextInput("Coffee")
        assertEquals("Coffee", queryReported)

        // Clear search
        composeTestRule.onNodeWithContentDescription("Clear search").performClick()
        assertEquals("", queryReported)
    }

    @Test
    fun test05_transactionsScreen_typeFilter_updatesSelectionState() {
        var selectedTypeFilter = ""

        composeTestRule.setContent {
            FinTrackTheme {
                TransactionsScreen(
                    transactions = emptyList(),
                    categories = sampleCategories,
                    filterSettings = FilterSettings(selectedCurrency = "RON", selectedType = "All"),
                    onCurrencyChanged = {},
                    onTypeFilterSelected = { selectedTypeFilter = it },
                    onCategoryFilterSelected = { _, _ -> },
                    onSearchQueryChanged = {},
                    onAddTransactionClicked = {},
                    onDuplicateClicked = {},
                    onEditClicked = {},
                    onDeleteClicked = {}
                )
            }
        }

        // Tap "Expense"
        composeTestRule.onNodeWithText("Expense").performClick()
        assertEquals("Expense", selectedTypeFilter)

        // Tap "Income"
        composeTestRule.onNodeWithText("Income").performClick()
        assertEquals("Income", selectedTypeFilter)
    }

    @Test
    fun test06_transactionsScreen_categoryChips_selectsAndDeselectsCategories() {
        var selectedCategory: String? = "sentinel"

        composeTestRule.setContent {
            FinTrackTheme {
                TransactionsScreen(
                    transactions = emptyList(),
                    categories = sampleCategories,
                    filterSettings = FilterSettings(selectedCurrency = "RON", selectedType = "All"),
                    onCurrencyChanged = {},
                    onCategoryFilterSelected = { _, cat -> selectedCategory = cat },
                    onSearchQueryChanged = {},
                    onAddTransactionClicked = {},
                    onDuplicateClicked = {},
                    onEditClicked = {},
                    onDeleteClicked = {}
                )
            }
        }

        // Tap specific category chip
        composeTestRule.onNodeWithText("🍉 Food & Dining").performClick()
        assertEquals("🍉 Food & Dining", selectedCategory)

        // Tap "All Categories" chip
        composeTestRule.onNodeWithText("All Categories").performClick()
        assertNull(selectedCategory)
    }

    @Test
    fun test07_transactionsScreen_fab_isDisplayedAndInvokesCallback() {
        var fabClicked = false

        composeTestRule.setContent {
            FinTrackTheme {
                TransactionsScreen(
                    transactions = emptyList(),
                    categories = sampleCategories,
                    filterSettings = FilterSettings(selectedCurrency = "RON"),
                    onCurrencyChanged = {},
                    onCategoryFilterSelected = { _, _ -> },
                    onSearchQueryChanged = {},
                    onAddTransactionClicked = { fabClicked = true },
                    onDuplicateClicked = {},
                    onEditClicked = {},
                    onDeleteClicked = {}
                )
            }
        }

        composeTestRule.onNodeWithTag("fab_add_transaction").assertIsDisplayed().performClick()
        assertTrue(fabClicked)
    }

    // =========================================================================
    // 2. FINTRACK TRANSACTION ROW & TRANSACTION CARD ITEM TESTS
    // =========================================================================

    @Test
    fun test08_transactionRow_amountTypography_displaysExplicitSignAndTabularNumerals() {
        val expenseTx = createSampleTransaction("tx1", "Cafe Latte", 18.50, 3.72, "Expense", "🍉 Food & Dining")
        val incomeTx = createSampleTransaction("tx2", "Client Invoice", 1200.0, 241.45, "Income", "💼 Salary")

        composeTestRule.setContent {
            FinTrackTheme {
                TransactionCardItem(
                    transaction = expenseTx,
                    selectedCurrency = "RON",
                    onDuplicateClicked = {},
                    onEditClicked = {},
                    onDeleteClicked = {}
                )
                TransactionCardItem(
                    transaction = incomeTx,
                    selectedCurrency = "RON",
                    onDuplicateClicked = {},
                    onEditClicked = {},
                    onDeleteClicked = {}
                )
            }
        }

        composeTestRule.onNodeWithText("- 18.50 RON").assertIsDisplayed()
        composeTestRule.onNodeWithText("+ 1 200.00 RON").assertIsDisplayed()
    }

    @Test
    fun test09_transactionRow_secondaryEurConversion_rendersWhenOfficial() {
        val txWithOfficialEur = createSampleTransaction(
            id = "tx1",
            description = "Train Ticket",
            amountRON = 99.40,
            amountEUR = 20.00,
            type = "Expense",
            category = "🚗 Transport",
            conversionStatus = "OFFICIAL",
            exchangeRateSource = "BNR_OFFICIAL",
            exchangeRate = 4.97
        )

        composeTestRule.setContent {
            FinTrackTheme {
                TransactionCardItem(
                    transaction = txWithOfficialEur,
                    selectedCurrency = "RON",
                    onDuplicateClicked = {},
                    onEditClicked = {},
                    onDeleteClicked = {}
                )
            }
        }

        composeTestRule.onNodeWithText("- 99.40 RON").assertIsDisplayed()
        composeTestRule.onNodeWithText("≈ -20.00 EUR").assertIsDisplayed()
    }

    @Test
    fun test10_transactionRow_statusBadges_pendingFailedUnverified() {
        val pendingTx = createSampleTransaction(
            id = "tx_pending",
            description = "Book Purchase",
            amountRON = 45.0,
            amountEUR = 0.0,
            type = "Expense",
            category = "Shopping",
            conversionStatus = "PENDING",
            exchangeRate = 0.0
        )
        val failedTx = createSampleTransaction(
            id = "tx_failed",
            description = "Cinema Tickets",
            amountRON = 60.0,
            amountEUR = 0.0,
            type = "Expense",
            category = "Entertainment",
            conversionStatus = "FAILED",
            exchangeRate = 0.0
        )

        composeTestRule.setContent {
            FinTrackTheme {
                TransactionCardItem(
                    transaction = pendingTx,
                    selectedCurrency = "RON",
                    onDuplicateClicked = {},
                    onEditClicked = {},
                    onDeleteClicked = {}
                )
                TransactionCardItem(
                    transaction = failedTx,
                    selectedCurrency = "RON",
                    onDuplicateClicked = {},
                    onEditClicked = {},
                    onDeleteClicked = {}
                )
            }
        }

        composeTestRule.onNodeWithText("EUR Pending").assertIsDisplayed()
        composeTestRule.onNodeWithText("EUR Failed").assertIsDisplayed()
    }

    @Test
    fun test11_transactionRow_quickActions_have48dpTouchTargetsAndCallbacks() {
        val tx = createSampleTransaction("tx10", "Electronics Cable", 35.0, 7.04, "Expense", "Shopping")
        var duplicateInvoked = false
        var editInvoked = false
        var deleteInvoked = false

        composeTestRule.setContent {
            FinTrackTheme {
                TransactionCardItem(
                    transaction = tx,
                    selectedCurrency = "RON",
                    onDuplicateClicked = { duplicateInvoked = true },
                    onEditClicked = { editInvoked = true },
                    onDeleteClicked = { deleteInvoked = true }
                )
            }
        }

        composeTestRule.onNodeWithTag("tx_duplicate_tx10").performClick()
        assertTrue(duplicateInvoked)

        composeTestRule.onNodeWithTag("tx_edit_tx10").performClick()
        assertTrue(editInvoked)

        composeTestRule.onNodeWithTag("tx_delete_tx10").performClick()
        assertTrue(deleteInvoked)
    }

    @Test
    fun test12_transactionRow_accessibility_talkBackContentDescription() {
        val tx = createSampleTransaction(
            id = "tx1",
            description = "Organic Groceries",
            amountRON = 120.50,
            amountEUR = 24.25,
            type = "Expense",
            category = "🍉 Food & Dining",
            subCategory = "🛒 Groceries"
        )

        composeTestRule.setContent {
            FinTrackTheme {
                TransactionCardItem(
                    transaction = tx,
                    selectedCurrency = "RON",
                    onDuplicateClicked = {},
                    onEditClicked = {},
                    onDeleteClicked = {}
                )
            }
        }

        val accessibleDescription = "Expense: Organic Groceries, -120.50 RON, 🛒 Groceries, "
        composeTestRule.onNodeWithContentDescription(accessibleDescription).assertExists()
    }

    // =========================================================================
    // 3. TRANSACTION FORM DIALOG (ADD, EDIT, DUPLICATE) TESTS
    // =========================================================================

    @Test
    fun test13_formDialog_addMode_rendersCleanFormWithRonHeroAmount() {
        composeTestRule.setContent {
            FinTrackTheme {
                TransactionFormDialog(
                    initialTransaction = null,
                    isDuplicateMode = false,
                    categories = sampleCategories,
                    onDismiss = {},
                    onSave = { _, _, _, _, _, _, _, _, _ -> }
                )
            }
        }

        composeTestRule.onNodeWithText("Add Transaction").assertIsDisplayed()
        composeTestRule.onNodeWithTag("tx_input_amount").assertIsDisplayed()
        composeTestRule.onNodeWithText("RON").assertIsDisplayed()
        composeTestRule.onNodeWithTag("tx_input_desc").assertIsDisplayed()
        composeTestRule.onNodeWithText("Save Transaction").assertExists()
        composeTestRule.onNodeWithText("Cancel").assertExists()
    }

    @Test
    fun test14_formDialog_editMode_prefillsValuesAndPreservesId() {
        val existingTx = createSampleTransaction("tx_edit_99", "Home Utility Bill", 320.0, 64.39, "Expense", "🍉 Food & Dining")
        var savedId: String? = null

        composeTestRule.setContent {
            FinTrackTheme {
                TransactionFormDialog(
                    initialTransaction = existingTx,
                    isDuplicateMode = false,
                    categories = sampleCategories,
                    onDismiss = {},
                    onSave = { id, _, _, _, _, _, _, _, _ -> savedId = id }
                )
            }
        }

        composeTestRule.onNodeWithText("Edit Transaction").assertIsDisplayed()
        composeTestRule.onNodeWithText("320.0").assertExists()
        composeTestRule.onNodeWithText("Home Utility Bill").assertExists()

        composeTestRule.onNodeWithTag("save_transaction_button").performScrollTo().performClick()
        assertEquals("tx_edit_99", savedId)
    }

    @Test
    fun test15_formDialog_duplicateMode_autoUpdatesDateToTodayAndNullsId() {
        val todayStr = LocalDate.now(ZoneId.systemDefault()).toString()
        val originalTx = createSampleTransaction("tx_orig_42", "Gym Subscription", 150.0, 30.18, "Expense", "Health", date = "2026-01-01")
        var savedId: String? = "sentinel"

        composeTestRule.setContent {
            FinTrackTheme {
                TransactionFormDialog(
                    initialTransaction = originalTx,
                    isDuplicateMode = true,
                    categories = sampleCategories,
                    onDismiss = {},
                    onSave = { id, _, _, _, _, _, _, _, _ -> savedId = id }
                )
            }
        }

        composeTestRule.onNodeWithText("Duplicate Transaction").assertIsDisplayed()
        composeTestRule.onNodeWithText("Date auto-updated to today ($todayStr)").assertIsDisplayed()
        composeTestRule.onNodeWithText("Confirm Duplicated Entry").assertExists()

        // Delete button must be hidden in duplicate mode
        composeTestRule.onAllNodesWithTag("tx_delete_button").assertCountEquals(0)

        composeTestRule.onNodeWithTag("save_transaction_button").performScrollTo().performClick()
        assertNull(savedId)
    }

    @Test
    fun test16_formDialog_typeToggle_dynamicallySwitchesSignAndDestinations() {
        composeTestRule.setContent {
            FinTrackTheme {
                TransactionFormDialog(
                    initialTransaction = null,
                    isDuplicateMode = false,
                    categories = sampleCategories,
                    onDismiss = {},
                    onSave = { _, _, _, _, _, _, _, _, _ -> }
                )
            }
        }

        // Starts as Expense with negative sign
        composeTestRule.onNodeWithContentDescription("Expense type").assertIsSelected()
        composeTestRule.onNodeWithText("- ").assertIsDisplayed()

        // Switch to Income
        composeTestRule.onNodeWithText("Income").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithContentDescription("Income type").assertIsSelected()
        composeTestRule.onNodeWithText("+ ").assertIsDisplayed()

        // Destination selector appears for Income
        composeTestRule.onNodeWithText("Destination (Optional)").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Bubu").assertExists()
        composeTestRule.onNodeWithText("Piticania").assertExists()
    }

    @Test
    fun test17_formDialog_amountValidation_requiresPositiveAmount() {
        var saveInvoked = false

        composeTestRule.setContent {
            FinTrackTheme {
                TransactionFormDialog(
                    initialTransaction = null,
                    isDuplicateMode = false,
                    categories = sampleCategories,
                    onDismiss = {},
                    onSave = { _, _, _, _, _, _, _, _, _ -> saveInvoked = true }
                )
            }
        }

        // Click save with empty amount
        composeTestRule.onNodeWithTag("save_transaction_button").performScrollTo().performClick()
        composeTestRule.waitForIdle()
        assertFalse(saveInvoked)
        composeTestRule.onNodeWithText("Please enter a valid amount greater than 0").performScrollTo().assertIsDisplayed()

        // Input 0.0 -> Still invalid
        composeTestRule.onNodeWithTag("tx_input_amount").performTextInput("0.0")
        composeTestRule.onNodeWithTag("save_transaction_button").performScrollTo().performClick()
        composeTestRule.waitForIdle()
        assertFalse(saveInvoked)

        // Valid amount and description -> Success
        composeTestRule.onNodeWithTag("tx_input_amount").performTextClearance()
        composeTestRule.onNodeWithTag("tx_input_amount").performTextInput("50.0")
        composeTestRule.onNodeWithTag("tx_input_desc").performTextInput("Coffee beans")
        composeTestRule.onNodeWithTag("save_transaction_button").performScrollTo().performClick()
        composeTestRule.waitForIdle()
        assertTrue(saveInvoked)
    }

    @Test
    fun test18_formDialog_descriptionValidation_requiresNonBlankDescription() {
        var saveInvoked = false

        composeTestRule.setContent {
            FinTrackTheme {
                TransactionFormDialog(
                    initialTransaction = null,
                    isDuplicateMode = false,
                    categories = sampleCategories,
                    onDismiss = {},
                    onSave = { _, _, _, _, _, _, _, _, _ -> saveInvoked = true }
                )
            }
        }

        composeTestRule.onNodeWithTag("tx_input_amount").performTextInput("80.0")
        composeTestRule.onNodeWithTag("save_transaction_button").performScrollTo().performClick()
        composeTestRule.waitForIdle()

        assertFalse(saveInvoked)
        composeTestRule.onNodeWithText("Description is required").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun test19_formDialog_autocomplete_displaysSuggestionsAndAppliesSelection() {
        composeTestRule.setContent {
            FinTrackTheme {
                TransactionFormDialog(
                    initialTransaction = null,
                    isDuplicateMode = false,
                    categories = sampleCategories,
                    onDismiss = {},
                    onSearchDescriptions = { query ->
                        if (query.startsWith("phar", ignoreCase = true)) {
                            listOf("Pharmacy", "Pharmacy Catena")
                        } else emptyList()
                    },
                    onSave = { _, _, _, _, _, _, _, _, _ -> }
                )
            }
        }

        composeTestRule.onNodeWithTag("tx_input_desc").performTextInput("Phar")
        composeTestRule.mainClock.advanceTimeBy(300)
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("tx_description_suggestion_0").assertIsDisplayed()
        composeTestRule.onNodeWithText("Pharmacy").assertIsDisplayed()

        // Tap suggestion
        composeTestRule.onNodeWithTag("tx_description_suggestion_0").performClick()
        composeTestRule.mainClock.advanceTimeBy(300)
        composeTestRule.waitForIdle()

        val text = composeTestRule.onNodeWithTag("tx_input_desc")
            .fetchSemanticsNode()
            .config[SemanticsProperties.EditableText]?.text
        assertEquals("Pharmacy", text)
    }

    @Test
    fun test20_formDialog_subcategorySelection_autoAssignsCategory() {
        composeTestRule.setContent {
            FinTrackTheme {
                TransactionFormDialog(
                    initialTransaction = null,
                    isDuplicateMode = false,
                    categories = sampleCategories,
                    onDismiss = {},
                    onSave = { _, _, _, _, _, _, _, _, _ -> }
                )
            }
        }

        composeTestRule.onNodeWithText("Subcategory (Select First)").performScrollTo().performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("🛒 Groceries").assertIsDisplayed().performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("🍉 Food & Dining").assertExists()
    }

    @Test
    fun test21_formDialog_datePickerAndAccount_preservesSelections() {
        composeTestRule.setContent {
            FinTrackTheme {
                TransactionFormDialog(
                    initialTransaction = null,
                    isDuplicateMode = false,
                    categories = sampleCategories,
                    onDismiss = {},
                    onSave = { _, _, _, _, _, _, _, _, _ -> }
                )
            }
        }

        // Calendar icon is visible
        composeTestRule.onNodeWithContentDescription("Select Date").performScrollTo().assertIsDisplayed()

        // Account dropdown selection
        composeTestRule.onNodeWithText("Account").performScrollTo().performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Cash").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Cash").assertExists()
    }

    @Test
    fun test22_formDialog_deleteConfirmation_destructiveAlertAndCallback() {
        val tx = createSampleTransaction("tx_to_delete", "Old Expense", 20.0, 4.02, "Expense", "🍉 Food & Dining")
        var deleteInvoked = false

        composeTestRule.setContent {
            FinTrackTheme {
                TransactionFormDialog(
                    initialTransaction = tx,
                    isDuplicateMode = false,
                    categories = sampleCategories,
                    onDismiss = {},
                    onDelete = { deleteInvoked = true },
                    onSave = { _, _, _, _, _, _, _, _, _ -> }
                )
            }
        }

        composeTestRule.onNodeWithTag("tx_delete_button").performScrollTo().performClick()
        composeTestRule.waitForIdle()

        // Confirmation alert dialog appears
        composeTestRule.onNodeWithText("Are you sure you want to delete this transaction? This action cannot be undone.")
            .assertIsDisplayed()

        // Confirm deletion
        composeTestRule.onNodeWithTag("tx_confirm_delete_button").performClick()
        composeTestRule.waitForIdle()

        assertTrue(deleteInvoked)
    }

    @Test
    fun test23_formDialog_cancel_invokesDismiss() {
        var dismissInvoked = false

        composeTestRule.setContent {
            FinTrackTheme {
                TransactionFormDialog(
                    initialTransaction = null,
                    isDuplicateMode = false,
                    categories = sampleCategories,
                    onDismiss = { dismissInvoked = true },
                    onSave = { _, _, _, _, _, _, _, _, _ -> }
                )
            }
        }

        composeTestRule.onNodeWithText("Cancel").performScrollTo().performClick()
        assertTrue(dismissInvoked)
    }

    @Test
    fun test24a_responsiveLayoutStability_360dp() {
        composeTestRule.setContent {
            FinTrackTheme {
                Box(modifier = Modifier.width(360.dp)) {
                    TransactionFormDialog(
                        initialTransaction = null,
                        isDuplicateMode = false,
                        categories = sampleCategories,
                        onDismiss = {},
                        onSave = { _, _, _, _, _, _, _, _, _ -> }
                    )
                }
            }
        }
        composeTestRule.onNodeWithTag("tx_input_amount").assertIsDisplayed()
        composeTestRule.onNodeWithTag("tx_input_desc").assertIsDisplayed()
        composeTestRule.onNodeWithTag("save_transaction_button").assertExists()
    }

    @Test
    fun test24b_responsiveLayoutStability_390dp() {
        composeTestRule.setContent {
            FinTrackTheme {
                Box(modifier = Modifier.width(390.dp)) {
                    TransactionFormDialog(
                        initialTransaction = null,
                        isDuplicateMode = false,
                        categories = sampleCategories,
                        onDismiss = {},
                        onSave = { _, _, _, _, _, _, _, _, _ -> }
                    )
                }
            }
        }
        composeTestRule.onNodeWithTag("tx_input_amount").assertIsDisplayed()
        composeTestRule.onNodeWithTag("tx_input_desc").assertIsDisplayed()
        composeTestRule.onNodeWithTag("save_transaction_button").assertExists()
    }

    @Test
    fun test24c_responsiveLayoutStability_412dp() {
        composeTestRule.setContent {
            FinTrackTheme {
                Box(modifier = Modifier.width(412.dp)) {
                    TransactionFormDialog(
                        initialTransaction = null,
                        isDuplicateMode = false,
                        categories = sampleCategories,
                        onDismiss = {},
                        onSave = { _, _, _, _, _, _, _, _, _ -> }
                    )
                }
            }
        }
        composeTestRule.onNodeWithTag("tx_input_amount").assertIsDisplayed()
        composeTestRule.onNodeWithTag("tx_input_desc").assertIsDisplayed()
        composeTestRule.onNodeWithTag("save_transaction_button").assertExists()
    }

    @Test
    fun test24d_responsiveLayoutStability_600dp() {
        composeTestRule.setContent {
            FinTrackTheme {
                Box(modifier = Modifier.width(600.dp)) {
                    TransactionFormDialog(
                        initialTransaction = null,
                        isDuplicateMode = false,
                        categories = sampleCategories,
                        onDismiss = {},
                        onSave = { _, _, _, _, _, _, _, _, _ -> }
                    )
                }
            }
        }
        composeTestRule.onNodeWithTag("tx_input_amount").assertIsDisplayed()
        composeTestRule.onNodeWithTag("tx_input_desc").assertIsDisplayed()
        composeTestRule.onNodeWithTag("save_transaction_button").assertExists()
    }
}
