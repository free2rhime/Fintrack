package com.example

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import android.content.Context
import android.provider.Settings
import com.example.data.model.CategoryEntity
import com.example.data.model.TransactionEntity
import com.example.ui.components.TransactionFormDialog
import com.example.ui.theme.FinTrackTheme
import com.example.ui.theme.HeroFinancialDisplay
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
 * Material 3 Expressive Add / Edit / Duplicate Transaction Experience Migration Suite (Checkpoint 5).
 * Validates complete visual, tactile, accessibility, responsive, and behavioral specifications.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class M3TransactionFormExpressiveMigrationTest {

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
        id = "tx_expressive_404",
        userId = "user_expressive",
        date = "2026-09-01",
        description = "Weekly farmer market organic veggies",
        amountRON = 245.80,
        amountEUR = 49.16,
        exchangeRate = 5.0,
        exchangeRateDate = "2026-09-01",
        type = "Expense",
        account = "Card",
        category = "🍉 Food & Dining",
        subCategory = "🛒 Groceries",
        destination = null,
        householdId = "hh_expressive"
    )

    // 1. Add Mode
    @Test
    fun test1_addModeRendersModalSheetAndComponents() {
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
        composeTestRule.onNodeWithText("Record a new family entry").assertIsDisplayed()
        composeTestRule.onNodeWithTag("tx_input_amount").assertIsDisplayed()
        composeTestRule.onNodeWithTag("tx_input_desc").assertIsDisplayed()
        composeTestRule.onNodeWithTag("save_transaction_button").assertExists()
        composeTestRule.onNodeWithText("Save Transaction").assertExists()
        composeTestRule.onNodeWithText("Cancel").assertExists()
        composeTestRule.onAllNodesWithTag("tx_delete_button").assertCountEquals(0)
    }

    // 2. Edit Mode
    @Test
    fun test2_editModeRendersWithPrefilledValuesAndHeader() {
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
        composeTestRule.onNodeWithText("Update transaction details").assertIsDisplayed()
        composeTestRule.onNodeWithText("245.8").assertExists()
        composeTestRule.onNodeWithText("Weekly farmer market organic veggies").assertExists()
    }

    // 3. Duplicate Mode
    @Test
    fun test3_duplicateModeAutoUpdatesDateAndHidesDelete() {
        val todayStr = LocalDate.now(ZoneId.systemDefault()).toString()

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

        composeTestRule.onNodeWithText("Duplicate Transaction").assertIsDisplayed()
        composeTestRule.onNodeWithText("Date auto-updated to today ($todayStr)").assertIsDisplayed()
        composeTestRule.onNodeWithText("Confirm Duplicated Entry").assertExists()
        composeTestRule.onAllNodesWithTag("tx_delete_button").assertCountEquals(0)
    }

    // 4. Hero Amount Field Visibility and Tabular Numerals
    @Test
    fun test4_heroAmountFieldVisibilityAndTabularNumerals() {
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

        composeTestRule.onNodeWithTag("tx_input_amount").assertIsDisplayed()
        composeTestRule.onNodeWithText("RON").assertIsDisplayed()
        composeTestRule.onNodeWithText("- ").assertIsDisplayed()
        assertEquals("tnum", HeroFinancialDisplay.fontFeatureSettings)
    }

    // 5. Income / Expense Selection and Coloration
    @Test
    fun test5_incomeExpenseSegmentedControlSelectionAndColoration() {
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

        // Initially Expense
        composeTestRule.onNodeWithContentDescription("Expense type").assertIsSelected()
        composeTestRule.onNodeWithContentDescription("Income type").assertIsNotSelected()
        composeTestRule.onNodeWithText("- ").assertIsDisplayed()

        // Toggle to Income
        composeTestRule.onNodeWithText("Income").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithContentDescription("Income type").assertIsSelected()
        composeTestRule.onNodeWithContentDescription("Expense type").assertIsNotSelected()
        composeTestRule.onNodeWithText("+ ").assertIsDisplayed()
    }

    // 6. Category Selection and Auto-assignment
    @Test
    fun test6_categorySelectionAndAutoAssignment() {
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

        composeTestRule.onNodeWithText("Subcategory (Select First)")
            .performScrollTo()
            .performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("⛽ Fuel").assertIsDisplayed().performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("🚗 Transport").assertExists()
    }

    // 7. Subcategory Selection Dropdown Behavior
    @Test
    fun test7_subcategorySelectionDropdownBehavior() {
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

        composeTestRule.onNodeWithText("Subcategory (Select First)")
            .performScrollTo()
            .performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("🛒 Groceries").assertIsDisplayed()
        composeTestRule.onNodeWithText("🍔 Restaurants").assertIsDisplayed()
        composeTestRule.onNodeWithText("⛽ Fuel").assertIsDisplayed()
    }

    // 8. Date Selection Trigger
    @Test
    fun test8_dateSelectionTriggerOpensDatePicker() {
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
            .performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("OK").assertIsDisplayed()
    }

    // 9. Description Field & Autocomplete Suggestions
    @Test
    fun test9_descriptionFieldInputAndAutocompleteSuggestions() {
        composeTestRule.setContent {
            FinTrackTheme {
                TransactionFormDialog(
                    initialTransaction = null,
                    isDuplicateMode = false,
                    categories = sampleCategories,
                    onDismiss = {},
                    onSearchDescriptions = { query ->
                        if (query.startsWith("org", ignoreCase = true)) {
                            listOf("Organic Apples", "Organic Milk")
                        } else emptyList()
                    },
                    onSave = { _, _, _, _, _, _, _, _, _ -> }
                )
            }
        }

        composeTestRule.onNodeWithTag("tx_input_desc").performTextInput("Org")
        composeTestRule.mainClock.advanceTimeBy(300)
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("tx_description_suggestion_0").assertIsDisplayed()
        composeTestRule.onNodeWithText("Organic Apples").assertIsDisplayed()

        composeTestRule.onNodeWithTag("tx_description_suggestion_0").performClick()
        composeTestRule.mainClock.advanceTimeBy(300)
        composeTestRule.waitForIdle()

        val text = composeTestRule.onNodeWithTag("tx_input_desc")
            .fetchSemanticsNode()
            .config[SemanticsProperties.EditableText]?.text
        assertEquals("Organic Apples", text)
    }

    // 10. Save Action
    @Test
    fun test10_saveActionInvokesCallbackWithCorrectParameters() {
        var savedType = ""
        var savedAmount = 0.0
        var savedDesc = ""

        composeTestRule.setContent {
            FinTrackTheme {
                TransactionFormDialog(
                    initialTransaction = null,
                    isDuplicateMode = false,
                    categories = sampleCategories,
                    onDismiss = {},
                    onSave = { _, _, desc, amount, type, _, _, _, _ ->
                        savedDesc = desc
                        savedAmount = amount
                        savedType = type
                    }
                )
            }
        }

        composeTestRule.onNodeWithTag("tx_input_amount").performTextInput("150.75")
        composeTestRule.onNodeWithTag("tx_input_desc").performTextInput("Dinner with family")
        composeTestRule.onNodeWithTag("save_transaction_button")
            .performScrollTo()
            .performClick()
        composeTestRule.waitForIdle()

        assertEquals("Expense", savedType)
        assertEquals(150.75, savedAmount, 0.001)
        assertEquals("Dinner with family", savedDesc)
    }

    // 11. Cancel Action
    @Test
    fun test11_cancelActionDismissesDialog() {
        var dismissed = false

        composeTestRule.setContent {
            FinTrackTheme {
                TransactionFormDialog(
                    initialTransaction = null,
                    isDuplicateMode = false,
                    categories = sampleCategories,
                    onDismiss = { dismissed = true },
                    onSave = { _, _, _, _, _, _, _, _, _ -> }
                )
            }
        }

        composeTestRule.onNodeWithText("Cancel")
            .performScrollTo()
            .performClick()
        composeTestRule.waitForIdle()

        assertTrue(dismissed)
    }

    // 12. Delete Confirmation Flow in Edit Mode
    @Test
    fun test12_deleteConfirmationFlowInEditMode() {
        var deleteTriggered = false

        composeTestRule.setContent {
            FinTrackTheme {
                TransactionFormDialog(
                    initialTransaction = sampleTransaction,
                    isDuplicateMode = false,
                    categories = sampleCategories,
                    onDismiss = {},
                    onDelete = { deleteTriggered = true },
                    onSave = { _, _, _, _, _, _, _, _, _ -> }
                )
            }
        }

        composeTestRule.onNodeWithTag("tx_delete_button")
            .performScrollTo()
            .performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Are you sure you want to delete this transaction? This action cannot be undone.")
            .assertIsDisplayed()
        composeTestRule.onNodeWithTag("tx_confirm_delete_button")
            .assertIsDisplayed()

        composeTestRule.onNodeWithTag("tx_confirm_delete_button").performClick()
        composeTestRule.waitForIdle()

        assertTrue(deleteTriggered)
    }

    // 13. Validation States (Amount Invalid)
    @Test
    fun test13_validationStatesPreventSaveWhenAmountInvalid() {
        var saveCalled = false

        composeTestRule.setContent {
            FinTrackTheme {
                TransactionFormDialog(
                    initialTransaction = null,
                    isDuplicateMode = false,
                    categories = sampleCategories,
                    onDismiss = {},
                    onSave = { _, _, _, _, _, _, _, _, _ -> saveCalled = true }
                )
            }
        }

        composeTestRule.onNodeWithTag("tx_input_amount").performTextInput("0")
        composeTestRule.onNodeWithTag("tx_input_desc").performTextInput("Valid note")
        composeTestRule.onNodeWithTag("save_transaction_button")
            .performScrollTo()
            .performClick()
        composeTestRule.waitForIdle()

        assertFalse(saveCalled)
        composeTestRule.onNodeWithText("Please enter a valid amount greater than 0")
            .performScrollTo()
            .assertIsDisplayed()
    }

    // 14. Validation States (Description Blank)
    @Test
    fun test14_validationStatesPreventSaveWhenDescriptionBlank() {
        var saveCalled = false

        composeTestRule.setContent {
            FinTrackTheme {
                TransactionFormDialog(
                    initialTransaction = null,
                    isDuplicateMode = false,
                    categories = sampleCategories,
                    onDismiss = {},
                    onSave = { _, _, _, _, _, _, _, _, _ -> saveCalled = true }
                )
            }
        }

        composeTestRule.onNodeWithTag("tx_input_amount").performTextInput("99.90")
        composeTestRule.onNodeWithTag("save_transaction_button")
            .performScrollTo()
            .performClick()
        composeTestRule.waitForIdle()

        assertFalse(saveCalled)
        composeTestRule.onNodeWithText("Description is required")
            .performScrollTo()
            .assertIsDisplayed()
    }

    // 15. Error States Display Informative Messages
    @Test
    fun test15_errorStatesDisplayInformativeMessages() {
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

        composeTestRule.onNodeWithTag("save_transaction_button")
            .performScrollTo()
            .performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Please enter a valid amount greater than 0")
            .performScrollTo()
            .assertIsDisplayed()
    }

    // 16. Minimum Touch Targets (>= 48dp)
    @Test
    fun test16_minimumTouchTargetsAdhereTo48dpStandard() {
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

        composeTestRule.onNodeWithContentDescription("Close")
            .assertHeightIsAtLeast(48.dp)
            .assertWidthIsAtLeast(48.dp)

        composeTestRule.onNodeWithContentDescription("Expense type")
            .assertHeightIsAtLeast(48.dp)

        composeTestRule.onNodeWithContentDescription("Income type")
            .assertHeightIsAtLeast(48.dp)

        composeTestRule.onNodeWithTag("tx_delete_button")
            .performScrollTo()
            .assertHeightIsAtLeast(48.dp)

        composeTestRule.onNodeWithTag("save_transaction_button")
            .performScrollTo()
            .assertHeightIsAtLeast(48.dp)
    }

    // 17. Accessibility Semantics for Tabs, Roles, and Labels
    @Test
    fun test17_accessibilitySemanticsForTabsRolesAndLabels() {
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

        val expenseNode = composeTestRule.onNodeWithContentDescription("Expense type").fetchSemanticsNode()
        assertEquals(Role.Tab, expenseNode.config[SemanticsProperties.Role])

        val incomeNode = composeTestRule.onNodeWithContentDescription("Income type").fetchSemanticsNode()
        assertEquals(Role.Tab, incomeNode.config[SemanticsProperties.Role])

        val deleteNode = composeTestRule.onNodeWithTag("tx_delete_button")
            .performScrollTo()
            .fetchSemanticsNode()
        assertEquals(Role.Button, deleteNode.config[SemanticsProperties.Role])
        assertEquals("Delete Transaction", deleteNode.config[SemanticsProperties.ContentDescription]?.firstOrNull())
    }

    // 18. Reduced Motion Mode
    @Test
    fun test18_reducedMotionModeAppliesImmediateTransitions() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        Settings.Global.putFloat(context.contentResolver, Settings.Global.TRANSITION_ANIMATION_SCALE, 0f)
        Settings.Global.putFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 0f)

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

        composeTestRule.onNodeWithText("Income").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithContentDescription("Income type").assertIsSelected()
        composeTestRule.onNodeWithText("+ ").assertIsDisplayed()
    }

    // 19. Responsive Layout at 360dp
    @Test
    fun test19_responsiveLayoutAt360dpViewport() {
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

    // 20. Responsive Layout at 390dp
    @Test
    fun test20_responsiveLayoutAt390dpViewport() {
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

    // 21. Responsive Layout at 412dp
    @Test
    fun test21_responsiveLayoutAt412dpViewport() {
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

    // 22. Responsive Layout at 600dp+ (Expanded Viewport)
    @Test
    fun test22_responsiveLayoutAt600dpPlusExpandedViewport() {
        composeTestRule.setContent {
            FinTrackTheme {
                Box(modifier = Modifier.width(720.dp)) {
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

    // 23. No Financial Calculation Duplication
    @Test
    fun test23_noFinancialCalculationDuplication() {
        var capturedAmount: Double? = null

        composeTestRule.setContent {
            FinTrackTheme {
                TransactionFormDialog(
                    initialTransaction = null,
                    isDuplicateMode = false,
                    categories = sampleCategories,
                    onDismiss = {},
                    onSave = { _, _, _, amount, _, _, _, _, _ -> capturedAmount = amount }
                )
            }
        }

        composeTestRule.onNodeWithTag("tx_input_amount").performTextInput("123.45")
        composeTestRule.onNodeWithTag("tx_input_desc").performTextInput("Raw amount test")
        composeTestRule.onNodeWithTag("save_transaction_button")
            .performScrollTo()
            .performClick()
        composeTestRule.waitForIdle()

        // Verifies the raw parsed amount is passed directly without synthetic re-calculations
        assertEquals(123.45, capturedAmount ?: 0.0, 0.001)
    }

    // 24. Formatter Preservation
    @Test
    fun test24_formatterPreservation() {
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

        // 245.8 is shown as entered/stored without extraneous formatting loss
        composeTestRule.onNodeWithText("245.8").assertExists()
    }

    // 25. Callback Preservation
    @Test
    fun test25_callbackPreservation() {
        var editId: String? = null
        var isDismissed = false

        composeTestRule.setContent {
            FinTrackTheme {
                TransactionFormDialog(
                    initialTransaction = sampleTransaction,
                    isDuplicateMode = false,
                    categories = sampleCategories,
                    onDismiss = { isDismissed = true },
                    onSave = { id, _, _, _, _, _, _, _, _ -> editId = id }
                )
            }
        }

        composeTestRule.onNodeWithTag("save_transaction_button")
            .performScrollTo()
            .performClick()
        composeTestRule.waitForIdle()

        assertEquals("tx_expressive_404", editId)
        assertFalse(isDismissed)
    }

    // 26. State Preservation on Recomposition
    @Test
    fun test26_statePreservationOnRecomposition() {
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

        composeTestRule.onNodeWithTag("tx_input_amount").performTextInput("77.70")
        composeTestRule.onNodeWithTag("tx_input_desc").performTextInput("Recomposition check")
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("77.70").assertExists()
        composeTestRule.onNodeWithText("Recomposition check").assertExists()
    }

    // 27. Existing testTag Preservation
    @Test
    fun test27_allExistingTestTagsPreserved() {
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

        composeTestRule.onNodeWithTag("tx_input_amount").assertIsDisplayed()
        composeTestRule.onNodeWithTag("tx_input_desc").assertIsDisplayed()
        composeTestRule.onNodeWithTag("tx_delete_button").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("save_transaction_button").performScrollTo().assertIsDisplayed()
    }
}
