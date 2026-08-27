package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.FinTrackDatabase
import com.example.data.model.CategoryEntity
import com.example.data.model.FilterSettings
import com.example.data.model.TransactionEntity
import com.example.data.repository.AuthRepository
import com.example.data.repository.AuthState
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
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

import com.example.data.util.CsvBackupManager
import com.example.data.util.CsvExporter
import com.example.data.util.CsvDuplicateMode
import com.example.data.util.CsvImportFinalResult
import com.example.data.util.CsvImporter
import com.example.data.util.CsvPreviewData
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import java.io.File

import com.example.data.repository.PendingRetryResult

import com.example.data.repository.FirestoreSyncRepository
import com.example.data.repository.SyncStatus
import com.example.data.repository.FirestoreMigrationPreflightCoordinator
import com.example.data.repository.FirestoreMigrationUploader
import com.example.data.repository.PreflightValidationResult
import com.example.data.repository.MigrationSessionCreationResult
import com.example.data.repository.MigrationUploadResult
import com.example.data.repository.ConflictReason

import com.example.data.model.HouseholdDto
import com.example.data.model.HouseholdInviteDto
import com.example.data.model.HouseholdMemberDto
import com.example.data.repository.HouseholdRepository
import com.example.data.repository.FirestoreHouseholdRepository
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOf

sealed class MigrationUiState {
    object Idle : MigrationUiState()
    object RunningPreflight : MigrationUiState()
    data class Preview(val preview: MigrationPreviewState) : MigrationUiState()
    data class Conflict(val conflict: MigrationConflictState) : MigrationUiState()
    data class Uploading(val progress: MigrationProgressState) : MigrationUiState()
    data class Success(val result: MigrationResultState.Success) : MigrationUiState()
    data class Failure(val failure: MigrationResultState.Failure) : MigrationUiState()
}

data class MigrationPreviewState(
    val householdId: String,
    val householdName: String? = null,
    val userUid: String,
    val userRole: String,
    val transactionsCount: Int,
    val categoriesCount: Int,
    val exchangeRatesCount: Int,
    val totalRecords: Int,
    val backupBundlePath: String?,
    val backupTimestamp: Long? = null,
    val backupValidationStatus: String = "VALIDATED",
    val preflightReadyData: PreflightValidationResult.Ready? = null
)

data class MigrationProgressState(
    val stage: String,
    val processedCount: Int,
    val totalCount: Int,
    val progressFraction: Float = if (totalCount > 0) processedCount.toFloat() / totalCount.toFloat() else 0f
)

sealed class MigrationResultState {
    data class Success(
        val migrationId: String,
        val categoriesUploaded: Int,
        val ratesUploaded: Int,
        val transactionsUploaded: Int,
        val totalProcessed: Int
    ) : MigrationResultState()

    data class Failure(
        val stage: String,
        val sanitizedError: String,
        val backupBundlePath: String? = null
    ) : MigrationResultState()
}

data class MigrationConflictState(
    val reason: String,
    val details: String
)

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

sealed interface HouseholdCreationUiState {
    data object Idle : HouseholdCreationUiState
    data object Creating : HouseholdCreationUiState
    data class Success(val householdId: String) : HouseholdCreationUiState
    data class Error(val message: String) : HouseholdCreationUiState
}

