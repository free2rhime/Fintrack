package com.example

import com.example.data.model.CategoryEntity
import com.example.data.model.ExchangeRateEntity
import com.example.data.model.TransactionEntity
import com.example.data.util.CsvBackupManager
import com.example.data.util.CsvExporter
import com.example.data.util.MigrationManifest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class Stage1CBackupBundleTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private val sampleTransactions = listOf(
        TransactionEntity(
            id = "tx-1",
            date = "2026-03-01",
            description = "Groceries Market",
            amountRON = 250.0,
            amountEUR = 50.0,
            exchangeRate = 5.0,
            exchangeRateDate = "2026-03-01",
            type = "Expense",
            account = "Card",
            category = "Food & Dining",
            subCategory = "Groceries",
            destination = null,
            exchangeRateSource = "BNR_OFFICIAL",
            conversionStatus = "OFFICIAL"
        ),
        TransactionEntity(
            id = "tx-2",
            date = "2026-03-02",
            description = "Monthly Salary",
            amountRON = 5000.0,
            amountEUR = 1000.0,
            exchangeRate = 5.0,
            exchangeRateDate = "2026-03-02",
            type = "Income",
            account = "Card",
            category = "Salary",
            subCategory = "Primary Job",
            destination = "Bubu",
            exchangeRateSource = "BNR_OFFICIAL",
            conversionStatus = "OFFICIAL"
        )
    )

    private val sampleCategories = listOf(
        CategoryEntity(
            id = "cat-1",
            name = "Food & Dining",
            type = "Expense",
            subCategory = "Groceries",
            userId = "user-123"
        ),
        CategoryEntity(
            id = "cat-2",
            name = "Salary",
            type = "Income",
            subCategory = "Primary Job",
            userId = "user-123"
        ),
        CategoryEntity(
            id = "cat-3",
            name = "Utilities",
            type = "Expense",
            subCategory = "Electricity",
            userId = "user-123"
        )
    )

    private val sampleExchangeRates = listOf(
        ExchangeRateEntity(
            date = "2026-03-01",
            requestedDate = "2026-03-01",
            effectiveDate = "2026-03-01",
            rate = 5.0,
            source = "BNR_OFFICIAL",
            status = "OFFICIAL"
        ),
        ExchangeRateEntity(
            date = "2026-03-02",
            requestedDate = "2026-03-02",
            effectiveDate = "2026-03-02",
            rate = 4.995,
            source = "BNR_OFFICIAL",
            status = "OFFICIAL"
        )
    )

    @Test
    fun testValidBundlePassesValidation() {
        val bundleDir = File(tempFolder.root, "valid_bundle")
        val result = CsvBackupManager.createMigrationBackupBundle(
            bundleDir = bundleDir,
            transactions = sampleTransactions,
            categories = sampleCategories,
            exchangeRates = sampleExchangeRates,
            databaseVersion = 5,
            creationTimestamp = 1770969600000L,
            backupVersion = 1
        )

        assertTrue("Expected valid bundle creation to succeed, got error: ${result.errorMessage}", result.isValid)
        assertNotNull(result.manifest)
        assertEquals(1, result.manifest?.backupVersion)
        assertEquals(1770969600000L, result.manifest?.creationTimestamp)
        assertEquals(5, result.manifest?.databaseVersion)
        assertEquals(2, result.manifest?.transactionCount)
        assertEquals(3, result.manifest?.categoryCount)
        assertEquals(2, result.manifest?.exchangeRateCount)

        val valResult = CsvBackupManager.validateMigrationBackupBundle(bundleDir)
        assertTrue("Expected bundle validation to succeed: ${valResult.errorMessage}", valResult.isValid)
        assertEquals(null, valResult.errorMessage)
    }

    @Test
    fun testMissingTransactionsCsvFails() {
        val bundleDir = File(tempFolder.root, "bundle_missing_tx")
        val createResult = CsvBackupManager.createMigrationBackupBundle(
            bundleDir = bundleDir,
            transactions = sampleTransactions,
            categories = sampleCategories,
            exchangeRates = sampleExchangeRates
        )
        assertTrue(createResult.isValid)

        // Delete transactions.csv
        val txFile = File(bundleDir, CsvBackupManager.TRANSACTIONS_FILE_NAME)
        assertTrue(txFile.delete())

        val valResult = CsvBackupManager.validateMigrationBackupBundle(bundleDir)
        assertFalse("Expected validation to fail when transactions.csv is missing", valResult.isValid)
        assertNotNull(valResult.errorMessage)
        assertTrue(valResult.errorMessage!!.contains("transactions.csv"))
    }

    @Test
    fun testMissingCategoriesCsvFails() {
        val bundleDir = File(tempFolder.root, "bundle_missing_cat")
        val createResult = CsvBackupManager.createMigrationBackupBundle(
            bundleDir = bundleDir,
            transactions = sampleTransactions,
            categories = sampleCategories,
            exchangeRates = sampleExchangeRates
        )
        assertTrue(createResult.isValid)

        // Delete categories.csv
        val catFile = File(bundleDir, CsvBackupManager.CATEGORIES_FILE_NAME)
        assertTrue(catFile.delete())

        val valResult = CsvBackupManager.validateMigrationBackupBundle(bundleDir)
        assertFalse("Expected validation to fail when categories.csv is missing", valResult.isValid)
        assertNotNull(valResult.errorMessage)
        assertTrue(valResult.errorMessage!!.contains("categories.csv"))
    }

    @Test
    fun testMissingExchangeRatesCsvFails() {
        val bundleDir = File(tempFolder.root, "bundle_missing_rates")
        val createResult = CsvBackupManager.createMigrationBackupBundle(
            bundleDir = bundleDir,
            transactions = sampleTransactions,
            categories = sampleCategories,
            exchangeRates = sampleExchangeRates
        )
        assertTrue(createResult.isValid)

        // Delete exchange_rates.csv
        val rateFile = File(bundleDir, CsvBackupManager.EXCHANGE_RATES_FILE_NAME)
        assertTrue(rateFile.delete())

        val valResult = CsvBackupManager.validateMigrationBackupBundle(bundleDir)
        assertFalse("Expected validation to fail when exchange_rates.csv is missing", valResult.isValid)
        assertNotNull(valResult.errorMessage)
        assertTrue(valResult.errorMessage!!.contains("exchange_rates.csv"))
    }

    @Test
    fun testInvalidManifestFails() {
        val bundleDir = File(tempFolder.root, "bundle_invalid_manifest")
        val createResult = CsvBackupManager.createMigrationBackupBundle(
            bundleDir = bundleDir,
            transactions = sampleTransactions,
            categories = sampleCategories,
            exchangeRates = sampleExchangeRates
        )
        assertTrue(createResult.isValid)

        // Corrupt manifest.json
        val manifestFile = File(bundleDir, CsvBackupManager.MANIFEST_FILE_NAME)
        manifestFile.writeText("{ corrupted_json: true, missing_fields: [] }")

        val valResult = CsvBackupManager.validateMigrationBackupBundle(bundleDir)
        assertFalse("Expected validation to fail on corrupted manifest", valResult.isValid)
        assertNotNull(valResult.errorMessage)
        assertTrue(valResult.errorMessage!!.contains("Manifest metadata is corrupted or invalid"))
    }

    @Test
    fun testRowCountMismatchFails() {
        val bundleDir = File(tempFolder.root, "bundle_count_mismatch")
        val createResult = CsvBackupManager.createMigrationBackupBundle(
            bundleDir = bundleDir,
            transactions = sampleTransactions,
            categories = sampleCategories,
            exchangeRates = sampleExchangeRates
        )
        assertTrue(createResult.isValid)

        // Tamper with transactions.csv to add extra line
        val txFile = File(bundleDir, CsvBackupManager.TRANSACTIONS_FILE_NAME)
        val originalContent = txFile.readText()
        txFile.writeText(originalContent + "tx-3,2026-03-03,10.0,2.0,5.0,2026-03-03,2026-03-03,BNR_OFFICIAL,OFFICIAL,Extra,Expense,Card,Food,Snacks,\n")

        val valResult = CsvBackupManager.validateMigrationBackupBundle(bundleDir)
        assertFalse("Expected validation to fail on row count mismatch", valResult.isValid)
        assertNotNull(valResult.errorMessage)
        assertTrue(valResult.errorMessage!!.contains("Transaction count in CSV (3) does not match manifest count (2)"))
    }

    @Test
    fun testCategoryRowCountMismatchFails() {
        val bundleDir = File(tempFolder.root, "bundle_cat_mismatch")
        val createResult = CsvBackupManager.createMigrationBackupBundle(
            bundleDir = bundleDir,
            transactions = sampleTransactions,
            categories = sampleCategories,
            exchangeRates = sampleExchangeRates
        )
        assertTrue(createResult.isValid)

        // Manifest states 3 categories; rewrite categories.csv with 1 row
        val catFile = File(bundleDir, CsvBackupManager.CATEGORIES_FILE_NAME)
        CsvExporter.writeCategoriesToFile(catFile, sampleCategories.take(1))

        val valResult = CsvBackupManager.validateMigrationBackupBundle(bundleDir)
        assertFalse("Expected validation to fail on category count mismatch", valResult.isValid)
        assertNotNull(valResult.errorMessage)
        assertTrue(valResult.errorMessage!!.contains("Category count in CSV (1) does not match manifest count (3)"))
    }

    @Test
    fun testExchangeRateRowCountMismatchFails() {
        val bundleDir = File(tempFolder.root, "bundle_rates_mismatch")
        val createResult = CsvBackupManager.createMigrationBackupBundle(
            bundleDir = bundleDir,
            transactions = sampleTransactions,
            categories = sampleCategories,
            exchangeRates = sampleExchangeRates
        )
        assertTrue(createResult.isValid)

        // Manifest states 2 exchange rates; rewrite exchange_rates.csv with 1 row
        val rateFile = File(bundleDir, CsvBackupManager.EXCHANGE_RATES_FILE_NAME)
        CsvExporter.writeExchangeRatesToFile(rateFile, sampleExchangeRates.take(1))

        val valResult = CsvBackupManager.validateMigrationBackupBundle(bundleDir)
        assertFalse("Expected validation to fail on exchange rate count mismatch", valResult.isValid)
        assertNotNull(valResult.errorMessage)
        assertTrue(valResult.errorMessage!!.contains("Exchange rate count in CSV (1) does not match manifest count (2)"))
    }

    @Test
    fun testEmptyBundleFails() {
        val emptyDir = File(tempFolder.root, "empty_bundle")
        assertTrue(emptyDir.mkdirs())

        val valResult = CsvBackupManager.validateMigrationBackupBundle(emptyDir)
        assertFalse("Expected validation to fail on empty bundle directory", valResult.isValid)
        assertNotNull(valResult.errorMessage)
        assertTrue(valResult.errorMessage!!.contains("Backup bundle is empty"))
    }

    @Test
    fun testInvalidHeaderFails() {
        val bundleDir = File(tempFolder.root, "bundle_invalid_header")
        val createResult = CsvBackupManager.createMigrationBackupBundle(
            bundleDir = bundleDir,
            transactions = sampleTransactions,
            categories = sampleCategories,
            exchangeRates = sampleExchangeRates
        )
        assertTrue(createResult.isValid)

        val txFile = File(bundleDir, CsvBackupManager.TRANSACTIONS_FILE_NAME)
        txFile.writeText("Bad_Header_1,Bad_Header_2\n")

        val valResult = CsvBackupManager.validateMigrationBackupBundle(bundleDir)
        assertFalse("Expected validation to fail on invalid header", valResult.isValid)
        assertNotNull(valResult.errorMessage)
        assertTrue(valResult.errorMessage!!.contains("Invalid header in 'transactions.csv'"))
    }

    @Test
    fun testSanitizedErrorsContainNoStackTraces() {
        val nonExistentDir = File(tempFolder.root, "non_existent_folder_abc")
        val valResult = CsvBackupManager.validateMigrationBackupBundle(nonExistentDir)
        assertFalse(valResult.isValid)
        assertNotNull(valResult.errorMessage)
        assertFalse("Error must not contain Exception class names", valResult.errorMessage!!.contains("java.io."))
        assertFalse("Error must not contain Exception class names", valResult.errorMessage!!.contains("FileNotFoundException"))
        assertFalse("Error must not contain stack traces", valResult.errorMessage!!.contains("at com.example"))

        // Also test corrupted manifest
        val brokenManifestDir = File(tempFolder.root, "broken_manifest_dir")
        brokenManifestDir.mkdirs()
        File(brokenManifestDir, CsvBackupManager.MANIFEST_FILE_NAME).writeText("{ broken")
        val manifestValResult = CsvBackupManager.validateMigrationBackupBundle(brokenManifestDir)
        assertFalse(manifestValResult.isValid)
        assertFalse("Error must not contain stack traces", manifestValResult.errorMessage!!.contains("Exception"))
        assertFalse("Error must not contain stack traces", manifestValResult.errorMessage!!.contains("at com.example"))
    }

    @Test
    fun testExistingCsvExportFunctionalityRemainsCompatible() {
        val exportFile = File(tempFolder.root, "test_export.csv")
        CsvExporter.writeTransactionsToFile(exportFile, sampleTransactions)

        assertTrue(exportFile.exists())
        val content = exportFile.readText()
        assertTrue(content.startsWith("Transaction_ID,Transaction_Date,Amount_RON"))
        assertTrue(content.contains("Groceries Market"))
        assertTrue(content.contains("Monthly Salary"))

        // Existing single-file backup validation
        val backupValidation = CsvBackupManager.createAndValidateBackup(exportFile, sampleTransactions)
        assertTrue(backupValidation.isValid)
        assertEquals(null, backupValidation.errorMessage)
    }

    @Test
    fun testEmptyDatabaseBackupBundleIsValid() {
        val emptyDbBundleDir = File(tempFolder.root, "empty_db_bundle")
        val result = CsvBackupManager.createMigrationBackupBundle(
            bundleDir = emptyDbBundleDir,
            transactions = emptyList(),
            categories = emptyList(),
            exchangeRates = emptyList(),
            databaseVersion = 5
        )

        assertTrue("Empty database backup bundle should be valid", result.isValid)
        assertNotNull(result.manifest)
        assertEquals(0, result.manifest?.transactionCount)
        assertEquals(0, result.manifest?.categoryCount)
        assertEquals(0, result.manifest?.exchangeRateCount)

        val valResult = CsvBackupManager.validateMigrationBackupBundle(emptyDbBundleDir)
        assertTrue("Validation on empty database backup bundle must succeed", valResult.isValid)
    }
}
