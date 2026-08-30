package com.example

import android.app.Application
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.TransactionEntity
import com.example.data.util.CsvDuplicateMode
import com.example.data.util.CsvExporter
import com.example.data.util.CsvImportOrchestrator
import com.example.data.util.CsvImportParseResult
import com.example.data.util.CsvPreviewData
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class CsvImportOrchestratorTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var app: Application
    private lateinit var cacheDir: File
    private lateinit var fakeTransactionRepository: FakeTransactionRepository
    private lateinit var fakeCategoryRepository: FakeCategoryRepository
    private lateinit var orchestrator: CsvImportOrchestrator

    @Before
    fun setUp() {
        app = ApplicationProvider.getApplicationContext()
        cacheDir = tempFolder.newFolder("cache")
        fakeTransactionRepository = FakeTransactionRepository()
        fakeCategoryRepository = FakeCategoryRepository()
        orchestrator = CsvImportOrchestrator(fakeTransactionRepository, fakeCategoryRepository)
    }

    @Test
    fun testParseAndValidateFromUriEmptyFile() = runBlocking {
        val emptyFile = tempFolder.newFile("empty.csv")
        val uri = Uri.fromFile(emptyFile)

        val result = orchestrator.parseAndValidateFromUri(app, uri)

        assertTrue(result is CsvImportParseResult.EmptyFile)
    }

    @Test
    fun testParseAndValidateFromUriValidCsv() = runBlocking {
        val tx = TransactionEntity(
            id = "tx_1",
            userId = "user_1",
            date = "2026-08-10",
            description = "Groceries",
            amountRON = 100.0,
            amountEUR = 20.0,
            exchangeRate = 5.0,
            exchangeRateDate = "2026-08-10",
            exchangeRateSource = "BNR_OFFICIAL",
            conversionStatus = "OFFICIAL",
            type = "Expense",
            account = "Card",
            category = "Food",
            subCategory = "Groceries"
        )
        val csvContent = CsvExporter.generateCsvContent(listOf(tx))

        val csvFile = tempFolder.newFile("valid.csv").apply {
            writeText(csvContent)
        }
        val uri = Uri.fromFile(csvFile)

        val result = orchestrator.parseAndValidateFromUri(app, uri)

        assertTrue(result is CsvImportParseResult.Success)
        val preview = (result as CsvImportParseResult.Success).preview
        assertEquals(1, preview.totalRows)
        assertEquals(1, preview.validRowsCount)
        assertEquals(1, preview.validTransactionsToImport.size)
        assertEquals("tx_1", preview.validTransactionsToImport[0].id)
    }

    @Test
    fun testUpdateDuplicateModeReEvaluatesPreview() = runBlocking {
        val existingTx = TransactionEntity(
            id = "tx_1",
            userId = "user_1",
            date = "2026-08-10",
            description = "Groceries",
            amountRON = 100.0,
            amountEUR = 20.0,
            exchangeRate = 5.0,
            exchangeRateDate = "2026-08-10",
            exchangeRateSource = "BNR_OFFICIAL",
            conversionStatus = "OFFICIAL",
            type = "Expense",
            account = "Card",
            category = "Food",
            subCategory = "Groceries"
        )
        fakeTransactionRepository.insertTransaction(existingTx)

        val csvContent = CsvExporter.generateCsvContent(listOf(existingTx))
        val csvFile = tempFolder.newFile("duplicate.csv").apply {
            writeText(csvContent)
        }
        val uri = Uri.fromFile(csvFile)

        val initialResult = orchestrator.parseAndValidateFromUri(app, uri) as CsvImportParseResult.Success
        assertEquals(CsvDuplicateMode.SKIP_EXISTING, initialResult.preview.duplicateMode)
        assertEquals(1, initialResult.preview.proposedSkipsCount)
        assertEquals(0, initialResult.preview.proposedUpdatesCount)

        val updatedPreview = orchestrator.updateDuplicateMode(initialResult.preview, CsvDuplicateMode.UPDATE_EXISTING)
        assertEquals(CsvDuplicateMode.UPDATE_EXISTING, updatedPreview.duplicateMode)
        assertEquals(0, updatedPreview.proposedSkipsCount)
        assertEquals(1, updatedPreview.proposedUpdatesCount)
    }

    @Test
    fun testExecuteImportDelegatesToRepository() = runBlocking {
        val preview = CsvPreviewData(
            totalRows = 1,
            validRowsCount = 1,
            invalidRowsCount = 0,
            newIdsCount = 1,
            existingIdsCount = 0,
            proposedUpdatesCount = 0,
            proposedSkipsCount = 0,
            totalRonIncome = 0.0,
            totalRonExpense = 100.0,
            officialCount = 1,
            unverifiedCount = 0,
            pendingCount = 0,
            missingCategories = emptyList(),
            rowErrors = emptyList(),
            validTransactionsToImport = emptyList(),
            duplicateMode = CsvDuplicateMode.SKIP_EXISTING,
            rawCsvContent = ""
        )

        val result = orchestrator.executeImport(preview, cacheDir)

        assertTrue(result.success)
    }
}
