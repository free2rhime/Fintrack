package com.example

import com.example.data.dao.ExchangeRateDao
import com.example.data.dao.TransactionDao
import com.example.data.model.ExchangeRateEntity
import com.example.data.model.TransactionEntity
import com.example.data.repository.TransactionRepository
import com.example.data.service.BnrRateResult
import com.example.data.service.ExchangeRateService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File

class BnrExchangeRateTest {

    private lateinit var mockRateDao: ExchangeRateDao
    private lateinit var exchangeRateService: ExchangeRateService

    @Before
    fun setUp() {
        mockRateDao = object : ExchangeRateDao {
            private val cache = mutableMapOf<String, ExchangeRateEntity>()
            override suspend fun getOfficialRateForDate(date: String): ExchangeRateEntity? = cache[date]
            override suspend fun getRateForDate(date: String): ExchangeRateEntity? = cache[date]
            override suspend fun insertRate(rate: ExchangeRateEntity) { cache[rate.date] = rate }
            override suspend fun deleteUnverifiedRatesForDate(date: String): Int = 0
            override suspend fun insertAllRates(rates: List<ExchangeRateEntity>) { rates.forEach { cache[it.date] = it } }
            override suspend fun getAllOfficialRates(): List<ExchangeRateEntity> = cache.values.filter { it.status == "OFFICIAL" }
            override suspend fun deleteAllRates() { cache.clear() }
        }
        exchangeRateService = ExchangeRateService(mockRateDao)
    }

    @Test
    fun testSuccessfulBnrResponse() {
        val sampleXml = """
            <?xml version="1.0" encoding="utf-8"?>
            <DataSet xmlns="http://www.bnr.ro/xsd">
                <Body>
                    <Cube date="2026-08-05">
                        <Rate currency="EUR">4.9765</Rate>
                        <Rate currency="USD">4.5120</Rate>
                    </Cube>
                </Body>
            </DataSet>
        """.trimIndent()

        val service = ExchangeRateService(mockRateDao, httpFetcher = { Pair(sampleXml, "200") })
        val result = runBlocking { service.getOfficialRate("2026-08-05") }

        assertEquals("OFFICIAL", result.status)
        assertEquals("BNR_OFFICIAL", result.source)
        assertEquals(4.9765, result.rate, 0.0001)
        assertEquals("2026-08-05", result.effectiveDate)
    }

    @Test
    fun testHttpFailureStatusHandling() {
        val service = ExchangeRateService(mockRateDao, httpFetcher = { Pair(null, "500") })
        val result = runBlocking { service.getOfficialRate("2026-08-05") }

        assertEquals("HTTP_ERROR", result.status)
        assertEquals(0.0, result.rate, 0.00001)
        assertNotEquals("BNR_OFFICIAL", result.source)
    }

    @Test
    fun testTimeoutAndNetworkFailureHandling() {
        val timeoutService = ExchangeRateService(mockRateDao, httpFetcher = { Pair(null, "TIMEOUT") })
        val timeoutResult = runBlocking { timeoutService.getOfficialRate("2026-08-05") }
        assertEquals("TIMEOUT", timeoutResult.status)

        val noNetService = ExchangeRateService(mockRateDao, httpFetcher = { Pair(null, "NO_NETWORK") })
        val noNetResult = runBlocking { noNetService.getOfficialRate("2026-08-05") }
        assertEquals("NO_NETWORK", noNetResult.status)
    }

    @Test
    fun testInvalidXmlAndEmptyXmlHandling() {
        val invalidXmlService = ExchangeRateService(mockRateDao, httpFetcher = { Pair("NOT_XML", "200") })
        val invalidResult = runBlocking { invalidXmlService.getOfficialRate("2026-08-05") }
        assertEquals("XML_PARSE_ERROR", invalidResult.status)

        val emptyXmlService = ExchangeRateService(mockRateDao, httpFetcher = { Pair(null, "EMPTY_RESPONSE") })
        val emptyResult = runBlocking { emptyXmlService.getOfficialRate("2026-08-05") }
        assertEquals("EMPTY_RESPONSE", emptyResult.status)
    }

    @Test
    fun testMissingEurRateHandling() {
        val noEurXml = """
            <?xml version="1.0" encoding="utf-8"?>
            <DataSet xmlns="http://www.bnr.ro/xsd">
                <Body>
                    <Cube date="2026-08-05">
                        <Rate currency="USD">4.5120</Rate>
                    </Cube>
                </Body>
            </DataSet>
        """.trimIndent()

        val service = ExchangeRateService(mockRateDao, httpFetcher = { Pair(noEurXml, "200") })
        val result = runBlocking { service.getOfficialRate("2026-08-05") }

        assertEquals("EUR_RATE_NOT_FOUND", result.status)
        assertEquals(0.0, result.rate, 0.00001)
    }

