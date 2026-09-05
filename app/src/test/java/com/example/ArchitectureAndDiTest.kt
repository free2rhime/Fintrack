package com.example

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.CategoryEntity
import com.example.data.model.FilterSettings
import com.example.data.model.TransactionEntity
import com.example.data.repository.AuthRepository
import com.example.data.repository.AuthState
import com.example.data.repository.CategoryRepository
import com.example.data.repository.DataStoreSettingsRepository
import com.example.data.repository.PendingRetryResult
import com.example.data.repository.PreparedRepairItem
import com.example.data.repository.RoomCategoryRepository
import com.example.data.repository.RoomTransactionRepository
import com.example.data.repository.SettingsRepository
import com.example.data.repository.TransactionRepository
import com.example.data.service.BnrDiagnosticResult
import com.example.data.service.BnrRateResult
import com.example.data.util.CsvImportFinalResult
import com.example.data.util.CsvPreviewData
import com.example.di.DefaultAppContainer
import com.example.ui.MainViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
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
class ArchitectureAndDiTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testMainViewModelConstructedWithFakeRepositories() = runTest {
        val fakeAuthRepo = FakeAuthRepository()
        val fakeTxRepo = FakeTransactionRepository(fakeAuthRepo)
        val fakeCatRepo = FakeCategoryRepository(fakeAuthRepo)
        val fakeSettingsRepo = FakeSettingsRepository()
        val app = ApplicationProvider.getApplicationContext<Application>()

        fakeAuthRepo.signInWithTestUid("test_user_1")

        val viewModel = MainViewModel(
            transactionRepository = fakeTxRepo,
            categoryRepository = fakeCatRepo,
            settingsRepository = fakeSettingsRepo,
            authRepository = fakeAuthRepo,
            application = app
        )

        // Seed fake data
        fakeTxRepo.saveTransaction(
            id = "tx1",
            date = "2026-08-11",
            description = "Fake Test Transaction",
            amountRON = 100.0,
            type = "Expense",
            account = "Card",
            category = "Food",
            subCategory = "Groceries",
            userId = "test_user_1"
        )

        fakeCatRepo.addCategory("Food", "Expense", "Groceries", userId = "test_user_1")

        // Verify MainViewModel flows collect from fake repositories
        val txs = viewModel.allTransactions.first { it.isNotEmpty() }
        assertEquals(1, txs.size)
        assertEquals("Fake Test Transaction", txs[0].description)

        val cats = viewModel.categories.first { it.isNotEmpty() }
        assertEquals(1, cats.size)
        assertEquals("Food", cats[0].name)

        val theme = viewModel.themeMode.first()
        assertEquals("light", theme)

