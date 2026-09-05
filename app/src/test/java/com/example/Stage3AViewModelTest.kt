package com.example

import android.app.Application
import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.db.FinTrackDatabase
import com.example.data.model.CategoryEntity
import com.example.data.model.ExchangeRateEntity
import com.example.data.model.FilterSettings
import com.example.data.model.TransactionEntity
import com.example.data.repository.AuthRepository
import com.example.data.repository.AuthState
import com.example.data.repository.CategoryRepository
import com.example.data.repository.FirestoreMigrationPreflightCoordinator
import com.example.data.repository.FirestoreMigrationUploader
import com.example.data.repository.FirestoreSyncRepository
import com.example.data.repository.RoomCategoryRepository
import com.example.data.repository.RoomTransactionRepository
import com.example.data.repository.SettingsRepository
import com.example.data.service.ExchangeRateService
import com.example.ui.MainViewModel
import com.example.ui.MigrationUiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

class FakeTestAuthRepository(initialUid: String? = "user_owner_1") : AuthRepository {
    private val _authState = MutableStateFlow<AuthState>(
        if (initialUid != null) AuthState.SignedIn(initialUid, "owner@example.com") else AuthState.SignedOut
    )
    override val authState: StateFlow<AuthState> = _authState.asStateFlow()

    override suspend fun signInWithGoogleCredential(idToken: String): Result<String> = Result.success("user_owner_1")
    override suspend fun signInWithTestUid(testUid: String, email: String?, displayName: String?): Result<String> {
        _authState.value = AuthState.SignedIn(testUid, email, displayName)
        return Result.success(testUid)
    }
    override suspend fun signOut() {
        _authState.value = AuthState.SignedOut
    }
    override fun getCurrentUserUid(): String? = (_authState.value as? AuthState.SignedIn)?.userUid
    override fun clearError() {}
    override fun setAuthError(message: String) {
        _authState.value = AuthState.AuthError(message)
    }
}

