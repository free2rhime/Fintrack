package com.example

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.data.db.FinTrackDatabase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [36])
class RoomMigrationTest {

    private val TEST_DB = "migration-test.db"

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        FinTrackDatabase::class.java,
        emptyList(),
        FrameworkSQLiteOpenHelperFactory()
    )

    @Test
    fun testMigration1To2UsingExportedSchema() {
        // 1. Create version 1 database using actual exported version-1 schema
        var db = helper.createDatabase(TEST_DB, 1)

        // 2. Insert representative version-1 Transactions, Categories, and Exchange Rates
        db.execSQL(
            """
            INSERT INTO transactions (
                id, userId, date, description, amountRON, amountEUR, exchangeRate,
                exchangeRateDate, type, account, category, subCategory, destination,
                createdAt, updatedAt
            ) VALUES (
                'tx_v1_001', 'local_user', '2026-08-01', 'Supermarket Purchase', 150.50, 30.10,
                5.0000, '2026-08-01', 'Expense', 'Card', 'Food', 'Groceries', 'Mega Image',
                1700000000000, 1700000001000
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            INSERT INTO categories (
                id, name, type, icon, isDefault, createdAt
            ) VALUES (
                'cat_v1_001', 'Food', 'Expense', 'ic_food', 1, 1690000000000
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            INSERT INTO exchange_rates (
                date, requestedDate, effectiveDate, rate, source, fetchedAt, status
            ) VALUES (
                '2026-08-01', '2026-08-01', '2026-08-01', 5.0000, 'BNR_OFFICIAL', 1700000000000, 'OFFICIAL'
            )
            """.trimIndent()
        )

        db.close()

        // 3. Run real production MIGRATION_1_2 and validate against Room version 2 schema
        val migratedDb = helper.runMigrationsAndValidate(TEST_DB, 2, true, FinTrackDatabase.MIGRATION_1_2)

        // 4. Verify transaction survives with all fields unchanged plus default new fields
        val txCursor = migratedDb.query("SELECT id, userId, date, description, amountRON, amountEUR, exchangeRate, exchangeRateDate, type, account, category, subCategory, destination, createdAt, updatedAt, exchangeRateSource, conversionStatus FROM transactions WHERE id = 'tx_v1_001'")
        assertEquals(true, txCursor.moveToFirst())
        assertEquals("tx_v1_001", txCursor.getString(0))
        assertEquals("local_user", txCursor.getString(1))
        assertEquals("2026-08-01", txCursor.getString(2))
        assertEquals("Supermarket Purchase", txCursor.getString(3))
        assertEquals(150.50, txCursor.getDouble(4), 0.001)
        assertEquals(30.10, txCursor.getDouble(5), 0.001)
        assertEquals(5.0000, txCursor.getDouble(6), 0.0001)
        assertEquals("2026-08-01", txCursor.getString(7))
        assertEquals("Expense", txCursor.getString(8))
        assertEquals("Card", txCursor.getString(9))
        assertEquals("Food", txCursor.getString(10))
        assertEquals("Groceries", txCursor.getString(11))
        assertEquals("Mega Image", txCursor.getString(12))
        assertEquals(1700000000000L, txCursor.getLong(13))
        assertEquals(1700000001000L, txCursor.getLong(14))
        assertEquals("UNVERIFIED", txCursor.getString(15))
        assertEquals("UNVERIFIED", txCursor.getString(16))
        txCursor.close()

        // 5. Verify exchange rate record survives unchanged
        val rateCursor = migratedDb.query("SELECT date, requestedDate, effectiveDate, rate, source, fetchedAt, status FROM exchange_rates WHERE date = '2026-08-01'")
        assertEquals(true, rateCursor.moveToFirst())
        assertEquals("2026-08-01", rateCursor.getString(0))
        assertEquals("2026-08-01", rateCursor.getString(1))
        assertEquals("2026-08-01", rateCursor.getString(2))
        assertEquals(5.0000, rateCursor.getDouble(3), 0.0001)
        assertEquals("BNR_OFFICIAL", rateCursor.getString(4))
        assertEquals(1700000000000L, rateCursor.getLong(5))
        assertEquals("OFFICIAL", rateCursor.getString(6))
        rateCursor.close()

        // 6. Verify version-1 Category record compatibility & transition to version-2
        val catCursor = migratedDb.query("SELECT id, name, type, subCategory FROM categories WHERE id = 'cat_v1_001'")
        assertEquals(true, catCursor.moveToFirst())
        assertEquals("cat_v1_001", catCursor.getString(0))
        assertEquals("Food", catCursor.getString(1))
        assertEquals("Expense", catCursor.getString(2))
        assertEquals("", catCursor.getString(3))
        catCursor.close()

        migratedDb.close()
    }

    @Test
    fun testMigration2To3UsingExportedSchema() {
        // 1. Create version 2 database using exported schema
        var db = helper.createDatabase(TEST_DB, 2)

        // 2. Insert representative version 2 transaction and category
        db.execSQL(
            """
            INSERT INTO transactions (
                id, userId, date, description, amountRON, amountEUR, exchangeRate,
                exchangeRateDate, type, account, category, subCategory, destination,
                createdAt, updatedAt, exchangeRateSource, conversionStatus
            ) VALUES (
                'tx_v2_001', 'local_user', '2026-08-10', 'Groceries Purchase', 250.00, 50.00,
                5.0000, '2026-08-10', 'Expense', 'Card', '🍉 Food & Dining', '🛒 Groceries', 'Supermarket',
                1710000000000, 1710000001000, 'BNR_OFFICIAL', 'OFFICIAL'
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            INSERT INTO categories (
                id, name, type, subCategory
            ) VALUES (
                'cat_v2_001', '🍉 Food & Dining', 'Expense', '🛒 Groceries'
            )
            """.trimIndent()
        )

        db.close()

        // 3. Run production MIGRATION_2_3 and validate against Room version 3 schema
        val migratedDb = helper.runMigrationsAndValidate(TEST_DB, 3, true, FinTrackDatabase.MIGRATION_2_3)

        // 4. Verify transaction survives with stable ID and category display strings unchanged
        val txCursor = migratedDb.query("SELECT id, category, subCategory, categoryId, subCategoryId, syncStatus, lastSyncedAt, isDeleted FROM transactions WHERE id = 'tx_v2_001'")
        assertEquals(true, txCursor.moveToFirst())
        assertEquals("tx_v2_001", txCursor.getString(0))
        assertEquals("🍉 Food & Dining", txCursor.getString(1))
        assertEquals("🛒 Groceries", txCursor.getString(2))
        assertNull(txCursor.getString(3)) // categoryId defaults to NULL
        assertNull(txCursor.getString(4)) // subCategoryId defaults to NULL
        assertEquals("PENDING", txCursor.getString(5)) // syncStatus
        assertNull(txCursor.getString(6)) // lastSyncedAt defaults to NULL
        assertEquals(0, txCursor.getInt(7)) // isDeleted defaults to 0
        txCursor.close()

        // 5. Verify category survives with stable ID and default sync fields
        val catCursor = migratedDb.query("SELECT id, name, type, subCategory, userId, isDeleted, syncStatus FROM categories WHERE id = 'cat_v2_001'")
        assertEquals(true, catCursor.moveToFirst())
        assertEquals("cat_v2_001", catCursor.getString(0))
        assertEquals("🍉 Food & Dining", catCursor.getString(1))
        assertEquals("Expense", catCursor.getString(2))
        assertEquals("🛒 Groceries", catCursor.getString(3))
        assertEquals("local_user", catCursor.getString(4))
        assertEquals(0, catCursor.getInt(5))
        assertEquals("PENDING", catCursor.getString(6))
        catCursor.close()

        migratedDb.close()
    }

    @Test
    fun testCategoryDeletionDecouplingAndCsvCompat() {
        val tx = com.example.data.model.TransactionEntity(
            id = "tx_stable_999",
            date = "2026-08-11",
            description = "Lunch Special",
            amountRON = 45.0,
            amountEUR = 9.0,
            exchangeRate = 5.0,
            exchangeRateDate = "2026-08-11",
            type = "Expense",
            account = "Card",
            category = "🍉 Food & Dining",
            subCategory = "🍔 Fast Food"
        )

        val cat = com.example.data.model.CategoryEntity(
            id = "cat_stable_999",
            name = "🍉 Food & Dining",
            type = "Expense",
            subCategory = "🍔 Fast Food"
        )

        // 1. Verify CSV Export Header and Row format retain all required fields
        val csvOutput = com.example.data.util.CsvExporter.generateCsvContent(listOf(tx))
        val expectedHeader = "Transaction_ID,Transaction_Date,Amount_RON,Amount_EUR,Exchange_Rate,Requested_Rate_Date,Effective_BNR_Rate_Date,Exchange_Rate_Source,Conversion_Status,Description,Type,Account,Category,SubCategory,Destination"
        assertEquals(true, csvOutput.startsWith(expectedHeader))
        assertEquals(true, csvOutput.contains("tx_stable_999"))
        assertEquals(true, csvOutput.contains("🍉 Food & Dining"))

        // 2. Verify CSV Parsing accepts standard exported string
        val preview = com.example.data.util.CsvImporter.parseAndValidate(
            csvContent = csvOutput,
            existingTransactions = emptyList(),
            existingCategories = listOf(cat)
        )
        assertEquals(1, preview.validTransactionsToImport.size)
        val importedTx = preview.validTransactionsToImport[0]
        assertEquals("tx_stable_999", importedTx.id)
        assertEquals("🍉 Food & Dining", importedTx.category)
        assertEquals("🍔 Fast Food", importedTx.subCategory)
    }
}
