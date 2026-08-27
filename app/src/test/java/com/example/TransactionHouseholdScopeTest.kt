package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.db.FinTrackDatabase
import com.example.data.model.ExchangeRateEntity
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

    @Test
    fun test9_outboundSyncEngine_processesExchangeRateAndTransactionFifoSuccessfully() = runBlocking {
        val recordingSource = RecordingSnapshotSource()
        val syncEngine = OutboundSyncEngine(
            database = db,
            syncOutboxDao = db.syncOutboxDao(),
            snapshotSource = recordingSource,
            syncStatusProvider = { SyncStatus.Synced("HH_FAMILY") },
            coroutineScope = CoroutineScope(Dispatchers.Unconfined)
        )

        val now = System.currentTimeMillis()

        // 1. Enqueue EXCHANGE_RATE at T1
        val rate = ExchangeRateEntity(
            date = "2026-08-27",
            requestedDate = "2026-08-27",
            effectiveDate = "2026-08-27",
            rate = 4.9765,
            source = "BNR_OFFICIAL",
            fetchedAt = now,
            status = "OFFICIAL"
        )
        db.exchangeRateDao().insertRate(rate)
        db.syncOutboxDao().insertOutboxEntry(
            SyncOutboxEntity(
                id = "outbox_rate_01",
                entityType = "EXCHANGE_RATE",
                entityId = "2026-08-27",
                operation = "UPSERT",
                status = "PENDING",
                createdAt = now - 1000
            )
        )

        // 2. Enqueue TRANSACTION at T2 (T2 > T1)
        val tx = TransactionEntity(
            id = "tx_groceries_01",
            userId = "user_owner_uid",
            date = "2026-08-27",
            description = "Family Groceries",
            amountRON = 250.0,
            amountEUR = 50.23,
            exchangeRate = 4.9765,
            exchangeRateDate = "2026-08-27",
            type = "Expense",
            account = "Card",
            category = "Food & Dining",
            subCategory = "Groceries",
            destination = null,
            createdAt = now,
            updatedAt = now,
            exchangeRateSource = "BNR_OFFICIAL",
            conversionStatus = "OFFICIAL",
            householdId = "HH_FAMILY",
            createdByUid = "user_owner_uid"
        )
        db.transactionDao().insertTransaction(tx)
        db.syncOutboxDao().insertOutboxEntry(
            SyncOutboxEntity(
                id = "outbox_tx_01",
                entityType = "TRANSACTION",
                entityId = "tx_groceries_01",
                operation = "UPSERT",
                status = "PENDING",
                createdAt = now
            )
        )

        // Process queue
        val processed = syncEngine.processPendingQueue()
        assertEquals(2, processed)

        // Verify EXCHANGE_RATE was processed and does NOT contain migrationId key
        assertEquals(1, recordingSource.exchangeRateUpserts.size)
        val rateUpsert = recordingSource.exchangeRateUpserts.first()
        assertEquals("HH_FAMILY", rateUpsert.first)
        assertEquals("2026-08-27", rateUpsert.second.first)
        assertFalse("Exchange rate payload must NOT contain migrationId when null", rateUpsert.second.second.containsKey("migrationId"))

        // Verify TRANSACTION was processed immediately following the rate without being blocked
        assertEquals(1, recordingSource.transactionUpserts.size)
        val txUpsert = recordingSource.transactionUpserts.first()
        assertEquals("HH_FAMILY", txUpsert.first)
        assertEquals("tx_groceries_01", txUpsert.second.first)
        assertEquals("user_owner_uid", txUpsert.second.second["createdByUid"])
        assertEquals("HH_FAMILY", txUpsert.second.second["householdId"])

        // Verify all outbox entries were acknowledged/marked SUCCESS
        assertEquals(0, db.syncOutboxDao().getPendingCount())
        val outbox1 = db.syncOutboxDao().getEntryById("outbox_rate_01")
        val outbox2 = db.syncOutboxDao().getEntryById("outbox_tx_01")
        assertEquals("SUCCESS", outbox1?.status)
        assertEquals("SUCCESS", outbox2?.status)
    }

    @Test
    fun test10_ownerAndMemberTransactionCreatedByUidPreservation() = runBlocking {
        val recordingSource = RecordingSnapshotSource()
        val syncEngine = OutboundSyncEngine(
            database = db,
            syncOutboxDao = db.syncOutboxDao(),
            snapshotSource = recordingSource,
            syncStatusProvider = { SyncStatus.Synced("HH_SHARED") },
            coroutineScope = CoroutineScope(Dispatchers.Unconfined)
        )

        // OWNER creates a transaction
        val txOwner = TransactionEntity(
            id = "tx_owner_99",
            userId = "owner_firebase_uid_123",
            date = "2026-08-27",
            description = "Owner Expense",
            amountRON = 100.0,
            amountEUR = 20.0,
            exchangeRate = 5.0,
            exchangeRateDate = "2026-08-27",
            type = "Expense",
            account = "Card",
            category = "General",
            subCategory = "",
            householdId = "HH_SHARED",
            createdByUid = "owner_firebase_uid_123"
        )
        db.transactionDao().insertTransaction(txOwner)
        db.syncOutboxDao().insertOutboxEntry(
            SyncOutboxEntity(
                id = "outbox_tx_owner",
                entityType = "TRANSACTION",
                entityId = "tx_owner_99",
                operation = "UPSERT",
                status = "PENDING"
            )
        )

        // MEMBER creates a transaction
        val txMember = TransactionEntity(
            id = "tx_member_88",
            userId = "member_firebase_uid_456",
            date = "2026-08-27",
            description = "Member Expense",
            amountRON = 50.0,
            amountEUR = 10.0,
            exchangeRate = 5.0,
            exchangeRateDate = "2026-08-27",
            type = "Expense",
            account = "Card",
            category = "General",
            subCategory = "",
            householdId = "HH_SHARED",
            createdByUid = "member_firebase_uid_456"
        )
        db.transactionDao().insertTransaction(txMember)
        db.syncOutboxDao().insertOutboxEntry(
            SyncOutboxEntity(
                id = "outbox_tx_member",
                entityType = "TRANSACTION",
                entityId = "tx_member_88",
                operation = "UPSERT",
                status = "PENDING"
            )
        )

        syncEngine.processPendingQueue()

        assertEquals(2, recordingSource.transactionUpserts.size)
        val uploadedOwner = recordingSource.transactionUpserts.find { it.second.first == "tx_owner_99" }
        val uploadedMember = recordingSource.transactionUpserts.find { it.second.first == "tx_member_88" }

        assertNotNull(uploadedOwner)
        assertEquals("owner_firebase_uid_123", uploadedOwner!!.second.second["createdByUid"])
        assertEquals("HH_SHARED", uploadedOwner.second.second["householdId"])

        assertNotNull(uploadedMember)
        assertEquals("member_firebase_uid_456", uploadedMember!!.second.second["createdByUid"])
        assertEquals("HH_SHARED", uploadedMember.second.second["householdId"])
    }

    @Test
    fun test11_rapidNotifyPending_processesAllOutboxEntriesWithoutRace() = runBlocking {
        val recordingSource = RecordingSnapshotSource()
        val syncEngine = OutboundSyncEngine(
            database = db,
            syncOutboxDao = db.syncOutboxDao(),
            snapshotSource = recordingSource,
            syncStatusProvider = { SyncStatus.Synced("HH_RACE") },
            coroutineScope = CoroutineScope(Dispatchers.Unconfined)
        )

        val now = System.currentTimeMillis()

        // 1. Rate entry
        val rate = ExchangeRateEntity(
            date = "2026-08-27",
            requestedDate = "2026-08-27",
            effectiveDate = "2026-08-27",
            rate = 4.9765,
            source = "BNR_OFFICIAL",
            fetchedAt = now,
            status = "OFFICIAL"
        )
        db.exchangeRateDao().insertRate(rate)
        db.syncOutboxDao().insertOutboxEntry(
            SyncOutboxEntity(
                id = "outbox_rate_race",
                entityType = "EXCHANGE_RATE",
                entityId = "2026-08-27",
                operation = "UPSERT",
                status = "PENDING",
                createdAt = now - 100
            )
        )

        // 2. Transaction entry
        val tx = TransactionEntity(
            id = "tx_race_01",
            userId = "user_race_1",
            date = "2026-08-27",
            description = "Race Test Coffee",
            amountRON = 15.0,
            amountEUR = 3.01,
            exchangeRate = 4.9765,
            exchangeRateDate = "2026-08-27",
            type = "Expense",
            account = "Card",
            category = "Food & Dining",
            subCategory = "Cafes",
            destination = null,
            createdAt = now,
            updatedAt = now,
            exchangeRateSource = "BNR_OFFICIAL",
            conversionStatus = "OFFICIAL",
            householdId = "HH_RACE",
            createdByUid = "user_race_1"
        )
        db.transactionDao().insertTransaction(tx)
        db.syncOutboxDao().insertOutboxEntry(
            SyncOutboxEntity(
                id = "outbox_tx_race",
                entityType = "TRANSACTION",
                entityId = "tx_race_01",
                operation = "UPSERT",
                status = "PENDING",
                createdAt = now
            )
        )

        syncEngine.start()
        // Fire rapid notifications in succession
        syncEngine.notifyPending()
        syncEngine.notifyPending()
        syncEngine.notifyPending()
        syncEngine.awaitIdle()

        // Both items must be processed and marked SUCCESS without getting stuck
        assertEquals(1, recordingSource.exchangeRateUpserts.size)
        assertEquals(1, recordingSource.transactionUpserts.size)
        assertEquals(0, db.syncOutboxDao().getPendingCount())

        val rateOutbox = db.syncOutboxDao().getEntryById("outbox_rate_race")
        val txOutbox = db.syncOutboxDao().getEntryById("outbox_tx_race")
        assertEquals("SUCCESS", rateOutbox?.status)
        assertEquals("SUCCESS", txOutbox?.status)

        syncEngine.stop()
    }

    @Test
    fun test12_realSaveTransactionCallPath_processesBothExchangeRateAndTransaction() = runBlocking {
        val recordingSource = RecordingSnapshotSource()
        lateinit var syncEngine: OutboundSyncEngine

        val exchangeRateService = ExchangeRateService(
            exchangeRateDao = db.exchangeRateDao(),
            syncOutboxDao = db.syncOutboxDao(),
            database = db,
            onOutboxMutated = { syncEngine.notifyPending() },
            httpFetcher = {
                Pair(
                    """<?xml version="1.0" encoding="utf-8"?>
                    <DataSet xmlns="http://www.bnr.ro/xsd">
                        <Body>
                            <OrigCurrency>RON</OrigCurrency>
                            <Cube date="2026-08-27">
                                <Rate currency="EUR">4.9765</Rate>
                            </Cube>
                        </Body>
                    </DataSet>""".trimIndent(),
                    "200"
                )
            }
        )

        val repo = RoomTransactionRepository(
            transactionDao = db.transactionDao(),
            exchangeRateService = exchangeRateService,
            exchangeRateDao = db.exchangeRateDao(),
            database = db,
            syncOutboxDao = db.syncOutboxDao(),
            onOutboxMutated = { syncEngine.notifyPending() }
        )

        syncEngine = OutboundSyncEngine(
            database = db,
            syncOutboxDao = db.syncOutboxDao(),
            snapshotSource = recordingSource,
            syncStatusProvider = { SyncStatus.Synced("HH_LIVE") },
            coroutineScope = CoroutineScope(Dispatchers.Unconfined)
        )

        syncEngine.start()
        syncEngine.awaitIdle()

        val saved = repo.saveTransaction(
            id = null,
            date = "2026-08-27",
            description = "Live Grocery",
            amountRON = 100.0,
            type = "Expense",
            account = "Card",
            category = "Food & Dining",
            subCategory = "Groceries",
            destination = null,
            userId = "user_owner_999",
            householdId = "HH_LIVE"
        )

        assertEquals("OFFICIAL", saved.conversionStatus)
        assertEquals(4.9765, saved.exchangeRate, 0.0001)

        syncEngine.awaitIdle()

        // Verify that both EXCHANGE_RATE and TRANSACTION are uploaded and marked SUCCESS
        assertEquals(1, recordingSource.exchangeRateUpserts.size)
        assertEquals(1, recordingSource.transactionUpserts.size)

        val txUpsert = recordingSource.transactionUpserts.first()
        assertEquals("HH_LIVE", txUpsert.first)
        assertEquals(saved.id, txUpsert.second.first)
        assertEquals("user_owner_999", txUpsert.second.second["createdByUid"])

        assertEquals(0, db.syncOutboxDao().getPendingCount())

        syncEngine.stop()
    }

    @Test
    fun test13_staleFailedOutboxEntry_doesNotBlockPendingTransaction() = runBlocking {
        val recordingSource = RecordingSnapshotSource()
        val syncEngine = OutboundSyncEngine(
            database = db,
            syncOutboxDao = db.syncOutboxDao(),
            snapshotSource = recordingSource,
            syncStatusProvider = { SyncStatus.Synced("HH_STALE") },
            coroutineScope = CoroutineScope(Dispatchers.Unconfined)
        )

        val now = System.currentTimeMillis()

        // 1. Insert a stale FAILED entry from before the fix
        db.syncOutboxDao().insertOutboxEntry(
            SyncOutboxEntity(
                id = "outbox_failed_old",
                entityType = "EXCHANGE_RATE",
                entityId = "2026-08-20",
                operation = "UPSERT",
                status = "FAILED",
                errorCode = "PERMISSION_DENIED",
                errorMessage = "Old error",
                createdAt = now - 5000
            )
        )

        // 2. Insert a new PENDING transaction
        val tx = TransactionEntity(
            id = "tx_after_failed",
            userId = "user_auth_1",
            date = "2026-08-27",
            description = "Tx after failed",
            amountRON = 30.0,
            amountEUR = 6.0,
            exchangeRate = 5.0,
            exchangeRateDate = "2026-08-27",
            type = "Expense",
            account = "Card",
            category = "General",
            subCategory = "",
            householdId = "HH_STALE",
            createdByUid = "user_auth_1"
        )
        db.transactionDao().insertTransaction(tx)
        db.syncOutboxDao().insertOutboxEntry(
            SyncOutboxEntity(
                id = "outbox_tx_pending",
                entityType = "TRANSACTION",
                entityId = "tx_after_failed",
                operation = "UPSERT",
                status = "PENDING",
                createdAt = now
            )
        )

        val processed = syncEngine.processPendingQueue()
        assertEquals(1, processed)

        // The transaction must be uploaded successfully
        assertEquals(1, recordingSource.transactionUpserts.size)
        assertEquals("tx_after_failed", recordingSource.transactionUpserts.first().second.first)

        // The failed entry remains FAILED, the transaction becomes SUCCESS
        val failedEntry = db.syncOutboxDao().getEntryById("outbox_failed_old")
        val txEntry = db.syncOutboxDao().getEntryById("outbox_tx_pending")
        assertEquals("FAILED", failedEntry?.status)
        assertEquals("SUCCESS", txEntry?.status)
    }

    @Test
    fun test14_bidirectionalSyncSimulation() = runBlocking {
        val recordingSource = RecordingSnapshotSource()
        val syncEngine = OutboundSyncEngine(
            database = db,
            syncOutboxDao = db.syncOutboxDao(),
            snapshotSource = recordingSource,
            syncStatusProvider = { SyncStatus.Synced("HH_BIDI") },
            coroutineScope = CoroutineScope(Dispatchers.Unconfined)
        )
        syncEngine.start()
        syncEngine.awaitIdle()

        // 1. OWNER creates transaction -> Outbox -> Uploaded
        val txOwner = TransactionEntity(
            id = "tx_owner_bidi",
            userId = "owner_uid",
            date = "2026-08-27",
            description = "Owner Lunch",
            amountRON = 45.0,
            amountEUR = 9.0,
            exchangeRate = 5.0,
            exchangeRateDate = "2026-08-27",
            type = "Expense",
            account = "Card",
            category = "Food & Dining",
            subCategory = "Lunch",
            householdId = "HH_BIDI",
            createdByUid = "owner_uid"
        )
        db.transactionDao().insertTransaction(txOwner)
        db.syncOutboxDao().insertOutboxEntry(
            SyncOutboxEntity(
                id = "outbox_owner_bidi",
                entityType = "TRANSACTION",
                entityId = "tx_owner_bidi",
                operation = "UPSERT",
                status = "PENDING"
            )
        )
        syncEngine.notifyPending()
        syncEngine.awaitIdle()

        assertEquals(1, recordingSource.transactionUpserts.size)
        assertEquals("tx_owner_bidi", recordingSource.transactionUpserts[0].second.first)
        assertEquals("owner_uid", recordingSource.transactionUpserts[0].second.second["createdByUid"])

        // 2. MEMBER simulates inbound receiving OWNER transaction
        val ownerPayload = recordingSource.transactionUpserts[0].second.second
        val receivedDto = TransactionDto(
            transactionId = ownerPayload["transactionId"] as String,
            householdId = ownerPayload["householdId"] as String,
            createdByUid = ownerPayload["createdByUid"] as String,
            transactionDate = ownerPayload["transactionDate"] as String,
            description = ownerPayload["description"] as String,
            amountRon = ownerPayload["amountRon"] as Double,
            amountEur = ownerPayload["amountEur"] as Double,
            exchangeRate = ownerPayload["exchangeRate"] as Double,
            exchangeRateDate = ownerPayload["exchangeRateDate"] as String,
            type = ownerPayload["type"] as String,
            account = ownerPayload["account"] as String,
            category = ownerPayload["category"] as String,
            subCategory = ownerPayload["subCategory"] as String,
            isDeleted = ownerPayload["isDeleted"] as Boolean
        )
        val entityFromOwner = receivedDto.toEntity("user_member_uid")
        assertNotNull(entityFromOwner)
        assertEquals("HH_BIDI", entityFromOwner!!.householdId)
        assertEquals("owner_uid", entityFromOwner.createdByUid)

        // 3. MEMBER creates transaction -> Outbox -> Uploaded
        val txMember = TransactionEntity(
            id = "tx_member_bidi",
            userId = "member_uid",
            date = "2026-08-27",
            description = "Member Fuel",
            amountRON = 300.0,
            amountEUR = 60.0,
            exchangeRate = 5.0,
            exchangeRateDate = "2026-08-27",
            type = "Expense",
            account = "Card",
            category = "Transportation",
            subCategory = "Fuel",
            householdId = "HH_BIDI",
            createdByUid = "member_uid"
        )
        db.transactionDao().insertTransaction(txMember)
        db.syncOutboxDao().insertOutboxEntry(
            SyncOutboxEntity(
                id = "outbox_member_bidi",
                entityType = "TRANSACTION",
                entityId = "tx_member_bidi",
                operation = "UPSERT",
                status = "PENDING"
            )
        )
        syncEngine.notifyPending()
        syncEngine.awaitIdle()

        assertEquals(2, recordingSource.transactionUpserts.size)
        assertEquals("tx_member_bidi", recordingSource.transactionUpserts[1].second.first)
        assertEquals("member_uid", recordingSource.transactionUpserts[1].second.second["createdByUid"])

        syncEngine.stop()
    }

    @Test
    fun test15_deleteTransaction_outboundPropagationWorks() = runBlocking {
        val recordingSource = RecordingSnapshotSource()
        val syncEngine = OutboundSyncEngine(
            database = db,
            syncOutboxDao = db.syncOutboxDao(),
            snapshotSource = recordingSource,
            syncStatusProvider = { SyncStatus.Synced("HH_DEL") },
            coroutineScope = CoroutineScope(Dispatchers.Unconfined)
        )
        syncEngine.start()
        syncEngine.awaitIdle()

        val repo = RoomTransactionRepository(
            transactionDao = db.transactionDao(),
            exchangeRateService = ExchangeRateService(db.exchangeRateDao()),
            exchangeRateDao = db.exchangeRateDao(),
            database = db,
            syncOutboxDao = db.syncOutboxDao(),
            onOutboxMutated = { syncEngine.notifyPending() }
        )

        // Insert and then delete
        val tx = TransactionEntity(
            id = "tx_to_delete_99",
            userId = "user_owner_1",
            date = "2026-08-27",
            description = "To be deleted",
            amountRON = 50.0,
            amountEUR = 10.0,
            exchangeRate = 5.0,
            exchangeRateDate = "2026-08-27",
            type = "Expense",
            account = "Cash",
            category = "General",
            subCategory = "",
            householdId = "HH_DEL"
        )
        db.transactionDao().insertTransaction(tx)

        repo.deleteTransaction(tx)
        syncEngine.awaitIdle()

        assertEquals(1, recordingSource.transactionDeletes.size)
        assertEquals("HH_DEL", recordingSource.transactionDeletes.first().first)
        assertEquals("tx_to_delete_99", recordingSource.transactionDeletes.first().second)
        assertEquals(0, db.syncOutboxDao().getPendingCount())

        syncEngine.stop()
    }

    @Test
    fun test16_stopAndRestart_lifecycleControl() = runBlocking {
        val recordingSource = RecordingSnapshotSource()
        val syncEngine = OutboundSyncEngine(
            database = db,
            syncOutboxDao = db.syncOutboxDao(),
            snapshotSource = recordingSource,
            syncStatusProvider = { SyncStatus.Synced("HH_LIFE") },
            coroutineScope = CoroutineScope(Dispatchers.Unconfined)
        )

        syncEngine.start()
        syncEngine.awaitIdle()
        syncEngine.stop()

        // Enqueue transaction while stopped
        val tx = TransactionEntity(
            id = "tx_stopped_01",
            userId = "user_1",
            date = "2026-08-27",
            description = "While stopped",
            amountRON = 10.0,
            amountEUR = 2.0,
            exchangeRate = 5.0,
            exchangeRateDate = "2026-08-27",
            type = "Expense",
            account = "Card",
            category = "General",
            subCategory = "",
            householdId = "HH_LIFE"
        )
        db.transactionDao().insertTransaction(tx)
        db.syncOutboxDao().insertOutboxEntry(
            SyncOutboxEntity(
                id = "outbox_stopped_01",
                entityType = "TRANSACTION",
                entityId = "tx_stopped_01",
                operation = "UPSERT",
                status = "PENDING"
            )
        )

        syncEngine.notifyPending()
        syncEngine.awaitIdle()
        // Must NOT be processed because engine is stopped
        assertEquals(0, recordingSource.transactionUpserts.size)
        assertEquals(1, db.syncOutboxDao().getPendingCount())

        // Start engine again
        syncEngine.start()
        syncEngine.awaitIdle()

        // Should immediately recover and process pending items
        assertEquals(1, recordingSource.transactionUpserts.size)
        assertEquals("tx_stopped_01", recordingSource.transactionUpserts.first().second.first)
        assertEquals(0, db.syncOutboxDao().getPendingCount())

        syncEngine.stop()
    }
}
