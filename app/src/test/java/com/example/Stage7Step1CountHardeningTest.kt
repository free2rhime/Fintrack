package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.db.FinTrackDatabase
import com.example.data.model.CategoryEntity
import com.example.data.model.ExchangeRateEntity
import com.example.data.model.TransactionEntity
import com.example.data.repository.ConflictReason
import com.example.data.repository.FirestoreMigrationPreflightCoordinator
import com.example.data.repository.FirestoreMigrationUploader
import com.example.data.repository.FirestoreSyncRepository
import com.example.data.repository.MigrationUploadResult
import com.example.data.repository.PreflightValidationResult
import com.example.data.util.CsvBackupManager
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class Stage7Step1CountHardeningTest {

    private lateinit var context: Context
    private lateinit var db: FinTrackDatabase
    private lateinit var fakeSnapshotSource: FakeSnapshotSource
    private lateinit var syncRepository: FirestoreSyncRepository
    private lateinit var coordinator: FirestoreMigrationPreflightCoordinator
    private lateinit var uploader: FirestoreMigrationUploader
    private lateinit var tempBackupDir: File

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
        coordinator = FirestoreMigrationPreflightCoordinator(
            database = db,
            snapshotSource = fakeSnapshotSource,
            backupManager = CsvBackupManager
        )
        uploader = FirestoreMigrationUploader(
            database = db,
            snapshotSource = fakeSnapshotSource,
            syncRepository = syncRepository,
            batchSize = 100
        )

        tempBackupDir = File(context.cacheDir, "step1_backup_${System.currentTimeMillis()}").apply {
            mkdirs()
        }
    }

    @After
    fun tearDown() {
        db.close()
        tempBackupDir.deleteRecursively()
    }

    @Test
    fun testRemoteTransactionCountExceptionFailsPreflightClosed() = runTest {
        fakeSnapshotSource.setMember("hh_fail_tx", "user_1", "OWNER", "ACTIVE")
        fakeSnapshotSource.shouldFailRemoteTransactionCount = true
        fakeSnapshotSource.remoteCountException = IOException("Network connection timed out during transaction count query")

        CsvBackupManager.createMigrationBackupBundle(
            bundleDir = tempBackupDir,
            transactions = emptyList(),
            categories = emptyList(),
            exchangeRates = emptyList()
        )

        val result = coordinator.validatePreflight("hh_fail_tx", "user_1", tempBackupDir)

        // Must fail-closed rather than passing as count=0
        assertTrue(result is PreflightValidationResult.Failure)
        val failure = result as PreflightValidationResult.Failure
        assertTrue(failure.sanitizedError.contains("Network connection timed out"))
    }

    @Test
    fun testRemoteCategoryCountPermissionDeniedFailsPreflightClosed() = runTest {
        fakeSnapshotSource.setMember("hh_perm_cat", "user_1", "OWNER", "ACTIVE")
        fakeSnapshotSource.shouldFailRemoteCategoryCount = true
        fakeSnapshotSource.remoteCountException = SecurityException("PERMISSION_DENIED: User is not authorized to count categories")

        CsvBackupManager.createMigrationBackupBundle(
            bundleDir = tempBackupDir,
            transactions = emptyList(),
            categories = emptyList(),
            exchangeRates = emptyList()
        )

        val result = coordinator.validatePreflight("hh_perm_cat", "user_1", tempBackupDir)

        // Must fail-closed rather than passing as count=0
        assertTrue(result is PreflightValidationResult.Failure)
        val failure = result as PreflightValidationResult.Failure
        assertTrue(failure.sanitizedError.contains("PERMISSION_DENIED"))
    }

    @Test
    fun testLargeDatasetCountGreaterThan100HandledAccuratelyInVerification() = runTest {
        fakeSnapshotSource.setMember("hh_large_data", "user_1", "OWNER", "ACTIVE")

        // Insert 150 local transactions
        val transactions = (1..150).map { i ->
            TransactionEntity(
                id = "tx_large_$i",
                amountRON = 10.0 * i,
                amountEUR = 2.0 * i,
                exchangeRate = 5.0,
                exchangeRateDate = "2026-08-14",
                description = "Item $i",
                type = "Expense",
                category = "General",
                subCategory = "",
                account = "Card",
                date = "2026-08-14"
            )
        }
        db.transactionDao().insertAllTransactions(transactions)

        db.categoryDao().insertCategory(
            CategoryEntity(id = "cat_gen", name = "General", type = "Expense")
        )
        db.exchangeRateDao().insertRate(
            ExchangeRateEntity(date = "2026-08-14", rate = 5.0, source = "BNR_OFFICIAL", status = "OFFICIAL")
        )

        // Upload execution
        val uploadResult = uploader.executeMigration(
            householdId = "hh_large_data",
            userUid = "user_1",
            migrationId = "mig_large_150"
        )

        // Must succeed with exact count of 150 transactions, 1 category, 1 exchange rate without 100-item truncation
        assertTrue(uploadResult is MigrationUploadResult.Success)
        val success = uploadResult as MigrationUploadResult.Success
        assertEquals(150, success.transactionsUploaded)
        assertEquals(1, success.categoriesUploaded)
        assertEquals(1, success.ratesUploaded)
        assertEquals(152, success.totalProcessed)
        assertEquals(150, fakeSnapshotSource.remoteTransactionCount)
    }

    @Test
    fun testUploaderVerificationFailsWhenRemoteCountThrowsException() = runTest {
        fakeSnapshotSource.setMember("hh_up_err", "user_1", "OWNER", "ACTIVE")

        db.categoryDao().insertCategory(CategoryEntity(id = "cat_1", name = "Food", type = "Expense"))

        // Simulate verification stage failing with remote exception
        fakeSnapshotSource.shouldFailRemoteCategoryCount = true
        fakeSnapshotSource.remoteCountException = IOException("Firestore unavailable during verification")

        val result = uploader.executeMigration(
            householdId = "hh_up_err",
            userUid = "user_1",
            migrationId = "mig_err_verify"
        )

        assertTrue(result is MigrationUploadResult.Failure)
        val failure = result as MigrationUploadResult.Failure
        assertTrue(failure.sanitizedError.contains("Firestore unavailable during verification"))
    }
}
