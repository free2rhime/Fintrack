package com.example

import android.database.sqlite.SQLiteDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.db.FinTrackDatabase
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.lang.reflect.Proxy

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class RoomMigrationTest {

    @Test
    fun testMigration1To2PreservesTransactions() {
        val db = SQLiteDatabase.create(null)
        db.execSQL(
            "CREATE TABLE `transactions` (" +
            "`id` TEXT NOT NULL, `date` TEXT NOT NULL, `description` TEXT NOT NULL, " +
            "`amountRON` REAL NOT NULL, `amountEUR` REAL NOT NULL, `exchangeRate` REAL NOT NULL, " +
            "`exchangeRateDate` TEXT NOT NULL, `type` TEXT NOT NULL, `account` TEXT NOT NULL, " +
            "`category` TEXT NOT NULL, `subCategory` TEXT NOT NULL, `destination` TEXT, " +
            "`createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, PRIMARY KEY(`id`))"
        )
        db.execSQL(
            "CREATE TABLE `exchange_rates` (" +
            "`date` TEXT NOT NULL, `rate` REAL NOT NULL, PRIMARY KEY(`date`))"
        )
        db.execSQL(
            "INSERT INTO transactions (id, date, description, amountRON, amountEUR, exchangeRate, exchangeRateDate, type, account, category, subCategory, createdAt, updatedAt) " +
            "VALUES ('v1_tx', '2026-08-01', 'V1 Item', 100.0, 20.0, 5.0, '2026-08-01', 'Expense', 'Checking', 'Food', 'Groceries', 1000, 1000)"
        )

        val supportDb = Proxy.newProxyInstance(
            SupportSQLiteDatabase::class.java.classLoader,
            arrayOf(SupportSQLiteDatabase::class.java)
        ) { _, method, args ->
            if (method.name == "execSQL" && args != null && args.isNotEmpty()) {
                db.execSQL(args[0] as String)
                null
            } else {
                throw UnsupportedOperationException("Method ${method.name} not stubbed")
            }
        } as SupportSQLiteDatabase

        FinTrackDatabase.MIGRATION_1_2.migrate(supportDb)

        val cursor = db.rawQuery("SELECT id, description, conversionStatus, exchangeRateSource FROM transactions WHERE id = 'v1_tx'", null)
        assertEquals(true, cursor.moveToFirst())
        assertEquals("v1_tx", cursor.getString(0))
        assertEquals("V1 Item", cursor.getString(1))
        assertEquals("UNVERIFIED", cursor.getString(2))
        assertEquals("UNVERIFIED", cursor.getString(3))
        cursor.close()
        db.close()
    }
}
