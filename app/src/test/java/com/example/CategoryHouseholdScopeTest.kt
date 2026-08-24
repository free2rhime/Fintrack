package com.example

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.data.dao.CategoryDao
import com.example.data.model.CategoryDto
import com.example.data.model.CategoryEntity
import com.example.data.model.FirestoreDtoValidator
import com.example.data.model.toEntity
import com.example.data.repository.RoomCategoryRepository
import com.example.ui.components.TransactionFormDialog
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

class InMemoryCategoryDao : CategoryDao {
    private val memory = mutableListOf<CategoryEntity>()
    private val _flow = MutableStateFlow<List<CategoryEntity>>(emptyList())

    private fun emit() {
        _flow.value = memory.toList()
    }

    override fun getAllCategories(householdId: String?): Flow<List<CategoryEntity>> {
        val filtered = MutableStateFlow(
            memory.filter { (householdId == null && it.householdId == null) || it.householdId == householdId }
        )
        return filtered
    }

    override suspend fun getAllCategoriesList(householdId: String?): List<CategoryEntity> {
        return memory.filter { (householdId == null && it.householdId == null) || it.householdId == householdId }
    }

    override suspend fun insertCategory(category: CategoryEntity) {
        memory.removeAll { it.id == category.id }
        memory.add(category)
        emit()
    }

    override suspend fun updateCategory(category: CategoryEntity) {
        val index = memory.indexOfFirst { it.id == category.id }
        if (index >= 0) {
            memory[index] = category
            emit()
        }
    }

    override suspend fun insertAllCategories(categories: List<CategoryEntity>) {
        categories.forEach { cat ->
            memory.removeAll { it.id == cat.id }
            memory.add(cat)
        }
        emit()
    }

    override suspend fun deleteCategory(category: CategoryEntity) {
        memory.removeAll { it.id == category.id }
        emit()
    }

    override suspend fun updateCategoryGroup(oldName: String, newName: String, type: String, householdId: String?) {
        val matches = memory.filter {
            it.name == oldName && it.type == type &&
                    ((householdId == null && it.householdId == null) || it.householdId == householdId)
        }
        matches.forEach { cat ->
            val index = memory.indexOfFirst { it.id == cat.id }
            if (index >= 0) {
                memory[index] = cat.copy(name = newName)
            }
        }
        emit()
    }

    override suspend fun deleteCategoryGroup(name: String, type: String, householdId: String?) {
        memory.removeAll {
            it.name == name && it.type == type &&
                    ((householdId == null && it.householdId == null) || it.householdId == householdId)
        }
        emit()
    }

    override suspend fun updateSubcategory(id: String, newSubCategory: String) {
        val index = memory.indexOfFirst { it.id == id }
        if (index >= 0) {
            memory[index] = memory[index].copy(subCategory = newSubCategory)
            emit()
        }
    }

    override suspend fun deleteSubcategory(id: String) {
        memory.removeAll { it.id == id }
        emit()
    }

    override suspend fun deleteCategoryById(id: String) {
        memory.removeAll { it.id == id }
        emit()
    }

    override suspend fun deleteCategoriesByHousehold(householdId: String?) {
        if (householdId == null) {
            memory.removeAll { it.householdId == null }
        } else {
            memory.removeAll { it.householdId == householdId }
        }
        emit()
    }

    override suspend fun deleteAllCategories(householdId: String?) {
        deleteCategoriesByHousehold(householdId)
    }

    override suspend fun getCategoriesGroup(name: String, type: String, householdId: String?): List<CategoryEntity> {
        return memory.filter {
            it.name == name && it.type == type &&
                    ((householdId == null && it.householdId == null) || it.householdId == householdId)
        }
    }

