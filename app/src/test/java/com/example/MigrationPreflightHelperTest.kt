package com.example

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.example.data.dao.ExchangeRateDao
import com.example.data.model.CategoryEntity
import com.example.data.model.ExchangeRateEntity
import com.example.data.model.TransactionEntity
import com.example.data.repository.HouseholdMemberInfo
import com.example.data.repository.LocalMigrationCounts
import com.example.data.repository.PreflightValidationResult
import com.example.data.util.CsvBackupManager
import com.example.data.util.MigrationPreflightHelper
import com.example.data.util.PreflightBackupResult
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
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
class MigrationPreflightHelperTest {

    private lateinit var app: Application
    private lateinit var tempDir: File
    private lateinit var fakeAuthRepo: FakeAuthRepository
    private lateinit var fakeTxRepo: FakeTransactionRepository
    private lateinit var fakeCatRepo: FakeCategoryRepository
    private lateinit var helper: MigrationPreflightHelper

    private val fakeExchangeRateDao = object : ExchangeRateDao {
        private val rates = mutableListOf<ExchangeRateEntity>()
        override suspend fun insertRate(rate: ExchangeRateEntity) { rates.add(rate) }
        override suspend fun insertAllRates(rates: List<ExchangeRateEntity>) { this.rates.addAll(rates) }
        override suspend fun getOfficialRateForDate(date: String): ExchangeRateEntity? = rates.find { (it.date == date || it.requestedDate == date) && it.source == "BNR_OFFICIAL" && it.status == "OFFICIAL" }
        override suspend fun getRateForDate(date: String): ExchangeRateEntity? = rates.find { it.date == date || it.requestedDate == date }
        override suspend fun getAllOfficialRates(): List<ExchangeRateEntity> = rates.filter { it.status == "OFFICIAL" }
        override suspend fun deleteUnverifiedRatesForDate(date: String): Int = 0
        override suspend fun deleteAllRates() { rates.clear() }
    }

    @Before
    fun setUp() {
        app = ApplicationProvider.getApplicationContext()
        tempDir = File(app.cacheDir, "migration_helper_test_${System.currentTimeMillis()}").apply { mkdirs() }
        fakeAuthRepo = FakeAuthRepository()
        fakeTxRepo = FakeTransactionRepository(fakeAuthRepo)
        fakeCatRepo = FakeCategoryRepository(fakeAuthRepo)
        helper = MigrationPreflightHelper(
            transactionRepository = fakeTxRepo,
            categoryRepository = fakeCatRepo,
            exchangeRateDao = fakeExchangeRateDao
        )
    }

    @After
    fun tearDown() {
        tempDir.deleteRecursively()
    }

    @Test
    fun test1_createPreflightBackup_createsValidBundleAndManifest() = runTest {
        fakeAuthRepo.signInWithTestUid("user_123")
        val tx = TransactionEntity(
            id = "tx_test_1",
            amountRON = 100.0,
            amountEUR = 20.0,
            exchangeRate = 5.0,
            exchangeRateDate = "2026-08-14",
            description = "Groceries",
            type = "Expense",
            category = "Food",
            subCategory = "Store",
            account = "Card",
            date = "2026-08-14",
            userId = "user_123"
        )
        fakeTxRepo.insertTransaction(tx)
        fakeCatRepo.addCategory("Food", "Expense", "Store", userId = "user_123")
        fakeExchangeRateDao.insertRate(ExchangeRateEntity(date = "2026-08-14", rate = 5.0, source = "BNR_OFFICIAL", status = "OFFICIAL"))

        val result = helper.createPreflightBackup(tempDir)
        assertTrue("Expected PreflightBackupResult.Success, got $result", result is PreflightBackupResult.Success)

        val bundleDir = (result as PreflightBackupResult.Success).backupBundleDir
        assertTrue(bundleDir.exists() && bundleDir.isDirectory)

        val manifestFile = File(bundleDir, CsvBackupManager.MANIFEST_FILE_NAME)
        val txFile = File(bundleDir, CsvBackupManager.TRANSACTIONS_FILE_NAME)
        val catFile = File(bundleDir, CsvBackupManager.CATEGORIES_FILE_NAME)
        val rateFile = File(bundleDir, CsvBackupManager.EXCHANGE_RATES_FILE_NAME)

        assertTrue(manifestFile.exists())
        assertTrue(txFile.exists())
        assertTrue(catFile.exists())
        assertTrue(rateFile.exists())

        val validation = CsvBackupManager.validateMigrationBackupBundle(bundleDir)
        assertTrue("Backup bundle must pass validation", validation.isValid)
    }

    @Test
    fun test2_sanitizeError_stripsStackTracesAndLimitsLength() {
        val rawTrace = "Database connection failed\n\tat com.example.db.DbHelper.open(DbHelper.kt:42)\n\tat java.lang.Thread.run(Thread.java:1012)"
        val sanitized = helper.sanitizeError(rawTrace)
        assertEquals("Database connection failed", sanitized)
        assertFalse(sanitized.contains("\tat "))
        assertFalse(sanitized.contains("DbHelper"))

        val longError = "A".repeat(200)
        val limited = helper.sanitizeError(longError)
        assertEquals(150, limited.length)

        assertEquals("An unexpected error occurred during migration.", helper.sanitizeError(null))
        assertEquals("An unexpected error occurred during migration.", helper.sanitizeError(""))
        assertEquals("An unexpected error occurred during migration.", helper.sanitizeError("   \n   "))
    }

    @Test
    fun test3_mapToPreviewState_convertsReadyResultWithManifestTimestamp() = runTest {
        val backupResult = helper.createPreflightBackup(tempDir)
        assertTrue(backupResult is PreflightBackupResult.Success)
        val bundleDir = (backupResult as PreflightBackupResult.Success).backupBundleDir

        val ready = PreflightValidationResult.Ready(
            householdId = "hh_alpha",
            userUid = "user_owner_42",
            memberInfo = HouseholdMemberInfo(role = "OWNER", status = "ACTIVE"),
            backupBundlePath = bundleDir.absolutePath,
            localCounts = LocalMigrationCounts(transactionsCount = 10, categoriesCount = 4, exchangeRatesCount = 2)
        )

        val preview = helper.mapToPreviewState(
            result = ready,
            resolvedHouseholdName = "Alpha Family",
            effectiveBackupDir = bundleDir
        )

        assertEquals("hh_alpha", preview.householdId)
        assertEquals("Alpha Family", preview.householdName)
        assertEquals("user_owner_42", preview.userUid)
        assertEquals("OWNER", preview.userRole)
        assertEquals(10, preview.transactionsCount)
        assertEquals(4, preview.categoriesCount)
        assertEquals(2, preview.exchangeRatesCount)
        assertEquals(16, preview.totalRecords)
        assertEquals(bundleDir.absolutePath, preview.backupBundlePath)
        assertNotNull(preview.backupTimestamp)
        assertTrue((preview.backupTimestamp ?: 0L) > 0)
        assertEquals("VALIDATED", preview.backupValidationStatus)
        assertEquals(ready, preview.preflightReadyData)
    }
}
