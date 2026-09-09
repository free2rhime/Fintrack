package com.example

import android.app.Application
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.dao.ExchangeRateDao
import com.example.data.db.FinTrackDatabase
import com.example.data.model.ExchangeRateEntity
import com.example.data.model.FilterSettings
import com.example.data.model.TransactionEntity
import com.example.data.repository.AuthRepository
import com.example.data.repository.AuthState
import com.example.data.repository.RoomCategoryRepository
import com.example.data.repository.RoomTransactionRepository
import com.example.data.service.ExchangeRateService
import com.example.domain.analytics.FinancialAnalyticsEngine
import com.example.ui.MainViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.UUID

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class Checkpoint41ADataContractTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private lateinit var db: FinTrackDatabase
    private lateinit var exchangeRateDao: ExchangeRateDao
    private lateinit var application: Application

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        application = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(application, FinTrackDatabase::class.java)
            .allowMainThreadQueries()
            .setQueryExecutor(Runnable::run)
            .setTransactionExecutor(Runnable::run)
            .build()
        exchangeRateDao = db.exchangeRateDao()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        db.close()
    }

    private fun createTx(
        id: String = UUID.randomUUID().toString(),
        type: String,
        amountRON: Double,
        amountEUR: Double,
        conversionStatus: String = "OFFICIAL",
        exchangeRateSource: String = "BNR_OFFICIAL",
        exchangeRate: Double = 5.0,
        date: String = "2026-03-01",
        category: String = "General"
    ): TransactionEntity {
        return TransactionEntity(
            id = id,
            date = date,
            description = "Test Tx $id",
            amountRON = amountRON,
            amountEUR = amountEUR,
            exchangeRate = exchangeRate,
            exchangeRateDate = date,
            type = type,
            account = "Card",
            category = category,
            subCategory = "General",
            conversionStatus = conversionStatus,
            exchangeRateSource = exchangeRateSource
        )
    }

    // 1. RON primary -> EUR secondary
    @Test
    fun testRonPrimaryToEurSecondary() {
        val txs = listOf(
            createTx(type = "Income", amountRON = 1000.0, amountEUR = 200.0),
            createTx(type = "Expense", amountRON = 400.0, amountEUR = 80.0)
        )

        val metrics = FinancialAnalyticsEngine.calculateMetrics(
            transactions = txs,
            currency = "RON",
            periodLabel = "Last Month"
        )

        assertEquals("RON", metrics.currency)
        assertEquals("EUR", metrics.secondaryCurrency)
        assertEquals(1000.0, metrics.totalIncome, 0.01)
        assertEquals(400.0, metrics.totalExpense, 0.01)
        assertEquals(600.0, metrics.balance, 0.01)
        assertNotNull(metrics.secondaryCurrencyBalance)
        assertEquals(120.0, metrics.secondaryCurrencyBalance!!, 0.01) // 200.0 - 80.0
        assertFalse(metrics.hasIncompleteEurData)
        assertEquals(0, metrics.excludedNonOfficialCount)
    }

    // 2. EUR primary -> RON secondary
    @Test
    fun testEurPrimaryToRonSecondary() {
        val txs = listOf(
            createTx(type = "Income", amountRON = 1000.0, amountEUR = 200.0),
            createTx(type = "Expense", amountRON = 400.0, amountEUR = 80.0)
        )

        val metrics = FinancialAnalyticsEngine.calculateMetrics(
            transactions = txs,
            currency = "EUR",
            periodLabel = "Last Month"
        )

        assertEquals("EUR", metrics.currency)
        assertEquals("RON", metrics.secondaryCurrency)
        assertEquals(200.0, metrics.totalIncome, 0.01)
        assertEquals(80.0, metrics.totalExpense, 0.01)
        assertEquals(120.0, metrics.balance, 0.01)
        assertNotNull(metrics.secondaryCurrencyBalance)
        assertEquals(600.0, metrics.secondaryCurrencyBalance!!, 0.01) // 1000.0 - 400.0
        assertFalse(metrics.hasIncompleteEurData)
        assertEquals(0, metrics.excludedNonOfficialCount)
    }

    // 3. Secondary balance is null when authoritative data is incomplete
    @Test
    fun testSecondaryBalanceIsNullWhenAuthoritativeDataIsIncomplete() {
        val txs = listOf(
            createTx(type = "Income", amountRON = 1000.0, amountEUR = 200.0, conversionStatus = "OFFICIAL"),
            createTx(type = "Expense", amountRON = 400.0, amountEUR = 0.0, conversionStatus = "PENDING", exchangeRate = 0.0)
        )

        // In RON mode: EUR data is incomplete, secondary balance MUST be null
        val metricsRon = FinancialAnalyticsEngine.calculateMetrics(
            transactions = txs,
            currency = "RON",
            periodLabel = "Last Month"
        )
        assertEquals(600.0, metricsRon.balance, 0.01)
        assertEquals("EUR", metricsRon.secondaryCurrency)
        assertNull(metricsRon.secondaryCurrencyBalance)
        assertFalse(metricsRon.hasIncompleteEurData) // RON mode itself includes all transactions

        // In EUR mode: non-official tx is excluded, secondary balance MUST be null
        val metricsEur = FinancialAnalyticsEngine.calculateMetrics(
            transactions = txs,
            currency = "EUR",
            periodLabel = "Last Month"
        )
        assertEquals(200.0, metricsEur.balance, 0.01)
        assertEquals("RON", metricsEur.secondaryCurrency)
        assertNull(metricsEur.secondaryCurrencyBalance)
        assertTrue(metricsEur.hasIncompleteEurData)
        assertEquals(1, metricsEur.excludedNonOfficialCount)
    }

    // 4. Latest BNR rate is exposed when available
    @Test
    fun testLatestBnrRateExposedWhenAvailable() {
        val metrics = FinancialAnalyticsEngine.calculateMetrics(
            transactions = emptyList(),
            currency = "RON",
            periodLabel = "Last Month",
            latestBnrRate = 4.9765,
            effectiveBnrDate = "2026-03-09",
            bnrStatus = "OFFICIAL"
        )

        assertEquals(4.9765, metrics.latestBnrRate!!, 0.0001)
        assertEquals("2026-03-09", metrics.effectiveBnrDate)
        assertEquals("OFFICIAL", metrics.bnrStatus)
    }

    // 5. Latest BNR rate is null when unavailable
    @Test
    fun testLatestBnrRateIsNullWhenUnavailable() {
        val metrics = FinancialAnalyticsEngine.calculateMetrics(
            transactions = emptyList(),
            currency = "RON",
            periodLabel = "Last Month",
            latestBnrRate = null,
            effectiveBnrDate = null,
            bnrStatus = null
        )

        assertNull(metrics.latestBnrRate)
        assertNull(metrics.effectiveBnrDate)
        assertNull(metrics.bnrStatus)
    }

    // 6. Latest rate does not replace historical transaction-date rates
    @Test
    fun testLatestRateDoesNotReplaceHistoricalTransactionDateRates() {
        // Two transactions with different historical dates and rates
        val tx1 = createTx(
            type = "Expense",
            amountRON = 500.0,
            amountEUR = 100.0, // Historical rate = 5.0000
            exchangeRate = 5.0,
            date = "2026-01-10"
        )
        val tx2 = createTx(
            type = "Expense",
            amountRON = 490.0,
            amountEUR = 100.0, // Historical rate = 4.9000
            exchangeRate = 4.9,
            date = "2026-02-10"
        )
        val txs = listOf(tx1, tx2)

        // Assume latest BNR rate today is 4.8000
        val metricsRon = FinancialAnalyticsEngine.calculateMetrics(
            transactions = txs,
            currency = "RON",
            periodLabel = "All Time",
            latestBnrRate = 4.8000,
            effectiveBnrDate = "2026-03-09",
            bnrStatus = "OFFICIAL"
        )

        // Primary expense: 500.0 + 490.0 = 990.0 RON
        assertEquals(990.0, metricsRon.totalExpense, 0.01)
        assertEquals(-990.0, metricsRon.balance, 0.01)

        // Secondary EUR balance must use historical amounts: -(100.0 + 100.0) = -200.0 EUR
        // It must NOT be -990.0 / 4.8000 = -206.25 EUR
        assertEquals(-200.0, metricsRon.secondaryCurrencyBalance!!, 0.01)
        assertEquals(4.8000, metricsRon.latestBnrRate!!, 0.0001)

        // In EUR mode:
        val metricsEur = FinancialAnalyticsEngine.calculateMetrics(
            transactions = txs,
            currency = "EUR",
            periodLabel = "All Time",
            latestBnrRate = 4.8000,
            effectiveBnrDate = "2026-03-09",
            bnrStatus = "OFFICIAL"
        )
        assertEquals(200.0, metricsEur.totalExpense, 0.01)
        assertEquals(-200.0, metricsEur.balance, 0.01)
        // Secondary RON balance must be -990.0 RON (NOT -200.0 * 4.8 = -960.0 RON)
        assertEquals(-990.0, metricsEur.secondaryCurrencyBalance!!, 0.01)
    }

    // 7. Existing financial metrics remain unchanged
    @Test
    fun testExistingFinancialMetricsRemainUnchanged() {
        val txs = listOf(
            createTx(type = "Income", amountRON = 2000.0, amountEUR = 400.0, category = "Salary"),
            createTx(type = "Expense", amountRON = 500.0, amountEUR = 100.0, category = "Groceries"),
            createTx(type = "Expense", amountRON = 300.0, amountEUR = 60.0, category = "Utilities")
        )

        val metrics = FinancialAnalyticsEngine.calculateMetrics(
            transactions = txs,
            currency = "RON",
            periodLabel = "Last Month"
        )

        assertEquals(2000.0, metrics.totalIncome, 0.01)
        assertEquals(800.0, metrics.totalExpense, 0.01)
        assertEquals(1200.0, metrics.balance, 0.01)
        assertEquals(60.0, metrics.savingsRate, 0.01) // (2000 - 800) / 2000 * 100 = 60%
        assertEquals(40.0, metrics.expensePressure, 0.01) // 800 / 2000 * 100 = 40%
        assertEquals("Groceries", metrics.topExpenseCategory)
        assertEquals(500.0, metrics.topExpenseCategoryAmount, 0.01)
        assertEquals(62.5, metrics.categoryConcentrationPercent, 0.01) // 500 / 800 * 100 = 62.5%
        assertEquals(3, metrics.transactionCount)
    }

    // 8. Selected currency correctly determines secondary currency
    @Test
    fun testSelectedCurrencyCorrectlyDeterminesSecondaryCurrency() {
        val metricsRon = FinancialAnalyticsEngine.calculateMetrics(emptyList(), "RON", "All Time")
        assertEquals("EUR", metricsRon.secondaryCurrency)

        val metricsEur = FinancialAnalyticsEngine.calculateMetrics(emptyList(), "EUR", "All Time")
        assertEquals("RON", metricsEur.secondaryCurrency)
    }

    // 9. Chronological monthly sorting from Checkpoint 1 remains intact
    @Test
    fun testMonthlyDataPointsChronologicalSortingPreserved() {
        val txList = listOf(
            createTx(date = "2026-05-10", type = "Income", amountRON = 1000.0, amountEUR = 200.0),
            createTx(date = "2026-01-15", type = "Income", amountRON = 1200.0, amountEUR = 240.0),
            createTx(date = "2026-03-20", type = "Income", amountRON = 1100.0, amountEUR = 220.0)
        )

        val monthly = FinancialAnalyticsEngine.calculateMonthlyDataPoints(txList, "RON")
        assertEquals(3, monthly.size)
        assertEquals("Jan 2026", monthly[0].monthYearLabel)
        assertEquals("Mar 2026", monthly[1].monthYearLabel)
        assertEquals("May 2026", monthly[2].monthYearLabel)
    }

    // 10. Room ExchangeRateDao getLatestOfficialRate query logic
    @Test
    fun testRoomExchangeRateDaoLatestOfficialRateQuery() = runTest {
        // Insert multiple exchange rates: older, newer, pending, unverified
        val rate1 = ExchangeRateEntity(
            date = "2026-03-01",
            requestedDate = "2026-03-01",
            effectiveDate = "2026-03-01",
            rate = 4.9700,
            source = "BNR_OFFICIAL",
            status = "OFFICIAL"
        )
        val rate2 = ExchangeRateEntity(
            date = "2026-03-05",
            requestedDate = "2026-03-05",
            effectiveDate = "2026-03-05",
            rate = 4.9750,
            source = "BNR_OFFICIAL",
            status = "OFFICIAL"
        )
        val ratePending = ExchangeRateEntity(
            date = "2026-03-08",
            requestedDate = "2026-03-08",
            effectiveDate = "2026-03-08",
            rate = 4.9800,
            source = "BNR_OFFICIAL",
            status = "PENDING"
        )
        val rateUnverified = ExchangeRateEntity(
            date = "2026-03-09",
            requestedDate = "2026-03-09",
            effectiveDate = "2026-03-09",
            rate = 4.9900,
            source = "SYNTHETIC",
            status = "UNVERIFIED"
        )

        exchangeRateDao.insertAllRates(listOf(rate1, rate2, ratePending, rateUnverified))

        val latest = exchangeRateDao.getLatestOfficialRate()
        assertNotNull(latest)
        // Must select the latest OFFICIAL rate (rate2 on 2026-03-05), ignoring pending/unverified
        assertEquals("2026-03-05", latest!!.effectiveDate)
        assertEquals(4.9750, latest.rate, 0.0001)
        assertEquals("OFFICIAL", latest.status)
        assertEquals("BNR_OFFICIAL", latest.source)

        // Now insert a newer official rate for 2026-03-07
        val rate3 = ExchangeRateEntity(
            date = "2026-03-07",
            requestedDate = "2026-03-07",
            effectiveDate = "2026-03-07",
            rate = 4.9765,
            source = "BNR_OFFICIAL",
            status = "OFFICIAL"
        )
        exchangeRateDao.insertRate(rate3)

        val updatedLatest = exchangeRateDao.getLatestOfficialRate()
        assertNotNull(updatedLatest)
        assertEquals("2026-03-07", updatedLatest!!.effectiveDate)
        assertEquals(4.9765, updatedLatest.rate, 0.0001)
    }

    // 11. ViewModel integration with reactive latest BNR rate
    @Test
    fun testViewModelExposesAuthoritativeBnrRateAndDashboardMetrics() = testScope.runTest {
        val exchangeRateService = ExchangeRateService(
            exchangeRateDao = exchangeRateDao,
            syncOutboxDao = db.syncOutboxDao(),
            database = db
        )

        val transactionRepo = RoomTransactionRepository(
            transactionDao = db.transactionDao(),
            exchangeRateService = exchangeRateService,
            exchangeRateDao = exchangeRateDao,
            database = db
        )

        val categoryRepo = RoomCategoryRepository(
            categoryDao = db.categoryDao(),
            syncOutboxDao = db.syncOutboxDao(),
            database = db
        )

        val authRepo = FakeTestAuthRepository("test_user_1")
        val settingsRepo = com.example.data.repository.DataStoreSettingsRepository(application)

        val viewModel = MainViewModel(
            transactionRepository = transactionRepo,
            categoryRepository = categoryRepo,
            settingsRepository = settingsRepo,
            authRepository = authRepo,
            database = db,
            ioDispatcher = testDispatcher,
            application = application
        )

        backgroundScope.launch { viewModel.dashboardMetrics.collect {} }
        backgroundScope.launch { viewModel.latestOfficialBnrRate.collect {} }
        advanceUntilIdle()

        // Initially no rates in DB
        assertNull(viewModel.latestOfficialBnrRate.value)
        assertNull(viewModel.dashboardMetrics.value.latestBnrRate)

        // Insert official BNR rate into DB
        val rateEntity = ExchangeRateEntity(
            date = "2026-03-09",
            requestedDate = "2026-03-09",
            effectiveDate = "2026-03-09",
            rate = 4.9765,
            source = "BNR_OFFICIAL",
            status = "OFFICIAL"
        )
        exchangeRateDao.insertRate(rateEntity)
        advanceUntilIdle()

        // Verify ViewModel receives and propagates latest rate
        assertEquals(4.9765, viewModel.dashboardMetrics.value.latestBnrRate ?: 0.0, 0.0001)
        assertEquals("2026-03-09", viewModel.dashboardMetrics.value.effectiveBnrDate)
        assertEquals("OFFICIAL", viewModel.dashboardMetrics.value.bnrStatus)
        assertEquals(4.9765, viewModel.latestOfficialBnrRate.value?.rate ?: 0.0, 0.0001)
    }
}
