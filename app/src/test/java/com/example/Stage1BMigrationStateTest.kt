package com.example

import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.data.db.FinTrackDatabase
import com.example.data.model.ExchangeRateDto
import com.example.data.model.ExchangeRateEntity
import com.example.data.model.FirestoreDtoValidator
import com.example.data.model.MigrationStateEntity
import com.example.data.model.SyncOutboxEntity
import com.example.data.model.toEntity
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [36])
class Stage1BMigrationStateTest {

    private val TEST_DB = "stage1b-migration-test.db"

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        FinTrackDatabase::class.java,
        emptyList(),
        FrameworkSQLiteOpenHelperFactory()
    )

    @Test
    fun testMigration4To5CreatesMigrationStateTableAndPreservesV4Data() {
        // 1. Create version 4 database using exported v4 schema
        var db = helper.createDatabase(TEST_DB, 4)

        // Insert representative v4 transaction, category, exchange rate, and outbox entry
        db.execSQL(
            """
            INSERT INTO transactions (
                id, userId, date, description, amountRON, amountEUR, exchangeRate,
                exchangeRateDate, type, account, category, subCategory, destination,
                createdAt, updatedAt, exchangeRateSource, conversionStatus, syncStatus, isDeleted
            ) VALUES (
                'tx_v4_001', 'user_123', '2026-08-12', 'Coffee', 15.00, 3.00,
                5.0000, '2026-08-12', 'Expense', 'Cash', 'Food', '', NULL,
                1730000000000, 1730000000000, 'BNR_OFFICIAL', 'OFFICIAL', 'PENDING', 0
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            INSERT INTO categories (
                id, name, type, subCategory, userId, createdAt, updatedAt, isDeleted, syncStatus
            ) VALUES (
                'cat_v4_001', 'Food', 'Expense', '', 'user_123', 1730000000000, 1730000000000, 0, 'PENDING'
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            INSERT INTO exchange_rates (
                date, requestedDate, effectiveDate, rate, source, fetchedAt, status
            ) VALUES (
                '2026-08-12', '2026-08-12', '2026-08-12', 5.0000, 'BNR_OFFICIAL', 1730000000000, 'OFFICIAL'
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            INSERT INTO sync_outbox (
                id, entityType, entityId, operation, status, createdAt, updatedAt, retryCount
            ) VALUES (
                'outbox_v4_001', 'TRANSACTION', 'tx_v4_001', 'CREATE', 'PENDING', 1730000000000, 1730000000000, 0
            )
            """.trimIndent()
        )

        db.close()

        // 2. Run MIGRATION_4_5 and validate against Room version 5 schema
        val migratedDb = helper.runMigrationsAndValidate(TEST_DB, 5, true, FinTrackDatabase.MIGRATION_4_5)

        // 3. Verify existing transactions, categories, exchange_rates, sync_outbox remain intact
        val txCursor = migratedDb.query("SELECT id, description FROM transactions WHERE id = 'tx_v4_001'")
        assertTrue(txCursor.moveToFirst())
        assertEquals("Coffee", txCursor.getString(1))
        txCursor.close()

        val catCursor = migratedDb.query("SELECT id, name FROM categories WHERE id = 'cat_v4_001'")
        assertTrue(catCursor.moveToFirst())
        assertEquals("Food", catCursor.getString(1))
        catCursor.close()

        val rateCursor = migratedDb.query("SELECT date, rate FROM exchange_rates WHERE date = '2026-08-12'")
        assertTrue(rateCursor.moveToFirst())
        assertEquals(5.0000, rateCursor.getDouble(1), 0.0001)
        rateCursor.close()

        val outboxCursor = migratedDb.query("SELECT id, status FROM sync_outbox WHERE id = 'outbox_v4_001'")
        assertTrue(outboxCursor.moveToFirst())
        assertEquals("PENDING", outboxCursor.getString(1))
        outboxCursor.close()

        // 4. Verify migration_state table exists and is empty initially
        val stateCursor = migratedDb.query("SELECT COUNT(*) FROM migration_state")
        assertTrue(stateCursor.moveToFirst())
        assertEquals(0, stateCursor.getInt(0))
        stateCursor.close()

        migratedDb.close()
    }

    @Test
    fun testMigrationStateDaoOperations() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val db = Room.inMemoryDatabaseBuilder(context, FinTrackDatabase::class.java).build()
        val dao = db.migrationStateDao()

        val initialState = MigrationStateEntity(
            migrationId = "mig_sess_1001",
            householdId = "hh_777",
            initiatedByUid = "user_owner_1",
            stage = "PREFLIGHT",
            processedCount = 0,
            totalCount = 150,
            currentPhase = "PREFLIGHT_CHECK",
            lastProcessedId = null,
            lastError = null,
            backupPath = "/backups/fintrack_hh_777.csv",
            createdAt = 1730000000000L,
            updatedAt = 1730000000000L
        )

        dao.insertMigrationState(initialState)

        val retrieved = dao.getMigrationStateById("mig_sess_1001")
        assertNotNull(retrieved)
        assertEquals("mig_sess_1001", retrieved?.migrationId)
        assertEquals("hh_777", retrieved?.householdId)
        assertEquals("user_owner_1", retrieved?.initiatedByUid)
        assertEquals("PREFLIGHT", retrieved?.stage)
        assertEquals(0, retrieved?.processedCount)
        assertEquals(150, retrieved?.totalCount)
        assertNull(retrieved?.lastProcessedId)

        // Advance progress
        val updatedState = retrieved!!.copy(
            stage = "TRANSACTIONS_UPLOADING",
            processedCount = 45,
            currentPhase = "TRANSACTIONS",
            lastProcessedId = "tx_45",
            updatedAt = 1730000010000L
        )
        dao.updateMigrationState(updatedState)

        val afterUpdate = dao.getMigrationStateById("mig_sess_1001")
        assertNotNull(afterUpdate)
        assertEquals("mig_sess_1001", afterUpdate?.migrationId) // Identity preserved
        assertEquals("TRANSACTIONS_UPLOADING", afterUpdate?.stage)
        assertEquals(45, afterUpdate?.processedCount)
        assertEquals("tx_45", afterUpdate?.lastProcessedId)

        // Sanitized error handling
        val errorState = afterUpdate!!.copy(
            stage = "FAILED",
            lastError = "Network error during transactions upload. Error code: PERMISSION_DENIED",
            updatedAt = 1730000020000L
        )
        dao.updateMigrationState(errorState)

        val afterError = dao.getMigrationStateById("mig_sess_1001")
        assertNotNull(afterError)
        assertEquals("FAILED", afterError?.stage)
        assertEquals("Network error during transactions upload. Error code: PERMISSION_DENIED", afterError?.lastError)
        assertTrue(!afterError?.lastError!!.contains("at com.example"))

        db.close()
    }

    @Test
    fun testExchangeRateDtoPreservesValidFieldsAndStrictStatusRules() {
        val entity = ExchangeRateEntity(
            date = "2026-08-13",
            requestedDate = "2026-08-13",
            effectiveDate = "2026-08-13",
            rate = 4.9765,
            source = "BNR_OFFICIAL",
            fetchedAt = 1730000000000L,
            status = "OFFICIAL"
        )

        val dto = ExchangeRateDto.fromEntity(entity, householdId = "hh_777", migrationId = "mig_sess_1001")
        assertEquals("2026-08-13", dto.requestedDate)
        assertEquals("2026-08-13", dto.effectiveDate)
        assertEquals(4.9765, dto.rate!!, 0.0001)
        assertEquals("BNR_OFFICIAL", dto.source)
        assertEquals("OFFICIAL", dto.status)
        assertEquals("hh_777", dto.householdId)
        assertEquals("mig_sess_1001", dto.migrationId)
        assertTrue(FirestoreDtoValidator.isValidExchangeRateDto(dto))

        // Non-official rate remains non-official
        val nonOfficialDto = ExchangeRateDto(
            requestedDate = "2026-08-13",
            effectiveDate = "2026-08-13",
            rate = 5.0000,
            source = "USER_MANUAL",
            status = "UNVERIFIED",
            householdId = "hh_777",
            migrationId = "mig_sess_1001"
        )
        assertTrue(FirestoreDtoValidator.isValidExchangeRateDto(nonOfficialDto))

        val convertedEntity = nonOfficialDto.toEntity("2026-08-13")
        assertNotNull(convertedEntity)
        assertEquals("UNVERIFIED", convertedEntity?.status)
        assertEquals("USER_MANUAL", convertedEntity?.source)

        // Attempting to claim OFFICIAL with non-official source fails validation
        val invalidOfficialDto = ExchangeRateDto(
            requestedDate = "2026-08-13",
            effectiveDate = "2026-08-13",
            rate = 5.0000,
            source = "USER_MANUAL",
            status = "OFFICIAL",
            householdId = "hh_777"
        )
        assertTrue(!FirestoreDtoValidator.isValidExchangeRateDto(invalidOfficialDto))

        // Conversion downgrades questionable OFFICIAL status when source is non-official
        val downgradedEntity = invalidOfficialDto.toEntity("2026-08-13")
        assertNotNull(downgradedEntity)
        assertEquals("UNVERIFIED", downgradedEntity?.status)
    }

    @Test
    fun testSyncOutboxRemainsUntouchedAndIndependent() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val db = Room.inMemoryDatabaseBuilder(context, FinTrackDatabase::class.java).build()
        val outboxDao = db.syncOutboxDao()
        val migrationDao = db.migrationStateDao()

        val outboxEntry = SyncOutboxEntity(
            id = "outbox_1",
            entityType = "TRANSACTION",
            entityId = "tx_123",
            operation = "CREATE",
            status = "PENDING",
            createdAt = 1730000000000L,
            updatedAt = 1730000000000L
        )
        outboxDao.insertOutboxEntry(outboxEntry)

        val migrationState = MigrationStateEntity(
            migrationId = "mig_sess_2000",
            householdId = "hh_999",
            initiatedByUid = "user_1",
            stage = "TRANSACTIONS_UPLOADING",
            processedCount = 10,
            totalCount = 20
        )
        migrationDao.insertMigrationState(migrationState)

        val pendingOutbox = outboxDao.getPendingEntries()
        assertEquals(1, pendingOutbox.size)
        assertEquals("outbox_1", pendingOutbox[0].id)

        val retrievedMigration = migrationDao.getMigrationStateById("mig_sess_2000")
        assertNotNull(retrievedMigration)
        assertEquals("mig_sess_2000", retrievedMigration?.migrationId)

        db.close()
    }
}
