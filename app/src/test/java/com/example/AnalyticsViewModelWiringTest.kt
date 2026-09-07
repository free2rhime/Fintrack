package com.example

import android.app.Application
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.db.FinTrackDatabase
import com.example.data.model.CategoryEntity
import com.example.data.model.ExchangeRateEntity
import com.example.data.model.FilterSettings
import com.example.data.model.TransactionEntity
import com.example.data.repository.RoomCategoryRepository
import com.example.data.repository.RoomTransactionRepository
import com.example.data.repository.SettingsRepository
import com.example.data.repository.FirestoreSyncRepository
import com.example.ui.MainViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class FakeAnalyticsSettingsRepository : SettingsRepository {
    private val _filterSettings = MutableStateFlow(FilterSettings(selectedPeriod = "Last 6 Months", selectedCurrency = "RON"))
    override val filterSettingsFlow: Flow<FilterSettings> = _filterSettings.asStateFlow()
    override val themeModeFlow: Flow<String> = flowOf("dark")

    fun setFilterSettings(settings: FilterSettings) {
        _filterSettings.value = settings
    }

    override suspend fun updateSelectedPeriod(period: String) {
        _filterSettings.value = _filterSettings.value.copy(selectedPeriod = period)
    }

    override suspend fun updateSelectedCurrency(currency: String) {
        _filterSettings.value = _filterSettings.value.copy(selectedCurrency = currency)
    }

    override suspend fun updateCustomDateRange(startDate: String, endDate: String) {
        _filterSettings.value = _filterSettings.value.copy(customStartDate = startDate, customEndDate = endDate)
    }

    override suspend fun updateSelectedType(type: String) {
        _filterSettings.value = _filterSettings.value.copy(selectedType = type)
    }

    override suspend fun updateCategoryFilter(type: String, categoryName: String?) {
        if (type == "Expense") {
            _filterSettings.value = _filterSettings.value.copy(selectedExpenseCategory = categoryName)
        } else {
            _filterSettings.value = _filterSettings.value.copy(selectedIncomeCategory = categoryName)
        }
    }

    override suspend fun updateThemeMode(mode: String) {}
}

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class AnalyticsViewModelWiringTest {

    private val testDispatcher = kotlinx.coroutines.test.UnconfinedTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private lateinit var application: Application
    private lateinit var db: FinTrackDatabase
    private lateinit var fakeSnapshotSource: FakeSnapshotSource
    private lateinit var syncRepository: FirestoreSyncRepository
    private lateinit var authRepository: FakeTestAuthRepository
    private lateinit var settingsRepository: FakeAnalyticsSettingsRepository
    private lateinit var transactionRepository: RoomTransactionRepository
    private lateinit var categoryRepository: RoomCategoryRepository
    private lateinit var viewModel: MainViewModel

    private val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        application = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(application, FinTrackDatabase::class.java)
            .allowMainThreadQueries()
            .setQueryExecutor(Runnable::run)
            .setTransactionExecutor(Runnable::run)
            .build()

        authRepository = FakeTestAuthRepository("user_owner_1")
        settingsRepository = FakeAnalyticsSettingsRepository()
        val exchangeRateService = com.example.data.service.ExchangeRateService(db.exchangeRateDao())
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
            database = db,
            ioDispatcher = testDispatcher,
            application = application
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        db.close()
    }

    private fun getCurrentMonthDate(day: Int = 10): String {
        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_MONTH, day)
        return sdf.format(cal.time)
    }

    private fun getPastMonthDate(monthsAgo: Int, day: Int = 10): String {
        val cal = Calendar.getInstance()
        cal.add(Calendar.MONTH, -monthsAgo)
        cal.set(Calendar.DAY_OF_MONTH, day)
        return sdf.format(cal.time)
    }

    private fun makeTx(
        id: String,
        date: String,
        type: String,
        category: String,
        amountRON: Double,
        amountEUR: Double,
        status: String = "OFFICIAL",
        source: String = "BNR_OFFICIAL"
    ): TransactionEntity {
        return TransactionEntity(
            id = id,
            date = date,
            description = "Test Tx $id",
            amountRON = amountRON,
            amountEUR = amountEUR,
            exchangeRate = if (amountEUR > 0.0) amountRON / amountEUR else 0.0,
            exchangeRateDate = date,
            type = type,
            account = "Checking",
            category = category,
            subCategory = "DefaultSub",
            exchangeRateSource = source,
            conversionStatus = status,
            householdId = null
        )
    }

    @Test
    fun test1_defaultSelectionIsExpense() = testScope.runTest {
        backgroundScope.launch { viewModel.filterSettings.collect {} }
        backgroundScope.launch { viewModel.periodFilteredTransactions.collect {} }
        backgroundScope.launch { viewModel.analyticsUiState.collect {} }
        advanceUntilIdle()

        val state = viewModel.analyticsUiState.value
        assertEquals("Expense", state.incomeExpenseSelection)
    }

    @Test
    fun test2_firstRankedExpenseCategorySelectedAutomatically() = testScope.runTest {
        val date = getCurrentMonthDate()
        // Insert transactions with Groceries (800) and Transport (200)
        db.transactionDao().insertTransaction(
            makeTx("tx1", date, "Expense", "Groceries", 800.0, 160.0)
        )
        db.transactionDao().insertTransaction(
            makeTx("tx2", date, "Expense", "Transport", 200.0, 40.0)
        )

        backgroundScope.launch { viewModel.filterSettings.collect {} }
        backgroundScope.launch { viewModel.periodFilteredTransactions.collect {} }
        backgroundScope.launch { viewModel.analyticsUiState.collect {} }
        advanceUntilIdle()

        val state = viewModel.analyticsUiState.value
        assertEquals("Groceries", state.selectedExpenseCategory)
        assertEquals(2, state.expenseCategoryRankings.size)
        assertEquals("Groceries", state.expenseCategoryRankings[0].categoryName)
        assertEquals("Transport", state.expenseCategoryRankings[1].categoryName)
    }

    @Test
    fun test3_firstRankedIncomeSourceSelectedAutomatically() = testScope.runTest {
        val date = getCurrentMonthDate()
        // Insert transactions: Salary (5000) and Freelance (2000)
        db.transactionDao().insertTransaction(
            makeTx("tx_inc1", date, "Income", "Salary", 5000.0, 1000.0)
        )
        db.transactionDao().insertTransaction(
            makeTx("tx_inc2", date, "Income", "Freelance", 2000.0, 400.0)
        )

        backgroundScope.launch { viewModel.filterSettings.collect {} }
        backgroundScope.launch { viewModel.periodFilteredTransactions.collect {} }
        backgroundScope.launch { viewModel.analyticsUiState.collect {} }
        advanceUntilIdle()

        val state = viewModel.analyticsUiState.value
        assertEquals("Salary", state.selectedIncomeSource)
        assertEquals(2, state.incomeSourceRankings.size)
        assertEquals("Salary", state.incomeSourceRankings[0].categoryName)
        assertEquals("Freelance", state.incomeSourceRankings[1].categoryName)
    }

    @Test
    fun test4_userCategorySelectionSurvivesPeriodChangeIfStillAvailable() = testScope.runTest {
        val currentMonthDate = getCurrentMonthDate()
        val twoMonthsAgoDate = getPastMonthDate(2)

        // Both months have Groceries and Transport
        db.transactionDao().insertTransaction(
            makeTx("tx1", currentMonthDate, "Expense", "Groceries", 500.0, 100.0)
        )
        db.transactionDao().insertTransaction(
            makeTx("tx2", currentMonthDate, "Expense", "Transport", 200.0, 40.0)
        )
        db.transactionDao().insertTransaction(
            makeTx("tx3", twoMonthsAgoDate, "Expense", "Groceries", 300.0, 60.0)
        )
        db.transactionDao().insertTransaction(
            makeTx("tx4", twoMonthsAgoDate, "Expense", "Transport", 100.0, 20.0)
        )

        backgroundScope.launch { viewModel.filterSettings.collect {} }
        backgroundScope.launch { viewModel.periodFilteredTransactions.collect {} }
        backgroundScope.launch { viewModel.analyticsUiState.collect {} }
        advanceUntilIdle()

        // User explicitly selects Transport
        viewModel.updateAnalyticsExpenseCategorySelection("Transport")
        advanceUntilIdle()
        assertEquals("Transport", viewModel.analyticsUiState.value.selectedExpenseCategory)

        // Period changes to Last 3 Months (Transport still exists)
        viewModel.updateSelectedPeriod("Last 3 Months")
        advanceUntilIdle()
        assertEquals("Transport", viewModel.analyticsUiState.value.selectedExpenseCategory)
    }

    @Test
    fun test5_unavailableCategoryFallsBackToFirstRanking() = testScope.runTest {
        val currentMonthDate = getCurrentMonthDate()
        val fiveMonthsAgoDate = getPastMonthDate(5)

        // Groceries is only in current month; Rent is only 5 months ago
        db.transactionDao().insertTransaction(
            makeTx("tx1", currentMonthDate, "Expense", "Groceries", 500.0, 100.0)
        )
        db.transactionDao().insertTransaction(
            makeTx("tx2", fiveMonthsAgoDate, "Expense", "Rent", 1500.0, 300.0)
        )

        backgroundScope.launch { viewModel.filterSettings.collect {} }
        backgroundScope.launch { viewModel.periodFilteredTransactions.collect {} }
        backgroundScope.launch { viewModel.analyticsUiState.collect {} }
        advanceUntilIdle()

        // In Last 6 Months, Rent is rank 1 (1500 RON), Groceries is rank 2 (500 RON)
        assertEquals("Rent", viewModel.analyticsUiState.value.selectedExpenseCategory)

        // User explicitly selects Rent
        viewModel.updateAnalyticsExpenseCategorySelection("Rent")
        advanceUntilIdle()
        assertEquals("Rent", viewModel.analyticsUiState.value.selectedExpenseCategory)

        // Period changes to Last Month -> Rent no longer in range, only Groceries exists
        viewModel.updateSelectedPeriod("Last Month")
        advanceUntilIdle()
        assertEquals("Groceries", viewModel.analyticsUiState.value.selectedExpenseCategory)
    }

    @Test
    fun test6_unavailableIncomeSourceFallsBackToFirstRanking() = testScope.runTest {
        val currentMonthDate = getCurrentMonthDate()
        val fiveMonthsAgoDate = getPastMonthDate(5)

        // Bonus only 5 months ago; Salary in current month
        db.transactionDao().insertTransaction(
            makeTx("tx_inc1", currentMonthDate, "Income", "Salary", 4000.0, 800.0)
        )
        db.transactionDao().insertTransaction(
            makeTx("tx_inc2", fiveMonthsAgoDate, "Income", "Bonus", 10000.0, 2000.0)
        )

        backgroundScope.launch { viewModel.filterSettings.collect {} }
        backgroundScope.launch { viewModel.periodFilteredTransactions.collect {} }
        backgroundScope.launch { viewModel.analyticsUiState.collect {} }
        advanceUntilIdle()

        // Initially in Last 6 Months, Bonus is #1
        assertEquals("Bonus", viewModel.analyticsUiState.value.selectedIncomeSource)

        // Period changes to Last Month -> Bonus not in range, falls back to Salary
        viewModel.updateSelectedPeriod("Last Month")
        advanceUntilIdle()
        assertEquals("Salary", viewModel.analyticsUiState.value.selectedIncomeSource)
    }

    @Test
    fun test7_changingPeriodUpdatesAllThreeAnalyticsResults() = testScope.runTest {
        val date = getCurrentMonthDate()
        db.transactionDao().insertTransaction(
            makeTx("tx1", date, "Expense", "Shopping", 1000.0, 200.0)
        )
        db.transactionDao().insertTransaction(
            makeTx("tx2", date, "Income", "Salary", 5000.0, 1000.0)
        )

        backgroundScope.launch { viewModel.filterSettings.collect {} }
        backgroundScope.launch { viewModel.periodFilteredTransactions.collect {} }
        backgroundScope.launch { viewModel.analyticsUiState.collect {} }
        advanceUntilIdle()

        // In Last 6 Months
        viewModel.updateSelectedPeriod("Last 6 Months")
        advanceUntilIdle()
        val state6M = viewModel.analyticsUiState.value
        assertEquals(6, state6M.incomeExpenseResult.monthCount)
        assertEquals(6, state6M.expenseCategoryResult.monthCount)
        assertEquals(6, state6M.incomeSourceResult.monthCount)

        // In Last 3 Months
        viewModel.updateSelectedPeriod("Last 3 Months")
        advanceUntilIdle()
        val state3M = viewModel.analyticsUiState.value
        assertEquals(3, state3M.incomeExpenseResult.monthCount)
        assertEquals(3, state3M.expenseCategoryResult.monthCount)
        assertEquals(3, state3M.incomeSourceResult.monthCount)
    }

    @Test
    fun test8_changingCurrencyUpdatesAllThreeAnalyticsResults() = testScope.runTest {
        val date = getCurrentMonthDate()
        // Save exchange rate
        db.exchangeRateDao().insertRate(
            ExchangeRateEntity(date = date, rate = 5.0, source = "BNR_OFFICIAL")
        )
        db.transactionDao().insertTransaction(
            makeTx("tx1", date, "Expense", "Food", 1000.0, 200.0, "OFFICIAL", "BNR_OFFICIAL")
        )
        db.transactionDao().insertTransaction(
            makeTx("tx2", date, "Income", "Salary", 2500.0, 500.0, "OFFICIAL", "BNR_OFFICIAL")
        )

        backgroundScope.launch { viewModel.filterSettings.collect {} }
        backgroundScope.launch { viewModel.periodFilteredTransactions.collect {} }
        backgroundScope.launch { viewModel.analyticsUiState.collect {} }
        advanceUntilIdle()

        // RON currency
        viewModel.updateSelectedCurrency("RON")
        advanceUntilIdle()
        val ronState = viewModel.analyticsUiState.value
        assertEquals("RON", ronState.incomeExpenseResult.currency)
        assertEquals("RON", ronState.expenseCategoryResult.currency)
        assertEquals("RON", ronState.incomeSourceResult.currency)
        assertEquals(1000.0, ronState.incomeExpenseResult.total, 0.01) // Expense selected
        assertEquals(1000.0, ronState.expenseCategoryResult.total, 0.01)
        assertEquals(2500.0, ronState.incomeSourceResult.total, 0.01)

        // EUR currency
        viewModel.updateSelectedCurrency("EUR")
        advanceUntilIdle()
        val eurState = viewModel.analyticsUiState.value
        assertEquals("EUR", eurState.incomeExpenseResult.currency)
        assertEquals("EUR", eurState.expenseCategoryResult.currency)
        assertEquals("EUR", eurState.incomeSourceResult.currency)
        assertEquals(200.0, eurState.incomeExpenseResult.total, 0.01)
        assertEquals(200.0, eurState.expenseCategoryResult.total, 0.01)
        assertEquals(500.0, eurState.incomeSourceResult.total, 0.01)
    }

    @Test
    fun test9_allThreeResultsUseIdenticalMonthAxis() = testScope.runTest {
        val date = getCurrentMonthDate()
        db.transactionDao().insertTransaction(
            makeTx("tx1", date, "Expense", "Food", 500.0, 100.0)
        )
        db.transactionDao().insertTransaction(
            makeTx("tx2", date, "Income", "Salary", 3000.0, 600.0)
        )

        backgroundScope.launch { viewModel.filterSettings.collect {} }
        backgroundScope.launch { viewModel.periodFilteredTransactions.collect {} }
        backgroundScope.launch { viewModel.analyticsUiState.collect {} }
        advanceUntilIdle()

        viewModel.updateSelectedPeriod("Last 6 Months")
        advanceUntilIdle()

        val state = viewModel.analyticsUiState.value
        val axis1 = state.incomeExpenseResult.dataPoints.map { it.yearMonth }
        val axis2 = state.expenseCategoryResult.dataPoints.map { it.yearMonth }
        val axis3 = state.incomeSourceResult.dataPoints.map { it.yearMonth }

        assertEquals(axis1, axis2)
        assertEquals(axis2, axis3)
        assertEquals(6, axis1.size)
    }

    @Test
    fun test10_emptyDataLeavesSelectionsNull() = testScope.runTest {
        backgroundScope.launch { viewModel.filterSettings.collect {} }
        backgroundScope.launch { viewModel.periodFilteredTransactions.collect {} }
        backgroundScope.launch { viewModel.analyticsUiState.collect {} }
        advanceUntilIdle()

        val state = viewModel.analyticsUiState.value
        assertNull(state.selectedExpenseCategory)
        assertNull(state.selectedIncomeSource)
        assertTrue(state.expenseCategoryRankings.isEmpty())
        assertTrue(state.incomeSourceRankings.isEmpty())
    }

    @Test
    fun test11_incomeExpenseSelectionToggleRecalculatesSeries1() = testScope.runTest {
        val date = getCurrentMonthDate()
        db.transactionDao().insertTransaction(
            makeTx("tx1", date, "Expense", "Food", 1200.0, 240.0)
        )
        db.transactionDao().insertTransaction(
            makeTx("tx2", date, "Income", "Salary", 4000.0, 800.0)
        )

        backgroundScope.launch { viewModel.filterSettings.collect {} }
        backgroundScope.launch { viewModel.periodFilteredTransactions.collect {} }
        backgroundScope.launch { viewModel.analyticsUiState.collect {} }
        advanceUntilIdle()

        // Default is Expense -> total is 1200.0
        val expenseState = viewModel.analyticsUiState.value
        assertEquals("Expense", expenseState.incomeExpenseSelection)
        assertEquals(1200.0, expenseState.incomeExpenseResult.total, 0.01)

        // Toggle to Income -> total is 4000.0
        viewModel.updateAnalyticsIncomeExpenseSelection("Income")
        advanceUntilIdle()
        val incomeState = viewModel.analyticsUiState.value
        assertEquals("Income", incomeState.incomeExpenseSelection)
        assertEquals(4000.0, incomeState.incomeExpenseResult.total, 0.01)
    }
}
