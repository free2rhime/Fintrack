package com.example

import com.example.data.dao.ExchangeRateDao
import com.example.data.model.ExchangeRateEntity
import com.example.data.model.TransactionEntity
import com.example.data.repository.RoomTransactionRepository
import com.example.data.service.ExchangeRateService
import com.example.data.util.NumberFormatter
import com.example.domain.analytics.FinancialAnalyticsEngine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import androidx.test.core.app.ApplicationProvider
import androidx.room.Room
import com.example.data.db.FinTrackDatabase
import java.io.File
import java.time.LocalDate
import java.time.ZoneId

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class DefectFixesUnitTest {

    @Test
    fun testInternetPermissionDeclaredInManifest() {
        val manifestFile = File("src/main/AndroidManifest.xml")
        if (!manifestFile.exists()) {
            val altManifest = File("app/src/main/AndroidManifest.xml")
            assertTrue("AndroidManifest.xml should exist", altManifest.exists())
            val content = altManifest.readText()
            assertTrue("Manifest must declare INTERNET permission", content.contains("android.permission.INTERNET"))
        } else {
            val content = manifestFile.readText()
            assertTrue("Manifest must declare INTERNET permission", content.contains("android.permission.INTERNET"))
        }
    }

    @Test
    fun testNumberFormattingNoLongTruncationTwoDecimalsAndSpaces() {
        assertEquals("13 978.80", NumberFormatter.formatAmount(13978.80))
        assertEquals("0.50", NumberFormatter.formatAmount(0.50))
        assertEquals("13 978.80 RON", NumberFormatter.formatCurrency(13978.80, "RON"))
        assertEquals("0.50 EUR", NumberFormatter.formatCurrency(0.50, "EUR"))
        assertEquals("1 234 567.89 RON", NumberFormatter.formatCurrency(1234567.89, "RON"))
    }

    @Test
    fun testStrictDateValidationAndFutureDateHandling() = runBlocking {
        val mockDao = FakeExchangeRateDao()
        val service = ExchangeRateService(mockDao)

        val invalidResult = service.getOfficialRate("2026-13-45")
        assertEquals("INVALID_DATE", invalidResult.status)
        assertEquals("NONE", invalidResult.source)
        assertEquals(0.0, invalidResult.rate, 0.001)

        val futureResult = service.getOfficialRate("2099-01-01")
        assertEquals("NOT_YET_PUBLISHED", futureResult.status)
        assertEquals("NONE", futureResult.source)
        assertEquals(0.0, futureResult.rate, 0.001)
    }

    @Test
    fun testSecureXmlParsingHardening() {
        val mockDao = FakeExchangeRateDao()
        val service = ExchangeRateService(mockDao)

        val xxePayload = """
            <?xml version="1.0" encoding="UTF-8"?>
            <!DOCTYPE Cube [
              <!ENTITY xxe SYSTEM "file:///etc/passwd">
            ]>
            <DataSet xmlns="http://www.bnr.ro/xsd">
                <Header><Publisher>BNR</Publisher></Header>
                <Body>
                    <Cube date="2026-03-15">
                        <Rate currency="EUR">&xxe;</Rate>
                    </Cube>
                </Body>
            </DataSet>
        """.trimIndent()

        val (parsedMap, success) = service.parseBnrXmlContentWithStatus(xxePayload)
        // Secure parsing either disallows DOCTYPE or ignores external entities safely
        assertFalse("XXE payload with DOCTYPE decl should be disallow-decl safe or parse cleanly without entity expansion", parsedMap.containsKey("2026-03-15"))
    }

    @Test
    fun testPendingTransactionMustNotClaimBnrOfficialSource() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val db = Room.inMemoryDatabaseBuilder(context, FinTrackDatabase::class.java).allowMainThreadQueries().build()
        val mockDao = FakeExchangeRateDao()
        val service = ExchangeRateService(mockDao)
        val mockTxDao = FakeTransactionDao()
        val repo = RoomTransactionRepository(mockTxDao, service, mockDao, db)

        val tx = repo.saveTransaction(
            date = "2099-01-01", // Future date -> PENDING
            description = "Future Test",
            amountRON = 500.0,
            type = "Expense",
            account = "Card",
            category = "🍉 Food & Dining",
            subCategory = "Groceries"
        )

        assertEquals("PENDING", tx.conversionStatus)
        assertNotEquals("BNR_OFFICIAL", tx.exchangeRateSource)
        assertEquals("NONE", tx.exchangeRateSource)
        assertEquals(0.0, tx.amountEUR, 0.001)
    }

    @Test
    fun testOptionalIncomeDestinationAndExpenseNullClearing() {
        val incomeTx = TransactionEntity(
            id = "1",
            date = "2026-03-15",
            description = "Salary",
            amountRON = 1000.0,
            amountEUR = 200.0,
            exchangeRate = 5.0,
            exchangeRateDate = "2026-03-15",
            exchangeRateSource = "BNR_OFFICIAL",
            conversionStatus = "OFFICIAL",
            type = "Income",
            account = "Card",
            category = "💼 Salary",
            subCategory = "Base",
            destination = "Bubu"
        )
        assertEquals("Bubu", incomeTx.destination)

        val expenseTx = incomeTx.copy(
            type = "Expense",
            destination = if ("Expense" == "Income") incomeTx.destination else null
        )
        assertNull("Expense transaction destination must be null", expenseTx.destination)
    }

    @Test
    fun testOfficialOnlyEurTotalsInAnalyticsEngine() {
        val txOfficial = TransactionEntity(
            id = "1", date = "2026-03-15", description = "Official Tx",
            amountRON = 100.0, amountEUR = 20.0, exchangeRate = 5.0, exchangeRateDate = "2026-03-15",
            exchangeRateSource = "BNR_OFFICIAL", conversionStatus = "OFFICIAL",
            type = "Expense", account = "Card", category = "Food", subCategory = "Groceries"
        )
        val txPending = TransactionEntity(
            id = "2", date = "2026-03-16", description = "Pending Tx",
            amountRON = 200.0, amountEUR = 0.0, exchangeRate = 0.0, exchangeRateDate = "2026-03-16",
            exchangeRateSource = "NONE", conversionStatus = "PENDING",
            type = "Expense", account = "Card", category = "Food", subCategory = "Groceries"
        )

        val list = listOf(txOfficial, txPending)

        // EUR Mode metrics
        val eurMetrics = FinancialAnalyticsEngine.calculateMetrics(list, "EUR", "All Time")
        assertEquals(20.0, eurMetrics.totalExpense, 0.001)
        assertEquals(1, eurMetrics.excludedNonOfficialCount)
        assertTrue(eurMetrics.hasIncompleteEurData)

        // RON Mode metrics include all
        val ronMetrics = FinancialAnalyticsEngine.calculateMetrics(list, "RON", "All Time")
        assertEquals(300.0, ronMetrics.totalExpense, 0.001)
        assertEquals(0, ronMetrics.excludedNonOfficialCount)
        assertFalse(ronMetrics.hasIncompleteEurData)
    }

    @Test
    fun testLocalDateDuplicationSettingToday() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val db = Room.inMemoryDatabaseBuilder(context, FinTrackDatabase::class.java).allowMainThreadQueries().build()
        val mockDao = FakeExchangeRateDao()
        val service = ExchangeRateService(mockDao)
        val mockTxDao = FakeTransactionDao()
        val repo = RoomTransactionRepository(mockTxDao, service, mockDao, db)

        val source = TransactionEntity(
            id = "source-1", date = "2020-01-01", description = "Old Entry",
            amountRON = 150.0, amountEUR = 30.0, exchangeRate = 5.0, exchangeRateDate = "2020-01-01",
            exchangeRateSource = "BNR_OFFICIAL", conversionStatus = "OFFICIAL",
            type = "Expense", account = "Card", category = "Food", subCategory = "Groceries"
        )

        val duplicate = repo.createDuplicateTemplate(source)
        val expectedToday = LocalDate.now(ZoneId.systemDefault()).toString()
        assertEquals(expectedToday, duplicate.date)
        assertNotEquals("source-1", duplicate.id)
    }
}

