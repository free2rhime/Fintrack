package com.example

import com.example.data.dao.ExchangeRateDao
import com.example.data.dao.TransactionDao
import com.example.data.model.ExchangeRateEntity
import com.example.data.model.TransactionEntity
import com.example.data.repository.RoomTransactionRepository
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
        assertEquals("XML_DOCUMENT_INVALID", invalidResult.status)

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
            override fun getAllTransactions(householdId: String?): Flow<List<TransactionEntity>> = MutableStateFlow(txStore.values.toList())
            override fun getTransactionsInRange(startDate: String, endDate: String, householdId: String?): Flow<List<TransactionEntity>> = MutableStateFlow(emptyList())
            override suspend fun getTransactionById(id: String): TransactionEntity? = txStore[id]
            override suspend fun getUnverifiedTransactions(): List<TransactionEntity> = emptyList()
            override suspend fun getRetryablePendingTransactions(): List<TransactionEntity> = getPendingTransactions()
            override suspend fun getPendingTransactions(): List<TransactionEntity> = txStore.values.filter {
                it.conversionStatus == "PENDING" || it.conversionStatus?.startsWith("PENDING_") == true ||
                it.conversionStatus == "FAILED" || it.conversionStatus?.startsWith("FAILED_") == true
            }
            override suspend fun getAllTransactionsList(householdId: String?): List<TransactionEntity> = txStore.values.toList()
            override suspend fun getDescriptionSuggestions(query: String, limit: Int, householdId: String?): List<String> = emptyList()
            override suspend fun insertTransaction(transaction: TransactionEntity) { txStore[transaction.id] = transaction }
            override suspend fun insertAllTransactions(transactions: List<TransactionEntity>) { transactions.forEach { txStore[it.id] = it } }
            override suspend fun deleteTransaction(transaction: TransactionEntity) { txStore.remove(transaction.id) }
            override suspend fun deleteTransactionById(id: String) { txStore.remove(id) }
            override suspend fun deleteAllTransactions(householdId: String?) { txStore.clear() }
            override suspend fun deleteTransactionsByHousehold(householdId: String?) { txStore.clear() }
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
        val repo = RoomTransactionRepository(
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
            override fun getAllTransactions(householdId: String?): Flow<List<TransactionEntity>> = MutableStateFlow(txStore.values.toList())
            override fun getTransactionsInRange(startDate: String, endDate: String, householdId: String?): Flow<List<TransactionEntity>> = MutableStateFlow(emptyList())
            override suspend fun getTransactionById(id: String): TransactionEntity? = txStore[id]
            override suspend fun getUnverifiedTransactions(): List<TransactionEntity> = emptyList()
            override suspend fun getRetryablePendingTransactions(): List<TransactionEntity> = getPendingTransactions()
            override suspend fun getPendingTransactions(): List<TransactionEntity> = txStore.values.filter {
                it.conversionStatus == "PENDING" || it.conversionStatus?.startsWith("PENDING_") == true ||
                it.conversionStatus == "FAILED" || it.conversionStatus?.startsWith("FAILED_") == true
            }
            override suspend fun getAllTransactionsList(householdId: String?): List<TransactionEntity> = txStore.values.toList()
            override suspend fun getDescriptionSuggestions(query: String, limit: Int, householdId: String?): List<String> = emptyList()
            override suspend fun insertTransaction(transaction: TransactionEntity) { txStore[transaction.id] = transaction }
            override suspend fun insertAllTransactions(transactions: List<TransactionEntity>) { transactions.forEach { txStore[it.id] = it } }
            override suspend fun deleteTransaction(transaction: TransactionEntity) { txStore.remove(transaction.id) }
            override suspend fun deleteTransactionById(id: String) { txStore.remove(id) }
            override suspend fun deleteAllTransactions(householdId: String?) { txStore.clear() }
            override suspend fun deleteTransactionsByHousehold(householdId: String?) { txStore.clear() }
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
        val repo = RoomTransactionRepository(
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

    @Test
    fun testRealBnrCurrentFeedStructure() {
        val xml = """
            <?xml version="1.0" encoding="utf-8"?>
            <DataSet xmlns="http://www.bnr.ro/xsd" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://www.bnr.ro/xsd nbrfxrates.xsd">
            	<Header>
            		<Publisher>National Bank of Romania</Publisher>
            		<PublishingDate>2026-08-07</PublishingDate>
            		<MessageType>DR</MessageType>
            	</Header>
            	<Body>
            		<Subject>Exchange Rates</Subject>
            		<OrigDoc>NBRFXRATES</OrigDoc>
            		<Cube date="2026-08-07">
            			<Rate currency="AED">1.2185</Rate>
            			<Rate currency="AUD">2.9512</Rate>
            			<Rate currency="USD">4.4752</Rate>
            			<Rate currency="EUR">4.9775</Rate>
            		</Cube>
            	</Body>
            </DataSet>
        """.trimIndent()

        val parseRes = exchangeRateService.parseBnrXmlDetailed(xml)
        assertEquals("OFFICIAL_RATES_PARSED", parseRes.failureCategory)
        assertTrue(parseRes.publicationDatesParsed > 0)
        assertEquals("2026-08-07", parseRes.latestPublicationDate)
        assertEquals(1, parseRes.cubeElementCount)
        assertEquals(4, parseRes.rateElementCount)
        assertEquals(1, parseRes.eurRateElementCount)
        assertEquals(4.9775, parseRes.ratesMap["2026-08-07"]!!, 0.0001)
    }

    @Test
    fun testRealBnr10DayStructure() {
        val xml = """
            <?xml version="1.0" encoding="utf-8"?>
            <DataSet xmlns="http://www.bnr.ro/xsd">
            	<Header>
            		<Publisher>National Bank of Romania</Publisher>
            		<PublishingDate>2026-08-07</PublishingDate>
            	</Header>
            	<Body>
            		<Subject>10 days Exchange Rates</Subject>
            		<OrigDoc>NBRFXRATES</OrigDoc>
            		<Cube date="2026-08-07">
            			<Rate currency="EUR">4.9775</Rate>
            		</Cube>
            		<Cube date="2026-08-06">
            			<Rate currency="EUR">4.9765</Rate>
            		</Cube>
            		<Cube date="2026-08-05">
            			<Rate currency="EUR">4.9750</Rate>
            		</Cube>
            	</Body>
            </DataSet>
        """.trimIndent()

        val parseRes = exchangeRateService.parseBnrXmlDetailed(xml)
        assertEquals("OFFICIAL_RATES_PARSED", parseRes.failureCategory)
        assertEquals(3, parseRes.publicationDatesParsed)
        assertEquals("2026-08-07", parseRes.latestPublicationDate)
        assertEquals(3, parseRes.cubeElementCount)
        assertEquals(3, parseRes.eurRateElementCount)
        assertEquals(4.9775, parseRes.ratesMap["2026-08-07"]!!, 0.0001)
        assertEquals(4.9765, parseRes.ratesMap["2026-08-06"]!!, 0.0001)
        assertEquals(4.9750, parseRes.ratesMap["2026-08-05"]!!, 0.0001)
    }

    @Test
    fun testRealBnrYearlyArchiveStructure() {
        val xml = """
            <?xml version="1.0" encoding="utf-8"?>
            <DataSet xmlns="http://www.bnr.ro/xsd">
            	<Header>
            		<Publisher>National Bank of Romania</Publisher>
            		<PublishingDate>2026-08-07</PublishingDate>
            	</Header>
            	<Body>
            		<Cube date="2026-01-05">
            			<Rate currency="EUR">4.9730</Rate>
            		</Cube>
            		<Cube date="2026-08-07">
            			<Rate currency="EUR">4.9775</Rate>
            		</Cube>
            	</Body>
            </DataSet>
        """.trimIndent()

        val parseRes = exchangeRateService.parseBnrXmlDetailed(xml)
        assertEquals("OFFICIAL_RATES_PARSED", parseRes.failureCategory)
        assertEquals(2, parseRes.publicationDatesParsed)
        assertEquals("2026-08-07", parseRes.latestPublicationDate)
        assertEquals(4.9730, parseRes.ratesMap["2026-01-05"]!!, 0.0001)
    }

    @Test
    fun testPrefixedNamespaceAndXmlDeclaration() {
        val xml = """
            <?xml version="1.0" encoding="utf-8"?>
            <bnr:DataSet xmlns:bnr="http://www.bnr.ro/xsd">
            	<bnr:Header>
            		<bnr:Publisher>National Bank of Romania</bnr:Publisher>
            	</bnr:Header>
            	<bnr:Body>
            		<bnr:Cube date="2026-08-07">
            			<bnr:Rate currency="EUR">4.9775</bnr:Rate>
            		</bnr:Cube>
            	</bnr:Body>
            </bnr:DataSet>
        """.trimIndent()

        val parseRes = exchangeRateService.parseBnrXmlDetailed(xml)
        assertEquals("OFFICIAL_RATES_PARSED", parseRes.failureCategory)
        assertTrue(parseRes.hasXmlDeclaration)
        assertEquals("DataSet", parseRes.rootLocalName)
        assertEquals(1, parseRes.publicationDatesParsed)
        assertEquals(4.9775, parseRes.ratesMap["2026-08-07"]!!, 0.0001)
    }

    @Test
    fun testHtmlResponseWithHttp200() {
        val html = """
            <!DOCTYPE html>
            <html>
            <head><title>302 Found</title></head>
            <body><h1>Redirecting...</h1></body>
            </html>
        """.trimIndent()

        val parseRes = exchangeRateService.parseBnrXmlDetailed(html)
        assertEquals("RESPONSE_IS_HTML", parseRes.failureCategory)
        assertEquals(0, parseRes.publicationDatesParsed)
        assertFalse(parseRes.stageC_xmlOpened)
    }

    @Test
    fun testGenericContentTypeContainingValidXml() {
        val xml = """
            <?xml version="1.0" encoding="utf-8"?>
            <DataSet xmlns="http://www.bnr.ro/xsd">
            	<Body>
            		<Cube date="2026-08-07">
            			<Rate currency="EUR">4.9775</Rate>
            		</Cube>
            	</Body>
            </DataSet>
        """.trimIndent()

        val service = ExchangeRateService(
            exchangeRateDao = mockRateDao,
            detailedHttpFetcher = { url ->
                com.example.data.service.HttpResponseData(
                    requestedUrl = url,
                    finalUrl = url,
                    httpStatus = "200",
                    contentType = "application/octet-stream",
                    contentEncoding = null,
                    byteCount = xml.toByteArray().size,
                    content = xml,
                    isHtml = false,
                    isGenericXml = true
                )
            }
        )

        val diag = runBlocking { service.runDebugDiagnostic() }
        assertTrue(diag.isReachable)
        assertEquals("200", diag.httpStatus)
        assertEquals("OFFICIAL_RATES_PARSED", diag.failureCategory)
        assertEquals(1, diag.publicationDatesParsed)
        assertTrue(diag.eurRateFound)
        assertEquals("2026-08-07", diag.latestPublicationDate)
    }

    @Test
    fun testEmptyResponseAndMissingElements() {
        val emptyParse = exchangeRateService.parseBnrXmlDetailed("")
        assertEquals("EMPTY_RESPONSE", emptyParse.failureCategory)

        val noCubeXml = "<DataSet><Body></Body></DataSet>"
        val noCubeParse = exchangeRateService.parseBnrXmlDetailed(noCubeXml)
        assertEquals("CUBE_NOT_FOUND", noCubeParse.failureCategory)

        val cubeNoDateXml = "<DataSet><Body><Cube><Rate currency=\"EUR\">4.97</Rate></Cube></Body></DataSet>"
        val cubeNoDateParse = exchangeRateService.parseBnrXmlDetailed(cubeNoDateXml)
        assertEquals("DATED_CUBE_NOT_FOUND", cubeNoDateParse.failureCategory)

        val noRateXml = "<DataSet><Body><Cube date=\"2026-08-07\"></Cube></Body></DataSet>"
        val noRateParse = exchangeRateService.parseBnrXmlDetailed(noRateXml)
        assertEquals("RATE_NOT_FOUND", noRateParse.failureCategory)

        val noEurXml = "<DataSet><Body><Cube date=\"2026-08-07\"><Rate currency=\"USD\">4.50</Rate></Cube></Body></DataSet>"
        val noEurParse = exchangeRateService.parseBnrXmlDetailed(noEurXml)
        assertEquals("EUR_RATE_NOT_FOUND", noEurParse.failureCategory)
    }

    @Test
    fun testEurWhitespaceMultiplierAndInvalidNumber() {
        val whitespaceXml = """
            <DataSet>
                <Body>
                    <Cube date="2026-08-07">
                        <Rate currency=" eur "> 4.9775 </Rate>
                        <Rate currency="JPY" multiplier="100"> 291.23 </Rate>
                    </Cube>
                </Body>
            </DataSet>
        """.trimIndent()

        val parseRes = exchangeRateService.parseBnrXmlDetailed(whitespaceXml)
        assertEquals("OFFICIAL_RATES_PARSED", parseRes.failureCategory)
        assertEquals(4.9775, parseRes.ratesMap["2026-08-07"]!!, 0.0001)

        val invalidNumberXml = """
            <DataSet>
                <Body>
                    <Cube date="2026-08-07">
                        <Rate currency="EUR">N/A</Rate>
                    </Cube>
                </Body>
            </DataSet>
        """.trimIndent()

        val invalidParse = exchangeRateService.parseBnrXmlDetailed(invalidNumberXml)
        assertEquals("INVALID_EUR_VALUE", invalidParse.failureCategory)
    }

    @Test
    fun testSecureParserConfiguration() {
        val xxeXml = """
            <?xml version="1.0" encoding="utf-8"?>
            <!DOCTYPE DataSet [
                <!ENTITY xxe SYSTEM "http://127.0.0.1/evil">
            ]>
            <DataSet xmlns="http://www.bnr.ro/xsd">
            	<Body>
            		<Cube date="2026-08-07">
            			<Rate currency="EUR">4.9775</Rate>
            		</Cube>
            	</Body>
            </DataSet>
        """.trimIndent()

        val parseRes = exchangeRateService.parseBnrXmlDetailed(xxeXml)
        assertEquals("OFFICIAL_RATES_PARSED", parseRes.failureCategory)
        assertEquals(1, parseRes.publicationDatesParsed)
        assertEquals(4.9775, parseRes.ratesMap["2026-08-07"]!!, 0.0001)
    }

    @Test
    fun testMalformedXmlAndHtmlBodiesDoNotCrashDiagnosticOrService() {
        val htmlService = ExchangeRateService(
            mockRateDao,
            detailedHttpFetcher = { url ->
                com.example.data.service.HttpResponseData(
                    requestedUrl = url,
                    finalUrl = url,
                    httpStatus = "200",
                    contentType = "text/html; charset=invalid-charset",
                    contentEncoding = null,
                    byteCount = 20,
                    content = "<html>Error</html>",
                    isHtml = true
                )
            }
        )

        val diag = runBlocking { htmlService.runDebugDiagnostic() }
        assertEquals("200", diag.httpStatus)
        assertEquals("RESPONSE_IS_HTML", diag.failureCategory)
        assertFalse(diag.eurRateFound)

        val rateRes = runBlocking { htmlService.getOfficialRate("2026-08-05") }
        assertEquals("RESPONSE_IS_HTML", rateRes.status)
        assertEquals(0.0, rateRes.rate, 0.0001)
    }

    @Test
    fun testTransactionRepositorySaveTransactionSavesRonWhenBnrFails() {
        val mockTxDao = object : TransactionDao {
            val list = mutableListOf<TransactionEntity>()
            override fun getAllTransactions(householdId: String?): Flow<List<TransactionEntity>> = MutableStateFlow(list)
            override fun getTransactionsInRange(startDate: String, endDate: String, householdId: String?): Flow<List<TransactionEntity>> = MutableStateFlow(list)
            override suspend fun getTransactionById(id: String): TransactionEntity? = list.find { it.id == id }
            override suspend fun getUnverifiedTransactions(): List<TransactionEntity> = emptyList()
            override suspend fun getRetryablePendingTransactions(): List<TransactionEntity> = list.filter { it.conversionStatus == "PENDING" }
            override suspend fun getPendingTransactions(): List<TransactionEntity> = list.filter { it.conversionStatus == "PENDING" }
            override suspend fun getAllTransactionsList(householdId: String?): List<TransactionEntity> = list
            override suspend fun getDescriptionSuggestions(query: String, limit: Int, householdId: String?): List<String> = emptyList()
            override suspend fun insertTransaction(transaction: TransactionEntity) { list.add(transaction) }
            override suspend fun insertAllTransactions(transactions: List<TransactionEntity>) { list.addAll(transactions) }
            override suspend fun deleteTransaction(transaction: TransactionEntity) { list.remove(transaction) }
            override suspend fun deleteTransactionById(id: String) { list.removeAll { it.id == id } }
            override suspend fun deleteAllTransactions(householdId: String?) { list.clear() }
            override suspend fun deleteTransactionsByHousehold(householdId: String?) { list.clear() }
        }

        val crashingService = ExchangeRateService(
            mockRateDao,
            httpFetcher = { Pair("Malformed <XML <Unclosed", "200") }
        )

        val mockDb = object : androidx.room.RoomDatabase() {
            override fun createOpenHelper(config: androidx.room.DatabaseConfiguration): androidx.sqlite.db.SupportSQLiteOpenHelper {
                throw UnsupportedOperationException("Not needed in test")
            }
            override fun createInvalidationTracker(): androidx.room.InvalidationTracker {
                throw UnsupportedOperationException("Not needed in test")
            }
            override fun clearAllTables() {}
        }

        val repo = RoomTransactionRepository(
            transactionDao = mockTxDao,
            exchangeRateService = crashingService,
            exchangeRateDao = mockRateDao,
            database = mockDb
        )

        val saved = runBlocking {
            repo.saveTransaction(
                id = "tx123",
                date = "2026-08-05",
                description = "Grocery Store",
                amountRON = 100.0,
                type = "Expense",
                account = "Cash",
                category = "Food",
                subCategory = "Groceries"
            )
        }

        assertEquals("tx123", saved.id)
        assertEquals(100.0, saved.amountRON, 0.001)
        assertEquals(0.0, saved.amountEUR, 0.001)
        assertEquals(0.0, saved.exchangeRate, 0.001)
        assertEquals("NONE", saved.exchangeRateSource)
        assertEquals("PENDING", saved.conversionStatus)
        assertEquals(1, mockTxDao.list.size)
    }
}
