package com.example

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
}
