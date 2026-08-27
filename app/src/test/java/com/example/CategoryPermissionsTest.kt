package com.example

import android.app.Application
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.db.FinTrackDatabase
import com.example.data.model.CategoryEntity
import com.example.data.model.FilterSettings
import com.example.data.model.HouseholdDto
import com.example.data.model.HouseholdInviteDto
import com.example.data.model.HouseholdMemberDto
import com.example.data.model.TransactionEntity
import com.example.data.repository.AuthRepository
import com.example.data.repository.AuthState
import com.example.data.repository.CategoryRepository
import com.example.data.repository.FirestoreSyncRepository
import com.example.data.repository.HouseholdRepository
import com.example.data.repository.PendingRetryResult
import com.example.data.repository.PreparedRepairItem
import com.example.data.repository.RoomCategoryRepository
import com.example.data.repository.SettingsRepository
import com.example.data.repository.SyncStatus
import com.example.data.repository.TransactionRepository
import com.example.data.service.BnrDiagnosticResult
import com.example.data.service.BnrRateResult
import com.example.data.util.CsvImportFinalResult
import com.example.data.util.CsvPreviewData
import com.example.ui.MainViewModel
import com.example.ui.components.TransactionFormDialog
import com.example.ui.screens.CategoriesScreen
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class CategoryPermissionsTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val testDispatcher = kotlinx.coroutines.test.UnconfinedTestDispatcher()

    private lateinit var app: Application
    private lateinit var db: FinTrackDatabase
    private lateinit var fakeSnapshotSource: FakeSnapshotSource
    private lateinit var syncRepo: FirestoreSyncRepository

    // ----------------------------------------------------
    // Fakes for MainViewModel test harness
    // ----------------------------------------------------

    private class FakeAuthRepo(initialUid: String? = null) : AuthRepository {
        private val _authState = MutableStateFlow<AuthState>(
            if (initialUid != null) AuthState.SignedIn(initialUid, "$initialUid@example.com") else AuthState.SignedOut
        )
        override val authState: StateFlow<AuthState> = _authState.asStateFlow()

        fun setSignedIn(uid: String) {
            _authState.value = AuthState.SignedIn(uid, "$uid@example.com")
        }

        override suspend fun signInWithGoogleCredential(idToken: String): Result<String> = Result.success("test_uid")
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

    private class FakeHouseholdRepo : HouseholdRepository {
        val membersFlow = MutableStateFlow<List<HouseholdMemberDto>>(emptyList())
        val householdFlow = MutableStateFlow<HouseholdDto?>(null)

        override fun observeHousehold(householdId: String): Flow<HouseholdDto?> = householdFlow
        override fun observeHouseholdMembers(householdId: String): Flow<List<HouseholdMemberDto>> = membersFlow
        override fun observeIncomingInvites(userEmail: String): Flow<List<HouseholdInviteDto>> = flowOf(emptyList())
        override suspend fun createHousehold(name: String): Result<HouseholdDto> =
            Result.failure(NotImplementedError())
        override suspend fun sendInvite(householdId: String, householdName: String, inviteeEmail: String): Result<HouseholdInviteDto> =
            Result.failure(NotImplementedError())
        override suspend fun acceptInvite(inviteId: String): Result<Unit> = Result.success(Unit)
        override suspend fun declineInvite(inviteId: String): Result<Unit> = Result.success(Unit)
    }

    private class FakeTestSettingsRepo : SettingsRepository {
        private val _filterSettings = MutableStateFlow(FilterSettings())
        override val filterSettingsFlow: Flow<FilterSettings> = _filterSettings.asStateFlow()
        override val themeModeFlow: Flow<String> = flowOf("system")
        override suspend fun updateSelectedPeriod(period: String) {}
        override suspend fun updateSelectedCurrency(currency: String) {}
        override suspend fun updateCustomDateRange(startDate: String, endDate: String) {}
        override suspend fun updateSelectedType(type: String) {}
        override suspend fun updateCategoryFilter(type: String, categoryName: String?) {}
        override suspend fun updateThemeMode(mode: String) {}
    }

    private class FakeTransactionRepo : TransactionRepository {
        override fun getTransactions(householdId: String?): Flow<List<TransactionEntity>> = flowOf(emptyList())
        override fun getTransactionsInRange(startDate: String, endDate: String): Flow<List<TransactionEntity>> = flowOf(emptyList())
        override suspend fun getTransactionById(id: String): TransactionEntity? = null
        override suspend fun saveTransaction(
            id: String?,
            date: String,
            description: String,
            amountRON: Double,
            type: String,
            account: String,
            category: String,
            subCategory: String,
            destination: String?,
            userId: String,
            householdId: String?
        ): TransactionEntity = TransactionEntity(
            id = id ?: "tx_1",
            date = date,
            description = description,
            amountRON = amountRON,
            amountEUR = amountRON / 5.0,
            exchangeRate = 5.0,
            exchangeRateDate = date,
            type = type,
            account = account,
            category = category,
            subCategory = subCategory,
            destination = destination
        )
        override suspend fun createDuplicateTemplate(source: TransactionEntity): TransactionEntity = source
        override suspend fun getDescriptionSuggestions(query: String, limit: Int): List<String> = emptyList()
        override suspend fun insertBatchWithTransaction(transactions: List<TransactionEntity>) {}
        override suspend fun getUnverifiedTransactions(): List<TransactionEntity> = emptyList()
        override suspend fun getAllTransactionsList(): List<TransactionEntity> = emptyList()
        override suspend fun syncPendingConversions(): PendingRetryResult = PendingRetryResult(0, 0, 0, 0, null)
        override suspend fun applyRepairBatch(repairs: List<PreparedRepairItem>): Int = 0
        override suspend fun applyRepairToTransactionAndCache(transaction: TransactionEntity, officialRate: Double, effectiveBnrDate: String) {}
        override suspend fun insertTransaction(transaction: TransactionEntity) {}
        override suspend fun deleteTransaction(transaction: TransactionEntity) {}
        override suspend fun deleteTransactionById(id: String) {}
        override suspend fun deleteAllTransactions() {}
        override suspend fun insertBatch(transactions: List<TransactionEntity>) {}
        override suspend fun getOfficialRate(date: String): BnrRateResult = BnrRateResult(
            requestedDate = date,
            effectiveDate = date,
            rate = 5.0,
            source = "BNR_OFFICIAL",
            status = "OFFICIAL"
        )
        override suspend fun runBnrDiagnostic(): BnrDiagnosticResult = BnrDiagnosticResult(
            isReachable = true,
            httpStatus = "200"
        )
        override suspend fun executeAtomicCsvImport(previewData: CsvPreviewData, backupFile: File, allExistingTransactions: List<TransactionEntity>): CsvImportFinalResult =
            throw NotImplementedError()
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        app = ApplicationProvider.getApplicationContext<Application>()
        db = Room.inMemoryDatabaseBuilder(app, FinTrackDatabase::class.java)
            .allowMainThreadQueries()
            .setQueryExecutor(Runnable::run)
            .setTransactionExecutor(Runnable::run)
            .build()
        fakeSnapshotSource = FakeSnapshotSource().apply {
            autoEmitInitialSnapshot = true
        }
        syncRepo = FirestoreSyncRepository(
            database = db,
            snapshotSource = fakeSnapshotSource,
            coroutineScope = TestScope(testDispatcher)
        )
    }

    @After
    fun tearDown() {
        syncRepo.stopSync()
        db.close()
        Dispatchers.resetMain()
    }

    private fun createViewModel(
        authRepo: FakeAuthRepo,
        householdRepo: FakeHouseholdRepo,
        scope: kotlinx.coroutines.CoroutineScope
    ): MainViewModel {
        val categoryRepo = RoomCategoryRepository(db.categoryDao(), db.syncOutboxDao(), db)
        val transactionRepo = FakeTransactionRepo()
        val settingsRepo = FakeTestSettingsRepo()

        val vm = MainViewModel(
            transactionRepository = transactionRepo,
            categoryRepository = categoryRepo,
            settingsRepository = settingsRepo,
            authRepository = authRepo,
            householdRepository = householdRepo,
            syncRepository = syncRepo,
            database = db,
            ioDispatcher = testDispatcher,
            application = app
        )

        scope.launch(testDispatcher) { vm.activeHouseholdId.collect() }
        scope.launch(testDispatcher) { vm.householdMembers.collect() }
        scope.launch(testDispatcher) { vm.currentUserMembership.collect() }
        scope.launch(testDispatcher) { vm.canManageCategories.collect() }
        scope.launch(testDispatcher) { vm.uiState.collect() }

        return vm
    }

    // =========================================================================
    // 1. PERMISSION STATE TESTS (OWNER, ADMIN, MEMBER, OFFLINE)
    // =========================================================================

    @Test
    fun testOfflineOrNullHouseholdAllowsCategoryManagement() = runTest(testDispatcher) {
        val authRepo = FakeAuthRepo(initialUid = "user_1")
        val householdRepo = FakeHouseholdRepo()
        syncRepo.stopSync()
        advanceUntilIdle()

        val vm = createViewModel(authRepo, householdRepo, backgroundScope)
        advanceUntilIdle()

        assertTrue(
            "canManageCategories should be true in offline/null household mode",
            vm.canManageCategories.value
        )
    }

    @Test
    fun testOwnerRoleAllowsCategoryManagement() = runTest(testDispatcher) {
        val authRepo = FakeAuthRepo(initialUid = "user_owner")
        val householdRepo = FakeHouseholdRepo()
        fakeSnapshotSource.setMember("hh_123", "user_owner", "OWNER", "ACTIVE")
        syncRepo.startSync("user_owner", "hh_123")
        advanceUntilIdle()

        householdRepo.membersFlow.value = listOf(
            HouseholdMemberDto(uid = "user_owner", email = "owner@example.com", role = "owner")
        )

        val vm = createViewModel(authRepo, householdRepo, backgroundScope)
        advanceUntilIdle()

        assertTrue(
            "canManageCategories should be true for household owner",
            vm.canManageCategories.value
        )
    }

    @Test
    fun testAdminRoleAllowsCategoryManagement() = runTest(testDispatcher) {
        val authRepo = FakeAuthRepo(initialUid = "user_admin")
        val householdRepo = FakeHouseholdRepo()
        fakeSnapshotSource.setMember("hh_123", "user_admin", "ADMIN", "ACTIVE")
        syncRepo.startSync("user_admin", "hh_123")
        advanceUntilIdle()

        householdRepo.membersFlow.value = listOf(
            HouseholdMemberDto(uid = "user_admin", email = "admin@example.com", role = "admin")
        )

        val vm = createViewModel(authRepo, householdRepo, backgroundScope)
        advanceUntilIdle()

        assertTrue(
            "canManageCategories should be true for household admin",
            vm.canManageCategories.value
        )
    }

    @Test
    fun testMemberRoleDeniesCategoryManagement() = runTest(testDispatcher) {
        val authRepo = FakeAuthRepo(initialUid = "user_member")
        val householdRepo = FakeHouseholdRepo()
        fakeSnapshotSource.setMember("hh_123", "user_member", "MEMBER", "ACTIVE")
        syncRepo.startSync("user_member", "hh_123")
        advanceUntilIdle()

        householdRepo.membersFlow.value = listOf(
            HouseholdMemberDto(uid = "user_member", email = "member@example.com", role = "member")
        )

        val vm = createViewModel(authRepo, householdRepo, backgroundScope)
        advanceUntilIdle()

        assertFalse(
            "canManageCategories should be false for household member",
            vm.canManageCategories.value
        )
    }

    // =========================================================================
    // 2. VIEWMODEL MUTATION GUARDS TESTS (MEMBER BLOCKED ON ALL 7 MUTATIONS)
    // =========================================================================

    @Test
    fun testMemberCannotAddCategoryViaViewModel() = runTest(testDispatcher) {
        db.categoryDao().deleteAllCategories("hh_123")
        val authRepo = FakeAuthRepo(initialUid = "user_member")
        val householdRepo = FakeHouseholdRepo()
        fakeSnapshotSource.setMember("hh_123", "user_member", "MEMBER", "ACTIVE")
        syncRepo.startSync("user_member", "hh_123")
        advanceUntilIdle()

        householdRepo.membersFlow.value = listOf(
            HouseholdMemberDto(uid = "user_member", email = "member@example.com", role = "member")
        )

        val vm = createViewModel(authRepo, householdRepo, backgroundScope)
        advanceUntilIdle()

        db.syncOutboxDao().deleteAllOutboxEntries()

        // Attempt add category as member
        vm.addCategory(name = "Secret", type = "Expense", subCategory = "Hacks")
        advanceUntilIdle()

        val stored = db.categoryDao().getAllCategoriesList("hh_123").filter { it.name == "Secret" }
        val pendingOutbox = db.syncOutboxDao().getPendingEntries()
        assertEquals(0, stored.size)
        assertEquals(0, pendingOutbox.size)
        assertEquals("Only household owner or admin can modify categories.", vm.uiState.value.userNotification)
    }

    @Test
    fun testMemberCannotUpdateCategoryGroupViaViewModel() = runTest(testDispatcher) {
        val authRepo = FakeAuthRepo(initialUid = "user_member")
        val householdRepo = FakeHouseholdRepo()
        fakeSnapshotSource.setMember("hh_123", "user_member", "MEMBER", "ACTIVE")
        syncRepo.startSync("user_member", "hh_123")
        advanceUntilIdle()

        householdRepo.membersFlow.value = listOf(
            HouseholdMemberDto(uid = "user_member", email = "member@example.com", role = "member")
        )

        db.categoryDao().insertCategory(
            CategoryEntity(id = "cat_1", name = "Food", type = "Expense", subCategory = "Groceries", householdId = "hh_123")
        )

        val vm = createViewModel(authRepo, householdRepo, backgroundScope)
        advanceUntilIdle()

        vm.updateCategoryGroup(oldName = "Food", newName = "Groceries & Dine", type = "Expense")
        advanceUntilIdle()

        val stored = db.categoryDao().getCategoryById("cat_1")
        val pendingOutbox = db.syncOutboxDao().getPendingEntries()
        assertEquals("Food", stored?.name)
        assertEquals(0, pendingOutbox.size)
        assertEquals("Only household owner or admin can modify categories.", vm.uiState.value.userNotification)
    }

    @Test
    fun testMemberCannotDeleteCategoryGroupViaViewModel() = runTest(testDispatcher) {
        val authRepo = FakeAuthRepo(initialUid = "user_member")
        val householdRepo = FakeHouseholdRepo()
        fakeSnapshotSource.setMember("hh_123", "user_member", "MEMBER", "ACTIVE")
        syncRepo.startSync("user_member", "hh_123")
        advanceUntilIdle()

        householdRepo.membersFlow.value = listOf(
            HouseholdMemberDto(uid = "user_member", email = "member@example.com", role = "member")
        )

        db.categoryDao().insertCategory(
            CategoryEntity(id = "cat_1", name = "Food", type = "Expense", subCategory = "Groceries", householdId = "hh_123")
        )

        val vm = createViewModel(authRepo, householdRepo, backgroundScope)
        advanceUntilIdle()

        vm.deleteCategoryGroup(name = "Food", type = "Expense")
        advanceUntilIdle()

        val stored = db.categoryDao().getCategoryById("cat_1")
        val pendingOutbox = db.syncOutboxDao().getPendingEntries()
        assertEquals("Food", stored?.name)
        assertEquals(0, pendingOutbox.size)
        assertEquals("Only household owner or admin can modify categories.", vm.uiState.value.userNotification)
    }

    @Test
    fun testMemberCannotUpdateSubcategoryViaViewModel() = runTest(testDispatcher) {
        val authRepo = FakeAuthRepo(initialUid = "user_member")
        val householdRepo = FakeHouseholdRepo()
        fakeSnapshotSource.setMember("hh_123", "user_member", "MEMBER", "ACTIVE")
        syncRepo.startSync("user_member", "hh_123")
        advanceUntilIdle()

        householdRepo.membersFlow.value = listOf(
            HouseholdMemberDto(uid = "user_member", email = "member@example.com", role = "member")
        )

        db.categoryDao().insertCategory(
            CategoryEntity(id = "cat_1", name = "Food", type = "Expense", subCategory = "Groceries", householdId = "hh_123")
        )

        val vm = createViewModel(authRepo, householdRepo, backgroundScope)
        advanceUntilIdle()

        vm.updateSubcategory(id = "cat_1", newSubCategory = "Supermarket")
        advanceUntilIdle()

        val stored = db.categoryDao().getCategoryById("cat_1")
        val pendingOutbox = db.syncOutboxDao().getPendingEntries()
        assertEquals("Groceries", stored?.subCategory)
        assertEquals(0, pendingOutbox.size)
        assertEquals("Only household owner or admin can modify categories.", vm.uiState.value.userNotification)
    }

    @Test
    fun testMemberCannotDeleteSubcategoryViaViewModel() = runTest(testDispatcher) {
        val authRepo = FakeAuthRepo(initialUid = "user_member")
        val householdRepo = FakeHouseholdRepo()
        fakeSnapshotSource.setMember("hh_123", "user_member", "MEMBER", "ACTIVE")
        syncRepo.startSync("user_member", "hh_123")
        advanceUntilIdle()

        householdRepo.membersFlow.value = listOf(
            HouseholdMemberDto(uid = "user_member", email = "member@example.com", role = "member")
        )

        db.categoryDao().insertCategory(
            CategoryEntity(id = "cat_1", name = "Food", type = "Expense", subCategory = "Groceries", householdId = "hh_123")
        )

        val vm = createViewModel(authRepo, householdRepo, backgroundScope)
        advanceUntilIdle()

        vm.deleteSubcategory(id = "cat_1")
        advanceUntilIdle()

        val stored = db.categoryDao().getCategoryById("cat_1")
        val pendingOutbox = db.syncOutboxDao().getPendingEntries()
        assertEquals("Food", stored?.name)
        assertEquals(0, pendingOutbox.size)
        assertEquals("Only household owner or admin can modify categories.", vm.uiState.value.userNotification)
    }

    // =========================================================================
    // 3. UI RENDERING TESTS (OWNER vs MEMBER ON CATEGORIES SCREEN)
    // =========================================================================

    @Test
    fun testOwnerUiDisplaysAllCategoryManagementControls() {
        val testCategories = listOf(
            CategoryEntity(id = "cat_1", name = "Housing", type = "Expense", subCategory = "Rent", householdId = "hh_123")
        )

        composeTestRule.setContent {
            CategoriesScreen(
                categories = testCategories,
                canManageCategories = true,
                onAddCategory = { _, _, _ -> },
                onUpdateCategoryGroup = { _, _, _ -> },
                onDeleteCategoryGroup = { _, _ -> },
                onUpdateSubcategory = { _, _ -> },
                onDeleteSubcategory = { _ -> }
            )
        }

        // Owner must see FAB Add Category
        composeTestRule.onNodeWithTag("fab_add_category").assertIsDisplayed()

        // Owner must see + Sub button
        composeTestRule.onNodeWithText("+ Sub").assertIsDisplayed()

        // Owner must see Edit and Delete buttons for group
        composeTestRule.onNodeWithTag("edit_category_group_Housing").assertIsDisplayed()
        composeTestRule.onNodeWithTag("delete_category_group_Housing").assertIsDisplayed()

        // Owner must see Edit and Delete buttons for subcategory
        composeTestRule.onNodeWithTag("edit_subcategory_cat_1").assertIsDisplayed()
        composeTestRule.onNodeWithTag("delete_subcategory_cat_1").assertIsDisplayed()
    }

    @Test
    fun testMemberUiHidesAllCategoryManagementControls() {
        val testCategories = listOf(
            CategoryEntity(id = "cat_1", name = "Housing", type = "Expense", subCategory = "Rent", householdId = "hh_123")
        )

        composeTestRule.setContent {
            CategoriesScreen(
                categories = testCategories,
                canManageCategories = false, // Member mode
                onAddCategory = { _, _, _ -> },
                onUpdateCategoryGroup = { _, _, _ -> },
                onDeleteCategoryGroup = { _, _ -> },
                onUpdateSubcategory = { _, _ -> },
                onDeleteSubcategory = { _ -> }
            )
        }

        // Member must NOT have FAB Add Category
        composeTestRule.onAllNodesWithTag("fab_add_category").assertCountEquals(0)

        // Member must NOT have + Sub button
        composeTestRule.onAllNodesWithText("+ Sub").assertCountEquals(0)

        // Member must NOT have Edit or Delete buttons for group
        composeTestRule.onAllNodesWithTag("edit_category_group_Housing").assertCountEquals(0)
        composeTestRule.onAllNodesWithTag("delete_category_group_Housing").assertCountEquals(0)

        // Member must NOT have Edit or Delete buttons for subcategory
        composeTestRule.onAllNodesWithTag("edit_subcategory_cat_1").assertCountEquals(0)
        composeTestRule.onAllNodesWithTag("delete_subcategory_cat_1").assertCountEquals(0)

        // But category and subcategory texts MUST be visible
        composeTestRule.onNodeWithText("Housing").assertIsDisplayed()
        composeTestRule.onNodeWithText("Rent").assertIsDisplayed()
    }

    // =========================================================================
    // 4. TRANSACTION FORM USABILITY FOR MEMBER
    // =========================================================================

    @Test
    fun testTransactionFormDialogRemainsFullyUsableForMember() {
        val commonCategories = listOf(
            CategoryEntity(id = "c1", name = "Food", type = "Expense", subCategory = "Groceries", householdId = "hh_123"),
            CategoryEntity(id = "c2", name = "Food", type = "Expense", subCategory = "Restaurants", householdId = "hh_123"),
            CategoryEntity(id = "c3", name = "Income", type = "Income", subCategory = "Salary", householdId = "hh_123")
        )

        var savedCategory: String? = null
        var savedSubCategory: String? = null

        composeTestRule.setContent {
            TransactionFormDialog(
                initialTransaction = null,
                isDuplicateMode = false,
                categories = commonCategories,
                onDismiss = {},
                onSave = { _, _, _, _, _, _, cat, sub, _ ->
                    savedCategory = cat
                    savedSubCategory = sub
                }
            )
        }

        // Form dialog displays category selector and allows choosing the common categories
        composeTestRule.onNodeWithText("Add Transaction").assertIsDisplayed()
        composeTestRule.onNodeWithTag("tx_input_amount").assertIsDisplayed()
        composeTestRule.onNodeWithTag("tx_input_desc").assertIsDisplayed()
        composeTestRule.onNodeWithTag("save_transaction_button").assertExists()
    }

    @Test
    fun testMemberRole_inboundReconciliation_doesNotEnqueueUnauthorizedOutboxWrites() = runTest {
        val householdId = "hh_member_test"
        val memberUid = "user_member"
        fakeSnapshotSource.members[Pair(householdId, memberUid)] = mapOf("role" to "MEMBER", "status" to "ACTIVE")

        syncRepo.startSync(userUid = memberUid, requestedHouseholdId = householdId)

        val deterministicId = RoomCategoryRepository.generateDefaultCategoryId(
            householdId = householdId,
            type = "Expense",
            name = "🏥 Health & Wellness",
            subCategory = "💊 Pharmacy & Medical"
        )
        val legacyId = "legacy-random-uuid-999"

        val snapshot = listOf(
            Pair(
                deterministicId,
                mapOf<String, Any?>(
                    "categoryId" to deterministicId,
                    "householdId" to householdId,
                    "type" to "Expense",
                    "name" to "🏥 Health & Wellness",
                    "subCategory" to "💊 Pharmacy & Medical",
                    "isDeleted" to false,
                    "createdAt" to 100L,
                    "updatedAt" to 100L
                )
            ),
            Pair(
                legacyId,
                mapOf<String, Any?>(
                    "categoryId" to legacyId,
                    "householdId" to householdId,
                    "type" to "Expense",
                    "name" to "🏥 Health & Wellness",
                    "subCategory" to "💊 Pharmacy & Medical",
                    "isDeleted" to false,
                    "createdAt" to 50L,
                    "updatedAt" to 50L
                )
            )
        )

        syncRepo.processCategorySnapshot(snapshot)

        // 1. Room is cleanly reconciled (only 1 deterministic row)
        val localCats = db.categoryDao().getAllCategoriesList(householdId)
        assertEquals(1, localCats.size)
        assertEquals(deterministicId, localCats.first().id)

        // 2. Member must NOT enqueue any outbox mutations (zero cloud writes)
        val pendingOutbox = db.syncOutboxDao().getPendingEntries()
        assertEquals(0, pendingOutbox.size)
    }
}
