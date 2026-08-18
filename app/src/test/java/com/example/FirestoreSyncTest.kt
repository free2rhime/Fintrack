package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.db.FinTrackDatabase
import com.example.data.model.TransactionEntity
import com.example.data.repository.FirestoreSnapshotSource
import com.example.data.repository.FirestoreSyncRepository
import com.example.data.repository.ListenerRegistrationHandle
import com.google.firebase.firestore.FirebaseFirestoreException
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
    var catCallback: ((List<Pair<String, Map<String, Any?>>>) -> Unit)? = null
    var txErrorCallback: ((Exception) -> Unit)? = null
    var catErrorCallback: ((Exception) -> Unit)? = null

    override fun listenToTransactions(
        householdId: String,
        onSnapshot: (List<Pair<String, Map<String, Any?>>>) -> Unit,
        onError: (Exception) -> Unit
    ): ListenerRegistrationHandle {
        lastTxHouseholdId = householdId
        txListenerActive = true
        txCallback = onSnapshot
        txErrorCallback = onError
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
        return object : ListenerRegistrationHandle {
            override fun remove() {
                catListenerActive = false
                catListenerRemoveCount++
            }
        }
    }

    override suspend fun resolveHouseholdId(userUid: String): String? {
        return if (userUid == "user_special") "hh_special_99" else null
    }

    val members = mutableMapOf<Pair<String, String>, Map<String, Any?>>()
    var activeMigrationSession: Map<String, Any?>? = null
    var remoteTransactionCount: Int = 0
    var remoteCategoryCount: Int = 0
    var remoteExchangeRateCount: Int = 0
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
        return remoteTransactionCount
    }

    override suspend fun getRemoteCategoryCount(householdId: String): Int {
        return remoteCategoryCount
    }

    override suspend fun getRemoteExchangeRateCount(householdId: String): Int {
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

    fun emitTransactions(docs: List<Pair<String, Map<String, Any?>>>) {
        txCallback?.invoke(docs)
    }

    fun emitCategories(docs: List<Pair<String, Map<String, Any?>>>) {
        catCallback?.invoke(docs)
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
    fun testListenerErrorsExposePermissionDeniedOrOffline() = testScope.runTest {
        syncRepository.startSync("user_123", "hh_test_1")
        assertEquals("Connecting", syncRepository.syncStatusState.value)

        fakeSnapshotSource.txErrorCallback?.invoke(
            FirebaseFirestoreException(
                "Permission denied",
                FirebaseFirestoreException.Code.PERMISSION_DENIED
            )
        )
        assertEquals("Permission denied", syncRepository.syncStatusState.value)

        fakeSnapshotSource.catErrorCallback?.invoke(Exception("Network unavailable"))
        assertEquals("Offline", syncRepository.syncStatusState.value)
    }

    @Test
    fun testHouseholdIdResolutionConvention() = testScope.runTest {
        // When special user provided, resolves via snapshotSource
        val resolvedSpecial = syncRepository.startSync("user_special")
        assertEquals("hh_special_99", resolvedSpecial)

        syncRepository.stopSync()

        // When no active household exists, listeners are not started.
        val resolvedStandard = syncRepository.startSync("user_regular")
        assertEquals(null, resolvedStandard)
        assertFalse(syncRepository.isListening)
        assertEquals(0, syncRepository.listenerCount)
        assertEquals(null, syncRepository.activeHouseholdId)
        assertEquals("No household", syncRepository.syncStatusState.value)
    }
}
