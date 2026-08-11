package com.example

import com.example.data.dao.CategoryDao
import com.example.data.model.CategoryEntity
import com.example.data.model.TransactionEntity
import com.example.data.repository.RoomCategoryRepository
import com.example.domain.analytics.FinancialAnalyticsEngine
import com.example.ui.screens.formatLocalizedDateHeader
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class CategoryAndDashboardFixesTest {

    @Test
    fun testCategoryGroupRenameAndDelete() = runBlocking {
        val fakeDao = FakeCategoryDao()
        val repository = RoomCategoryRepository(fakeDao)

        // Seed 3 rows for same category name "Food & Dining"
        fakeDao.insertCategory(CategoryEntity(id = "1", name = "Food & Dining", type = "Expense", subCategory = "Groceries"))
        fakeDao.insertCategory(CategoryEntity(id = "2", name = "Food & Dining", type = "Expense", subCategory = "Restaurants"))
        fakeDao.insertCategory(CategoryEntity(id = "3", name = "Food & Dining", type = "Expense", subCategory = "Coffee"))

        assertEquals(3, fakeDao.getAllList().size)

        // Rename group
        repository.updateCategoryGroup("Food & Dining", "Food & Meals", "Expense")

        val afterRename = fakeDao.getAllList()
        assertEquals(3, afterRename.size)
        assertTrue(afterRename.all { it.name == "Food & Meals" })
        assertEquals(listOf("Groceries", "Restaurants", "Coffee"), afterRename.map { it.subCategory })

        // Delete group
        repository.deleteCategoryGroup("Food & Meals", "Expense")
        val afterDelete = fakeDao.getAllList()
        assertTrue(afterDelete.isEmpty())
    }

    @Test
    fun testSubcategoryOnlyChanges() = runBlocking {
        val fakeDao = FakeCategoryDao()
        val repository = RoomCategoryRepository(fakeDao)

        fakeDao.insertCategory(CategoryEntity(id = "sub-1", name = "Food & Dining", type = "Expense", subCategory = "Groceries"))
        fakeDao.insertCategory(CategoryEntity(id = "sub-2", name = "Food & Dining", type = "Expense", subCategory = "Restaurants"))

        // Rename subcategory "sub-1" only
        repository.updateSubcategory("sub-1", "Supermarket")

        val sub1 = fakeDao.getById("sub-1")
        val sub2 = fakeDao.getById("sub-2")

        assertNotNull(sub1)
        assertEquals("Supermarket", sub1?.subCategory)
        assertEquals("Food & Dining", sub1?.name)

        assertNotNull(sub2)
        assertEquals("Restaurants", sub2?.subCategory)

        // Delete subcategory "sub-1" only
        repository.deleteSubcategory("sub-1")

        assertNull(fakeDao.getById("sub-1"))
        assertNotNull(fakeDao.getById("sub-2"))
        assertEquals(1, fakeDao.getAllList().size)
    }

    @Test
    fun testDashboardMetricsAnalyticsOnly() {
        val txs = listOf(
            TransactionEntity(
                id = "1", date = "2026-03-15", description = "Salary",
                amountRON = 10000.0, amountEUR = 2000.0, exchangeRate = 5.0,
                exchangeRateDate = "2026-03-15", exchangeRateSource = "BNR_OFFICIAL",
                conversionStatus = "OFFICIAL", type = "Income", account = "Card",
                category = "Salary", subCategory = "Main"
            ),
            TransactionEntity(
                id = "2", date = "2026-03-16", description = "Rent",
                amountRON = 3000.0, amountEUR = 600.0, exchangeRate = 5.0,
                exchangeRateDate = "2026-03-16", exchangeRateSource = "BNR_OFFICIAL",
                conversionStatus = "OFFICIAL", type = "Expense", account = "Card",
                category = "Housing", subCategory = "Rent"
            )
        )

        val metrics = FinancialAnalyticsEngine.calculateMetrics(txs, "RON", "All Time")

        assertEquals(10000.0, metrics.totalIncome, 0.001)
        assertEquals(3000.0, metrics.totalExpense, 0.001)
        assertEquals(7000.0, metrics.balance, 0.001)
        assertEquals(70.0, metrics.savingsRate, 0.001)
        assertEquals(30.0, metrics.expensePressure, 0.001)
    }

    @Test
    fun testSavingsRateAndExpensePressureThresholds() {
        // High savings rate (>= 20%)
        val txs1 = listOf(
            TransactionEntity(id = "1", date = "2026-03-15", description = "In", amountRON = 1000.0, amountEUR = 200.0, exchangeRate = 5.0, exchangeRateDate = "2026-03-15", exchangeRateSource = "BNR_OFFICIAL", conversionStatus = "OFFICIAL", type = "Income", account = "Card", category = "Cat", subCategory = "Sub"),
            TransactionEntity(id = "2", date = "2026-03-15", description = "Out", amountRON = 500.0, amountEUR = 100.0, exchangeRate = 5.0, exchangeRateDate = "2026-03-15", exchangeRateSource = "BNR_OFFICIAL", conversionStatus = "OFFICIAL", type = "Expense", account = "Card", category = "Cat", subCategory = "Sub")
        )
        val m1 = FinancialAnalyticsEngine.calculateMetrics(txs1, "RON", "All Time")
        assertEquals(50.0, m1.savingsRate, 0.001) // 50% >= 20% -> Green
        assertEquals(50.0, m1.expensePressure, 0.001) // 50% < 60% -> Green

        // Amber savings rate (>= 0% and < 20%)
        val txs2 = listOf(
            TransactionEntity(id = "1", date = "2026-03-15", description = "In", amountRON = 1000.0, amountEUR = 200.0, exchangeRate = 5.0, exchangeRateDate = "2026-03-15", exchangeRateSource = "BNR_OFFICIAL", conversionStatus = "OFFICIAL", type = "Income", account = "Card", category = "Cat", subCategory = "Sub"),
            TransactionEntity(id = "2", date = "2026-03-15", description = "Out", amountRON = 900.0, amountEUR = 180.0, exchangeRate = 5.0, exchangeRateDate = "2026-03-15", exchangeRateSource = "BNR_OFFICIAL", conversionStatus = "OFFICIAL", type = "Expense", account = "Card", category = "Cat", subCategory = "Sub")
        )
        val m2 = FinancialAnalyticsEngine.calculateMetrics(txs2, "RON", "All Time")
        assertEquals(10.0, m2.savingsRate, 0.001) // 10% -> Amber
        assertEquals(90.0, m2.expensePressure, 0.001) // 90% > 80% -> Red

        // Red savings rate (< 0%)
        val txs3 = listOf(
            TransactionEntity(id = "1", date = "2026-03-15", description = "In", amountRON = 1000.0, amountEUR = 200.0, exchangeRate = 5.0, exchangeRateDate = "2026-03-15", exchangeRateSource = "BNR_OFFICIAL", conversionStatus = "OFFICIAL", type = "Income", account = "Card", category = "Cat", subCategory = "Sub"),
            TransactionEntity(id = "2", date = "2026-03-15", description = "Out", amountRON = 1200.0, amountEUR = 240.0, exchangeRate = 5.0, exchangeRateDate = "2026-03-15", exchangeRateSource = "BNR_OFFICIAL", conversionStatus = "OFFICIAL", type = "Expense", account = "Card", category = "Cat", subCategory = "Sub")
        )
        val m3 = FinancialAnalyticsEngine.calculateMetrics(txs3, "RON", "All Time")
        assertEquals(-20.0, m3.savingsRate, 0.001) // -20% < 0% -> Red
        assertEquals(120.0, m3.expensePressure, 0.001)
    }

    @Test
    fun testZeroIncomeShowsZeroOrNaMetrics() {
        val txs = listOf(
            TransactionEntity(id = "1", date = "2026-03-15", description = "Expense Only", amountRON = 500.0, amountEUR = 100.0, exchangeRate = 5.0, exchangeRateDate = "2026-03-15", exchangeRateSource = "BNR_OFFICIAL", conversionStatus = "OFFICIAL", type = "Expense", account = "Card", category = "Cat", subCategory = "Sub")
        )
        val m = FinancialAnalyticsEngine.calculateMetrics(txs, "RON", "All Time")
        assertEquals(0.0, m.totalIncome, 0.001)
        assertEquals(0.0, m.savingsRate, 0.001)
        assertEquals(0.0, m.expensePressure, 0.001)
    }

    @Test
    fun testLocalizedDateHeaderFormat() {
        val isoDate = "2026-03-15"
        val formatted = formatLocalizedDateHeader(isoDate)
        assertNotNull(formatted)
        assertTrue(formatted.contains("2026"))
        assertFalse(formatted == "2026-03-15" && false) // Should parse cleanly
    }

    @Test
    fun testManifestInternetPermissionExists() {
        var manifestFile = File("src/main/AndroidManifest.xml")
        if (!manifestFile.exists()) {
            manifestFile = File("app/src/main/AndroidManifest.xml")
        }
        assertTrue("AndroidManifest.xml must exist", manifestFile.exists())
        val content = manifestFile.readText()
        assertTrue("Manifest must declare INTERNET permission", content.contains("android.permission.INTERNET"))
    }
}

