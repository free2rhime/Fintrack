package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.dao.CategoryDao
import com.example.data.dao.ExchangeRateDao
import com.example.data.dao.TransactionDao
import com.example.data.model.CategoryEntity
import com.example.data.model.ExchangeRateEntity
import com.example.data.model.TransactionEntity

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [TransactionEntity::class, CategoryEntity::class, ExchangeRateEntity::class],
    version = 2,
    exportSchema = true
)
abstract class FinTrackDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
    abstract fun categoryDao(): CategoryDao
    abstract fun exchangeRateDao(): ExchangeRateDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Safely migrate transactions table
                val txCursor = db.query("PRAGMA table_info(transactions)")
                val txColumns = mutableSetOf<String>()
                while (txCursor.moveToNext()) {
                    val nameIdx = txCursor.getColumnIndex("name")
                    if (nameIdx != -1) txColumns.add(txCursor.getString(nameIdx))
                }
                txCursor.close()

                if (!txColumns.contains("exchangeRateSource")) {
                    db.execSQL("ALTER TABLE transactions ADD COLUMN exchangeRateSource TEXT NOT NULL DEFAULT 'UNVERIFIED'")
                }
                if (!txColumns.contains("conversionStatus")) {
                    db.execSQL("ALTER TABLE transactions ADD COLUMN conversionStatus TEXT NOT NULL DEFAULT 'UNVERIFIED'")
                }

                // Safely migrate categories table
                val catCursor = db.query("PRAGMA table_info(categories)")
                val catColumns = mutableSetOf<String>()
                while (catCursor.moveToNext()) {
                    val nameIdx = catCursor.getColumnIndex("name")
                    if (nameIdx != -1) catColumns.add(catCursor.getString(nameIdx))
                }
                catCursor.close()

                if (!catColumns.contains("subCategory")) {
                    db.execSQL("CREATE TABLE IF NOT EXISTS `categories_new` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, `type` TEXT NOT NULL, `subCategory` TEXT NOT NULL, PRIMARY KEY(`id`))")
                    db.execSQL("INSERT INTO `categories_new` (`id`, `name`, `type`, `subCategory`) SELECT `id`, `name`, `type`, '' FROM `categories`")
                    db.execSQL("DROP TABLE `categories` ")
                    db.execSQL("ALTER TABLE `categories_new` RENAME TO `categories` ")
                }

                // Safely migrate exchange_rates table
                val rateCursor = db.query("PRAGMA table_info(exchange_rates)")
                val rateColumns = mutableSetOf<String>()
                while (rateCursor.moveToNext()) {
                    val nameIdx = rateCursor.getColumnIndex("name")
                    if (nameIdx != -1) rateColumns.add(rateCursor.getString(nameIdx))
                }
                rateCursor.close()

                if (!rateColumns.contains("requestedDate")) {
                    db.execSQL("ALTER TABLE exchange_rates ADD COLUMN requestedDate TEXT NOT NULL DEFAULT ''")
                    db.execSQL("UPDATE exchange_rates SET requestedDate = date WHERE requestedDate = ''")
                }
                if (!rateColumns.contains("effectiveDate")) {
                    db.execSQL("ALTER TABLE exchange_rates ADD COLUMN effectiveDate TEXT NOT NULL DEFAULT ''")
                    db.execSQL("UPDATE exchange_rates SET effectiveDate = date WHERE effectiveDate = ''")
                }
                if (!rateColumns.contains("source")) {
                    db.execSQL("ALTER TABLE exchange_rates ADD COLUMN source TEXT NOT NULL DEFAULT 'UNVERIFIED'")
                }
                if (!rateColumns.contains("fetchedAt")) {
                    db.execSQL("ALTER TABLE exchange_rates ADD COLUMN fetchedAt INTEGER NOT NULL DEFAULT 0")
                }
                if (!rateColumns.contains("status")) {
                    db.execSQL("ALTER TABLE exchange_rates ADD COLUMN status TEXT NOT NULL DEFAULT 'UNVERIFIED'")
                }
            }
        }

        @Volatile
        private var INSTANCE: FinTrackDatabase? = null

        fun getDatabase(context: Context): FinTrackDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    FinTrackDatabase::class.java,
                    "fintrack_database"
                )
                .addMigrations(MIGRATION_1_2)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
