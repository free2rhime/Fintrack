package com.example

import android.app.Application
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.db.FinTrackDatabase
import com.example.data.model.FilterSettings
import com.example.data.model.TransactionEntity
import com.example.data.repository.RoomCategoryRepository
import com.example.data.repository.RoomTransactionRepository
import com.example.data.repository.SettingsRepository
import com.example.data.service.ExchangeRateService
import com.example.domain.analytics.DashboardMetrics
import com.example.domain.analytics.SmartFinancialInsights
import com.example.ui.MainViewModel
import com.example.ui.screens.AnalyticsScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.TransactionsScreen
import com.example.ui.theme.FinTrackTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
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
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

private class MutableGlobalSettingsRepository(
    initialPeriod: String = "Last Month",
    initialCurrency: String = "RON"
) : SettingsRepository {
    private val _filterSettings = MutableStateFlow(
        FilterSettings(
            selectedPeriod = initialPeriod,
            selectedCurrency = initialCurrency
        )
    )
    override val filterSettingsFlow: Flow<FilterSettings> = _filterSettings.asStateFlow()
    override val themeModeFlow: Flow<String> = flowOf("dark")

    override suspend fun updateSelectedPeriod(period: String) {
        _filterSettings.value = _filterSettings.value.copy(selectedPeriod = period)
    }

    override suspend fun updateSelectedCurrency(currency: String) {
        _filterSettings.value = _filterSettings.value.copy(selectedCurrency = currency)
    }

    override suspend fun updateCustomDateRange(startDate: String, endDate: String) {
        _filterSettings.value = _filterSettings.value.copy(
            customStartDate = startDate,
            customEndDate = endDate,
            selectedPeriod = "Custom Range"
        )
    }

    override suspend fun updateSelectedType(type: String) {
        _filterSettings.value = _filterSettings.value.copy(selectedType = type)
    }

    override suspend fun updateCategoryFilter(type: String, categoryName: String?) {
        _filterSettings.value = if (type == "Income") {
            _filterSettings.value.copy(selectedIncomeCategory = categoryName)
        } else {
            _filterSettings.value.copy(selectedExpenseCategory = categoryName)
        }
    }

    override suspend fun updateThemeMode(mode: String) {}
}

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class GlobalPeriodFilterTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private lateinit var application: Application
    private lateinit var db: FinTrackDatabase
    private lateinit var transactionRepo: RoomTransactionRepository
    private lateinit var categoryRepo: RoomCategoryRepository
    private lateinit var settingsRepo: MutableGlobalSettingsRepository
    private lateinit var authRepo: FakeTestAuthRepository

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        application = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(application, FinTrackDatabase::class.java)
            .allowMainThreadQueries()
            .setQueryExecutor(Runnable::run)
            .setTransactionExecutor(Runnable::run)
            .build()

        val exchangeRateService = ExchangeRateService(db.exchangeRateDao())
        transactionRepo = RoomTransactionRepository(
            transactionDao = db.transactionDao(),
            exchangeRateService = exchangeRateService,
            exchangeRateDao = db.exchangeRateDao(),
            database = db
        )
        categoryRepo = RoomCategoryRepository(
            categoryDao = db.categoryDao(),
            syncOutboxDao = db.syncOutboxDao(),
            database = db
        )
        settingsRepo = MutableGlobalSettingsRepository()
        authRepo = FakeTestAuthRepository("user_owner_1")
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        db.close()
    }

    @Test
    fun testSingleGlobalPeriodControlsTransactionsAndAnalyticsConsistently() = testScope.runTest {
        val viewModel = MainViewModel(
            transactionRepository = transactionRepo,
            categoryRepository = categoryRepo,
            settingsRepository = settingsRepo,
            authRepository = authRepo,
            database = db,
            ioDispatcher = testDispatcher,
            application = application
        )

        val tx1 = TransactionEntity(
            id = "tx_aug",
            date = "2026-08-15",
            description = "August Grocery",
            amountRON = 200.0,
            amountEUR = 40.0,
            exchangeRate = 5.0,
            exchangeRateDate = "2026-08-15",
            exchangeRateSource = "BNR_OFFICIAL",
            conversionStatus = "OFFICIAL",
            type = "Expense",
            account = "Checking",
            category = "Food",
            subCategory = "Groceries"
        )
        val tx2 = TransactionEntity(
            id = "tx_jul",
            date = "2026-07-20",
            description = "July Electric Bill",
            amountRON = 300.0,
            amountEUR = 60.0,
            exchangeRate = 5.0,
            exchangeRateDate = "2026-07-20",
            exchangeRateSource = "BNR_OFFICIAL",
            conversionStatus = "OFFICIAL",
            type = "Expense",
            account = "Checking",
            category = "Utilities",
            subCategory = "Electricity"
        )
        db.transactionDao().insertTransaction(tx1)
        db.transactionDao().insertTransaction(tx2)

        backgroundScope.launch { viewModel.filterSettings.collect {} }
        backgroundScope.launch { viewModel.filteredTransactions.collect {} }
        backgroundScope.launch { viewModel.periodFilteredTransactions.collect {} }
        backgroundScope.launch { viewModel.dashboardMetrics.collect {} }

        advanceUntilIdle()

        // 1. Initial period: Last Month
        assertEquals("Last Month", viewModel.filterSettings.value.selectedPeriod)

        // 2. Dashboard updates global period to "All Time"
        viewModel.updateSelectedPeriod("All Time")
        advanceUntilIdle()

        assertEquals("All Time", viewModel.filterSettings.value.selectedPeriod)
        // Both Transactions and Dashboard/Analytics reflect both transactions
        assertEquals(2, viewModel.filteredTransactions.value.size)
        assertEquals(2, viewModel.periodFilteredTransactions.value.size)
        assertEquals(500.0, viewModel.dashboardMetrics.value.totalExpense, 0.01)

        // 3. Changing to Custom Range
        viewModel.updateCustomDateRange("2026-08-01", "2026-08-31")
        advanceUntilIdle()

        assertEquals("Custom Range", viewModel.filterSettings.value.selectedPeriod)
        assertEquals(1, viewModel.filteredTransactions.value.size)
        assertEquals("tx_aug", viewModel.filteredTransactions.value[0].id)
        assertEquals(1, viewModel.periodFilteredTransactions.value.size)
        assertEquals(200.0, viewModel.dashboardMetrics.value.totalExpense, 0.01)
    }

    @Test
    fun testTransactionsScreenHasNoPeriodSelectorChips() {
        val sampleTx = TransactionEntity(
            id = "tx1",
            date = "2026-08-01",
            description = "Supermarket",
            amountRON = 150.0,
            amountEUR = 30.14,
            exchangeRate = 4.9765,
            exchangeRateDate = "2026-08-01",
            type = "Expense",
            account = "Checking",
            category = "Food",
            subCategory = "Groceries"
        )

        composeTestRule.setContent {
            FinTrackTheme {
                TransactionsScreen(
                    transactions = listOf(sampleTx),
                    categories = emptyList(),
                    filterSettings = FilterSettings(selectedPeriod = "Last Month"),
                    onCurrencyChanged = {},
                    onTypeFilterSelected = {},
                    onCategoryFilterSelected = { _, _ -> },
                    onSearchQueryChanged = {},
                    onAddTransactionClicked = {},
                    onDuplicateClicked = {},
                    onEditClicked = {},
                    onDeleteClicked = {}
                )
            }
        }

        // TransactionsScreen should display Search input and FAB
        composeTestRule.onNodeWithTag("search_transactions_input").assertIsDisplayed()
        composeTestRule.onNodeWithTag("fab_add_transaction").assertIsDisplayed()

        // Period chips should NOT exist on TransactionsScreen
        composeTestRule.onNodeWithTag("period_chip_Last_Month").assertDoesNotExist()
        composeTestRule.onNodeWithTag("period_chip_All_Time").assertDoesNotExist()
        composeTestRule.onNodeWithTag("period_selector_dropdown").assertDoesNotExist()
    }

    @Test
    fun testAnalyticsScreenHasNoPeriodSelectorChips() {
        composeTestRule.setContent {
            FinTrackTheme {
                AnalyticsScreen(
                    metrics = DashboardMetrics(),
                    filterSettings = FilterSettings(selectedPeriod = "Last Month"),
                    categoryExpenseShares = emptyList(),
                    categoryIncomeShares = emptyList(),
                    monthlyDataPoints = emptyList(),
                    insights = SmartFinancialInsights(),
                    onCurrencyChanged = {}
                )
            }
        }

        // Currency toggles and smart insights card should be present
        composeTestRule.onNodeWithTag("currency_toggle_RON").assertIsDisplayed()
        composeTestRule.onNodeWithTag("currency_toggle_EUR").assertIsDisplayed()
        composeTestRule.onNodeWithTag("analytics_smart_insights_card").assertIsDisplayed()

        // Period chips should NOT exist on AnalyticsScreen
        composeTestRule.onNodeWithTag("period_chip_Last_Month").assertDoesNotExist()
        composeTestRule.onNodeWithTag("period_chip_All_Time").assertDoesNotExist()
        composeTestRule.onNodeWithTag("period_selector_dropdown").assertDoesNotExist()
    }

    @Test
    fun testDashboardScreenRetainsFinTrackPeriodDropdown() {
        composeTestRule.setContent {
            FinTrackTheme {
                DashboardScreen(
                    metrics = DashboardMetrics(),
                    filterSettings = FilterSettings(selectedPeriod = "Last Month"),
                    monthlyDataPoints = emptyList(),
                    categoryShares = emptyList(),
                    smartInsights = SmartFinancialInsights(),
                    onPeriodSelected = {},
                    onCurrencyChanged = {}
                )
            }
        }

        // Dashboard MUST retain its FinTrackPeriodDropdown and currency toggles
        composeTestRule.onNodeWithTag("period_selector_dropdown").assertIsDisplayed()
        composeTestRule.onNodeWithTag("currency_toggle_RON").assertIsDisplayed()
        composeTestRule.onNodeWithTag("currency_toggle_EUR").assertIsDisplayed()
        composeTestRule.onNodeWithTag("dashboard_top_card").assertIsDisplayed()
    }
}
