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
class Stage2CUploadTest {

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
    fun testSuccessfulUploadReachesCompletedAndCorrectStageTransitions() = runTest {
        fakeSnapshotSource.setMember("hh_100", "user_owner_100", "OWNER", "ACTIVE")

        // Setup local data
        db.categoryDao().insertCategory(CategoryEntity(id = "cat_1", name = "Food", type = "Expense"))
        db.categoryDao().insertCategory(CategoryEntity(id = "cat_2", name = "Salary", type = "Income"))
        db.exchangeRateDao().insertRate(
            ExchangeRateEntity(date = "2026-08-14", rate = 5.0, source = "BNR_OFFICIAL", status = "OFFICIAL")
        )
        db.transactionDao().insertTransaction(
            TransactionEntity(
                id = "tx_1",
                amountRON = 100.0,
                amountEUR = 20.0,
                exchangeRate = 5.0,
                exchangeRateDate = "2026-08-14",
                description = "Lunch",
                type = "Expense",
                category = "Food",
                subCategory = "",
                account = "Card",
                date = "2026-08-14"
            )
        )

        val result = uploader.executeMigration(
            householdId = "hh_100",
            userUid = "user_owner_100",
            migrationId = "mig_stage_test",
            backupBundlePath = "/mock/backup/dir"
        )

        assertTrue("Upload should succeed", result is MigrationUploadResult.Success)
        val success = result as MigrationUploadResult.Success
        assertEquals("mig_stage_test", success.migrationId)
        assertEquals(2, success.categoriesUploaded)
        assertEquals(1, success.ratesUploaded)
        assertEquals(1, success.transactionsUploaded)
        assertEquals(4, success.totalProcessed)

        // Verify local Room migration state
        val localState = db.migrationStateDao().getMigrationStateById("mig_stage_test")
        assertNotNull(localState)
        assertEquals("COMPLETED", localState!!.stage)
        assertEquals("COMPLETED", localState.currentPhase)
        assertEquals(4, localState.processedCount)
        assertEquals(4, localState.totalCount)

        // Verify remote migration state document
        val remoteDoc = fakeSnapshotSource.createdMigrationDocs["mig_stage_test"]
        assertNotNull(remoteDoc)
        assertEquals("COMPLETED", remoteDoc!!["stage"])
        assertEquals(4, remoteDoc["processedCount"])
        assertEquals(4, remoteDoc["totalCount"])

        // Verify stage progression recorded across updates
        val stagesRecorded = fakeSnapshotSource.updatedMigrationDocs.mapNotNull { it.second["stage"] as? String }
        assertTrue(stagesRecorded.contains("BACKUP_CREATED"))
        assertTrue(stagesRecorded.contains("CATEGORIES_UPLOADING"))
        assertTrue(stagesRecorded.contains("RATES_UPLOADING"))
        assertTrue(stagesRecorded.contains("TRANSACTIONS_UPLOADING"))
        assertTrue(stagesRecorded.contains("VERIFYING"))
        assertTrue(stagesRecorded.contains("COMPLETED"))
    }

    @Test
    fun testEntitiesSplitInto100ChunkBatches() = runTest {
        fakeSnapshotSource.setMember("hh_batch_test", "user_admin", "ADMIN", "ACTIVE")

        // Insert 250 transactions
        for (i in 1..250) {
            db.transactionDao().insertTransaction(
                TransactionEntity(
                    id = "tx_batch_$i",
                    amountRON = 10.0 * i,
                    amountEUR = 2.0 * i,
                    exchangeRate = 5.0,
                    exchangeRateDate = "2026-08-14",
                    description = "Tx $i",
                    type = "Expense",
                    category = "General",
                    subCategory = "",
                    account = "Card",
                    date = "2026-08-14"
                )
            )
        }

        val result = uploader.executeMigration(
            householdId = "hh_batch_test",
            userUid = "user_admin",
            migrationId = "mig_batch_250"
        )

        assertTrue(result is MigrationUploadResult.Success)
        assertEquals(3, fakeSnapshotSource.uploadedTransactionBatches.size)
        assertEquals(100, fakeSnapshotSource.uploadedTransactionBatches[0].size)
        assertEquals(100, fakeSnapshotSource.uploadedTransactionBatches[1].size)
        assertEquals(50, fakeSnapshotSource.uploadedTransactionBatches[2].size)
    }

    @Test
    fun testUploadOrderIsCategoriesThenRatesThenTransactions() = runTest {
        fakeSnapshotSource.setMember("hh_order_check", "user_admin", "ADMIN", "ACTIVE")

        db.categoryDao().insertCategory(CategoryEntity(id = "cat_ord", name = "TestCat", type = "Expense"))
        db.exchangeRateDao().insertRate(
            ExchangeRateEntity(date = "2026-08-14", rate = 4.97, source = "BNR_OFFICIAL", status = "OFFICIAL")
        )
        db.transactionDao().insertTransaction(
            TransactionEntity(
                id = "tx_ord",
                amountRON = 50.0,
                amountEUR = 10.0,
                exchangeRate = 5.0,
                exchangeRateDate = "2026-08-14",
                description = "Order test",
                type = "Expense",
                category = "TestCat",
                subCategory = "",
                account = "Cash",
                date = "2026-08-14"
            )
        )

        val result = uploader.executeMigration(
            householdId = "hh_order_check",
            userUid = "user_admin",
            migrationId = "mig_order"
        )

        assertTrue(result is MigrationUploadResult.Success)
        assertEquals(listOf("CATEGORIES", "RATES", "TRANSACTIONS"), fakeSnapshotSource.operationOrder)
    }

