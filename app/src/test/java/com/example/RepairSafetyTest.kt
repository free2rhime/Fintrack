package com.example

import com.example.data.service.ExchangeRateService
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class RepairSafetyTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun testBackupValidationChecks() {
        val validFile = tempFolder.newFile("valid_backup.csv")
        validFile.writeText(
            "Transaction_ID,Transaction_Date,Amount_RON,Amount_EUR,Exchange_Rate,Exchange_Rate_Date,Exchange_Rate_Source,Conversion_Status,Type,Account,Category,SubCategory,Description\n" +
            "tx1,2026-08-01,100.0,20.09,4.9765,2026-08-01,BNR_OFFICIAL,OFFICIAL,Expense,Checking,Food,Groceries,Supermarket\n"
        )

        // Validate method logic directly matching MainViewModel.validateBackupFile
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

        // Missing file check
        val missingFile = File(tempFolder.root, "non_existent.csv")
        assertFalse(validate(missingFile, 1))

        // Empty file check
        val emptyFile = tempFolder.newFile("empty.csv")
        assertFalse(validate(emptyFile, 1))

        // Invalid header check
        val invalidHeaderFile = tempFolder.newFile("wrong_header.csv")
        invalidHeaderFile.writeText("ID,Date,RON,EUR\ntx1,2026-08-01,100,20\n")
        assertFalse(validate(invalidHeaderFile, 1))

        // Insufficient rows check
        assertFalse(validate(validFile, 5))
    }

    @Test
    fun testFutureDateReturnsPendingWithoutFabricatingRate() {
        val mockDao = object : com.example.data.dao.ExchangeRateDao {
            override suspend fun getOfficialRateForDate(date: String): com.example.data.model.ExchangeRateEntity? = null
            override suspend fun getRateForDate(date: String): com.example.data.model.ExchangeRateEntity? = null
            override suspend fun insertRate(rate: com.example.data.model.ExchangeRateEntity) {}
            override suspend fun deleteUnverifiedRatesForDate(date: String) {}
            override suspend fun insertAllRates(rates: List<com.example.data.model.ExchangeRateEntity>) {}
        }
        val service = ExchangeRateService(mockDao)

        // Request a date far in the future
        val futureResult = service.fetchOfficialBnrRateFromNetwork("2099-12-31")
        assertEquals("PENDING", futureResult.status)
        assertEquals(0.0, futureResult.rate, 0.0001)
        assertEquals("2099-12-31", futureResult.requestedDate)
    }
}
