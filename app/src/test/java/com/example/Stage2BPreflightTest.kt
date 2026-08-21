package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.db.FinTrackDatabase
import com.example.data.model.CategoryEntity
import com.example.data.model.ExchangeRateEntity
import com.example.data.model.TransactionEntity
import com.example.data.repository.ConflictReason
import com.example.data.repository.FirestoreMigrationPreflightCoordinator
import com.example.data.repository.MigrationSessionCreationResult
import com.example.data.repository.PreflightValidationResult
import com.example.data.util.CsvBackupManager
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class Stage2BPreflightTest {

    private lateinit var context: Context
    private lateinit var db: FinTrackDatabase
    private lateinit var fakeSnapshotSource: FakeSnapshotSource
    private lateinit var coordinator: FirestoreMigrationPreflightCoordinator
    private lateinit var tempBackupDir: File

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, FinTrackDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        fakeSnapshotSource = FakeSnapshotSource()
        coordinator = FirestoreMigrationPreflightCoordinator(
            database = db,
            snapshotSource = fakeSnapshotSource,
            backupManager = CsvBackupManager
        )

        tempBackupDir = File(context.cacheDir, "test_backup_${System.currentTimeMillis()}").apply {
            mkdirs()
        }
    }

    @After
    fun tearDown() {
        db.close()
        tempBackupDir.deleteRecursively()
    }

    @Test
    fun testCleanHouseholdPassesPreflight() = runTest {
        // Setup owner authorization
        fakeSnapshotSource.setMember("hh_clean_1", "user_owner_1", "OWNER", "ACTIVE")
        fakeSnapshotSource.remoteTransactionCount = 0
        fakeSnapshotSource.remoteCategoryCount = 0
        fakeSnapshotSource.activeMigrationSession = null

        // Populate some local Room rows to count
        db.categoryDao().insertCategory(
            CategoryEntity(id = "cat_1", name = "Groceries", type = "Expense")
        )
        db.transactionDao().insertTransaction(
            TransactionEntity(
                id = "tx_1",
                amountRON = 120.0,
                amountEUR = 24.0,
                exchangeRate = 5.0,
                exchangeRateDate = "2026-08-14",
                description = "Supermarket",
                type = "Expense",
                category = "Groceries",
                subCategory = "",
                account = "Card",
                date = "2026-08-14"
            )
        )
        db.exchangeRateDao().insertRate(
            ExchangeRateEntity(
                date = "2026-08-14",
                rate = 5.0,
                source = "BNR_OFFICIAL",
                status = "OFFICIAL"
            )
        )

        // Create valid backup bundle matching Room state
        CsvBackupManager.createMigrationBackupBundle(
            bundleDir = tempBackupDir,
            transactions = listOf(db.transactionDao().getAllTransactionsList().first()),
            categories = listOf(db.categoryDao().getAllCategoriesList().first()),
            exchangeRates = listOf(db.exchangeRateDao().getAllOfficialRates().first())
        )

        val result = coordinator.validatePreflight("hh_clean_1", "user_owner_1", tempBackupDir)
        assertTrue(result is PreflightValidationResult.Ready)
        val ready = result as PreflightValidationResult.Ready
        assertEquals("hh_clean_1", ready.householdId)
        assertEquals("user_owner_1", ready.userUid)
        assertEquals("OWNER", ready.memberInfo.role)
        assertEquals(1, ready.localCounts.transactionsCount)
        assertEquals(1, ready.localCounts.categoriesCount)
        assertEquals(1, ready.localCounts.exchangeRatesCount)
        assertEquals(3, ready.localCounts.totalCount)
    }

    @Test
    fun testActiveMigrationBlocksPreflight() = runTest {
        fakeSnapshotSource.setMember("hh_busy_1", "user_admin_1", "ADMIN", "ACTIVE")
        fakeSnapshotSource.activeMigrationSession = mapOf(
            "migrationId" to "mig_active_999",
            "householdId" to "hh_busy_1",
            "initiatedByUid" to "user_other",
            "stage" to "CATEGORIES_UPLOADING"
        )

        val result = coordinator.validatePreflight("hh_busy_1", "user_admin_1", tempBackupDir)
        assertTrue(result is PreflightValidationResult.Conflict)
        val conflict = result as PreflightValidationResult.Conflict
        assertEquals(ConflictReason.ACTIVE_MIGRATION_IN_PROGRESS, conflict.reason)
        assertTrue(conflict.details.contains("mig_active_999"))
        assertTrue(conflict.details.contains("CATEGORIES_UPLOADING"))
    }

    @Test
    fun testExistingTransactionConflictBlocksPreflight() = runTest {
        fakeSnapshotSource.setMember("hh_conflict_tx", "user_owner_1", "OWNER", "ACTIVE")
        fakeSnapshotSource.remoteTransactionCount = 42
        fakeSnapshotSource.remoteCategoryCount = 0
        fakeSnapshotSource.activeMigrationSession = null

        val result = coordinator.validatePreflight("hh_conflict_tx", "user_owner_1", tempBackupDir)
        assertTrue(result is PreflightValidationResult.Conflict)
        val conflict = result as PreflightValidationResult.Conflict
        assertEquals(ConflictReason.EXISTING_REMOTE_DATA_DETECTED, conflict.reason)
        assertTrue(conflict.details.contains("transaction"))
        assertTrue(conflict.details.contains("42 documents found"))
    }

    @Test
    fun testExistingCategoryConflictBlocksPreflight() = runTest {
        fakeSnapshotSource.setMember("hh_conflict_cat", "user_owner_1", "OWNER", "ACTIVE")
        fakeSnapshotSource.remoteTransactionCount = 0
        fakeSnapshotSource.remoteCategoryCount = 8
        fakeSnapshotSource.activeMigrationSession = null

        val result = coordinator.validatePreflight("hh_conflict_cat", "user_owner_1", tempBackupDir)
        assertTrue(result is PreflightValidationResult.Conflict)
        val conflict = result as PreflightValidationResult.Conflict
        assertEquals(ConflictReason.EXISTING_REMOTE_DATA_DETECTED, conflict.reason)
        assertTrue(conflict.details.contains("category"))
        assertTrue(conflict.details.contains("8 documents found"))
    }

    @Test
    fun testExistingExchangeRateConflictBlocksPreflight() = runTest {
        fakeSnapshotSource.setMember("hh_conflict_rate", "user_owner_1", "OWNER", "ACTIVE")
        fakeSnapshotSource.remoteTransactionCount = 0
        fakeSnapshotSource.remoteCategoryCount = 0
        fakeSnapshotSource.remoteExchangeRateCount = 5
        fakeSnapshotSource.activeMigrationSession = null

        val result = coordinator.validatePreflight("hh_conflict_rate", "user_owner_1", tempBackupDir)
        assertTrue(result is PreflightValidationResult.Conflict)
        val conflict = result as PreflightValidationResult.Conflict
        assertEquals(ConflictReason.EXISTING_REMOTE_DATA_DETECTED, conflict.reason)
        assertTrue(conflict.details.contains("exchange rate"))
        assertTrue(conflict.details.contains("5 documents found"))
    }

    @Test
    fun testNonAdminNonOwnerBlocked() = runTest {
        fakeSnapshotSource.setMember("hh_role_check", "user_regular_member", "MEMBER", "ACTIVE")

        val result = coordinator.validatePreflight("hh_role_check", "user_regular_member", tempBackupDir)
        assertTrue(result is PreflightValidationResult.Conflict)
        val conflict = result as PreflightValidationResult.Conflict
        assertEquals(ConflictReason.INSUFFICIENT_PERMISSIONS, conflict.reason)
        assertTrue(conflict.details.contains("Insufficient permissions"))
        assertTrue(conflict.details.contains("MEMBER"))
    }

    @Test
    fun testSuccessfulMigrationStateCreation() = runTest {
        fakeSnapshotSource.setMember("hh_create_success", "user_owner_2", "OWNER", "ACTIVE")
        fakeSnapshotSource.remoteTransactionCount = 0
        fakeSnapshotSource.remoteCategoryCount = 0
        fakeSnapshotSource.remoteExchangeRateCount = 0
        fakeSnapshotSource.activeMigrationSession = null

        val cat1 = CategoryEntity(id = "cat_a", name = "Dining", type = "Expense")
        val cat2 = CategoryEntity(id = "cat_b", name = "Salary", type = "Income")
        db.categoryDao().insertCategory(cat1)
        db.categoryDao().insertCategory(cat2)

        CsvBackupManager.createMigrationBackupBundle(
            bundleDir = tempBackupDir,
            transactions = emptyList(),
            categories = listOf(cat1, cat2),
            exchangeRates = emptyList()
        )

        val preflightResult = coordinator.validatePreflight("hh_create_success", "user_owner_2", tempBackupDir)
        assertTrue(preflightResult is PreflightValidationResult.Ready)
        val ready = preflightResult as PreflightValidationResult.Ready

        val createResult = coordinator.createMigrationSession(ready, migrationIdOverride = "mig_session_123")
        assertTrue(createResult is MigrationSessionCreationResult.Success)
        val success = createResult as MigrationSessionCreationResult.Success
        assertEquals("mig_session_123", success.migrationId)
        assertEquals("PREFLIGHT", success.entity.stage)
        assertEquals("INITIALIZED", success.entity.currentPhase)
        assertEquals(2, success.entity.totalCount)

        // Verify local Room persistence
        val persistedEntity = db.migrationStateDao().getMigrationStateById("mig_session_123")
        assertNotNull(persistedEntity)
        assertEquals("hh_create_success", persistedEntity!!.householdId)
        assertEquals("user_owner_2", persistedEntity.initiatedByUid)
        assertEquals("PREFLIGHT", persistedEntity.stage)
        assertEquals(2, persistedEntity.totalCount)

        // Verify remote snapshot source write
        val remoteDoc = fakeSnapshotSource.createdMigrationDocs["mig_session_123"]
        assertNotNull(remoteDoc)
        assertEquals("mig_session_123", remoteDoc!!["migrationId"])
        assertEquals("hh_create_success", remoteDoc["householdId"])
        assertEquals("user_owner_2", remoteDoc["initiatedByUid"])
        assertEquals("PREFLIGHT", remoteDoc["stage"])
        assertEquals(2, remoteDoc["totalCount"])
    }

    @Test
    fun testMigrationStateFieldsSatisfySecurityContract() = runTest {
        fakeSnapshotSource.setMember("hh_contract_check", "user_admin_contract", "ADMIN", "ACTIVE")
        fakeSnapshotSource.remoteTransactionCount = 0
        fakeSnapshotSource.remoteCategoryCount = 0
        fakeSnapshotSource.remoteExchangeRateCount = 0
        fakeSnapshotSource.activeMigrationSession = null

        val preflight = coordinator.validatePreflight("hh_contract_check", "user_admin_contract", tempBackupDir)
        assertTrue(preflight is PreflightValidationResult.Ready)
        val ready = preflight as PreflightValidationResult.Ready

        val createResult = coordinator.createMigrationSession(ready, "mig_contract_001")
        assertTrue(createResult is MigrationSessionCreationResult.Success)
        val remoteDoc = fakeSnapshotSource.createdMigrationDocs["mig_contract_001"]!!

        assertEquals("hh_contract_check", remoteDoc["householdId"])
        assertEquals("user_admin_contract", remoteDoc["initiatedByUid"])
        val validStages = setOf(
            "PREFLIGHT", "BACKUP_CREATED", "CATEGORIES_UPLOADING",
            "RATES_UPLOADING", "TRANSACTIONS_UPLOADING", "VERIFYING",
            "COMPLETED", "FAILED", "CANCELLED"
        )
        assertTrue(remoteDoc["stage"] in validStages)
        assertNotNull(remoteDoc["createdAt"])
        assertNotNull(remoteDoc["updatedAt"])
    }

    @Test
    fun testSanitizedErrorsContainNoStackTraces() = runTest {
        val failingSnapshotSource = object : com.example.data.repository.FirestoreSnapshotSource {
            override fun listenToTransactions(householdId: String, onSnapshot: (List<Pair<String, Map<String, Any?>>>) -> Unit, onError: (Exception) -> Unit) =
                object : com.example.data.repository.ListenerRegistrationHandle { override fun remove() {} }
            override fun listenToCategories(householdId: String, onSnapshot: (List<Pair<String, Map<String, Any?>>>) -> Unit, onError: (Exception) -> Unit) =
                object : com.example.data.repository.ListenerRegistrationHandle { override fun remove() {} }
            override suspend fun resolveHouseholdId(userUid: String): com.example.data.repository.HouseholdResolutionResult =
                com.example.data.repository.HouseholdResolutionResult.NoHousehold
            override suspend fun getHouseholdMembership(householdId: String, userUid: String): Map<String, Any?>? {
                throw IllegalStateException("INTERNAL_DATABASE_CRASH: Fatal SQLite/Firestore error\n\tat com.google.cloud.FirestoreWorker.doWork(FirestoreWorker.kt:312)\n\tat java.lang.Thread.run(Thread.java:1012)")
            }
        }

        val failingCoordinator = FirestoreMigrationPreflightCoordinator(
            database = db,
            snapshotSource = failingSnapshotSource
        )

        val result = failingCoordinator.validatePreflight("hh_err", "user_1", tempBackupDir)
        assertTrue(result is PreflightValidationResult.Failure)
        val failure = result as PreflightValidationResult.Failure

        assertFalse(failure.sanitizedError.contains("\tat "))
        assertFalse(failure.sanitizedError.contains("FirestoreWorker.kt"))
        assertFalse(failure.sanitizedError.contains("Thread.java"))
        assertTrue(failure.sanitizedError.startsWith("Household verification error:"))
    }
}