        val filter = viewModel.filterSettings.first()
        assertEquals("Last Month", filter.selectedPeriod)
    }

    @Test
    fun testSignedOutStateExposesNoFinancialRecords() = runTest {
        val fakeAuthRepo = FakeAuthRepository(AuthState.SignedOut)
        val fakeTxRepo = FakeTransactionRepository(fakeAuthRepo)
        val fakeCatRepo = FakeCategoryRepository(fakeAuthRepo)
        val fakeSettingsRepo = FakeSettingsRepository()
        val app = ApplicationProvider.getApplicationContext<Application>()

        val viewModel = MainViewModel(
            transactionRepository = fakeTxRepo,
            categoryRepository = fakeCatRepo,
            settingsRepository = fakeSettingsRepo,
            authRepository = fakeAuthRepo,
            application = app
        )

        fakeTxRepo.saveTransaction(
            id = "tx1", date = "2026-08-11", description = "Secret Alpha Transaction",
            amountRON = 100.0, type = "Expense", account = "Card",
            category = "Food", subCategory = "Groceries", userId = "user_alpha"
        )
        fakeCatRepo.addCategory("Alpha Category", "Expense", "SubAlpha", userId = "user_alpha")

        assertTrue(viewModel.allTransactions.first().isEmpty())
        assertTrue(viewModel.categories.first().isEmpty())
    }

    @Test
    fun testSuccessfulSignInExposesActiveUidAndData() = runTest {
        val fakeAuthRepo = FakeAuthRepository()
        val fakeTxRepo = FakeTransactionRepository(fakeAuthRepo)
        val fakeCatRepo = FakeCategoryRepository(fakeAuthRepo)
        val fakeSettingsRepo = FakeSettingsRepository()
        val app = ApplicationProvider.getApplicationContext<Application>()

        fakeAuthRepo.signInWithTestUid("user_alpha")

        val viewModel = MainViewModel(
            transactionRepository = fakeTxRepo,
            categoryRepository = fakeCatRepo,
            settingsRepository = fakeSettingsRepo,
            authRepository = fakeAuthRepo,
            application = app
        )

        fakeTxRepo.saveTransaction(
            id = "tx_alpha", date = "2026-08-11", description = "Alpha Dinner",
            amountRON = 150.0, type = "Expense", account = "Card",
            category = "Food", subCategory = "Restaurants", userId = "user_alpha"
        )
        fakeTxRepo.saveTransaction(
            id = "tx_beta", date = "2026-08-11", description = "Beta Movie",
            amountRON = 50.0, type = "Expense", account = "Card",
            category = "Entertainment", subCategory = "Cinema", userId = "user_beta"
        )

        fakeCatRepo.addCategory("Alpha Category", "Expense", "SubAlpha", userId = "user_alpha")
        fakeCatRepo.addCategory("Default Shared", "Expense", "SubDefault", userId = "local_user")

        assertEquals("user_alpha", viewModel.activeUserUid.first())

        val txs = viewModel.allTransactions.first { it.isNotEmpty() }
        assertEquals(1, txs.size)
        assertEquals("Alpha Dinner", txs[0].description)

        val cats = viewModel.categories.first { it.isNotEmpty() }
        assertEquals(2, cats.size)
        assertTrue(cats.any { it.name == "Alpha Category" })
        assertTrue(cats.any { it.name == "Default Shared" })
    }

    @Test
    fun testSignOutClearsFinancialStateFlows() = runTest {
        val fakeAuthRepo = FakeAuthRepository()
        val fakeTxRepo = FakeTransactionRepository(fakeAuthRepo)
        val fakeCatRepo = FakeCategoryRepository(fakeAuthRepo)
        val fakeSettingsRepo = FakeSettingsRepository()
        val app = ApplicationProvider.getApplicationContext<Application>()

        fakeAuthRepo.signInWithTestUid("user_alpha")

        val viewModel = MainViewModel(
            transactionRepository = fakeTxRepo,
            categoryRepository = fakeCatRepo,
            settingsRepository = fakeSettingsRepo,
            authRepository = fakeAuthRepo,
            application = app
        )

        fakeTxRepo.saveTransaction(
            id = "tx1", date = "2026-08-11", description = "Alpha Salary",
            amountRON = 5000.0, type = "Income", account = "Bank",
            category = "Salary", subCategory = "Main", userId = "user_alpha"
        )
        fakeCatRepo.addCategory("Alpha Income", "Income", "SubIncome", userId = "user_alpha")

        val initialTxs = viewModel.allTransactions.first { it.isNotEmpty() }
        assertEquals(1, initialTxs.size)

        fakeAuthRepo.signOut()

        val activeUid = viewModel.activeUserUid.first { it == null }
        assertEquals(null, activeUid)

        val clearedTxs = viewModel.allTransactions.first { it.isEmpty() }
        assertTrue(clearedTxs.isEmpty())

        val clearedCats = viewModel.categories.first { it.isEmpty() }
        assertTrue(clearedCats.isEmpty())
    }

    @Test
    fun testSwitchingUidHidesPreviousUserRecords() = runTest {
        val fakeAuthRepo = FakeAuthRepository()
        val fakeTxRepo = FakeTransactionRepository(fakeAuthRepo)
        val fakeCatRepo = FakeCategoryRepository(fakeAuthRepo)
        val fakeSettingsRepo = FakeSettingsRepository()
        val app = ApplicationProvider.getApplicationContext<Application>()

        fakeAuthRepo.signInWithTestUid("uid_alpha")

        val viewModel = MainViewModel(
            transactionRepository = fakeTxRepo,
            categoryRepository = fakeCatRepo,
            settingsRepository = fakeSettingsRepo,
            authRepository = fakeAuthRepo,
            application = app
        )

        fakeTxRepo.saveTransaction(
            id = "tx_alpha", date = "2026-08-11", description = "Alpha Laptop",
            amountRON = 3000.0, type = "Expense", account = "Card",
            category = "Tech", subCategory = "Hardware", userId = "uid_alpha"
        )
        fakeTxRepo.saveTransaction(
            id = "tx_beta", date = "2026-08-11", description = "Beta Phone",
            amountRON = 2000.0, type = "Expense", account = "Card",
            category = "Tech", subCategory = "Gadgets", userId = "uid_beta"
        )

        fakeCatRepo.addCategory("Alpha Tech", "Expense", "SubAlpha", userId = "uid_alpha")
        fakeCatRepo.addCategory("Beta Tech", "Expense", "SubBeta", userId = "uid_beta")

        val alphaTxs = viewModel.allTransactions.first { it.isNotEmpty() }
        assertEquals(1, alphaTxs.size)
        assertEquals("Alpha Laptop", alphaTxs[0].description)
        assertTrue(viewModel.categories.first().any { it.name == "Alpha Tech" })

        // Switch to uid_beta
        fakeAuthRepo.signInWithTestUid("uid_beta")

        val betaUid = viewModel.activeUserUid.first { it == "uid_beta" }
        assertEquals("uid_beta", betaUid)

        val betaTxs = viewModel.allTransactions.first { txs -> txs.any { it.description == "Beta Phone" } }
        assertEquals(1, betaTxs.size)
        assertEquals("Beta Phone", betaTxs[0].description)

        val betaCats = viewModel.categories.first { cats -> cats.any { it.name == "Beta Tech" } }
        assertEquals(1, betaCats.size)
        assertEquals("Beta Tech", betaCats[0].name)
    }

    @Test
    fun testAuthErrorExposesNoFinancialRecords() = runTest {
        val fakeAuthRepo = FakeAuthRepository(AuthState.AuthError("Sign-in failed"))
        val fakeTxRepo = FakeTransactionRepository(fakeAuthRepo)
        val fakeCatRepo = FakeCategoryRepository(fakeAuthRepo)
        val fakeSettingsRepo = FakeSettingsRepository()
        val app = ApplicationProvider.getApplicationContext<Application>()

        val viewModel = MainViewModel(
            transactionRepository = fakeTxRepo,
            categoryRepository = fakeCatRepo,
            settingsRepository = fakeSettingsRepo,
            authRepository = fakeAuthRepo,
            application = app
        )

        fakeTxRepo.saveTransaction(
            id = "tx1", date = "2026-08-11", description = "Protected Tx",
            amountRON = 100.0, type = "Expense", account = "Card",
            category = "Food", subCategory = "Groceries", userId = "user_alpha"
        )

        assertTrue(viewModel.allTransactions.first().isEmpty())
        assertTrue(viewModel.categories.first().isEmpty())
    }

    @Test
    fun testRoomRepositoryIsActiveDefaultImplementation() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val container = DefaultAppContainer(app)

        assertNotNull(container.database)
        assertTrue(container.transactionRepository is RoomTransactionRepository)
        assertTrue(container.categoryRepository is RoomCategoryRepository)
        assertTrue(container.settingsRepository is DataStoreSettingsRepository)
    }
}

