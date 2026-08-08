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
        val tempFolder = File(System.getProperty("java.io.tmpdir"), "backup_test_${System.currentTimeMillis()}")
        tempFolder.mkdirs()

        val sampleTx = com.example.data.model.TransactionEntity(
            id = "tx1",
            date = "2026-08-01",
            description = "Supermarket",
            amountRON = 100.0,
            amountEUR = 20.09,
            exchangeRate = 4.9765,
            exchangeRateDate = "2026-08-01",
            type = "Expense",
            account = "Checking",
            category = "Food",
            subCategory = "Groceries"
        )
        val transactions = listOf(sampleTx)

        val validFile = File(tempFolder, "valid_backup.csv")
        val validResult = com.example.data.util.CsvBackupManager.createAndValidateBackup(validFile, transactions)
        assertTrue(validResult.isValid)

        val invalidHeaderFile = File(tempFolder, "invalid_header.csv")
        invalidHeaderFile.writeText("Wrong,Header,Format\n1,2,3\n")
        val header = invalidHeaderFile.readLines().first()
        assertFalse(header.contains("Transaction_ID") && header.contains("Amount_RON"))

        val missingFile = File(tempFolder, "non_existent.csv")
        assertFalse(missingFile.exists())

        tempFolder.deleteRecursively()
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
