package com.example

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.data.db.FinTrackDatabase
import com.example.data.model.CategoryEntity
import com.example.data.model.ExchangeRateEntity
import com.example.data.model.TransactionEntity
import com.example.data.repository.RoomCategoryRepository
import com.example.data.repository.RoomTransactionRepository
import com.example.data.service.ExchangeRateService
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [36])
class RepositoryOutboxWiringTest {

    private lateinit var database: FinTrackDatabase
    private lateinit var transactionRepository: RoomTransactionRepository
    private lateinit var categoryRepository: RoomCategoryRepository
    private lateinit var exchangeRateService: ExchangeRateService
    private var transactionNotified = false
    private var categoryNotified = false
    private var serviceNotified = false

    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            FinTrackDatabase::class.java
        ).allowMainThreadQueries().build()

        serviceNotified = false
        transactionNotified = false
        categoryNotified = false

        exchangeRateService = ExchangeRateService(
            exchangeRateDao = database.exchangeRateDao(),
            syncOutboxDao = database.syncOutboxDao(),
            database = database,
            onOutboxMutated = { serviceNotified = true }
        )

        transactionRepository = RoomTransactionRepository(
            transactionDao = database.transactionDao(),
            exchangeRateService = exchangeRateService,
            exchangeRateDao = database.exchangeRateDao(),
            database = database,
            syncOutboxDao = database.syncOutboxDao(),
            onOutboxMutated = { transactionNotified = true }
        )

        categoryRepository = RoomCategoryRepository(
            categoryDao = database.categoryDao(),
            syncOutboxDao = database.syncOutboxDao(),
            database = database,
            onOutboxMutated = { categoryNotified = true }
        )
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun testTransactionUpsertEnqueuesOutboxAndNotifies() = runBlocking {
        // Pre-insert rate
        database.exchangeRateDao().insertRate(
            ExchangeRateEntity("2026-08-21", "2026-08-21", "2026-08-21", 5.0, "BNR_OFFICIAL", System.currentTimeMillis(), "OFFICIAL")
        )

        val tx = transactionRepository.saveTransaction(
            id = "tx_123",
            date = "2026-08-21",
            description = "Groceries",
            amountRON = 100.0,
            type = "Expense",
            account = "Card",
            category = "Food",
            subCategory = "Market",
            destination = null,
            userId = "user_1"
        )

        val outboxEntry = database.syncOutboxDao().getPendingEntryForEntity("tx_123")
        assertNotNull(outboxEntry)
        assertEquals("TRANSACTION", outboxEntry?.entityType)
        assertEquals("UPSERT", outboxEntry?.operation)
        assertEquals("PENDING", outboxEntry?.status)
        assertTrue(transactionNotified)
    }

    @Test
    fun testTransactionDeleteEnqueuesOutboxAndNotifies() = runBlocking {
        val tx = TransactionEntity(
            id = "tx_del_999",
            date = "2026-08-21",
            description = "To Delete",
            amountRON = 50.0,
            amountEUR = 10.0,
            exchangeRate = 5.0,
            exchangeRateDate = "2026-08-21",
            type = "Expense",
            account = "Card",
            category = "Food",
            subCategory = "Groceries"
        )
        database.transactionDao().insertTransaction(tx)

        transactionNotified = false
        transactionRepository.deleteTransactionById("tx_del_999")

        val outboxEntry = database.syncOutboxDao().getPendingEntryForEntity("tx_del_999")
        assertNotNull(outboxEntry)
        assertEquals("TRANSACTION", outboxEntry?.entityType)
        assertEquals("DELETE", outboxEntry?.operation)
        assertTrue(transactionNotified)
    }

    @Test
    fun testCategoryUpsertEnqueuesOutboxAndNotifies() = runBlocking {
        categoryRepository.addCategory("Utilities", "Expense", "Electricity", "user_1")

        val outbox = database.syncOutboxDao().getPendingEntries()
        assertTrue(outbox.isNotEmpty())
        val catEntry = outbox.first { it.entityType == "CATEGORY" }
        assertEquals("UPSERT", catEntry.operation)
        assertEquals("PENDING", catEntry.status)
        assertTrue(categoryNotified)
    }

    @Test
    fun testCategoryDeleteEnqueuesOutboxAndNotifies() = runBlocking {
        val cat = CategoryEntity(id = "cat_del_01", name = "Test", type = "Expense", subCategory = "Sub")
        database.categoryDao().insertCategory(cat)

        categoryNotified = false
        categoryRepository.deleteCategory(cat)

        val outboxEntry = database.syncOutboxDao().getPendingEntryForEntity("cat_del_01")
        assertNotNull(outboxEntry)
        assertEquals("CATEGORY", outboxEntry?.entityType)
        assertEquals("DELETE", outboxEntry?.operation)
        assertTrue(categoryNotified)
    }

    @Test
    fun testDuplicateSuppressionCoalescesPendingOutboxEntry() = runBlocking {
        val tx = TransactionEntity(
            id = "tx_dup_1",
            date = "2026-08-21",
            description = "Initial",
            amountRON = 50.0,
            amountEUR = 10.0,
            exchangeRate = 5.0,
            exchangeRateDate = "2026-08-21",
            type = "Expense",
            account = "Card",
            category = "Food",
            subCategory = "Groceries"
        )
        transactionRepository.insertTransaction(tx)
        val initialCount = database.syncOutboxDao().getPendingEntries().size
        assertEquals(1, initialCount)

        // Mutate again with same operation
        val updatedTx = tx.copy(description = "Updated")
        transactionRepository.insertTransaction(updatedTx)

        val finalCount = database.syncOutboxDao().getPendingEntries().size
        assertEquals(1, finalCount)
        val entry = database.syncOutboxDao().getPendingEntryForEntity("tx_dup_1")
        assertNotNull(entry)
        assertEquals("UPSERT", entry?.operation)
    }

    @Test
    fun syncPendingConversions_withSuccessfulResolution_enqueuesOutboxAndNotifies() = runBlocking {
        // Pre-insert official rate for date
        database.exchangeRateDao().insertRate(
            ExchangeRateEntity("2026-08-21", "2026-08-21", "2026-08-21", 5.0, "BNR_OFFICIAL", System.currentTimeMillis(), "OFFICIAL")
        )

        // Directly seed pending transaction into DAO
        val pendingTx = TransactionEntity(
            id = "tx_pending_1",
            date = "2026-08-21",
            description = "Pending Item",
            amountRON = 100.0,
            amountEUR = 0.0,
            exchangeRate = 0.0,
            exchangeRateDate = "",
            exchangeRateSource = "",
            conversionStatus = "PENDING_NO_OFFICIAL_RATE",
            type = "Expense",
            account = "Card",
            category = "Food",
            subCategory = "Groceries",
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        database.transactionDao().insertTransaction(pendingTx)

        // Clear outbox & notifications
        database.syncOutboxDao().deleteAllOutboxEntries()
        transactionNotified = false

        val result = transactionRepository.syncPendingConversions()
        assertEquals(1, result.convertedSuccessfully)
        assertEquals(0, result.stillPending)

        val updated = database.transactionDao().getTransactionById("tx_pending_1")
        assertNotNull(updated)
        assertEquals("OFFICIAL", updated?.conversionStatus)
        assertEquals(20.0, updated?.amountEUR ?: 0.0, 0.001)

        val txEntry = database.syncOutboxDao().getPendingEntryForEntity("tx_pending_1")
        assertNotNull(txEntry)
        assertEquals("TRANSACTION", txEntry?.entityType)
        assertEquals("UPSERT", txEntry?.operation)
        assertTrue(transactionNotified)
    }

    @Test
    fun syncPendingConversions_withUnresolvedStatus_doesNotEnqueueOutbox() = runBlocking {
        // Seed pending transaction with a future/unresolvable date (no exchange rate pre-seeded)
        val pendingTx = TransactionEntity(
            id = "tx_pending_unresolved",
            date = "2099-01-01",
            description = "Future Item",
            amountRON = 100.0,
            amountEUR = 0.0,
            exchangeRate = 0.0,
            exchangeRateDate = "",
            exchangeRateSource = "",
            conversionStatus = "PENDING_NO_OFFICIAL_RATE",
            type = "Expense",
            account = "Card",
            category = "Food",
            subCategory = "Groceries",
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        database.transactionDao().insertTransaction(pendingTx)

        // Clear outbox & notifications
        database.syncOutboxDao().deleteAllOutboxEntries()
        transactionNotified = false

        val result = transactionRepository.syncPendingConversions()
        assertEquals(0, result.convertedSuccessfully)
        assertEquals(1, result.stillPending)

        // Assert transaction was updated locally for diagnostics or remained pending
        val updated = database.transactionDao().getTransactionById("tx_pending_unresolved")
        assertNotNull(updated)

        // Assert sync_outbox contains NO transaction upsert and callback was not triggered
        val txEntry = database.syncOutboxDao().getPendingEntryForEntity("tx_pending_unresolved")
        org.junit.Assert.assertNull(txEntry)
        org.junit.Assert.assertFalse(transactionNotified)
    }
}
