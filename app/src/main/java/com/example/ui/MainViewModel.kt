package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.FinTrackDatabase
import com.example.data.model.CategoryEntity
import com.example.data.model.FilterSettings
import com.example.data.model.TransactionEntity
import com.example.data.repository.CategoryRepository
import com.example.data.repository.SettingsRepository
import com.example.data.repository.TransactionRepository
import com.example.data.service.ExchangeRateService
import com.example.data.util.SampleDataSeeder
import com.example.domain.analytics.CategoryExpenseShare
import com.example.domain.analytics.DashboardMetrics
import com.example.domain.analytics.FinancialAnalyticsEngine
import com.example.domain.analytics.MonthlyDataPoint
import com.example.domain.analytics.SmartFinancialInsights
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class MainUiState(
    val selectedTab: Int = 0, // 0: Dashboard, 1: Transactions, 2: Analytics, 3: Categories, 4: Settings
    val activeTransactionForEdit: TransactionEntity? = null,
    val showTransactionDialog: Boolean = false,
    val isDuplicateMode: Boolean = false,
    val showCategoryDialog: Boolean = false,
    val userNotification: String? = null
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val database = FinTrackDatabase.getDatabase(application)
    private val exchangeRateService = ExchangeRateService(database.exchangeRateDao())
    val transactionRepository = TransactionRepository(database.transactionDao(), exchangeRateService)
    val categoryRepository = CategoryRepository(database.categoryDao())
    val settingsRepository = SettingsRepository(application)

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    val themeMode: StateFlow<String> = settingsRepository.themeModeFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = "dark"
    )

    val filterSettings: StateFlow<FilterSettings> = settingsRepository.filterSettingsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = FilterSettings()
    )

    val categories: StateFlow<List<CategoryEntity>> = categoryRepository.allCategories.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val allTransactions: StateFlow<List<TransactionEntity>> = transactionRepository.allTransactions.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Filtered transactions for Transactions screen (reacting to period, search, and category filter)
    val filteredTransactions: StateFlow<List<TransactionEntity>> = combine(
        allTransactions,
        filterSettings
    ) { txs, settings ->
        FinancialAnalyticsEngine.filterTransactionsByPeriod(txs, settings, ignoreCategoryFilter = false)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Period-filtered transactions for Dashboard & Analytics (ignores category filter)
    val periodFilteredTransactions: StateFlow<List<TransactionEntity>> = combine(
        allTransactions,
        filterSettings
    ) { txs, settings ->
        FinancialAnalyticsEngine.filterTransactionsByPeriod(txs, settings, ignoreCategoryFilter = true)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Dashboard Metrics
    val dashboardMetrics: StateFlow<DashboardMetrics> = combine(
        periodFilteredTransactions,
        filterSettings
    ) { txs, settings ->
        FinancialAnalyticsEngine.calculateMetrics(
            transactions = txs,
            currency = settings.selectedCurrency,
            periodLabel = settings.selectedPeriod
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DashboardMetrics()
    )

    // Category Expense Distribution
    val categoryExpenseShares: StateFlow<List<CategoryExpenseShare>> = combine(
        periodFilteredTransactions,
        filterSettings
    ) { txs, settings ->
        FinancialAnalyticsEngine.calculateCategoryShares(txs, settings.selectedCurrency, "Expense")
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Category Income Distribution
    val categoryIncomeShares: StateFlow<List<CategoryExpenseShare>> = combine(
        periodFilteredTransactions,
        filterSettings
    ) { txs, settings ->
        FinancialAnalyticsEngine.calculateCategoryShares(txs, settings.selectedCurrency, "Income")
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Monthly Data Points
    val monthlyDataPoints: StateFlow<List<MonthlyDataPoint>> = combine(
        periodFilteredTransactions,
        filterSettings
    ) { txs, settings ->
        FinancialAnalyticsEngine.calculateMonthlyDataPoints(txs, settings.selectedCurrency)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Smart Insights
    val smartInsights: StateFlow<SmartFinancialInsights> = combine(
        periodFilteredTransactions,
        filterSettings
    ) { txs, settings ->
        FinancialAnalyticsEngine.calculateSmartInsights(txs, settings.selectedCurrency)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SmartFinancialInsights()
    )

    init {
        viewModelScope.launch {
            categoryRepository.ensureDefaultCategoriesSeeded()
            val existingTxs = allTransactions.first()
            if (existingTxs.isEmpty()) {
                SampleDataSeeder.seedInitialTransactionsIfEmpty(transactionRepository)
            }
        }
    }

    fun selectTab(index: Int) {
        _uiState.value = _uiState.value.copy(selectedTab = index)
    }

    fun openNewTransactionDialog(defaultType: String = "Expense") {
        _uiState.value = _uiState.value.copy(
            activeTransactionForEdit = null,
            showTransactionDialog = true,
            isDuplicateMode = false
        )
    }

    fun openEditTransactionDialog(transaction: TransactionEntity) {
        _uiState.value = _uiState.value.copy(
            activeTransactionForEdit = transaction,
            showTransactionDialog = true,
            isDuplicateMode = false
        )
    }

    /**
     * DUPLICATE TRANSACTION ACTION
     * Copies selected transaction, sets date to current date, recomputes historical rate,
     * opens transaction form with duplicate prefilled data.
     */
    fun openDuplicateTransactionDialog(transaction: TransactionEntity) {
        viewModelScope.launch {
            val duplicateTemplate = transactionRepository.createDuplicateTemplate(transaction)
            _uiState.value = _uiState.value.copy(
                activeTransactionForEdit = duplicateTemplate,
                showTransactionDialog = true,
                isDuplicateMode = true
            )
        }
    }

    fun dismissTransactionDialog() {
        _uiState.value = _uiState.value.copy(
            showTransactionDialog = false,
            activeTransactionForEdit = null,
            isDuplicateMode = false
        )
    }

    fun saveTransaction(
        id: String?,
        date: String,
        description: String,
        amountRON: Double,
        type: String,
        account: String,
        category: String,
        subCategory: String,
        destination: String?
    ) {
        viewModelScope.launch {
            transactionRepository.saveTransaction(
                id = id,
                date = date,
                description = description,
                amountRON = amountRON,
                type = type,
                account = account,
                category = category,
                subCategory = subCategory,
                destination = destination
            )
            dismissTransactionDialog()
            showNotification("Transaction saved successfully")
        }
    }

    fun deleteTransaction(transaction: TransactionEntity) {
        viewModelScope.launch {
            transactionRepository.deleteTransaction(transaction)
            showNotification("Transaction deleted")
        }
    }

    fun updateSelectedPeriod(period: String) {
        viewModelScope.launch {
            settingsRepository.updateSelectedPeriod(period)
        }
    }

    fun updateSelectedCurrency(currency: String) {
        viewModelScope.launch {
            settingsRepository.updateSelectedCurrency(currency)
        }
    }

    fun updateCustomDateRange(startDate: String, endDate: String) {
        viewModelScope.launch {
            settingsRepository.updateCustomDateRange(startDate, endDate)
        }
    }

    fun updateSearchQuery(query: String) {
        viewModelScope.launch {
            val current = filterSettings.value
            settingsRepository.updateSelectedPeriod(current.selectedPeriod) // triggers flow update if needed
        }
    }

    fun updateCategoryFilter(type: String, categoryName: String?) {
        viewModelScope.launch {
            settingsRepository.updateCategoryFilter(type, categoryName)
        }
    }

    fun addCategory(name: String, type: String, subCategory: String) {
        viewModelScope.launch {
            categoryRepository.addCategory(name, type, subCategory)
            showNotification("Category added")
        }
    }

    fun updateCategory(category: CategoryEntity) {
        viewModelScope.launch {
            categoryRepository.updateCategory(category)
            showNotification("Category updated")
        }
    }

    fun deleteCategory(category: CategoryEntity) {
        viewModelScope.launch {
            categoryRepository.deleteCategory(category)
            showNotification("Category removed")
        }
    }

    fun seedDemoData() {
        viewModelScope.launch {
            SampleDataSeeder.seedInitialTransactionsIfEmpty(transactionRepository)
            showNotification("Demo data generated!")
        }
    }

    fun resetData() {
        viewModelScope.launch {
            transactionRepository.deleteAllTransactions()
            showNotification("All transaction data cleared")
        }
    }

    fun updateThemeMode(mode: String) {
        viewModelScope.launch {
            settingsRepository.updateThemeMode(mode)
        }
    }

    fun dismissNotification() {
        _uiState.value = _uiState.value.copy(userNotification = null)
    }

    private fun showNotification(msg: String) {
        _uiState.value = _uiState.value.copy(userNotification = msg)
    }
}
