package com.example

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.data.db.FinTrackDatabase
import com.example.data.model.SyncOutboxEntity
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [36])
class SyncOutboxDaoHardeningTest {

    private lateinit var database: FinTrackDatabase
    private lateinit var outboxDao: com.example.data.dao.SyncOutboxDao

    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            FinTrackDatabase::class.java
        ).allowMainThreadQueries().build()

        outboxDao = database.syncOutboxDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun testGetPendingBatchRetrievesFifoWithLimit() = runBlocking {
        val entry1 = SyncOutboxEntity(
            id = "outbox_1",
            entityType = "TRANSACTION",
            entityId = "tx_1",
            operation = "UPSERT",
            status = "PENDING",
            createdAt = 1000L
        )
        val entry2 = SyncOutboxEntity(
            id = "outbox_2",
            entityType = "TRANSACTION",
            entityId = "tx_2",
            operation = "UPSERT",
            status = "PENDING",
            createdAt = 2000L
        )
        val entry3 = SyncOutboxEntity(
            id = "outbox_3",
            entityType = "CATEGORY",
            entityId = "cat_1",
            operation = "UPSERT",
            status = "PENDING",
            createdAt = 3000L
        )
        val entryAck = SyncOutboxEntity(
            id = "outbox_4",
            entityType = "TRANSACTION",
            entityId = "tx_3",
            operation = "UPSERT",
            status = "ACKNOWLEDGED",
            createdAt = 500L
        )

        outboxDao.insertAllOutboxEntries(listOf(entry2, entry3, entry1, entryAck))

        // Batch of 2 should return entry1 and entry2 (FIFO by createdAt)
        val batch = outboxDao.getPendingBatch(limit = 2)
        assertEquals(2, batch.size)
        assertEquals("outbox_1", batch[0].id)
        assertEquals(1000L, batch[0].createdAt)
        assertEquals("outbox_2", batch[1].id)
        assertEquals(2000L, batch[1].createdAt)

        // Pending count should be 3
        assertEquals(3, outboxDao.getPendingCount())
    }

    @Test
    fun testStatusTransitionsUpdateStateAndTimestamps() = runBlocking {
        val entry = SyncOutboxEntity(
            id = "test_item",
            entityType = "TRANSACTION",
            entityId = "tx_100",
            operation = "UPSERT",
            status = "PENDING",
            createdAt = 1000L,
            updatedAt = 1000L
        )
        outboxDao.insertOutboxEntry(entry)

        // 1. Transition PENDING -> IN_PROGRESS
        val updatedRows1 = outboxDao.markInProgress(id = "test_item", lastAttemptAt = 2000L, updatedAt = 2000L)
        assertEquals(1, updatedRows1)

        val inProgressItem = outboxDao.getEntryById("test_item")
        assertNotNull(inProgressItem)
        assertEquals("IN_PROGRESS", inProgressItem?.status)
        assertEquals(2000L, inProgressItem?.lastAttemptAt)
        assertEquals(2000L, inProgressItem?.updatedAt)

        // 2. Transition IN_PROGRESS -> FAILED
        val updatedRows2 = outboxDao.markFailed(
            id = "test_item",
            errorCode = "UNAVAILABLE",
            errorMessage = "Service unavailable",
            retryCount = 1,
            lastAttemptAt = 3000L,
            updatedAt = 3000L
        )
        assertEquals(1, updatedRows2)

        val failedItem = outboxDao.getEntryById("test_item")
        assertEquals("FAILED", failedItem?.status)
        assertEquals("UNAVAILABLE", failedItem?.errorCode)
        assertEquals("Service unavailable", failedItem?.errorMessage)
        assertEquals(1, failedItem?.retryCount)

        // 3. Transition FAILED -> PENDING (manual retry or re-queue)
        val updatedRows3 = outboxDao.markPending(id = "test_item", updatedAt = 4000L)
        assertEquals(1, updatedRows3)
        val pendingItem = outboxDao.getEntryById("test_item")
        assertEquals("PENDING", pendingItem?.status)

        // 4. Transition -> ACKNOWLEDGED
        outboxDao.markAcknowledged(id = "test_item", updatedAt = 5000L)
        val ackItem = outboxDao.getEntryById("test_item")
        assertEquals("ACKNOWLEDGED", ackItem?.status)
        assertEquals(5000L, ackItem?.updatedAt)
    }

    @Test
    fun testStartupRecoveryResetsInProgressToPending() = runBlocking {
        val pending = SyncOutboxEntity(
            id = "item_pending",
            entityType = "TRANSACTION",
            entityId = "tx_1",
            operation = "UPSERT",
            status = "PENDING"
        )
        val inProgress1 = SyncOutboxEntity(
            id = "item_in_progress_1",
            entityType = "TRANSACTION",
            entityId = "tx_2",
            operation = "UPSERT",
            status = "IN_PROGRESS"
        )
        val inProgress2 = SyncOutboxEntity(
            id = "item_in_progress_2",
            entityType = "CATEGORY",
            entityId = "cat_1",
            operation = "DELETE",
            status = "IN_PROGRESS"
        )
        val acknowledged = SyncOutboxEntity(
            id = "item_ack",
            entityType = "TRANSACTION",
            entityId = "tx_3",
            operation = "UPSERT",
            status = "ACKNOWLEDGED"
        )

        outboxDao.insertAllOutboxEntries(listOf(pending, inProgress1, inProgress2, acknowledged))

        // Execute startup recovery
        val resetCount = outboxDao.resetInProgressToPending(updatedAt = 9999L)
        assertEquals(2, resetCount)

        val item1 = outboxDao.getEntryById("item_in_progress_1")
        assertEquals("PENDING", item1?.status)
        assertEquals(9999L, item1?.updatedAt)

        val item2 = outboxDao.getEntryById("item_in_progress_2")
        assertEquals("PENDING", item2?.status)

        val ack = outboxDao.getEntryById("item_ack")
        assertEquals("ACKNOWLEDGED", ack?.status) // Untouched
    }

    @Test
    fun testRetryTrackingAndFailureRecord() = runBlocking {
        val entry = SyncOutboxEntity(
            id = "retry_item",
            entityType = "TRANSACTION",
            entityId = "tx_retry",
            operation = "UPSERT",
            status = "IN_PROGRESS",
            retryCount = 0
        )
        outboxDao.insertOutboxEntry(entry)

        // Record a retry failure
        val rows = outboxDao.recordRetryFailure(
            id = "retry_item",
            errorCode = "NETWORK_ERROR",
            errorMessage = "Socket timeout",
            lastAttemptAt = 5000L,
            updatedAt = 5000L
        )
        assertEquals(1, rows)

        val fetched = outboxDao.getEntryById("retry_item")
        assertEquals("PENDING", fetched?.status)
        assertEquals(1, fetched?.retryCount)
        assertEquals("NETWORK_ERROR", fetched?.errorCode)
        assertEquals("Socket timeout", fetched?.errorMessage)
        assertEquals(5000L, fetched?.lastAttemptAt)

        // Increment retry count again
        outboxDao.incrementRetryCount(
            id = "retry_item",
            lastAttemptAt = 6000L,
            errorCode = "TIMEOUT",
            errorMessage = "Deadline exceeded",
            updatedAt = 6000L
        )
        val fetchedAgain = outboxDao.getEntryById("retry_item")
        assertEquals(2, fetchedAgain?.retryCount)
        assertEquals("TIMEOUT", fetchedAgain?.errorCode)
    }

    @Test
    fun testQueueCleanupDeletesCompletedAndOldFailedRecords() = runBlocking {
        val ack = SyncOutboxEntity(
            id = "ack_1",
            entityType = "TRANSACTION",
            entityId = "tx_1",
            operation = "UPSERT",
            status = "ACKNOWLEDGED",
            updatedAt = 1000L
        )
        val success = SyncOutboxEntity(
            id = "success_1",
            entityType = "TRANSACTION",
            entityId = "tx_2",
            operation = "UPSERT",
            status = "SUCCESS",
            updatedAt = 1000L
        )
        val oldFailed = SyncOutboxEntity(
            id = "failed_old",
            entityType = "TRANSACTION",
            entityId = "tx_3",
            operation = "UPSERT",
            status = "FAILED",
            updatedAt = 500L
        )
        val recentFailed = SyncOutboxEntity(
            id = "failed_recent",
            entityType = "TRANSACTION",
            entityId = "tx_4",
            operation = "UPSERT",
            status = "FAILED",
            updatedAt = 2500L
        )
        val pending = SyncOutboxEntity(
            id = "pending_1",
            entityType = "TRANSACTION",
            entityId = "tx_5",
            operation = "UPSERT",
            status = "PENDING",
            updatedAt = 500L
        )

        outboxDao.insertAllOutboxEntries(listOf(ack, success, oldFailed, recentFailed, pending))

        // 1. Delete acknowledged
        val deletedAck = outboxDao.deleteAcknowledgedEntries()
        assertEquals(1, deletedAck)
        assertNull(outboxDao.getEntryById("ack_1"))

        // 2. Delete old completed entries
        val deletedSuccess = outboxDao.deleteSuccessEntries()
        assertEquals(1, deletedSuccess)
        assertNull(outboxDao.getEntryById("success_1"))

        // 3. Delete old failed records before cutoff 1000L
        val deletedOldFailed = outboxDao.deleteOldFailedEntries(cutoffTime = 1000L)
        assertEquals(1, deletedOldFailed)
        assertNull(outboxDao.getEntryById("failed_old"))
        assertNotNull(outboxDao.getEntryById("failed_recent"))

        // 4. Pending entry should remain intact
        assertNotNull(outboxDao.getEntryById("pending_1"))
    }

    @Test
    fun testDuplicateSuppressionQueriesByEntityTypeAndId() = runBlocking {
        val entry1 = SyncOutboxEntity(
            id = "dup_1",
            entityType = "CATEGORY",
            entityId = "cat_food",
            operation = "UPSERT",
            status = "PENDING",
            createdAt = 1000L
        )
        val entry2 = SyncOutboxEntity(
            id = "dup_2",
            entityType = "TRANSACTION",
            entityId = "cat_food", // same ID but different entity type
            operation = "UPSERT",
            status = "PENDING",
            createdAt = 2000L
        )

        outboxDao.insertAllOutboxEntries(listOf(entry1, entry2))

        // Query by entityType and entityId
        val foundCategory = outboxDao.getPendingEntry(entityType = "CATEGORY", entityId = "cat_food")
        assertNotNull(foundCategory)
        assertEquals("dup_1", foundCategory?.id)

        val foundTx = outboxDao.getPendingEntry(entityType = "TRANSACTION", entityId = "cat_food")
        assertNotNull(foundTx)
        assertEquals("dup_2", foundTx?.id)

        // Active entry check (in progress or pending)
        outboxDao.markInProgress("dup_1", 3000L, 3000L)
        val activeCategory = outboxDao.getActiveEntry("CATEGORY", "cat_food")
        assertNotNull(activeCategory)
        assertEquals("IN_PROGRESS", activeCategory?.status)

        // Delete entries for specific entity
        val deletedCount = outboxDao.deleteEntriesForEntity("CATEGORY", "cat_food")
        assertEquals(1, deletedCount)
        assertNull(outboxDao.getEntryById("dup_1"))
        assertNotNull(outboxDao.getEntryById("dup_2"))
    }
}
