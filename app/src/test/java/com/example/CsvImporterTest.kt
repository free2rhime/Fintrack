package com.example

import com.example.data.dao.ExchangeRateDao
import com.example.data.model.ExchangeRateEntity
import com.example.data.service.BnrRateResult
import com.example.data.service.ExchangeRateService
import com.example.data.util.CsvImporter
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class CsvImporterTest {

    private val fakeDao = object : ExchangeRateDao {
        override suspend fun getOfficialRateForDate(date: String): ExchangeRateEntity? {
            return ExchangeRateEntity(
                date = date,
                requestedDate = date,
                effectiveDate = date,
                rate = 4.9750,
                source = "BNR_OFFICIAL",
                fetchedAt = System.currentTimeMillis(),
                status = "OFFICIAL"
            )
        }

        override suspend fun getRateForDate(date: String): ExchangeRateEntity? {
            return getOfficialRateForDate(date)
        }

        override suspend fun insertRate(rate: ExchangeRateEntity) {}
        override suspend fun insertAllRates(rates: List<ExchangeRateEntity>) {}
        override suspend fun deleteUnverifiedRatesForDate(date: String) {}
    }

    private val exchangeRateService = ExchangeRateService(fakeDao)

    @Test
    fun testParseCsvLineWithQuotes() {
        val line = """123,2026-03-01,100.0,20.10,4.975,2026-03-01,2026-03-01,BNR_OFFICIAL,OFFICIAL,"Grocery, Store",Expense,Card,"🍉 Food & Dining","Supermarket",Bubu"""
        val tokens = CsvImporter.parseCsvLine(line)
        assertEquals(15, tokens.size)
        assertEquals("Grocery, Store", tokens[9])
        assertEquals("Bubu", tokens[14])
    }

    @Test
    fun testParseAndValidateCsv_ValidRows() = runBlocking {
        val csv = """Transaction_ID,Transaction_Date,Amount_RON,Amount_EUR,Exchange_Rate,Requested_Rate_Date,Effective_BNR_Rate_Date,Exchange_Rate_Source,Conversion_Status,Description,Type,Account,Category,SubCategory,Destination
TX100,2026-03-01,500.0,100.5,4.975,2026-03-01,2026-03-01,BNR_OFFICIAL,OFFICIAL,Bonus Salary,Income,Card,💼 Salary,Work Bonus,Bubu
TX101,2026-03-02,150.0,30.15,4.975,2026-03-02,2026-03-02,BNR_OFFICIAL,OFFICIAL,Weekly Groceries,Expense,Card,🍉 Food & Dining,Groceries,
"""
        val result = CsvImporter.parseAndValidateCsv(
            csvContent = csv,
            existingIds = emptySet(),
            exchangeRateService = exchangeRateService
        )

        assertEquals(2, result.transactionsToInsert.size)
        assertEquals(2, result.insertedCount)
        assertEquals(0, result.updatedCount)
        assertEquals(0, result.skippedCount)

        val incomeTx = result.transactionsToInsert.find { it.id == "TX100" }
        assertNotNull(incomeTx)
        assertEquals("Bubu", incomeTx?.destination)
        assertEquals("Income", incomeTx?.type)

        val expenseTx = result.transactionsToInsert.find { it.id == "TX101" }
        assertNotNull(expenseTx)
        assertNull(expenseTx?.destination)
        assertEquals("Expense", expenseTx?.type)
    }

    @Test
    fun testParseAndValidateCsv_IdempotencyWithExistingId() = runBlocking {
        val csv = """Transaction_ID,Transaction_Date,Amount_RON,Amount_EUR,Exchange_Rate,Requested_Rate_Date,Effective_BNR_Rate_Date,Exchange_Rate_Source,Conversion_Status,Description,Type,Account,Category,SubCategory,Destination
TX100,2026-03-01,600.0,120.6,4.975,2026-03-01,2026-03-01,BNR_OFFICIAL,OFFICIAL,Updated Bonus,Income,Card,💼 Salary,Work Bonus,Piticania
"""
        val result = CsvImporter.parseAndValidateCsv(
            csvContent = csv,
            existingIds = setOf("TX100"),
            exchangeRateService = exchangeRateService
        )

        assertEquals(1, result.transactionsToInsert.size)
        assertEquals(0, result.insertedCount)
        assertEquals(1, result.updatedCount)
        assertEquals(0, result.skippedCount)
        assertEquals("Piticania", result.transactionsToInsert[0].destination)
    }

    @Test
    fun testParseAndValidateCsv_SkipInvalidRows() = runBlocking {
        val csv = """Transaction_ID,Transaction_Date,Amount_RON,Amount_EUR,Exchange_Rate,Requested_Rate_Date,Effective_BNR_Rate_Date,Exchange_Rate_Source,Conversion_Status,Description,Type,Account,Category,SubCategory,Destination
TX200,invalid-date,100.0,20.0,5.0,2026-03-01,2026-03-01,BNR_OFFICIAL,OFFICIAL,Test,Expense,Card,Food,Food,
TX201,2026-03-01,-50.0,10.0,5.0,2026-03-01,2026-03-01,BNR_OFFICIAL,OFFICIAL,Negative Amt,Expense,Card,Food,Food,
TX202,2026-03-01,100.0,20.0,5.0,2026-03-01,2026-03-01,BNR_OFFICIAL,OFFICIAL,Valid Tx,Expense,Card,Food,Food,
"""
        val result = CsvImporter.parseAndValidateCsv(
            csvContent = csv,
            existingIds = emptySet(),
            exchangeRateService = exchangeRateService
        )

        assertEquals(1, result.transactionsToInsert.size)
        assertEquals(2, result.skippedCount)
        assertEquals("TX202", result.transactionsToInsert[0].id)
    }
}
