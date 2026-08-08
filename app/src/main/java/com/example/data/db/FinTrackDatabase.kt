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
                // Safely migrate exchange_rates table
                db.execSQL("ALTER TABLE exchange_rates ADD COLUMN requestedDate TEXT NOT NULL DEFAULT ''")
                db.execSQL("UPDATE exchange_rates SET requestedDate = date WHERE requestedDate = ''")
                db.execSQL("ALTER TABLE exchange_rates ADD COLUMN effectiveDate TEXT NOT NULL DEFAULT ''")
                db.execSQL("UPDATE exchange_rates SET effectiveDate = date WHERE effectiveDate = ''")
                db.execSQL("ALTER TABLE exchange_rates ADD COLUMN source TEXT NOT NULL DEFAULT 'UNVERIFIED'")
                db.execSQL("ALTER TABLE exchange_rates ADD COLUMN fetchedAt INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE exchange_rates ADD COLUMN status TEXT NOT NULL DEFAULT 'UNVERIFIED'")

                // Safely migrate transactions table
                db.execSQL("ALTER TABLE transactions ADD COLUMN exchangeRateSource TEXT NOT NULL DEFAULT 'UNVERIFIED'")
                db.execSQL("ALTER TABLE transactions ADD COLUMN conversionStatus TEXT NOT NULL DEFAULT 'UNVERIFIED'")
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