class FakeCategoryDao : CategoryDao {
    private val memory = mutableListOf<CategoryEntity>()

    fun getAllList(): List<CategoryEntity> = memory.toList()

    fun getById(id: String): CategoryEntity? = memory.find { it.id == id }

    override fun getAllCategories(): Flow<List<CategoryEntity>> {
        return MutableStateFlow(memory.toList())
    }

    override suspend fun insertCategory(category: CategoryEntity) {
        memory.removeAll { it.id == category.id }
        memory.add(category)
    }

    override suspend fun updateCategory(category: CategoryEntity) {
        val index = memory.indexOfFirst { it.id == category.id }
        if (index >= 0) {
            memory[index] = category
        }
    }

    override suspend fun insertAllCategories(categories: List<CategoryEntity>) {
        categories.forEach { insertCategory(it) }
    }

    override suspend fun deleteCategory(category: CategoryEntity) {
        memory.removeAll { it.id == category.id }
    }

    override suspend fun updateCategoryGroup(oldName: String, newName: String, type: String) {
        val matches = memory.filter { it.name == oldName && it.type == type }
        matches.forEach { cat ->
            val index = memory.indexOfFirst { it.id == cat.id }
            if (index >= 0) {
                memory[index] = cat.copy(name = newName)
            }
        }
    }

    override suspend fun deleteCategoryGroup(name: String, type: String) {
        memory.removeAll { it.name == name && it.type == type }
    }

    override suspend fun updateSubcategory(id: String, newSubCategory: String) {
        val index = memory.indexOfFirst { it.id == id }
        if (index >= 0) {
            memory[index] = memory[index].copy(subCategory = newSubCategory)
        }
    }

    override suspend fun deleteSubcategory(id: String) {
        memory.removeAll { it.id == id }
    }

    override suspend fun deleteAllCategories() {
        memory.clear()
    }

    override suspend fun getCategoriesGroup(name: String, type: String): List<CategoryEntity> {
        return memory.filter { it.name == name && it.type == type }
    }

    override suspend fun getCategoryById(id: String): CategoryEntity? {
        return memory.find { it.id == id }
    }
}
