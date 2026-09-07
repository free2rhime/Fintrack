package com.example

import com.example.data.model.FilterSettings
import com.example.data.model.TransactionEntity
import com.example.domain.analytics.FinancialAnalyticsEngine
import java.util.Calendar
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

    @Test
    fun testGenerateContiguousMonthsForPeriodLast6Months() {
        // Fixed calendar for 2026-06-15 (June 2026)
        val fixedCal = Calendar.getInstance().apply {
            set(2026, Calendar.JUNE, 15)
        }
        val months = FinancialAnalyticsEngine.generateContiguousMonthsForPeriod(
            period = "Last 6 Months",
            referenceCal = fixedCal
        )
        assertEquals(6, months.size)
        assertEquals("2026-01", months.first())
        assertEquals("2026-06", months.last())
        assertEquals(listOf("2026-01", "2026-02", "2026-03", "2026-04", "2026-05", "2026-06"), months)
    }

    @Test
    fun testGenerateContiguousMonthsForPeriodLast3MonthsAndLast12Months() {
        val fixedCal = Calendar.getInstance().apply {
            set(2026, Calendar.JUNE, 15)
        }
        val months3 = FinancialAnalyticsEngine.generateContiguousMonthsForPeriod(
            period = "Last 3 Months",
            referenceCal = fixedCal
        )
        assertEquals(3, months3.size)
        assertEquals(listOf("2026-04", "2026-05", "2026-06"), months3)

        val months12 = FinancialAnalyticsEngine.generateContiguousMonthsForPeriod(
            period = "Last 12 Months",
            referenceCal = fixedCal
        )
        assertEquals(12, months12.size)
        assertEquals("2025-07", months12.first())
        assertEquals("2026-06", months12.last())
    }

    @Test
    fun testGenerateContiguousMonthsYearToDate() {
        val fixedCal = Calendar.getInstance().apply {
            set(2026, Calendar.APRIL, 10)
        }
        val ytdMonths = FinancialAnalyticsEngine.generateContiguousMonthsForPeriod(
            period = "Year To Date",
            referenceCal = fixedCal
        )
        assertEquals(4, ytdMonths.size)
        assertEquals(listOf("2026-01", "2026-02", "2026-03", "2026-04"), ytdMonths)
    }

    @Test
    fun testGenerateContiguousMonthsCustomRangeAndAllTime() {
        val customMonths = FinancialAnalyticsEngine.generateContiguousMonthsForPeriod(
            period = "Custom Range",
            customStart = "2026-02-10",
            customEnd = "2026-05-20"
        )
        assertEquals(4, customMonths.size)
        assertEquals(listOf("2026-02", "2026-03", "2026-04", "2026-05"), customMonths)

        val allTimeMonths = FinancialAnalyticsEngine.generateContiguousMonthsForPeriod(
            period = "All Time",
            referenceDates = listOf("2025-11-01", "2026-01-15", "2026-02-28")
        )
        assertEquals(4, allTimeMonths.size)
        assertEquals(listOf("2025-11", "2025-12", "2026-01", "2026-02"), allTimeMonths)
    }

    @Test
    fun testSingleSeriesZeroFillAndAverageDividedByAllMonths() {
        // Example from prompt:
        // Filter: Last 6 Months (Jan - Jun)
        // Transactions only in Jan (1500), Mar (1200), Jun (720) for "Groceries"
        // Expected: Jan=1500, Feb=0, Mar=1200, Apr=0, May=0, Jun=720
        // Total = 3420, Monthly Average = 3420 / 6 = 570 (NOT 3420 / 3)
        val txs = listOf(
            TransactionEntity(id = "1", date = "2026-01-10", description = "T1", amountRON = 1500.0, amountEUR = 300.0, exchangeRate = 5.0, exchangeRateDate = "2026-01-10", type = "Expense", account = "A", category = "Groceries", subCategory = "Food"),
            TransactionEntity(id = "2", date = "2026-03-05", description = "T2", amountRON = 1200.0, amountEUR = 240.0, exchangeRate = 5.0, exchangeRateDate = "2026-03-05", type = "Expense", account = "A", category = "Groceries", subCategory = "Food"),
            TransactionEntity(id = "3", date = "2026-06-20", description = "T3", amountRON = 720.0, amountEUR = 144.0, exchangeRate = 5.0, exchangeRateDate = "2026-06-20", type = "Expense", account = "A", category = "Groceries", subCategory = "Food")
        )
        val months = listOf("2026-01", "2026-02", "2026-03", "2026-04", "2026-05", "2026-06")

        val result = FinancialAnalyticsEngine.calculateSingleSeries(
            transactions = txs,
            contiguousYearMonths = months,
            currency = "RON",
            categoryFilter = "Groceries"
        )

        assertEquals(6, result.dataPoints.size)
        assertEquals(1500.0, result.dataPoints[0].value, 0.01)
        assertEquals(0.0, result.dataPoints[1].value, 0.01) // Feb zero-filled
        assertEquals(1200.0, result.dataPoints[2].value, 0.01)
        assertEquals(0.0, result.dataPoints[3].value, 0.01) // Apr zero-filled
        assertEquals(0.0, result.dataPoints[4].value, 0.01) // May zero-filled
        assertEquals(720.0, result.dataPoints[5].value, 0.01)

        assertEquals(3420.0, result.total, 0.01)
        assertEquals(570.0, result.monthlyAverage, 0.01) // exactly 3420 / 6
        assertEquals(6, result.monthCount)
    }

    @Test
    fun testExpenseCategoryRankingsSortedDescendingByShare() {
        val txs = listOf(
            TransactionEntity(id = "1", date = "2026-01-10", description = "Groceries", amountRON = 1000.0, amountEUR = 200.0, exchangeRate = 5.0, exchangeRateDate = "2026-01-10", type = "Expense", account = "A", category = "Groceries", subCategory = "Food"),
            TransactionEntity(id = "2", date = "2026-01-12", description = "Restaurants", amountRON = 500.0, amountEUR = 100.0, exchangeRate = 5.0, exchangeRateDate = "2026-01-12", type = "Expense", account = "A", category = "Restaurants", subCategory = "Food"),
            TransactionEntity(id = "3", date = "2026-01-15", description = "Transport", amountRON = 300.0, amountEUR = 60.0, exchangeRate = 5.0, exchangeRateDate = "2026-01-15", type = "Expense", account = "A", category = "Transport", subCategory = "Bus"),
            TransactionEntity(id = "4", date = "2026-01-18", description = "Bills", amountRON = 200.0, amountEUR = 40.0, exchangeRate = 5.0, exchangeRateDate = "2026-01-18", type = "Expense", account = "A", category = "Bills", subCategory = "Energy")
        )
        // Total expense = 2000. Groceries = 1000 (50%), Restaurants = 500 (25%), Transport = 300 (15%), Bills = 200 (10%)
        val rankings = FinancialAnalyticsEngine.calculateExpenseCategoryRankings(txs, "RON")
        assertEquals(4, rankings.size)
        assertEquals("Groceries", rankings[0].categoryName)
        assertEquals(50.0, rankings[0].percentage, 0.1)

        assertEquals("Restaurants", rankings[1].categoryName)
        assertEquals(25.0, rankings[1].percentage, 0.1)

        assertEquals("Transport", rankings[2].categoryName)
        assertEquals(15.0, rankings[2].percentage, 0.1)

        assertEquals("Bills", rankings[3].categoryName)
        assertEquals(10.0, rankings[3].percentage, 0.1)
    }

    @Test
    fun testIncomeSourceRankingsSortedDescendingByShare() {
        val txs = listOf(
            TransactionEntity(id = "1", date = "2026-01-10", description = "Salary", amountRON = 8000.0, amountEUR = 1600.0, exchangeRate = 5.0, exchangeRateDate = "2026-01-10", type = "Income", account = "A", category = "Salary", subCategory = "Main"),
            TransactionEntity(id = "2", date = "2026-01-12", description = "Freelance", amountRON = 1500.0, amountEUR = 300.0, exchangeRate = 5.0, exchangeRateDate = "2026-01-12", type = "Income", account = "A", category = "Freelance", subCategory = "Dev"),
            TransactionEntity(id = "3", date = "2026-01-15", description = "Investments", amountRON = 500.0, amountEUR = 100.0, exchangeRate = 5.0, exchangeRateDate = "2026-01-15", type = "Income", account = "A", category = "Investments", subCategory = "Dividends")
        )
        // Total income = 10000. Salary = 80%, Freelance = 15%, Investments = 5%
        val rankings = FinancialAnalyticsEngine.calculateIncomeSourceRankings(txs, "RON")
        assertEquals(3, rankings.size)
        assertEquals("Salary", rankings[0].categoryName)
        assertEquals(80.0, rankings[0].percentage, 0.1)

        assertEquals("Freelance", rankings[1].categoryName)
        assertEquals(15.0, rankings[1].percentage, 0.1)

        assertEquals("Investments", rankings[2].categoryName)
        assertEquals(5.0, rankings[2].percentage, 0.1)
    }

    @Test
    fun testSingleSeriesEurModeExcludesUnverifiedTransactions() {
        val txs = listOf(
            createTx("1", "Expense", 500.0, 100.0, "OFFICIAL", "BNR_OFFICIAL"),
            createTx("2", "Expense", 250.0, 50.0, "UNVERIFIED", "SYNTHETIC")
        )
        val months = listOf("2026-08")
        val result = FinancialAnalyticsEngine.calculateSingleSeries(
            transactions = txs,
            contiguousYearMonths = months,
            currency = "EUR",
            typeFilter = "Expense"
        )
        assertEquals(100.0, result.total, 0.01)
        assertEquals(100.0, result.monthlyAverage, 0.01)
    }
}