// Fake Repositories for testing MainViewModel decoupling
class FakeAuthRepository(
    initialState: AuthState = AuthState.SignedOut
) : AuthRepository {
    private val _authState = MutableStateFlow<AuthState>(initialState)
    override val authState: StateFlow<AuthState> = _authState

    override suspend fun signInWithGoogleCredential(idToken: String): Result<String> {
        _authState.value = AuthState.SigningIn
        val uid = "google_$idToken"
        _authState.value = AuthState.SignedIn(userUid = uid, email = "$uid@example.com")
        return Result.success(uid)
    }

    override suspend fun signInWithTestUid(testUid: String, email: String?, displayName: String?): Result<String> {
        _authState.value = AuthState.SigningIn
        _authState.value = AuthState.SignedIn(
            userUid = testUid,
            email = email ?: "$testUid@example.com",
            displayName = displayName ?: testUid
        )
        return Result.success(testUid)
    }

    override fun setAuthError(message: String) {
        _authState.value = AuthState.AuthError(message)
    }

    override suspend fun signOut() {
        _authState.value = AuthState.SignedOut
    }

    override fun getCurrentUserUid(): String? {
        val state = _authState.value
        return if (state is AuthState.SignedIn) state.userUid else null
    }

    override fun clearError() {
        if (_authState.value is AuthState.AuthError) {
            _authState.value = AuthState.SignedOut
        }
    }
}

