package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.db.FinTrackDatabase
import com.example.data.model.ExchangeRateMetadataDto
import com.example.data.model.SyncOutboxEntity
import com.example.data.model.TransactionDto
import com.example.data.model.TransactionEntity
import com.example.data.model.toEntity
import com.example.data.repository.OutboundSyncEngine
import com.example.data.repository.RoomTransactionRepository
import com.example.data.repository.SyncStatus
import com.example.data.service.ExchangeRateService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class TransactionHouseholdScopeTest {

    private lateinit var db: FinTrackDatabase
    private lateinit var repository: RoomTransactionRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, FinTrackDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        val exchangeRateService = ExchangeRateService(db.exchangeRateDao())
        repository = RoomTransactionRepository(
            transactionDao = db.transactionDao(),
            exchangeRateService = exchangeRateService,
            exchangeRateDao = db.exchangeRateDao(),
            syncOutboxDao = db.syncOutboxDao(),
            database = db
        )
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun test1_transactionDtoToEntityPreservesHouseholdIdAndCreatedByUid() {
        val dto = TransactionDto(
            transactionId = "tx_remote_01",
            householdId = "hh_family_42",
            createdByUid = "user_remote_99",
            transactionDate = "2026-08-20",
            description = "Supermarket",
            amountRon = 150.0,
            amountEur = 30.0,
            exchangeRate = 5.0,
            exchangeRateDate = "2026-08-19",
            type = "Expense",
            account = "Card",
            category = "Food & Dining",
            subCategory = "Groceries",
            exchangeRateMetadata = ExchangeRateMetadataDto(
                source = "BNR_OFFICIAL",
                status = "OFFICIAL",
                rate = 5.0,
                effectiveDate = "2026-08-19"
            )
        )

        val entity = dto.toEntity("tx_remote_01")
        assertNotNull(entity)
        assertEquals("tx_remote_01", entity!!.id)
        assertEquals("hh_family_42", entity.householdId)
        assertEquals("user_remote_99", entity.createdByUid)
        assertEquals("user_remote_99", entity.userId)
    }

    @Test
    fun test2_saveTransactionPreservesHouseholdIdInRoom() = runBlocking {
        val saved = repository.saveTransaction(
            date = "2026-08-20",
            description = "Electricity Bill",
            amountRON = 200.0,
            type = "Expense",
            account = "Card",
            category = "Housing & Utilities",
            subCategory = "Utilities & Internet",
            userId = "user_alpha",
            householdId = "hh_alpha"
        )

        assertEquals("hh_alpha", saved.householdId)
        assertEquals("user_alpha", saved.createdByUid)

        val fromDb = db.transactionDao().getTransactionById(saved.id)
        assertNotNull(fromDb)
        assertEquals("hh_alpha", fromDb!!.householdId)
        assertEquals("user_alpha", fromDb.createdByUid)
    }

    @Test
    fun test3_roomIsolationBetweenHouseholds() = runBlocking {
        // Insert transaction for household A
        val txA = repository.saveTransaction(
            date = "2026-08-20",
            description = "Tx for HH A",
            amountRON = 100.0,
            type = "Expense",
            account = "Card",
            category = "Food & Dining",
            subCategory = "Groceries",
            householdId = "HH_A"
        )

        // Insert transaction for household B
        val txB = repository.saveTransaction(
            date = "2026-08-20",
            description = "Tx for HH B",
            amountRON = 200.0,
            type = "Expense",
            account = "Card",
            category = "Food & Dining",
            subCategory = "Groceries",
            householdId = "HH_B"
        )

        // Query household A
        val listA = repository.getTransactions("HH_A").first()
        assertEquals(1, listA.size)
        assertEquals(txA.id, listA[0].id)
        assertEquals("HH_A", listA[0].householdId)

        // Query household B
        val listB = repository.getTransactions("HH_B").first()
        assertEquals(1, listB.size)
        assertEquals(txB.id, listB[0].id)
        assertEquals("HH_B", listB[0].householdId)
    }

    @Test
    fun test4_legacyNullHouseholdTransactionsDoNotLeakIntoHouseholdQueries() = runBlocking {
        // Insert legacy transaction with null householdId
        val txLegacy = repository.saveTransaction(
            date = "2026-08-20",
            description = "Legacy Tx",
            amountRON = 50.0,
            type = "Expense",
            account = "Cash",
            category = "Food & Dining",
            subCategory = "Groceries",
            householdId = null
        )

        assertNull(txLegacy.householdId)

        // Query HH_A
        val listA = repository.getTransactions("HH_A").first()
        assertTrue(listA.none { it.id == txLegacy.id })

        // Query legacy (null householdId)
        val legacyList = repository.getTransactions(null).first()
        assertTrue(legacyList.any { it.id == txLegacy.id })
    }

    @Test
    fun test5_updateTransactionPreservesExistingHouseholdIdWhenNotOverridden() = runBlocking {
        val original = repository.saveTransaction(
            date = "2026-08-20",
            description = "Original",
            amountRON = 75.0,
            type = "Expense",
            account = "Card",
            category = "Food & Dining",
            subCategory = "Groceries",
            userId = "user_initial",
            householdId = "HH_MAIN"
        )

        assertEquals("HH_MAIN", original.householdId)

        // Update transaction without specifying householdId
        val updated = repository.saveTransaction(
            id = original.id,
            date = "2026-08-21",
            description = "Updated Description",
            amountRON = 80.0,
            type = "Expense",
            account = "Card",
            category = "Food & Dining",
            subCategory = "Groceries",
            userId = "user_initial",
            householdId = null // should fall back to existingTx.householdId
        )

        assertEquals("HH_MAIN", updated.householdId)
        assertEquals("user_initial", updated.createdByUid)

        val fromDb = db.transactionDao().getTransactionById(original.id)
        assertEquals("HH_MAIN", fromDb?.householdId)
    }

    @Test
    fun test6_outboundSyncEngineSkipsTransactionWithMismatchedHouseholdId() = runBlocking {
        val recordingSource = RecordingSnapshotSource()
        val syncEngine = OutboundSyncEngine(
            database = db,
            syncOutboxDao = db.syncOutboxDao(),
            snapshotSource = recordingSource,
            syncStatusProvider = { SyncStatus.Synced("HH_A") },
            coroutineScope = CoroutineScope(Dispatchers.Unconfined)
        )

        // Transaction belongs to HH_B
        val txB = TransactionEntity(
            id = "tx_foreign",
            date = "2026-08-20",
            description = "Foreign Tx",
            amountRON = 100.0,
            amountEUR = 20.0,
            exchangeRate = 5.0,
            exchangeRateDate = "2026-08-20",
            type = "Expense",
            account = "Card",
            category = "Food",
            subCategory = "",
            householdId = "HH_B"
        )
        db.transactionDao().insertTransaction(txB)
        db.syncOutboxDao().insertOutboxEntry(
            SyncOutboxEntity(
                entityType = "TRANSACTION",
                entityId = "tx_foreign",
                operation = "UPSERT",
                status = "PENDING"
            )
        )

        // Process queue while active household is HH_A
        syncEngine.processPendingQueue()

        // Verify transaction was skipped and not uploaded to HH_A
        assertTrue(recordingSource.transactionUpserts.none { it.second.first == "tx_foreign" })
    }

    @Test
    fun test7_outboundSyncEngineUploadsTransactionWithMatchingHouseholdId() = runBlocking {
        val recordingSource = RecordingSnapshotSource()
        val syncEngine = OutboundSyncEngine(
            database = db,
            syncOutboxDao = db.syncOutboxDao(),
            snapshotSource = recordingSource,
            syncStatusProvider = { SyncStatus.Synced("HH_A") },
            coroutineScope = CoroutineScope(Dispatchers.Unconfined)
        )

        // Transaction belongs to HH_A
        val txA = TransactionEntity(
            id = "tx_matching",
            date = "2026-08-20",
            description = "Matching Tx",
            amountRON = 120.0,
            amountEUR = 24.0,
            exchangeRate = 5.0,
            exchangeRateDate = "2026-08-20",
            type = "Expense",
            account = "Card",
            category = "Food",
            subCategory = "",
            householdId = "HH_A"
        )
        db.transactionDao().insertTransaction(txA)
        db.syncOutboxDao().insertOutboxEntry(
            SyncOutboxEntity(
                entityType = "TRANSACTION",
                entityId = "tx_matching",
                operation = "UPSERT",
                status = "PENDING"
            )
        )

        syncEngine.processPendingQueue()

        // Verify transaction was uploaded to HH_A
        assertEquals(1, recordingSource.transactionUpserts.size)
        val upsert = recordingSource.transactionUpserts.first()
        assertEquals("HH_A", upsert.first)
        assertEquals("tx_matching", upsert.second.first)
    }

    @Test
    fun test8_outboundSyncEngineUploadsLegacyNullHouseholdTransactionToActiveHousehold() = runBlocking {
        val recordingSource = RecordingSnapshotSource()
        val syncEngine = OutboundSyncEngine(
            database = db,
            syncOutboxDao = db.syncOutboxDao(),
            snapshotSource = recordingSource,
            syncStatusProvider = { SyncStatus.Synced("HH_A") },
            coroutineScope = CoroutineScope(Dispatchers.Unconfined)
        )

        // Legacy local transaction with null householdId
        val txLegacy = TransactionEntity(
            id = "tx_legacy_01",
            date = "2026-08-20",
            description = "Legacy Tx",
            amountRON = 99.0,
            amountEUR = 19.8,
            exchangeRate = 5.0,
            exchangeRateDate = "2026-08-20",
            type = "Expense",
            account = "Cash",
            category = "General",
            subCategory = "",
            householdId = null
        )
        db.transactionDao().insertTransaction(txLegacy)
        db.syncOutboxDao().insertOutboxEntry(
            SyncOutboxEntity(
                entityType = "TRANSACTION",
                entityId = "tx_legacy_01",
                operation = "UPSERT",
                status = "PENDING"
            )
        )

        syncEngine.processPendingQueue()

        // Verify legacy transaction with null householdId is synchronized to active household HH_A
        assertEquals(1, recordingSource.transactionUpserts.size)
        val upsert = recordingSource.transactionUpserts.first()
        assertEquals("HH_A", upsert.first)
        assertEquals("tx_legacy_01", upsert.second.first)
    }
}
