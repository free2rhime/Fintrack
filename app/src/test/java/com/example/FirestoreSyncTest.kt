package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.db.FinTrackDatabase
import com.example.data.model.CategoryEntity
import com.example.data.model.SyncOutboxEntity
import com.example.data.model.TransactionEntity
import com.example.data.repository.HouseholdResolutionResult
import com.example.data.repository.SyncStatus
import com.example.data.repository.FirestoreSnapshotSource
import com.example.data.repository.FirestoreSyncRepository
import com.example.data.repository.ListenerRegistrationHandle
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
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

class FakeSnapshotSource : FirestoreSnapshotSource {
    var lastTxHouseholdId: String? = null
    var lastCatHouseholdId: String? = null
    var txListenerActive = false
    var catListenerActive = false
    var txListenerRemoveCount = 0
    var catListenerRemoveCount = 0

    var txCallback: ((List<Pair<String, Map<String, Any?>>>) -> Unit)? = null
    var txCallbackWithChanges: ((List<Pair<String, Map<String, Any?>>>, List<String>) -> Unit)? = null
    var catCallback: ((List<Pair<String, Map<String, Any?>>>) -> Unit)? = null
    var txErrorCallback: ((Exception) -> Unit)? = null
    var catErrorCallback: ((Exception) -> Unit)? = null

    var autoEmitInitialSnapshot: Boolean = false
    var shouldFailHouseholdResolution: Boolean = false
    var shouldPermissionDenyHouseholdResolution: Boolean = false
    val customResolutionResults = mutableMapOf<String, HouseholdResolutionResult>()

    override fun listenToTransactions(
        householdId: String,
        onSnapshot: (List<Pair<String, Map<String, Any?>>>) -> Unit,
        onError: (Exception) -> Unit
    ): ListenerRegistrationHandle {
        lastTxHouseholdId = householdId
        txListenerActive = true
        txCallback = onSnapshot
        txCallbackWithChanges = { docs, _ -> onSnapshot(docs) }
        txErrorCallback = onError
        if (autoEmitInitialSnapshot) {
            onSnapshot(emptyList())
        }
        return object : ListenerRegistrationHandle {
            override fun remove() {
                txListenerActive = false
                txListenerRemoveCount++
            }
        }
    }

    override fun listenToTransactions(
        householdId: String,
        onSnapshotWithChanges: (docs: List<Pair<String, Map<String, Any?>>>, removedDocIds: List<String>) -> Unit,
        onError: (Exception) -> Unit
    ): ListenerRegistrationHandle {
        lastTxHouseholdId = householdId
        txListenerActive = true
        txCallback = { docs -> onSnapshotWithChanges(docs, emptyList()) }
        txCallbackWithChanges = onSnapshotWithChanges
        txErrorCallback = onError
        if (autoEmitInitialSnapshot) {
            onSnapshotWithChanges(emptyList(), emptyList())
        }
        return object : ListenerRegistrationHandle {
            override fun remove() {
                txListenerActive = false
                txListenerRemoveCount++
            }
        }
    }

    override fun listenToCategories(
        householdId: String,
        onSnapshot: (List<Pair<String, Map<String, Any?>>>) -> Unit,
        onError: (Exception) -> Unit
    ): ListenerRegistrationHandle {
        lastCatHouseholdId = householdId
        catListenerActive = true
        catCallback = onSnapshot
        catErrorCallback = onError
        if (autoEmitInitialSnapshot) {
            onSnapshot(emptyList())
        }
        return object : ListenerRegistrationHandle {
            override fun remove() {
                catListenerActive = false
                catListenerRemoveCount++
            }
        }
    }

    override suspend fun resolveHouseholdId(userUid: String): HouseholdResolutionResult {
        customResolutionResults[userUid]?.let { return it }
        if (shouldPermissionDenyHouseholdResolution) {
            return HouseholdResolutionResult.PermissionDenied
        }
        if (shouldFailHouseholdResolution) {
            return HouseholdResolutionResult.Failure("Network failure during household resolution")
        }
        val activeMember = members.entries.firstOrNull {
            it.key.second == userUid && (it.value["status"] as? String)?.uppercase() == "ACTIVE"
        }
        if (activeMember != null) {
            return HouseholdResolutionResult.Success(activeMember.key.first)
        }
        return if (userUid == "user_special") {
            HouseholdResolutionResult.Success("hh_special_99")
        } else {
            HouseholdResolutionResult.NoHousehold
        }
    }

