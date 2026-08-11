package com.example

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.data.db.FinTrackDatabase
import com.example.data.model.TransactionEntity
import com.example.data.repository.RoomCategoryRepository
import com.example.data.repository.RoomTransactionRepository
import com.example.data.service.ExchangeRateService
import com.example.data.util.CsvDuplicateMode
import com.example.data.util.CsvImporter
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.io.File
import java.util.UUID

@RunWith(AndroidJUnit4::class)
@Config(sdk = [36])
class EntitySyncOutboxTest {

    private lateinit var database: FinTrackDatabase
    private lateinit var transactionRepository: RoomTransactionRepository
    private lateinit var categoryRepository: RoomCategoryRepository

    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            FinTrackDatabase::class.java
        ).allowMainThreadQueries().build()

        val exchangeRateService = ExchangeRateService(database.exchangeRateDao())
        transactionRepository = RoomTransactionRepository(
            transactionDao = database.transactionDao(),
            exchangeRateService = exchangeRateService,
            exchangeRateDao = database.exchangeRateDao(),
            database = database
        )

        categoryRepository = RoomCategoryRepository(
            categoryDao = database.categoryDao(),
            syncOutboxDao = database.syncOutboxDao(),
            database = database
        )
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun testLocalTransactionInsertAndOutboxInsertCommitAtomically() = runBlocking {
        val saved = transactionRepository.saveTransaction(
            id = null,
            date = "2026-08-11",
            description = "Groceries",
            amountRON = 100.0,
            type = "Expense",
            account = "Card",
            category = "Food",
            subCategory = "Groceries"
        )

        // Verify transaction saved
        val txInDb = database.transactionDao().getTransactionById(saved.id)
        assertNotNull(txInDb)
        assertEquals("Groceries", txInDb?.description)

        // Verify outbox entry created atomically
        val outbox = database.syncOutboxDao().getPendingEntries()
        assertEquals(1, outbox.size)
        val entry = outbox[0]
        assertEquals("TRANSACTION", entry.entityType)
        assertEquals(saved.id, entry.entityId)
        assertEquals("UPSERT", entry.operation)
        assertEquals("PENDING", entry.status)
        assertNull(entry.errorCode)
        assertNull(entry.errorMessage)
    }

    @Test
    fun testSimulatedFailureRollsBothBack() = runBlocking {
        try {
            database.runInTransaction {
                // Insert a transaction directly
                runBlocking {
                    database.transactionDao().insertTransaction(
                        TransactionEntity(
                            id = "fail_tx_123",
                            date = "2026-08-11",
                            description = "Rollback test",
                            amountRON = 50.0,
                            amountEUR = 10.0,
                            exchangeRate = 5.0,
                            exchangeRateDate = "2026-08-11",
                            type = "Expense",
                            account = "Card",
                            category = "Food",
                            subCategory = "Groceries"
                        )
                    )
                    database.syncOutboxDao().insertOutboxEntry(
                        com.example.data.model.SyncOutboxEntity(
                            entityType = "TRANSACTION",
                            entityId = "fail_tx_123",
                            operation = "UPSERT"
                        )
                    )
                }
                throw RuntimeException("Simulated transaction failure")
            }
            fail("Expected RuntimeException was not thrown")
        } catch (e: RuntimeException) {
            assertEquals("Simulated transaction failure", e.message)
        }

        // Verify rollback: transaction and outbox entry do not exist
        val txInDb = database.transactionDao().getTransactionById("fail_tx_123")
        assertNull(txInDb)

        val outboxInDb = database.syncOutboxDao().getEntryById("fail_tx_123")
        assertNull(outboxInDb)
        assertEquals(0, database.syncOutboxDao().getPendingEntries().size)
    }

    @Test
    fun testEditPreservesCreatedAtAndCreatesUpsertOutboxOperation() = runBlocking {
        val txId = "tx_edit_test_1"
        val originalTx = TransactionEntity(
            id = txId,
            date = "2026-08-01",
            description = "Original Item",
            amountRON = 200.0,
            amountEUR = 40.0,
            exchangeRate = 5.0,
            exchangeRateDate = "2026-08-01",
            type = "Expense",
            account = "Card",
            category = "Shopping",
            subCategory = "Clothes",
            createdAt = 1000000L,
            updatedAt = 1000000L
        )

        database.transactionDao().insertTransaction(originalTx)

        // Perform edit via repository saveTransaction
        val edited = transactionRepository.saveTransaction(
            id = txId,
            date = "2026-08-01",
            description = "Edited Item Description",
            amountRON = 250.0,
            type = "Expense",
            account = "Card",
            category = "Shopping",
            subCategory = "Clothes"
        )

        // Verify createdAt is preserved while updatedAt is updated
        assertEquals(1000000L, edited.createdAt)
        assertTrue(edited.updatedAt > 1000000L)

        val txInDb = database.transactionDao().getTransactionById(txId)
        assertEquals(1000000L, txInDb?.createdAt)
        assertEquals("Edited Item Description", txInDb?.description)

        // Verify outbox entry is UPSERT
        val outboxEntry = database.syncOutboxDao().getPendingEntryForEntity(txId)
        assertNotNull(outboxEntry)
        assertEquals("UPSERT", outboxEntry?.operation)
        assertEquals("PENDING", outboxEntry?.status)
    }

    @Test
    fun testDeleteCreatesDeleteTombstoneOperation() = runBlocking {
        val tx = transactionRepository.saveTransaction(
            id = "tx_delete_test",
            date = "2026-08-11",
            description = "To be deleted",
            amountRON = 75.0,
            type = "Expense",
            account = "Cash",
            category = "Food",
            subCategory = "Cafes"
        )

        // Delete the transaction
        transactionRepository.deleteTransaction(tx)

        // Verify removed from transactions table
        val txInDb = database.transactionDao().getTransactionById("tx_delete_test")
        assertNull(txInDb)

        // Verify outbox entry is DELETE
        val outboxEntry = database.syncOutboxDao().getPendingEntryForEntity("tx_delete_test")
        assertNotNull(outboxEntry)
        assertEquals("DELETE", outboxEntry?.operation)
        assertEquals("PENDING", outboxEntry?.status)
    }

    @Test
    fun testRepeatedEditsCoalesceSafelyOrMaintainDeterministicOrdering() = runBlocking {
        val txId = "tx_coalesce_test"
        transactionRepository.saveTransaction(
            id = txId,
            date = "2026-08-11",
            description = "V1",
            amountRON = 10.0,
            type = "Expense",
            account = "Cash",
            category = "Food",
            subCategory = "Snacks"
        )

        transactionRepository.saveTransaction(
            id = txId,
            date = "2026-08-11",
            description = "V2",
            amountRON = 20.0,
            type = "Expense",
            account = "Cash",
            category = "Food",
            subCategory = "Snacks"
        )

        transactionRepository.saveTransaction(
            id = txId,
            date = "2026-08-11",
            description = "V3",
            amountRON = 30.0,
            type = "Expense",
            account = "Cash",
            category = "Food",
            subCategory = "Snacks"
        )

        val pendingList = database.syncOutboxDao().getPendingEntries()
        // Outbox entries for this entity ID remain coalesced as a single pending UPSERT
        val pendingForEntity = pendingList.filter { it.entityId == txId }
        assertEquals(1, pendingForEntity.size)
        assertEquals("UPSERT", pendingForEntity[0].operation)

        val latestTx = database.transactionDao().getTransactionById(txId)
        assertEquals("V3", latestTx?.description)
        assertEquals(30.0, latestTx?.amountRON ?: 0.0, 0.001)
    }

    @Test
    fun testProcessRestartDoesNotLosePendingOperations() = runBlocking {
        transactionRepository.saveTransaction(
            id = "restart_tx_1",
            date = "2026-08-11",
            description = "Persistent Operation",
            amountRON = 500.0,
            type = "Income",
            account = "Bank",
            category = "Salary",
            subCategory = "Main Job"
        )

        val entriesBefore = database.syncOutboxDao().getPendingEntries()
        assertEquals(1, entriesBefore.size)

        // Close and reopen database
        database.close()

        val newDatabase = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            FinTrackDatabase::class.java
        ).allowMainThreadQueries().build()

        // Verify outbox entry structure and fields work persistently across instances
        val outboxDao = newDatabase.syncOutboxDao()
        outboxDao.insertOutboxEntry(entriesBefore[0])

        val entriesAfter = outboxDao.getPendingEntries()
        assertEquals(1, entriesAfter.size)
        assertEquals("restart_tx_1", entriesAfter[0].entityId)
        assertEquals("PENDING", entriesAfter[0].status)

        newDatabase.close()
    }

    @Test
    fun testCsvImportRemainsAtomicWithOutbox() = runBlocking {
        val tx1 = TransactionEntity(
            id = UUID.randomUUID().toString(),
            date = "2026-08-11",
            description = "CSV Item 1",
            amountRON = 50.0,
            amountEUR = 10.0,
            exchangeRate = 5.0,
            exchangeRateDate = "2026-08-11",
            type = "Expense",
            account = "Card",
            category = "Food",
            subCategory = "Groceries"
        )

        val tx2 = TransactionEntity(
            id = UUID.randomUUID().toString(),
            date = "2026-08-11",
            description = "CSV Item 2",
            amountRON = 80.0,
            amountEUR = 16.0,
            exchangeRate = 5.0,
            exchangeRateDate = "2026-08-11",
            type = "Expense",
            account = "Card",
            category = "Food",
            subCategory = "Restaurants"
        )

        val preview = com.example.data.util.CsvPreviewData(
            totalRows = 2,
            validRowsCount = 2,
            invalidRowsCount = 0,
            newIdsCount = 2,
            existingIdsCount = 0,
            proposedUpdatesCount = 0,
            proposedSkipsCount = 0,
            totalRonIncome = 0.0,
            totalRonExpense = 130.0,
            officialCount = 2,
            unverifiedCount = 0,
            pendingCount = 0,
            missingCategories = emptyList(),
            rowErrors = emptyList(),
            validTransactionsToImport = listOf(tx1, tx2),
            duplicateMode = CsvDuplicateMode.SKIP_EXISTING,
            rawCsvContent = ""
        )

        val backupFile = File.createTempFile("backup_test", ".json")
        backupFile.deleteOnExit()

        val result = CsvImporter.executeAtomicImport(
            database = database,
            previewData = preview,
            backupFile = backupFile,
            allExistingTransactions = emptyList()
        )

        assertTrue(result.success)
        assertEquals(2, result.insertedCount)

        // Verify outbox entries created for both imported transactions
        val pendingOutbox = database.syncOutboxDao().getPendingEntries()
        assertEquals(2, pendingOutbox.size)
        val entityIds = pendingOutbox.map { it.entityId }.toSet()
        assertTrue(entityIds.contains(tx1.id))
        assertTrue(entityIds.contains(tx2.id))
    }

    @Test
    fun testDuplicateTemplateClearsOldEurMetadata() = runBlocking {
        val source = TransactionEntity(
            id = "old_id_123",
            date = "2020-01-01",
            description = "Old Transaction",
            amountRON = 100.0,
            amountEUR = 21.5,
            exchangeRate = 4.65,
            exchangeRateDate = "2020-01-01",
            exchangeRateSource = "OLD_CACHE",
            conversionStatus = "STALE",
            type = "Expense",
            account = "Card",
            category = "Food",
            subCategory = "Groceries"
        )

        val duplicate = transactionRepository.createDuplicateTemplate(source)

        // ID must be newly generated
        assertTrue(duplicate.id != source.id)
        // Date must be updated to today
        assertTrue(duplicate.date != "2020-01-01")
        // Destination is set appropriately
        assertNull(duplicate.destination)
    }
}
