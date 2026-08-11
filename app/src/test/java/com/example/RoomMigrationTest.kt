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
}