    val members = mutableMapOf<Pair<String, String>, Map<String, Any?>>()
    var activeMigrationSession: Map<String, Any?>? = null
    var remoteTransactionCount: Int = 0
    var remoteCategoryCount: Int = 0
    var remoteExchangeRateCount: Int = 0
    var shouldFailRemoteTransactionCount: Boolean = false
    var shouldFailRemoteCategoryCount: Boolean = false
    var shouldFailRemoteExchangeRateCount: Boolean = false
    var remoteCountException: Exception? = null
    val createdMigrationDocs = mutableMapOf<String, Map<String, Any?>>()
    val updatedMigrationDocs = mutableListOf<Pair<String, Map<String, Any?>>>()
    var shouldFailMigrationDocCreation: Boolean = false
    var shouldFailMigrationDocUpdate: Boolean = false
    var shouldFailCategoryUpload: Boolean = false
    var shouldFailExchangeRateUpload: Boolean = false
    var shouldFailTransactionUpload: Boolean = false

    val uploadedCategoryBatches = mutableListOf<List<Map<String, Any?>>>()
    val uploadedExchangeRateBatches = mutableListOf<List<Map<String, Any?>>>()
    val uploadedTransactionBatches = mutableListOf<List<Map<String, Any?>>>()
    val operationOrder = mutableListOf<String>()

    override suspend fun getHouseholdMembership(householdId: String, userUid: String): Map<String, Any?>? {
        return members[Pair(householdId, userUid)]
    }

    override suspend fun getActiveMigrationSession(householdId: String): Map<String, Any?>? {
        return activeMigrationSession
    }

    override suspend fun getRemoteTransactionCount(householdId: String): Int {
        if (shouldFailRemoteTransactionCount) {
            throw remoteCountException ?: IllegalStateException("Simulated remote transaction count failure")
        }
        return remoteTransactionCount
    }

    override suspend fun getRemoteCategoryCount(householdId: String): Int {
        if (shouldFailRemoteCategoryCount) {
            throw remoteCountException ?: IllegalStateException("Simulated remote category count failure")
        }
        return remoteCategoryCount
    }

    override suspend fun getRemoteExchangeRateCount(householdId: String): Int {
        if (shouldFailRemoteExchangeRateCount) {
            throw remoteCountException ?: IllegalStateException("Simulated remote exchange rate count failure")
        }
        return remoteExchangeRateCount
    }

    override suspend fun createMigrationStateDoc(householdId: String, migrationId: String, data: Map<String, Any?>): Boolean {
        if (shouldFailMigrationDocCreation) return false
        createdMigrationDocs[migrationId] = data
        return true
    }

    override suspend fun updateMigrationStateDoc(householdId: String, migrationId: String, updates: Map<String, Any?>): Boolean {
        if (shouldFailMigrationDocUpdate) return false
        updatedMigrationDocs.add(Pair(migrationId, updates))
        val current = createdMigrationDocs[migrationId] ?: emptyMap()
        createdMigrationDocs[migrationId] = current + updates
        return true
    }

    override suspend fun uploadCategoriesBatch(householdId: String, categories: List<Map<String, Any?>>): Boolean {
        operationOrder.add("CATEGORIES")
        if (shouldFailCategoryUpload) return false
        uploadedCategoryBatches.add(categories)
        remoteCategoryCount += categories.size
        return true
    }

    override suspend fun uploadExchangeRatesBatch(householdId: String, rates: List<Map<String, Any?>>): Boolean {
        operationOrder.add("RATES")
        if (shouldFailExchangeRateUpload) return false
        uploadedExchangeRateBatches.add(rates)
        remoteExchangeRateCount += rates.size
        return true
    }

    override suspend fun uploadTransactionsBatch(householdId: String, transactions: List<Map<String, Any?>>): Boolean {
        operationOrder.add("TRANSACTIONS")
        if (shouldFailTransactionUpload) return false
        uploadedTransactionBatches.add(transactions)
        remoteTransactionCount += transactions.size
        return true
    }

    fun setMember(householdId: String, userUid: String, role: String, status: String) {
        members[Pair(householdId, userUid)] = mapOf(
            "role" to role,
            "status" to status
        )
    }

    fun emitTransactions(docs: List<Pair<String, Map<String, Any?>>>, removedDocIds: List<String> = emptyList()) {
        txCallbackWithChanges?.invoke(docs, removedDocIds) ?: txCallback?.invoke(docs)
    }

    fun emitCategories(docs: List<Pair<String, Map<String, Any?>>>) {
        catCallback?.invoke(docs)
    }

    fun emitTransactionError(ex: Exception) {
        txErrorCallback?.invoke(ex)
    }