    override suspend fun getCategoryById(id: String): CategoryEntity? {
        return memory.find { it.id == id }
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class CategoryHouseholdScopeTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun test1_ensureDefaultCategoriesSeeded_setsHouseholdId() = runTest {
        val dao = InMemoryCategoryDao()
        val repo = RoomCategoryRepository(categoryDao = dao)

        repo.ensureDefaultCategoriesSeeded("HH_A")

        val categories = dao.getAllCategoriesList("HH_A")
        assertEquals(19, categories.size)
        assertTrue(categories.all { it.householdId == "HH_A" })
    }

    @Test
    fun test2_ensureDefaultCategoriesSeeded_isIdempotent() = runTest {
        val dao = InMemoryCategoryDao()
        val repo = RoomCategoryRepository(categoryDao = dao)

        repo.ensureDefaultCategoriesSeeded("HH_A")
        repo.ensureDefaultCategoriesSeeded("HH_A")

        val categories = dao.getAllCategoriesList("HH_A")
        assertEquals(19, categories.size)
    }

    @Test
    fun test3_getCategories_returnsCompleteCatalogForHousehold() = runTest {
        val dao = InMemoryCategoryDao()
        val repo = RoomCategoryRepository(categoryDao = dao)

        repo.ensureDefaultCategoriesSeeded("HH_A")

        val flowCategories = repo.getCategories("HH_A").first()
        assertEquals(19, flowCategories.size)
        val expenseCats = flowCategories.filter { it.type == "Expense" }
        val incomeCats = flowCategories.filter { it.type == "Income" }
        assertEquals(14, expenseCats.size)
        assertEquals(5, incomeCats.size)
    }

    @Test
    fun test4_and_test5_householdIsolation_HH_A_and_HH_B_doNotLeak() = runTest {
        val dao = InMemoryCategoryDao()
        val repo = RoomCategoryRepository(categoryDao = dao)

        repo.ensureDefaultCategoriesSeeded("HH_A")
        repo.ensureDefaultCategoriesSeeded("HH_B")

        val hhACategories = repo.getCategories("HH_A").first()
        val hhBCategories = repo.getCategories("HH_B").first()

        assertEquals(19, hhACategories.size)
        assertEquals(19, hhBCategories.size)

        assertTrue(hhACategories.all { it.householdId == "HH_A" })
        assertFalse(hhACategories.any { it.householdId == "HH_B" })

        assertTrue(hhBCategories.all { it.householdId == "HH_B" })
        assertFalse(hhBCategories.any { it.householdId == "HH_A" })
    }

    @Test
    fun test6_categoryDtoToEntity_preservesHouseholdId() {
        val dto = CategoryDto(
            categoryId = "cat_123",
            householdId = "HH_A",
            name = "🍉 Food & Dining",
            type = "Expense",
            subCategory = "🛒 Groceries",
            createdByUid = "user_abc",
            createdAt = 1000L,
            updatedAt = 2000L,
            isDeleted = false
        )

        val entity = dto.toEntity()
        assertNotNull(entity)
        assertEquals("cat_123", entity!!.id)
        assertEquals("HH_A", entity.householdId)
        assertEquals("🍉 Food & Dining", entity.name)
        assertEquals("Expense", entity.type)
        assertEquals("🛒 Groceries", entity.subCategory)
        assertEquals("SYNCED", entity.syncStatus)
    }

    @Test
    fun test7_addCategory_preservesHouseholdId() = runTest {
        val dao = InMemoryCategoryDao()
        val repo = RoomCategoryRepository(categoryDao = dao)

        repo.addCategory(
            name = "🎨 Hobbies",
            type = "Expense",
            subCategory = "🖌️ Painting",
            userId = "user_1",
            householdId = "HH_A"
        )

        val categories = dao.getAllCategoriesList("HH_A")
        assertEquals(1, categories.size)
        assertEquals("HH_A", categories[0].householdId)
        assertEquals("🎨 Hobbies", categories[0].name)
    }

    @Test
    fun test8_updateCategoryGroup_preservesHouseholdIdScope() = runTest {
        val dao = InMemoryCategoryDao()
        val repo = RoomCategoryRepository(categoryDao = dao)

        repo.addCategory("Food", "Expense", "Groceries", "u1", "HH_A")
        repo.addCategory("Food", "Expense", "Groceries", "u2", "HH_B")

        repo.updateCategoryGroup(
            oldName = "Food",
            newName = "Food & Meals",
            type = "Expense",
            householdId = "HH_A"
        )

        val hhACats = dao.getAllCategoriesList("HH_A")
        val hhBCats = dao.getAllCategoriesList("HH_B")

        assertEquals("Food & Meals", hhACats[0].name)
        assertEquals("Food", hhBCats[0].name) // HH_B remains unmodified!
    }

    @Test
    fun test9_legacyCategoriesWithNullHouseholdId_doNotAppearInHouseholdQueries() = runTest {
        val dao = InMemoryCategoryDao()
        val repo = RoomCategoryRepository(categoryDao = dao)

        // Seed legacy offline categories
        repo.ensureDefaultCategoriesSeeded(null)
        val legacy = dao.getAllCategoriesList(null)
        assertEquals(19, legacy.size)
        assertTrue(legacy.all { it.householdId == null })

        // Seed HH_A
        repo.ensureDefaultCategoriesSeeded("HH_A")

        val hhACategories = repo.getCategories("HH_A").first()
        assertEquals(19, hhACategories.size)
        assertTrue(hhACategories.all { it.householdId == "HH_A" })
        assertFalse(hhACategories.any { it.householdId == null })
    }

    @Test
    fun test10_newlyCreatedHousehold_hasDefaultCategoriesAvailable() = runTest {
        val dao = InMemoryCategoryDao()
        val repo = RoomCategoryRepository(categoryDao = dao)

        // Simulating the createHousehold flow callback
        val newHouseholdId = "household_family_77"
        repo.ensureDefaultCategoriesSeeded(newHouseholdId)

        val result = repo.getCategories(newHouseholdId).first()
        assertTrue(result.isNotEmpty())
        assertEquals(19, result.size)
    }

    @Test
    fun test11_transactionFormDialog_populatesSubcategoriesWhenCategoriesAvailable() {
        val sampleCategories = listOf(
            CategoryEntity(name = "🍉 Food & Dining", type = "Expense", subCategory = "🛒 Groceries", householdId = "HH_A"),
            CategoryEntity(name = "🍉 Food & Dining", type = "Expense", subCategory = "🍔 Restaurants & Cafes", householdId = "HH_A"),
            CategoryEntity(name = "💼 Salary", type = "Income", subCategory = "🏢 Main Job", householdId = "HH_A")
        )

        composeTestRule.setContent {
            TransactionFormDialog(
                initialTransaction = null,
                isDuplicateMode = false,
                categories = sampleCategories,
                onDismiss = {},
                onSave = { _, _, _, _, _, _, _, _, _ -> }
            )
        }

        // Click on Subcategory dropdown to expand
        composeTestRule.onNodeWithText("Subcategory (Select First)").performClick()
        composeTestRule.onNodeWithText("🛒 Groceries").assertIsDisplayed()
        composeTestRule.onNodeWithText("🛒 Groceries").performClick()

        // Verify Category field is auto-assigned to "🍉 Food & Dining"
        composeTestRule.onNodeWithText("🍉 Food & Dining").assertExists()
    }
}