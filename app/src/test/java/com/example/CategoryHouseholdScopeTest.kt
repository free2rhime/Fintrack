package com.example

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.data.dao.CategoryDao
import com.example.data.dao.SyncOutboxDao
import com.example.data.model.CategoryDto
import com.example.data.model.CategoryEntity
import com.example.data.model.FirestoreDtoValidator
import com.example.data.model.SyncOutboxEntity
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

class InMemorySyncOutboxDao : SyncOutboxDao {
    private val entries = mutableListOf<SyncOutboxEntity>()

    override fun getAllOutboxEntries(): Flow<List<SyncOutboxEntity>> = MutableStateFlow(entries.toList())

    override suspend fun getPendingEntries(): List<SyncOutboxEntity> {
        return entries.filter { it.status == "PENDING" }.sortedBy { it.createdAt }
    }

    override suspend fun getPendingBatch(limit: Int): List<SyncOutboxEntity> {
        return entries.filter { it.status == "PENDING" }.sortedBy { it.createdAt }.take(limit)
    }

    override suspend fun getPendingCount(): Int = entries.count { it.status == "PENDING" }

    override fun getPendingCountFlow(): Flow<Int> = MutableStateFlow(entries.count { it.status == "PENDING" })

    override suspend fun getPendingEntryForEntity(entityId: String): SyncOutboxEntity? {
        return entries.filter { it.entityId == entityId && it.status == "PENDING" }.maxByOrNull { it.createdAt }
    }

    override suspend fun getEntryById(id: String): SyncOutboxEntity? {
        return entries.find { it.id == id }
    }

    override suspend fun markInProgress(id: String, lastAttemptAt: Long, updatedAt: Long): Int {
        val index = entries.indexOfFirst { it.id == id }
        if (index >= 0) {
            entries[index] = entries[index].copy(status = "IN_PROGRESS", lastAttemptAt = lastAttemptAt, updatedAt = updatedAt)
            return 1
        }
        return 0
    }

    override suspend fun markPending(id: String, updatedAt: Long): Int {
        val index = entries.indexOfFirst { it.id == id }
        if (index >= 0) {
            entries[index] = entries[index].copy(status = "PENDING", updatedAt = updatedAt)
            return 1
        }
        return 0
    }

    override suspend fun markAcknowledged(id: String, updatedAt: Long): Int {
        val index = entries.indexOfFirst { it.id == id }
        if (index >= 0) {
            entries[index] = entries[index].copy(status = "ACKNOWLEDGED", updatedAt = updatedAt)
            return 1
        }
        return 0
    }

    override suspend fun markSuccess(id: String, updatedAt: Long): Int {
        val index = entries.indexOfFirst { it.id == id }
        if (index >= 0) {
            entries[index] = entries[index].copy(status = "SUCCESS", updatedAt = updatedAt)
            return 1
        }
        return 0
    }

    override suspend fun markFailed(id: String, errorCode: String?, errorMessage: String?, retryCount: Int, lastAttemptAt: Long, updatedAt: Long): Int {
        val index = entries.indexOfFirst { it.id == id }
        if (index >= 0) {
            entries[index] = entries[index].copy(
                status = "FAILED",
                errorCode = errorCode,
                errorMessage = errorMessage,
                retryCount = retryCount,
                lastAttemptAt = lastAttemptAt,
                updatedAt = updatedAt
            )
            return 1
        }
        return 0
    }

    override suspend fun resetInProgressToPending(updatedAt: Long): Int {
        var count = 0
        entries.indices.forEach { i ->
            if (entries[i].status == "IN_PROGRESS") {
                entries[i] = entries[i].copy(status = "PENDING", updatedAt = updatedAt)
                count++
            }
        }
        return count
    }

    override suspend fun incrementRetryCount(id: String, lastAttemptAt: Long, errorCode: String?, errorMessage: String?, updatedAt: Long): Int {
        val index = entries.indexOfFirst { it.id == id }
        if (index >= 0) {
            val curr = entries[index]
            entries[index] = curr.copy(
                retryCount = curr.retryCount + 1,
                lastAttemptAt = lastAttemptAt,
                errorCode = errorCode,
                errorMessage = errorMessage,
                updatedAt = updatedAt
            )
            return 1
        }
        return 0
    }

