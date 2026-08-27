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

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.db.FinTrackDatabase
import com.example.data.repository.FirestoreSnapshotSource
import com.example.data.repository.FirestoreSyncRepository
import com.example.data.repository.HouseholdResolutionResult
import com.example.data.repository.ListenerRegistrationHandle

class TestSnapshotSource : FirestoreSnapshotSource {
    val members = mutableMapOf<Pair<String, String>, Map<String, Any?>>()
    override fun listenToTransactions(
        householdId: String,
        onSnapshot: (List<Pair<String, Map<String, Any?>>>) -> Unit,
        onError: (Exception) -> Unit
    ) = object : ListenerRegistrationHandle { override fun remove() {} }

    override fun listenToCategories(
        householdId: String,
        onSnapshot: (List<Pair<String, Map<String, Any?>>>) -> Unit,
        onError: (Exception) -> Unit
    ) = object : ListenerRegistrationHandle { override fun remove() {} }

    override suspend fun resolveHouseholdId(userUid: String): HouseholdResolutionResult {
        val entry = members.entries.firstOrNull { it.key.second == userUid && (it.value["status"] as? String)?.uppercase() == "ACTIVE" }
        return if (entry != null) HouseholdResolutionResult.Success(entry.key.first) else HouseholdResolutionResult.NoHousehold
    }

