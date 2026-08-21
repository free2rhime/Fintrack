package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.db.FinTrackDatabase
import com.example.data.model.CategoryEntity
import com.example.data.model.ExchangeRateEntity
import com.example.data.model.TransactionEntity
import com.example.data.repository.FirestoreMigrationUploader
import com.example.data.repository.FirestoreSyncRepository
import com.example.data.repository.MigrationUploadResult
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class Stage7Step3ExecutionRevalidationTest {

    private lateinit var context: Context
    private lateinit var db: FinTrackDatabase
    private lateinit var fakeSnapshotSource: FakeSnapshotSource
    private lateinit var syncRepository: FirestoreSyncRepository
    private lateinit var uploader: FirestoreMigrationUploader

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, FinTrackDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        fakeSnapshotSource = FakeSnapshotSource()
        syncRepository = FirestoreSyncRepository(
            database = db,
            snapshotSource = fakeSnapshotSource
        )
        uploader = FirestoreMigrationUploader(
            database = db,
            snapshotSource = fakeSnapshotSource,
            syncRepository = syncRepository,
            batchSize = 100
        )
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun testOwnerRemovedAfterPreflightAbortsUploadBeforeFirstBatch() = runTest {
        // Local data to upload
        db.categoryDao().insertCategory(CategoryEntity(id = "cat_1", name = "Groceries", type = "Expense"))

        // Simulate user revoked from household after preflight
        fakeSnapshotSource.members.clear() // User has no membership

        val result = uploader.executeMigration(
            householdId = "hh_stale_1",
            userUid = "user_revoked",
            migrationId = "mig_revoked"
        )

        assertTrue("Upload must fail immediately", result is MigrationUploadResult.Failure)
        val failure = result as MigrationUploadResult.Failure
        assertEquals("PREFLIGHT", failure.stage)
        assertTrue(failure.sanitizedError.contains("Execution revalidation failed"))

        // Ensure no categories were uploaded to cloud
        assertEquals(0, fakeSnapshotSource.uploadedCategoryBatches.size)
        assertEquals(0, fakeSnapshotSource.operationOrder.size)

        // Ensure suppression is cleaned up
        assertFalse(syncRepository.isSuppressed)

        // Ensure migration state in Room is marked FAILED
        val localState = db.migrationStateDao().getMigrationStateById("mig_revoked")
        assertNotNull(localState)
        assertEquals("FAILED", localState!!.stage)
    }

    @Test
    fun testUserRoleDemotedToMemberAbortsUploadBeforeFirstBatch() = runTest {
        db.categoryDao().insertCategory(CategoryEntity(id = "cat_2", name = "Rent", type = "Expense"))

        // User is ACTIVE but MEMBER (not OWNER or ADMIN)
        fakeSnapshotSource.setMember("hh_demoted", "user_demoted", "MEMBER", "ACTIVE")

        val result = uploader.executeMigration(
            householdId = "hh_demoted",
            userUid = "user_demoted",
            migrationId = "mig_demoted"
        )

        assertTrue(result is MigrationUploadResult.Failure)
        val failure = result as MigrationUploadResult.Failure
        assertEquals("PREFLIGHT", failure.stage)
        assertTrue(failure.sanitizedError.contains("is no longer an active OWNER or ADMIN"))

        // Ensure no batch uploads executed
        assertEquals(0, fakeSnapshotSource.uploadedCategoryBatches.size)
        assertFalse(syncRepository.isSuppressed)
    }

    @Test
    fun testActiveHouseholdChangedAfterPreflightAbortsUpload() = runTest {
        db.categoryDao().insertCategory(CategoryEntity(id = "cat_3", name = "Utilities", type = "Expense"))

        // User belongs to hh_new, but migration was requested for hh_old
        fakeSnapshotSource.setMember("hh_new", "user_switched", "OWNER", "ACTIVE")

        val result = uploader.executeMigration(
            householdId = "hh_old",
            userUid = "user_switched",
            migrationId = "mig_switched"
        )

        assertTrue(result is MigrationUploadResult.Failure)
        val failure = result as MigrationUploadResult.Failure
        assertEquals("PREFLIGHT", failure.stage)
        assertTrue(failure.sanitizedError.contains("Active household mismatch"))
        assertEquals(0, fakeSnapshotSource.uploadedCategoryBatches.size)
    }

    @Test
    fun testLockHeldByAnotherSessionAbortsUpload() = runTest {
        db.categoryDao().insertCategory(CategoryEntity(id = "cat_4", name = "Dining", type = "Expense"))

        fakeSnapshotSource.setMember("hh_locked", "user_lock", "OWNER", "ACTIVE")

        // Active lock held by another session
        fakeSnapshotSource.activeMigrationSession = mapOf(
            "migrationId" to "mig_other_session_999",
            "stage" to "CATEGORIES_UPLOADING"
        )

        val result = uploader.executeMigration(
            householdId = "hh_locked",
            userUid = "user_lock",
            migrationId = "mig_current_session_111"
        )

        assertTrue(result is MigrationUploadResult.Failure)
        val failure = result as MigrationUploadResult.Failure
        assertEquals("PREFLIGHT", failure.stage)
        assertTrue(failure.sanitizedError.contains("Migration lock held by another active session"))
        assertEquals(0, fakeSnapshotSource.uploadedCategoryBatches.size)
    }

    @Test
    fun testExecutionRevalidationPassesForValidSessionAndCompletesUpload() = runTest {
        fakeSnapshotSource.setMember("hh_valid", "user_valid", "OWNER", "ACTIVE")

        db.categoryDao().insertCategory(CategoryEntity(id = "cat_ok", name = "Test", type = "Expense"))
        db.exchangeRateDao().insertRate(
            ExchangeRateEntity(date = "2026-08-14", rate = 5.0, source = "BNR_OFFICIAL", status = "OFFICIAL")
        )
        db.transactionDao().insertTransaction(
            TransactionEntity(
                id = "tx_ok",
                amountRON = 100.0,
                amountEUR = 20.0,
                exchangeRate = 5.0,
                exchangeRateDate = "2026-08-14",
                description = "Test Tx",
                type = "Expense",
                category = "Test",
                subCategory = "",
                account = "Card",
                date = "2026-08-14"
            )
        )

        val result = uploader.executeMigration(
            householdId = "hh_valid",
            userUid = "user_valid",
            migrationId = "mig_valid_123"
        )

        assertTrue(result is MigrationUploadResult.Success)
        val success = result as MigrationUploadResult.Success
        assertEquals("mig_valid_123", success.migrationId)
        assertEquals(1, success.categoriesUploaded)
        assertEquals(1, success.ratesUploaded)
        assertEquals(1, success.transactionsUploaded)
        assertEquals(3, success.totalProcessed)
    }
}
