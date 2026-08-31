package com.example

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.db.FinTrackDatabase
import com.example.data.model.CategoryEntity
import com.example.data.model.ExchangeRateEntity
import com.example.data.model.TransactionEntity
import com.example.data.repository.CategoryRepository
import com.example.data.repository.PendingRetryResult
import com.example.data.repository.PreparedRepairItem
import com.example.data.repository.RoomCategoryRepository
import com.example.data.repository.RoomTransactionRepository
import com.example.data.repository.TransactionRepository
import com.example.data.service.BnrDiagnosticResult
import com.example.data.service.BnrRateResult
import com.example.data.service.ExchangeRateService
import com.example.data.util.CsvDuplicateMode
import com.example.data.util.CsvExporter
import com.example.data.util.CsvImportFinalResult
import com.example.data.util.CsvImportOrchestrator
import com.example.data.util.CsvImportParseResult
import com.example.data.util.CsvPreviewData
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class CsvImportOrchestratorTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var app: Application
    private lateinit var cacheDir: File
    private lateinit var db: FinTrackDatabase
    private lateinit var roomTxRepo: RoomTransactionRepository
    private lateinit var roomCatRepo: RoomCategoryRepository

    @Before
    fun setUp() {
        app = ApplicationProvider.getApplicationContext()
        cacheDir = tempFolder.newFolder("cache")
        db = Room.inMemoryDatabaseBuilder(app, FinTrackDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val exchangeRateService = ExchangeRateService(db.exchangeRateDao())
        roomTxRepo = RoomTransactionRepository(
            transactionDao = db.transactionDao(),
            exchangeRateService = exchangeRateService,
            exchangeRateDao = db.exchangeRateDao(),
            syncOutboxDao = db.syncOutboxDao(),
            database = db
        )
        roomCatRepo = RoomCategoryRepository(
            categoryDao = db.categoryDao(),
            syncOutboxDao = db.syncOutboxDao(),
            database = db
        )
    }

    @After
    fun tearDown() {
        db.close()
    }

    // Mock repository for fine-grained call counting and rate mocking
    private class MockTestTransactionRepository(
        val rateResolver: (String) -> BnrRateResult = { date ->
            BnrRateResult(date, date, 5.0, "BNR_OFFICIAL", "OFFICIAL", "OK")
        }
    ) : TransactionRepository {
        val existingTransactions = mutableListOf<TransactionEntity>()
        val rateCalls = mutableListOf<String>()

        override val allTransactions: Flow<List<TransactionEntity>> = emptyFlow()
        override fun getTransactions(householdId: String?): Flow<List<TransactionEntity>> = emptyFlow()
        override fun getTransactionsInRange(startDate: String, endDate: String): Flow<List<TransactionEntity>> = emptyFlow()
        override suspend fun getTransactionById(id: String): TransactionEntity? = existingTransactions.find { it.id == id }
        override suspend fun saveTransaction(id: String?, date: String, description: String, amountRON: Double, type: String, account: String, category: String, subCategory: String, destination: String?, userId: String, householdId: String?): TransactionEntity = throw UnsupportedOperationException()
        override suspend fun createDuplicateTemplate(source: TransactionEntity): TransactionEntity = source
        override suspend fun getDescriptionSuggestions(query: String, limit: Int): List<String> = emptyList()
        override suspend fun insertBatchWithTransaction(transactions: List<TransactionEntity>) { existingTransactions.addAll(transactions) }
        override suspend fun getUnverifiedTransactions(): List<TransactionEntity> = emptyList()
        override suspend fun getAllTransactionsList(): List<TransactionEntity> = existingTransactions.toList()
        override suspend fun syncPendingConversions(): PendingRetryResult = PendingRetryResult(0, 0, 0, 0, null)
        override suspend fun applyRepairBatch(repairs: List<PreparedRepairItem>): Int = repairs.size
        override suspend fun applyRepairToTransactionAndCache(transaction: TransactionEntity, officialRate: Double, effectiveBnrDate: String) {}
        override suspend fun insertTransaction(transaction: TransactionEntity) { existingTransactions.add(transaction) }
        override suspend fun deleteTransaction(transaction: TransactionEntity) { existingTransactions.remove(transaction) }
        override suspend fun deleteTransactionById(id: String) { existingTransactions.removeAll { it.id == id } }
        override suspend fun deleteAllTransactions() { existingTransactions.clear() }
        override suspend fun insertBatch(transactions: List<TransactionEntity>) { existingTransactions.addAll(transactions) }

        override suspend fun getOfficialRate(date: String): BnrRateResult {
            rateCalls.add(date)
            return rateResolver(date)
        }

        override suspend fun runBnrDiagnostic(): BnrDiagnosticResult = BnrDiagnosticResult(isReachable = true, httpStatus = "200")
        override suspend fun executeAtomicCsvImport(previewData: CsvPreviewData, backupFile: File, allExistingTransactions: List<TransactionEntity>): CsvImportFinalResult {
            return CsvImportFinalResult(true, previewData.validTransactionsToImport.size, 0, 0, 0, 0, 0, 0, 0)
        }
    }

    private class MockTestCategoryRepository(
        val categories: List<CategoryEntity> = listOf(
            CategoryEntity(id = "c1", name = "Food", type = "Expense", subCategory = "Groceries")
        )
    ) : CategoryRepository {
        override val allCategories: Flow<List<CategoryEntity>> = emptyFlow()
        override fun getCategories(householdId: String?): Flow<List<CategoryEntity>> = emptyFlow()
        override suspend fun getAllCategoriesList(): List<CategoryEntity> = categories
        override suspend fun ensureDefaultCategoriesSeeded(householdId: String?, enqueueOutbox: Boolean) {}
        override suspend fun addCategory(name: String, type: String, subCategory: String, userId: String, householdId: String?) {}
        override suspend fun updateCategory(category: CategoryEntity) {}
        override suspend fun deleteCategory(category: CategoryEntity) {}
        override suspend fun updateCategoryGroup(oldName: String, newName: String, type: String, householdId: String?) {}
        override suspend fun deleteCategoryGroup(name: String, type: String, householdId: String?) {}
        override suspend fun updateSubcategory(id: String, newSubCategory: String) {}
        override suspend fun deleteSubcategory(id: String) {}
    }

    @Test
    fun test1_ronOnlyCsvAutomaticallyReceivesOfficialEurConversion() = runBlocking {
        val mockRepo = MockTestTransactionRepository { date ->
            BnrRateResult(date, date, 4.9750, "BNR_OFFICIAL", "OFFICIAL", "OK")
        }
        val orchestrator = CsvImportOrchestrator(mockRepo, MockTestCategoryRepository())

        val csv = """Transaction_Date,Amount_RON,Description,Type,Account,Category,SubCategory
2026-08-10,100.0,Groceries,Expense,Card,Food,Groceries
"""
        val result = orchestrator.parseAndValidateFromContent(csv)
        assertTrue(result is CsvImportParseResult.Success)
        val preview = (result as CsvImportParseResult.Success).preview

        assertEquals(1, preview.validRowsCount)
        assertEquals(1, preview.officialCount)
        assertEquals(0, preview.pendingCount)

        val tx = preview.validTransactionsToImport.first()
        assertEquals(20.10, tx.amountEUR, 0.001)
        assertEquals(4.9750, tx.exchangeRate, 0.0001)
        assertEquals("2026-08-10", tx.exchangeRateDate)
        assertEquals("BNR_OFFICIAL", tx.exchangeRateSource)
        assertEquals("OFFICIAL", tx.conversionStatus)
    }

    @Test
    fun test2_correctFormulaAmountEurUsingExchangeRateServiceCalculation() = runBlocking {
        val mockRepo = MockTestTransactionRepository { date ->
            BnrRateResult(date, date, 4.9765, "BNR_OFFICIAL", "OFFICIAL", "OK")
        }
        val orchestrator = CsvImportOrchestrator(mockRepo, MockTestCategoryRepository())

        val csv = """Transaction_Date,Amount_RON,Description,Type,Account,Category,SubCategory
2026-08-10,100.0,Groceries,Expense,Card,Food,Groceries
"""
        val result = orchestrator.parseAndValidateFromContent(csv) as CsvImportParseResult.Success
        val tx = result.preview.validTransactionsToImport.first()

        val expectedEur = ExchangeRateService.calculateAmountEUR(100.0, 4.9765)
        assertEquals(expectedEur, tx.amountEUR, 0.0001)
        assertEquals(20.09, tx.amountEUR, 0.001)
    }

    @Test
    fun test3_historicalTransactionDateUsedForRateResolution() = runBlocking {
        var requestedDateReceived = ""
        val mockRepo = MockTestTransactionRepository { date ->
            requestedDateReceived = date
            BnrRateResult(date, date, 4.9500, "BNR_OFFICIAL", "OFFICIAL", "OK")
        }
        val orchestrator = CsvImportOrchestrator(mockRepo, MockTestCategoryRepository())

        val csv = """Transaction_Date,Amount_RON,Description,Type,Account,Category,SubCategory
2024-05-15,200.0,Groceries,Expense,Card,Food,Groceries
"""
        orchestrator.parseAndValidateFromContent(csv)

        assertEquals("2024-05-15", requestedDateReceived)
    }

    @Test
    fun test4_weekendHolidayDateUsesEffectiveBnrRateResolution() = runBlocking {
        val mockRepo = MockTestTransactionRepository { date ->
            // Saturday 2026-08-01 resolves to Friday 2026-07-31 effective rate
            BnrRateResult(
                requestedDate = date,
                effectiveDate = "2026-07-31",
                rate = 4.9780,
                source = "BNR_OFFICIAL",
                status = "OFFICIAL"
            )
        }
        val orchestrator = CsvImportOrchestrator(mockRepo, MockTestCategoryRepository())

        val csv = """Transaction_Date,Amount_RON,Description,Type,Account,Category,SubCategory
2026-08-01,150.0,Groceries,Expense,Card,Food,Groceries
"""
        val result = orchestrator.parseAndValidateFromContent(csv) as CsvImportParseResult.Success
        val tx = result.preview.validTransactionsToImport.first()

        assertEquals("2026-08-01", tx.date)
        assertEquals("2026-07-31", tx.exchangeRateDate)
        assertEquals(4.9780, tx.exchangeRate, 0.0001)
        assertEquals("OFFICIAL", tx.conversionStatus)
        assertEquals("BNR_OFFICIAL", tx.exchangeRateSource)
    }

    @Test
    fun test5_multipleTransactionsSharingOneDateReuseOneResolvedRate() = runBlocking {
        val mockRepo = MockTestTransactionRepository()
        val orchestrator = CsvImportOrchestrator(mockRepo, MockTestCategoryRepository())

        val csv = """Transaction_Date,Amount_RON,Description,Type,Account,Category,SubCategory
2026-08-10,10.0,Item 1,Expense,Card,Food,Groceries
2026-08-10,20.0,Item 2,Expense,Card,Food,Groceries
2026-08-10,30.0,Item 3,Expense,Card,Food,Groceries
2026-08-10,40.0,Item 4,Expense,Card,Food,Groceries
2026-08-10,50.0,Item 5,Expense,Card,Food,Groceries
2026-08-11,10.0,Item 6,Expense,Card,Food,Groceries
2026-08-11,20.0,Item 7,Expense,Card,Food,Groceries
2026-08-11,30.0,Item 8,Expense,Card,Food,Groceries
"""
        val result = orchestrator.parseAndValidateFromContent(csv) as CsvImportParseResult.Success

        assertEquals(8, result.preview.validRowsCount)
        assertEquals(8, result.preview.officialCount)
        // Rate resolver should only have been called twice (once for 2026-08-10 and once for 2026-08-11)
        assertEquals(listOf("2026-08-10", "2026-08-11"), mockRepo.rateCalls)
    }

    @Test
    fun test6_multipleDatesResolveIndependently() = runBlocking {
        val mockRepo = MockTestTransactionRepository { date ->
            when (date) {
                "2026-08-10" -> BnrRateResult(date, date, 4.90, "BNR_OFFICIAL", "OFFICIAL")
                "2026-08-11" -> BnrRateResult(date, date, 5.00, "BNR_OFFICIAL", "OFFICIAL")
                else -> BnrRateResult(date, date, 0.0, "NONE", "TIMEOUT")
            }
        }
        val orchestrator = CsvImportOrchestrator(mockRepo, MockTestCategoryRepository())

        val csv = """Transaction_Date,Amount_RON,Description,Type,Account,Category,SubCategory
2026-08-10,490.0,Item 1,Expense,Card,Food,Groceries
2026-08-11,500.0,Item 2,Expense,Card,Food,Groceries
"""
        val result = orchestrator.parseAndValidateFromContent(csv) as CsvImportParseResult.Success
        val tx1 = result.preview.validTransactionsToImport[0]
        val tx2 = result.preview.validTransactionsToImport[1]

        assertEquals(100.0, tx1.amountEUR, 0.001)
        assertEquals(4.90, tx1.exchangeRate, 0.001)
        assertEquals(100.0, tx2.amountEUR, 0.001)
        assertEquals(5.00, tx2.exchangeRate, 0.001)
    }

    @Test
    fun test7_rateResolutionFailureProducesSafePendingState() = runBlocking {
        val mockRepo = MockTestTransactionRepository { date ->
            BnrRateResult(
                requestedDate = date,
                effectiveDate = date,
                rate = 0.0,
                source = "NONE",
                status = "TIMEOUT"
            )
        }
        val orchestrator = CsvImportOrchestrator(mockRepo, MockTestCategoryRepository())

        val csv = """Transaction_Date,Amount_RON,Description,Type,Account,Category,SubCategory
2026-08-10,100.0,Groceries,Expense,Card,Food,Groceries
"""
        val result = orchestrator.parseAndValidateFromContent(csv) as CsvImportParseResult.Success
        val tx = result.preview.validTransactionsToImport.first()

        assertEquals(0, result.preview.officialCount)
        assertEquals(1, result.preview.pendingCount)
        assertEquals(0.0, tx.amountEUR, 0.001)
        assertEquals(0.0, tx.exchangeRate, 0.001)
        assertEquals("2026-08-10", tx.exchangeRateDate)
        assertEquals("NONE", tx.exchangeRateSource)
        assertEquals("PENDING", tx.conversionStatus)
    }

    @Test
    fun test8_noFabricatedEurValueWhenRateResolutionFails() = runBlocking {
        val mockRepo = MockTestTransactionRepository { date ->
            BnrRateResult(date, date, 0.0, "NONE", "NO_NETWORK")
        }
        val orchestrator = CsvImportOrchestrator(mockRepo, MockTestCategoryRepository())

        val csv = """Transaction_Date,Amount_RON,Description,Type,Account,Category,SubCategory
2026-08-10,999.0,Groceries,Expense,Card,Food,Groceries
"""
        val result = orchestrator.parseAndValidateFromContent(csv) as CsvImportParseResult.Success
        val tx = result.preview.validTransactionsToImport.first()

        assertEquals(0.0, tx.amountEUR, 0.0)
        assertEquals(0.0, tx.exchangeRate, 0.0)
        assertEquals("PENDING", tx.conversionStatus)
    }

    @Test
    fun test9_existingExplicitOfficialCsvExchangeRateMetadataCompatible() = runBlocking {
        val mockRepo = MockTestTransactionRepository { date ->
            fail("Rate resolver should not be called when official metadata is already present in CSV")
            BnrRateResult(date, date, 0.0, "NONE", "NONE")
        }
        val orchestrator = CsvImportOrchestrator(mockRepo, MockTestCategoryRepository())

        val csv = """Transaction_ID,Transaction_Date,Amount_RON,Amount_EUR,Exchange_Rate,Requested_Rate_Date,Effective_BNR_Rate_Date,Exchange_Rate_Source,Conversion_Status,Description,Type,Account,Category,SubCategory,Destination
TX101,2026-08-10,500.0,100.0,5.0,2026-08-10,2026-08-10,BNR_OFFICIAL,OFFICIAL,Groceries,Expense,Card,Food,Groceries,
"""
        val result = orchestrator.parseAndValidateFromContent(csv) as CsvImportParseResult.Success
        val tx = result.preview.validTransactionsToImport.first()

        assertEquals(1, result.preview.officialCount)
        assertEquals(100.0, tx.amountEUR, 0.001)
        assertEquals(5.0, tx.exchangeRate, 0.001)
        assertEquals("BNR_OFFICIAL", tx.exchangeRateSource)
        assertEquals("OFFICIAL", tx.conversionStatus)
        assertTrue(mockRepo.rateCalls.isEmpty())
    }

    @Test
    fun test10_skipExistingPreservesCorrectConversion() = runBlocking {
        val existingTx = TransactionEntity(
            id = "tx_dup_1",
            date = "2026-08-10",
            description = "Existing",
            amountRON = 100.0,
            amountEUR = 20.0,
            exchangeRate = 5.0,
            exchangeRateDate = "2026-08-10",
            exchangeRateSource = "BNR_OFFICIAL",
            conversionStatus = "OFFICIAL",
            type = "Expense",
            account = "Card",
            category = "Food",
            subCategory = "Groceries"
        )
        val mockRepo = MockTestTransactionRepository().apply {
            existingTransactions.add(existingTx)
        }
        val orchestrator = CsvImportOrchestrator(mockRepo, MockTestCategoryRepository())

        val csv = """Transaction_ID,Transaction_Date,Amount_RON,Description,Type,Account,Category,SubCategory
tx_dup_1,2026-08-10,100.0,Existing,Expense,Card,Food,Groceries
"""
        val result = orchestrator.parseAndValidateFromContent(csv, CsvDuplicateMode.SKIP_EXISTING) as CsvImportParseResult.Success
        val preview = result.preview

        assertEquals(1, preview.existingIdsCount)
        assertEquals(1, preview.proposedSkipsCount)
        assertEquals(0, preview.proposedUpdatesCount)
        assertEquals(1, preview.validTransactionsToImport.size)
        assertEquals(20.0, preview.validTransactionsToImport[0].amountEUR, 0.001)
    }

    @Test
    fun test11_updateExistingPreservesCorrectConversion() = runBlocking {
        val existingTx = TransactionEntity(
            id = "tx_dup_1",
            date = "2026-08-10",
            description = "Existing",
            amountRON = 100.0,
            amountEUR = 20.0,
            exchangeRate = 5.0,
            exchangeRateDate = "2026-08-10",
            exchangeRateSource = "BNR_OFFICIAL",
            conversionStatus = "OFFICIAL",
            type = "Expense",
            account = "Card",
            category = "Food",
            subCategory = "Groceries"
        )
        val mockRepo = MockTestTransactionRepository().apply {
            existingTransactions.add(existingTx)
        }
        val orchestrator = CsvImportOrchestrator(mockRepo, MockTestCategoryRepository())

        val csv = """Transaction_ID,Transaction_Date,Amount_RON,Description,Type,Account,Category,SubCategory
tx_dup_1,2026-08-10,100.0,Existing,Expense,Card,Food,Groceries
"""
        val initialResult = orchestrator.parseAndValidateFromContent(csv, CsvDuplicateMode.SKIP_EXISTING) as CsvImportParseResult.Success
        val updatedPreview = orchestrator.updateDuplicateMode(initialResult.preview, CsvDuplicateMode.UPDATE_EXISTING)

        assertEquals(1, updatedPreview.existingIdsCount)
        assertEquals(0, updatedPreview.proposedSkipsCount)
        assertEquals(1, updatedPreview.proposedUpdatesCount)
        assertEquals(20.0, updatedPreview.validTransactionsToImport[0].amountEUR, 0.001)
        assertEquals("OFFICIAL", updatedPreview.validTransactionsToImport[0].conversionStatus)
    }

    @Test
    fun test12_previewContainsCalculatedEurValues() = runBlocking {
        val mockRepo = MockTestTransactionRepository { date ->
            BnrRateResult(date, date, 5.0, "BNR_OFFICIAL", "OFFICIAL", "OK")
        }
        val orchestrator = CsvImportOrchestrator(mockRepo, MockTestCategoryRepository())

        val csv = """Transaction_Date,Amount_RON,Description,Type,Account,Category,SubCategory
2026-08-10,250.0,Salary,Expense,Card,Food,Groceries
"""
        val result = orchestrator.parseAndValidateFromContent(csv) as CsvImportParseResult.Success
        val preview = result.preview

        assertEquals(1, preview.validRowsCount)
        assertEquals(1, preview.officialCount)
        assertEquals(50.0, preview.validTransactionsToImport.first().amountEUR, 0.001)
    }

    @Test
    fun test13_finalRoomImportPersistsCalculatedEurAndRateMetadata() = runBlocking {
        // Seed category in Room DB
        roomCatRepo.addCategory("Food", "Expense", "Groceries")
        // Cache rate in Room DB
        db.exchangeRateDao().insertRate(
            ExchangeRateEntity(
                date = "2026-08-10",
                requestedDate = "2026-08-10",
                effectiveDate = "2026-08-10",
                rate = 5.0,
                source = "BNR_OFFICIAL",
                status = "OFFICIAL",
                fetchedAt = System.currentTimeMillis()
            )
        )

        val orchestrator = CsvImportOrchestrator(roomTxRepo, roomCatRepo)
        val csv = """Transaction_ID,Transaction_Date,Amount_RON,Description,Type,Account,Category,SubCategory
tx_room_1,2026-08-10,500.0,Groceries,Expense,Card,Food,Groceries
"""
        val parseResult = orchestrator.parseAndValidateFromContent(csv) as CsvImportParseResult.Success
        val finalResult = orchestrator.executeImport(parseResult.preview, cacheDir)

        assertTrue(finalResult.success)
        assertEquals(1, finalResult.insertedCount)

        val savedTx = db.transactionDao().getTransactionById("tx_room_1")
        assertNotNull(savedTx)
        assertEquals(500.0, savedTx!!.amountRON, 0.001)
        assertEquals(100.0, savedTx.amountEUR, 0.001)
        assertEquals(5.0, savedTx.exchangeRate, 0.001)
        assertEquals("2026-08-10", savedTx.exchangeRateDate)
        assertEquals("BNR_OFFICIAL", savedTx.exchangeRateSource)
        assertEquals("OFFICIAL", savedTx.conversionStatus)
    }

    @Test
    fun test14_outboxEntriesGeneratedCorrectly() = runBlocking {
        roomCatRepo.addCategory("Food", "Expense", "Groceries")
        db.exchangeRateDao().insertRate(
            ExchangeRateEntity(
                date = "2026-08-10",
                requestedDate = "2026-08-10",
                effectiveDate = "2026-08-10",
                rate = 5.0,
                source = "BNR_OFFICIAL",
                status = "OFFICIAL",
                fetchedAt = System.currentTimeMillis()
            )
        )

        val orchestrator = CsvImportOrchestrator(roomTxRepo, roomCatRepo)
        val csv = """Transaction_ID,Transaction_Date,Amount_RON,Description,Type,Account,Category,SubCategory
tx_outbox_1,2026-08-10,250.0,Groceries,Expense,Card,Food,Groceries
"""
        val parseResult = orchestrator.parseAndValidateFromContent(csv) as CsvImportParseResult.Success
        orchestrator.executeImport(parseResult.preview, cacheDir)

        val outboxEntries = db.syncOutboxDao().getPendingEntries()
        val txEntry = outboxEntries.find { it.entityId == "tx_outbox_1" && it.entityType == "TRANSACTION" }
        assertNotNull(txEntry)
        assertEquals("UPSERT", txEntry!!.operation)
        assertEquals("PENDING", txEntry.status)
    }

    @Test
    fun test15_crossHouseholdIsolationIntact() = runBlocking {
        // Household isolation is preserved because CSV import creates transactions with default null or repository-bound household context
        roomCatRepo.addCategory("Food", "Expense", "Groceries")
        db.exchangeRateDao().insertRate(
            ExchangeRateEntity(
                date = "2026-08-10",
                requestedDate = "2026-08-10",
                effectiveDate = "2026-08-10",
                rate = 5.0,
                source = "BNR_OFFICIAL",
                status = "OFFICIAL",
                fetchedAt = System.currentTimeMillis()
            )
        )

        val orchestrator = CsvImportOrchestrator(roomTxRepo, roomCatRepo)
        val csv = """Transaction_ID,Transaction_Date,Amount_RON,Description,Type,Account,Category,SubCategory,Household_ID
tx_hh_1,2026-08-10,100.0,Groceries,Expense,Card,Food,Groceries,injected_untrusted_hh
"""
        val parseResult = orchestrator.parseAndValidateFromContent(csv) as CsvImportParseResult.Success
        orchestrator.executeImport(parseResult.preview, cacheDir)

        val tx = db.transactionDao().getTransactionById("tx_hh_1")
        assertNotNull(tx)
        // Ensure CSV cannot arbitrarily bind unauthorized foreign household ID into Room directly
        assertEquals(null, tx!!.householdId)
    }

    @Test
    fun test16_cancellationPropagatesCorrectly() = runBlocking {
        val mockRepo = MockTestTransactionRepository {
            throw CancellationException("Simulated coroutine cancellation")
        }
        val orchestrator = CsvImportOrchestrator(mockRepo, MockTestCategoryRepository())

        val csv = """Transaction_Date,Amount_RON,Description,Type,Account,Category,SubCategory
2026-08-10,100.0,Groceries,Expense,Card,Food,Groceries
"""
        try {
            orchestrator.parseAndValidateFromContent(csv)
            fail("Should have re-thrown CancellationException")
        } catch (e: CancellationException) {
            assertEquals("Simulated coroutine cancellation", e.message)
        }
    }
}