    override suspend fun getHouseholdMembership(householdId: String, userUid: String): Map<String, Any?>? {
        return members[Pair(householdId, userUid)]
    }
}

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

    override suspend fun getCategoriesByLogicalIdentity(
        householdId: String?,
        type: String,
        name: String,
        subCategory: String
    ): List<CategoryEntity> {
        return memory.filter {
            ((householdId == null && it.householdId == null) || it.householdId == householdId) &&
                    it.type == type && it.name == name && it.subCategory == subCategory
        }
    }

    override suspend fun deleteCategoriesByLogicalIdentity(
        householdId: String?,
        type: String,
        name: String,
        subCategory: String
    ) {
        memory.removeAll {
            ((householdId == null && it.householdId == null) || it.householdId == householdId) &&
                    it.type == type && it.name == name && it.subCategory == subCategory
        }
        emit()
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

    @Test
    fun test16_testInboundSync_collapsesLegacyAndDeterministicDuplicates() = runTest {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val db = Room.inMemoryDatabaseBuilder(context, FinTrackDatabase::class.java).allowMainThreadQueries().build()
        val snapshotSource = TestSnapshotSource()
        val householdId = "FzzoMr4dohrfFFCxuzo"
        val userUid = "owner_uid"
        snapshotSource.members[Pair(householdId, userUid)] = mapOf("role" to "OWNER", "status" to "ACTIVE")

        val syncRepo = FirestoreSyncRepository(database = db, snapshotSource = snapshotSource)
        syncRepo.startSync(userUid = userUid, requestedHouseholdId = householdId)

        val deterministicId = RoomCategoryRepository.generateDefaultCategoryId(
            householdId = householdId,
            type = "Expense",
            name = "🏥 Health & Wellness",
            subCategory = "💊 Pharmacy & Medical"
        )
        val legacyId = "0b02656d-ae81-4798-9faf-b8e804d6e789"

        val snapshot = listOf(
            Pair(
                deterministicId,
                mapOf<String, Any?>(
                    "categoryId" to deterministicId,
                    "householdId" to householdId,
                    "type" to "Expense",
                    "name" to "🏥 Health & Wellness",
                    "subCategory" to "💊 Pharmacy & Medical",
                    "isDeleted" to false,
                    "createdAt" to 1000L,
                    "updatedAt" to 1000L
                )
            ),
            Pair(
                legacyId,
                mapOf<String, Any?>(
                    "categoryId" to legacyId,
                    "householdId" to householdId,
                    "type" to "Expense",
                    "name" to "🏥 Health & Wellness",
                    "subCategory" to "💊 Pharmacy & Medical",
                    "isDeleted" to false,
                    "createdAt" to 900L,
                    "updatedAt" to 950L
                )
            )
        )

        syncRepo.processCategorySnapshot(snapshot)

        val localCats = db.categoryDao().getAllCategoriesList(householdId)
        assertEquals(1, localCats.size)
        assertEquals(deterministicId, localCats.first().id)
        assertEquals("💊 Pharmacy & Medical", localCats.first().subCategory)

        // Outbox must contain DELETE tombstone for the legacy ID
        val outbox = db.syncOutboxDao().getPendingEntries()
        assertEquals(1, outbox.size)
        assertEquals("CATEGORY", outbox.first().entityType)
        assertEquals(legacyId, outbox.first().entityId)
        assertEquals("DELETE", outbox.first().operation)
    }

    @Test
    fun test17_testInboundSync_multipleLegacyDocuments_collapsedToDeterministic() = runTest {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val db = Room.inMemoryDatabaseBuilder(context, FinTrackDatabase::class.java).allowMainThreadQueries().build()
        val snapshotSource = TestSnapshotSource()
        val householdId = "HH_ABC"
        val userUid = "admin_uid"
        snapshotSource.members[Pair(householdId, userUid)] = mapOf("role" to "ADMIN", "status" to "ACTIVE")

        val syncRepo = FirestoreSyncRepository(database = db, snapshotSource = snapshotSource)
        syncRepo.startSync(userUid = userUid, requestedHouseholdId = householdId)

        val deterministicId = RoomCategoryRepository.generateDefaultCategoryId(
            householdId = householdId,
            type = "Expense",
            name = "🏥 Health & Wellness",
            subCategory = "💊 Pharmacy & Medical"
        )
        val legacy1 = "legacy-uuid-1"
        val legacy2 = "legacy-uuid-2"
        val legacy3 = "legacy-uuid-3"

        val snapshot = listOf(
            Pair(
                legacy1,
                mapOf<String, Any?>(
                    "categoryId" to legacy1,
                    "householdId" to householdId,
                    "type" to "Expense",
                    "name" to "🏥 Health & Wellness",
                    "subCategory" to "💊 Pharmacy & Medical",
                    "isDeleted" to false,
                    "createdAt" to 100L,
                    "updatedAt" to 150L
                )
            ),
            Pair(
                legacy2,
                mapOf<String, Any?>(
                    "categoryId" to legacy2,
                    "householdId" to householdId,
                    "type" to "Expense",
                    "name" to "🏥 Health & Wellness",
                    "subCategory" to "💊 Pharmacy & Medical",
                    "isDeleted" to false,
                    "createdAt" to 200L,
                    "updatedAt" to 250L
                )
            ),
            Pair(
                legacy3,
                mapOf<String, Any?>(
                    "categoryId" to legacy3,
                    "householdId" to householdId,
                    "type" to "Expense",
                    "name" to "🏥 Health & Wellness",
                    "subCategory" to "💊 Pharmacy & Medical",
                    "isDeleted" to false,
                    "createdAt" to 300L,
                    "updatedAt" to 350L
                )
            )
        )

        syncRepo.processCategorySnapshot(snapshot)

        val localCats = db.categoryDao().getAllCategoriesList(householdId)
        assertEquals(1, localCats.size)
        assertEquals(deterministicId, localCats.first().id)

        val outbox = db.syncOutboxDao().getPendingEntries()
        assertEquals(4, outbox.size)
        val upsertEntry = outbox.firstOrNull { it.operation == "UPSERT" }
        assertNotNull("Must contain UPSERT for canonical deterministicId", upsertEntry)
        assertEquals(deterministicId, upsertEntry!!.entityId)

        val deletedIds = outbox.filter { it.operation == "DELETE" }.map { it.entityId }.toSet()
        assertEquals(setOf(legacy1, legacy2, legacy3), deletedIds)
    }

    @Test
    fun test18_testInboundSync_tombstonesLegacyDocumentWhenDeterministicDeleted() = runTest {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val db = Room.inMemoryDatabaseBuilder(context, FinTrackDatabase::class.java).allowMainThreadQueries().build()
        val snapshotSource = TestSnapshotSource()
        val householdId = "HH_ABC"
        val userUid = "owner_uid"
        snapshotSource.members[Pair(householdId, userUid)] = mapOf("role" to "OWNER", "status" to "ACTIVE")

        val syncRepo = FirestoreSyncRepository(database = db, snapshotSource = snapshotSource)
        syncRepo.startSync(userUid = userUid, requestedHouseholdId = householdId)

        val deterministicId = RoomCategoryRepository.generateDefaultCategoryId(
            householdId = householdId,
            type = "Expense",
            name = "🏥 Health & Wellness",
            subCategory = "💊 Pharmacy & Medical"
        )
        val legacyId = "legacy-active-uuid"

        val snapshot = listOf(
            Pair(
                deterministicId,
                mapOf<String, Any?>(
                    "categoryId" to deterministicId,
                    "householdId" to householdId,
                    "type" to "Expense",
                    "name" to "🏥 Health & Wellness",
                    "subCategory" to "💊 Pharmacy & Medical",
                    "isDeleted" to true,
                    "createdAt" to 100L,
                    "updatedAt" to 500L
                )
            ),
            Pair(
                legacyId,
                mapOf<String, Any?>(
                    "categoryId" to legacyId,
                    "householdId" to householdId,
                    "type" to "Expense",
                    "name" to "🏥 Health & Wellness",
                    "subCategory" to "💊 Pharmacy & Medical",
                    "isDeleted" to false,
                    "createdAt" to 50L,
                    "updatedAt" to 200L
                )
            )
        )

        syncRepo.processCategorySnapshot(snapshot)

        val localCats = db.categoryDao().getAllCategoriesList(householdId)
        assertEquals(0, localCats.size)

        val outbox = db.syncOutboxDao().getPendingEntries()
        assertEquals(1, outbox.size)
        assertEquals(legacyId, outbox.first().entityId)
        assertEquals("DELETE", outbox.first().operation)
    }

    @Test
    fun test19_testInboundSync_isIdempotentOnRepeatedSnapshots() = runTest {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val db = Room.inMemoryDatabaseBuilder(context, FinTrackDatabase::class.java).allowMainThreadQueries().build()
        val snapshotSource = TestSnapshotSource()
        val householdId = "HH_ABC"
        val userUid = "owner_uid"
        snapshotSource.members[Pair(householdId, userUid)] = mapOf("role" to "OWNER", "status" to "ACTIVE")

        val syncRepo = FirestoreSyncRepository(database = db, snapshotSource = snapshotSource)
        syncRepo.startSync(userUid = userUid, requestedHouseholdId = householdId)

        val deterministicId = RoomCategoryRepository.generateDefaultCategoryId(
            householdId = householdId,
            type = "Expense",
            name = "🏥 Health & Wellness",
            subCategory = "💊 Pharmacy & Medical"
        )
        val legacyId = "legacy-dup-1"

        val snapshot = listOf(
            Pair(
                deterministicId,
                mapOf<String, Any?>(
                    "categoryId" to deterministicId,
                    "householdId" to householdId,
                    "type" to "Expense",
                    "name" to "🏥 Health & Wellness",
                    "subCategory" to "💊 Pharmacy & Medical",
                    "isDeleted" to false,
                    "createdAt" to 100L,
                    "updatedAt" to 100L
                )
            ),
            Pair(
                legacyId,
                mapOf<String, Any?>(
                    "categoryId" to legacyId,
                    "householdId" to householdId,
                    "type" to "Expense",
                    "name" to "🏥 Health & Wellness",
                    "subCategory" to "💊 Pharmacy & Medical",
                    "isDeleted" to false,
                    "createdAt" to 80L,
                    "updatedAt" to 80L
                )
            )
        )

        // Run 1
        syncRepo.processCategorySnapshot(snapshot)
        val outbox1 = db.syncOutboxDao().getPendingEntries()
        assertEquals(1, outbox1.size)

        // Run 2 (Repeated snapshot)
        syncRepo.processCategorySnapshot(snapshot)
        val outbox2 = db.syncOutboxDao().getPendingEntries()
        assertEquals(1, outbox2.size)
        assertEquals(1, db.categoryDao().getAllCategoriesList(householdId).size)
    }

    @Test
    fun test20_testDeleteSubcategory_deletesAllLogicalDuplicatesAndEnqueuesOutboxDeletes() = runTest {
        val dao = InMemoryCategoryDao()
        val outboxDao = InMemorySyncOutboxDao()
        val repo = RoomCategoryRepository(categoryDao = dao, syncOutboxDao = outboxDao)

        val householdId = "HH_A"
        val deterministicId = RoomCategoryRepository.generateDefaultCategoryId(
            householdId = householdId,
            type = "Expense",
            name = "🏥 Health & Wellness",
            subCategory = "💊 Pharmacy & Medical"
        )
        val legacyId = "legacy-row-uuid"

        // Seed both into Room
        dao.insertCategory(
            CategoryEntity(
                id = deterministicId,
                householdId = householdId,
                type = "Expense",
                name = "🏥 Health & Wellness",
                subCategory = "💊 Pharmacy & Medical"
            )
        )
        dao.insertCategory(
            CategoryEntity(
                id = legacyId,
                householdId = householdId,
                type = "Expense",
                name = "🏥 Health & Wellness",
                subCategory = "💊 Pharmacy & Medical"
            )
        )

        assertEquals(2, dao.getAllCategoriesList(householdId).size)

        // Delete using deterministicId
        repo.deleteSubcategory(deterministicId)

        // Both rows must be removed from Room
        val remaining = dao.getAllCategoriesList(householdId)
        assertEquals(0, remaining.size)

        // Outbox must contain DELETE for both IDs
        val outbox = outboxDao.getPendingEntries()
        val deletedIds = outbox.map { it.entityId }.toSet()
        assertEquals(setOf(deterministicId, legacyId), deletedIds)
        assertTrue(outbox.all { it.operation == "DELETE" })
    }

    @Test
    fun test21_testReconciliation_preservesHouseholdAndTypeIsolation() = runTest {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val db = Room.inMemoryDatabaseBuilder(context, FinTrackDatabase::class.java).allowMainThreadQueries().build()
        val snapshotSource = TestSnapshotSource()
        val syncRepo = FirestoreSyncRepository(database = db, snapshotSource = snapshotSource)

        val snapshot = listOf(
            Pair(
                "hhA_exp_legacy",
                mapOf<String, Any?>(
                    "categoryId" to "hhA_exp_legacy",
                    "householdId" to "HH_A",
                    "type" to "Expense",
                    "name" to "Freelance",
                    "subCategory" to "Consulting",
                    "isDeleted" to false,
                    "createdAt" to 100L,
                    "updatedAt" to 100L
                )
            ),
            Pair(
                "hhA_inc_legacy",
                mapOf<String, Any?>(
                    "categoryId" to "hhA_inc_legacy",
                    "householdId" to "HH_A",
                    "type" to "Income",
                    "name" to "Freelance",
                    "subCategory" to "Consulting",
                    "isDeleted" to false,
                    "createdAt" to 100L,
                    "updatedAt" to 100L
                )
            ),
            Pair(
                "hhB_exp_legacy",
                mapOf<String, Any?>(
                    "categoryId" to "hhB_exp_legacy",
                    "householdId" to "HH_B",
                    "type" to "Expense",
                    "name" to "Freelance",
                    "subCategory" to "Consulting",
                    "isDeleted" to false,
                    "createdAt" to 100L,
                    "updatedAt" to 100L
                )
            ),
            Pair(
                "hhB_inc_legacy",
                mapOf<String, Any?>(
                    "categoryId" to "hhB_inc_legacy",
                    "householdId" to "HH_B",
                    "type" to "Income",
                    "name" to "Freelance",
                    "subCategory" to "Consulting",
                    "isDeleted" to false,
                    "createdAt" to 100L,
                    "updatedAt" to 100L
                )
            )
        )

        syncRepo.processCategorySnapshot(snapshot)

        val catsA = db.categoryDao().getAllCategoriesList("HH_A")
        val catsB = db.categoryDao().getAllCategoriesList("HH_B")

        assertEquals(2, catsA.size)
        assertEquals(2, catsB.size)

        val expA = catsA.first { it.type == "Expense" }
        val incA = catsA.first { it.type == "Income" }
        val expB = catsB.first { it.type == "Expense" }
        val incB = catsB.first { it.type == "Income" }

        val allIds = setOf(expA.id, incA.id, expB.id, incB.id)
        assertEquals("All 4 categories must have unique deterministic IDs", 4, allIds.size)
    }

    @Test
    fun test22_customCategory_creationGeneratesDeterministicIdAndSurvivesInboundSync() = runTest {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val db = Room.inMemoryDatabaseBuilder(context, FinTrackDatabase::class.java).allowMainThreadQueries().build()
        val snapshotSource = TestSnapshotSource()
        val householdId = "HH_CUSTOM_1"
        val userUid = "owner_uid"
        snapshotSource.members[Pair(householdId, userUid)] = mapOf("role" to "OWNER", "status" to "ACTIVE")

        val syncRepo = FirestoreSyncRepository(database = db, snapshotSource = snapshotSource)
        syncRepo.startSync(userUid = userUid, requestedHouseholdId = householdId)

        val repo = RoomCategoryRepository(
            categoryDao = db.categoryDao(),
            syncOutboxDao = db.syncOutboxDao(),
            database = db
        )

        // 1. Add custom category
        repo.addCategory(
            name = "🚗 Vehicles",
            type = "Expense",
            subCategory = "⛽ Fuel",
            userId = userUid,
            householdId = householdId
        )

        val expectedCanonicalId = RoomCategoryRepository.generateDefaultCategoryId(
            householdId = householdId,
            type = "Expense",
            name = "🚗 Vehicles",
            subCategory = "⛽ Fuel"
        )

        val localCats = db.categoryDao().getAllCategoriesList(householdId)
        assertEquals(1, localCats.size)
        assertEquals(expectedCanonicalId, localCats.first().id)
        assertEquals("🚗 Vehicles", localCats.first().name)
        assertEquals("⛽ Fuel", localCats.first().subCategory)

        // 2. Inbound snapshot receives the canonical document from Firestore
        val snapshot = listOf(
            Pair(
                expectedCanonicalId,
                mapOf<String, Any?>(
                    "categoryId" to expectedCanonicalId,
                    "householdId" to householdId,
                    "type" to "Expense",
                    "name" to "🚗 Vehicles",
                    "subCategory" to "⛽ Fuel",
                    "isDeleted" to false,
                    "createdAt" to 100L,
                    "updatedAt" to 100L
                )
            )
        )

        syncRepo.processCategorySnapshot(snapshot)

        // Category survives snapshot and remains active
        val catsAfterSync = db.categoryDao().getAllCategoriesList(householdId)
        assertEquals(1, catsAfterSync.size)
        assertEquals(expectedCanonicalId, catsAfterSync.first().id)
        assertFalse(catsAfterSync.first().isDeleted)
    }

    @Test
    fun test23_inboundSync_migratesLegacyCustomCategoryToCanonicalIdInFirestore() = runTest {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val db = Room.inMemoryDatabaseBuilder(context, FinTrackDatabase::class.java).allowMainThreadQueries().build()
        val snapshotSource = TestSnapshotSource()
        val householdId = "HH_CUSTOM_2"
        val userUid = "owner_uid"
        snapshotSource.members[Pair(householdId, userUid)] = mapOf("role" to "OWNER", "status" to "ACTIVE")

        val syncRepo = FirestoreSyncRepository(database = db, snapshotSource = snapshotSource)
        syncRepo.startSync(userUid = userUid, requestedHouseholdId = householdId)

        val legacyId = "custom-legacy-uuid-123"
        val expectedCanonicalId = RoomCategoryRepository.generateDefaultCategoryId(
            householdId = householdId,
            type = "Expense",
            name = "🚗 Vehicles",
            subCategory = "⛽ Fuel"
        )

        // Snapshot contains ONLY legacy random-UUID document
        val snapshot = listOf(
            Pair(
                legacyId,
                mapOf<String, Any?>(
                    "categoryId" to legacyId,
                    "householdId" to householdId,
                    "type" to "Expense",
                    "name" to "🚗 Vehicles",
                    "subCategory" to "⛽ Fuel",
                    "isDeleted" to false,
                    "createdAt" to 100L,
                    "updatedAt" to 200L,
                    "createdByUid" to "creator_user"
                )
            )
        )

        syncRepo.processCategorySnapshot(snapshot)

        // 1. Room contains canonical category
        val localCats = db.categoryDao().getAllCategoriesList(householdId)
        assertEquals(1, localCats.size)
        assertEquals(expectedCanonicalId, localCats.first().id)
        assertEquals("🚗 Vehicles", localCats.first().name)
        assertEquals("⛽ Fuel", localCats.first().subCategory)

        // 2. Outbox contains UPSERT for canonicalId and DELETE for legacyId
        val outbox = db.syncOutboxDao().getPendingEntries()
        assertEquals(2, outbox.size)

        val upsertEntry = outbox.firstOrNull { it.operation == "UPSERT" }
        val deleteEntry = outbox.firstOrNull { it.operation == "DELETE" }

        assertNotNull("Must have UPSERT for canonicalId", upsertEntry)
        assertEquals(expectedCanonicalId, upsertEntry!!.entityId)

        assertNotNull("Must have DELETE for legacyId", deleteEntry)
        assertEquals(legacyId, deleteEntry!!.entityId)
    }

    @Test
    fun test24_inboundSync_legacyCustomCategory_memberDoesNotEnqueueCloudMutations() = runTest {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val db = Room.inMemoryDatabaseBuilder(context, FinTrackDatabase::class.java).allowMainThreadQueries().build()
        val snapshotSource = TestSnapshotSource()
        val householdId = "HH_CUSTOM_3"
        val memberUid = "member_uid"
        snapshotSource.members[Pair(householdId, memberUid)] = mapOf("role" to "MEMBER", "status" to "ACTIVE")

        val syncRepo = FirestoreSyncRepository(database = db, snapshotSource = snapshotSource)
        syncRepo.startSync(userUid = memberUid, requestedHouseholdId = householdId)

        val legacyId = "custom-legacy-uuid-456"
        val expectedCanonicalId = RoomCategoryRepository.generateDefaultCategoryId(
            householdId = householdId,
            type = "Expense",
            name = "🚗 Vehicles",
            subCategory = "⛽ Fuel"
        )

        // Snapshot contains ONLY legacy random-UUID document
        val snapshot = listOf(
            Pair(
                legacyId,
                mapOf<String, Any?>(
                    "categoryId" to legacyId,
                    "householdId" to householdId,
                    "type" to "Expense",
                    "name" to "🚗 Vehicles",
                    "subCategory" to "⛽ Fuel",
                    "isDeleted" to false,
                    "createdAt" to 100L,
                    "updatedAt" to 200L
                )
            )
        )

        syncRepo.processCategorySnapshot(snapshot)

        // 1. Room is cleanly reconciled with canonical category
        val localCats = db.categoryDao().getAllCategoriesList(householdId)
        assertEquals(1, localCats.size)
        assertEquals(expectedCanonicalId, localCats.first().id)

        // 2. Member must NOT enqueue any outbox mutations (zero cloud writes)
        val outbox = db.syncOutboxDao().getPendingEntries()
        assertEquals(0, outbox.size)
    }
}