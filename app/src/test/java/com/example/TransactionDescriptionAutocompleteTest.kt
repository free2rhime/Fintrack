package com.example

import android.content.Context
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.db.FinTrackDatabase
import com.example.data.model.CategoryEntity
import com.example.data.model.TransactionEntity
import com.example.data.repository.RoomTransactionRepository
import com.example.data.service.ExchangeRateService
import com.example.ui.components.TransactionFormDialog
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class TransactionDescriptionAutocompleteTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var db: FinTrackDatabase
    private lateinit var repository: RoomTransactionRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, FinTrackDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        val exchangeRateService = ExchangeRateService(db.exchangeRateDao())
        repository = RoomTransactionRepository(
            transactionDao = db.transactionDao(),
            exchangeRateService = exchangeRateService,
            exchangeRateDao = db.exchangeRateDao(),
            syncOutboxDao = db.syncOutboxDao(),
            database = db
        )
    }

    @After
    fun tearDown() {
        db.close()
    }

    private suspend fun insertTx(
        description: String,
        householdId: String? = null,
        type: String = "Expense",
        isDeleted: Boolean = false
    ) {
        val tx = TransactionEntity(
            id = java.util.UUID.randomUUID().toString(),
            userId = "user_test",
            date = "2026-09-01",
            description = description,
            amountRON = 100.0,
            amountEUR = 20.0,
            exchangeRate = 5.0,
            exchangeRateDate = "2026-09-01",
            type = type,
            account = "Card",
            category = "Food",
            subCategory = "Groceries",
            householdId = householdId,
            isDeleted = isDeleted
        )
        db.transactionDao().insertTransaction(tx)
    }

    @Test
    fun test1_prefixMatching_returnsOnlyDescriptionsStartingWithQuery() = runBlocking {
        insertTx("Avocado")
        insertTx("Avans salariu")
        insertTx("Avion")
        insertTx("Restaurant")
        insertTx("Abonament Netflix")

        val suggestions = repository.getDescriptionSuggestions("Av", limit = 8)

        assertEquals(3, suggestions.size)
        assertTrue(suggestions.contains("Avocado"))
        assertTrue(suggestions.contains("Avans salariu"))
        assertTrue(suggestions.contains("Avion"))
        assertFalse(suggestions.contains("Restaurant"))
        assertFalse(suggestions.contains("Abonament Netflix"))
    }

    @Test
    fun test2_caseInsensitiveMatching_producesSameResultsRegardlessOfCasing() = runBlocking {
        insertTx("Avocado")
        insertTx("Avans salariu")

        val suggestionsLower = repository.getDescriptionSuggestions("av", limit = 8)
        val suggestionsMixed = repository.getDescriptionSuggestions("Av", limit = 8)
        val suggestionsUpper = repository.getDescriptionSuggestions("AV", limit = 8)

        assertEquals(listOf("Avans salariu", "Avocado"), suggestionsLower)
        assertEquals(suggestionsLower, suggestionsMixed)
        assertEquals(suggestionsLower, suggestionsUpper)
    }

    @Test
    fun test3_noSubstringMatching_doesNotReturnMatchesInsideString() = runBlocking {
        insertTx("Avocado")
        insertTx("Restaurant cu avocado")
        insertTx("Salată avocado")

        val suggestions = repository.getDescriptionSuggestions("av", limit = 8)

        assertEquals(1, suggestions.size)
        assertEquals("Avocado", suggestions[0])
    }

    @Test
    fun test4_duplicateElimination_returnsUniqueDescriptions() = runBlocking {
        insertTx("Avocado")
        insertTx("Avocado")
        insertTx("Avocado")

        val suggestions = repository.getDescriptionSuggestions("Av", limit = 8)

        assertEquals(1, suggestions.size)
        assertEquals("Avocado", suggestions[0])
    }

    @Test
    fun test5_resultLimit_respectsConfiguredMaximum() = runBlocking {
        for (i in 1..15) {
            insertTx("Test item $i")
        }

        val suggestionsLimit5 = repository.getDescriptionSuggestions("Test", limit = 5)
        val suggestionsLimit8 = repository.getDescriptionSuggestions("Test", limit = 8)

        assertEquals(5, suggestionsLimit5.size)
        assertEquals(8, suggestionsLimit8.size)
    }

    @Test
    fun test6_emptyAndBlankInput_returnsEmptyList() = runBlocking {
        insertTx("Avocado")
        insertTx("Apple")

        val emptyResult = repository.getDescriptionSuggestions("")
        val spaceResult = repository.getDescriptionSuggestions("   ")

        assertTrue(emptyResult.isEmpty())
        assertTrue(spaceResult.isEmpty())
    }

    @Test
    fun test7_singleCharacterInput_triggersMatching() = runBlocking {
        insertTx("Avocado")
        insertTx("Banana")
        insertTx("Bread")

        val suggestionsA = repository.getDescriptionSuggestions("A", limit = 8)
        val suggestionsB = repository.getDescriptionSuggestions("B", limit = 8)

        assertEquals(listOf("Avocado"), suggestionsA)
        assertEquals(2, suggestionsB.size)
        assertTrue(suggestionsB.contains("Banana"))
        assertTrue(suggestionsB.contains("Bread"))
    }

    @Test
    fun test8_householdIsolation_strictlyScopesSuggestions() = runBlocking {
        insertTx("Alpha Coffee", householdId = "HH_A")
        insertTx("Alpha Groceries", householdId = "HH_A")
        insertTx("Alpha Restaurant", householdId = "HH_B")
        insertTx("Alpha Legacy", householdId = null)

        // Query Household A
        val suggestionsA = repository.getDescriptionSuggestions("Alpha", householdId = "HH_A")
        assertEquals(2, suggestionsA.size)
        assertTrue(suggestionsA.contains("Alpha Coffee"))
        assertTrue(suggestionsA.contains("Alpha Groceries"))
        assertFalse(suggestionsA.contains("Alpha Restaurant"))
        assertFalse(suggestionsA.contains("Alpha Legacy"))

        // Query Household B
        val suggestionsB = repository.getDescriptionSuggestions("Alpha", householdId = "HH_B")
        assertEquals(1, suggestionsB.size)
        assertEquals("Alpha Restaurant", suggestionsB[0])

        // Query Local / Legacy (null household)
        val suggestionsNull = repository.getDescriptionSuggestions("Alpha", householdId = null)
        assertEquals(1, suggestionsNull.size)
        assertEquals("Alpha Legacy", suggestionsNull[0])
    }

    @Test
    fun test9_deletedTransactions_areExcludedFromSuggestions() = runBlocking {
        insertTx("Active Avocado", isDeleted = false)
        insertTx("Archived Avocado", isDeleted = true)

        val suggestions = repository.getDescriptionSuggestions("A", limit = 8)

        assertEquals(1, suggestions.size)
        assertEquals("Active Avocado", suggestions[0])
    }

    @Test
    fun test10_wildcardCharacters_areEscapedAndTreatedLiterally() = runBlocking {
        insertTx("100% Cotton")
        insertTx("1000 items")
        insertTx("A_B Test")
        insertTx("AXB Test")

        // "%" wildcard escaping: querying "100%" should only match "100% Cotton", not "1000 items"
        val percentMatches = repository.getDescriptionSuggestions("100%", limit = 8)
        assertEquals(1, percentMatches.size)
        assertEquals("100% Cotton", percentMatches[0])

        // "_" wildcard escaping: querying "A_" should only match "A_B Test", not "AXB Test"
        val underscoreMatches = repository.getDescriptionSuggestions("A_", limit = 8)
        assertEquals(1, underscoreMatches.size)
        assertEquals("A_B Test", underscoreMatches[0])
    }

    @Test
    fun test11_composeUi_selectionInAddExpensePopulatesFieldAndClosesDropdown() {
        val sampleCategories = listOf(
            CategoryEntity(name = "🍉 Food & Dining", type = "Expense", subCategory = "Groceries")
        )

        composeTestRule.setContent {
            TransactionFormDialog(
                initialTransaction = null,
                isDuplicateMode = false,
                categories = sampleCategories,
                onDismiss = {},
                onSearchDescriptions = { query ->
                    if (query.startsWith("av", ignoreCase = true)) {
                        listOf("Avocado", "Avans salariu", "Avion")
                    } else emptyList()
                },
                onSave = { _, _, _, _, _, _, _, _, _ -> }
            )
        }

        // Type "Av" in the description field
        val descNode = composeTestRule.onNodeWithTag("tx_input_desc")
        descNode.performTextInput("Av")

        // Wait for 200ms debounce
        composeTestRule.mainClock.advanceTimeBy(300)
        composeTestRule.waitForIdle()

        // Verify suggestion items are displayed with test tags
        composeTestRule.onNodeWithTag("tx_description_suggestion_0").assertIsDisplayed()
        composeTestRule.onNodeWithText("Avocado").assertIsDisplayed()
        composeTestRule.onNodeWithText("Avans salariu").assertIsDisplayed()
        composeTestRule.onNodeWithText("Avion").assertIsDisplayed()

        // Tap the first suggestion: "Avocado"
        composeTestRule.onNodeWithTag("tx_description_suggestion_0").performClick()
        composeTestRule.mainClock.advanceTimeBy(300)
        composeTestRule.waitForIdle()

        // Description field should now contain "Avocado"
        val editableText = descNode.fetchSemanticsNode().config[androidx.compose.ui.semantics.SemanticsProperties.EditableText]?.text
        assertEquals("Avocado", editableText)

        // Suggestions dropdown should be closed
        composeTestRule.onAllNodesWithTag("tx_description_suggestion_0").assertCountEquals(0)
    }

    @Test
    fun test12_composeUi_autocompleteWorksInIncomeMode() {
        val sampleCategories = listOf(
            CategoryEntity(name = "💼 Salary", type = "Income", subCategory = "Primary Job")
        )

        composeTestRule.setContent {
            TransactionFormDialog(
                initialTransaction = null,
                isDuplicateMode = false,
                categories = sampleCategories,
                onDismiss = {},
                onSearchDescriptions = { query ->
                    if (query.startsWith("av", ignoreCase = true)) {
                        listOf("Avans salariu")
                    } else emptyList()
                },
                onSave = { _, _, _, _, _, _, _, _, _ -> }
            )
        }

        // Toggle to Income mode
        composeTestRule.onNodeWithText("Income").performClick()
        composeTestRule.waitForIdle()

        // Type "av" in the description field
        val descNode = composeTestRule.onNodeWithTag("tx_input_desc")
        descNode.performTextInput("av")

        // Wait for debounce
        composeTestRule.mainClock.advanceTimeBy(300)
        composeTestRule.waitForIdle()

        // Verify suggestion appears
        composeTestRule.onNodeWithTag("tx_description_suggestion_0").assertIsDisplayed()
        composeTestRule.onNodeWithText("Avans salariu").assertIsDisplayed()

        // Click suggestion
        composeTestRule.onNodeWithTag("tx_description_suggestion_0").performClick()
        composeTestRule.mainClock.advanceTimeBy(300)
        composeTestRule.waitForIdle()

        // Description populated
        val editableText = descNode.fetchSemanticsNode().config[androidx.compose.ui.semantics.SemanticsProperties.EditableText]?.text
        assertEquals("Avans salariu", editableText)
        composeTestRule.onAllNodesWithTag("tx_description_suggestion_0").assertCountEquals(0)
    }

    @Test
    fun test13_composeUi_savingTransactionRetainsSelectedDescriptionAndOtherFields() {
        val sampleCategories = listOf(
            CategoryEntity(name = "🍉 Food & Dining", type = "Expense", subCategory = "Groceries")
        )

        var savedDescription: String? = null
        var savedAmount: Double? = null
        var savedType: String? = null

        composeTestRule.setContent {
            TransactionFormDialog(
                initialTransaction = null,
                isDuplicateMode = false,
                categories = sampleCategories,
                onDismiss = {},
                onSearchDescriptions = { listOf("Avocado") },
                onSave = { _, _, desc, amt, type, _, _, _, _ ->
                    savedDescription = desc
                    savedAmount = amt
                    savedType = type
                }
            )
        }

        // Enter amount
        composeTestRule.onNodeWithTag("tx_input_amount").performTextInput("45.50")

        // Type "Av"
        composeTestRule.onNodeWithTag("tx_input_desc").performTextInput("Av")
        composeTestRule.mainClock.advanceTimeBy(300)
        composeTestRule.waitForIdle()

        // Tap suggestion
        composeTestRule.onNodeWithTag("tx_description_suggestion_0").performClick()
        composeTestRule.mainClock.advanceTimeBy(300)
        composeTestRule.waitForIdle()

        // Click save button
        composeTestRule.onNodeWithTag("save_transaction_button")
            .performScrollTo()
            .performClick()
        composeTestRule.waitForIdle()

        assertEquals("Avocado", savedDescription)
        assertEquals(45.50, savedAmount!!, 0.001)
        assertEquals("Expense", savedType)
    }
}