    fun emitCategoryError(ex: Exception) {
        catErrorCallback?.invoke(ex)
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class FirestoreSyncTest {

    private lateinit var db: FinTrackDatabase
    private lateinit var fakeSnapshotSource: FakeSnapshotSource
    private lateinit var syncRepository: FirestoreSyncRepository
    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, FinTrackDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        fakeSnapshotSource = FakeSnapshotSource()
        syncRepository = FirestoreSyncRepository(
            database = db,
            snapshotSource = fakeSnapshotSource,
            coroutineScope = testScope
        )
    }

    @After
    fun tearDown() {
        syncRepository.stopSync()
        db.close()
    }

    @Test
    fun testValidRemoteUpsertAppliesToRoom() = testScope.runTest {
        val resolved = syncRepository.startSync("user_123", "hh_test_1")
        assertEquals("hh_test_1", resolved)
        assertTrue(syncRepository.isListening)

        val txDoc = Pair(
            "tx_remote_1",
            mapOf(
                "transactionId" to "tx_remote_1",
                "householdId" to "hh_test_1",
                "createdByUid" to "user_123",
                "transactionDate" to "2026-08-10",
                "description" to "Remote Coffee",
                "amountRon" to 25.0,
                "amountEur" to 5.0,
                "exchangeRate" to 5.0,
                "type" to "Expense",
                "account" to "Card",
                "category" to "Food"
            )
        )

        fakeSnapshotSource.emitTransactions(listOf(txDoc))
        syncRepository.processTransactionSnapshot(listOf(txDoc))

        val loaded = db.transactionDao().getTransactionById("tx_remote_1")
        assertNotNull(loaded)
        assertEquals("Remote Coffee", loaded!!.description)
        assertEquals(25.0, loaded.amountRON, 0.0001)
        assertEquals("SYNCED", loaded.syncStatus)
    }

    @Test
    fun testTombstoneRemovesEntityFromRoom() = testScope.runTest {
        // Pre-insert local transaction
        val localTx = TransactionEntity(
            id = "tx_to_delete",
            userId = "user_123",
            date = "2026-08-10",
            description = "To be deleted",
            amountRON = 100.0,
            amountEUR = 20.0,
            exchangeRate = 5.0,
            exchangeRateDate = "2026-08-10",
            type = "Expense",
            account = "Cash",
            category = "Other",
            subCategory = "",
            createdAt = 1000L,
            updatedAt = 1000L
        )
        db.transactionDao().insertTransaction(localTx)
        assertNotNull(db.transactionDao().getTransactionById("tx_to_delete"))

        syncRepository.startSync("user_123", "hh_test_1")

        // Emit tombstone for tx_to_delete
        val tombstoneDoc = Pair(
            "tx_to_delete",
            mapOf(
                "transactionId" to "tx_to_delete",
                "householdId" to "hh_test_1",
                "createdByUid" to "user_123",
                "transactionDate" to "2026-08-10",
                "amountRon" to 100.0,
                "amountEur" to 20.0,
                "exchangeRate" to 5.0,
                "type" to "Expense",
                "account" to "Cash",
                "category" to "Other",
                "isDeleted" to true
            )
        )

        fakeSnapshotSource.emitTransactions(listOf(tombstoneDoc))
        syncRepository.processTransactionSnapshot(listOf(tombstoneDoc))

        assertNull(db.transactionDao().getTransactionById("tx_to_delete"))
    }

    @Test
    fun testMalformedDocumentRejectedWithoutModifyingRoom() = testScope.runTest {
        syncRepository.startSync("user_123", "hh_test_1")

        val malformedDoc = Pair(
            "tx_bad",
            mapOf(
                "transactionId" to "tx_bad",
                "type" to "InvalidType", // Invalid type
                "account" to "Card",
                "amountRon" to 10.0
            )
        )

        fakeSnapshotSource.emitTransactions(listOf(malformedDoc))
        syncRepository.processTransactionSnapshot(listOf(malformedDoc))

        assertNull(db.transactionDao().getTransactionById("tx_bad"))
    }

    @Test
    fun testUnrelatedLocalRowsRemainUnchanged() = testScope.runTest {
        val localTx = TransactionEntity(
            id = "tx_unrelated_local",
            userId = "user_123",
            date = "2026-08-01",
            description = "Local Unique Tx",
            amountRON = 500.0,
            amountEUR = 100.0,
            exchangeRate = 5.0,
            exchangeRateDate = "2026-08-01",
            type = "Income",
            account = "Card",
            category = "Salary",
            subCategory = "",
            createdAt = 500L,
            updatedAt = 500L
        )
        db.transactionDao().insertTransaction(localTx)

        syncRepository.startSync("user_123", "hh_test_1")

        val remoteTx = Pair(
            "tx_remote_new",
            mapOf(
                "transactionId" to "tx_remote_new",
                "householdId" to "hh_test_1",
                "createdByUid" to "user_123",
                "transactionDate" to "2026-08-10",
                "description" to "Remote Tx",
                "amountRon" to 50.0,
                "amountEur" to 10.0,
                "exchangeRate" to 5.0,
                "type" to "Expense",
                "account" to "Cash",
                "category" to "Food"
            )
        )

        fakeSnapshotSource.emitTransactions(listOf(remoteTx))
        syncRepository.processTransactionSnapshot(listOf(remoteTx))

        val loadedUnrelated = db.transactionDao().getTransactionById("tx_unrelated_local")
        assertNotNull(loadedUnrelated)
        assertEquals("Local Unique Tx", loadedUnrelated!!.description)
        assertEquals(500.0, loadedUnrelated.amountRON, 0.0001)

        val loadedRemote = db.transactionDao().getTransactionById("tx_remote_new")
        assertNotNull(loadedRemote)
    }

    @Test
    fun testListenerCleanupAndDuplicatePrevention() = testScope.runTest {
        // Start sync first time
        val hh1 = syncRepository.startSync("user_123", "hh_test_1")
        assertEquals("hh_test_1", hh1)
        assertTrue(syncRepository.isListening)
        assertEquals(2, syncRepository.listenerCount)

        // Attempt duplicate sync with same user and household
        val hhDuplicate = syncRepository.startSync("user_123", "hh_test_1")
        assertEquals("hh_test_1", hhDuplicate)
        // Ensure no extra listeners removed/re-registered
        assertEquals(0, fakeSnapshotSource.txListenerRemoveCount)

        // Switch household -> should remove old listeners
        val hh2 = syncRepository.startSync("user_123", "hh_test_2")
        assertEquals("hh_test_2", hh2)
        assertEquals(1, fakeSnapshotSource.txListenerRemoveCount)
        assertEquals(1, fakeSnapshotSource.catListenerRemoveCount)

        // Stop sync -> removes all listeners
        syncRepository.stopSync()
        assertFalse(syncRepository.isListening)
        assertEquals(0, syncRepository.listenerCount)
        assertEquals(2, fakeSnapshotSource.txListenerRemoveCount)
        assertEquals(2, fakeSnapshotSource.catListenerRemoveCount)
    }

    @Test
    fun testHouseholdIdResolutionConvention() = testScope.runTest {
        // When special user provided, resolves via snapshotSource
        val resolvedSpecial = syncRepository.startSync("user_special")
        assertEquals("hh_special_99", resolvedSpecial)

        syncRepository.stopSync()

        // When standard user provided with no active household, returns null and does not start sync
        val resolvedStandard = syncRepository.startSync("user_regular")
        assertNull(resolvedStandard)
        assertFalse(syncRepository.isListening)
        assertNull(syncRepository.activeHouseholdId)
        assertEquals(SyncStatus.NoHousehold, syncRepository.syncStatusState.value)
    }

    @Test
    fun testTwoSnapshotHandshakeTransitionsSyncingToSynced() = testScope.runTest {
        fakeSnapshotSource.autoEmitInitialSnapshot = false

        val resolved = syncRepository.startSync("user_123", "hh_test_1")
        assertEquals("hh_test_1", resolved)
        assertTrue(syncRepository.isListening)

        // Before any snapshot is received, status must be Connecting/Syncing
        assertEquals(SyncStatus.Connecting, syncRepository.syncStatusState.value)
        assertFalse(syncRepository.isHandshakeComplete)
        assertFalse(syncRepository.hasReceivedTxSnapshot)
        assertFalse(syncRepository.hasReceivedCatSnapshot)

        // 1st snapshot arrives (Transactions)
        fakeSnapshotSource.emitTransactions(emptyList())
        assertTrue(syncRepository.hasReceivedTxSnapshot)
        assertFalse(syncRepository.hasReceivedCatSnapshot)
        assertFalse(syncRepository.isHandshakeComplete)
        assertEquals(SyncStatus.Connecting, syncRepository.syncStatusState.value) // Still syncing/connecting!

        // 2nd snapshot arrives (Categories) -> Handshake complete!
        fakeSnapshotSource.emitCategories(emptyList())
        assertTrue(syncRepository.hasReceivedCatSnapshot)
        assertTrue(syncRepository.isHandshakeComplete)
        assertEquals(SyncStatus.Synced("hh_test_1"), syncRepository.syncStatusState.value)
    }

    @Test
    fun testDifferentiatePermissionDeniedVsOfflineInListeners() = testScope.runTest {
        syncRepository.startSync("user_123", "hh_test_1")
        fakeSnapshotSource.emitTransactions(emptyList())
        fakeSnapshotSource.emitCategories(emptyList())
        assertEquals(SyncStatus.Synced("hh_test_1"), syncRepository.syncStatusState.value)

        // Permission denied error in listener
        fakeSnapshotSource.emitTransactionError(SecurityException("PERMISSION_DENIED: User lacks access"))
        assertEquals(SyncStatus.PermissionDenied, syncRepository.syncStatusState.value)

        // Re-start sync and emit offline/network error
        syncRepository.startSync("user_123", "hh_test_1")
        fakeSnapshotSource.emitTransactions(emptyList())
        fakeSnapshotSource.emitCategories(emptyList())
        assertEquals(SyncStatus.Synced("hh_test_1"), syncRepository.syncStatusState.value)

        fakeSnapshotSource.emitCategoryError(java.io.IOException("Network unavailable"))
        assertEquals(SyncStatus.Offline, syncRepository.syncStatusState.value)
    }

    @Test
    fun testDifferentiatePermissionDeniedVsOfflineInHouseholdResolution() = testScope.runTest {
        // Case 1: Permission Denied during resolution
        fakeSnapshotSource.shouldPermissionDenyHouseholdResolution = true
        val resolvedDenied = syncRepository.startSync("user_denied")
        assertNull(resolvedDenied)
        assertFalse(syncRepository.isListening)
        assertEquals(SyncStatus.PermissionDenied, syncRepository.syncStatusState.value)

        // Case 2: Network / Unexpected Failure during resolution
        fakeSnapshotSource.shouldPermissionDenyHouseholdResolution = false
        fakeSnapshotSource.shouldFailHouseholdResolution = true
        val resolvedFailed = syncRepository.startSync("user_fail")
        assertNull(resolvedFailed)
        assertFalse(syncRepository.isListening)
        assertEquals(SyncStatus.Offline, syncRepository.syncStatusState.value)

        // Case 3: No Active Household found
        fakeSnapshotSource.shouldFailHouseholdResolution = false
        val resolvedNone = syncRepository.startSync("user_none")
        assertNull(resolvedNone)
        assertFalse(syncRepository.isListening)
        assertEquals(SyncStatus.NoHousehold, syncRepository.syncStatusState.value)
    }

    @Test
    fun testHouseholdResolutionResultContractDirect() = testScope.runTest {
        fakeSnapshotSource.setMember("hh_active_1", "user_active_1", "MEMBER", "ACTIVE")
        val successResult = syncRepository.resolveHousehold("user_active_1")
        assertTrue(successResult is HouseholdResolutionResult.Success)
        assertEquals("hh_active_1", (successResult as HouseholdResolutionResult.Success).householdId)

        val noneResult = syncRepository.resolveHousehold("user_unknown")
        assertTrue(noneResult is HouseholdResolutionResult.NoHousehold)

        fakeSnapshotSource.shouldPermissionDenyHouseholdResolution = true
        val permResult = syncRepository.resolveHousehold("user_perm_err")
        assertTrue(permResult is HouseholdResolutionResult.PermissionDenied)

        fakeSnapshotSource.shouldPermissionDenyHouseholdResolution = false
        fakeSnapshotSource.shouldFailHouseholdResolution = true
        val failResult = syncRepository.resolveHousehold("user_fail_err")
        assertTrue(failResult is HouseholdResolutionResult.Failure)
    }

    // ========================================================================
    // SyncStatus Accuracy Tests (Step 7.9)
    // ========================================================================

    @Test
    fun test1_inboundHandshakeWithEmptyOutboxProducesSynced() = testScope.runTest {
        syncRepository.startSync("user_123", "hh_test_1")
        testScheduler.advanceUntilIdle()

        fakeSnapshotSource.emitTransactions(emptyList())
        fakeSnapshotSource.emitCategories(emptyList())
        testScheduler.advanceUntilIdle()

        assertEquals(SyncStatus.Synced("hh_test_1"), syncRepository.syncStatusState.value)
    }

    @Test
    fun test2_inboundHandshakeWithPendingOutboxRemainsConnecting() = testScope.runTest {
        db.syncOutboxDao().insertOutboxEntry(
            SyncOutboxEntity(
                id = "outbox_1",
                entityType = "TRANSACTION",
                entityId = "tx_1",
                operation = "UPSERT",
                status = "PENDING"
            )
        )

        syncRepository.startSync("user_123", "hh_test_1")
        testScheduler.advanceUntilIdle()

        fakeSnapshotSource.emitTransactions(emptyList())
        fakeSnapshotSource.emitCategories(emptyList())
        testScheduler.advanceUntilIdle()

        assertEquals(SyncStatus.Connecting, syncRepository.syncStatusState.value)
    }

    @Test
    fun test3_inboundHandshakeWithInProgressOutboxRemainsConnecting() = testScope.runTest {
        db.syncOutboxDao().insertOutboxEntry(
            SyncOutboxEntity(
                id = "outbox_2",
                entityType = "TRANSACTION",
                entityId = "tx_2",
                operation = "UPSERT",
                status = "IN_PROGRESS"
            )
        )

        syncRepository.startSync("user_123", "hh_test_1")
        testScheduler.advanceUntilIdle()

        fakeSnapshotSource.emitTransactions(emptyList())
        fakeSnapshotSource.emitCategories(emptyList())
        testScheduler.advanceUntilIdle()

        assertEquals(SyncStatus.Connecting, syncRepository.syncStatusState.value)
    }

    @Test
    fun test4_inboundHandshakeWithFailedOutboxProducesErrorState() = testScope.runTest {
        db.syncOutboxDao().insertOutboxEntry(
            SyncOutboxEntity(
                id = "outbox_3",
                entityType = "TRANSACTION",
                entityId = "tx_3",
                operation = "UPSERT",
                status = "FAILED",
                errorCode = "PERMISSION_DENIED",
                errorMessage = "Missing authorization"
            )
        )

        syncRepository.startSync("user_123", "hh_test_1")
        testScheduler.advanceUntilIdle()

        fakeSnapshotSource.emitTransactions(emptyList())
        fakeSnapshotSource.emitCategories(emptyList())
        testScheduler.advanceUntilIdle()

        assertEquals(SyncStatus.PermissionDenied, syncRepository.syncStatusState.value)
    }

    @Test
    fun test5_pendingOutboundItemSuccessfullyCompletesTransitionsToSynced() = testScope.runTest {
        val tx4 = TransactionEntity(
            id = "tx_4",
            userId = "user_123",
            date = "2026-08-20",
            description = "Test Tx 4",
            amountRON = 100.0,
            amountEUR = 20.0,
            exchangeRate = 5.0,
            exchangeRateDate = "2026-08-20",
            type = "Expense",
            account = "Card",
            category = "Food",
            subCategory = "",
            householdId = "hh_test_1"
        )
        db.transactionDao().insertTransaction(tx4)
        db.syncOutboxDao().insertOutboxEntry(
            SyncOutboxEntity(
                id = "outbox_4",
                entityType = "TRANSACTION",
                entityId = "tx_4",
                operation = "UPSERT",
                status = "PENDING"
            )
        )

        syncRepository.startSync("user_123", "hh_test_1")
        fakeSnapshotSource.emitTransactions(emptyList())
        fakeSnapshotSource.emitCategories(emptyList())

        // Inbound handshake complete, but outbox has pending item -> Connecting
        assertEquals(SyncStatus.Connecting, syncRepository.syncStatusState.value)

        // Wait for OutboundSyncEngine to process and drain the queue
        syncRepository.outboundSyncEngine.awaitIdle()
        testScheduler.advanceUntilIdle()

        // After outbound engine drains the queue -> Synced
        assertEquals(SyncStatus.Synced("hh_test_1"), syncRepository.syncStatusState.value)
    }

    @Test
    fun test6_newLocalMutationWhileSyncedTransitionsBackToConnectingUntilResolved() = testScope.runTest {
        syncRepository.startSync("user_123", "hh_test_1")
        fakeSnapshotSource.emitTransactions(emptyList())
        fakeSnapshotSource.emitCategories(emptyList())
        testScheduler.advanceUntilIdle()

        assertEquals(SyncStatus.Synced("hh_test_1"), syncRepository.syncStatusState.value)

        val tx5 = TransactionEntity(
            id = "tx_5",
            userId = "user_123",
            date = "2026-08-20",
            description = "Test Tx 5",
            amountRON = 100.0,
            amountEUR = 20.0,
            exchangeRate = 5.0,
            exchangeRateDate = "2026-08-20",
            type = "Expense",
            account = "Card",
            category = "Food",
            subCategory = "",
            householdId = "hh_test_1"
        )
        db.transactionDao().insertTransaction(tx5)
        db.syncOutboxDao().insertOutboxEntry(
            SyncOutboxEntity(
                id = "outbox_5",
                entityType = "TRANSACTION",
                entityId = "tx_5",
                operation = "UPSERT",
                status = "PENDING"
            )
        )
        syncRepository.outboundSyncEngine.notifyPending()

        // Should immediately transition to Connecting
        assertEquals(SyncStatus.Connecting, syncRepository.syncStatusState.value)

        // Wait for outbound engine to process and drain
        syncRepository.outboundSyncEngine.awaitIdle()
        testScheduler.advanceUntilIdle()

        assertEquals(SyncStatus.Synced("hh_test_1"), syncRepository.syncStatusState.value)
    }

    @Test
    fun test7_offlineWithPendingLocalMutationShowsOffline() = testScope.runTest {
        db.syncOutboxDao().insertOutboxEntry(
            SyncOutboxEntity(
                id = "outbox_6",
                entityType = "TRANSACTION",
                entityId = "tx_6",
                operation = "UPSERT",
                status = "PENDING"
            )
        )

        syncRepository.startSync("user_123", "hh_test_1")
        testScheduler.advanceUntilIdle()

        fakeSnapshotSource.emitTransactionError(java.io.IOException("Network unavailable"))
        testScheduler.advanceUntilIdle()

        assertEquals(SyncStatus.Offline, syncRepository.syncStatusState.value)
    }

    @Test
    fun test8_reconnectWithPendingOutboxRemainsConnectingUntilOutboundCompletes() = testScope.runTest {
        val tx7 = TransactionEntity(
            id = "tx_7",
            userId = "user_123",
            date = "2026-08-20",
            description = "Test Tx 7",
            amountRON = 100.0,
            amountEUR = 20.0,
            exchangeRate = 5.0,
            exchangeRateDate = "2026-08-20",
            type = "Expense",
            account = "Card",
            category = "Food",
            subCategory = "",
            householdId = "hh_test_1"
        )
        db.transactionDao().insertTransaction(tx7)
        db.syncOutboxDao().insertOutboxEntry(
            SyncOutboxEntity(
                id = "outbox_7",
                entityType = "TRANSACTION",
                entityId = "tx_7",
                operation = "UPSERT",
                status = "PENDING"
            )
        )

        syncRepository.startSync("user_123", "hh_test_1")
        fakeSnapshotSource.emitTransactionError(java.io.IOException("Network unavailable"))
        assertEquals(SyncStatus.Offline, syncRepository.syncStatusState.value)

        // Reconnect
        syncRepository.startSync("user_123", "hh_test_1")
        fakeSnapshotSource.emitTransactions(emptyList())
        fakeSnapshotSource.emitCategories(emptyList())

        // Inbound handshake complete, but outbox has pending item -> Connecting
        assertEquals(SyncStatus.Connecting, syncRepository.syncStatusState.value)

        // Wait for OutboundSyncEngine to process the queue
        syncRepository.outboundSyncEngine.awaitIdle()
        testScheduler.advanceUntilIdle()

        // After outbound engine drains the queue -> Synced
        assertEquals(SyncStatus.Synced("hh_test_1"), syncRepository.syncStatusState.value)
    }

    @Test
    fun test_inboundTransactionRemovedRemovesLocalRoomEntity() = testScope.runTest {
        val tx = TransactionEntity(
            id = "tx_rem_1",
            userId = "user_123",
            date = "2026-08-20",
            description = "To be deleted remotely",
            amountRON = 150.0,
            amountEUR = 30.0,
            exchangeRate = 5.0,
            exchangeRateDate = "2026-08-20",
            type = "Expense",
            account = "Card",
            category = "Food",
            subCategory = "",
            householdId = "hh_test_1"
        )
        db.transactionDao().insertTransaction(tx)
        assertNotNull(db.transactionDao().getTransactionById("tx_rem_1"))

        syncRepository.startSync("user_123", "hh_test_1")

        // Remote device hard deletes tx_rem_1 -> document change REMOVED
        fakeSnapshotSource.emitTransactions(docs = emptyList(), removedDocIds = listOf("tx_rem_1"))
        syncRepository.processTransactionSnapshot(docs = emptyList(), removedDocIds = listOf("tx_rem_1"))

        // Local Room entity must be deleted
        assertNull(db.transactionDao().getTransactionById("tx_rem_1"))
    }

    @Test
    fun test_inboundTransactionRemovedShieldedByActiveOutbox() = testScope.runTest {
        val tx = TransactionEntity(
            id = "tx_shield_1",
            userId = "user_123",
            date = "2026-08-20",
            description = "Local edit shielded",
            amountRON = 200.0,
            amountEUR = 40.0,
            exchangeRate = 5.0,
            exchangeRateDate = "2026-08-20",
            type = "Expense",
            account = "Card",
            category = "Food",
            subCategory = "",
            householdId = "hh_test_1"
        )
        db.transactionDao().insertTransaction(tx)
        // Enqueue active outbox operation
        db.syncOutboxDao().insertOutboxEntry(
            SyncOutboxEntity(
                id = "outbox_shield_1",
                entityType = "TRANSACTION",
                entityId = "tx_shield_1",
                operation = "UPSERT",
                status = "PENDING"
            )
        )

        var conflictDetected = false
        syncRepository.onConflictDetected = { conflict ->
            if (conflict.entityId == "tx_shield_1") {
                conflictDetected = true
            }
        }

        syncRepository.startSync("user_123", "hh_test_1")

        // Remote device hard-deletes transaction while local mutation is pending
        fakeSnapshotSource.emitTransactions(docs = emptyList(), removedDocIds = listOf("tx_shield_1"))
        syncRepository.processTransactionSnapshot(docs = emptyList(), removedDocIds = listOf("tx_shield_1"))

        // Outbox shield must prevent deletion of local entity
        assertNotNull(db.transactionDao().getTransactionById("tx_shield_1"))
        assertTrue("Conflict event must be emitted for shielded outbox entity", conflictDetected)
    }

    @Test
    fun test_inboundTransactionWithIsDeletedTombstoneRemovesLocalEntity() = testScope.runTest {
        val tx = TransactionEntity(
            id = "tx_tombstone_1",
            userId = "user_123",
            date = "2026-08-20",
            description = "Tombstone legacy doc",
            amountRON = 50.0,
            amountEUR = 10.0,
            exchangeRate = 5.0,
            exchangeRateDate = "2026-08-20",
            type = "Expense",
            account = "Card",
            category = "Food",
            subCategory = "",
            householdId = "hh_test_1"
        )
        db.transactionDao().insertTransaction(tx)
        assertNotNull(db.transactionDao().getTransactionById("tx_tombstone_1"))

        syncRepository.startSync("user_123", "hh_test_1")

        val legacyDoc = mapOf(
            "transactionId" to "tx_tombstone_1",
            "householdId" to "hh_test_1",
            "createdByUid" to "user_123",
            "amountRon" to 50.0,
            "amountEur" to 10.0,
            "exchangeRate" to 5.0,
            "exchangeRateDate" to "2026-08-20",
            "transactionDate" to "2026-08-20",
            "type" to "Expense",
            "account" to "Card",
            "category" to "Food",
            "isDeleted" to true,
            "updatedAt" to 1700000000000L
        )

        val tombstoneList = listOf(Pair("tx_tombstone_1", legacyDoc))
        fakeSnapshotSource.emitTransactions(docs = tombstoneList)
        syncRepository.processTransactionSnapshot(docs = tombstoneList)

        assertNull(db.transactionDao().getTransactionById("tx_tombstone_1"))
    }

    @Test
    fun test_categoryHardDeleteMirrorSyncPreservesHistoricalTransactions() = testScope.runTest {
        val cat = CategoryEntity(
            id = "cat_test_del",
            name = "Entertainment",
            type = "Expense",
            subCategory = "Movies",
            userId = "user_123",
            householdId = "hh_test_1",
            createdAt = 1700000000000L,
            updatedAt = 1700000000000L,
            isDeleted = false
        )
        db.categoryDao().insertCategory(cat)

        val tx = TransactionEntity(
            id = "tx_hist_1",
            userId = "user_123",
            date = "2026-08-20",
            description = "Cinema Tickets",
            amountRON = 100.0,
            amountEUR = 20.0,
            exchangeRate = 5.0,
            exchangeRateDate = "2026-08-20",
            type = "Expense",
            account = "Card",
            category = "Entertainment",
            subCategory = "Movies",
            householdId = "hh_test_1"
        )
        db.transactionDao().insertTransaction(tx)

        syncRepository.startSync("user_123", "hh_test_1")

        // Remote snapshot no longer includes cat_test_del (hard deleted)
        fakeSnapshotSource.emitCategories(emptyList())
        syncRepository.processCategorySnapshot(emptyList())

        // Local category row deleted via mirror sync
        assertNull(db.categoryDao().getCategoryById("cat_test_del"))

        // Historical transaction must survive completely intact
        val preservedTx = db.transactionDao().getTransactionById("tx_hist_1")
        assertNotNull(preservedTx)
        assertEquals("Entertainment", preservedTx!!.category)
        assertEquals("Movies", preservedTx.subCategory)
        assertEquals(100.0, preservedTx.amountRON, 0.001)
    }
}
