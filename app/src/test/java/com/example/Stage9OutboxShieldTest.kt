package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.db.FinTrackDatabase
import com.example.data.model.CategoryEntity
import com.example.data.model.SyncOutboxEntity
import com.example.data.model.TransactionEntity
import com.example.data.repository.FirestoreSyncRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class Stage9OutboxShieldTest {

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
    fun testPendingUpdateShielded() = testScope.runTest {
        // Local transaction edited offline (amount = 150.0)
        val localTx = TransactionEntity(
            id = "tx_shield_1",
            userId = "user_1",
            date = "2026-08-10",
            description = "Local Offline Coffee",
            amountRON = 150.0,
            amountEUR = 30.0,
            exchangeRate = 5.0,
            exchangeRateDate = "2026-08-10",
            type = "Expense",
            account = "Card",
            category = "Food",
            subCategory = "",
            createdAt = 1000L,
            updatedAt = 2000L
        )
        db.transactionDao().insertTransaction(localTx)

        // Active PENDING outbox entry
        db.syncOutboxDao().insertOutboxEntry(
            SyncOutboxEntity(
                id = "outbox_1",
                entityType = "TRANSACTION",
                entityId = "tx_shield_1",
                operation = "UPSERT",
                payload = """{"amountRon": 150.0}""",
                status = "PENDING",
                retryCount = 0,
                createdAt = 2000L,
                updatedAt = 2000L
            )
        )

        // Incoming remote snapshot from Device B with amount = 200.0
        val remoteDoc = Pair(
            "tx_shield_1",
            mapOf(
                "transactionId" to "tx_shield_1",
                "householdId" to "hh_1",
                "createdByUid" to "user_2",
                "transactionDate" to "2026-08-10",
                "description" to "Remote Coffee Overwrite",
                "amountRon" to 200.0,
                "amountEur" to 40.0,
                "exchangeRate" to 5.0,
                "type" to "Expense",
                "account" to "Card",
                "category" to "Food",
                "updatedAt" to 2500L
            )
        )

        syncRepository.processTransactionSnapshot(listOf(remoteDoc))

        // Assert: Local Room record is SHIELDED and remains untouched (amount = 150.0)
        val currentTx = db.transactionDao().getTransactionById("tx_shield_1")
        assertNotNull(currentTx)
        assertEquals(150.0, currentTx!!.amountRON, 0.001)
        assertEquals("Local Offline Coffee", currentTx.description)
    }

    @Test
    fun testPendingDeleteShielded() = testScope.runTest {
        // User deleted tx_shield_del locally and outbox has PENDING delete
        db.syncOutboxDao().insertOutboxEntry(
            SyncOutboxEntity(
                id = "outbox_del",
                entityType = "TRANSACTION",
                entityId = "tx_shield_del",
                operation = "DELETE",
                payload = """{}""",
                status = "PENDING",
                retryCount = 0,
                createdAt = 2000L,
                updatedAt = 2000L
            )
        )

        // Incoming remote snapshot attempts to resurrect or update tx_shield_del
        val remoteDoc = Pair(
            "tx_shield_del",
            mapOf(
                "transactionId" to "tx_shield_del",
                "householdId" to "hh_1",
                "createdByUid" to "user_2",
                "transactionDate" to "2026-08-10",
                "description" to "Resurrected Transaction",
                "amountRon" to 50.0,
                "amountEur" to 10.0,
                "exchangeRate" to 5.0,
                "type" to "Expense",
                "account" to "Card",
                "category" to "Food"
            )
        )

        syncRepository.processTransactionSnapshot(listOf(remoteDoc))

        // Assert: tx_shield_del was NOT inserted into Room because it is shielded by pending delete
        val currentTx = db.transactionDao().getTransactionById("tx_shield_del")
        assertNull(currentTx)
    }

    @Test
    fun testInProgressShielded() = testScope.runTest {
        val localTx = TransactionEntity(
            id = "tx_in_prog",
            userId = "user_1",
            date = "2026-08-10",
            description = "Local In-Progress Tx",
            amountRON = 80.0,
            amountEUR = 16.0,
            exchangeRate = 5.0,
            exchangeRateDate = "2026-08-10",
            type = "Expense",
            account = "Cash",
            category = "General",
            subCategory = "",
            createdAt = 1000L,
            updatedAt = 2000L
        )
        db.transactionDao().insertTransaction(localTx)

        // Active IN_PROGRESS outbox entry (engine currently dispatching)
        db.syncOutboxDao().insertOutboxEntry(
            SyncOutboxEntity(
                id = "outbox_prog",
                entityType = "TRANSACTION",
                entityId = "tx_in_prog",
                operation = "UPSERT",
                payload = """{}""",
                status = "IN_PROGRESS",
                retryCount = 1,
                createdAt = 2000L,
                updatedAt = 2100L
            )
        )

        // Incoming snapshot during flight
        val remoteDoc = Pair(
            "tx_in_prog",
            mapOf(
                "transactionId" to "tx_in_prog",
                "householdId" to "hh_1",
                "createdByUid" to "user_2",
                "transactionDate" to "2026-08-10",
                "description" to "Stale Remote",
                "amountRon" to 300.0,
                "amountEur" to 60.0,
                "exchangeRate" to 5.0,
                "type" to "Expense",
                "account" to "Cash",
                "category" to "General"
            )
        )

        syncRepository.processTransactionSnapshot(listOf(remoteDoc))

        val currentTx = db.transactionDao().getTransactionById("tx_in_prog")
        assertNotNull(currentTx)
        assertEquals(80.0, currentTx!!.amountRON, 0.001)
        assertEquals("Local In-Progress Tx", currentTx.description)
    }

    @Test
    fun testMixedBatchSnapshot() = testScope.runTest {
        // 1. tx_1 is local pending
        val localTx1 = TransactionEntity(
            id = "tx_1",
            userId = "user_1",
            date = "2026-08-10",
            description = "Protected tx_1",
            amountRON = 100.0,
            amountEUR = 20.0,
            exchangeRate = 5.0,
            exchangeRateDate = "2026-08-10",
            type = "Expense",
            account = "Card",
            category = "Food",
            subCategory = "",
            createdAt = 1000L,
            updatedAt = 2000L
        )
        db.transactionDao().insertTransaction(localTx1)
        db.syncOutboxDao().insertOutboxEntry(
            SyncOutboxEntity(
                id = "out_1",
                entityType = "TRANSACTION",
                entityId = "tx_1",
                operation = "UPSERT",
                payload = """{}""",
                status = "PENDING",
                createdAt = 2000L,
                updatedAt = 2000L
            )
        )

        // 2. tx_3 exists locally and will be deleted by remote tombstone
        val localTx3 = TransactionEntity(
            id = "tx_3",
            userId = "user_1",
            date = "2026-08-10",
            description = "To be deleted tx_3",
            amountRON = 50.0,
            amountEUR = 10.0,
            exchangeRate = 5.0,
            exchangeRateDate = "2026-08-10",
            type = "Expense",
            account = "Card",
            category = "Food",
            subCategory = "",
            createdAt = 1000L,
            updatedAt = 1000L
        )
        db.transactionDao().insertTransaction(localTx3)

        // Batch containing:
        // - tx_1 (shielded overwrite)
        // - tx_2 (unshielded new remote upsert)
        // - tx_3 (unshielded remote tombstone)
        val batch = listOf(
            Pair(
                "tx_1",
                mapOf(
                    "transactionId" to "tx_1",
                    "householdId" to "hh_1",
                    "createdByUid" to "user_2",
                    "transactionDate" to "2026-08-10",
                    "description" to "Attempted Overwrite tx_1",
                    "amountRon" to 999.0,
                    "amountEur" to 199.8,
                    "exchangeRate" to 5.0,
                    "type" to "Expense",
                    "account" to "Card",
                    "category" to "Food"
                )
            ),
            Pair(
                "tx_2",
                mapOf(
                    "transactionId" to "tx_2",
                    "householdId" to "hh_1",
                    "createdByUid" to "user_2",
                    "transactionDate" to "2026-08-10",
                    "description" to "Brand New Remote tx_2",
                    "amountRon" to 75.0,
                    "amountEur" to 15.0,
                    "exchangeRate" to 5.0,
                    "type" to "Expense",
                    "account" to "Card",
                    "category" to "Food"
                )
            ),
            Pair(
                "tx_3",
                mapOf(
                    "transactionId" to "tx_3",
                    "householdId" to "hh_1",
                    "createdByUid" to "user_2",
                    "transactionDate" to "2026-08-10",
                    "description" to "Deleted",
                    "amountRon" to 50.0,
                    "amountEur" to 10.0,
                    "exchangeRate" to 5.0,
                    "type" to "Expense",
                    "account" to "Card",
                    "category" to "Food",
                    "isDeleted" to true
                )
            )
        )

        syncRepository.processTransactionSnapshot(batch)

        // Assertions:
        // tx_1 kept local amount 100.0
        val resTx1 = db.transactionDao().getTransactionById("tx_1")
        assertNotNull(resTx1)
        assertEquals(100.0, resTx1!!.amountRON, 0.001)
        assertEquals("Protected tx_1", resTx1.description)

        // tx_2 upserted
        val resTx2 = db.transactionDao().getTransactionById("tx_2")
        assertNotNull(resTx2)
        assertEquals(75.0, resTx2!!.amountRON, 0.001)
        assertEquals("Brand New Remote tx_2", resTx2.description)

        // tx_3 deleted
        val resTx3 = db.transactionDao().getTransactionById("tx_3")
        assertNull(resTx3)
    }

    @Test
    fun testNormalInboundPathWithoutActiveOutbox() = testScope.runTest {
        val remoteDoc = Pair(
            "tx_unshielded",
            mapOf(
                "transactionId" to "tx_unshielded",
                "householdId" to "hh_1",
                "createdByUid" to "user_2",
                "transactionDate" to "2026-08-10",
                "description" to "Normal Inbound",
                "amountRon" to 42.0,
                "amountEur" to 8.4,
                "exchangeRate" to 5.0,
                "type" to "Expense",
                "account" to "Card",
                "category" to "Food"
            )
        )

        syncRepository.processTransactionSnapshot(listOf(remoteDoc))

        val loaded = db.transactionDao().getTransactionById("tx_unshielded")
        assertNotNull(loaded)
        assertEquals(42.0, loaded!!.amountRON, 0.001)
        assertEquals("Normal Inbound", loaded.description)
    }

    @Test
    fun testEchoAfterSuccess() = testScope.runTest {
        // Outbox was marked SUCCESS (or ACKNOWLEDGED)
        db.syncOutboxDao().insertOutboxEntry(
            SyncOutboxEntity(
                id = "out_done",
                entityType = "TRANSACTION",
                entityId = "tx_echo",
                operation = "UPSERT",
                payload = """{}""",
                status = "SUCCESS",
                retryCount = 0,
                createdAt = 1000L,
                updatedAt = 1500L
            )
        )

        // Inbound echo snapshot arrives
        val echoDoc = Pair(
            "tx_echo",
            mapOf(
                "transactionId" to "tx_echo",
                "householdId" to "hh_1",
                "createdByUid" to "user_1",
                "transactionDate" to "2026-08-10",
                "description" to "Confirmed Echo",
                "amountRon" to 120.0,
                "amountEur" to 24.0,
                "exchangeRate" to 5.0,
                "type" to "Expense",
                "account" to "Card",
                "category" to "Food"
            )
        )

        syncRepository.processTransactionSnapshot(listOf(echoDoc))

        // Assert: Because status is SUCCESS (not active PENDING/IN_PROGRESS), echo applies cleanly
        val loaded = db.transactionDao().getTransactionById("tx_echo")
        assertNotNull(loaded)
        assertEquals(120.0, loaded!!.amountRON, 0.001)
        assertEquals("Confirmed Echo", loaded.description)
    }

    @Test
    fun testCategoryPendingUpdateShielded() = testScope.runTest {
        val localCat = CategoryEntity(
            id = "cat_1",
            name = "Local Supermarket",
            type = "Expense",
            subCategory = "Groceries",
            createdAt = 1000L,
            updatedAt = 2000L
        )
        db.categoryDao().insertCategory(localCat)

        db.syncOutboxDao().insertOutboxEntry(
            SyncOutboxEntity(
                id = "out_cat_1",
                entityType = "CATEGORY",
                entityId = "cat_1",
                operation = "UPSERT",
                payload = """{}""",
                status = "PENDING",
                createdAt = 2000L,
                updatedAt = 2000L
            )
        )

        val remoteDoc = Pair(
            "cat_1",
            mapOf(
                "categoryId" to "cat_1",
                "householdId" to "hh_1",
                "name" to "Remote Supermarket Overwrite",
                "type" to "Expense",
                "subCategory" to "Wholesale"
            )
        )

        syncRepository.processCategorySnapshot(listOf(remoteDoc))

        val loaded = db.categoryDao().getCategoryById("cat_1")
        assertNotNull(loaded)
        assertEquals("Local Supermarket", loaded!!.name)
        assertEquals("Groceries", loaded.subCategory)
    }
}
