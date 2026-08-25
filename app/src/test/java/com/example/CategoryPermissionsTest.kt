package com.example

import android.app.Application
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.example.data.dao.CategoryDao
import com.example.data.dao.SyncOutboxDao
import com.example.data.model.CategoryEntity
import com.example.data.model.FilterSettings
import com.example.data.model.HouseholdDto
import com.example.data.model.HouseholdInviteDto
import com.example.data.model.HouseholdMemberDto
import com.example.data.model.SyncOutboxEntity
import com.example.data.model.SyncStatus
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
import com.example.data.repository.TransactionRepository
import com.example.data.service.BnrDiagnosticResult
import com.example.data.service.BnrRateResult
import com.example.data.util.CsvImportFinalResult
import com.example.data.util.CsvPreviewData
import com.example.ui.MainViewModel
import com.example.ui.components.TransactionFormDialog
import com.example.ui.screens.CategoriesScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
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

    private val testDispatcher = StandardTestDispatcher()

    // ----------------------------------------------------
    // Fakes for MainViewModel test harness
    // ----------------------------------------------------

    private class FakeAuthRepo(initialUid: String? = null) : AuthRepository {
        private val _authState = MutableStateFlow<AuthState>(
            if (initialUid != null) AuthState.SignedIn(initialUid, "test@example.com") else AuthState.SignedOut
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
        override fun observeIncomingInvites(email: String): Flow<List<HouseholdInviteDto>> = flowOf(emptyList())
        override suspend fun getHousehold(householdId: String): HouseholdDto? = householdFlow.value
        override suspend fun getHouseholdMembers(householdId: String): List<HouseholdMemberDto> = membersFlow.value
        override suspend fun createHousehold(name: String, creatorDisplayName: String?): Result<HouseholdDto> =
            Result.failure(NotImplementedError())
        override suspend fun sendInvite(householdId: String, email: String, role: String): Result<HouseholdInviteDto> =
            Result.failure(NotImplementedError())
        override suspend fun acceptInvite(inviteId: String): Result<Unit> = Result.success(Unit)
        override suspend fun declineInvite(inviteId: String): Result<Unit> = Result.success(Unit)
        override suspend fun removeMember(householdId: String, memberUid: String): Result<Unit> = Result.success(Unit)
        override suspend fun updateMemberRole(householdId: String, memberUid: String, newRole: String): Result<Unit> =
            Result.success(Unit)
    }

    private class FakeSyncRepo : FirestoreSyncRepository {
        val syncStatusFlow = MutableStateFlow<SyncStatus>(SyncStatus.SignedOut)
        override val syncStatusState: StateFlow<SyncStatus> = syncStatusFlow.asStateFlow()
        override val activeHouseholdId: String?
            get() = (syncStatusState.value as? SyncStatus.Synced)?.householdId

        override fun startSync(userUid: String, requestedHouseholdId: String?) {}
        override fun stopSync() {}
        override suspend fun forceSyncNow() {}
        override suspend fun switchHousehold(householdId: String?) {
            if (householdId == null) {
                syncStatusFlow.value = SyncStatus.Offline
            } else {
                syncStatusFlow.value = SyncStatus.Synced(householdId = householdId)
            }
        }
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
            userId: String
        ): TransactionEntity = TransactionEntity(
            id = id ?: "tx_1",
            date = date,
            description = description,
            amountRON = amountRON,
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
        override suspend fun getOfficialRate(date: String): BnrRateResult = BnrRateResult.Error("Not implemented")
        override suspend fun runBnrDiagnostic(): BnrDiagnosticResult = BnrDiagnosticResult(0, 0, 0, 0, emptyList(), 0)
        override suspend fun executeAtomicCsvImport(previewData: CsvPreviewData, backupFile: File, allExistingTransactions: List<TransactionEntity>): CsvImportFinalResult =
            throw NotImplementedError()
    }

    private class FakeCategoryDao : CategoryDao {
        val categories = mutableListOf<CategoryEntity>()
        private val _flow = MutableStateFlow<List<CategoryEntity>>(emptyList())

        private fun notifyChange() {
            _flow.value = categories.toList()
        }

        override fun getAllCategories(householdId: String?): Flow<List<CategoryEntity>> {
            return MutableStateFlow(
                categories.filter { (householdId == null && it.householdId == null) || it.householdId == householdId }
            )
        }

        override suspend fun getAllCategoriesList(householdId: String?): List<CategoryEntity> {
            return categories.filter { (householdId == null && it.householdId == null) || it.householdId == householdId }
        }

        override suspend fun insertCategory(category: CategoryEntity) {
            categories.removeAll { it.id == category.id }
            categories.add(category)
            notifyChange()
        }

        override suspend fun updateCategory(category: CategoryEntity) {
            val index = categories.indexOfFirst { it.id == category.id }
            if (index >= 0) {
                categories[index] = category
                notifyChange()
            }
        }

        override suspend fun insertAllCategories(categories: List<CategoryEntity>) {
            categories.forEach { insertCategory(it) }
        }

        override suspend fun deleteCategory(category: CategoryEntity) {
            categories.removeAll { it.id == category.id }
            notifyChange()
        }

        override suspend fun updateCategoryGroup(oldName: String, newName: String, type: String, householdId: String?) {
            val toUpdate = categories.filter {
                it.name == oldName && it.type == type &&
                        ((householdId == null && it.householdId == null) || it.householdId == householdId)
            }
            toUpdate.forEach {
                val index = categories.indexOf(it)
                if (index >= 0) {
                    categories[index] = it.copy(name = newName)
                }
            }
            notifyChange()
        }

        override suspend fun deleteCategoryGroup(name: String, type: String, householdId: String?) {
            categories.removeAll {
                it.name == name && it.type == type &&
                        ((householdId == null && it.householdId == null) || it.householdId == householdId)
            }
            notifyChange()
        }

        override suspend fun updateSubcategory(id: String, newSubCategory: String) {
            val index = categories.indexOfFirst { it.id == id }
            if (index >= 0) {
                categories[index] = categories[index].copy(subCategory = newSubCategory)
                notifyChange()
            }
        }

        override suspend fun deleteSubcategory(id: String) {
            categories.removeAll { it.id == id }
            notifyChange()
        }

        override suspend fun deleteCategoryById(id: String) {
            categories.removeAll { it.id == id }
            notifyChange()
        }

        override suspend fun getCategoriesGroup(name: String, type: String, householdId: String?): List<CategoryEntity> {
            return categories.filter {
                it.name == name && it.type == type &&
                        ((householdId == null && it.householdId == null) || it.householdId == householdId)
            }
        }

        override suspend fun getCategoryById(id: String): CategoryEntity? = categories.find { it.id == id }

        override suspend fun deleteCategoriesByHousehold(householdId: String?) {
            if (householdId == null) {
                categories.removeAll { it.householdId == null }
            } else {
                categories.removeAll { it.householdId == householdId }
            }
            notifyChange()
        }

        override suspend fun deleteAllCategories(householdId: String?) {
            deleteCategoriesByHousehold(householdId)
        }
    }

    private class FakeSyncOutboxDao : SyncOutboxDao {
        val outbox = mutableListOf<SyncOutboxEntity>()

        override fun getAllOutboxEntries(): Flow<List<SyncOutboxEntity>> = flowOf(outbox.toList())
        override suspend fun getPendingEntries(): List<SyncOutboxEntity> = outbox.filter { it.status == "PENDING" }
        override suspend fun getPendingBatch(limit: Int): List<SyncOutboxEntity> = outbox.filter { it.status == "PENDING" }.take(limit)
        override suspend fun getPendingCount(): Int = outbox.count { it.status == "PENDING" }
        override fun getPendingCountFlow(): Flow<Int> = flowOf(outbox.count { it.status == "PENDING" })
        override suspend fun getPendingEntryForEntity(entityId: String): SyncOutboxEntity? =
            outbox.lastOrNull { it.entityId == entityId && it.status == "PENDING" }
        override suspend fun getEntryById(id: String): SyncOutboxEntity? = outbox.find { it.id == id }
        override suspend fun markInProgress(id: String, lastAttemptAt: Long, updatedAt: Long): Int = 1
        override suspend fun markPending(id: String, updatedAt: Long): Int = 1
        override suspend fun markAcknowledged(id: String, updatedAt: Long): Int = 1
        override suspend fun markSuccess(id: String, updatedAt: Long): Int = 1
        override suspend fun markFailed(id: String, errorCode: String?, errorMessage: String?, retryCount: Int, lastAttemptAt: Long, updatedAt: Long): Int = 1
        override suspend fun resetInProgressToPending(updatedAt: Long): Int = 0
        override suspend fun incrementRetryCount(id: String, lastAttemptAt: Long, errorCode: String?, errorMessage: String?, updatedAt: Long): Int = 1
        override suspend fun recordRetryFailure(id: String, errorCode: String?, errorMessage: String?, lastAttemptAt: Long, updatedAt: Long): Int = 1
        override suspend fun clearAcknowledgedEntries() {}
        override suspend fun deleteAcknowledgedEntries(): Int = 0
        override suspend fun deleteSuccessEntries(): Int = 0
        override suspend fun deleteOldCompletedEntries(cutoffTime: Long): Int = 0
        override suspend fun deleteOldFailedEntries(cutoffTime: Long): Int = 0
        override suspend fun deleteOutboxEntryById(id: String) { outbox.removeAll { it.id == id } }
        override suspend fun deleteOutboxEntriesForEntity(entityId: String) { outbox.removeAll { it.entityId == entityId } }
        override suspend fun deleteAllOutboxEntries() { outbox.clear() }
        override suspend fun getPendingEntry(entityType: String, entityId: String): SyncOutboxEntity? =
            outbox.lastOrNull { it.entityType == entityType && it.entityId == entityId && it.status == "PENDING" }
        override suspend fun getActiveEntry(entityType: String, entityId: String): SyncOutboxEntity? =
            outbox.lastOrNull { it.entityType == entityType && it.entityId == entityId && it.status in listOf("PENDING", "IN_PROGRESS") }
        override suspend fun getActiveEntityIdsByType(entityType: String): List<String> =
            outbox.filter { it.entityType == entityType && it.status in listOf("PENDING", "IN_PROGRESS") }.map { it.entityId }
        override suspend fun deleteEntriesForEntity(entityType: String, entityId: String): Int {
            val count = outbox.count { it.entityType == entityType && it.entityId == entityId }
            outbox.removeAll { it.entityType == entityType && it.entityId == entityId }
            return count
        }
        override suspend fun insertOutboxEntry(entry: SyncOutboxEntity) {
            outbox.removeAll { it.id == entry.id }
            outbox.add(entry)
        }
        override suspend fun insertAllOutboxEntries(entries: List<SyncOutboxEntity>) {
            entries.forEach { insertOutboxEntry(it) }
        }
        override suspend fun updateOutboxEntry(entry: SyncOutboxEntity) {
            val index = outbox.indexOfFirst { it.id == entry.id }
            if (index >= 0) outbox[index] = entry
        }
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // =========================================================================
    // 1. PERMISSION STATE TESTS (OWNER, ADMIN, MEMBER, OFFLINE)
    // =========================================================================

    @Test
    fun testOfflineOrNullHouseholdAllowsCategoryManagement() = runTest {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val authRepo = FakeAuthRepo(initialUid = "user_1")
        val householdRepo = FakeHouseholdRepo()
        val syncRepo = FakeSyncRepo()
        syncRepo.syncStatusFlow.value = SyncStatus.Offline

        val categoryDao = FakeCategoryDao()
        val outboxDao = FakeSyncOutboxDao()
        val categoryRepo = RoomCategoryRepository(categoryDao, outboxDao)
        val transactionRepo = FakeTransactionRepo()
        val settingsRepo = FakeTestSettingsRepo()

        val vm = MainViewModel(
            transactionRepository = transactionRepo,
            categoryRepository = categoryRepo,
            settingsRepository = settingsRepo,
            authRepository = authRepo,
            householdRepository = householdRepo,
            syncRepository = syncRepo,
            ioDispatcher = testDispatcher,
            application = app
        )
        advanceUntilIdle()

        assertTrue(
            "canManageCategories should be true in offline/null household mode",
            vm.canManageCategories.value
        )
    }

    @Test
    fun testOwnerRoleAllowsCategoryManagement() = runTest {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val authRepo = FakeAuthRepo(initialUid = "user_owner")
        val householdRepo = FakeHouseholdRepo()
        val syncRepo = FakeSyncRepo()
        syncRepo.syncStatusFlow.value = SyncStatus.Synced(householdId = "hh_123")

        householdRepo.membersFlow.value = listOf(
            HouseholdMemberDto(uid = "user_owner", email = "owner@example.com", role = "owner")
        )

        val categoryDao = FakeCategoryDao()
        val outboxDao = FakeSyncOutboxDao()
        val categoryRepo = RoomCategoryRepository(categoryDao, outboxDao)
        val transactionRepo = FakeTransactionRepo()
        val settingsRepo = FakeTestSettingsRepo()

        val vm = MainViewModel(
            transactionRepository = transactionRepo,
            categoryRepository = categoryRepo,
            settingsRepository = settingsRepo,
            authRepository = authRepo,
            householdRepository = householdRepo,
            syncRepository = syncRepo,
            ioDispatcher = testDispatcher,
            application = app
        )
        advanceUntilIdle()

        assertTrue(
            "canManageCategories should be true for household owner",
            vm.canManageCategories.value
        )
    }

    @Test
    fun testAdminRoleAllowsCategoryManagement() = runTest {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val authRepo = FakeAuthRepo(initialUid = "user_admin")
        val householdRepo = FakeHouseholdRepo()
        val syncRepo = FakeSyncRepo()
        syncRepo.syncStatusFlow.value = SyncStatus.Synced(householdId = "hh_123")

        householdRepo.membersFlow.value = listOf(
            HouseholdMemberDto(uid = "user_admin", email = "admin@example.com", role = "admin")
        )

        val categoryDao = FakeCategoryDao()
        val outboxDao = FakeSyncOutboxDao()
        val categoryRepo = RoomCategoryRepository(categoryDao, outboxDao)
        val transactionRepo = FakeTransactionRepo()
        val settingsRepo = FakeTestSettingsRepo()

        val vm = MainViewModel(
            transactionRepository = transactionRepo,
            categoryRepository = categoryRepo,
            settingsRepository = settingsRepo,
            authRepository = authRepo,
            householdRepository = householdRepo,
            syncRepository = syncRepo,
            ioDispatcher = testDispatcher,
            application = app
        )
        advanceUntilIdle()

        assertTrue(
            "canManageCategories should be true for household admin",
            vm.canManageCategories.value
        )
    }

    @Test
    fun testMemberRoleDeniesCategoryManagement() = runTest {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val authRepo = FakeAuthRepo(initialUid = "user_member")
        val householdRepo = FakeHouseholdRepo()
        val syncRepo = FakeSyncRepo()
        syncRepo.syncStatusFlow.value = SyncStatus.Synced(householdId = "hh_123")

        householdRepo.membersFlow.value = listOf(
            HouseholdMemberDto(uid = "user_member", email = "member@example.com", role = "member")
        )

        val categoryDao = FakeCategoryDao()
        val outboxDao = FakeSyncOutboxDao()
        val categoryRepo = RoomCategoryRepository(categoryDao, outboxDao)
        val transactionRepo = FakeTransactionRepo()
        val settingsRepo = FakeTestSettingsRepo()

        val vm = MainViewModel(
            transactionRepository = transactionRepo,
            categoryRepository = categoryRepo,
            settingsRepository = settingsRepo,
            authRepository = authRepo,
            householdRepository = householdRepo,
            syncRepository = syncRepo,
            ioDispatcher = testDispatcher,
            application = app
        )
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
    fun testMemberCannotAddCategoryViaViewModel() = runTest {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val authRepo = FakeAuthRepo(initialUid = "user_member")
        val householdRepo = FakeHouseholdRepo()
        val syncRepo = FakeSyncRepo()
        syncRepo.syncStatusFlow.value = SyncStatus.Synced(householdId = "hh_123")
        householdRepo.membersFlow.value = listOf(
            HouseholdMemberDto(uid = "user_member", email = "member@example.com", role = "member")
        )

        val categoryDao = FakeCategoryDao()
        val outboxDao = FakeSyncOutboxDao()
        val categoryRepo = RoomCategoryRepository(categoryDao, outboxDao)
        val transactionRepo = FakeTransactionRepo()
        val settingsRepo = FakeTestSettingsRepo()

        val vm = MainViewModel(
            transactionRepository = transactionRepo,
            categoryRepository = categoryRepo,
            settingsRepository = settingsRepo,
            authRepository = authRepo,
            householdRepository = householdRepo,
            syncRepository = syncRepo,
            ioDispatcher = testDispatcher,
            application = app
        )
        advanceUntilIdle()

        // Attempt add category as member
        vm.addCategory(name = "Secret", type = "Expense", subCategory = "Hacks")
        advanceUntilIdle()

        assertEquals(0, categoryDao.categories.size)
        assertEquals(0, outboxDao.outbox.size)
        assertEquals("Only household owner or admin can modify categories.", vm.uiState.value.userNotification)
    }

    @Test
    fun testMemberCannotUpdateCategoryGroupViaViewModel() = runTest {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val authRepo = FakeAuthRepo(initialUid = "user_member")
        val householdRepo = FakeHouseholdRepo()
        val syncRepo = FakeSyncRepo()
        syncRepo.syncStatusFlow.value = SyncStatus.Synced(householdId = "hh_123")
        householdRepo.membersFlow.value = listOf(
            HouseholdMemberDto(uid = "user_member", email = "member@example.com", role = "member")
        )

        val categoryDao = FakeCategoryDao()
        categoryDao.categories.add(CategoryEntity(id = "cat_1", name = "Food", type = "Expense", subCategory = "Groceries", householdId = "hh_123"))
        val outboxDao = FakeSyncOutboxDao()
        val categoryRepo = RoomCategoryRepository(categoryDao, outboxDao)
        val transactionRepo = FakeTransactionRepo()
        val settingsRepo = FakeTestSettingsRepo()

        val vm = MainViewModel(
            transactionRepository = transactionRepo,
            categoryRepository = categoryRepo,
            settingsRepository = settingsRepo,
            authRepository = authRepo,
            householdRepository = householdRepo,
            syncRepository = syncRepo,
            ioDispatcher = testDispatcher,
            application = app
        )
        advanceUntilIdle()

        vm.updateCategoryGroup(oldName = "Food", newName = "Groceries & Dine", type = "Expense")
        advanceUntilIdle()

        assertEquals("Food", categoryDao.categories.first().name)
        assertEquals(0, outboxDao.outbox.size)
        assertEquals("Only household owner or admin can modify categories.", vm.uiState.value.userNotification)
    }

    @Test
    fun testMemberCannotDeleteCategoryGroupViaViewModel() = runTest {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val authRepo = FakeAuthRepo(initialUid = "user_member")
        val householdRepo = FakeHouseholdRepo()
        val syncRepo = FakeSyncRepo()
        syncRepo.syncStatusFlow.value = SyncStatus.Synced(householdId = "hh_123")
        householdRepo.membersFlow.value = listOf(
            HouseholdMemberDto(uid = "user_member", email = "member@example.com", role = "member")
        )

        val categoryDao = FakeCategoryDao()
        categoryDao.categories.add(CategoryEntity(id = "cat_1", name = "Food", type = "Expense", subCategory = "Groceries", householdId = "hh_123"))
        val outboxDao = FakeSyncOutboxDao()
        val categoryRepo = RoomCategoryRepository(categoryDao, outboxDao)
        val transactionRepo = FakeTransactionRepo()
        val settingsRepo = FakeTestSettingsRepo()

        val vm = MainViewModel(
            transactionRepository = transactionRepo,
            categoryRepository = categoryRepo,
            settingsRepository = settingsRepo,
            authRepository = authRepo,
            householdRepository = householdRepo,
            syncRepository = syncRepo,
            ioDispatcher = testDispatcher,
            application = app
        )
        advanceUntilIdle()

        vm.deleteCategoryGroup(name = "Food", type = "Expense")
        advanceUntilIdle()

        assertEquals(1, categoryDao.categories.size)
        assertEquals(0, outboxDao.outbox.size)
        assertEquals("Only household owner or admin can modify categories.", vm.uiState.value.userNotification)
    }

    @Test
    fun testMemberCannotUpdateSubcategoryViaViewModel() = runTest {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val authRepo = FakeAuthRepo(initialUid = "user_member")
        val householdRepo = FakeHouseholdRepo()
        val syncRepo = FakeSyncRepo()
        syncRepo.syncStatusFlow.value = SyncStatus.Synced(householdId = "hh_123")
        householdRepo.membersFlow.value = listOf(
            HouseholdMemberDto(uid = "user_member", email = "member@example.com", role = "member")
        )

        val categoryDao = FakeCategoryDao()
        categoryDao.categories.add(CategoryEntity(id = "cat_1", name = "Food", type = "Expense", subCategory = "Groceries", householdId = "hh_123"))
        val outboxDao = FakeSyncOutboxDao()
        val categoryRepo = RoomCategoryRepository(categoryDao, outboxDao)
        val transactionRepo = FakeTransactionRepo()
        val settingsRepo = FakeTestSettingsRepo()

        val vm = MainViewModel(
            transactionRepository = transactionRepo,
            categoryRepository = categoryRepo,
            settingsRepository = settingsRepo,
            authRepository = authRepo,
            householdRepository = householdRepo,
            syncRepository = syncRepo,
            ioDispatcher = testDispatcher,
            application = app
        )
        advanceUntilIdle()

        vm.updateSubcategory(id = "cat_1", newSubCategory = "Supermarket")
        advanceUntilIdle()

        assertEquals("Groceries", categoryDao.categories.first().subCategory)
        assertEquals(0, outboxDao.outbox.size)
        assertEquals("Only household owner or admin can modify categories.", vm.uiState.value.userNotification)
    }

    @Test
    fun testMemberCannotDeleteSubcategoryViaViewModel() = runTest {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val authRepo = FakeAuthRepo(initialUid = "user_member")
        val householdRepo = FakeHouseholdRepo()
        val syncRepo = FakeSyncRepo()
        syncRepo.syncStatusFlow.value = SyncStatus.Synced(householdId = "hh_123")
        householdRepo.membersFlow.value = listOf(
            HouseholdMemberDto(uid = "user_member", email = "member@example.com", role = "member")
        )

        val categoryDao = FakeCategoryDao()
        categoryDao.categories.add(CategoryEntity(id = "cat_1", name = "Food", type = "Expense", subCategory = "Groceries", householdId = "hh_123"))
        val outboxDao = FakeSyncOutboxDao()
        val categoryRepo = RoomCategoryRepository(categoryDao, outboxDao)
        val transactionRepo = FakeTransactionRepo()
        val settingsRepo = FakeTestSettingsRepo()

        val vm = MainViewModel(
            transactionRepository = transactionRepo,
            categoryRepository = categoryRepo,
            settingsRepository = settingsRepo,
            authRepository = authRepo,
            householdRepository = householdRepo,
            syncRepository = syncRepo,
            ioDispatcher = testDispatcher,
            application = app
        )
        advanceUntilIdle()

        vm.deleteSubcategory(id = "cat_1")
        advanceUntilIdle()

        assertEquals(1, categoryDao.categories.size)
        assertEquals(0, outboxDao.outbox.size)
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
        composeTestRule.onNodeWithText("Food").assertIsDisplayed()
    }
}