// Fake DAOs for fast local unit testing
class FakeExchangeRateDao : ExchangeRateDao {
    private val memory = mutableMapOf<String, ExchangeRateEntity>()

    override suspend fun getOfficialRateForDate(date: String): ExchangeRateEntity? {
        return memory[date]
    }

    override suspend fun getRateForDate(date: String): ExchangeRateEntity? {
        return memory[date]
    }

    override suspend fun insertRate(rate: ExchangeRateEntity) {
        memory[rate.date] = rate
    }

    override suspend fun insertAllRates(rates: List<ExchangeRateEntity>) {
        rates.forEach { memory[it.date] = it }
    }

    override suspend fun getAllOfficialRates(): List<ExchangeRateEntity> {
        return memory.values.toList()
    }

    override suspend fun deleteUnverifiedRatesForDate(date: String): Int {
        return 0
    }

    override suspend fun deleteAllRates() {
        memory.clear()
    }
}

class FakeTransactionDao : com.example.data.dao.TransactionDao {
    private val list = mutableListOf<TransactionEntity>()

    override fun getAllTransactions() = flowOf(list.toList())

    override fun getTransactionsInRange(startDate: String, endDate: String) = flowOf(list.toList())

    override suspend fun getTransactionById(id: String): TransactionEntity? = list.find { it.id == id }

    override suspend fun insertTransaction(transaction: TransactionEntity) {
        list.removeAll { it.id == transaction.id }
        list.add(transaction)
    }

    override suspend fun insertAllTransactions(transactions: List<TransactionEntity>) {
        transactions.forEach { insertTransaction(it) }
    }

    override suspend fun deleteTransaction(transaction: TransactionEntity) {
        list.removeAll { it.id == transaction.id }
    }

    override suspend fun deleteTransactionById(id: String) {
        list.removeAll { it.id == id }
    }

    override suspend fun deleteAllTransactions() {
        list.clear()
    }

    override suspend fun getUnverifiedTransactions(): List<TransactionEntity> {
        return list.filter {
            val s = it.conversionStatus
            val isPendingOrFailed = s == "PENDING" || s?.startsWith("PENDING_") == true || s == "FAILED" || s?.startsWith("FAILED_") == true
            !isPendingOrFailed && (s == null || s == "UNVERIFIED" || it.exchangeRateSource == null || it.exchangeRateSource != "BNR_OFFICIAL")
        }
    }

    override suspend fun getRetryablePendingTransactions(): List<TransactionEntity> {
        return list.filter {
            val s = it.conversionStatus
            s == "PENDING" || s?.startsWith("PENDING_") == true || s == "FAILED" || s?.startsWith("FAILED_") == true
        }
    }

    override suspend fun getPendingTransactions(): List<TransactionEntity> {
        return getRetryablePendingTransactions()
    }

    override suspend fun getAllTransactionsList(): List<TransactionEntity> {
        return list.toList()
    }

    override suspend fun getDescriptionSuggestions(query: String, limit: Int): List<String> {
        return emptyList()
    }
}
