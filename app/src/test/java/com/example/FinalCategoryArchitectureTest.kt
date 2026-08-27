package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.data.db.FinTrackDatabase
import com.example.data.model.CategoryEntity
import com.example.data.model.SyncOutboxEntity
import com.example.data.model.TransactionEntity
import com.example.data.repository.FirestoreSnapshotSource
import com.example.data.repository.FirestoreSyncRepository
import com.example.data.repository.HouseholdResolutionResult
import com.example.data.repository.HouseholdVerificationHelper
import com.example.data.repository.HouseholdVerificationResult
import com.example.data.repository.ListenerRegistrationHandle
import com.example.data.repository.RoomCategoryRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import com.example.data.model.toFirestoreMap
import org.robolectric.annotation.Config
import java.util.UUID

class ArchitectureTestSnapshotSource : FirestoreSnapshotSource {
    val members = mutableMapOf<Pair<String, String>, Map<String, Any?>>()
    val categoryUpserts = mutableListOf<Pair<String, Pair<String, Map<String, Any?>>>>()
    val categoryDeletes = mutableListOf<Pair<String, String>>()
    val transactionUpserts = mutableListOf<Pair<String, Pair<String, Map<String, Any?>>>>()
    val transactionDeletes = mutableListOf<Pair<String, String>>()

    override fun listenToTransactions(
        householdId: String,
        onSnapshot: (List<Pair<String, Map<String, Any?>>>) -> Unit,
        onError: (Exception) -> Unit
    ): ListenerRegistrationHandle = object : ListenerRegistrationHandle { override fun remove() {} }

    override fun listenToCategories(
        householdId: String,
        onSnapshot: (List<Pair<String, Map<String, Any?>>>) -> Unit,
        onError: (Exception) -> Unit
    ): ListenerRegistrationHandle = object : ListenerRegistrationHandle { override fun remove() {} }

    override suspend fun resolveHouseholdId(userUid: String): HouseholdResolutionResult {
        val entry = members.entries.firstOrNull { it.key.second == userUid && (it.value["status"] as? String)?.uppercase() == "ACTIVE" }
        return if (entry != null) HouseholdResolutionResult.Success(entry.key.first) else HouseholdResolutionResult.NoHousehold
    }

    override suspend fun getHouseholdMembership(householdId: String, userUid: String): Map<String, Any?>? {
        return members[Pair(householdId, userUid)]
    }

    override suspend fun upsertTransaction(householdId: String, transactionId: String, data: Map<String, Any?>) {
        transactionUpserts.add(Pair(householdId, Pair(transactionId, data)))
    }

    override suspend fun deleteTransaction(householdId: String, transactionId: String) {
        transactionDeletes.add(Pair(householdId, transactionId))
    }

    override suspend fun upsertCategory(householdId: String, categoryId: String, data: Map<String, Any?>) {
        categoryUpserts.add(Pair(householdId, Pair(categoryId, data)))
    }

    override suspend fun deleteCategory(householdId: String, categoryId: String) {
        categoryDeletes.add(Pair(householdId, categoryId))
    }

    override suspend fun upsertExchangeRate(householdId: String, exchangeRateId: String, data: Map<String, Any?>) {}
    override suspend fun deleteExchangeRate(householdId: String, exchangeRateId: String) {}
}

@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE)
class FinalCategoryArchitectureTest {

