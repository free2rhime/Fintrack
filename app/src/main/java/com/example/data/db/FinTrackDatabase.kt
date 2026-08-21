package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.dao.CategoryDao
import com.example.data.dao.ExchangeRateDao
import com.example.data.dao.MigrationStateDao
import com.example.data.dao.SyncOutboxDao
import com.example.data.dao.TransactionDao
import com.example.data.model.CategoryEntity
import com.example.data.model.ExchangeRateEntity
import com.example.data.model.MigrationStateEntity
import com.example.data.model.SyncOutboxEntity
import com.example.data.model.TransactionEntity

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        TransactionEntity::class,
        CategoryEntity::class,
        ExchangeRateEntity::class,
        SyncOutboxEntity::class,
        MigrationStateEntity::class
    ],
    version = 6,
    exportSchema = true
)
abstract class FinTrackDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
    abstract fun categoryDao(): CategoryDao
    abstract fun exchangeRateDao(): ExchangeRateDao
    abstract fun syncOutboxDao(): SyncOutboxDao
    abstract fun migrationStateDao(): MigrationStateDao

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

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Safely migrate transactions table for cloud sync compatibility
                val txCursor = db.query("PRAGMA table_info(transactions)")
                val txColumns = mutableSetOf<String>()
                while (txCursor.moveToNext()) {
                    val nameIdx = txCursor.getColumnIndex("name")
                    if (nameIdx != -1) txColumns.add(txCursor.getString(nameIdx))
                }
                txCursor.close()

                if (!txColumns.contains("categoryId")) {
                    db.execSQL("ALTER TABLE transactions ADD COLUMN categoryId TEXT DEFAULT NULL")
                }
                if (!txColumns.contains("subCategoryId")) {
                    db.execSQL("ALTER TABLE transactions ADD COLUMN subCategoryId TEXT DEFAULT NULL")
                }
                if (!txColumns.contains("syncStatus")) {
                    db.execSQL("ALTER TABLE transactions ADD COLUMN syncStatus TEXT NOT NULL DEFAULT 'PENDING'")
                }
                if (!txColumns.contains("lastSyncedAt")) {
                    db.execSQL("ALTER TABLE transactions ADD COLUMN lastSyncedAt INTEGER DEFAULT NULL")
                }
                if (!txColumns.contains("isDeleted")) {
                    db.execSQL("ALTER TABLE transactions ADD COLUMN isDeleted INTEGER NOT NULL DEFAULT 0")
                }

                // Safely migrate categories table for cloud sync compatibility
                val catCursor = db.query("PRAGMA table_info(categories)")
                val catColumns = mutableSetOf<String>()
                while (catCursor.moveToNext()) {
                    val nameIdx = catCursor.getColumnIndex("name")
                    if (nameIdx != -1) catColumns.add(catCursor.getString(nameIdx))
                }
                catCursor.close()

                if (!catColumns.contains("userId")) {
                    db.execSQL("ALTER TABLE categories ADD COLUMN userId TEXT NOT NULL DEFAULT 'local_user'")
                }
                if (!catColumns.contains("createdAt")) {
                    db.execSQL("ALTER TABLE categories ADD COLUMN createdAt INTEGER NOT NULL DEFAULT 0")
                }
                if (!catColumns.contains("updatedAt")) {
                    db.execSQL("ALTER TABLE categories ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT 0")
                }
                if (!catColumns.contains("isDeleted")) {
                    db.execSQL("ALTER TABLE categories ADD COLUMN isDeleted INTEGER NOT NULL DEFAULT 0")
                }
                if (!catColumns.contains("syncStatus")) {
                    db.execSQL("ALTER TABLE categories ADD COLUMN syncStatus TEXT NOT NULL DEFAULT 'PENDING'")
                }
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `sync_outbox` (
                        `id` TEXT NOT NULL,
                        `entityType` TEXT NOT NULL,
                        `entityId` TEXT NOT NULL,
                        `operation` TEXT NOT NULL,
                        `status` TEXT NOT NULL DEFAULT 'PENDING',
                        `createdAt` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL,
                        `retryCount` INTEGER NOT NULL DEFAULT 0,
                        `lastAttemptAt` INTEGER DEFAULT NULL,
                        `errorCode` TEXT DEFAULT NULL,
                        `errorMessage` TEXT DEFAULT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `migration_state` (
                        `migrationId` TEXT NOT NULL,
                        `householdId` TEXT NOT NULL,
                        `initiatedByUid` TEXT NOT NULL,
                        `stage` TEXT NOT NULL,
                        `processedCount` INTEGER NOT NULL DEFAULT 0,
                        `totalCount` INTEGER NOT NULL DEFAULT 0,
                        `currentPhase` TEXT NOT NULL DEFAULT '',
                        `lastProcessedId` TEXT DEFAULT NULL,
                        `lastError` TEXT DEFAULT NULL,
                        `backupPath` TEXT DEFAULT NULL,
                        `createdAt` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL,
                        PRIMARY KEY(`migrationId`)
                    )
                    """.trimIndent()
                )
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Safely migrate transactions table
                val txCursor = db.query("PRAGMA table_info(transactions)")
                val txColumns = mutableSetOf<String>()
                while (txCursor.moveToNext()) {
                    val nameIdx = txCursor.getColumnIndex("name")
                    if (nameIdx != -1) txColumns.add(txCursor.getString(nameIdx))
                }
                txCursor.close()

                if (!txColumns.contains("householdId")) {
                    db.execSQL("ALTER TABLE transactions ADD COLUMN householdId TEXT DEFAULT NULL")
                }
                if (!txColumns.contains("createdByUid")) {
                    db.execSQL("ALTER TABLE transactions ADD COLUMN createdByUid TEXT DEFAULT NULL")
                }
                db.execSQL("UPDATE transactions SET createdByUid = userId WHERE createdByUid IS NULL")

                // Safely migrate categories table
                val catCursor = db.query("PRAGMA table_info(categories)")
                val catColumns = mutableSetOf<String>()
                while (catCursor.moveToNext()) {
                    val nameIdx = catCursor.getColumnIndex("name")
                    if (nameIdx != -1) catColumns.add(catCursor.getString(nameIdx))
                }
                catCursor.close()

                if (!catColumns.contains("householdId")) {
                    db.execSQL("ALTER TABLE categories ADD COLUMN householdId TEXT DEFAULT NULL")
                }
                if (!catColumns.contains("createdByUid")) {
                    db.execSQL("ALTER TABLE categories ADD COLUMN createdByUid TEXT DEFAULT NULL")
                }
                db.execSQL("UPDATE categories SET createdByUid = userId WHERE createdByUid IS NULL")
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
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
