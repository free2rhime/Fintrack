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
import java.util.UUID

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

    override suspend fun deleteCategoriesNotInList(activeIds: List<String>, householdId: String?) {
        memory.removeAll {
            ((householdId == null && it.householdId == null) || it.householdId == householdId) &&
                    !activeIds.contains(it.id)
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
    fun test12_seedingHousehold_generatesUniqueStableUUIDs_andIsIdempotent() = runTest {
        val dao = InMemoryCategoryDao()
        val repo = RoomCategoryRepository(categoryDao = dao)

        // First seed
        repo.ensureDefaultCategoriesSeeded("HH_A")
        val cats = dao.getAllCategoriesList("HH_A")
        assertEquals(19, cats.size)
        val uniqueIds = cats.map { it.id }.toSet()
        assertEquals(19, uniqueIds.size)
        assertTrue(uniqueIds.all { it.isNotBlank() })

        // Second call on the same DB must be idempotent (no duplicate rows created)
        repo.ensureDefaultCategoriesSeeded("HH_A")
        val catsAfter = dao.getAllCategoriesList("HH_A")
        assertEquals(19, catsAfter.size)
    }

    @Test
    fun test13_seedingHousehold_enqueuesExactOutboxUpsertsForCreatedCategories() = runTest {
        val dao = InMemoryCategoryDao()
        val outboxDao = InMemorySyncOutboxDao()
        val repo = RoomCategoryRepository(categoryDao = dao, syncOutboxDao = outboxDao)

        repo.ensureDefaultCategoriesSeeded("HH_A", enqueueOutbox = true)

        val cats = dao.getAllCategoriesList("HH_A")
        assertEquals(19, cats.size)

        val outbox = outboxDao.getPendingEntries()
        assertEquals(19, outbox.size)
        val outboxEntityIds = outbox.map { it.entityId }.toSet()
        val catIds = cats.map { it.id }.toSet()
        assertEquals(catIds, outboxEntityIds)
        assertTrue(outbox.all { it.operation == "UPSERT" && it.entityType == "CATEGORY" })
    }

    @Test
    fun test14_secondDevice_mirrorsFirstDeviceCategoriesViaInboundSync() = runTest {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val dbA = Room.inMemoryDatabaseBuilder(context, FinTrackDatabase::class.java).allowMainThreadQueries().build()
        val dbB = Room.inMemoryDatabaseBuilder(context, FinTrackDatabase::class.java).allowMainThreadQueries().build()
        val snapshotSource = TestSnapshotSource()
        val householdId = "HH_A"
        val userUid = "owner_uid"
        snapshotSource.members[Pair(householdId, userUid)] = mapOf("role" to "OWNER", "status" to "ACTIVE")

        // Device A: Seeds categories
        val repoA = RoomCategoryRepository(categoryDao = dbA.categoryDao(), syncOutboxDao = dbA.syncOutboxDao(), database = dbA)
        repoA.ensureDefaultCategoriesSeeded(householdId, enqueueOutbox = true)
        val catsA = dbA.categoryDao().getAllCategoriesList(householdId)
        assertEquals(19, catsA.size)

        // Construct remote snapshot from Device A's categories
        val remoteSnapshot = catsA.map { cat ->
            Pair(
                cat.id,
                mapOf<String, Any?>(
                    "categoryId" to cat.id,
                    "householdId" to cat.householdId,
                    "type" to cat.type,
                    "name" to cat.name,
                    "subCategory" to cat.subCategory,
                    "isDeleted" to false,
                    "createdAt" to cat.createdAt,
                    "updatedAt" to cat.updatedAt
                )
            )
        }

        // Device B: Connects and receives remote snapshot via inbound sync
        val syncRepoB = FirestoreSyncRepository(database = dbB, snapshotSource = snapshotSource)
        syncRepoB.startSync(userUid = "member_uid", requestedHouseholdId = householdId)
        syncRepoB.processCategorySnapshot(remoteSnapshot)

        val catsB = dbB.categoryDao().getAllCategoriesList(householdId)
        assertEquals(19, catsB.size)
        assertEquals(catsA.map { it.id }.toSet(), catsB.map { it.id }.toSet())
        assertEquals(catsA.map { Triple(it.type, it.name, it.subCategory) }.toSet(), catsB.map { Triple(it.type, it.name, it.subCategory) }.toSet())

        dbA.close()
        dbB.close()
    }

    @Test
    fun test15_joiningMemberSeedingWithEnqueueOutboxFalse_doesNotEnqueueOutboxUpserts() = runTest {
        val dao = InMemoryCategoryDao()
        val outboxDao = InMemorySyncOutboxDao()
        val repo = RoomCategoryRepository(categoryDao = dao, syncOutboxDao = outboxDao)

        // Seeding with enqueueOutbox = false (as called when joining/starting an existing household)
        repo.ensureDefaultCategoriesSeeded("HH_A", enqueueOutbox = false)

        val localCats = dao.getAllCategoriesList("HH_A")
        val outboxEntries = outboxDao.getPendingEntries()
        assertEquals(0, outboxEntries.size)
    }

    @Test
    fun test16_testInboundSync_mirrorsSnapshotDirectlyIntoRoom() = runTest {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val db = Room.inMemoryDatabaseBuilder(context, FinTrackDatabase::class.java).allowMainThreadQueries().build()
        val snapshotSource = TestSnapshotSource()
        val householdId = "FzzoMr4dohrfFFCxuzo"
        val userUid = "owner_uid"
        snapshotSource.members[Pair(householdId, userUid)] = mapOf("role" to "OWNER", "status" to "ACTIVE")

        val syncRepo = FirestoreSyncRepository(database = db, snapshotSource = snapshotSource)
        syncRepo.startSync(userUid = userUid, requestedHouseholdId = householdId)

        val catId1 = UUID.randomUUID().toString()
        val catId2 = UUID.randomUUID().toString()

        val snapshot = listOf(
            Pair(
                catId1,
                mapOf<String, Any?>(
                    "categoryId" to catId1,
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
                catId2,
                mapOf<String, Any?>(
                    "categoryId" to catId2,
                    "householdId" to householdId,
                    "type" to "Expense",
                    "name" to "🍉 Food & Dining",
                    "subCategory" to "🛒 Groceries",
                    "isDeleted" to false,
                    "createdAt" to 900L,
                    "updatedAt" to 950L
                )
            )
        )

        syncRepo.processCategorySnapshot(snapshot)

        val localCats = db.categoryDao().getAllCategoriesList(householdId)
        assertEquals(2, localCats.size)
        val localIds = localCats.map { it.id }.toSet()
        assertEquals(setOf(catId1, catId2), localIds)
    }

    @Test
    fun test17_testInboundSync_mirrorsAllActiveDocumentsDirectly() = runTest {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val db = Room.inMemoryDatabaseBuilder(context, FinTrackDatabase::class.java).allowMainThreadQueries().build()
        val snapshotSource = TestSnapshotSource()
        val householdId = "HH_ABC"
        val userUid = "admin_uid"
        snapshotSource.members[Pair(householdId, userUid)] = mapOf("role" to "ADMIN", "status" to "ACTIVE")

        val syncRepo = FirestoreSyncRepository(database = db, snapshotSource = snapshotSource)
        syncRepo.startSync(userUid = userUid, requestedHouseholdId = householdId)

        val id1 = UUID.randomUUID().toString()
        val id2 = UUID.randomUUID().toString()
        val id3 = UUID.randomUUID().toString()

        val snapshot = listOf(
            Pair(
                id1,
                mapOf<String, Any?>(
                    "categoryId" to id1,
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
                id2,
                mapOf<String, Any?>(
                    "categoryId" to id2,
                    "householdId" to householdId,
                    "type" to "Expense",
                    "name" to "🏥 Health & Wellness",
                    "subCategory" to "🏋️ Gym & Fitness",
                    "isDeleted" to false,
                    "createdAt" to 200L,
                    "updatedAt" to 250L
                )
            ),
            Pair(
                id3,
                mapOf<String, Any?>(
                    "categoryId" to id3,
                    "householdId" to householdId,
                    "type" to "Expense",
                    "name" to "🎬 Entertainment",
                    "subCategory" to "🍿 Subscriptions",
                    "isDeleted" to false,
                    "createdAt" to 300L,
                    "updatedAt" to 350L
                )
            )
        )

        syncRepo.processCategorySnapshot(snapshot)

        val localCats = db.categoryDao().getAllCategoriesList(householdId)
        assertEquals(3, localCats.size)
        assertEquals(setOf(id1, id2, id3), localCats.map { it.id }.toSet())
    }

    @Test
    fun test18_testInboundSync_missingDocumentInSnapshotDeletesLocalRoomRow() = runTest {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val db = Room.inMemoryDatabaseBuilder(context, FinTrackDatabase::class.java).allowMainThreadQueries().build()
        val snapshotSource = TestSnapshotSource()
        val householdId = "HH_ABC"
        val userUid = "owner_uid"
        snapshotSource.members[Pair(householdId, userUid)] = mapOf("role" to "OWNER", "status" to "ACTIVE")

        val syncRepo = FirestoreSyncRepository(database = db, snapshotSource = snapshotSource)
        syncRepo.startSync(userUid = userUid, requestedHouseholdId = householdId)

        val activeId = UUID.randomUUID().toString()
        val deletedId = UUID.randomUUID().toString()

        // Seed both into Room
        db.categoryDao().insertCategory(
            CategoryEntity(id = activeId, householdId = householdId, type = "Expense", name = "Food", subCategory = "Groceries")
        )
        db.categoryDao().insertCategory(
            CategoryEntity(id = deletedId, householdId = householdId, type = "Expense", name = "OldCat", subCategory = "OldSub")
        )

        assertEquals(2, db.categoryDao().getAllCategoriesList(householdId).size)

        // Snapshot contains ONLY activeId (deletedId was physically deleted in Firestore)
        val snapshot = listOf(
            Pair(
                activeId,
                mapOf<String, Any?>(
                    "categoryId" to activeId,
                    "householdId" to householdId,
                    "type" to "Expense",
                    "name" to "Food",
                    "subCategory" to "Groceries",
                    "isDeleted" to false,
                    "createdAt" to 100L,
                    "updatedAt" to 500L
                )
            )
        )

        syncRepo.processCategorySnapshot(snapshot)

        // Room now contains ONLY activeId
        val localCats = db.categoryDao().getAllCategoriesList(householdId)
        assertEquals(1, localCats.size)
        assertEquals(activeId, localCats.first().id)
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

        val id1 = UUID.randomUUID().toString()

        val snapshot = listOf(
            Pair(
                id1,
                mapOf<String, Any?>(
                    "categoryId" to id1,
                    "householdId" to householdId,
                    "type" to "Expense",
                    "name" to "🏥 Health & Wellness",
                    "subCategory" to "💊 Pharmacy & Medical",
                    "isDeleted" to false,
                    "createdAt" to 100L,
                    "updatedAt" to 100L
                )
            )
        )

        // Run 1
        syncRepo.processCategorySnapshot(snapshot)
        assertEquals(1, db.categoryDao().getAllCategoriesList(householdId).size)

        // Run 2 (Repeated snapshot)
        syncRepo.processCategorySnapshot(snapshot)
        assertEquals(1, db.categoryDao().getAllCategoriesList(householdId).size)
    }

    @Test
    fun test20_testDeleteSubcategory_removesRoomRowAndEnqueuesOutboxDelete() = runTest {
        val dao = InMemoryCategoryDao()
        val outboxDao = InMemorySyncOutboxDao()
        val repo = RoomCategoryRepository(categoryDao = dao, syncOutboxDao = outboxDao)

        val householdId = "HH_A"
        val catId = UUID.randomUUID().toString()

        // Seed into Room
        dao.insertCategory(
            CategoryEntity(
                id = catId,
                householdId = householdId,
                type = "Expense",
                name = "🏥 Health & Wellness",
                subCategory = "💊 Pharmacy & Medical"
            )
        )

        assertEquals(1, dao.getAllCategoriesList(householdId).size)

        // Delete subcategory
        repo.deleteSubcategory(catId)

        // Row must be removed from Room
        val remaining = dao.getAllCategoriesList(householdId)
        assertEquals(0, remaining.size)

        // Outbox must contain DELETE
        val outbox = outboxDao.getPendingEntries()
        assertEquals(1, outbox.size)
        assertEquals(catId, outbox.first().entityId)
        assertEquals("DELETE", outbox.first().operation)
    }

    @Test
    fun test21_testReconciliation_preservesHouseholdAndTypeIsolation() = runTest {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val db = Room.inMemoryDatabaseBuilder(context, FinTrackDatabase::class.java).allowMainThreadQueries().build()
        val snapshotSource = TestSnapshotSource()
        val syncRepo = FirestoreSyncRepository(database = db, snapshotSource = snapshotSource)

        val idAExp = UUID.randomUUID().toString()
        val idAInc = UUID.randomUUID().toString()
        val idBExp = UUID.randomUUID().toString()
        val idBInc = UUID.randomUUID().toString()

        val snapshot = listOf(
            Pair(
                idAExp,
                mapOf<String, Any?>(
                    "categoryId" to idAExp,
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
                idAInc,
                mapOf<String, Any?>(
                    "categoryId" to idAInc,
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
                idBExp,
                mapOf<String, Any?>(
                    "categoryId" to idBExp,
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
                idBInc,
                mapOf<String, Any?>(
                    "categoryId" to idBInc,
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

        assertEquals(idAExp, expA.id)
        assertEquals(idAInc, incA.id)
        assertEquals(idBExp, expB.id)
        assertEquals(idBInc, incB.id)
    }

    @Test
    fun test22_customCategory_creationGeneratesRandomUUIDAndSurvivesInboundSync() = runTest {
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

        val localCats = db.categoryDao().getAllCategoriesList(householdId)
        assertEquals(1, localCats.size)
        val createdId = localCats.first().id
        assertTrue("Category ID must be valid UUID", createdId.isNotBlank())
        assertEquals("🚗 Vehicles", localCats.first().name)
        assertEquals("⛽ Fuel", localCats.first().subCategory)

        // 2. Inbound snapshot receives the document with the same ID
        val snapshot = listOf(
            Pair(
                createdId,
                mapOf<String, Any?>(
                    "categoryId" to createdId,
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
        assertEquals(createdId, catsAfterSync.first().id)
        assertFalse(catsAfterSync.first().isDeleted)
    }

    @Test
    fun test23_inboundSync_shieldsPendingLocalUpsertFromDeletion() = runTest {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val db = Room.inMemoryDatabaseBuilder(context, FinTrackDatabase::class.java).allowMainThreadQueries().build()
        val snapshotSource = TestSnapshotSource()
        val householdId = "HH_CUSTOM_2"
        val userUid = "owner_uid"
        snapshotSource.members[Pair(householdId, userUid)] = mapOf("role" to "OWNER", "status" to "ACTIVE")

        val syncRepo = FirestoreSyncRepository(database = db, snapshotSource = snapshotSource)
        syncRepo.startSync(userUid = userUid, requestedHouseholdId = householdId)

        val repo = RoomCategoryRepository(
            categoryDao = db.categoryDao(),
            syncOutboxDao = db.syncOutboxDao(),
            database = db
        )

        // Locally create category (enqueues UPSERT in outbox)
        repo.addCategory("Food", "Expense", "Groceries", userUid, householdId)
        val localCats = db.categoryDao().getAllCategoriesList(householdId)
        val localId = localCats.first().id

        // Empty remote snapshot arrives before outbox uploads localId
        val emptySnapshot = emptyList<Pair<String, Map<String, Any?>>>()
        syncRepo.processCategorySnapshot(emptySnapshot)

        // Local category is shielded and NOT deleted
        val catsAfterSnapshot = db.categoryDao().getAllCategoriesList(householdId)
        assertEquals(1, catsAfterSnapshot.size)
        assertEquals(localId, catsAfterSnapshot.first().id)
    }

    @Test
    fun test24_inboundSync_memberRole_doesNotEnqueueCloudMutations() = runTest {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val db = Room.inMemoryDatabaseBuilder(context, FinTrackDatabase::class.java).allowMainThreadQueries().build()
        val snapshotSource = TestSnapshotSource()
        val householdId = "HH_CUSTOM_3"
        val memberUid = "member_uid"
        snapshotSource.members[Pair(householdId, memberUid)] = mapOf("role" to "MEMBER", "status" to "ACTIVE")

        val syncRepo = FirestoreSyncRepository(database = db, snapshotSource = snapshotSource)
        syncRepo.startSync(userUid = memberUid, requestedHouseholdId = householdId)

        val catId = UUID.randomUUID().toString()

        val snapshot = listOf(
            Pair(
                catId,
                mapOf<String, Any?>(
                    "categoryId" to catId,
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

        // 1. Room is cleanly mirrored
        val localCats = db.categoryDao().getAllCategoriesList(householdId)
        assertEquals(1, localCats.size)
        assertEquals(catId, localCats.first().id)

        // 2. Member must NOT enqueue any outbox mutations (zero cloud writes)
        val outbox = db.syncOutboxDao().getPendingEntries()
        assertEquals(0, outbox.size)
    }
}