    @Test
    fun testWeekendFallbackLogic() {
        val sampleXml = """
            <?xml version="1.0" encoding="utf-8"?>
            <DataSet xmlns="http://www.bnr.ro/xsd">
                <Body>
                    <Cube date="2026-07-31">
                        <Rate currency="EUR">4.9760</Rate>
                    </Cube>
                </Body>
            </DataSet>
        """.trimIndent()

        val service = ExchangeRateService(mockRateDao, httpFetcher = { Pair(sampleXml, "200") })
        val result = runBlocking { service.getOfficialRate("2026-08-02") }

        assertEquals("OFFICIAL", result.status)
        assertEquals("2026-07-31", result.effectiveDate)
        assertEquals(4.9760, result.rate, 0.0001)
    }

    @Test
    fun testPreviousYearFallbackLogic() {
        val currYearXml = """
            <?xml version="1.0" encoding="utf-8"?>
            <DataSet xmlns="http://www.bnr.ro/xsd">
                <Body>
                    <Cube date="2026-01-05">
                        <Rate currency="EUR">4.9780</Rate>
                    </Cube>
                </Body>
            </DataSet>
        """.trimIndent()

        val prevYearXml = """
            <?xml version="1.0" encoding="utf-8"?>
            <DataSet xmlns="http://www.bnr.ro/xsd">
                <Body>
                    <Cube date="2025-12-31">
                        <Rate currency="EUR">4.9740</Rate>
                    </Cube>
                </Body>
            </DataSet>
        """.trimIndent()

        val service = ExchangeRateService(mockRateDao, httpFetcher = { url ->
            if (url.contains("2026")) Pair(currYearXml, "200")
            else if (url.contains("2025")) Pair(prevYearXml, "200")
            else Pair(null, "404")
        })

        val result = runBlocking { service.getOfficialRate("2026-01-01") }

        assertEquals("OFFICIAL", result.status)
        assertEquals("2025-12-31", result.effectiveDate)
        assertEquals(4.9740, result.rate, 0.0001)
    }

    @Test
    fun testFutureDateHandling() {
        val service = ExchangeRateService(mockRateDao)
        val result = runBlocking { service.getOfficialRate("2099-01-01") }

        assertEquals("NOT_YET_PUBLISHED", result.status)
        assertEquals(0.0, result.rate, 0.00001)
    }

    @Test
    fun testAmountEurCalculationAndRounding() {
        val amountRON = 1000.0
        val rate = 4.9765

        val calculated = ExchangeRateService.calculateAmountEUR(amountRON, rate)
        assertEquals(200.94, calculated, 0.001)
    }

