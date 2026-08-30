package com.example

import com.example.data.model.TransactionEntity
import com.example.data.repository.PreparedRepairItem
import com.example.data.repository.TransactionRepository
import com.example.data.service.BnrDiagnosticResult
import com.example.data.service.BnrRateResult
import com.example.data.util.CsvImportFinalResult
import com.example.data.util.CsvPreviewData
import com.example.data.util.HistoricalRateRepairCoordinator
import com.example.data.util.RepairExecutionResult
import com.example.ui.DiscrepancyItem
import com.example.ui.DiscrepancyReport
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class HistoricalRateRepairCoordinatorTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var cacheDir: File
    private lateinit var fakeTransactionRepository: TestTransactionRepository
    private lateinit var coordinator: HistoricalRateRepairCoordinator

    private class TestTransactionRepository : TransactionRepository {
        val transactions = mutableListOf<TransactionEntity>()
        val appliedRepairs = mutableListOf<PreparedRepairItem>()
        var officialRateToReturn = 5.0

        override val allTransactions: Flow<List<TransactionEntity>> = emptyFlow()
        override fun getTransactions(householdId: String?): Flow<List<TransactionEntity>> = emptyFlow()
        override fun getTransactionsInRange(startDate: String, endDate: String): Flow<List<TransactionEntity>> = emptyFlow()
        override suspend fun getTransactionById(id: String): TransactionEntity? = transactions.find { it.id == id }

        override suspend fun saveTransaction(
            id: String?, date: String, description: String, amountRON: Double,
            type: String, account: String, category: String, subCategory: String,
            destination: String?, userId: String, householdId: String?
        ): TransactionEntity = throw UnsupportedOperationException()

        override suspend fun createDuplicateTemplate(source: TransactionEntity): TransactionEntity = source
        override suspend fun getDescriptionSuggestions(query: String, limit: Int): List<String> = emptyList()
        override suspend fun insertBatchWithTransaction(transactions: List<TransactionEntity>) { this.transactions.addAll(transactions) }
        override suspend fun getUnverifiedTransactions(): List<TransactionEntity> = transactions.filter { it.conversionStatus != "OFFICIAL" }
        override suspend fun getAllTransactionsList(): List<TransactionEntity> = transactions.toList()
        override suspend fun syncPendingConversions(): com.example.data.repository.PendingRetryResult = com.example.data.repository.PendingRetryResult(0, 0, 0, 0, null)

        override suspend fun applyRepairBatch(repairs: List<PreparedRepairItem>): Int {
            appliedRepairs.addAll(repairs)
            return repairs.size
        }

        override suspend fun applyRepairToTransactionAndCache(transaction: TransactionEntity, officialRate: Double, effectiveBnrDate: String) {}
        override suspend fun insertTransaction(transaction: TransactionEntity) { transactions.add(transaction) }
        override suspend fun deleteTransaction(transaction: TransactionEntity) { transactions.remove(transaction) }
        override suspend fun deleteTransactionById(id: String) { transactions.removeAll { it.id == id } }
        override suspend fun deleteAllTransactions() { transactions.clear() }
        override suspend fun insertBatch(transactions: List<TransactionEntity>) { this.transactions.addAll(transactions) }

        override suspend fun getOfficialRate(date: String): BnrRateResult {
            return BnrRateResult(
                requestedDate = date,
                effectiveDate = date,
                rate = officialRateToReturn,
                source = "BNR_OFFICIAL",
                status = "OFFICIAL",
                diagnostic = null
            )
        }

        override suspend fun runBnrDiagnostic(): BnrDiagnosticResult = BnrDiagnosticResult(isReachable = true, httpStatus = "200")
        override suspend fun executeAtomicCsvImport(previewData: CsvPreviewData, backupFile: File, allExistingTransactions: List<TransactionEntity>): CsvImportFinalResult = CsvImportFinalResult(true, 0, 0, 0, 0, 0, 0, 0, 0)
    }

    @Before
    fun setUp() {
        cacheDir = tempFolder.newFolder("cache")
        fakeTransactionRepository = TestTransactionRepository()
        coordinator = HistoricalRateRepairCoordinator(fakeTransactionRepository)
    }

    @Test
    fun testGenerateDiscrepancyReportDetectsDifferencesAndCreatesBackup() = runBlocking {
        val tx1 = TransactionEntity(
            id = "tx_1",
            userId = "user_1",
            date = "2026-08-10",
            description = "Groceries",
            amountRON = 100.0,
            amountEUR = 18.0, // Should be 20.0 with 5.0 rate -> diff 2.0
            exchangeRate = 5.55,
            exchangeRateDate = "2026-08-10",
            conversionStatus = "ESTIMATED",
            type = "Expense",
            account = "Card",
            category = "Food",
            subCategory = "Groceries"
        )
        fakeTransactionRepository.transactions.add(tx1)

        val report = coordinator.generateDiscrepancyReport(cacheDir)

        assertEquals(1, report.items.size)
        val item = report.items[0]
        assertEquals("tx_1", item.transactionId)
        assertEquals(20.0, item.correctAmountEUR, 0.001)
        assertEquals(2.0, item.differenceEUR, 0.001)
        assertEquals(2.0, report.totalDiscrepancyEUR, 0.001)

        assertNotNull(report.backupFilePath)
        val backupFile = File(report.backupFilePath!!)
        assertTrue(backupFile.exists())
        assertTrue(backupFile.readText().contains("tx_1"))
    }

    @Test
    fun testConfirmAndApplyRepairAppliesBatchSuccessfully() = runBlocking {
        val tx1 = TransactionEntity(
            id = "tx_1",
            userId = "user_1",
            date = "2026-08-10",
            description = "Groceries",
            amountRON = 100.0,
            amountEUR = 18.0,
            exchangeRate = 5.55,
            exchangeRateDate = "2026-08-10",
            conversionStatus = "ESTIMATED",
            type = "Expense",
            account = "Card",
            category = "Food",
            subCategory = "Groceries"
        )
        fakeTransactionRepository.transactions.add(tx1)

        val discrepancyItem = DiscrepancyItem(
            transactionId = "tx_1",
            date = "2026-08-10",
            description = "Groceries",
            amountRON = 100.0,
            oldRate = 5.55,
            correctRate = 5.0,
            effectiveBnrDate = "2026-08-10",
            oldAmountEUR = 18.0,
            correctAmountEUR = 20.0,
            differenceEUR = 2.0
        )
        val report = DiscrepancyReport(
            items = listOf(discrepancyItem),
            totalDiscrepancyEUR = 2.0,
            backupFilePath = null
        )

        val result = coordinator.confirmAndApplyRepair(report, cacheDir)

        assertTrue(result is RepairExecutionResult.Success)
        assertEquals(1, (result as RepairExecutionResult.Success).updatedCount)
        assertEquals(1, fakeTransactionRepository.appliedRepairs.size)
        assertEquals("tx_1", fakeTransactionRepository.appliedRepairs[0].transaction.id)
        assertEquals(5.0, fakeTransactionRepository.appliedRepairs[0].officialRate, 0.001)
    }

    @Test
    fun testValidateBackupFileValidationRules() {
        val validFile = File(cacheDir, "valid_backup.csv").apply {
            writeText("Transaction_ID,Transaction_Date,Amount_RON,Amount_EUR\ntx_1,2026-08-10,100.0,20.0\n")
        }
        assertTrue(coordinator.validateBackupFile(validFile, 1))

        val countMismatchFile = File(cacheDir, "short_backup.csv").apply {
            writeText("Transaction_ID,Transaction_Date,Amount_RON,Amount_EUR\ntx_1,2026-08-10,100.0,20.0\n")
        }
        assertFalse(coordinator.validateBackupFile(countMismatchFile, 2))

        val invalidHeaderFile = File(cacheDir, "bad_header.csv").apply {
            writeText("ID,Date,Amount\ntx_1,2026-08-10,100.0\n")
        }
        assertFalse(coordinator.validateBackupFile(invalidHeaderFile, 1))

        val emptyFile = File(cacheDir, "empty.csv").apply {
            createNewFile()
        }
        assertFalse(coordinator.validateBackupFile(emptyFile, 0))

        val nonExistentFile = File(cacheDir, "does_not_exist.csv")
        assertFalse(coordinator.validateBackupFile(nonExistentFile, 0))
    }
}
