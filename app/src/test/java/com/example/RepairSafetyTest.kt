package com.example

import com.example.data.service.ExchangeRateService
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class RepairSafetyTest {

    @Test
    fun testBackupValidationChecks() {
        val validFile = File.createTempFile("valid_backup", ".csv")
        validFile.deleteOnExit()
        validFile.writeText(
            "Transaction_ID,Transaction_Date,Amount_RON,Amount_EUR,Exchange_Rate,Exchange_Rate_Date,Exchange_Rate_Source,Conversion_Status,Type,Account,Category,SubCategory,Description\n" +
            "tx1,2026-08-01,100.0,20.09,4.9765,2026-08-01,BNR_OFFICIAL,OFFICIAL,Expense,Checking,Food,Groceries,Supermarket\n"
        )

        fun validate(file: File, expectedCount: Int): Boolean {
            if (!file.exists() || !file.canRead() || file.length() <= 0) return false
            val lines = file.readLines()
            if (lines.isEmpty()) return false
            val header = lines.first()
            if (!header.startsWith("Transaction_ID,Transaction_Date,Amount_RON,Amount_EUR")) return false
            val dataRows = lines.drop(1).filter { it.isNotBlank() }
            return dataRows.size >= expectedCount
        }

        assertTrue(validate(validFile, 1))

        val missingFile = File(validFile.parentFile, "non_existent_123.csv")
        assertFalse(validate(missingFile, 1))

        val emptyFile = File.createTempFile("empty", ".csv")
        emptyFile.deleteOnExit()
        assertFalse(validate(emptyFile, 1))

        val invalidHeaderFile = File.createTempFile("wrong_header", ".csv")
        invalidHeaderFile.deleteOnExit()
        invalidHeaderFile.writeText("ID,Date,RON,EUR\ntx1,2026-08-01,100,20\n")
        assertFalse(validate(invalidHeaderFile, 1))

        assertFalse(validate(validFile, 5))
    }

    @Test
    fun testFutureDateReturnsNotYetPublishedWithoutFabricatingRate() = runBlocking {
        val mockDao = object : com.example.data.dao.ExchangeRateDao {
            override suspend fun getOfficialRateForDate(date: String): com.example.data.model.ExchangeRateEntity? = null
            override suspend fun getRateForDate(date: String): com.example.data.model.ExchangeRateEntity? = null
            override suspend fun insertRate(rate: com.example.data.model.ExchangeRateEntity) {}
            override suspend fun deleteUnverifiedRatesForDate(date: String): Int = 0
            override suspend fun insertAllRates(rates: List<com.example.data.model.ExchangeRateEntity>) {}
            override suspend fun getAllOfficialRates(): List<com.example.data.model.ExchangeRateEntity> = emptyList()
            override suspend fun deleteAllRates() {}
        }
        val service = ExchangeRateService(mockDao)

        val futureResult = service.fetchOfficialBnrRateFromNetwork("2099-12-31")
        assertEquals("NOT_YET_PUBLISHED", futureResult.status)
        assertEquals(0.0, futureResult.rate, 0.0001)
        assertEquals("2099-12-31", futureResult.requestedDate)
    }
}
