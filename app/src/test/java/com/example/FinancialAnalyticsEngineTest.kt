package com.example

import com.example.data.model.FilterSettings
import com.example.data.model.TransactionEntity
import com.example.domain.analytics.FinancialAnalyticsEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FinancialAnalyticsEngineTest {

    private fun createTx(
        id: String,
        type: String,
        amountRON: Double,
        amountEUR: Double,
        status: String = "OFFICIAL",
        source: String = "BNR_OFFICIAL"
    ): TransactionEntity {
        return TransactionEntity(
            id = id,
            date = "2026-08-01",
            description = "Test Tx $id",
            amountRON = amountRON,
            amountEUR = amountEUR,
            exchangeRate = if (amountEUR > 0) amountRON / amountEUR else 0.0,
            exchangeRateDate = "2026-08-01",
            exchangeRateSource = source,
            conversionStatus = status,
            type = type,
            account = "Checking",
            category = "Food",
            subCategory = "Groceries"
        )
    }

    @Test
    fun testEurAnalyticsExcludesPendingAndUnverifiedTransactions() {
        val txList = listOf(
            createTx("1", "Income", 1000.0, 200.0, "OFFICIAL", "BNR_OFFICIAL"),
            createTx("2", "Expense", 500.0, 100.0, "OFFICIAL", "BNR_OFFICIAL"),
            createTx("3", "Expense", 250.0, 0.0, "PENDING", "BNR_OFFICIAL"),
            createTx("4", "Expense", 250.0, 50.0, "UNVERIFIED", "SYNTHETIC")
        )

        // EUR Mode
        val metricsEur = FinancialAnalyticsEngine.calculateMetrics(txList, "EUR", "Last Month")
        assertEquals(200.0, metricsEur.totalIncome, 0.01)
        assertEquals(100.0, metricsEur.totalExpense, 0.01)
        assertEquals(100.0, metricsEur.balance, 0.01)
        assertEquals(50.0, metricsEur.savingsRate, 0.01) // (200 - 100) / 200 * 100
        assertEquals(2, metricsEur.excludedNonOfficialCount)
        assertTrue(metricsEur.hasIncompleteEurData)
    }

    @Test
    fun testRonAnalyticsIncludesAllTransactions() {
        val txList = listOf(
            createTx("1", "Income", 1000.0, 200.0, "OFFICIAL", "BNR_OFFICIAL"),
            createTx("2", "Expense", 500.0, 100.0, "OFFICIAL", "BNR_OFFICIAL"),
            createTx("3", "Expense", 250.0, 0.0, "PENDING", "BNR_OFFICIAL"),
            createTx("4", "Expense", 250.0, 50.0, "UNVERIFIED", "SYNTHETIC")
        )

        // RON Mode
        val metricsRon = FinancialAnalyticsEngine.calculateMetrics(txList, "RON", "Last Month")
        assertEquals(1000.0, metricsRon.totalIncome, 0.01)
        assertEquals(1000.0, metricsRon.totalExpense, 0.01) // 500 + 250 + 250
        assertEquals(0.0, metricsRon.balance, 0.01)
        assertEquals(0, metricsRon.excludedNonOfficialCount)
        assertFalse(metricsRon.hasIncompleteEurData)
    }

    @Test
    fun testCategorySharesExcludesUnverifiedInEurMode() {
        val txList = listOf(
            createTx("1", "Expense", 500.0, 100.0, "OFFICIAL", "BNR_OFFICIAL"),
            createTx("2", "Expense", 250.0, 50.0, "UNVERIFIED", "SYNTHETIC")
        )

        val sharesEur = FinancialAnalyticsEngine.calculateCategoryShares(txList, "EUR", "Expense")
        assertEquals(1, sharesEur.size)
        assertEquals(100.0, sharesEur[0].totalAmount, 0.01)

        val sharesRon = FinancialAnalyticsEngine.calculateCategoryShares(txList, "RON", "Expense")
        assertEquals(1, sharesRon.size)
        assertEquals(750.0, sharesRon[0].totalAmount, 0.01)
    }

    @Test
    fun testSearchFilteringSupportsDescriptionCategorySubCategoryAccountAndCaseInsensitive() {
        val tx1 = TransactionEntity(
            id = "1",
            date = "2026-08-01",
            description = "Mega Image Groceries",
            amountRON = 150.0,
            amountEUR = 30.0,
            exchangeRate = 5.0,
            exchangeRateDate = "2026-08-01",
            type = "Expense",
            account = "Checking",
            category = "Food",
            subCategory = "Groceries"
        )
        val tx2 = TransactionEntity(
            id = "2",
            date = "2026-08-05",
            description = "Monthly Electricity Bill",
            amountRON = 200.0,
            amountEUR = 40.0,
            exchangeRate = 5.0,
            exchangeRateDate = "2026-08-05",
            type = "Expense",
            account = "Revolut",
            category = "Utilities",
            subCategory = "Power"
        )
        val tx3 = TransactionEntity(
            id = "3",
            date = "2026-08-10",
            description = "Salary Bonus",
            amountRON = 5000.0,
            amountEUR = 1000.0,
            exchangeRate = 5.0,
            exchangeRateDate = "2026-08-10",
            type = "Income",
            account = "Savings",
            category = "Salary",
            subCategory = "Bonus"
        )
        val transactions = listOf(tx1, tx2, tx3)

        // 1. Search by description: "groceries" finds "Mega Image Groceries"
        val resDesc = FinancialAnalyticsEngine.filterTransactionsByPeriod(
            transactions,
            FilterSettings(selectedPeriod = "All Time", searchQuery = "groceries")
        )
        assertEquals(1, resDesc.size)
        assertEquals("1", resDesc[0].id)

        // 2. Search by category: "utilities" finds transaction with category "Utilities"
        val resCat = FinancialAnalyticsEngine.filterTransactionsByPeriod(
            transactions,
            FilterSettings(selectedPeriod = "All Time", searchQuery = "utilities")
        )
        assertEquals(1, resCat.size)
        assertEquals("2", resCat[0].id)

        // 3. Search by subCategory: "Power" finds transaction with subCategory "Power"
        val resSubCat = FinancialAnalyticsEngine.filterTransactionsByPeriod(
            transactions,
            FilterSettings(selectedPeriod = "All Time", searchQuery = "Power")
        )
        assertEquals(1, resSubCat.size)
        assertEquals("2", resSubCat[0].id)

        // 4. Search by account: "revolut" finds transaction with account "Revolut"
        val resAccount = FinancialAnalyticsEngine.filterTransactionsByPeriod(
            transactions,
            FilterSettings(selectedPeriod = "All Time", searchQuery = "revolut")
        )
        assertEquals(1, resAccount.size)
        assertEquals("2", resAccount[0].id)

        // 5. Case-insensitive matching: "mEgA", "UTILITIES", "bOnUs"
        val resCaseDesc = FinancialAnalyticsEngine.filterTransactionsByPeriod(
            transactions,
            FilterSettings(selectedPeriod = "All Time", searchQuery = "mEgA")
        )
        assertEquals(1, resCaseDesc.size)
        assertEquals("1", resCaseDesc[0].id)

        val resCaseCat = FinancialAnalyticsEngine.filterTransactionsByPeriod(
            transactions,
            FilterSettings(selectedPeriod = "All Time", searchQuery = "UTILITIES")
        )
        assertEquals(1, resCaseCat.size)
        assertEquals("2", resCaseCat[0].id)

        // 6. Non-matching query returns empty list
        val resNone = FinancialAnalyticsEngine.filterTransactionsByPeriod(
            transactions,
            FilterSettings(selectedPeriod = "All Time", searchQuery = "gym membership")
        )
        assertTrue(resNone.isEmpty())

        // 7. Empty search query returns all applicable transactions
        val resEmpty = FinancialAnalyticsEngine.filterTransactionsByPeriod(
            transactions,
            FilterSettings(selectedPeriod = "All Time", searchQuery = "")
        )
        assertEquals(3, resEmpty.size)

        // 8. Search remains combined with existing filters rather than replacing them
        // 8a. Combined with Type = "Income"
        val resCombinedType = FinancialAnalyticsEngine.filterTransactionsByPeriod(
            transactions,
            FilterSettings(selectedPeriod = "All Time", selectedType = "Income", searchQuery = "Salary")
        )
        assertEquals(1, resCombinedType.size)
        assertEquals("3", resCombinedType[0].id)

        // Search for "groceries" when type is Income -> 0 matches because tx1 is Expense
        val resTypeMismatch = FinancialAnalyticsEngine.filterTransactionsByPeriod(
            transactions,
            FilterSettings(selectedPeriod = "All Time", selectedType = "Income", searchQuery = "groceries")
        )
        assertTrue(resTypeMismatch.isEmpty())

        // 8b. Combined with Category filter
        val resCombinedCat = FinancialAnalyticsEngine.filterTransactionsByPeriod(
            transactions,
            FilterSettings(
                selectedPeriod = "All Time",
                selectedType = "Expense",
                selectedExpenseCategory = "Food",
                searchQuery = "groceries"
            )
        )
        assertEquals(1, resCombinedCat.size)
        assertEquals("1", resCombinedCat[0].id)

        // Search for "power" when category is Food -> 0 matches because tx2 is Utilities
        val resCatMismatch = FinancialAnalyticsEngine.filterTransactionsByPeriod(
            transactions,
            FilterSettings(
                selectedPeriod = "All Time",
                selectedType = "Expense",
                selectedExpenseCategory = "Food",
                searchQuery = "power"
            )
        )
        assertTrue(resCatMismatch.isEmpty())
    }
}