    @Test
    fun testPendingToOfficialRetryAndRoomPersistence() {
        val txStore = mutableMapOf<String, TransactionEntity>()

        val mockTxDao = object : TransactionDao {
            override fun getAllTransactions(): Flow<List<TransactionEntity>> = MutableStateFlow(txStore.values.toList())
            override fun getTransactionsInRange(startDate: String, endDate: String): Flow<List<TransactionEntity>> = MutableStateFlow(emptyList())
            override suspend fun getTransactionById(id: String): TransactionEntity? = txStore[id]
            override suspend fun getUnverifiedTransactions(): List<TransactionEntity> = emptyList()
            override suspend fun getPendingTransactions(): List<TransactionEntity> = txStore.values.filter {
                it.conversionStatus == "PENDING" || it.conversionStatus?.startsWith("PENDING_") == true ||
                it.conversionStatus == "FAILED" || it.conversionStatus?.startsWith("FAILED_") == true
            }
            override suspend fun getAllTransactionsList(): List<TransactionEntity> = txStore.values.toList()
            override suspend fun getDescriptionSuggestions(query: String, limit: Int): List<String> = emptyList()
            override suspend fun insertTransaction(transaction: TransactionEntity) { txStore[transaction.id] = transaction }
            override suspend fun insertAllTransactions(transactions: List<TransactionEntity>) { transactions.forEach { txStore[it.id] = it } }
            override suspend fun deleteTransaction(transaction: TransactionEntity) { txStore.remove(transaction.id) }
            override suspend fun deleteTransactionById(id: String) { txStore.remove(id) }
            override suspend fun deleteAllTransactions() { txStore.clear() }
        }

        val pendingTx = TransactionEntity(
            id = "tx1",
            date = "2026-08-05",
            description = "Coffee",
            amountRON = 49.765,
            amountEUR = 0.0,
            category = "Food",
            subCategory = "Coffee",
            account = "Bank",
            type = "Expense",
            exchangeRate = 0.0,
            exchangeRateDate = "2026-08-05",
            exchangeRateSource = "NONE",
            conversionStatus = "PENDING",
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        txStore["tx1"] = pendingTx

        val sampleXml = """
            <?xml version="1.0" encoding="utf-8"?>
            <DataSet xmlns="http://www.bnr.ro/xsd">
                <Body>
                    <Cube date="2026-08-05">
                        <Rate currency="EUR">4.9765</Rate>
                    </Cube>
                </Body>
            </DataSet>
        """.trimIndent()

        val service = ExchangeRateService(mockRateDao, httpFetcher = { Pair(sampleXml, "200") })
        val repo = TransactionRepository(
            transactionDao = mockTxDao,
            exchangeRateService = service,
            exchangeRateDao = mockRateDao,
            database = object : androidx.room.RoomDatabase() {
                override fun createOpenHelper(config: androidx.room.DatabaseConfiguration) = throw UnsupportedOperationException()
                override fun createInvalidationTracker() = throw UnsupportedOperationException()
                override fun clearAllTables() {}
            }
        )

        val retryResult = runBlocking { repo.syncPendingConversions() }

        assertEquals(1, retryResult.pendingBefore)
        assertEquals(1, retryResult.convertedSuccessfully)
        assertEquals(0, retryResult.stillPending)
        assertEquals(0, retryResult.failedCount)

        val updated = txStore["tx1"]!!
        assertEquals("OFFICIAL", updated.conversionStatus)
        assertEquals("BNR_OFFICIAL", updated.exchangeRateSource)
        assertEquals(4.9765, updated.exchangeRate, 0.0001)
        assertEquals(10.0, updated.amountEUR, 0.001)
    }

    @Test
    fun testNoOfficialSourceAssignedOnFailure() {
        val txStore = mutableMapOf<String, TransactionEntity>()

        val mockTxDao = object : TransactionDao {
            override fun getAllTransactions(): Flow<List<TransactionEntity>> = MutableStateFlow(txStore.values.toList())
            override fun getTransactionsInRange(startDate: String, endDate: String): Flow<List<TransactionEntity>> = MutableStateFlow(emptyList())
            override suspend fun getTransactionById(id: String): TransactionEntity? = txStore[id]
            override suspend fun getUnverifiedTransactions(): List<TransactionEntity> = emptyList()
            override suspend fun getPendingTransactions(): List<TransactionEntity> = txStore.values.filter {
                it.conversionStatus == "PENDING" || it.conversionStatus?.startsWith("PENDING_") == true ||
                it.conversionStatus == "FAILED" || it.conversionStatus?.startsWith("FAILED_") == true
            }
            override suspend fun getAllTransactionsList(): List<TransactionEntity> = txStore.values.toList()
            override suspend fun getDescriptionSuggestions(query: String, limit: Int): List<String> = emptyList()
            override suspend fun insertTransaction(transaction: TransactionEntity) { txStore[transaction.id] = transaction }
            override suspend fun insertAllTransactions(transactions: List<TransactionEntity>) { transactions.forEach { txStore[it.id] = it } }
            override suspend fun deleteTransaction(transaction: TransactionEntity) { txStore.remove(transaction.id) }
            override suspend fun deleteTransactionById(id: String) { txStore.remove(id) }
            override suspend fun deleteAllTransactions() { txStore.clear() }
        }

        val pendingTx = TransactionEntity(
            id = "tx1",
            date = "2026-08-05",
            description = "Lunch",
            amountRON = 100.0,
            amountEUR = 0.0,
            category = "Food",
            subCategory = "Lunch",
            account = "Bank",
            type = "Expense",
            exchangeRate = 0.0,
            exchangeRateDate = "2026-08-05",
            exchangeRateSource = "NONE",
            conversionStatus = "PENDING",
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        txStore["tx1"] = pendingTx

        val service = ExchangeRateService(mockRateDao, httpFetcher = { Pair(null, "NO_NETWORK") })
        val repo = TransactionRepository(
            transactionDao = mockTxDao,
            exchangeRateService = service,
            exchangeRateDao = mockRateDao,
            database = object : androidx.room.RoomDatabase() {
                override fun createOpenHelper(config: androidx.room.DatabaseConfiguration) = throw UnsupportedOperationException()
                override fun createInvalidationTracker() = throw UnsupportedOperationException()
                override fun clearAllTables() {}
            }
        )

        val retryResult = runBlocking { repo.syncPendingConversions() }

        assertEquals(1, retryResult.pendingBefore)
        assertEquals(0, retryResult.convertedSuccessfully)
        assertEquals(1, retryResult.stillPending)
        assertEquals(1, retryResult.failedCount)
        assertEquals("Network Unavailable", retryResult.mainFailureReason)

        val updated = txStore["tx1"]!!
        assertNotEquals("OFFICIAL", updated.conversionStatus)
        assertNotEquals("BNR_OFFICIAL", updated.exchangeRateSource)
    }

    @Test
    fun testMergedDebugManifestContainsInternetPermission() {
        val manifestFile = File("src/main/AndroidManifest.xml")
        assertTrue(manifestFile.exists())
        val content = manifestFile.readText()
        assertTrue(content.contains("android.permission.INTERNET"))
    }
}
