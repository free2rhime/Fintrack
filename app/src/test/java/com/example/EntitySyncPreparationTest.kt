package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.data.db.FinTrackDatabase
import com.example.data.model.CategoryEntity
import com.example.data.model.TransactionEntity
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [36])
class EntitySyncPreparationTest {

    private lateinit var db: FinTrackDatabase

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, FinTrackDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun teardown() {
        db.close()
    }

    @Test
    fun testManagedCategoryDeletionDoesNotAlterHistoricalTransaction() = runBlocking {
        // 1. Insert category
        val cat = CategoryEntity(
            id = "cat_uuid_101",
            name = "🍉 Food & Dining",
            type = "Expense",
            subCategory = "🛒 Groceries"
        )
        db.categoryDao().insertCategory(cat)

        // 2. Insert transaction linked by string values and optional categoryId
        val tx = TransactionEntity(
            id = "tx_uuid_505",
            date = "2026-08-11",
            description = "Supermarket Run",
            amountRON = 120.0,
            amountEUR = 24.0,
            exchangeRate = 5.0,
            exchangeRateDate = "2026-08-11",
            type = "Expense",
            account = "Card",
            category = "🍉 Food & Dining",
            subCategory = "🛒 Groceries",
            categoryId = cat.id
        )
        db.transactionDao().insertTransaction(tx)

        // Verify inserted transaction
        val savedTxBefore = db.transactionDao().getTransactionById("tx_uuid_505")
        assertNotNull(savedTxBefore)
        assertEquals("tx_uuid_505", savedTxBefore!!.id)
        assertEquals("🍉 Food & Dining", savedTxBefore.category)
        assertEquals("🛒 Groceries", savedTxBefore.subCategory)

        // 3. Delete or rename category group in managed category table
        db.categoryDao().deleteCategoryGroup("🍉 Food & Dining", "Expense")

        // 4. Verify transaction remains intact and display data is unchanged
        val savedTxAfter = db.transactionDao().getTransactionById("tx_uuid_505")
        assertNotNull(savedTxAfter)
        assertEquals("tx_uuid_505", savedTxAfter!!.id)
        assertEquals("🍉 Food & Dining", savedTxAfter.category)
        assertEquals("🛒 Groceries", savedTxAfter.subCategory)
    }

    @Test
    fun testStableIdentityAndDefaults() = runBlocking {
        val tx = TransactionEntity(
            id = "stable_doc_id_999",
            date = "2026-08-11",
            description = "Test Transaction",
            amountRON = 100.0,
            amountEUR = 20.0,
            exchangeRate = 5.0,
            exchangeRateDate = "2026-08-11",
            type = "Expense",
            account = "Card",
            category = "Utilities",
            subCategory = "Electricity"
        )
        db.transactionDao().insertTransaction(tx)

        val retrieved = db.transactionDao().getTransactionById("stable_doc_id_999")
        assertNotNull(retrieved)
        assertEquals("stable_doc_id_999", retrieved!!.id)
        assertEquals("PENDING", retrieved.syncStatus)
        assertEquals(false, retrieved.isDeleted)
        assertEquals(null, retrieved.categoryId)
        assertEquals(null, retrieved.subCategoryId)
        assertEquals(null, retrieved.lastSyncedAt)
    }
}
