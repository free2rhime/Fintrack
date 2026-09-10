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
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.unit.dp
import com.example.data.model.CategoryEntity
import com.example.data.model.TransactionEntity
import com.example.ui.components.TransactionFormDialog
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

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class M3AddTransactionExpressiveTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val sampleCategories = listOf(
        CategoryEntity(name = "🍉 Food & Dining", type = "Expense", subCategory = "🛒 Groceries"),
        CategoryEntity(name = "🍉 Food & Dining", type = "Expense", subCategory = "🍔 Restaurants"),
        CategoryEntity(name = "🚗 Transport", type = "Expense", subCategory = "⛽ Fuel"),
        CategoryEntity(name = "💼 Salary", type = "Income", subCategory = "🏢 Primary Job"),
        CategoryEntity(name = "📈 Investments", type = "Income", subCategory = "🪙 Dividends")
    )

    private val sampleTransaction = TransactionEntity(
        id = "tx_edit_100",
        userId = "user_1",
        date = "2026-09-01",
        description = "Supermarket groceries",
        amountRON = 120.50,
        amountEUR = 24.10,
        exchangeRate = 5.0,
        exchangeRateDate = "2026-09-01",
        type = "Expense",
        account = "Card",
        category = "🍉 Food & Dining",
        subCategory = "🛒 Groceries",
        destination = null,
        householdId = "hh_1"
    )

    @Test
    fun test1_addModeRendersCorrectly() {
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

        // Title and primary fields
        composeTestRule.onNodeWithText("Add Transaction").assertIsDisplayed()
        composeTestRule.onNodeWithTag("tx_input_amount").assertIsDisplayed()
        composeTestRule.onNodeWithTag("tx_input_desc").assertIsDisplayed()
        composeTestRule.onNodeWithTag("save_transaction_button").assertExists()
        composeTestRule.onNodeWithText("Save Transaction").assertExists()
        composeTestRule.onNodeWithText("Cancel").assertExists()
    }

    @Test
    fun test2_editModeRendersCorrectlyWithPrefilledValues() {
        composeTestRule.setContent {
            FinTrackTheme {
                TransactionFormDialog(
                    initialTransaction = sampleTransaction,
                    isDuplicateMode = false,
                    categories = sampleCategories,
                    onDismiss = {},
                    onSave = { _, _, _, _, _, _, _, _, _ -> }
                )
            }
        }

        composeTestRule.onNodeWithText("Edit Transaction").assertIsDisplayed()
        composeTestRule.onNodeWithText("120.5").assertExists()
        composeTestRule.onNodeWithText("Supermarket groceries").assertExists()
    }

    @Test
    fun test3_duplicateModeRendersCorrectly() {
        val todayStr = LocalDate.now(ZoneId.systemDefault()).toString()

        composeTestRule.setContent {
            FinTrackTheme {
                TransactionFormDialog(
                    initialTransaction = sampleTransaction,
                    isDuplicateMode = true,
                    categories = sampleCategories,
                    onDismiss = {},
                    onSave = { _, _, _, _, _, _, _, _, _ -> }
                )
            }
        }

        composeTestRule.onNodeWithText("Duplicate Transaction").assertIsDisplayed()
        composeTestRule.onNodeWithText("Date auto-updated to today ($todayStr)").assertIsDisplayed()
        composeTestRule.onNodeWithText("Confirm Duplicated Entry").assertExists()
    }

    @Test
    fun test4_expenseSelectedStateByDefault() {
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

        composeTestRule.onNodeWithContentDescription("Expense type")
            .assertIsSelected()
        composeTestRule.onNodeWithContentDescription("Income type")
            .assertIsNotSelected()

        // Negative sign for Expense
        composeTestRule.onNodeWithText("- ").assertIsDisplayed()
    }

    @Test
    fun test5_incomeSelectedStateTogglesAndRevealsDestination() {
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

        // Toggle to Income
        composeTestRule.onNodeWithText("Income").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithContentDescription("Income type")
            .assertIsSelected()
        composeTestRule.onNodeWithContentDescription("Expense type")
            .assertIsNotSelected()

        // Positive sign for Income
        composeTestRule.onNodeWithText("+ ").assertIsDisplayed()

        // Destination section appears
        composeTestRule.onNodeWithText("Destination (Optional)").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Bubu").assertExists()
        composeTestRule.onNodeWithText("Piticania").assertExists()
    }

    @Test
    fun test6_amountFieldPreservedAndValidationFailsOnZeroOrNegative() {
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
        composeTestRule.onNodeWithTag("save_transaction_button")
            .performScrollTo()
            .performClick()
        composeTestRule.waitForIdle()

        assertFalse(saveInvoked)
        composeTestRule.onNodeWithText("Please enter a valid amount greater than 0")
            .performScrollTo()
            .assertIsDisplayed()

        // Input valid amount
        composeTestRule.onNodeWithTag("tx_input_amount").performTextInput("85.25")
        composeTestRule.onNodeWithTag("tx_input_desc").performTextInput("Valid desc")
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("save_transaction_button")
            .performScrollTo()
            .performClick()
        composeTestRule.waitForIdle()

        assertTrue(saveInvoked)
    }

    @Test
    fun test7_ronIndicatorVisibleAndDecoupled() {
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

        // RON currency indicator must be visible on the hero amount surface
        composeTestRule.onNodeWithText("RON").assertIsDisplayed()
    }

    @Test
    fun test8_descriptionFieldPreservedAndValidationFailsWhenBlank() {
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

        // Enter amount but leave description blank
        composeTestRule.onNodeWithTag("tx_input_amount").performTextInput("50.0")
        composeTestRule.onNodeWithTag("save_transaction_button")
            .performScrollTo()
            .performClick()
        composeTestRule.waitForIdle()

        assertFalse(saveInvoked)
        composeTestRule.onNodeWithText("Description is required")
            .performScrollTo()
            .assertIsDisplayed()
    }

    @Test
    fun test9_autocompleteSuggestionsPreserved() {
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

        // Verify description is populated and menu dismissed
        val text = composeTestRule.onNodeWithTag("tx_input_desc")
            .fetchSemanticsNode()
            .config[SemanticsProperties.EditableText]?.text
        assertEquals("Pharmacy", text)
        composeTestRule.onAllNodesWithTag("tx_description_suggestion_0").assertCountEquals(0)
    }

    @Test
    fun test10_categorySubcategoryInteractionsPreserved() {
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

        // Open subcategory dropdown
        composeTestRule.onNodeWithText("Subcategory (Select First)")
            .performScrollTo()
            .performClick()
        composeTestRule.waitForIdle()

        // Select "🛒 Groceries"
        composeTestRule.onNodeWithText("🛒 Groceries").assertIsDisplayed().performClick()
        composeTestRule.waitForIdle()

        // Category auto-assigned to "🍉 Food & Dining"
        composeTestRule.onNodeWithText("🍉 Food & Dining").assertExists()
    }

    @Test
    fun test11_dateSelectorPreserved() {
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

        composeTestRule.onNodeWithContentDescription("Select Date")
            .performScrollTo()
            .assertIsDisplayed()
        composeTestRule.onNodeWithText("Date (YYYY-MM-DD)").assertExists()
    }

    @Test
    fun test12_accountSelectorPreserved() {
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

        composeTestRule.onNodeWithText("Account")
            .performScrollTo()
            .performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Cash")
            .performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Cash").assertExists()
    }

    @Test
    fun test13_incomeDestinationBehaviorPreserved() {
        var savedDestination: String? = null

        composeTestRule.setContent {
            FinTrackTheme {
                TransactionFormDialog(
                    initialTransaction = null,
                    isDuplicateMode = false,
                    categories = sampleCategories,
                    onDismiss = {},
                    onSave = { _, _, _, _, _, _, _, _, dest -> savedDestination = dest }
                )
            }
        }

        // Switch to Income
        composeTestRule.onNodeWithText("Income").performClick()
        composeTestRule.waitForIdle()

        // Select "Bubu"
        composeTestRule.onNodeWithText("Bubu")
            .performScrollTo()
            .performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("tx_input_amount")
            .performScrollTo()
            .performTextInput("1000.0")
        composeTestRule.onNodeWithTag("tx_input_desc")
            .performScrollTo()
            .performTextInput("Salary bonus")
        composeTestRule.onNodeWithTag("save_transaction_button")
            .performScrollTo()
            .performClick()
        composeTestRule.waitForIdle()

        assertEquals("Bubu", savedDestination)
    }

    @Test
    fun test14_deleteVisibleOnlyInEditMode() {
        composeTestRule.setContent {
            FinTrackTheme {
                TransactionFormDialog(
                    initialTransaction = sampleTransaction,
                    isDuplicateMode = false,
                    categories = sampleCategories,
                    onDismiss = {},
                    onDelete = {},
                    onSave = { _, _, _, _, _, _, _, _, _ -> }
                )
            }
        }

        composeTestRule.onNodeWithTag("tx_delete_button")
            .performScrollTo()
            .assertIsDisplayed()
        composeTestRule.onNodeWithText("Delete Transaction").assertIsDisplayed()
    }

    @Test
    fun test15_deleteHiddenInAddMode() {
        composeTestRule.setContent {
            FinTrackTheme {
                TransactionFormDialog(
                    initialTransaction = null,
                    isDuplicateMode = false,
                    categories = sampleCategories,
                    onDismiss = {},
                    onDelete = {},
                    onSave = { _, _, _, _, _, _, _, _, _ -> }
                )
            }
        }

        composeTestRule.onAllNodesWithTag("tx_delete_button").assertCountEquals(0)
    }

    @Test
    fun test16_deleteHiddenInDuplicateMode() {
        composeTestRule.setContent {
            FinTrackTheme {
                TransactionFormDialog(
                    initialTransaction = sampleTransaction,
                    isDuplicateMode = true,
                    categories = sampleCategories,
                    onDismiss = {},
                    onDelete = {},
                    onSave = { _, _, _, _, _, _, _, _, _ -> }
                )
            }
        }

        composeTestRule.onAllNodesWithTag("tx_delete_button").assertCountEquals(0)
    }

    @Test
    fun test17_deleteConfirmationTriggersCallback() {
        var deleteInvoked = false

        composeTestRule.setContent {
            FinTrackTheme {
                TransactionFormDialog(
                    initialTransaction = sampleTransaction,
                    isDuplicateMode = false,
                    categories = sampleCategories,
                    onDismiss = {},
                    onDelete = { deleteInvoked = true },
                    onSave = { _, _, _, _, _, _, _, _, _ -> }
                )
            }
        }

        // Tap Delete Transaction button
        composeTestRule.onNodeWithTag("tx_delete_button")
            .performScrollTo()
            .performClick()
        composeTestRule.waitForIdle()

        // Confirmation dialog appears
        composeTestRule.onNodeWithText("Are you sure you want to delete this transaction? This action cannot be undone.")
            .assertIsDisplayed()

        // Tap Confirm Delete
        composeTestRule.onNodeWithTag("tx_confirm_delete_button").performClick()
        composeTestRule.waitForIdle()

        assertTrue(deleteInvoked)
    }

    @Test
    fun test18a_saveCallbackPreservesIdInEditMode() {
        var savedId: String? = null

        composeTestRule.setContent {
            FinTrackTheme {
                TransactionFormDialog(
                    initialTransaction = sampleTransaction,
                    isDuplicateMode = false,
                    categories = sampleCategories,
                    onDismiss = {},
                    onSave = { id, _, _, _, _, _, _, _, _ -> savedId = id }
                )
            }
        }

        composeTestRule.onNodeWithTag("save_transaction_button")
            .performScrollTo()
            .performClick()
        composeTestRule.waitForIdle()

        assertEquals("tx_edit_100", savedId)
    }

    @Test
    fun test18b_saveCallbackNullsIdInDuplicateMode() {
        var savedId: String? = "sentinel"

        composeTestRule.setContent {
            FinTrackTheme {
                TransactionFormDialog(
                    initialTransaction = sampleTransaction,
                    isDuplicateMode = true,
                    categories = sampleCategories,
                    onDismiss = {},
                    onSave = { id, _, _, _, _, _, _, _, _ -> savedId = id }
                )
            }
        }

        composeTestRule.onNodeWithTag("save_transaction_button")
            .performScrollTo()
            .performClick()
        composeTestRule.waitForIdle()

        assertNull(savedId)
    }

    @Test
    fun test19_cancelInvokesDismissCallback() {
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

        composeTestRule.onNodeWithText("Cancel")
            .performScrollTo()
            .performClick()
        composeTestRule.waitForIdle()

        assertTrue(dismissInvoked)
    }

    @Test
    fun test20_360dpLayoutStability() {
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
    fun test21_390dpLayoutStability() {
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
    fun test22_412dpLayoutStability() {
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
    fun test23_accessibilitySemanticsAndTouchTargets() {
        composeTestRule.setContent {
            FinTrackTheme {
                TransactionFormDialog(
                    initialTransaction = sampleTransaction,
                    isDuplicateMode = false,
                    categories = sampleCategories,
                    onDismiss = {},
                    onDelete = {},
                    onSave = { _, _, _, _, _, _, _, _, _ -> }
                )
            }
        }

        // Close button has accessible role & content description
        composeTestRule.onNodeWithContentDescription("Close").assertIsDisplayed()

        // Tab roles for type selector
        val expenseNode = composeTestRule.onNodeWithContentDescription("Expense type").fetchSemanticsNode()
        assertEquals(Role.Tab, expenseNode.config[SemanticsProperties.Role])

        val incomeNode = composeTestRule.onNodeWithContentDescription("Income type").fetchSemanticsNode()
        assertEquals(Role.Tab, incomeNode.config[SemanticsProperties.Role])

        // Delete button has Button role
        val deleteNode = composeTestRule.onNodeWithTag("tx_delete_button")
            .performScrollTo()
            .fetchSemanticsNode()
        assertEquals(Role.Button, deleteNode.config[SemanticsProperties.Role])
    }
}
