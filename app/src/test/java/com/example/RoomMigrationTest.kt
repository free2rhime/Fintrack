package com.example

import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.data.db.FinTrackDatabase
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [36])
class RoomMigrationTest {

    @Test
    fun testMigration1To2PreservesTransactionsAndAddsColumnsWithDefaultValues() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val dbName = "migration-test.db"
        context.deleteDatabase(dbName)

        val config = SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(dbName)
            .callback(object : SupportSQLiteOpenHelper.Callback(1) {
                override fun onCreate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                    db.execSQL(
                        "CREATE TABLE IF NOT EXISTS `transactions` (" +
                        "`id` TEXT NOT NULL, `userId` TEXT NOT NULL, `date` TEXT NOT NULL, " +
                        "`description` TEXT NOT NULL, `amountRON` REAL NOT NULL, `amountEUR` REAL NOT NULL, " +
                        "`exchangeRate` REAL NOT NULL, `exchangeRateDate` TEXT NOT NULL, `type` TEXT NOT NULL, " +
                        "`account` TEXT NOT NULL, `category` TEXT NOT NULL, `subCategory` TEXT NOT NULL, " +
                        "`destination` TEXT, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, " +
                        "PRIMARY KEY(`id`))"
                    )
                    db.execSQL("CREATE INDEX IF NOT EXISTS `index_transactions_date` ON `transactions` (`date`)")
                    db.execSQL("CREATE INDEX IF NOT EXISTS `index_transactions_category` ON `transactions` (`category`)")
                    db.execSQL("CREATE INDEX IF NOT EXISTS `index_transactions_type` ON `transactions` (`type`)")
                    db.execSQL("CREATE INDEX IF NOT EXISTS `index_transactions_description` ON `transactions` (`description`)")

                    db.execSQL(
                        "CREATE TABLE IF NOT EXISTS `exchange_rates` (" +
                        "`date` TEXT NOT NULL, `rate` REAL NOT NULL, PRIMARY KEY(`date`))"
                    )
                }

                override fun onUpgrade(db: androidx.sqlite.db.SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {}
            })
            .build()

        val openHelper = FrameworkSQLiteOpenHelperFactory().create(config)
        val v1Db = openHelper.writableDatabase

        v1Db.execSQL(
            "INSERT INTO transactions (id, userId, date, description, amountRON, amountEUR, exchangeRate, exchangeRateDate, type, account, category, subCategory, createdAt, updatedAt) " +
            "VALUES ('v1_tx', 'local_user', '2026-08-01', 'V1 Item', 100.0, 20.0, 5.0, '2026-08-01', 'Expense', 'Checking', 'Food', 'Groceries', 1000, 1000)"
        )
        v1Db.execSQL("INSERT INTO exchange_rates (date, rate) VALUES ('2026-08-01', 5.0)")

        // Execute Migration 1 -> 2
        FinTrackDatabase.MIGRATION_1_2.migrate(v1Db)

        // Validate transactions migration results
        val cursor = v1Db.query("SELECT id, description, conversionStatus, exchangeRateSource FROM transactions WHERE id = 'v1_tx'")
        assertEquals(true, cursor.moveToFirst())
        assertEquals("v1_tx", cursor.getString(0))
        assertEquals("V1 Item", cursor.getString(1))
        assertEquals("UNVERIFIED", cursor.getString(2))
        assertEquals("UNVERIFIED", cursor.getString(3))
        cursor.close()

        // Validate exchange_rates migration results
        val rateCursor = v1Db.query("SELECT date, rate, requestedDate, effectiveDate, source, status FROM exchange_rates WHERE date = '2026-08-01'")
        assertEquals(true, rateCursor.moveToFirst())
        assertEquals("2026-08-01", rateCursor.getString(0))
        assertEquals(5.0, rateCursor.getDouble(1), 0.001)
        assertEquals("2026-08-01", rateCursor.getString(2))
        assertEquals("2026-08-01", rateCursor.getString(3))
        assertEquals("UNVERIFIED", rateCursor.getString(4))
        assertEquals("UNVERIFIED", rateCursor.getString(5))
        rateCursor.close()

        v1Db.close()
    }
}