class FakeTransactionRepository(private val authRepo: AuthRepository? = null) : TransactionRepository {
    private val txList = mutableListOf<TransactionEntity>()
    private val _txListFlow = MutableStateFlow<List<TransactionEntity>>(emptyList())

    override val allTransactions: Flow<List<TransactionEntity>>
        get() = if (authRepo != null) {
            combine(_txListFlow, authRepo.authState) { list, state ->
                val uid = (state as? AuthState.SignedIn)?.userUid
                if (uid != null) list.filter { it.userId == uid } else emptyList()
            }
        } else {
            _txListFlow
        }

    override fun getTransactions(householdId: String?): Flow<List<TransactionEntity>> = allTransactions

    override fun getTransactionsInRange(startDate: String, endDate: String): Flow<List<TransactionEntity>> = allTransactions

    override suspend fun getTransactionById(id: String): TransactionEntity? = txList.find { it.id == id }

    override suspend fun saveTransaction(
        id: String?, date: String, description: String, amountRON: Double,
        type: String, account: String, category: String, subCategory: String, destination: String?,
        userId: String, householdId: String?
    ): TransactionEntity {
        val tx = TransactionEntity(
            id = id ?: "tx_${txList.size + 1}",
            userId = userId,
            date = date,
            description = description,
            amountRON = amountRON,
            amountEUR = amountRON / 5.0,
            exchangeRate = 5.0,
            exchangeRateDate = date,
            exchangeRateSource = "BNR_OFFICIAL",
            conversionStatus = "OFFICIAL",
            type = type,
            account = account,
            category = category,
            subCategory = subCategory,
            destination = destination,
            householdId = householdId
        )
        txList.removeAll { it.id == tx.id }
        txList.add(tx)
        _txListFlow.value = txList.toList()
        return tx
    }