class FakeTestSettingsRepository : SettingsRepository {
    private val _filterSettings = MutableStateFlow(FilterSettings())
    override val filterSettingsFlow: Flow<FilterSettings> = _filterSettings.asStateFlow()
    override val themeModeFlow: Flow<String> = flowOf("dark")
    override suspend fun updateSelectedPeriod(period: String) {}
    override suspend fun updateSelectedCurrency(currency: String) {}
    override suspend fun updateCustomDateRange(startDate: String, endDate: String) {}
    override suspend fun updateSelectedType(type: String) {}
    override suspend fun updateCategoryFilter(type: String, categoryName: String?) {}
    override suspend fun updateThemeMode(mode: String) {}
}

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class Stage3AViewModelTest {

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

        tempBackupDir = File(application.cacheDir, "vm_test_backup_${System.currentTimeMillis()}").apply {
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
            snapshotSource = fakeSnapshotSource
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
            ioDispatcher = testDispatcher,
            application = application
        )
    }

    @After
    fun tearDown() {
        syncRepository.stopSync()
        Dispatchers.resetMain()
        db.close()
        tempBackupDir.deleteRecursively()
    }

    @Test
    fun testPreflightSuccessProducesPreviewState() = runTest(testDispatcher) {
        fakeSnapshotSource.setMember("hh_1", "user_owner_1", "OWNER", "ACTIVE")
        fakeSnapshotSource.remoteTransactionCount = 0
        fakeSnapshotSource.remoteCategoryCount = 0
        fakeSnapshotSource.remoteExchangeRateCount = 0
        fakeSnapshotSource.activeMigrationSession = null

        db.categoryDao().deleteAllCategories()
        val cat = CategoryEntity(id = "cat_1", name = "Food", type = "Expense")
        val rate = ExchangeRateEntity(date = "2026-08-14", rate = 5.0, source = "BNR_OFFICIAL", status = "OFFICIAL")
        val tx = TransactionEntity(
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
        db.categoryDao().insertCategory(cat)
        db.exchangeRateDao().insertRate(rate)
        db.transactionDao().insertTransaction(tx)

        com.example.data.util.CsvBackupManager.createMigrationBackupBundle(
            bundleDir = tempBackupDir,
            transactions = listOf(tx),
            categories = listOf(cat),
            exchangeRates = listOf(rate)
        )

        assertEquals(MigrationUiState.Idle, viewModel.migrationUiState.value)

        viewModel.startMigrationPreflight(
            targetHouseholdId = "hh_1",
            targetUserUid = "user_owner_1",
            backupBundleDir = tempBackupDir
        )
        advanceUntilIdle()

        val state = viewModel.migrationUiState.value
        assertTrue("Expected MigrationUiState.Preview but got $state", state is MigrationUiState.Preview)
        val preview = (state as MigrationUiState.Preview).preview

        assertEquals("hh_1", preview.householdId)
        assertEquals("user_owner_1", preview.userUid)
        assertEquals("OWNER", preview.userRole)
        assertEquals(1, preview.categoriesCount)
        assertEquals(1, preview.exchangeRatesCount)
        assertEquals(1, preview.transactionsCount)
        assertEquals(3, preview.totalRecords)
        assertNotNull(preview.backupBundlePath)
    }

    @Test
    fun testPermissionFailureProducesConflictState() = runTest(testDispatcher) {
        // User has MEMBER role (not OWNER/ADMIN)
        fakeSnapshotSource.setMember("hh_1", "user_member_1", "MEMBER", "ACTIVE")

        viewModel.startMigrationPreflight(
            targetHouseholdId = "hh_1",
            targetUserUid = "user_member_1",
            backupBundleDir = null
        )
        advanceUntilIdle()

        val state = viewModel.migrationUiState.value
        assertTrue("Expected MigrationUiState.Conflict but got $state", state is MigrationUiState.Conflict)
        val conflict = (state as MigrationUiState.Conflict).conflict

        assertEquals("INSUFFICIENT_PERMISSIONS", conflict.reason)
        assertTrue(conflict.details.contains("Insufficient permissions"))
        assertFalse("Error details must be sanitized without stack traces", conflict.details.contains("\tat "))
    }

    @Test
    fun testRemoteConflictProducesConflictState() = runTest(testDispatcher) {
        fakeSnapshotSource.setMember("hh_1", "user_owner_1", "OWNER", "ACTIVE")
        fakeSnapshotSource.remoteTransactionCount = 5

        viewModel.startMigrationPreflight(
            targetHouseholdId = "hh_1",
            targetUserUid = "user_owner_1",
            backupBundleDir = null
        )
        advanceUntilIdle()

        val state = viewModel.migrationUiState.value
        assertTrue("Expected MigrationUiState.Conflict but got $state", state is MigrationUiState.Conflict)
        val conflict = (state as MigrationUiState.Conflict).conflict

        assertEquals("EXISTING_REMOTE_DATA_DETECTED", conflict.reason)
        assertTrue(conflict.details.contains("Existing remote records detected") || conflict.details.contains("Conflicting remote"))
    }

    @Test
    fun testCancellationReturnsToIdleWithoutCreatingSessionOrUploads() = runTest(testDispatcher) {
        fakeSnapshotSource.setMember("hh_cancel", "user_owner_1", "OWNER", "ACTIVE")
        fakeSnapshotSource.remoteTransactionCount = 0
        fakeSnapshotSource.remoteCategoryCount = 0
        fakeSnapshotSource.remoteExchangeRateCount = 0

        viewModel.startMigrationPreflight(
            targetHouseholdId = "hh_cancel",
            targetUserUid = "user_owner_1",
            backupBundleDir = null
        )
        advanceUntilIdle()

        assertTrue(viewModel.migrationUiState.value is MigrationUiState.Preview)

        // Cancel preview before confirmation
        viewModel.cancelMigrationPreview()

        assertEquals(MigrationUiState.Idle, viewModel.migrationUiState.value)

        // Verify zero migration documents written to remote source or local DB
        assertTrue(fakeSnapshotSource.createdMigrationDocs.isEmpty())
        val latestState = db.migrationStateDao().getLatestMigrationStateForHousehold("hh_cancel")
        assertNull(latestState)
    }

    @Test
    fun testConfirmationLaunchesUploaderAndProducesSuccessState() = runTest(testDispatcher) {
        fakeSnapshotSource.setMember("hh_exec", "user_owner_1", "OWNER", "ACTIVE")
        fakeSnapshotSource.remoteTransactionCount = 0
        fakeSnapshotSource.remoteCategoryCount = 0
        fakeSnapshotSource.remoteExchangeRateCount = 0

        db.categoryDao().deleteAllCategories()
        db.categoryDao().insertCategory(CategoryEntity(id = "cat_exec", name = "Bills", type = "Expense"))
        db.exchangeRateDao().insertRate(ExchangeRateEntity(date = "2026-08-14", rate = 5.0, source = "BNR_OFFICIAL", status = "OFFICIAL"))
        db.transactionDao().insertTransaction(
            TransactionEntity(
                id = "tx_exec",
                amountRON = 200.0,
                amountEUR = 40.0,
                exchangeRate = 5.0,
                exchangeRateDate = "2026-08-14",
                description = "Electricity",
                type = "Expense",
                category = "Bills",
                subCategory = "",
                account = "Bank",
                date = "2026-08-14"
            )
        )

        viewModel.startMigrationPreflight(
            targetHouseholdId = "hh_exec",
            targetUserUid = "user_owner_1",
            backupBundleDir = null
        )
        advanceUntilIdle()

        assertTrue(viewModel.migrationUiState.value is MigrationUiState.Preview)

        // Confirm and execute migration
        viewModel.confirmAndExecuteMigration()
        advanceUntilIdle()

        val finalState = viewModel.migrationUiState.value
        assertTrue("Expected MigrationUiState.Success but got $finalState", finalState is MigrationUiState.Success)
        val success = (finalState as MigrationUiState.Success).result

        assertEquals(1, success.categoriesUploaded)
        assertEquals(1, success.ratesUploaded)
        assertEquals(1, success.transactionsUploaded)
        assertEquals(3, success.totalProcessed)
        assertTrue(success.migrationId.startsWith("mig_"))

        // Check local Room entity stage is COMPLETED
        val localState = db.migrationStateDao().getMigrationStateById(success.migrationId)
        assertNotNull(localState)
        assertEquals("COMPLETED", localState!!.stage)
    }

    @Test
    fun testUploadFailureProducesFailureState() = runTest(testDispatcher) {
        fakeSnapshotSource.setMember("hh_fail", "user_owner_1", "OWNER", "ACTIVE")
        fakeSnapshotSource.remoteTransactionCount = 0
        fakeSnapshotSource.remoteCategoryCount = 0
        fakeSnapshotSource.remoteExchangeRateCount = 0

        db.categoryDao().deleteAllCategories()
        db.categoryDao().insertCategory(CategoryEntity(id = "cat_ok", name = "Test", type = "Expense"))
        db.transactionDao().insertTransaction(
            TransactionEntity(
                id = "tx_fail",
                amountRON = 10.0,
                amountEUR = 2.0,
                exchangeRate = 5.0,
                exchangeRateDate = "2026-08-14",
                description = "Item",
                type = "Expense",
                category = "Test",
                subCategory = "",
                account = "Cash",
                date = "2026-08-14"
            )
        )

        viewModel.startMigrationPreflight(
            targetHouseholdId = "hh_fail",
            targetUserUid = "user_owner_1",
            backupBundleDir = null
        )
        advanceUntilIdle()

        assertTrue(viewModel.migrationUiState.value is MigrationUiState.Preview)

        // Inject transaction upload failure
        fakeSnapshotSource.shouldFailTransactionUpload = true

        viewModel.confirmAndExecuteMigration()
        advanceUntilIdle()

        val finalState = viewModel.migrationUiState.value
        assertTrue("Expected MigrationUiState.Failure but got $finalState", finalState is MigrationUiState.Failure)
        val failure = (finalState as MigrationUiState.Failure).failure

        assertEquals("TRANSACTIONS_UPLOADING", failure.stage)
        assertTrue(failure.sanitizedError.contains("Failed to upload transaction batch"))
        assertFalse(failure.sanitizedError.contains("\tat "))
    }

    @Test
    fun testAllUiVisibleErrorsAreSanitized() = runTest(testDispatcher) {
        val throwingSnapshotSource = object : com.example.data.repository.FirestoreSnapshotSource {
            override fun listenToTransactions(householdId: String, onSnapshot: (List<Pair<String, Map<String, Any?>>>) -> Unit, onError: (Exception) -> Unit) =
                object : com.example.data.repository.ListenerRegistrationHandle { override fun remove() {} }
            override fun listenToCategories(householdId: String, onSnapshot: (List<Pair<String, Map<String, Any?>>>) -> Unit, onError: (Exception) -> Unit) =
                object : com.example.data.repository.ListenerRegistrationHandle { override fun remove() {} }
            override suspend fun resolveHouseholdId(userUid: String): com.example.data.repository.HouseholdResolutionResult =
                com.example.data.repository.HouseholdResolutionResult.NoHousehold
            override suspend fun getHouseholdMembership(householdId: String, userUid: String): Map<String, Any?>? {
                throw RuntimeException("INTERNAL_CRASH: Bad state at io.grpc.internal.Stream.flush(Stream.java:999)\n\tat java.lang.Thread.run(Thread.java:1012)")
            }
        }

        val errCoordinator = FirestoreMigrationPreflightCoordinator(
            database = db,
            snapshotSource = throwingSnapshotSource
        )

        val errViewModel = MainViewModel(
            transactionRepository = transactionRepository,
            categoryRepository = categoryRepository,
            settingsRepository = settingsRepository,
            authRepository = authRepository,
            syncRepository = syncRepository,
            preflightCoordinator = errCoordinator,
            migrationUploader = migrationUploader,
            ioDispatcher = testDispatcher,
            application = application
        )

        errViewModel.startMigrationPreflight(
            targetHouseholdId = "hh_throw",
            targetUserUid = "user_1",
            backupBundleDir = tempBackupDir
        )
        advanceUntilIdle()

        val state = errViewModel.migrationUiState.value
        assertTrue("Expected MigrationUiState.Failure but got $state", state is MigrationUiState.Failure)
        val failure = (state as MigrationUiState.Failure).failure

        assertFalse("Sanitized error must not contain stack trace markers", failure.sanitizedError.contains("\tat "))
        assertFalse("Sanitized error must not contain raw class references", failure.sanitizedError.contains("Stream.java"))
        assertFalse("Sanitized error must not contain Thread.java", failure.sanitizedError.contains("Thread.java"))
        assertTrue(failure.sanitizedError.contains("Household verification error") || failure.sanitizedError.contains("INTERNAL_CRASH"))
    }

    @Test
    fun testDismissMigrationDialogsResetsToIdle() = runTest(testDispatcher) {
        viewModel.startMigrationPreflight(
            targetHouseholdId = "",
            targetUserUid = "",
            backupBundleDir = tempBackupDir
        )
        advanceUntilIdle()

        assertTrue(viewModel.migrationUiState.value is MigrationUiState.Conflict)

        viewModel.dismissMigrationDialogs()
        assertEquals(MigrationUiState.Idle, viewModel.migrationUiState.value)
    }
    @Test
    fun testStartMigrationWithoutActiveHouseholdProducesConflict() = runTest(testDispatcher) {
        advanceUntilIdle()

        viewModel.startMigrationPreflight()

        advanceUntilIdle()

        val state = viewModel.migrationUiState.value

        assertTrue(
            "Expected MigrationUiState.Conflict but got $state",
             state is MigrationUiState.Conflict
        )

        val conflict = (state as MigrationUiState.Conflict).conflict

    assertEquals(
        "INSUFFICIENT_PERMISSIONS",
        conflict.reason
         )

    assertEquals(
        "Authentication required: You must be signed in with an active household to migrate data.",
        conflict.details
        )
}

    @Test
    fun testUpdateSearchQueryFiltersTransactionsInViewModel() = runTest(testDispatcher) {
        val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date())
        val tx1 = TransactionEntity(
            id = "tx_1",
            householdId = null,
            date = today,
            description = "Mega Image Groceries",
            amountRON = 100.0,
            amountEUR = 20.0,
            exchangeRate = 5.0,
            exchangeRateDate = today,
            exchangeRateSource = "BNR_OFFICIAL",
            conversionStatus = "OFFICIAL",
            type = "Expense",
            account = "Checking",
            category = "Food",
            subCategory = "Groceries"
        )
        val tx2 = TransactionEntity(
            id = "tx_2",
            householdId = null,
            date = today,
            description = "Electric Utility Bill",
            amountRON = 200.0,
            amountEUR = 40.0,
            exchangeRate = 5.0,
            exchangeRateDate = today,
            exchangeRateSource = "BNR_OFFICIAL",
            conversionStatus = "OFFICIAL",
            type = "Expense",
            account = "Checking",
            category = "Utilities",
            subCategory = "Power"
        )
        db.transactionDao().insertTransaction(tx1)
        db.transactionDao().insertTransaction(tx2)

        viewModel.updateSelectedPeriod("All Time")

        val collectJob = backgroundScope.launch {
            viewModel.filteredTransactions.collect {}
        }
        advanceUntilIdle()

        assertEquals(2, viewModel.filteredTransactions.value.size)

        viewModel.updateSearchQuery("groceries")
        advanceUntilIdle()

        assertEquals(1, viewModel.filteredTransactions.value.size)
        assertEquals("tx_1", viewModel.filteredTransactions.value[0].id)
        assertEquals("Mega Image Groceries", viewModel.filteredTransactions.value[0].description)

        viewModel.updateSearchQuery("")
        advanceUntilIdle()

        assertEquals(2, viewModel.filteredTransactions.value.size)
    }
}
