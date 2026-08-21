package com.example

import android.app.Application
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
import com.example.data.repository.PreflightValidationResult
import com.example.data.repository.RoomCategoryRepository
import com.example.data.repository.RoomTransactionRepository
import com.example.data.service.ExchangeRateService
import com.example.data.util.CsvBackupManager
import com.example.ui.MainViewModel
import com.example.ui.MigrationUiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class Stage7Step2MandatoryBackupTest {

    private val testDispatcher = kotlinx.coroutines.test.UnconfinedTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private lateinit var application: Application
    private lateinit var db: FinTrackDatabase
    private lateinit var fakeSnapshotSource: FakeSnapshotSource
    private lateinit var syncRepository: FirestoreSyncRepository
    private lateinit var preflightCoordinator: FirestoreMigrationPreflightCoordinator
    private lateinit var migrationUploader: FirestoreMigrationUploader
    private lateinit var authRepository: FakeTestAuthRepository
    private lateinit var settingsRepository: FakeTestSettingsRepository
    private lateinit var transactionRepository: RoomTransactionRepository
    private lateinit var categoryRepository: RoomCategoryRepository
    private lateinit var viewModel: MainViewModel
    private lateinit var tempBackupDir: File

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        application = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(application, FinTrackDatabase::class.java)
            .allowMainThreadQueries()
            .setQueryExecutor(Runnable::run)
            .setTransactionExecutor(Runnable::run)
            .build()

        tempBackupDir = File(application.cacheDir, "stage7_step2_backup_${System.currentTimeMillis()}").apply {
            mkdirs()
        }

        fakeSnapshotSource = FakeSnapshotSource()
        syncRepository = FirestoreSyncRepository(
            database = db,
            snapshotSource = fakeSnapshotSource,
            coroutineScope = testScope
        )

        preflightCoordinator = FirestoreMigrationPreflightCoordinator(
            database = db,
            snapshotSource = fakeSnapshotSource,
            backupManager = CsvBackupManager
        )

        migrationUploader = FirestoreMigrationUploader(
            database = db,
            snapshotSource = fakeSnapshotSource,
            syncRepository = syncRepository,
            batchSize = 100
        )

        authRepository = FakeTestAuthRepository("user_owner_1")
        settingsRepository = FakeTestSettingsRepository()

        val exchangeRateService = ExchangeRateService(db.exchangeRateDao())
        transactionRepository = RoomTransactionRepository(
            transactionDao = db.transactionDao(),
            exchangeRateService = exchangeRateService,
            exchangeRateDao = db.exchangeRateDao(),
            database = db
        )

        categoryRepository = RoomCategoryRepository(
            categoryDao = db.categoryDao(),
            syncOutboxDao = db.syncOutboxDao(),
            database = db
        )

        viewModel = MainViewModel(
            transactionRepository = transactionRepository,
            categoryRepository = categoryRepository,
            settingsRepository = settingsRepository,
            authRepository = authRepository,
            syncRepository = syncRepository,
            preflightCoordinator = preflightCoordinator,
            migrationUploader = migrationUploader,
            database = db,
            ioDispatcher = testDispatcher,
            application = application
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        db.close()
        tempBackupDir.deleteRecursively()
    }

    @Test
    fun testAutomaticMandatoryBackupCreationWhenNoneProvided() = runTest(testDispatcher) {
        fakeSnapshotSource.setMember("hh_auto_backup", "user_owner_1", "OWNER", "ACTIVE")
        fakeSnapshotSource.remoteTransactionCount = 0
        fakeSnapshotSource.remoteCategoryCount = 0
        fakeSnapshotSource.remoteExchangeRateCount = 0

        val cat = CategoryEntity(id = "cat_1", name = "Salary", type = "Income")
        db.categoryDao().insertCategory(cat)

        val tx = TransactionEntity(
            id = "tx_1",
            amountRON = 5000.0,
            amountEUR = 1000.0,
            exchangeRate = 5.0,
            exchangeRateDate = "2026-08-14",
            description = "Monthly paycheck",
            type = "Income",
            category = "Salary",
            subCategory = "",
            account = "Bank",
            date = "2026-08-14"
        )
        db.transactionDao().insertTransaction(tx)

        val rate = ExchangeRateEntity(
            date = "2026-08-14",
            rate = 5.0,
            source = "BNR_OFFICIAL",
            status = "OFFICIAL"
        )
        db.exchangeRateDao().insertRate(rate)

        // Trigger preflight with backupBundleDir = null (automatic backup generation)
        viewModel.startMigrationPreflight(
            targetHouseholdId = "hh_auto_backup",
            targetUserUid = "user_owner_1",
            backupBundleDir = null
        )
        advanceUntilIdle()

        val state = viewModel.migrationUiState.value
        assertTrue("Expected Preview state after automatic backup creation, got: $state", state is MigrationUiState.Preview)
        val preview = (state as MigrationUiState.Preview).preview
        assertNotNull(preview.backupBundlePath)

        val backupDir = File(preview.backupBundlePath!!)
        assertTrue(backupDir.exists() && backupDir.isDirectory)
        assertTrue(File(backupDir, CsvBackupManager.MANIFEST_FILE_NAME).exists())
        assertTrue(File(backupDir, CsvBackupManager.TRANSACTIONS_FILE_NAME).exists())
        assertTrue(File(backupDir, CsvBackupManager.CATEGORIES_FILE_NAME).exists())
        assertTrue(File(backupDir, CsvBackupManager.EXCHANGE_RATES_FILE_NAME).exists())
    }

    @Test
    fun testBackupValidationFailureCorruptedManifest() = runTest {
        fakeSnapshotSource.setMember("hh_corrupt_manifest", "user_owner_1", "OWNER", "ACTIVE")

        // Create corrupt manifest backup
        CsvBackupManager.createMigrationBackupBundle(
            bundleDir = tempBackupDir,
            transactions = emptyList(),
            categories = emptyList(),
            exchangeRates = emptyList()
        )
        File(tempBackupDir, CsvBackupManager.MANIFEST_FILE_NAME).writeText("{ corrupt json: broken ")

        val result = preflightCoordinator.validatePreflight(
            householdId = "hh_corrupt_manifest",
            userUid = "user_owner_1",
            backupBundleDir = tempBackupDir
        )

        assertTrue(result is PreflightValidationResult.Conflict)
        val conflict = result as PreflightValidationResult.Conflict
        assertEquals(ConflictReason.BACKUP_INVALID, conflict.reason)
        assertTrue(conflict.details.contains("Manifest metadata is corrupted or invalid"))
    }

    @Test
    fun testBackupValidationFailureMissingRequiredFile() = runTest {
        fakeSnapshotSource.setMember("hh_missing_file", "user_owner_1", "OWNER", "ACTIVE")

        CsvBackupManager.createMigrationBackupBundle(
            bundleDir = tempBackupDir,
            transactions = emptyList(),
            categories = emptyList(),
            exchangeRates = emptyList()
        )
        File(tempBackupDir, CsvBackupManager.TRANSACTIONS_FILE_NAME).delete()

        val result = preflightCoordinator.validatePreflight(
            householdId = "hh_missing_file",
            userUid = "user_owner_1",
            backupBundleDir = tempBackupDir
        )

        assertTrue(result is PreflightValidationResult.Conflict)
        val conflict = result as PreflightValidationResult.Conflict
        assertEquals(ConflictReason.BACKUP_INVALID, conflict.reason)
        assertTrue(conflict.details.contains("transactions.csv"))
    }

    @Test
    fun testBackupValidationFailureRowCountParityMismatch() = runTest {
        fakeSnapshotSource.setMember("hh_parity_mismatch", "user_owner_1", "OWNER", "ACTIVE")

        val tx1 = TransactionEntity(
            id = "tx_1",
            amountRON = 10.0,
            amountEUR = 2.0,
            exchangeRate = 5.0,
            exchangeRateDate = "2026-08-14",
            description = "Coffee",
            type = "Expense",
            category = "General",
            subCategory = "",
            account = "Card",
            date = "2026-08-14"
        )

        // Bundle created with 1 transaction in manifest
        CsvBackupManager.createMigrationBackupBundle(
            bundleDir = tempBackupDir,
            transactions = listOf(tx1),
            categories = emptyList(),
            exchangeRates = emptyList()
        )

        // Modify file to add an extra un-manifested line
        File(tempBackupDir, CsvBackupManager.TRANSACTIONS_FILE_NAME).appendText("tx_extra,2026-08-14,Extra,20.0,Expense,General,,,4.0,5.0,2026-08-14,OFFICIAL,OFFICIAL,local_user,,0,0\n")

        val result = preflightCoordinator.validatePreflight(
            householdId = "hh_parity_mismatch",
            userUid = "user_owner_1",
            backupBundleDir = tempBackupDir
        )

        assertTrue(result is PreflightValidationResult.Conflict)
        val conflict = result as PreflightValidationResult.Conflict
        assertEquals(ConflictReason.BACKUP_INVALID, conflict.reason)
        assertTrue(conflict.details.contains("does not match manifest count"))
    }

    @Test
    fun testExchangeRateConflictDetectionInPreflight() = runTest {
        fakeSnapshotSource.setMember("hh_rate_conflict", "user_owner_1", "OWNER", "ACTIVE")
        fakeSnapshotSource.remoteTransactionCount = 0
        fakeSnapshotSource.remoteCategoryCount = 0
        fakeSnapshotSource.remoteExchangeRateCount = 12 // Remote exchange rates exist

        CsvBackupManager.createMigrationBackupBundle(
            bundleDir = tempBackupDir,
            transactions = emptyList(),
            categories = emptyList(),
            exchangeRates = emptyList()
        )

        val result = preflightCoordinator.validatePreflight(
            householdId = "hh_rate_conflict",
            userUid = "user_owner_1",
            backupBundleDir = tempBackupDir
        )

        assertTrue(result is PreflightValidationResult.Conflict)
        val conflict = result as PreflightValidationResult.Conflict
        assertEquals(ConflictReason.EXISTING_REMOTE_DATA_DETECTED, conflict.reason)
        assertTrue(conflict.details.contains("exchange rate data detected"))
        assertTrue(conflict.details.contains("12 documents found"))
    }
}