    @Test
    fun testListenerSuppressionRemainsActiveDuringUploadAndRestoredAfter() = runTest {
        var suppressionDuringUpload: Boolean? = null

        val customSnapshotSource = object : com.example.data.repository.FirestoreSnapshotSource by fakeSnapshotSource {
            override suspend fun uploadCategoriesBatch(householdId: String, categories: List<Map<String, Any?>>): Boolean {
                suppressionDuringUpload = syncRepository.isSuppressed
                return fakeSnapshotSource.uploadCategoriesBatch(householdId, categories)
            }
        }

        val customUploader = FirestoreMigrationUploader(
            database = db,
            snapshotSource = customSnapshotSource,
            syncRepository = syncRepository,
            batchSize = 100
        )

        fakeSnapshotSource.setMember("hh_sup", "user_sup", "OWNER", "ACTIVE")
        db.categoryDao().insertCategory(CategoryEntity(id = "cat_sup", name = "SuppressionCheck", type = "Income"))

        assertFalse("Suppression initially false", syncRepository.isSuppressed)
        val result = customUploader.executeMigration(
            householdId = "hh_sup",
            userUid = "user_sup",
            migrationId = "mig_sup"
        )

        assertTrue(result is MigrationUploadResult.Success)
        assertEquals(true, suppressionDuringUpload)
        assertFalse("Suppression restored to false after migration", syncRepository.isSuppressed)
    }

    @Test
    fun testVerificationMismatchTransitionsToFailed() = runTest {
        fakeSnapshotSource.setMember("hh_mismatch", "user_mismatch", "OWNER", "ACTIVE")
        db.categoryDao().insertCategory(CategoryEntity(id = "cat_mismatch", name = "Mismatch", type = "Expense"))
        db.transactionDao().insertTransaction(
            TransactionEntity(
                id = "tx_mismatch",
                amountRON = 100.0,
                amountEUR = 20.0,
                exchangeRate = 5.0,
                exchangeRateDate = "2026-08-14",
                description = "Mismatch test",
                type = "Expense",
                category = "Mismatch",
                subCategory = "",
                account = "Card",
                date = "2026-08-14"
            )
        )

        // Snapshot source will automatically increment counts on upload, so let's override getRemoteTransactionCount to simulate a remote count mismatch
        val mismatchSnapshotSource = object : com.example.data.repository.FirestoreSnapshotSource by fakeSnapshotSource {
            override suspend fun getRemoteTransactionCount(householdId: String): Int {
                return 999 // Mismatch from expected 1
            }
        }

        val mismatchUploader = FirestoreMigrationUploader(
            database = db,
            snapshotSource = mismatchSnapshotSource,
            syncRepository = syncRepository
        )

        val result = mismatchUploader.executeMigration(
            householdId = "hh_mismatch",
            userUid = "user_mismatch",
            migrationId = "mig_mismatch"
        )

        assertTrue("Expected failure due to verification mismatch", result is MigrationUploadResult.Failure)
        val failure = result as MigrationUploadResult.Failure
        assertEquals("VERIFYING", failure.stage)
        assertTrue(failure.sanitizedError.contains("Verification mismatch"))

        // Check local Room entity stage is FAILED
        val localState = db.migrationStateDao().getMigrationStateById("mig_mismatch")
        assertNotNull(localState)
        assertEquals("FAILED", localState!!.stage)
        assertTrue(localState.lastError?.contains("Verification mismatch") == true)
    }

    @Test
    fun testUploadFailureTransitionsToFailedWithSanitizedError() = runTest {
        fakeSnapshotSource.setMember("hh_upload_fail", "user_fail", "OWNER", "ACTIVE")
        fakeSnapshotSource.shouldFailTransactionUpload = true

        db.categoryDao().insertCategory(CategoryEntity(id = "cat_fail", name = "FailCat", type = "Expense"))
        db.transactionDao().insertTransaction(
            TransactionEntity(
                id = "tx_fail",
                amountRON = 100.0,
                amountEUR = 20.0,
                exchangeRate = 5.0,
                exchangeRateDate = "2026-08-14",
                description = "Fail Tx",
                type = "Expense",
                category = "FailCat",
                subCategory = "",
                account = "Card",
                date = "2026-08-14"
            )
        )

        val result = uploader.executeMigration(
            householdId = "hh_upload_fail",
            userUid = "user_fail",
            migrationId = "mig_upload_fail"
        )

        assertTrue(result is MigrationUploadResult.Failure)
        val failure = result as MigrationUploadResult.Failure
        assertEquals("TRANSACTIONS_UPLOADING", failure.stage)
        assertTrue(failure.sanitizedError.contains("Failed to upload transaction batch"))
        assertFalse(failure.sanitizedError.contains("\tat "))

        // Verify local Room entity marked FAILED
        val localState = db.migrationStateDao().getMigrationStateById("mig_upload_fail")
        assertNotNull(localState)
        assertEquals("FAILED", localState!!.stage)
        assertFalse("Suppression restored even after failure", syncRepository.isSuppressed)
    }
}