    override suspend fun createDuplicateTemplate(source: TransactionEntity): TransactionEntity = source.copy(id = "dup_${System.currentTimeMillis()}")
    override suspend fun getDescriptionSuggestions(query: String, limit: Int, householdId: String?): List<String> = emptyList()
    override suspend fun insertBatchWithTransaction(transactions: List<TransactionEntity>) { txList.addAll(transactions); _txListFlow.value = txList.toList() }
    override suspend fun getUnverifiedTransactions(): List<TransactionEntity> = emptyList()
    override suspend fun getAllTransactionsList(): List<TransactionEntity> = txList.toList()
    override suspend fun syncPendingConversions(): PendingRetryResult = PendingRetryResult(0, 0, 0, 0, null)
    override suspend fun applyRepairBatch(repairs: List<PreparedRepairItem>): Int = repairs.size
    override suspend fun applyRepairToTransactionAndCache(transaction: TransactionEntity, officialRate: Double, effectiveBnrDate: String) {}
    override suspend fun insertTransaction(transaction: TransactionEntity) { txList.add(transaction); _txListFlow.value = txList.toList() }
    override suspend fun deleteTransaction(transaction: TransactionEntity) { txList.remove(transaction); _txListFlow.value = txList.toList() }
    override suspend fun deleteTransactionById(id: String) { txList.removeAll { it.id == id }; _txListFlow.value = txList.toList() }
    override suspend fun deleteAllTransactions() { txList.clear(); _txListFlow.value = emptyList() }
    override suspend fun insertBatch(transactions: List<TransactionEntity>) { txList.addAll(transactions); _txListFlow.value = txList.toList() }
    override suspend fun getOfficialRate(date: String): BnrRateResult = BnrRateResult(date, date, 5.0, "BNR_OFFICIAL", "OFFICIAL", "OK")
    override suspend fun runBnrDiagnostic(): BnrDiagnosticResult = BnrDiagnosticResult(isReachable = true, httpStatus = "200")
    override suspend fun executeAtomicCsvImport(previewData: CsvPreviewData, backupFile: File, allExistingTransactions: List<TransactionEntity>): CsvImportFinalResult {
        return CsvImportFinalResult(true, 0, 0, 0, 0, 0, 0, 0, 0)
    }
}

class FakeCategoryRepository(private val authRepo: AuthRepository? = null) : CategoryRepository {
    private val catList = mutableListOf<CategoryEntity>()
    private val _catListFlow = MutableStateFlow<List<CategoryEntity>>(emptyList())

    override val allCategories: Flow<List<CategoryEntity>>
        get() = if (authRepo != null) {
            combine(_catListFlow, authRepo.authState) { list, state ->
                val uid = (state as? AuthState.SignedIn)?.userUid
                if (uid != null) list.filter { it.userId == uid || it.userId == "local_user" } else emptyList()
            }
        } else {
            _catListFlow
        }

    override fun getCategories(householdId: String?): Flow<List<CategoryEntity>> = allCategories

    override suspend fun getAllCategoriesList(): List<CategoryEntity> = catList.toList()
    override suspend fun ensureDefaultCategoriesSeeded(householdId: String?, enqueueOutbox: Boolean) {}
    override suspend fun addCategory(name: String, type: String, subCategory: String, userId: String, householdId: String?) {
        catList.add(CategoryEntity(id = "cat_${catList.size + 1}", name = name, type = type, subCategory = subCategory, userId = userId, householdId = householdId))
        _catListFlow.value = catList.toList()
    }
    override suspend fun updateCategory(category: CategoryEntity) {}
    override suspend fun deleteCategory(category: CategoryEntity) {}
    override suspend fun updateCategoryGroup(oldName: String, newName: String, type: String, householdId: String?) {}
    override suspend fun deleteCategoryGroup(name: String, type: String, householdId: String?) {}
    override suspend fun updateSubcategory(id: String, newSubCategory: String) {}
    override suspend fun deleteSubcategory(id: String) {}
}

class FakeSettingsRepository : SettingsRepository {
    private val _filterFlow = MutableStateFlow(FilterSettings())
    private val _themeFlow = MutableStateFlow("light")

    override val filterSettingsFlow: Flow<FilterSettings> = _filterFlow
    override val themeModeFlow: Flow<String> = _themeFlow

    override suspend fun updateSelectedPeriod(period: String) {}
    override suspend fun updateSelectedCurrency(currency: String) {}
    override suspend fun updateCustomDateRange(startDate: String, endDate: String) {}
    override suspend fun updateSelectedType(type: String) {}
    override suspend fun updateCategoryFilter(type: String, categoryName: String?) {}
    override suspend fun updateThemeMode(mode: String) { _themeFlow.value = mode }
}
