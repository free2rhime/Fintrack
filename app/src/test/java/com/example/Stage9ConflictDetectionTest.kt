package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.db.FinTrackDatabase
import com.example.data.model.CategoryEntity
import com.example.data.model.SyncOutboxEntity
import com.example.data.model.TransactionEntity
import com.example.data.repository.FirestoreSyncRepository
import com.example.data.repository.SyncConflictEvent
import com.example.data.repository.SyncConflictType
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
class Stage9ConflictDetectionTest {

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
    fun testUpdateVsUpdateDetected() = testScope.runTest {
        val conflicts = mutableListOf<SyncConflictEvent>()
        syncRepository.onConflictDetected = { conflicts.add(it) }

        // Local state: amountRON = 100.0, description = "Lunch"
        val localTx = TransactionEntity(
            id = "tx_conf_1",
            userId = "user_1",
            date = "2026-08-10",
            description = "Lunch",
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
        db.transactionDao().insertTransaction(localTx)

        // Pending outbox UPSERT
        db.syncOutboxDao().insertOutboxEntry(
            SyncOutboxEntity(
                id = "out_conf_1",
                entityType = "TRANSACTION",
                entityId = "tx_conf_1",
                operation = "UPSERT",
                status = "PENDING",
                createdAt = 2000L,
                updatedAt = 2000L
            )
        )

        // Remote snapshot: amountRon = 250.0, description = "Team Lunch"
        val remoteDoc = Pair(
            "tx_conf_1",
            mapOf(
                "transactionId" to "tx_conf_1",
                "householdId" to "hh_1",
                "createdByUid" to "user_2",
                "transactionDate" to "2026-08-10",
                "description" to "Team Lunch",
                "amountRon" to 250.0,
                "amountEur" to 50.0,
                "exchangeRate" to 5.0,
                "type" to "Expense",
                "account" to "Card",
                "category" to "Food"
            )
        )

        syncRepository.processTransactionSnapshot(listOf(remoteDoc))

        // Assert conflict detected and classified
        assertEquals(1, conflicts.size)
        val event = conflicts[0]
        assertEquals("TRANSACTION", event.entityType)
        assertEquals("tx_conf_1", event.entityId)
        assertEquals(SyncConflictType.UPDATE_VS_UPDATE, event.conflictType)
        assertEquals("UPSERT", event.localOperation)
        assertEquals(false, event.remoteIsDeleted)

        // Assert local outbox precedence: Room still has local amount 100.0
        val tx = db.transactionDao().getTransactionById("tx_conf_1")
        assertNotNull(tx)
        assertEquals(100.0, tx!!.amountRON, 0.001)
        assertEquals("Lunch", tx.description)
    }

    @Test
    fun testDeleteVsUpdateDetected() = testScope.runTest {
        val conflicts = mutableListOf<SyncConflictEvent>()
        syncRepository.onConflictDetected = { conflicts.add(it) }

        // Local state: deleted by user, pending outbox DELETE
        db.syncOutboxDao().insertOutboxEntry(
            SyncOutboxEntity(
                id = "out_del_1",
                entityType = "TRANSACTION",
                entityId = "tx_del_vs_upd",
                operation = "DELETE",
                status = "PENDING",
                createdAt = 2000L,
                updatedAt = 2000L
            )
        )

        // Remote snapshot: user 2 edited it (active, not deleted)
        val remoteDoc = Pair(
            "tx_del_vs_upd",
            mapOf(
                "transactionId" to "tx_del_vs_upd",
                "householdId" to "hh_1",
                "createdByUid" to "user_2",
                "transactionDate" to "2026-08-10",
                "description" to "Remote Edit on Deleted Item",
                "amountRon" to 80.0,
                "amountEur" to 16.0,
                "exchangeRate" to 5.0,
                "type" to "Expense",
                "account" to "Card",
                "category" to "Food",
                "isDeleted" to false
            )
        )

        syncRepository.processTransactionSnapshot(listOf(remoteDoc))

        // Assert conflict detected as DELETE_VS_UPDATE
        assertEquals(1, conflicts.size)
        val event = conflicts[0]
        assertEquals("TRANSACTION", event.entityType)
        assertEquals("tx_del_vs_upd", event.entityId)
        assertEquals(SyncConflictType.DELETE_VS_UPDATE, event.conflictType)
        assertEquals("DELETE", event.localOperation)
        assertEquals(false, event.remoteIsDeleted)

        // Assert local outbox precedence: transaction remains deleted locally
        val tx = db.transactionDao().getTransactionById("tx_del_vs_upd")
        assertNull(tx)
    }

    @Test
    fun testUpdateVsDeleteDetected() = testScope.runTest {
        val conflicts = mutableListOf<SyncConflictEvent>()
        syncRepository.onConflictDetected = { conflicts.add(it) }

        // Local state: active local edit (UPSERT)
        val localTx = TransactionEntity(
            id = "tx_upd_vs_del",
            userId = "user_1",
            date = "2026-08-10",
            description = "Resurrected Edit",
            amountRON = 300.0,
            amountEUR = 60.0,
            exchangeRate = 5.0,
            exchangeRateDate = "2026-08-10",
            type = "Expense",
            account = "Card",
            category = "Utilities",
            subCategory = "",
            createdAt = 1000L,
            updatedAt = 2000L
        )
        db.transactionDao().insertTransaction(localTx)

        db.syncOutboxDao().insertOutboxEntry(
            SyncOutboxEntity(
                id = "out_upd_1",
                entityType = "TRANSACTION",
                entityId = "tx_upd_vs_del",
                operation = "UPSERT",
                status = "PENDING",
                createdAt = 2000L,
                updatedAt = 2000L
            )
        )

        // Remote snapshot: remote tombstone (isDeleted = true)
        val remoteDoc = Pair(
            "tx_upd_vs_del",
            mapOf(
                "transactionId" to "tx_upd_vs_del",
                "householdId" to "hh_1",
                "createdByUid" to "user_2",
                "transactionDate" to "2026-08-10",
                "description" to "Deleted",
                "amountRon" to 300.0,
                "amountEur" to 60.0,
                "exchangeRate" to 5.0,
                "type" to "Expense",
                "account" to "Card",
                "category" to "Utilities",
                "isDeleted" to true
            )
        )

        syncRepository.processTransactionSnapshot(listOf(remoteDoc))

        // Assert conflict detected as UPDATE_VS_DELETE
        assertEquals(1, conflicts.size)
        val event = conflicts[0]
        assertEquals("TRANSACTION", event.entityType)
        assertEquals("tx_upd_vs_del", event.entityId)
        assertEquals(SyncConflictType.UPDATE_VS_DELETE, event.conflictType)
        assertEquals("UPSERT", event.localOperation)
        assertEquals(true, event.remoteIsDeleted)

        // Assert local outbox precedence: local transaction is preserved
        val tx = db.transactionDao().getTransactionById("tx_upd_vs_del")
        assertNotNull(tx)
        assertEquals(300.0, tx!!.amountRON, 0.001)
        assertEquals("Resurrected Edit", tx.description)
    }

    @Test
    fun testIdenticalContentNoConflictLogged() = testScope.runTest {
        val conflicts = mutableListOf<SyncConflictEvent>()
        syncRepository.onConflictDetected = { conflicts.add(it) }

        // Local state
        val localTx = TransactionEntity(
            id = "tx_same",
            userId = "user_1",
            date = "2026-08-10",
            description = "Exact Same Description",
            amountRON = 50.0,
            amountEUR = 10.0,
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

        db.syncOutboxDao().insertOutboxEntry(
            SyncOutboxEntity(
                id = "out_same",
                entityType = "TRANSACTION",
                entityId = "tx_same",
                operation = "UPSERT",
                status = "PENDING",
                createdAt = 2000L,
                updatedAt = 2000L
            )
        )

        // Remote snapshot matches exactly
        val remoteDoc = Pair(
            "tx_same",
            mapOf(
                "transactionId" to "tx_same",
                "householdId" to "hh_1",
                "createdByUid" to "user_1",
                "transactionDate" to "2026-08-10",
                "description" to "Exact Same Description",
                "amountRon" to 50.0,
                "amountEur" to 10.0,
                "exchangeRate" to 5.0,
                "type" to "Expense",
                "account" to "Card",
                "category" to "Food",
                "subCategory" to "",
                "isDeleted" to false
            )
        )

        syncRepository.processTransactionSnapshot(listOf(remoteDoc))

        // Assert NO conflict is reported
        assertEquals(0, conflicts.size)
        // Entity was still shielded from unnecessary Room rewrite
        val tx = db.transactionDao().getTransactionById("tx_same")
        assertNotNull(tx)
        assertEquals(50.0, tx!!.amountRON, 0.001)
    }

    @Test
    fun testCategoryConflictDetected() = testScope.runTest {
        val conflicts = mutableListOf<SyncConflictEvent>()
        syncRepository.onConflictDetected = { conflicts.add(it) }

        val localCat = CategoryEntity(
            id = "cat_conf_1",
            name = "Groceries Local",
            type = "Expense",
            subCategory = "Food",
            createdAt = 1000L,
            updatedAt = 2000L
        )
        db.categoryDao().insertCategory(localCat)

        db.syncOutboxDao().insertOutboxEntry(
            SyncOutboxEntity(
                id = "out_cat_c1",
                entityType = "CATEGORY",
                entityId = "cat_conf_1",
                operation = "UPSERT",
                status = "PENDING",
                createdAt = 2000L,
                updatedAt = 2000L
            )
        )

        // Remote has different name and subCategory
        val remoteDoc = Pair(
            "cat_conf_1",
            mapOf(
                "categoryId" to "cat_conf_1",
                "householdId" to "hh_1",
                "name" to "Groceries Remote",
                "type" to "Expense",
                "subCategory" to "Wholesale"
            )
        )

        syncRepository.processCategorySnapshot(listOf(remoteDoc))

        assertEquals(1, conflicts.size)
        val event = conflicts[0]
        assertEquals("CATEGORY", event.entityType)
        assertEquals("cat_conf_1", event.entityId)
        assertEquals(SyncConflictType.UPDATE_VS_UPDATE, event.conflictType)

        val cat = db.categoryDao().getCategoryById("cat_conf_1")
        assertNotNull(cat)
        assertEquals("Groceries Local", cat!!.name)
        assertEquals("Food", cat.subCategory)
    }
}
