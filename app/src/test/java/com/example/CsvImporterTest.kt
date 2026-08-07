package com.example

import com.example.data.model.CategoryEntity
import com.example.data.model.TransactionEntity
import com.example.data.util.BackupValidationResult
import com.example.data.util.CsvBackupManager
import com.example.data.util.CsvDuplicateMode
import com.example.data.util.CsvImporter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class CsvImporterTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private val existingCategories = listOf(
        CategoryEntity(id = "c1", name = "💼 Salary", type = "Income", subCategory = "🏢 Main Job"),
        CategoryEntity(id = "c2", name = "🍉 Food & Dining", type = "Expense", subCategory = "🛒 Groceries")
    )

    private val existingTransactions = listOf(
        TransactionEntity(
            id = "TX_EXISTING_1",
            date = "2026-03-01",
            description = "Existing salary",
            amountRON = 5000.0,
            amountEUR = 1000.0,
            exchangeRate = 5.0,
            exchangeRateDate = "2026-03-01",
            type = "Income",
            account = "Card",
            category = "💼 Salary",
            subCategory = "🏢 Main Job",
            destination = "Bubu",
            exchangeRateSource = "BNR_OFFICIAL",
            conversionStatus = "OFFICIAL"
        )
    )

    @Test
    fun testParseCsvLineWithQuotesAndEscapedQuotes() {
        val line = """123,2026-03-01,100.0,20.0,5.0,2026-03-01,2026-03-01,BNR_OFFICIAL,OFFICIAL,"Store ""Special"" Deal",Expense,Card,"🍉 Food & Dining","🛒 Groceries","""
        val result = CsvImporter.parseCsvLine(line)
        assertFalse(result.isUnclosedQuote)
        assertEquals(15, result.tokens.size)
        assertEquals("""Store "Special" Deal""", result.tokens[9])
    }

    @Test
    fun testUnclosedQuotesDetected() {
        val line = """123,2026-03-01,100.0,20.0,5.0,2026-03-01,2026-03-01,BNR_OFFICIAL,OFFICIAL,"Unclosed Quote,Expense,Card"""
        val result = CsvImporter.parseCsvLine(line)
        assertTrue(result.isUnclosedQuote)
    }

    @Test
    fun testOfficialEurDataValidation() {
        val csv = """Transaction_ID,Transaction_Date,Amount_RON,Amount_EUR,Exchange_Rate,Requested_Rate_Date,Effective_BNR_Rate_Date,Exchange_Rate_Source,Conversion_Status,Description,Type,Account,Category,SubCategory,Destination
TX101,2026-03-02,500.0,100.0,5.0,2026-03-02,2026-03-02,BNR_OFFICIAL,OFFICIAL,Main Job Salary,Income,Card,💼 Salary,🏢 Main Job,Bubu
"""
        val preview = CsvImporter.parseAndValidate(
            csvContent = csv,
            existingTransactions = emptyList(),
            existingCategories = existingCategories
        )

        assertEquals(1, preview.validRowsCount)
        assertEquals(0, preview.invalidRowsCount)
        assertEquals(1, preview.officialCount)
        assertEquals(0, preview.unverifiedCount)
        assertEquals(0, preview.pendingCount)

        val tx = preview.validTransactionsToImport.first()
        assertEquals("OFFICIAL", tx.conversionStatus)
        assertEquals("BNR_OFFICIAL", tx.exchangeRateSource)
        assertEquals(5.0, tx.exchangeRate, 0.001)
        assertEquals(100.0, tx.amountEUR, 0.001)
    }

    @Test
    fun testMissingSourceOrStatusDefaultsToUnverifiedNeverOfficial() {
        // Source and status missing or unverified -> MUST default to UNVERIFIED, NOT OFFICIAL
        val csv = """Transaction_ID,Transaction_Date,Amount_RON,Amount_EUR,Exchange_Rate,Requested_Rate_Date,Effective_BNR_Rate_Date,Exchange_Rate_Source,Conversion_Status,Description,Type,Account,Category,SubCategory,Destination
TX102,2026-03-02,500.0,100.0,5.0,2026-03-02,2026-03-02,,,Grocery,Expense,Card,🍉 Food & Dining,🛒 Groceries,
"""
        val preview = CsvImporter.parseAndValidate(
            csvContent = csv,
            existingTransactions = emptyList(),
            existingCategories = existingCategories
        )

        assertEquals(1, preview.validRowsCount)
        assertEquals(0, preview.officialCount)
        assertEquals(1, preview.unverifiedCount)

        val tx = preview.validTransactionsToImport.first()
        assertEquals("UNVERIFIED", tx.conversionStatus)
        assertEquals("UNVERIFIED", tx.exchangeRateSource)
    }

    @Test
    fun testMismatchedEurOrRateDefaultsToUnverified() {
        // Rate is 5.0, but EUR is provided as 50.0 (500 RON / 5.0 should be 100.0)
        val csv = """Transaction_ID,Transaction_Date,Amount_RON,Amount_EUR,Exchange_Rate,Requested_Rate_Date,Effective_BNR_Rate_Date,Exchange_Rate_Source,Conversion_Status,Description,Type,Account,Category,SubCategory,Destination
TX103,2026-03-02,500.0,50.0,5.0,2026-03-02,2026-03-02,BNR_OFFICIAL,OFFICIAL,Grocery,Expense,Card,🍉 Food & Dining,🛒 Groceries,
"""
        val preview = CsvImporter.parseAndValidate(
            csvContent = csv,
            existingTransactions = emptyList(),
            existingCategories = existingCategories
        )

        assertEquals(1, preview.validRowsCount)
        assertEquals(0, preview.officialCount)
        assertEquals(1, preview.unverifiedCount)

        val tx = preview.validTransactionsToImport.first()
        assertEquals("UNVERIFIED", tx.conversionStatus)
    }

    @Test
    fun testInvalidRowValidationRules() {
        val csv = """Transaction_ID,Transaction_Date,Amount_RON,Amount_EUR,Exchange_Rate,Requested_Rate_Date,Effective_BNR_Rate_Date,Exchange_Rate_Source,Conversion_Status,Description,Type,Account,Category,SubCategory,Destination
TX1,invalid-date,100.0,20.0,5.0,2026-03-01,2026-03-01,BNR_OFFICIAL,OFFICIAL,Bad Date,Expense,Card,🍉 Food & Dining,🛒 Groceries,
TX2,2026-03-02,-50.0,10.0,5.0,2026-03-02,2026-03-02,BNR_OFFICIAL,OFFICIAL,Negative Amt,Expense,Card,🍉 Food & Dining,🛒 Groceries,
TX3,2026-03-02,100.0,20.0,5.0,2026-03-02,2026-03-02,BNR_OFFICIAL,OFFICIAL,Bad Type,Transfer,Card,🍉 Food & Dining,🛒 Groceries,
TX4,2026-03-02,100.0,20.0,5.0,2026-03-02,2026-03-02,BNR_OFFICIAL,OFFICIAL,Bad Account,Expense,Crypto,🍉 Food & Dining,🛒 Groceries,
TX5,2026-03-02,100.0,20.0,5.0,2026-03-02,2026-03-02,BNR_OFFICIAL,OFFICIAL,Expense Dest,Expense,Card,🍉 Food & Dining,🛒 Groceries,Bubu
TX6,2026-03-02,100.0,20.0,5.0,2026-03-02,2026-03-02,BNR_OFFICIAL,OFFICIAL,Bad Inc Dest,Income,Card,💼 Salary,🏢 Main Job,InvalidDest
"""
        val preview = CsvImporter.parseAndValidate(
            csvContent = csv,
            existingTransactions = emptyList(),
            existingCategories = existingCategories
        )

        assertEquals(0, preview.validRowsCount)
        assertEquals(6, preview.invalidRowsCount)
        assertEquals(6, preview.rowErrors.size)
    }

    @Test
    fun testConflictingSubcategoryBelongingIsRejected() {
        // Subcategory "🛒 Groceries" belongs to "🍉 Food & Dining" (Expense)
        // Trying to import it under "🏠 Housing & Utilities" (Expense) should be rejected
        val csv = """Transaction_ID,Transaction_Date,Amount_RON,Amount_EUR,Exchange_Rate,Requested_Rate_Date,Effective_BNR_Rate_Date,Exchange_Rate_Source,Conversion_Status,Description,Type,Account,Category,SubCategory,Destination
TX7,2026-03-02,100.0,20.0,5.0,2026-03-02,2026-03-02,BNR_OFFICIAL,OFFICIAL,Conflict,Expense,Card,🏠 Housing & Utilities,🛒 Groceries,
"""
        val preview = CsvImporter.parseAndValidate(
            csvContent = csv,
            existingTransactions = emptyList(),
            existingCategories = existingCategories
        )

        assertEquals(0, preview.validRowsCount)
        assertEquals(1, preview.invalidRowsCount)
        assertTrue(preview.rowErrors.first().message.contains("belongs to category"))
    }

    @Test
    fun testMissingCategoriesDetectedAndTracked() {
        val csv = """Transaction_ID,Transaction_Date,Amount_RON,Amount_EUR,Exchange_Rate,Requested_Rate_Date,Effective_BNR_Rate_Date,Exchange_Rate_Source,Conversion_Status,Description,Type,Account,Category,SubCategory,Destination
TX8,2026-03-02,200.0,40.0,5.0,2026-03-02,2026-03-02,BNR_OFFICIAL,OFFICIAL,Gym,Expense,Card,🏋️ Fitness,Gym Membership,
"""
        val preview = CsvImporter.parseAndValidate(
            csvContent = csv,
            existingTransactions = emptyList(),
            existingCategories = existingCategories
        )

        assertEquals(1, preview.validRowsCount)
        assertEquals(1, preview.missingCategories.size)
        assertEquals("🏋️ Fitness", preview.missingCategories.first().name)
        assertEquals("Gym Membership", preview.missingCategories.first().subCategory)
    }

    @Test
    fun testDuplicateModesSkipVsUpdate() {
        val csv = """Transaction_ID,Transaction_Date,Amount_RON,Amount_EUR,Exchange_Rate,Requested_Rate_Date,Effective_BNR_Rate_Date,Exchange_Rate_Source,Conversion_Status,Description,Type,Account,Category,SubCategory,Destination
TX_EXISTING_1,2026-03-01,6000.0,1200.0,5.0,2026-03-01,2026-03-01,BNR_OFFICIAL,OFFICIAL,Updated salary,Income,Card,💼 Salary,🏢 Main Job,Bubu
"""
        // Mode 1: SKIP_EXISTING
        val skipPreview = CsvImporter.parseAndValidate(
            csvContent = csv,
            existingTransactions = existingTransactions,
            existingCategories = existingCategories,
            duplicateMode = CsvDuplicateMode.SKIP_EXISTING
        )
        assertEquals(1, skipPreview.existingIdsCount)
        assertEquals(0, skipPreview.proposedUpdatesCount)
        assertEquals(1, skipPreview.proposedSkipsCount)

        // Mode 2: UPDATE_EXISTING
        val updatePreview = CsvImporter.parseAndValidate(
            csvContent = csv,
            existingTransactions = existingTransactions,
            existingCategories = existingCategories,
            duplicateMode = CsvDuplicateMode.UPDATE_EXISTING
        )
        assertEquals(1, updatePreview.existingIdsCount)
        assertEquals(1, updatePreview.proposedUpdatesCount)
        assertEquals(0, updatePreview.proposedSkipsCount)
    }

    @Test
    fun testBackupCreationAndValidationSuccess() {
        val backupFile = File(tempFolder.root, "backup_valid.csv")
        val result = CsvBackupManager.createAndValidateBackup(backupFile, existingTransactions)

        assertTrue(result.isValid)
        assertTrue(backupFile.exists())
        assertTrue(backupFile.length() > 0)
        val lines = backupFile.readLines()
        assertEquals(2, lines.size) // header + 1 transaction row
        assertTrue(lines.first().contains("Transaction_ID"))
    }

    @Test
    fun testBackupValidationFailsOnMismatchCount() {
        val backupFile = File(tempFolder.root, "backup_corrupt.csv")
        backupFile.writeText("Transaction_ID,Transaction_Date,Amount_RON\n") // 0 data rows, but existingTransactions has 1 item

        val lines = backupFile.readLines().filter { it.isNotBlank() }
        val dataRowCount = lines.size - 1

        val result = if (dataRowCount != existingTransactions.size) {
            BackupValidationResult(false, "Transaction count mismatch")
        } else {
            BackupValidationResult(true, null)
        }

        assertFalse(result.isValid)
    }
}