class MainViewModel(
    val transactionRepository: TransactionRepository,
    val categoryRepository: CategoryRepository,
    val settingsRepository: SettingsRepository,
    val authRepository: AuthRepository,
    val syncRepository: FirestoreSyncRepository? = null,
    val preflightCoordinator: FirestoreMigrationPreflightCoordinator? = null,
    val migrationUploader: FirestoreMigrationUploader? = null,
    val householdRepository: HouseholdRepository? = null,
    val database: FinTrackDatabase? = null,
    val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    application: Application
) : AndroidViewModel(application) {

    private val activeHouseholdRepo: HouseholdRepository by lazy {
        householdRepository ?: FirestoreHouseholdRepository(authRepository = authRepository)
    }

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    private val _migrationUiState = MutableStateFlow<MigrationUiState>(MigrationUiState.Idle)
    val migrationUiState: StateFlow<MigrationUiState> = _migrationUiState.asStateFlow()

    private val _householdCreationUiState = MutableStateFlow<HouseholdCreationUiState>(HouseholdCreationUiState.Idle)
    val householdCreationUiState: StateFlow<HouseholdCreationUiState> = _householdCreationUiState.asStateFlow()

    private val _householdError = MutableStateFlow<String?>(null)
    val householdError: StateFlow<String?> = _householdError.asStateFlow()

    private val _isInvitationProcessing = MutableStateFlow(false)
    val isInvitationProcessing: StateFlow<Boolean> = _isInvitationProcessing.asStateFlow()

    private val _invitationError = MutableStateFlow<String?>(null)
    val invitationError: StateFlow<String?> = _invitationError.asStateFlow()

    val syncStatus: StateFlow<SyncStatus> = syncRepository?.syncStatusState
        ?: MutableStateFlow(SyncStatus.SignedOut)

    val authState: StateFlow<AuthState> = authRepository.authState.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = authRepository.authState.value
    )

    val activeUserUid: StateFlow<String?> = authRepository.authState.map { state ->
        if (state is AuthState.SignedIn) state.userUid else null
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = authRepository.getCurrentUserUid()
    )

    val activeHouseholdId: StateFlow<String?> = syncStatus.map { status ->
        when (status) {
            is SyncStatus.Synced -> status.householdId ?: syncRepository?.activeHouseholdId
            is SyncStatus.Connecting -> syncRepository?.activeHouseholdId
            else -> null
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = (syncStatus.value as? SyncStatus.Synced)?.householdId ?: syncRepository?.activeHouseholdId
    )

    val currentHousehold: StateFlow<HouseholdDto?> = activeHouseholdId.flatMapLatest { householdId ->
        if (householdId.isNullOrBlank()) {
            flowOf(null)
        } else {
            activeHouseholdRepo.observeHousehold(householdId)
                .catch { e ->
                    _householdError.value = e.message
                    emit(null)
                }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    val householdMembers: StateFlow<List<HouseholdMemberDto>> = activeHouseholdId.flatMapLatest { householdId ->
        if (householdId.isNullOrBlank()) {
            flowOf(emptyList())
        } else {
            activeHouseholdRepo.observeHouseholdMembers(householdId)
                .catch { e ->
                    _householdError.value = e.message
                    emit(emptyList())
                }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val currentUserMembership: StateFlow<HouseholdMemberDto?> = combine(
        householdMembers,
        activeUserUid
    ) { members, userUid ->
        if (userUid.isNullOrBlank()) null
        else members.find { it.uid == userUid }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    val canManageCategories: StateFlow<Boolean> = combine(
        activeHouseholdId,
        currentUserMembership
    ) { householdId, membership ->
        if (householdId.isNullOrBlank()) {
            true
        } else {
            val role = membership?.role?.trim()?.lowercase()
            role == "owner" || role == "admin"
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = activeHouseholdId.value.isNullOrBlank() ||
            (currentUserMembership.value?.role?.trim()?.lowercase().let { it == "owner" || it == "admin" })
    )

    val isHouseholdLoading: StateFlow<Boolean> = combine(
        activeHouseholdId,
        currentHousehold
    ) { householdId, household ->
        !householdId.isNullOrBlank() && household == null
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = false
    )

    val incomingInvites: StateFlow<List<HouseholdInviteDto>> = authState.flatMapLatest { state ->
        val email = (state as? AuthState.SignedIn)?.email
        if (email.isNullOrBlank()) {
            flowOf(emptyList())
        } else {
            activeHouseholdRepo.observeIncomingInvites(email)
                .catch { e ->
                    _invitationError.value = e.message
                    emit(emptyList())
                }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

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

    val categories: StateFlow<List<CategoryEntity>> = activeHouseholdId.flatMapLatest { householdId ->
        categoryRepository.getCategories(householdId)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val allTransactions: StateFlow<List<TransactionEntity>> = activeHouseholdId.flatMapLatest { householdId ->
        transactionRepository.getTransactions(householdId)
    }.stateIn(
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
            authRepository.authState.collect { state ->
                if (state is AuthState.SignedIn) {
                    syncRepository?.startSync(state.userUid)
                } else {
                    syncRepository?.stopSync()
                }
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
            try {
                val currentUid = activeUserUid.value ?: "local_user"
                val effectiveHouseholdId = activeHouseholdId.value ?: _uiState.value.activeTransactionForEdit?.householdId
                val savedTx = transactionRepository.saveTransaction(
                    id = id,
                    date = date,
                    description = description,
                    amountRON = amountRON,
                    type = type,
                    account = account,
                    category = category,
                    subCategory = subCategory,
                    destination = destination,
                    userId = currentUid,
                    householdId = effectiveHouseholdId
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

                val result = transactionRepository.executeAtomicCsvImport(
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
            if (!canManageCategories.value) {
                showNotification("Only household owner or admin can modify categories.")
                return@launch
            }
            val currentUid = activeUserUid.value ?: "local_user"
            categoryRepository.addCategory(name, type, subCategory, userId = currentUid, householdId = activeHouseholdId.value)
            showNotification("Category added")
        }
    }

    fun updateCategory(category: CategoryEntity) {
        viewModelScope.launch {
            if (!canManageCategories.value) {
                showNotification("Only household owner or admin can modify categories.")
                return@launch
            }
            categoryRepository.updateCategory(category)
            showNotification("Category updated")
        }
    }

    fun deleteCategory(category: CategoryEntity) {
        viewModelScope.launch {
            if (!canManageCategories.value) {
                showNotification("Only household owner or admin can modify categories.")
                return@launch
            }
            categoryRepository.deleteCategory(category)
            showNotification("Category removed")
        }
    }

    fun updateCategoryGroup(oldName: String, newName: String, type: String) {
        viewModelScope.launch {
            if (!canManageCategories.value) {
                showNotification("Only household owner or admin can modify categories.")
                return@launch
            }
            categoryRepository.updateCategoryGroup(oldName, newName, type, householdId = activeHouseholdId.value)
            showNotification("Category group updated")
        }
    }

    fun deleteCategoryGroup(name: String, type: String) {
        viewModelScope.launch {
            if (!canManageCategories.value) {
                showNotification("Only household owner or admin can modify categories.")
                return@launch
            }
            categoryRepository.deleteCategoryGroup(name, type, householdId = activeHouseholdId.value)
            showNotification("Category group deleted")
        }
    }

    fun updateSubcategory(id: String, newSubCategory: String) {
        viewModelScope.launch {
            if (!canManageCategories.value) {
                showNotification("Only household owner or admin can modify categories.")
                return@launch
            }
            categoryRepository.updateSubcategory(id, newSubCategory)
            showNotification("Subcategory updated")
        }
    }

    fun deleteSubcategory(id: String) {
        viewModelScope.launch {
            if (!canManageCategories.value) {
                showNotification("Only household owner or admin can modify categories.")
                return@launch
            }
            categoryRepository.deleteSubcategory(id)
            showNotification("Subcategory deleted")
        }
    }

    fun seedDemoData() {
        viewModelScope.launch {
            val currentUid = activeUserUid.value ?: "local_user"
            SampleDataSeeder.seedInitialTransactionsIfEmpty(transactionRepository, userId = currentUid)
            showNotification("Demo data generated!")
        }
    }

    fun signInWithGoogle(idToken: String) {
        viewModelScope.launch {
            authRepository.signInWithGoogleCredential(idToken)
        }
    }

    fun signInWithTestUid(testUid: String) {
        viewModelScope.launch {
            authRepository.signInWithTestUid(testUid)
        }
    }

    fun signOut() {
        viewModelScope.launch {
            _uiState.value = MainUiState()
            authRepository.signOut()
        }
    }

    fun clearAuthError() {
        authRepository.clearError()
    }

    fun setAuthError(message: String) {
        authRepository.setAuthError(message)
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
                val bnrResult = transactionRepository.getOfficialRate(tx.date)
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
                val result = transactionRepository.runBnrDiagnostic()
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

    // ==========================================
    // MIGRATION ORCHESTRATION METHODS (STAGE 3A)
    // ==========================================

    fun startMigrationPreflight(
        targetHouseholdId: String? = null,
        targetUserUid: String? = null,
        backupBundleDir: File? = null
    ) {
        _migrationUiState.value = MigrationUiState.RunningPreflight
        viewModelScope.launch(ioDispatcher) {
            try {
                val resolvedUserUid = targetUserUid
                    ?: (authState.value as? AuthState.SignedIn)?.userUid
                    ?: authRepository.getCurrentUserUid()
                val resolvedHouseholdId = targetHouseholdId
                    ?: syncRepository?.activeHouseholdId

                if (resolvedUserUid.isNullOrBlank() || resolvedHouseholdId.isNullOrBlank()) {
                    _migrationUiState.value = MigrationUiState.Conflict(
                        MigrationConflictState(
                            reason = ConflictReason.INSUFFICIENT_PERMISSIONS.name,
                            details = "Authentication required: You must be signed in with an active household to migrate data."
                        )
                    )
                    return@launch
                }

                val coordinator = preflightCoordinator
                if (coordinator == null) {
                    _migrationUiState.value = MigrationUiState.Failure(
                        MigrationResultState.Failure(
                            stage = "PREFLIGHT",
                            sanitizedError = "Migration preflight service is not initialized."
                        )
                    )
                    return@launch
                }

                // Ensure a valid backup bundle exists before running preflight
                val effectiveBackupDir = backupBundleDir ?: run {
                    val backupsRootDir = File(getApplication<Application>().filesDir, "migration_backups").apply { mkdirs() }
                    val newBundleDir = File(backupsRootDir, "backup_${System.currentTimeMillis()}")
                    
                    val allTxs = transactionRepository.allTransactions.first()
                    val allCats = categoryRepository.getAllCategoriesList()
                    val allRates = database?.exchangeRateDao()?.getAllOfficialRates() ?: emptyList()

                    val backupCreationResult = CsvBackupManager.createMigrationBackupBundle(
                        bundleDir = newBundleDir,
                        transactions = allTxs,
                        categories = allCats,
                        exchangeRates = allRates
                    )

                    if (!backupCreationResult.isValid) {
                        _migrationUiState.value = MigrationUiState.Failure(
                            MigrationResultState.Failure(
                                stage = "PREFLIGHT_BACKUP",
                                sanitizedError = backupCreationResult.errorMessage ?: "Failed to generate mandatory preflight backup bundle."
                            )
                        )
                        return@launch
                    }
                    newBundleDir
                }

                val result = coordinator.validatePreflight(
                    householdId = resolvedHouseholdId,
                    userUid = resolvedUserUid,
                    backupBundleDir = effectiveBackupDir
                )

                when (result) {
                    is PreflightValidationResult.Ready -> {
                        val resolvedHouseholdName = currentHousehold.value?.name?.takeIf { it.isNotBlank() }
                            ?: resolvedHouseholdId

                        val manifestTimestamp = try {
                            val manifestFile = File(effectiveBackupDir, CsvBackupManager.MANIFEST_FILE_NAME)
                            if (manifestFile.exists()) {
                                com.example.data.util.MigrationManifest.fromJson(manifestFile.readText())?.creationTimestamp
                            } else null
                        } catch (_: Exception) { null } ?: System.currentTimeMillis()

                        _migrationUiState.value = MigrationUiState.Preview(
                            MigrationPreviewState(
                                householdId = result.householdId,
                                householdName = resolvedHouseholdName,
                                userUid = result.userUid,
                                userRole = result.memberInfo.role,
                                transactionsCount = result.localCounts.transactionsCount,
                                categoriesCount = result.localCounts.categoriesCount,
                                exchangeRatesCount = result.localCounts.exchangeRatesCount,
                                totalRecords = result.localCounts.totalCount,
                                backupBundlePath = result.backupBundlePath,
                                backupTimestamp = manifestTimestamp,
                                backupValidationStatus = "VALIDATED",
                                preflightReadyData = result
                            )
                        )
                    }
                    is PreflightValidationResult.Conflict -> {
                        _migrationUiState.value = MigrationUiState.Conflict(
                            MigrationConflictState(
                                reason = result.reason.name,
                                details = sanitizeMigrationError(result.details)
                            )
                        )
                    }
                    is PreflightValidationResult.Failure -> {
                        _migrationUiState.value = MigrationUiState.Failure(
                            MigrationResultState.Failure(
                                stage = "PREFLIGHT",
                                sanitizedError = sanitizeMigrationError(result.sanitizedError)
                            )
                        )
                    }
                }
            } catch (e: Exception) {
                _migrationUiState.value = MigrationUiState.Failure(
                    MigrationResultState.Failure(
                        stage = "PREFLIGHT",
                        sanitizedError = sanitizeMigrationError(e.message)
                    )
                )
            }
        }
    }

    fun cancelMigrationPreview() {
        if (_migrationUiState.value is MigrationUiState.Preview) {
            _migrationUiState.value = MigrationUiState.Idle
        }
    }

    fun dismissMigrationDialogs() {
        _migrationUiState.value = MigrationUiState.Idle
    }

    fun confirmAndExecuteMigration() {
        val current = _migrationUiState.value
        val preview = (current as? MigrationUiState.Preview)?.preview ?: return

        _migrationUiState.value = MigrationUiState.Uploading(
            MigrationProgressState(
                stage = "INITIALIZING",
                processedCount = 0,
                totalCount = preview.totalRecords
            )
        )

        viewModelScope.launch(ioDispatcher) {
            try {
                val coordinator = preflightCoordinator
                val uploader = migrationUploader

                if (coordinator == null || uploader == null) {
                    _migrationUiState.value = MigrationUiState.Failure(
                        MigrationResultState.Failure(
                            stage = "UPLOADING",
                            sanitizedError = "Migration services are not initialized.",
                            backupBundlePath = preview.backupBundlePath
                        )
                    )
                    return@launch
                }

                val readyData = preview.preflightReadyData
                val migrationId = if (readyData != null) {
                    val sessionResult = coordinator.createMigrationSession(readyData)
                    when (sessionResult) {
                        is MigrationSessionCreationResult.Success -> sessionResult.migrationId
                        is MigrationSessionCreationResult.Failure -> {
                            _migrationUiState.value = MigrationUiState.Failure(
                                MigrationResultState.Failure(
                                    stage = "SESSION_INITIALIZATION",
                                    sanitizedError = sanitizeMigrationError(sessionResult.sanitizedError),
                                    backupBundlePath = preview.backupBundlePath
                                )
                            )
                            return@launch
                        }
                    }
                } else {
                    "mig_" + java.util.UUID.randomUUID().toString()
                }

                val uploadResult = uploader.executeMigration(
                    householdId = preview.householdId,
                    userUid = preview.userUid,
                    migrationId = migrationId,
                    backupBundlePath = preview.backupBundlePath,
                    onProgress = { stage, processed, total ->
                        _migrationUiState.value = MigrationUiState.Uploading(
                            MigrationProgressState(
                                stage = stage,
                                processedCount = processed,
                                totalCount = total
                            )
                        )
                    }
                )

                when (uploadResult) {
                    is MigrationUploadResult.Success -> {
                        _migrationUiState.value = MigrationUiState.Success(
                            MigrationResultState.Success(
                                migrationId = uploadResult.migrationId,
                                categoriesUploaded = uploadResult.categoriesUploaded,
                                ratesUploaded = uploadResult.ratesUploaded,
                                transactionsUploaded = uploadResult.transactionsUploaded,
                                totalProcessed = uploadResult.totalProcessed
                            )
                        )
                    }
                    is MigrationUploadResult.Failure -> {
                        _migrationUiState.value = MigrationUiState.Failure(
                            MigrationResultState.Failure(
                                stage = uploadResult.stage,
                                sanitizedError = sanitizeMigrationError(uploadResult.sanitizedError),
                                backupBundlePath = preview.backupBundlePath
                            )
                        )
                    }
                }
            } catch (e: Exception) {
                _migrationUiState.value = MigrationUiState.Failure(
                    MigrationResultState.Failure(
                        stage = "UPLOADING",
                        sanitizedError = sanitizeMigrationError(e.message),
                        backupBundlePath = preview.backupBundlePath
                    )
                )
            }
        }
    }

    private fun sanitizeMigrationError(rawError: String?): String {
        if (rawError.isNullOrBlank()) return "An unexpected error occurred during migration."
        val clean = rawError.lines()
            .map { it.replace(Regex("at [a-zA-Z0-9_$.]+\\(.*\\)"), "").trim() }
            .filter { it.isNotBlank() && !it.startsWith("java.") && !it.startsWith("kotlin.") && !it.startsWith("android.") }
            .firstOrNull() ?: "An unexpected error occurred during migration."
        return clean.take(150)
    }

    fun resetHouseholdCreationState() {
        _householdCreationUiState.value = HouseholdCreationUiState.Idle
    }

    fun createHousehold(name: String) {
        if (_householdCreationUiState.value is HouseholdCreationUiState.Creating) {
            return
        }

        val authStateVal = authState.value
        val signedInUser = authStateVal as? AuthState.SignedIn
        if (signedInUser == null) {
            _householdCreationUiState.value = HouseholdCreationUiState.Error("You must be signed in to create a household")
            return
        }

        val trimmedName = name.trim()
        if (trimmedName.isEmpty()) {
            _householdCreationUiState.value = HouseholdCreationUiState.Error("Household name cannot be empty")
            return
        }

        if (trimmedName.length !in 2..50) {
            _householdCreationUiState.value = HouseholdCreationUiState.Error("Household name must be between 2 and 50 characters")
            return
        }

        _householdCreationUiState.value = HouseholdCreationUiState.Creating

        viewModelScope.launch(ioDispatcher) {
            val result = activeHouseholdRepo.createHousehold(trimmedName)
            result.onSuccess { household ->
                val householdId = household.householdId.orEmpty()
                _householdCreationUiState.value = HouseholdCreationUiState.Success(householdId)
                if (householdId.isNotBlank()) {
                    categoryRepository.ensureDefaultCategoriesSeeded(householdId, enqueueOutbox = true)
                }
                syncRepository?.startSync(signedInUser.userUid, householdId)
            }.onFailure { error ->
                _householdCreationUiState.value = HouseholdCreationUiState.Error(error.message ?: "Failed to create household")
            }
        }
    }

    fun sendInvite(inviteeEmail: String, onComplete: () -> Unit = {}) {
        val household = currentHousehold.value
        val householdId = household?.householdId
        if (householdId.isNullOrBlank()) {
            _invitationError.value = "No active household found"
            showNotification("No active household found")
            return
        }

        val trimmedEmail = inviteeEmail.trim()
        if (trimmedEmail.isBlank()) {
            _invitationError.value = "Email cannot be empty"
            return
        }

        _isInvitationProcessing.value = true
        _invitationError.value = null

        viewModelScope.launch(ioDispatcher) {
            val result = activeHouseholdRepo.sendInvite(
                householdId = householdId,
                householdName = household.name.orEmpty().ifEmpty { "Household" },
                inviteeEmail = trimmedEmail
            )
            _isInvitationProcessing.value = false
            result.onSuccess {
                showNotification("Invitation sent")
                onComplete()
            }.onFailure { error ->
                val msg = error.message ?: "Failed to send invitation"
                _invitationError.value = msg
                showNotification(msg)
            }
        }
    }

    fun acceptInvite(inviteId: String) {
        if (inviteId.isBlank()) return
        _isInvitationProcessing.value = true
        _invitationError.value = null

        viewModelScope.launch(ioDispatcher) {
            val result = activeHouseholdRepo.acceptInvite(inviteId)
            _isInvitationProcessing.value = false
            result.onSuccess {
                showNotification("Invitation accepted")
                val signedInUser = authState.value as? AuthState.SignedIn
                if (signedInUser != null) {
                    syncRepository?.startSync(signedInUser.userUid)
                }
            }.onFailure { error ->
                val msg = error.message ?: "Failed to accept invitation"
                _invitationError.value = msg
                showNotification(msg)
            }
        }
    }

    fun declineInvite(inviteId: String) {
        if (inviteId.isBlank()) return
        _isInvitationProcessing.value = true
        _invitationError.value = null

        viewModelScope.launch(ioDispatcher) {
            val result = activeHouseholdRepo.declineInvite(inviteId)
            _isInvitationProcessing.value = false
            result.onSuccess {
                showNotification("Invitation declined")
            }.onFailure { error ->
                val msg = error.message ?: "Failed to decline invitation"
                _invitationError.value = msg
                showNotification(msg)
            }
        }
    }

    fun clearInvitationError() {
        _invitationError.value = null
    }
}