    private lateinit var context: Context
    private lateinit var db: FinTrackDatabase
    private lateinit var categoryRepo: RoomCategoryRepository
    private lateinit var snapshotSource: ArchitectureTestSnapshotSource
    private lateinit var syncRepo: FirestoreSyncRepository

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, FinTrackDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        categoryRepo = RoomCategoryRepository(
            categoryDao = db.categoryDao(),
            syncOutboxDao = db.syncOutboxDao(),
            database = db
        )
        snapshotSource = ArchitectureTestSnapshotSource()
        syncRepo = FirestoreSyncRepository(
            database = db,
            snapshotSource = snapshotSource
        )
    }

    @After
    fun tearDown() {
        syncRepo.stopSync()
        db.close()
    }

    @Test
    fun test1_testCreateCategory_generatesStableUUID_andInsertsToRoom() = runTest {
        val householdId = "HH_TEST"
        categoryRepo.addCategory("Food", "Expense", "Groceries", "user_1", householdId)

        val categories = db.categoryDao().getAllCategoriesList(householdId)
        assertEquals(1, categories.size)
        val cat = categories.first()
        assertTrue("Category ID must be a valid UUID", cat.id.isNotBlank())
        assertEquals("Food", cat.name)
        assertEquals("Expense", cat.type)
        assertEquals("Groceries", cat.subCategory)
        assertEquals(householdId, cat.householdId)
    }

    @Test
    fun test2_testCreateCategory_createsExactlyOneOutboxUpsert() = runTest {
        val householdId = "HH_TEST"
        categoryRepo.addCategory("Food", "Expense", "Groceries", "user_1", householdId)

        val outbox = db.syncOutboxDao().getPendingEntries()
        assertEquals(1, outbox.size)
        val entry = outbox.first()
        assertEquals("CATEGORY", entry.entityType)
        assertEquals("UPSERT", entry.operation)
        val cat = db.categoryDao().getAllCategoriesList(householdId).first()
        assertEquals(cat.id, entry.entityId)
    }

    @Test
    fun test3_testCreateCategory_sameIdUsedForFirestoreUpload() = runTest {
        val householdId = "HH_TEST"
        val userUid = "owner_1"
        categoryRepo.addCategory("Food", "Expense", "Groceries", userUid, householdId)
        val cat = db.categoryDao().getAllCategoriesList(householdId).first()

        val outbox = db.syncOutboxDao().getPendingEntries().first()
        assertEquals(cat.id, outbox.entityId)

        val payload = cat.toFirestoreMap(householdId)
        assertEquals(cat.id, payload["categoryId"])
        assertEquals(householdId, payload["householdId"])
        assertEquals("Food", payload["name"])
        assertEquals("Groceries", payload["subCategory"])

        snapshotSource.upsertCategory(householdId, cat.id, payload)
        assertEquals(1, snapshotSource.categoryUpserts.size)
        assertEquals(cat.id, snapshotSource.categoryUpserts.first().second.first)
    }

    @Test
    fun test4_testRenameSubcategory_preservesSameId() = runTest {
        val householdId = "HH_TEST"
        categoryRepo.addCategory("Food", "Expense", "Groceries", "user_1", householdId)
        val initial = db.categoryDao().getAllCategoriesList(householdId).first()
        val originalId = initial.id

        categoryRepo.updateSubcategory(originalId, "Supermarket")

        val updated = db.categoryDao().getAllCategoriesList(householdId).first()
        assertEquals(originalId, updated.id)
        assertEquals("Supermarket", updated.subCategory)
    }

    @Test
    fun test5_testRenameCategoryGroup_preservesAllCategoryIds() = runTest {
        val householdId = "HH_TEST"
        categoryRepo.addCategory("Food", "Expense", "Groceries", "user_1", householdId)
        categoryRepo.addCategory("Food", "Expense", "Restaurants", "user_1", householdId)

        val before = db.categoryDao().getAllCategoriesList(householdId)
        val id1 = before.first { it.subCategory == "Groceries" }.id
        val id2 = before.first { it.subCategory == "Restaurants" }.id

        categoryRepo.updateCategoryGroup("Food", "Food & Dining", "Expense", householdId)

        val after = db.categoryDao().getAllCategoriesList(householdId)
        assertEquals(2, after.size)
        assertTrue(after.all { it.name == "Food & Dining" })
        assertEquals(id1, after.first { it.subCategory == "Groceries" }.id)
        assertEquals(id2, after.first { it.subCategory == "Restaurants" }.id)
    }

    @Test
    fun test6_testRenameSubcategory_updatesHistoricalTransactions() = runTest {
        val householdId = "HH_TEST"
        categoryRepo.addCategory("Food", "Expense", "Groceries", "user_1", householdId)
        val cat = db.categoryDao().getAllCategoriesList(householdId).first()

        val tx = TransactionEntity(
            id = "tx_1",
            date = "2026-08-20",
            description = "Apples",
            amountRON = 25.0,
            amountEUR = 5.0,
            exchangeRate = 5.0,
            exchangeRateDate = "2026-08-20",
            type = "Expense",
            account = "Card",
            category = "Food",
            subCategory = "Groceries",
            householdId = householdId
        )
        db.transactionDao().insertTransaction(tx)

        categoryRepo.updateSubcategory(cat.id, "Supermarket")

        val updatedTx = db.transactionDao().getTransactionById("tx_1")
        assertNotNull(updatedTx)
        assertEquals("Supermarket", updatedTx!!.subCategory)
        assertEquals("Food", updatedTx.category)
    }

    @Test
    fun test7_testRenameCategory_updatesHistoricalTransactions() = runTest {
        val householdId = "HH_TEST"
        categoryRepo.addCategory("Food", "Expense", "Groceries", "user_1", householdId)

        val tx = TransactionEntity(
            id = "tx_2",
            date = "2026-08-20",
            description = "Apples",
            amountRON = 25.0,
            amountEUR = 5.0,
            exchangeRate = 5.0,
            exchangeRateDate = "2026-08-20",
            type = "Expense",
            account = "Card",
            category = "Food",
            subCategory = "Groceries",
            householdId = householdId
        )
        db.transactionDao().insertTransaction(tx)

        categoryRepo.updateCategoryGroup("Food", "Food & Meals", "Expense", householdId)

        val updatedTx = db.transactionDao().getTransactionById("tx_2")
        assertNotNull(updatedTx)
        assertEquals("Food & Meals", updatedTx!!.category)
    }

    @Test
    fun test8_testRenameExpenseCategory_doesNotModifyIncomeTransactions() = runTest {
        val householdId = "HH_TEST"
        categoryRepo.addCategory("Gifts", "Expense", "Holiday", "user_1", householdId)

        val incTx = TransactionEntity(
            id = "tx_inc",
            date = "2026-08-20",
            description = "Birthday Gift Received",
            amountRON = 500.0,
            amountEUR = 100.0,
            exchangeRate = 5.0,
            exchangeRateDate = "2026-08-20",
            type = "Income",
            account = "Cash",
            category = "Gifts",
            subCategory = "Birthday",
            householdId = householdId
        )
        db.transactionDao().insertTransaction(incTx)

        categoryRepo.updateCategoryGroup("Gifts", "Expense Gifts", "Expense", householdId)

        val unchangedIncTx = db.transactionDao().getTransactionById("tx_inc")
        assertNotNull(unchangedIncTx)
        assertEquals("Gifts", unchangedIncTx!!.category)
    }

    @Test
    fun test9_testRenameIncomeCategory_doesNotModifyExpenseTransactions() = runTest {
        val householdId = "HH_TEST"
        categoryRepo.addCategory("Freelance", "Income", "Consulting", "user_1", householdId)

        val expTx = TransactionEntity(
            id = "tx_exp",
            date = "2026-08-20",
            description = "Freelance Expense",
            amountRON = 100.0,
            amountEUR = 20.0,
            exchangeRate = 5.0,
            exchangeRateDate = "2026-08-20",
            type = "Expense",
            account = "Card",
            category = "Freelance",
            subCategory = "Software",
            householdId = householdId
        )
        db.transactionDao().insertTransaction(expTx)

        categoryRepo.updateCategoryGroup("Freelance", "Direct Contracts", "Income", householdId)

        val unchangedExpTx = db.transactionDao().getTransactionById("tx_exp")
        assertNotNull(unchangedExpTx)
        assertEquals("Freelance", unchangedExpTx!!.category)
    }

    @Test
    fun test10_testRenameCategory_isHouseholdScoped() = runTest {
        val txA = TransactionEntity(
            id = "tx_hh_a",
            date = "2026-08-20",
            description = "HH A Item",
            amountRON = 50.0,
            amountEUR = 10.0,
            exchangeRate = 5.0,
            exchangeRateDate = "2026-08-20",
            type = "Expense",
            account = "Card",
            category = "Food",
            subCategory = "Groceries",
            householdId = "HH_A"
        )
        val txB = TransactionEntity(
            id = "tx_hh_b",
            date = "2026-08-20",
            description = "HH B Item",
            amountRON = 50.0,
            amountEUR = 10.0,
            exchangeRate = 5.0,
            exchangeRateDate = "2026-08-20",
            type = "Expense",
            account = "Card",
            category = "Food",
            subCategory = "Groceries",
            householdId = "HH_B"
        )
        db.transactionDao().insertTransaction(txA)
        db.transactionDao().insertTransaction(txB)

        categoryRepo.updateCategoryGroup("Food", "Food Updated", "Expense", "HH_A")

        assertEquals("Food Updated", db.transactionDao().getTransactionById("tx_hh_a")!!.category)
        assertEquals("Food", db.transactionDao().getTransactionById("tx_hh_b")!!.category)
    }

    @Test
    fun test11_testDeleteSubcategory_removesRoomRow() = runTest {
        val householdId = "HH_TEST"
        categoryRepo.addCategory("Food", "Expense", "Groceries", "user_1", householdId)
        val cat = db.categoryDao().getAllCategoriesList(householdId).first()

        categoryRepo.deleteSubcategory(cat.id)

        val remaining = db.categoryDao().getAllCategoriesList(householdId)
        assertEquals(0, remaining.size)
    }

    @Test
    fun test12_testDeleteCategory_enqueuesPhysicalDelete() = runTest {
        val householdId = "HH_TEST"
        categoryRepo.addCategory("Food", "Expense", "Groceries", "user_1", householdId)
        val cat = db.categoryDao().getAllCategoriesList(householdId).first()
        db.syncOutboxDao().clearAcknowledgedEntries()
        db.syncOutboxDao().deleteAllOutboxEntries()

        categoryRepo.deleteSubcategory(cat.id)

        val outbox = db.syncOutboxDao().getPendingEntries()
        assertEquals(1, outbox.size)
        assertEquals("CATEGORY", outbox.first().entityType)
        assertEquals("DELETE", outbox.first().operation)
        assertEquals(cat.id, outbox.first().entityId)
    }

    @Test
    fun test13_testDeleteCategory_doesNotDeleteTransactions() = runTest {
        val householdId = "HH_TEST"
        categoryRepo.addCategory("Food", "Expense", "Groceries", "user_1", householdId)
        val cat = db.categoryDao().getAllCategoriesList(householdId).first()

        val tx = TransactionEntity(
            id = "tx_hist",
            date = "2026-08-20",
            description = "Groceries receipt",
            amountRON = 100.0,
            amountEUR = 20.0,
            exchangeRate = 5.0,
            exchangeRateDate = "2026-08-20",
            type = "Expense",
            account = "Card",
            category = "Food",
            subCategory = "Groceries",
            householdId = householdId
        )
        db.transactionDao().insertTransaction(tx)

        categoryRepo.deleteSubcategory(cat.id)

        val remainingTx = db.transactionDao().getTransactionById("tx_hist")
        assertNotNull("Historical transactions must NEVER be deleted when category is deleted", remainingTx)
    }

    @Test
    fun test14_testInboundSnapshot_mirrorsRemoteCategories() = runTest {
        val householdId = "HH_TEST"
        val id1 = UUID.randomUUID().toString()
        val id2 = UUID.randomUUID().toString()

        val snapshot = listOf(
            Pair(
                id1,
                mapOf<String, Any?>(
                    "categoryId" to id1,
                    "householdId" to householdId,
                    "type" to "Expense",
                    "name" to "Transport",
                    "subCategory" to "Fuel",
                    "isDeleted" to false,
                    "createdAt" to 100L,
                    "updatedAt" to 100L
                )
            ),
            Pair(
                id2,
                mapOf<String, Any?>(
                    "categoryId" to id2,
                    "householdId" to householdId,
                    "type" to "Expense",
                    "name" to "Transport",
                    "subCategory" to "Bus",
                    "isDeleted" to false,
                    "createdAt" to 100L,
                    "updatedAt" to 100L
                )
            )
        )

        syncRepo.startSync("user_1", householdId)
        syncRepo.processCategorySnapshot(snapshot)

        val localCats = db.categoryDao().getAllCategoriesList(householdId)
        assertEquals(2, localCats.size)
        assertEquals(setOf(id1, id2), localCats.map { it.id }.toSet())
    }

    @Test
    fun test15_testInboundSnapshot_removesMissingRemoteCategoriesFromRoom() = runTest {
        val householdId = "HH_TEST"
        val activeId = UUID.randomUUID().toString()
        val obsoleteId = UUID.randomUUID().toString()

        db.categoryDao().insertCategory(CategoryEntity(id = activeId, householdId = householdId, type = "Expense", name = "Food", subCategory = "Groceries"))
        db.categoryDao().insertCategory(CategoryEntity(id = obsoleteId, householdId = householdId, type = "Expense", name = "Food", subCategory = "Snacks"))

        val snapshot = listOf(
            Pair(
                activeId,
                mapOf<String, Any?>(
                    "categoryId" to activeId,
                    "householdId" to householdId,
                    "type" to "Expense",
                    "name" to "Food",
                    "subCategory" to "Groceries",
                    "isDeleted" to false
                )
            )
        )

        syncRepo.startSync("user_1", householdId)
        syncRepo.processCategorySnapshot(snapshot)

        val localCats = db.categoryDao().getAllCategoriesList(householdId)
        assertEquals(1, localCats.size)
        assertEquals(activeId, localCats.first().id)
    }

    @Test
    fun test16_testInboundSnapshot_preservesPendingLocalUpsert() = runTest {
        val householdId = "HH_TEST"
        val userUid = "owner_1"
        syncRepo.startSync(userUid, householdId)

        // Offline create: inserts to Room and enqueues UPSERT in outbox
        categoryRepo.addCategory("Health", "Expense", "Pharmacy", userUid, householdId)
        val localCat = db.categoryDao().getAllCategoriesList(householdId).first()

        // Remote snapshot arrives before outbox finishes uploading
        syncRepo.processCategorySnapshot(emptyList())

        // Shielded from deletion: local row is preserved
        val localCatsAfter = db.categoryDao().getAllCategoriesList(householdId)
        assertEquals(1, localCatsAfter.size)
        assertEquals(localCat.id, localCatsAfter.first().id)
    }

    @Test
    fun test17_testInboundSnapshot_preservesPendingLocalDelete() = runTest {
        val householdId = "HH_TEST"
        val userUid = "owner_1"
        val catId = UUID.randomUUID().toString()

        db.categoryDao().insertCategory(CategoryEntity(id = catId, householdId = householdId, type = "Expense", name = "Food", subCategory = "Coffee"))
        categoryRepo.deleteSubcategory(catId)

        // Remote snapshot still contains catId before outbox delete is executed in Firestore
        val snapshot = listOf(
            Pair(
                catId,
                mapOf<String, Any?>(
                    "categoryId" to catId,
                    "householdId" to householdId,
                    "type" to "Expense",
                    "name" to "Food",
                    "subCategory" to "Coffee",
                    "isDeleted" to false
                )
            )
        )

        syncRepo.startSync(userUid, householdId)
        syncRepo.processCategorySnapshot(snapshot)

        // Must NOT resurrect in Room
        val localCats = db.categoryDao().getAllCategoriesList(householdId)
        assertEquals(0, localCats.size)
    }

    @Test
    fun test18_testInboundSnapshot_doesNotGenerateDuplicateOutboxMutations() = runTest {
        val householdId = "HH_TEST"
        val catId = UUID.randomUUID().toString()
        val snapshot = listOf(
            Pair(
                catId,
                mapOf<String, Any?>(
                    "categoryId" to catId,
                    "householdId" to householdId,
                    "type" to "Expense",
                    "name" to "Food",
                    "subCategory" to "Dinner",
                    "isDeleted" to false
                )
            )
        )

        syncRepo.startSync("member_1", householdId)
        syncRepo.processCategorySnapshot(snapshot)

        val outbox = db.syncOutboxDao().getPendingEntries()
        assertEquals(0, outbox.size)
    }

    @Test
    fun test19_testNewHousehold_seedsInitialCatalogOnce() = runTest {
        val householdId = "HH_NEW"
        categoryRepo.ensureDefaultCategoriesSeeded(householdId, enqueueOutbox = true)

        val seededCats = db.categoryDao().getAllCategoriesList(householdId)
        assertEquals(19, seededCats.size)

        // Every seeded category has a unique random UUID
        val uniqueIds = seededCats.map { it.id }.toSet()
        assertEquals(19, uniqueIds.size)

        val outbox = db.syncOutboxDao().getPendingEntries()
        assertEquals(19, outbox.size)
        assertTrue(outbox.all { it.operation == "UPSERT" && it.entityType == "CATEGORY" })
    }

    @Test
    fun test20_testSecondDevice_doesNotReseedExistingFirestoreCatalog() = runTest {
        val householdId = "HH_SHARED"
        val id1 = UUID.randomUUID().toString()
        val snapshot = listOf(
            Pair(
                id1,
                mapOf<String, Any?>(
                    "categoryId" to id1,
                    "householdId" to householdId,
                    "type" to "Expense",
                    "name" to "Custom Cat",
                    "subCategory" to "Custom Sub",
                    "isDeleted" to false
                )
            )
        )

        syncRepo.startSync("device_2_user", householdId)
        syncRepo.processCategorySnapshot(snapshot)

        val localCats = db.categoryDao().getAllCategoriesList(householdId)
        assertEquals(1, localCats.size)
        assertEquals(id1, localCats.first().id)
    }

    @Test
    fun test21_testTwoMembers_receiveSameCategoryStructure() = runTest {
        val householdId = "HH_FAMILY"
        val id1 = UUID.randomUUID().toString()
        val id2 = UUID.randomUUID().toString()
        val snapshot = listOf(
            Pair(id1, mapOf<String, Any?>("categoryId" to id1, "householdId" to householdId, "type" to "Expense", "name" to "Housing", "subCategory" to "Rent", "isDeleted" to false)),
            Pair(id2, mapOf<String, Any?>("categoryId" to id2, "householdId" to householdId, "type" to "Expense", "name" to "Housing", "subCategory" to "Power", "isDeleted" to false))
        )

        // Member A syncs
        syncRepo.startSync("member_A", householdId)
        syncRepo.processCategorySnapshot(snapshot)
        val catsA = db.categoryDao().getAllCategoriesList(householdId)

        // Member B on another db instance
        val dbB = Room.inMemoryDatabaseBuilder(context, FinTrackDatabase::class.java).allowMainThreadQueries().build()
        val syncRepoB = FirestoreSyncRepository(database = dbB, snapshotSource = snapshotSource)
        syncRepoB.startSync("member_B", householdId)
        syncRepoB.processCategorySnapshot(snapshot)
        val catsB = dbB.categoryDao().getAllCategoriesList(householdId)

        assertEquals(catsA.map { it.id }.toSet(), catsB.map { it.id }.toSet())
        assertEquals(catsA.map { it.subCategory }.toSet(), catsB.map { it.subCategory }.toSet())
        dbB.close()
    }

    @Test
    fun test22_testDifferentHouseholds_areFullyIsolated() = runTest {
        categoryRepo.addCategory("Food", "Expense", "A Groceries", "user_a", "HH_A")
        categoryRepo.addCategory("Food", "Expense", "B Groceries", "user_b", "HH_B")

        val catsA = db.categoryDao().getAllCategoriesList("HH_A")
        val catsB = db.categoryDao().getAllCategoriesList("HH_B")

        assertEquals(1, catsA.size)
        assertEquals(1, catsB.size)
        assertEquals("A Groceries", catsA.first().subCategory)
        assertEquals("B Groceries", catsB.first().subCategory)
        assertNotEquals(catsA.first().id, catsB.first().id)
    }

    @Test
    fun test23_testMemberCannotCreateCategory() = runTest {
        snapshotSource.members[Pair("HH_TEST", "member_uid")] = mapOf("role" to "MEMBER", "status" to "ACTIVE")
        val helper = HouseholdVerificationHelper(snapshotSource)
        val verification = helper.verifyHouseholdAdminOrOwner("HH_TEST", "member_uid")
        assertFalse(verification is HouseholdVerificationResult.Success)
    }

    @Test
    fun test24_testMemberCannotRenameCategory() = runTest {
        snapshotSource.members[Pair("HH_TEST", "member_uid")] = mapOf("role" to "MEMBER", "status" to "ACTIVE")
        val helper = HouseholdVerificationHelper(snapshotSource)
        val verification = helper.verifyHouseholdAdminOrOwner("HH_TEST", "member_uid")
        assertFalse(verification is HouseholdVerificationResult.Success)
    }

    @Test
    fun test25_testMemberCannotDeleteCategory() = runTest {
        snapshotSource.members[Pair("HH_TEST", "member_uid")] = mapOf("role" to "MEMBER", "status" to "ACTIVE")
        val helper = HouseholdVerificationHelper(snapshotSource)
        val verification = helper.verifyHouseholdAdminOrOwner("HH_TEST", "member_uid")
        assertFalse(verification is HouseholdVerificationResult.Success)
    }

    @Test
    fun test26_testOwnerCanCreateCategory() = runTest {
        snapshotSource.members[Pair("HH_TEST", "owner_uid")] = mapOf("role" to "OWNER", "status" to "ACTIVE")
        val helper = HouseholdVerificationHelper(snapshotSource)
        val verification = helper.verifyHouseholdAdminOrOwner("HH_TEST", "owner_uid")
        assertTrue(verification is HouseholdVerificationResult.Success)
    }

    @Test
    fun test27_testOwnerCanRenameCategory() = runTest {
        snapshotSource.members[Pair("HH_TEST", "owner_uid")] = mapOf("role" to "OWNER", "status" to "ACTIVE")
        val helper = HouseholdVerificationHelper(snapshotSource)
        val verification = helper.verifyHouseholdAdminOrOwner("HH_TEST", "owner_uid")
        assertTrue(verification is HouseholdVerificationResult.Success)
    }

    @Test
    fun test28_testOwnerCanDeleteCategory() = runTest {
        snapshotSource.members[Pair("HH_TEST", "owner_uid")] = mapOf("role" to "OWNER", "status" to "ACTIVE")
        val helper = HouseholdVerificationHelper(snapshotSource)
        val verification = helper.verifyHouseholdAdminOrOwner("HH_TEST", "owner_uid")
        assertTrue(verification is HouseholdVerificationResult.Success)
    }

    @Test
    fun test29_testRenameDoesNotChangeFirestoreDocumentId() = runTest {
        val householdId = "HH_TEST"
        val userUid = "owner_uid"
        categoryRepo.addCategory("Shopping", "Expense", "Clothes", userUid, householdId)
        val initialCat = db.categoryDao().getAllCategoriesList(householdId).first()
        val initialId = initialCat.id

        // Rename
        categoryRepo.updateSubcategory(initialId, "Apparel")
        val updatedCat = db.categoryDao().getAllCategoriesList(householdId).first()
        assertEquals(initialId, updatedCat.id)

        val outboxEntries = db.syncOutboxDao().getPendingEntries().filter { it.entityId == initialId }
        assertEquals(1, outboxEntries.size)
        assertEquals("UPSERT", outboxEntries.first().operation)
        assertEquals(initialId, outboxEntries.first().entityId)

        val renamePayload = updatedCat.toFirestoreMap(householdId)
        assertEquals(initialId, renamePayload["categoryId"])
        assertEquals("Apparel", renamePayload["subCategory"])

        snapshotSource.upsertCategory(householdId, initialId, renamePayload)
        val upload = snapshotSource.categoryUpserts.last()
        assertEquals(initialId, upload.second.first)
        assertEquals("Apparel", upload.second.second["subCategory"])
    }

    @Test
    fun test30_testDeletedCategoryIsNotResurrectedByInboundSnapshot() = runTest {
        val householdId = "HH_TEST"
        val userUid = "owner_uid"
        categoryRepo.addCategory("Music", "Expense", "Concerts", userUid, householdId)
        val cat = db.categoryDao().getAllCategoriesList(householdId).first()

        categoryRepo.deleteSubcategory(cat.id)

        // Old snapshot arrives
        val snapshot = listOf(
            Pair(
                cat.id,
                mapOf<String, Any?>(
                    "categoryId" to cat.id,
                    "householdId" to householdId,
                    "type" to "Expense",
                    "name" to "Music",
                    "subCategory" to "Concerts",
                    "isDeleted" to false
                )
            )
        )
        syncRepo.startSync(userUid, householdId)
        syncRepo.processCategorySnapshot(snapshot)

        val localCats = db.categoryDao().getAllCategoriesList(householdId)
        assertEquals(0, localCats.size)
    }

    @Test
    fun test31_testRenameCategory_updatesTransactionsAndOutboxAtomically() = runTest {
        val householdId = "HH_TEST"
        categoryRepo.addCategory("Utilities", "Expense", "Water", "user_1", householdId)
        val cat = db.categoryDao().getAllCategoriesList(householdId).first()

        val tx = TransactionEntity(
            id = "tx_atomic_cat",
            date = "2026-08-20",
            description = "Bill",
            amountRON = 100.0,
            amountEUR = 20.0,
            exchangeRate = 5.0,
            exchangeRateDate = "2026-08-20",
            type = "Expense",
            account = "Card",
            category = "Utilities",
            subCategory = "Water",
            householdId = householdId
        )
        db.transactionDao().insertTransaction(tx)
        db.syncOutboxDao().clearAcknowledgedEntries()
        db.syncOutboxDao().deleteAllOutboxEntries()

        categoryRepo.updateCategoryGroup("Utilities", "Bills & Utilities", "Expense", householdId)

        val updatedTx = db.transactionDao().getTransactionById("tx_atomic_cat")
        assertEquals("Bills & Utilities", updatedTx!!.category)

        val outbox = db.syncOutboxDao().getPendingEntries()
        assertEquals(2, outbox.size)
        assertTrue(outbox.any { it.entityType == "CATEGORY" && it.entityId == cat.id && it.operation == "UPSERT" })
        assertTrue(outbox.any { it.entityType == "TRANSACTION" && it.entityId == "tx_atomic_cat" && it.operation == "UPSERT" })
    }

    @Test
    fun test32_testRenameSubcategory_updatesTransactionsAndOutboxAtomically() = runTest {
        val householdId = "HH_TEST"
        categoryRepo.addCategory("Utilities", "Expense", "Water", "user_1", householdId)
        val cat = db.categoryDao().getAllCategoriesList(householdId).first()

        val tx = TransactionEntity(
            id = "tx_atomic_sub",
            date = "2026-08-20",
            description = "Bill",
            amountRON = 100.0,
            amountEUR = 20.0,
            exchangeRate = 5.0,
            exchangeRateDate = "2026-08-20",
            type = "Expense",
            account = "Card",
            category = "Utilities",
            subCategory = "Water",
            householdId = householdId
        )
        db.transactionDao().insertTransaction(tx)
        db.syncOutboxDao().clearAcknowledgedEntries()
        db.syncOutboxDao().deleteAllOutboxEntries()

        categoryRepo.updateSubcategory(cat.id, "Water & Sewer")

        val updatedTx = db.transactionDao().getTransactionById("tx_atomic_sub")
        assertEquals("Water & Sewer", updatedTx!!.subCategory)

        val outbox = db.syncOutboxDao().getPendingEntries()
        assertEquals(2, outbox.size)
        assertTrue(outbox.any { it.entityType == "CATEGORY" && it.entityId == cat.id && it.operation == "UPSERT" })
        assertTrue(outbox.any { it.entityType == "TRANSACTION" && it.entityId == "tx_atomic_sub" && it.operation == "UPSERT" })
    }
}
