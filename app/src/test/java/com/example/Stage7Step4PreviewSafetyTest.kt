package com.example

import android.app.Application
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.db.FinTrackDatabase
import com.example.data.model.CategoryEntity
import com.example.data.model.ExchangeRateEntity
import com.example.data.model.HouseholdDto
import com.example.data.model.HouseholdInviteDto
import com.example.data.model.HouseholdMemberDto
import com.example.data.model.TransactionEntity
import com.example.data.repository.AuthRepository
import com.example.data.repository.AuthState
import com.example.data.repository.CategoryRepository
import com.example.data.repository.FirestoreMigrationPreflightCoordinator
import com.example.data.repository.FirestoreSnapshotSource
import com.example.data.repository.FirestoreSyncRepository
import com.example.data.repository.HouseholdRepository
import com.example.data.repository.RoomCategoryRepository
import com.example.data.repository.RoomTransactionRepository
import com.example.data.repository.TransactionRepository
import com.example.data.service.ExchangeRateService
import com.example.ui.MainViewModel
import com.example.ui.MigrationPreviewState
import com.example.ui.MigrationUiState
import com.example.ui.components.MigrationPreviewDialog
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

class FakeHouseholdRepoForPreview : HouseholdRepository {
    val householdFlow = MutableStateFlow<HouseholdDto?>(null)
    override suspend fun createHousehold(name: String): Result<HouseholdDto> = Result.success(HouseholdDto("hh_1", name))
    override fun observeHousehold(householdId: String): Flow<HouseholdDto?> = householdFlow
    override fun observeHouseholdMembers(householdId: String): Flow<List<HouseholdMemberDto>> = flowOf(emptyList())
    override suspend fun sendInvite(householdId: String, householdName: String, inviteeEmail: String): Result<HouseholdInviteDto> =
        Result.success(
            HouseholdInviteDto(
                inviteId = "inv_1",
                householdId = householdId,
                householdName = householdName,
                inviteeEmail = inviteeEmail,
                inviterUid = "user_test",
                status = "PENDING",
                createdAt = System.currentTimeMillis()
            )
        )
    override fun observeIncomingInvites(userEmail: String) = flowOf(emptyList<com.example.data.model.HouseholdInviteDto>())
    override suspend fun acceptInvite(inviteId: String): Result<Unit> = Result.success(Unit)
    override suspend fun declineInvite(inviteId: String): Result<Unit> = Result.success(Unit)
}

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class Stage7Step4PreviewSafetyTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var app: Application
    private lateinit var db: FinTrackDatabase
    private lateinit var fakeSnapshotSource: FakeSnapshotSource
    private lateinit var fakeAuthRepo: FakeAuthRepository
    private lateinit var fakeHouseholdRepo: FakeHouseholdRepoForPreview
    private lateinit var syncRepo: FirestoreSyncRepository
    private lateinit var preflightCoordinator: FirestoreMigrationPreflightCoordinator
    private lateinit var viewModel: MainViewModel
    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setup() {
        app = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(app, FinTrackDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        fakeSnapshotSource = FakeSnapshotSource()
        fakeAuthRepo = FakeAuthRepository(AuthState.SignedIn("user_test", "user@test.com", "Test User"))
        fakeHouseholdRepo = FakeHouseholdRepoForPreview()
        syncRepo = FirestoreSyncRepository(db, fakeSnapshotSource)
        preflightCoordinator = FirestoreMigrationPreflightCoordinator(db, fakeSnapshotSource)

        val rateService = ExchangeRateService(db.exchangeRateDao())
        val txRepo = RoomTransactionRepository(db.transactionDao(), rateService, db.exchangeRateDao(), db)
        val catRepo = RoomCategoryRepository(db.categoryDao(), db.syncOutboxDao(), db)
        val settingsRepo = FakeSettingsRepository()

        viewModel = MainViewModel(
            application = app,
            transactionRepository = txRepo,
            categoryRepository = catRepo,
            settingsRepository = settingsRepo,
            authRepository = fakeAuthRepo,
            householdRepository = fakeHouseholdRepo,
            syncRepository = syncRepo,
            preflightCoordinator = preflightCoordinator,
            database = db,
            ioDispatcher = testDispatcher
        )
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun testPreflightCapturesHumanReadableHouseholdNameAndBackupMetadata() = runTest(testDispatcher) {
        fakeSnapshotSource.setMember("hh_family", "user_test", "OWNER", "ACTIVE")
        fakeHouseholdRepo.householdFlow.value = HouseholdDto(householdId = "hh_family", name = "Our Happy Home")

        db.categoryDao().insertCategory(CategoryEntity(id = "c1", name = "Groceries", type = "Expense"))
        db.exchangeRateDao().insertRate(ExchangeRateEntity(date = "2026-08-14", rate = 4.97, source = "BNR_OFFICIAL", status = "OFFICIAL"))
        db.transactionDao().insertTransaction(
            TransactionEntity(
                id = "tx1",
                amountRON = 150.0,
                amountEUR = 30.0,
                exchangeRate = 5.0,
                exchangeRateDate = "2026-08-14",
                description = "Market",
                type = "Expense",
                category = "Groceries",
                subCategory = "",
                account = "Card",
                date = "2026-08-14"
            )
        )

        viewModel.startMigrationPreflight(targetHouseholdId = "hh_family", targetUserUid = "user_test")

        val state = viewModel.migrationUiState.value
        assertTrue("State must be Preview", state is MigrationUiState.Preview)
        val preview = (state as MigrationUiState.Preview).preview

        assertEquals("hh_family", preview.householdId)
        assertEquals("Our Happy Home", preview.householdName)
        assertEquals("OWNER", preview.userRole)
        assertEquals(1, preview.transactionsCount)
        assertEquals(1, preview.categoriesCount)
        assertEquals(1, preview.exchangeRatesCount)
        assertEquals(3, preview.totalRecords)
        assertNotNull(preview.backupBundlePath)
        assertNotNull(preview.backupTimestamp)
        assertEquals("VALIDATED", preview.backupValidationStatus)
    }

    @Test
    fun testProceedButtonIsDisabledUntilCheckboxIsChecked() {
        val previewState = MigrationPreviewState(
            householdId = "hh_123",
            householdName = "Miller Household",
            userUid = "user_owner",
            userRole = "OWNER",
            transactionsCount = 10,
            categoriesCount = 5,
            exchangeRatesCount = 2,
            totalRecords = 17,
            backupBundlePath = "/data/user/0/app/files/migration_backups/backup_123",
            backupTimestamp = 1755678900000L,
            backupValidationStatus = "VALIDATED"
        )

        var confirmed = false
        var cancelled = false

        composeTestRule.setContent {
            MigrationPreviewDialog(
                previewState = previewState,
                onConfirm = { confirmed = true },
                onCancel = { cancelled = true }
            )
        }

        // Verify dialog components displayed
        composeTestRule.onNodeWithTag("migration_preview_dialog").assertIsDisplayed()
        composeTestRule.onNodeWithTag("preview_household_name").assertIsDisplayed()
        composeTestRule.onNodeWithText("Miller Household").assertIsDisplayed()
        composeTestRule.onNodeWithText("hh_123").assertIsDisplayed()
        composeTestRule.onNodeWithTag("preview_backup_timestamp").assertIsDisplayed()

        // Proceed button MUST BE DISABLED initially
        composeTestRule.onNodeWithTag("migration_preview_confirm_button").assertIsNotEnabled()

        // Check the acknowledgment checkbox
        composeTestRule.onNodeWithTag("migration_acknowledgment_checkbox").performClick()

        // Proceed button MUST BE ENABLED now
        composeTestRule.onNodeWithTag("migration_preview_confirm_button").assertIsEnabled()

        // Click proceed
        composeTestRule.onNodeWithTag("migration_preview_confirm_button").performClick()
        assertTrue("Confirm action must be called", confirmed)
    }

    @Test
    fun testReopeningPreviewDialogResetsAcknowledgment() {
        val previewState = MigrationPreviewState(
            householdId = "hh_456",
            householdName = "Johnson Residence",
            userUid = "user_admin",
            userRole = "ADMIN",
            transactionsCount = 2,
            categoriesCount = 2,
            exchangeRatesCount = 0,
            totalRecords = 4,
            backupBundlePath = "/path/backup",
            backupTimestamp = System.currentTimeMillis()
        )

        // First presentation
        composeTestRule.setContent {
            MigrationPreviewDialog(
                previewState = previewState,
                onConfirm = {},
                onCancel = {}
            )
        }

        // Check it
        composeTestRule.onNodeWithTag("migration_acknowledgment_checkbox").performClick()
        composeTestRule.onNodeWithTag("migration_preview_confirm_button").assertIsEnabled()

        // Remount dialog (simulate reopening)
        composeTestRule.setContent {
            MigrationPreviewDialog(
                previewState = previewState,
                onConfirm = {},
                onCancel = {}
            )
        }

        // Checkbox must be reset to unchecked and proceed disabled
        composeTestRule.onNodeWithTag("migration_preview_confirm_button").assertIsNotEnabled()
    }
}
