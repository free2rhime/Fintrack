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
import com.example.data.repository.PreparedRepairItem
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
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

import com.example.data.util.CsvExporter
import com.example.data.util.CsvDuplicateMode
import com.example.data.util.CsvImportFinalResult
import com.example.data.util.CsvImporter
import com.example.data.util.CsvPreviewData
import kotlinx.coroutines.Dispatchers
import java.io.File

import com.example.data.repository.PendingRetryResult

data class DiscrepancyItem(
    val transactionId: String,
    val date: String,
    val description: String,
    val amountRON: Double,
    val oldRate: Double,
    val correctRate: Double,
    val effectiveBnrDate: String,
    val oldAmountEUR: Double,
    val correctAmountEUR: Double,
    val differenceEUR: Double
)

data class DiscrepancyReport(
    val items: List<DiscrepancyItem>,
    val totalDiscrepancyEUR: Double,
    val backupFilePath: String? = null
)

data class MainUiState(
    val selectedTab: Int = 0, // 0: Dashboard, 1: Transactions, 2: Analytics, 3: Categories, 4: Settings
    val activeTransactionForEdit: TransactionEntity? = null,
    val showTransactionDialog: Boolean = false,
    val isDuplicateMode: Boolean = false,
    val showCategoryDialog: Boolean = false,
    val userNotification: String? = null,
    val discrepancyReport: DiscrepancyReport? = null,
    val isAuditingHistoricalRates: Boolean = false,
    val csvPreviewData: CsvPreviewData? = null,
    val csvImportFinalResult: CsvImportFinalResult? = null,
    val pendingRetryResult: PendingRetryResult? = null,
    val isRetryingPending: Boolean = false,
    val debugDiagnosticResult: com.example.data.service.BnrDiagnosticResult? = null
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val database = FinTrackDatabase.getDatabase(application)
    private val exchangeRateService = ExchangeRateService(database.exchangeRateDao())
    val transactionRepository = TransactionRepository(
        transactionDao = database.transactionDao(),
        exchangeRateService = exchangeRateService,
        exchangeRateDao = database.exchangeRateDao(),
        database = database
    )
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
            try {
                val savedTx = transactionRepository.saveTransaction(
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
                if (savedTx.conversionStatus == "OFFICIAL") {
                    showNotification("Transaction saved successfully")
                } else {
                    showNotification("Transaction saved. EUR rate pending (${savedTx.conversionStatus}).")
                }
            } catch (e: Exception) {
                showNotification("Failed to save transaction: ${e.message?.take(100)}")
            }
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

    suspend fun getDescriptionSuggestions(query: String): List<String> {
        return transactionRepository.getDescriptionSuggestions(query)
    }

    fun updateSelectedTypeFilter(type: String) {
        viewModelScope.launch {
            // Check if active category is invalid for new type
            val currentSettings = filterSettings.value
            val activeCat = currentSettings.selectedExpenseCategory ?: currentSettings.selectedIncomeCategory
            val availableCats = categories.value.filter { if (type == "All") true else it.type == type }
            if (activeCat != null && availableCats.none { it.name.equals(activeCat, ignoreCase = true) || it.subCategory.equals(activeCat, ignoreCase = true) }) {
                settingsRepository.updateCategoryFilter(type, null)
            }
            settingsRepository.updateSelectedType(type)
        }
    }

    fun updateCategoryFilter(type: String, categoryName: String?) {
        viewModelScope.launch {
            settingsRepository.updateCategoryFilter(type, categoryName)
        }
    }

    fun importCsv(context: android.content.Context, uri: android.net.Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val csvContent = inputStream?.bufferedReader()?.use { it.readText() } ?: ""
                if (csvContent.isBlank()) {
                    showNotification("CSV file is empty or could not be read.")
                    return@launch
                }

                val allExistingTxs = transactionRepository.getAllTransactionsList()
                val currentCategories = categoryRepository.getAllCategoriesList()

                val preview = CsvImporter.parseAndValidate(
                    csvContent = csvContent,
                    existingTransactions = allExistingTxs,
                    existingCategories = currentCategories,
                    duplicateMode = CsvDuplicateMode.SKIP_EXISTING
                )

                _uiState.value = _uiState.value.copy(csvPreviewData = preview)
            } catch (e: Exception) {
                showNotification("Import error: ${e.message}")
            }
        }
    }

    fun updateCsvDuplicateMode(mode: CsvDuplicateMode) {
        val currentPreview = _uiState.value.csvPreviewData ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val allExistingTxs = transactionRepository.getAllTransactionsList()
            val currentCategories = categoryRepository.getAllCategoriesList()

            val updatedPreview = CsvImporter.parseAndValidate(
                csvContent = currentPreview.rawCsvContent,
                existingTransactions = allExistingTxs,
                existingCategories = currentCategories,
                duplicateMode = mode
            )

            _uiState.value = _uiState.value.copy(csvPreviewData = updatedPreview)
        }
    }

    fun executeCsvImport(context: android.content.Context) {
        val preview = _uiState.value.csvPreviewData ?: return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val allExistingTxs = transactionRepository.getAllTransactionsList()
                val backupFile = File(context.cacheDir, "fintrack_pre_import_backup_${System.currentTimeMillis()}.csv")

                val result = CsvImporter.executeAtomicImport(
                    database = database,
                    previewData = preview,
                    backupFile = backupFile,
                    allExistingTransactions = allExistingTxs
                )

                _uiState.value = _uiState.value.copy(
                    csvPreviewData = null,
                    csvImportFinalResult = result
                )

                if (result.success) {
                    showNotification("Import complete: ${result.insertedCount} inserted, ${result.updatedCount} updated, ${result.skippedCount} skipped.")
                } else {
                    showNotification("Import failed: ${result.errorMessage}")
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    csvPreviewData = null,
                    csvImportFinalResult = CsvImportFinalResult(
                        success = false,
                        insertedCount = 0,
                        updatedCount = 0,
                        skippedCount = 0,
                        failedCount = preview.validTransactionsToImport.size,
                        categoriesCreatedCount = 0,
                        subcategoriesCreatedCount = 0,
                        pendingCount = 0,
                        unverifiedCount = 0,
                        errorMessage = "Execution error: ${e.message}"
                    )
                )
            }
        }
    }

    fun dismissCsvPreview() {
        _uiState.value = _uiState.value.copy(csvPreviewData = null)
    }

    fun dismissCsvResult() {
        _uiState.value = _uiState.value.copy(csvImportFinalResult = null)
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

    fun updateCategoryGroup(oldName: String, newName: String, type: String) {
        viewModelScope.launch {
            categoryRepository.updateCategoryGroup(oldName, newName, type)
            showNotification("Category group updated")
        }
    }

    fun deleteCategoryGroup(name: String, type: String) {
        viewModelScope.launch {
            categoryRepository.deleteCategoryGroup(name, type)
            showNotification("Category group deleted")
        }
    }

    fun updateSubcategory(id: String, newSubCategory: String) {
        viewModelScope.launch {
            categoryRepository.updateSubcategory(id, newSubCategory)
            showNotification("Subcategory updated")
        }
    }

    fun deleteSubcategory(id: String) {
        viewModelScope.launch {
            categoryRepository.deleteSubcategory(id)
            showNotification("Subcategory deleted")
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

    fun generateDiscrepancyReport() {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.value = _uiState.value.copy(isAuditingHistoricalRates = true)
            val unverified = transactionRepository.getUnverifiedTransactions()
            val items = mutableListOf<DiscrepancyItem>()

            for (tx in unverified) {
                val bnrResult = exchangeRateService.getOfficialRate(tx.date)
                if (bnrResult.status == "OFFICIAL" && bnrResult.rate > 0.0) {
                    val correctEUR = ExchangeRateService.calculateAmountEUR(tx.amountRON, bnrResult.rate)
                    val diff = kotlin.math.abs(correctEUR - tx.amountEUR)
                    items.add(
                        DiscrepancyItem(
                            transactionId = tx.id,
                            date = tx.date,
                            description = tx.description,
                            amountRON = tx.amountRON,
                            oldRate = tx.exchangeRate,
                            correctRate = bnrResult.rate,
                            effectiveBnrDate = bnrResult.effectiveDate,
                            oldAmountEUR = tx.amountEUR,
                            correctAmountEUR = correctEUR,
                            differenceEUR = Math.round(diff * 100.0) / 100.0
                        )
                    )
                }
            }

            // Create backup CSV before applying corrections
            val allTxs = transactionRepository.getAllTransactionsList()
            val backupFile = File(getApplication<Application>().cacheDir, "fintrack_backup_before_repair.csv")
            CsvExporter.writeTransactionsToFile(backupFile, allTxs)

            val totalDiff = items.sumOf { it.differenceEUR }
            val report = DiscrepancyReport(
                items = items,
                totalDiscrepancyEUR = Math.round(totalDiff * 100.0) / 100.0,
                backupFilePath = backupFile.absolutePath
            )

            _uiState.value = _uiState.value.copy(
                isAuditingHistoricalRates = false,
                discrepancyReport = report
            )
        }
    }

    private val isSyncingPending = java.util.concurrent.atomic.AtomicBoolean(false)

    fun confirmAndApplyRepair() {
        val report = uiState.value.discrepancyReport ?: return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val allTxs = transactionRepository.getAllTransactionsList()
                val backupFile = File(getApplication<Application>().cacheDir, "fintrack_backup_before_repair.csv")

                CsvExporter.writeTransactionsToFile(backupFile, allTxs)

                // Perform strict 6-point backup validation
                if (!validateBackupFile(backupFile, allTxs.size)) {
                    _uiState.value = _uiState.value.copy(
                        discrepancyReport = null,
                        userNotification = "Repair aborted: CSV backup validation failed."
                    )
                    return@launch
                }

                val preparedItems = mutableListOf<PreparedRepairItem>()
                for (item in report.items) {
                    val tx = transactionRepository.getTransactionById(item.transactionId) ?: continue
                    preparedItems.add(PreparedRepairItem(tx, item.correctRate, item.effectiveBnrDate))
                }

                val updatedCount = transactionRepository.applyRepairBatch(preparedItems)

                _uiState.value = _uiState.value.copy(
                    discrepancyReport = null,
                    userNotification = "Applied official BNR rates to $updatedCount transaction(s)."
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    discrepancyReport = null,
                    userNotification = "Error applying repair: ${e.message}"
                )
            }
        }
    }

    fun validateBackupFile(file: File, expectedCount: Int): Boolean {
        try {
            if (!file.exists()) return false
            if (!file.canRead()) return false
            if (file.length() <= 0) return false

            val lines = file.readLines()
            if (lines.isEmpty()) return false

            val header = lines.first()
            if (!header.startsWith("Transaction_ID,Transaction_Date,Amount_RON,Amount_EUR")) return false

            val dataRows = lines.drop(1).filter { it.isNotBlank() }
            if (dataRows.size < expectedCount) return false

            return true
        } catch (e: Exception) {
            return false
        }
    }

    fun dismissDiscrepancyReport() {
        _uiState.value = _uiState.value.copy(discrepancyReport = null)
    }

    fun syncPendingConversions() {
        retryPendingConversions(showResultDialog = false)
    }

    fun retryPendingConversions(showResultDialog: Boolean = true) {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.value = _uiState.value.copy(isRetryingPending = true)
            try {
                val result = transactionRepository.syncPendingConversions()
                _uiState.value = _uiState.value.copy(
                    isRetryingPending = false,
                    pendingRetryResult = if (showResultDialog) result else null
                )
                if (result.convertedSuccessfully > 0) {
                    showNotification("Converted ${result.convertedSuccessfully} pending EUR transactions with BNR")
                }
            } catch (e: Exception) {
                showNotification("Error syncing pending conversions: ${e.message?.take(100)}")
            } finally {
                _uiState.value = _uiState.value.copy(isRetryingPending = false)
            }
        }
    }

    fun dismissRetryResultDialog() {
        _uiState.value = _uiState.value.copy(pendingRetryResult = null)
    }

    fun runBnrDiagnostic() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val result = exchangeRateService.runDebugDiagnostic()
                _uiState.value = _uiState.value.copy(debugDiagnosticResult = result)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    debugDiagnosticResult = com.example.data.service.BnrDiagnosticResult(
                        isReachable = false,
                        httpStatus = "EXCEPTION",
                        failureCategory = "EXCEPTION",
                        sanitizedPreview = "Exception: ${e.javaClass.simpleName}: ${e.message?.take(100)}"
                    )
                )
            }
        }
    }

    fun dismissDebugDiagnostic() {
        _uiState.value = _uiState.value.copy(debugDiagnosticResult = null)
    }

    fun dismissNotification() {
        _uiState.value = _uiState.value.copy(userNotification = null)
    }

    private fun showNotification(msg: String) {
        _uiState.value = _uiState.value.copy(userNotification = msg)
    }
}
