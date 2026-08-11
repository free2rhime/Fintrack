package com.example

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.CategoryEntity
import com.example.data.model.FilterSettings
import com.example.data.model.TransactionEntity
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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ArchitectureAndDiTest {

    @Test
    fun testMainViewModelConstructedWithFakeRepositories() = runBlocking {
        val fakeTxRepo = FakeTransactionRepository()
        val fakeCatRepo = FakeCategoryRepository()
        val fakeSettingsRepo = FakeSettingsRepository()
        val app = ApplicationProvider.getApplicationContext<Application>()

        val viewModel = MainViewModel(
            transactionRepository = fakeTxRepo,
            categoryRepository = fakeCatRepo,
            settingsRepository = fakeSettingsRepo,
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
            subCategory = "Groceries"
        )

        fakeCatRepo.addCategory("Food", "Expense", "Groceries")

        // Verify MainViewModel flows collect from fake repositories
        val txs = viewModel.allTransactions.first()
        assertEquals(1, txs.size)
        assertEquals("Fake Test Transaction", txs[0].description)

        val cats = viewModel.categories.first()
        assertEquals(1, cats.size)
        assertEquals("Food", cats[0].name)

        val theme = viewModel.themeMode.first()
        assertEquals("light", theme)

        val filter = viewModel.filterSettings.first()
        assertEquals("Last Month", filter.selectedPeriod)
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
class FakeTransactionRepository : TransactionRepository {
    private val txList = mutableListOf<TransactionEntity>()
    private val _flow = MutableStateFlow<List<TransactionEntity>>(emptyList())

    override val allTransactions: Flow<List<TransactionEntity>> = _flow

    override fun getTransactionsInRange(startDate: String, endDate: String): Flow<List<TransactionEntity>> = _flow

    override suspend fun getTransactionById(id: String): TransactionEntity? = txList.find { it.id == id }

    override suspend fun saveTransaction(
        id: String?, date: String, description: String, amountRON: Double,
        type: String, account: String, category: String, subCategory: String, destination: String?
    ): TransactionEntity {
        val tx = TransactionEntity(
            id = id ?: "tx_${txList.size + 1}",
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
            destination = destination
        )
        txList.removeAll { it.id == tx.id }
        txList.add(tx)
        _flow.value = txList.toList()
        return tx
    }

    override suspend fun createDuplicateTemplate(source: TransactionEntity): TransactionEntity = source.copy(id = "dup_${System.currentTimeMillis()}")
    override suspend fun getDescriptionSuggestions(query: String, limit: Int): List<String> = emptyList()
    override suspend fun insertBatchWithTransaction(transactions: List<TransactionEntity>) { txList.addAll(transactions); _flow.value = txList.toList() }
    override suspend fun getUnverifiedTransactions(): List<TransactionEntity> = emptyList()
    override suspend fun getAllTransactionsList(): List<TransactionEntity> = txList.toList()
    override suspend fun syncPendingConversions(): PendingRetryResult = PendingRetryResult(0, 0, 0, 0, null)
    override suspend fun applyRepairBatch(repairs: List<PreparedRepairItem>): Int = repairs.size
    override suspend fun applyRepairToTransactionAndCache(transaction: TransactionEntity, officialRate: Double, effectiveBnrDate: String) {}
    override suspend fun insertTransaction(transaction: TransactionEntity) { txList.add(transaction); _flow.value = txList.toList() }
    override suspend fun deleteTransaction(transaction: TransactionEntity) { txList.remove(transaction); _flow.value = txList.toList() }
    override suspend fun deleteTransactionById(id: String) { txList.removeAll { it.id == id }; _flow.value = txList.toList() }
    override suspend fun deleteAllTransactions() { txList.clear(); _flow.value = emptyList() }
    override suspend fun insertBatch(transactions: List<TransactionEntity>) { txList.addAll(transactions); _flow.value = txList.toList() }
    override suspend fun getOfficialRate(date: String): BnrRateResult = BnrRateResult(date, date, 5.0, "BNR_OFFICIAL", "OFFICIAL", "OK")
    override suspend fun runBnrDiagnostic(): BnrDiagnosticResult = BnrDiagnosticResult(isReachable = true, httpStatus = "200")
    override suspend fun executeAtomicCsvImport(previewData: CsvPreviewData, backupFile: File, allExistingTransactions: List<TransactionEntity>): CsvImportFinalResult {
        return CsvImportFinalResult(true, 0, 0, 0, 0, 0, 0, 0, 0)
    }
}

class FakeCategoryRepository : CategoryRepository {
    private val catList = mutableListOf<CategoryEntity>()
    private val _flow = MutableStateFlow<List<CategoryEntity>>(emptyList())

    override val allCategories: Flow<List<CategoryEntity>> = _flow

    override suspend fun getAllCategoriesList(): List<CategoryEntity> = catList.toList()
    override suspend fun ensureDefaultCategoriesSeeded() {}
    override suspend fun addCategory(name: String, type: String, subCategory: String) {
        catList.add(CategoryEntity(id = "cat_${catList.size + 1}", name = name, type = type, subCategory = subCategory))
        _flow.value = catList.toList()
    }
    override suspend fun updateCategory(category: CategoryEntity) {}
    override suspend fun deleteCategory(category: CategoryEntity) {}
    override suspend fun updateCategoryGroup(oldName: String, newName: String, type: String) {}
    override suspend fun deleteCategoryGroup(name: String, type: String) {}
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