    override suspend fun recordRetryFailure(id: String, errorCode: String?, errorMessage: String?, lastAttemptAt: Long, updatedAt: Long): Int {
        val index = entries.indexOfFirst { it.id == id }
        if (index >= 0) {
            val curr = entries[index]
            entries[index] = curr.copy(
                status = "PENDING",
                retryCount = curr.retryCount + 1,
                lastAttemptAt = lastAttemptAt,
                errorCode = errorCode,
                errorMessage = errorMessage,
                updatedAt = updatedAt
            )
            return 1
        }
        return 0
    }

    override suspend fun clearAcknowledgedEntries() {
        entries.removeAll { it.status == "ACKNOWLEDGED" }
    }

    override suspend fun deleteAcknowledgedEntries(): Int {
        val initial = entries.size
        entries.removeAll { it.status == "ACKNOWLEDGED" }
        return initial - entries.size
    }

    override suspend fun deleteSuccessEntries(): Int {
        val initial = entries.size
        entries.removeAll { it.status == "SUCCESS" }
        return initial - entries.size
    }

    override suspend fun deleteOldCompletedEntries(cutoffTime: Long): Int {
        val initial = entries.size
        entries.removeAll { it.status in listOf("ACKNOWLEDGED", "SUCCESS") && it.updatedAt < cutoffTime }
        return initial - entries.size
    }

    override suspend fun deleteOldFailedEntries(cutoffTime: Long): Int {
        val initial = entries.size
        entries.removeAll { it.status == "FAILED" && it.updatedAt < cutoffTime }
        return initial - entries.size
    }

    override suspend fun deleteOutboxEntryById(id: String) {
        entries.removeAll { it.id == id }
    }

    override suspend fun deleteOutboxEntriesForEntity(entityId: String) {
        entries.removeAll { it.entityId == entityId }
    }

    override suspend fun deleteAllOutboxEntries() {
        entries.clear()
    }

    override suspend fun getPendingEntry(entityType: String, entityId: String): SyncOutboxEntity? {
        return entries.filter { it.entityType == entityType && it.entityId == entityId && it.status == "PENDING" }.maxByOrNull { it.createdAt }
    }

    override suspend fun getActiveEntry(entityType: String, entityId: String): SyncOutboxEntity? {
        return entries.filter { it.entityType == entityType && it.entityId == entityId && it.status in listOf("PENDING", "IN_PROGRESS") }.maxByOrNull { it.createdAt }
    }

    override suspend fun getActiveEntityIdsByType(entityType: String): List<String> {
        return entries.filter { it.entityType == entityType && it.status in listOf("PENDING", "IN_PROGRESS") }.map { it.entityId }
    }

    override suspend fun deleteEntriesForEntity(entityType: String, entityId: String): Int {
        val initial = entries.size
        entries.removeAll { it.entityType == entityType && it.entityId == entityId }
        return initial - entries.size
    }

    override suspend fun insertOutboxEntry(entry: SyncOutboxEntity) {
        entries.removeAll { it.id == entry.id }
        entries.add(entry)
    }

    override suspend fun insertAllOutboxEntries(entriesToInsert: List<SyncOutboxEntity>) {
        entriesToInsert.forEach { insertOutboxEntry(it) }
    }

