package com.example

import android.content.Context
import androidx.room.Room
import androidx.room.withTransaction
import androidx.test.core.app.ApplicationProvider
import com.example.data.db.FinTrackDatabase
import com.example.data.model.ExchangeRateEntity
import com.example.data.model.TransactionEntity
import com.example.data.repository.PreparedRepairItem
import com.example.data.repository.TransactionRepository
import com.example.data.service.ExchangeRateService
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class RepairRoomTransactionTest {

    private lateinit var db: FinTrackDatabase
    private lateinit var repository: TransactionRepository

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, FinTrackDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        val exchangeRateService = ExchangeRateService(db.exchangeRateDao())
        repository = TransactionRepository(
            transactionDao = db.transactionDao(),
            exchangeRateService = exchangeRateService,
            exchangeRateDao = db.exchangeRateDao(),
            database = db
        )
    }

    @After
    fun closeDb() {
        db.close()
    }

    @Test
    fun testRealRoomRollbackWhenSecondWriteFails() = runBlocking {
        // 1. Initial legacy transaction & unverified exchange rate in DB
        val initialTx = TransactionEntity(
            id = "tx1",
            date = "2026-08-01",
            description = "Test Tx",
            amountRON = 100.0,
            amountEUR = 20.0,
            exchangeRate = 5.0,
            exchangeRateDate = "2026-08-01",
            exchangeRateSource = "UNVERIFIED",
            conversionStatus = "UNVERIFIED",
            type = "Expense",
            account = "Checking",
            category = "Food",
            subCategory = "Groceries"
        )
        val initialRate = ExchangeRateEntity(
            date = "2026-08-01",
            requestedDate = "2026-08-01",
            effectiveDate = "2026-08-01",
            rate = 5.0,
            source = "UNVERIFIED",
            status = "UNVERIFIED"
        )
        db.transactionDao().insertTransaction(initialTx)
        db.exchangeRateDao().insertRate(initialRate)

        // Verify initial state
        assertEquals("UNVERIFIED", db.transactionDao().getTransactionById("tx1")?.conversionStatus)

        // 2. Attempt batch repair where an exception is thrown inside withTransaction
        try {
            db.withTransaction {
                // First DAO write succeeds: update transaction
                val correctEUR = ExchangeRateService.calculateAmountEUR(initialTx.amountRON, 4.9765)
                db.transactionDao().insertTransaction(
                    initialTx.copy(amountEUR = correctEUR, conversionStatus = "OFFICIAL")
                )

                // Simulate error during second operation (exchange rate insert)
                throw IllegalStateException("Simulated database failure during cache write")
            }
            fail("Expected exception to be thrown")
        } catch (e: IllegalStateException) {
            // Exception caught after rollback
        }

        // 3. Verify BOTH changes were completely rolled back
        val rolledBackTx = db.transactionDao().getTransactionById("tx1")
        assertEquals("UNVERIFIED", rolledBackTx?.conversionStatus)
        assertEquals(20.0, rolledBackTx?.amountEUR ?: 0.0, 0.001)

        val rateInDb = db.exchangeRateDao().getRateForDate("2026-08-01")
        assertEquals("UNVERIFIED", rateInDb?.source)
    }

    @Test
    fun testSuccessfulRepairUpdatesTransactionAndCacheAtomically() = runBlocking {
        val initialTx = TransactionEntity(
            id = "tx2",
            date = "2026-08-02",
            description = "Test Tx 2",
            amountRON = 100.0,
            amountEUR = 20.0,
            exchangeRate = 5.0,
            exchangeRateDate = "2026-08-02",
            exchangeRateSource = "UNVERIFIED",
            conversionStatus = "UNVERIFIED",
            type = "Expense",
            account = "Checking",
            category = "Food",
            subCategory = "Groceries"
        )
        db.transactionDao().insertTransaction(initialTx)

        val preparedItems = listOf(
            PreparedRepairItem(initialTx, 4.9765, "2026-08-01")
        )

        val count = repository.applyRepairBatch(preparedItems)
        assertEquals(1, count)

        // Verify transaction updated
        val updatedTx = db.transactionDao().getTransactionById("tx2")
        assertEquals("OFFICIAL", updatedTx?.conversionStatus)
        assertEquals("BNR_OFFICIAL", updatedTx?.exchangeRateSource)
        assertEquals(4.9765, updatedTx?.exchangeRate ?: 0.0, 0.0001)

        // Verify cache updated
        val cache = db.exchangeRateDao().getOfficialRateForDate("2026-08-02")
        assertNotNull(cache)
        assertEquals("BNR_OFFICIAL", cache?.source)
        assertEquals("OFFICIAL", cache?.status)
    }

    @Test
    fun testLegacyNullQueriesInUnverifiedTransactions() = runBlocking {
        // Insert 4 transactions with different conversionStatus / source states
        val tx1Null = TransactionEntity(
            id = "tx_null",
            date = "2026-08-01",
            description = "Null status",
            amountRON = 100.0,
            amountEUR = 20.0,
            exchangeRate = 5.0,
            exchangeRateDate = "2026-08-01",
            type = "Expense",
            account = "Checking",
            category = "Food",
            subCategory = "Groceries",
            conversionStatus = "UNVERIFIED",
            exchangeRateSource = "UNVERIFIED"
        )
        val tx2Unverified = TransactionEntity(
            id = "tx_unverified",
            date = "2026-08-01",
            description = "Unverified status",
            amountRON = 100.0,
            amountEUR = 20.0,
            exchangeRate = 5.0,
            exchangeRateDate = "2026-08-01",
            type = "Expense",
            account = "Checking",
            category = "Food",
            subCategory = "Groceries",
            conversionStatus = "UNVERIFIED",
            exchangeRateSource = "SYNTHETIC"
        )
        val tx3Official = TransactionEntity(
            id = "tx_official",
            date = "2026-08-01",
            description = "Official status",
            amountRON = 100.0,
            amountEUR = 20.0,
            exchangeRate = 5.0,
            exchangeRateDate = "2026-08-01",
            type = "Expense",
            account = "Checking",
            category = "Food",
            subCategory = "Groceries",
            conversionStatus = "OFFICIAL",
            exchangeRateSource = "BNR_OFFICIAL"
        )
        val tx4Pending = TransactionEntity(
            id = "tx_pending",
            date = "2026-08-01",
            description = "Pending status",
            amountRON = 100.0,
            amountEUR = 0.0,
            exchangeRate = 0.0,
            exchangeRateDate = "2026-08-01",
            type = "Expense",
            account = "Checking",
            category = "Food",
            subCategory = "Groceries",
            conversionStatus = "PENDING",
            exchangeRateSource = "BNR_OFFICIAL"
        )

        db.transactionDao().insertAllTransactions(listOf(tx1Null, tx2Unverified, tx3Official, tx4Pending))

        val unverifiedList = repository.getUnverifiedTransactions()
        assertEquals(2, unverifiedList.size)
        assertTrue(unverifiedList.any { it.id == "tx_null" })
        assertTrue(unverifiedList.any { it.id == "tx_unverified" })

        // Ensure OFFICIAL and PENDING are excluded
        assertTrue(unverifiedList.none { it.id == "tx_official" })
        assertTrue(unverifiedList.none { it.id == "tx_pending" })
    }

    @Test
    fun testPendingSyncHandledSeparatelyFromLegacyRepair() = runBlocking {
        val txPending = TransactionEntity(
            id = "tx_pending",
            date = "2026-08-01",
            description = "Pending tx",
            amountRON = 100.0,
            amountEUR = 0.0,
            exchangeRate = 0.0,
            exchangeRateDate = "2026-08-01",
            type = "Expense",
            account = "Checking",
            category = "Food",
            subCategory = "Groceries",
            conversionStatus = "PENDING",
            exchangeRateSource = "BNR_OFFICIAL"
        )
        db.transactionDao().insertTransaction(txPending)

        // Pending should be returned by getPendingTransactions
        val pending = db.transactionDao().getPendingTransactions()
        assertEquals(1, pending.size)
        assertEquals("tx_pending", pending[0].id)

        // Pending should NOT be returned by getUnverifiedTransactions
        val unverified = db.transactionDao().getUnverifiedTransactions()
        assertTrue(unverified.none { it.id == "tx_pending" })
    }
}
