package com.example

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.data.model.CategoryEntity
import com.example.data.model.FilterSettings
import com.example.data.model.TransactionEntity
import com.example.ui.components.FinTrackTransactionRow
import com.example.ui.components.RecentActivitySection
import com.example.ui.components.TransactionCardItem
import com.example.ui.screens.TransactionsScreen
import com.example.ui.theme.ExpenseCoral
import com.example.ui.theme.FinTrackTheme
import com.example.ui.theme.IncomeEmerald
import com.example.ui.theme.ShapeGroupedContainer
import com.example.ui.theme.ShapeGroupedItemBottom
import com.example.ui.theme.ShapeGroupedItemMiddle
import com.example.ui.theme.ShapeGroupedItemSingle
import com.example.ui.theme.ShapeGroupedItemTop
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class Phase3DMaterial3ExpressiveTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun createDummyTransaction(
        id: String,
        desc: String,
        amountRon: Double,
        amountEur: Double,
        category: String = "Food",
        type: String = "Expense",
        date: String = "2026-03-10"
    ): TransactionEntity {
        return TransactionEntity(
            id = id,
            description = desc,
            amountRON = amountRon,
            amountEUR = amountEur,
            category = category,
            subCategory = "",
            type = type,
            date = date,
            account = "Cash",
            exchangeRate = 4.97,
            exchangeRateDate = "2026-03-10",
            exchangeRateSource = "BNR_OFFICIAL",
            conversionStatus = "OFFICIAL"
        )
    }

    @Test
    fun test01_GroupedShapeTokensVerifyContract() {
        assertNotNull(ShapeGroupedContainer)
        assertNotNull(ShapeGroupedItemTop)
        assertNotNull(ShapeGroupedItemMiddle)
        assertNotNull(ShapeGroupedItemBottom)
        assertNotNull(ShapeGroupedItemSingle)
    }

    @Test
    fun test02_RecentActivitySectionRendersInGroupedContainer() {
        val tx1 = createDummyTransaction("1", "Organic Groceries", 145.50, 29.28, "Food")
        val tx2 = createDummyTransaction("2", "Metro Pass", 70.00, 14.08, "Transport")
        val tx3 = createDummyTransaction("3", "Freelance Invoice", 3200.00, 643.86, "Salary", type = "Income")

        var clickedTx: TransactionEntity? = null
        var viewAllClicked = false

        composeTestRule.setContent {
            FinTrackTheme {
                RecentActivitySection(
                    transactions = listOf(tx1, tx2, tx3),
                    selectedCurrency = "RON",
                    onTransactionClicked = { clickedTx = it },
                    onViewAllClicked = { viewAllClicked = true }
                )
            }
        }

        // Verify container and header
        composeTestRule.onNodeWithTag("recent_activity_section").assertIsDisplayed()
        composeTestRule.onNodeWithTag("view_all_activity_button").assertIsDisplayed()
        composeTestRule.onNodeWithText("Recent Activity").assertIsDisplayed()

        // Verify items displayed with stable test tags
        composeTestRule.onNodeWithTag("recent_tx_item_1").assertIsDisplayed()
        composeTestRule.onNodeWithTag("recent_tx_item_2").assertIsDisplayed()
        composeTestRule.onNodeWithTag("recent_tx_item_3").assertIsDisplayed()

        // Verify descriptions & amounts
        composeTestRule.onNodeWithText("Organic Groceries").assertIsDisplayed()
        composeTestRule.onNodeWithText("Metro Pass").assertIsDisplayed()
        composeTestRule.onNodeWithText("Freelance Invoice").assertIsDisplayed()

        // Test interaction
        composeTestRule.onNodeWithTag("recent_tx_item_2").performClick()
        assertEquals("2", clickedTx?.id)

        composeTestRule.onNodeWithTag("view_all_activity_button").performClick()
        assertTrue(viewAllClicked)
    }

    @Test
    fun test03_RecentActivitySectionEmptyStateRendersInGroupedContainer() {
        composeTestRule.setContent {
            FinTrackTheme {
                RecentActivitySection(
                    transactions = emptyList(),
                    selectedCurrency = "RON",
                    onTransactionClicked = {},
                    onViewAllClicked = {}
                )
            }
        }

        composeTestRule.onNodeWithTag("recent_activity_section").assertIsDisplayed()
        composeTestRule.onNodeWithTag("recent_activity_empty_state").assertIsDisplayed()
        composeTestRule.onNodeWithText("No Recent Activity").assertIsDisplayed()
    }

    @Test
    fun test04_FinTrackTransactionRowSupportsTactileAndDividers() {
        composeTestRule.setContent {
            FinTrackTheme {
                FinTrackTransactionRow(
                    description = "Bakery & Coffee",
                    categoryName = "Food & Dining",
                    dateFormatted = "Today",
                    accountName = "Main Card",
                    amountPrimaryFormatted = "32.50",
                    isIncome = false,
                    amountSecondaryFormatted = "-6.54",
                    primaryCurrency = "RON",
                    secondaryCurrency = "EUR",
                    categoryIcon = Icons.Default.Restaurant,
                    categoryColor = ExpenseCoral,
                    shape = ShapeGroupedItemTop,
                    showDivider = true,
                    onClick = {},
                    modifier = Modifier.testTag("test_tx_row")
                )
            }
        }

        composeTestRule.onNodeWithTag("test_tx_row").assertExists()
        composeTestRule.onNodeWithText("Bakery & Coffee").assertExists()
        composeTestRule.onNodeWithText("- 32.50 RON").assertExists()
    }

    @Test
    fun test05_TransactionCardItemPreservesAccessibilityAndActions() {
        val tx = createDummyTransaction("42", "Pharmacy Prescription", 89.0, 17.91, "Health")
        var editedTx: TransactionEntity? = null
        var duplicatedTx: TransactionEntity? = null
        var deletedTx: TransactionEntity? = null

        composeTestRule.setContent {
            FinTrackTheme {
                TransactionCardItem(
                    transaction = tx,
                    selectedCurrency = "RON",
                    onDuplicateClicked = { duplicatedTx = it },
                    onEditClicked = { editedTx = it },
                    onDeleteClicked = { deletedTx = it },
                    shape = ShapeGroupedItemSingle,
                    showDivider = false
                )
            }
        }

        composeTestRule.onNodeWithTag("transaction_item_42").assertIsDisplayed()
        composeTestRule.onNodeWithTag("tx_duplicate_42").assertIsDisplayed()
        composeTestRule.onNodeWithTag("tx_delete_42").assertIsDisplayed()

        composeTestRule.onNodeWithTag("tx_duplicate_42").performClick()
        assertEquals("42", duplicatedTx?.id)

        composeTestRule.onNodeWithTag("tx_delete_42").performClick()
        assertEquals("42", deletedTx?.id)

        composeTestRule.onNodeWithTag("transaction_item_42").performClick()
        assertEquals("42", editedTx?.id)
    }

    @Test
    fun test06_TransactionsScreenRendersDateGroupsInContinuousContainers() {
        val txDay1A = createDummyTransaction("101", "Dinner Out", 210.0, 42.25, "Food", date = "2026-03-10")
        val txDay1B = createDummyTransaction("102", "Taxi Fare", 45.0, 9.05, "Transport", date = "2026-03-10")
        val txDay2 = createDummyTransaction("103", "Salary Transfer", 5000.0, 1006.03, "Salary", type = "Income", date = "2026-03-09")

        val cat1 = CategoryEntity(id = "c1", name = "Food", type = "Expense")
        val cat2 = CategoryEntity(id = "c2", name = "Transport", type = "Expense")
        val cat3 = CategoryEntity(id = "c3", name = "Salary", type = "Income")

        composeTestRule.setContent {
            FinTrackTheme {
                TransactionsScreen(
                    transactions = listOf(txDay1A, txDay1B, txDay2),
                    categories = listOf(cat1, cat2, cat3),
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

        // Verify date group and transactions exist in LazyColumn
        composeTestRule.onNodeWithTag("transaction_date_group_2026-03-10").assertExists()
        composeTestRule.onNodeWithTag("transaction_item_101").assertExists()
        composeTestRule.onNodeWithTag("transaction_item_102").assertExists()
    }
}