    override suspend fun updateOutboxEntry(entry: SyncOutboxEntity) {
        val index = entries.indexOfFirst { it.id == entry.id }
        if (index >= 0) {
            entries[index] = entry
        }
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

    @Test
    fun test12_regression_independentSeedingForSameHousehold_mustGenerateDeterministicIds() = runTest {
        // Instance A: Seeds HH_A into local database A
        val daoA = InMemoryCategoryDao()
        val repoA = RoomCategoryRepository(categoryDao = daoA)
        repoA.ensureDefaultCategoriesSeeded("HH_A")
        val catsA = daoA.getAllCategoriesList("HH_A")
        assertEquals(19, catsA.size)

        // Instance B: Fresh local database on second device/session for SAME household
        val daoB = InMemoryCategoryDao()
        val repoB = RoomCategoryRepository(categoryDao = daoB)
        repoB.ensureDefaultCategoriesSeeded("HH_A")
        val catsB = daoB.getAllCategoriesList("HH_A")
        assertEquals(19, catsB.size)

        // Map logical category -> categoryId for both instances
        val idMapA = catsA.associate { Triple(it.type, it.name, it.subCategory) to it.id }
        val idMapB = catsB.associate { Triple(it.type, it.name, it.subCategory) to it.id }

        // Assert deterministic IDs for every logical default category across independent seeds
        for ((logicalKey, idA) in idMapA) {
            val idB = idMapB[logicalKey]
            assertNotNull("Category for $logicalKey should exist in instance B", idB)
            assertEquals("Default category ID for $logicalKey must be deterministic across independent seeds for the same household", idA, idB)
        }
    }

    @Test
    fun test13_regression_independentSeedingForSameHousehold_mustNotDuplicateOutboxEntityIds() = runTest {
        // Instance A: Seeds HH_A with its outbox
        val daoA = InMemoryCategoryDao()
        val outboxDaoA = InMemorySyncOutboxDao()
        val repoA = RoomCategoryRepository(categoryDao = daoA, syncOutboxDao = outboxDaoA)
        repoA.ensureDefaultCategoriesSeeded("HH_A")
        val outboxA = outboxDaoA.getPendingEntries()
        assertEquals(19, outboxA.size)
        val entityIdsA = outboxA.map { it.entityId }.toSet()

        // Instance B: Fresh local database seeds HH_A with its outbox
        val daoB = InMemoryCategoryDao()
        val outboxDaoB = InMemorySyncOutboxDao()
        val repoB = RoomCategoryRepository(categoryDao = daoB, syncOutboxDao = outboxDaoB)
        repoB.ensureDefaultCategoriesSeeded("HH_A")
        val outboxB = outboxDaoB.getPendingEntries()
        assertEquals(19, outboxB.size)
        val entityIdsB = outboxB.map { it.entityId }.toSet()

        // Assert outbox entityIds are identical (deterministic) so they do not upload separate documents
        assertEquals("Outbox CATEGORY UPSERT entityIds must be identical across independent seeds for the same household", entityIdsA, entityIdsB)
    }

    @Test
    fun test14_regression_mergingIndependentSeeds_mustNotCreateLogicalDuplicates() = runTest {
        // Instance A seeds HH_A
        val daoA = InMemoryCategoryDao()
        val repoA = RoomCategoryRepository(categoryDao = daoA)
        repoA.ensureDefaultCategoriesSeeded("HH_A")
        val catsA = daoA.getAllCategoriesList("HH_A")

        // Instance B seeds HH_A
        val daoB = InMemoryCategoryDao()
        val repoB = RoomCategoryRepository(categoryDao = daoB)
        repoB.ensureDefaultCategoriesSeeded("HH_A")
        val catsB = daoB.getAllCategoriesList("HH_A")

        // Group the combined records by logical identity: (type, name, subCategory)
        val mergedList = catsA + catsB
        val duplicates = mergedList.groupBy { Triple(it.type, it.name, it.subCategory) }
            .filter { it.value.map { cat -> cat.id }.distinct().size > 1 }

        assertTrue(
            "Expected 0 logical duplicate IDs when independent seeds for the same household are merged, but found ${duplicates.size} duplicate groups: ${duplicates.keys.take(3)}...",
            duplicates.isEmpty()
        )
    }

    @Test
    fun test15_joiningMemberSeedingWithEnqueueOutboxFalse_doesNotEnqueueOutboxUpserts() = runTest {
        val dao = InMemoryCategoryDao()
        val outboxDao = InMemorySyncOutboxDao()
        val repo = RoomCategoryRepository(categoryDao = dao, syncOutboxDao = outboxDao)

        // Seeding with enqueueOutbox = false (as called when joining/starting an existing household)
        repo.ensureDefaultCategoriesSeeded("HH_A", enqueueOutbox = false)

        val localCats = dao.getAllCategoriesList("HH_A")
        assertEquals(19, localCats.size)

        val outboxEntries = outboxDao.getPendingEntries()
        assertEquals(0, outboxEntries.size)
    }
}