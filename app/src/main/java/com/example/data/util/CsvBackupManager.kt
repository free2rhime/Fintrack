package com.example.data.util

import com.example.data.model.CategoryEntity
import com.example.data.model.ExchangeRateEntity
import com.example.data.model.TransactionEntity
import java.io.File

data class BackupValidationResult(
    val isValid: Boolean,
    val errorMessage: String? = null
)

data class MigrationManifest(
    val backupVersion: Int = 1,
    val creationTimestamp: Long = System.currentTimeMillis(),
    val databaseVersion: Int = 5,
    val transactionCount: Int,
    val categoryCount: Int,
    val exchangeRateCount: Int
) {
    fun toJson(): String {
        return """
            {
              "backupVersion": $backupVersion,
              "creationTimestamp": $creationTimestamp,
              "databaseVersion": $databaseVersion,
              "transactionCount": $transactionCount,
              "categoryCount": $categoryCount,
              "exchangeRateCount": $exchangeRateCount
            }
        """.trimIndent()
    }

    companion object {
        fun fromJson(jsonStr: String): MigrationManifest? {
            return try {
                val trimmed = jsonStr.trim()
                if (!trimmed.startsWith("{") || !trimmed.endsWith("}")) return null

                fun extractLong(key: String): Long? {
                    val pattern = Regex("\"$key\"\\s*:\\s*(\\d+)")
                    val match = pattern.find(trimmed) ?: return null
                    return match.groupValues[1].toLongOrNull()
                }

                fun extractInt(key: String): Int? {
                    return extractLong(key)?.toInt()
                }

                val backupVersion = extractInt("backupVersion") ?: return null
                val creationTimestamp = extractLong("creationTimestamp") ?: return null
                val databaseVersion = extractInt("databaseVersion") ?: return null
                val transactionCount = extractInt("transactionCount") ?: return null
                val categoryCount = extractInt("categoryCount") ?: return null
                val exchangeRateCount = extractInt("exchangeRateCount") ?: return null

                if (backupVersion <= 0 || creationTimestamp <= 0 || databaseVersion <= 0 ||
                    transactionCount < 0 || categoryCount < 0 || exchangeRateCount < 0) {
                    return null
                }

                MigrationManifest(
                    backupVersion = backupVersion,
                    creationTimestamp = creationTimestamp,
                    databaseVersion = databaseVersion,
                    transactionCount = transactionCount,
                    categoryCount = categoryCount,
                    exchangeRateCount = exchangeRateCount
                )
            } catch (e: Exception) {
                null
            }
        }
    }
}

data class MigrationBackupBundleResult(
    val isValid: Boolean,
    val bundleDir: File? = null,
    val manifest: MigrationManifest? = null,
    val errorMessage: String? = null
)

object CsvBackupManager {

    const val MANIFEST_FILE_NAME = "manifest.json"
    const val TRANSACTIONS_FILE_NAME = "transactions.csv"
    const val CATEGORIES_FILE_NAME = "categories.csv"
    const val EXCHANGE_RATES_FILE_NAME = "exchange_rates.csv"

    fun createAndValidateBackup(
        backupFile: File,
        existingTransactions: List<TransactionEntity>
    ): BackupValidationResult {
        return try {
            if (backupFile.exists()) {
                backupFile.delete()
            }

            // Generate backup CSV file
            CsvExporter.writeTransactionsToFile(backupFile, existingTransactions)

            // 1. Validate file exists
            if (!backupFile.exists()) {
                return BackupValidationResult(false, "Backup file was not created on disk")
            }

            // 2. Validate file readable
            if (!backupFile.canRead()) {
                return BackupValidationResult(false, "Backup file is not readable")
            }

            // 3. Validate non-empty
            if (backupFile.length() <= 0L) {
                return BackupValidationResult(false, "Backup file is 0 bytes")
            }

            // 4. Validate header and transaction count
            val lines = backupFile.readLines().filter { it.isNotBlank() }
            if (lines.isEmpty()) {
                return BackupValidationResult(false, "Backup file contains no lines")
            }

            val header = lines.first()
            if (!header.contains("Transaction_ID") || !header.contains("Transaction_Date") || !header.contains("Amount_RON")) {
                return BackupValidationResult(false, "Backup file header is invalid: $header")
            }

            val dataRowCount = lines.size - 1
            if (dataRowCount != existingTransactions.size) {
                return BackupValidationResult(
                    false,
                    "Backup transaction count ($dataRowCount) does not match expected database count (${existingTransactions.size})"
                )
            }

            BackupValidationResult(true, null)
        } catch (e: Exception) {
            val sanitized = sanitizeError(e.message)
            BackupValidationResult(false, "Backup creation error: $sanitized")
        }
    }

    fun createMigrationBackupBundle(
        bundleDir: File,
        transactions: List<TransactionEntity>,
        categories: List<CategoryEntity>,
        exchangeRates: List<ExchangeRateEntity>,
        databaseVersion: Int = 5,
        creationTimestamp: Long = System.currentTimeMillis(),
        backupVersion: Int = 1
    ): MigrationBackupBundleResult {
        return try {
            if (bundleDir.exists() && !bundleDir.isDirectory) {
                return MigrationBackupBundleResult(
                    isValid = false,
                    errorMessage = "Target backup bundle path is not a directory"
                )
            }

            if (!bundleDir.exists()) {
                val created = bundleDir.mkdirs()
                if (!created && !bundleDir.exists()) {
                    return MigrationBackupBundleResult(
                        isValid = false,
                        errorMessage = "Failed to create backup bundle directory"
                    )
                }
            }

            val txFile = File(bundleDir, TRANSACTIONS_FILE_NAME)
            val catFile = File(bundleDir, CATEGORIES_FILE_NAME)
            val rateFile = File(bundleDir, EXCHANGE_RATES_FILE_NAME)
            val manifestFile = File(bundleDir, MANIFEST_FILE_NAME)

            CsvExporter.writeTransactionsToFile(txFile, transactions)
            CsvExporter.writeCategoriesToFile(catFile, categories)
            CsvExporter.writeExchangeRatesToFile(rateFile, exchangeRates)

            val manifest = MigrationManifest(
                backupVersion = backupVersion,
                creationTimestamp = creationTimestamp,
                databaseVersion = databaseVersion,
                transactionCount = transactions.size,
                categoryCount = categories.size,
                exchangeRateCount = exchangeRates.size
            )
            manifestFile.writeText(manifest.toJson())

            validateMigrationBackupBundle(bundleDir)
        } catch (e: Exception) {
            val sanitized = sanitizeError(e.message)
            MigrationBackupBundleResult(
                isValid = false,
                errorMessage = "Failed to create backup bundle: $sanitized"
            )
        }
    }

    fun validateMigrationBackupBundle(bundleDir: File): MigrationBackupBundleResult {
        return try {
            if (!bundleDir.exists()) {
                return MigrationBackupBundleResult(false, errorMessage = "Backup bundle directory does not exist")
            }
            if (!bundleDir.isDirectory) {
                return MigrationBackupBundleResult(false, errorMessage = "Backup bundle path is not a directory")
            }
            if (!bundleDir.canRead()) {
                return MigrationBackupBundleResult(false, errorMessage = "Backup bundle directory is unreadable")
            }

            val files = bundleDir.listFiles()
            if (files == null || files.isEmpty()) {
                return MigrationBackupBundleResult(false, errorMessage = "Backup bundle is empty")
            }

            // 1. Validate manifest.json
            val manifestFile = File(bundleDir, MANIFEST_FILE_NAME)
            if (!manifestFile.exists()) {
                return MigrationBackupBundleResult(
                    false,
                    errorMessage = "Manifest metadata file '$MANIFEST_FILE_NAME' is missing from backup bundle"
                )
            }
            if (!manifestFile.canRead() || manifestFile.length() <= 0L) {
                return MigrationBackupBundleResult(
                    false,
                    errorMessage = "Manifest metadata file '$MANIFEST_FILE_NAME' is unreadable or empty"
                )
            }

            val manifestContent = manifestFile.readText()
            val manifest = MigrationManifest.fromJson(manifestContent)
                ?: return MigrationBackupBundleResult(
                    false,
                    errorMessage = "Manifest metadata is corrupted or invalid"
                )

            // 2. Validate transactions.csv
            val txFile = File(bundleDir, TRANSACTIONS_FILE_NAME)
            if (!txFile.exists()) {
                return MigrationBackupBundleResult(
                    false,
                    errorMessage = "Required backup file '$TRANSACTIONS_FILE_NAME' is missing"
                )
            }
            if (!txFile.canRead() || txFile.length() <= 0L) {
                return MigrationBackupBundleResult(
                    false,
                    errorMessage = "Backup file '$TRANSACTIONS_FILE_NAME' is unreadable or empty"
                )
            }
            val txLines = txFile.readLines().filter { it.isNotBlank() }
            if (txLines.isEmpty()) {
                return MigrationBackupBundleResult(
                    false,
                    errorMessage = "Backup file '$TRANSACTIONS_FILE_NAME' contains no rows"
                )
            }
            val txHeader = txLines.first()
            if (!txHeader.contains("Transaction_ID") || !txHeader.contains("Transaction_Date") || !txHeader.contains("Amount_RON")) {
                return MigrationBackupBundleResult(
                    false,
                    errorMessage = "Invalid header in '$TRANSACTIONS_FILE_NAME': $txHeader"
                )
            }
            val txDataRowCount = txLines.size - 1
            if (txDataRowCount != manifest.transactionCount) {
                return MigrationBackupBundleResult(
                    false,
                    errorMessage = "Transaction count in CSV ($txDataRowCount) does not match manifest count (${manifest.transactionCount})"
                )
            }

            // 3. Validate categories.csv
            val catFile = File(bundleDir, CATEGORIES_FILE_NAME)
            if (!catFile.exists()) {
                return MigrationBackupBundleResult(
                    false,
                    errorMessage = "Required backup file '$CATEGORIES_FILE_NAME' is missing"
                )
            }
            if (!catFile.canRead() || catFile.length() <= 0L) {
                return MigrationBackupBundleResult(
                    false,
                    errorMessage = "Backup file '$CATEGORIES_FILE_NAME' is unreadable or empty"
                )
            }
            val catLines = catFile.readLines().filter { it.isNotBlank() }
            if (catLines.isEmpty()) {
                return MigrationBackupBundleResult(
                    false,
                    errorMessage = "Backup file '$CATEGORIES_FILE_NAME' contains no rows"
                )
            }
            val catHeader = catLines.first()
            if (!catHeader.contains("Category_ID") || !catHeader.contains("Name") || !catHeader.contains("Type")) {
                return MigrationBackupBundleResult(
                    false,
                    errorMessage = "Invalid header in '$CATEGORIES_FILE_NAME': $catHeader"
                )
            }
            val catDataRowCount = catLines.size - 1
            if (catDataRowCount != manifest.categoryCount) {
                return MigrationBackupBundleResult(
                    false,
                    errorMessage = "Category count in CSV ($catDataRowCount) does not match manifest count (${manifest.categoryCount})"
                )
            }

            // 4. Validate exchange_rates.csv
            val rateFile = File(bundleDir, EXCHANGE_RATES_FILE_NAME)
            if (!rateFile.exists()) {
                return MigrationBackupBundleResult(
                    false,
                    errorMessage = "Required backup file '$EXCHANGE_RATES_FILE_NAME' is missing"
                )
            }
            if (!rateFile.canRead() || rateFile.length() <= 0L) {
                return MigrationBackupBundleResult(
                    false,
                    errorMessage = "Backup file '$EXCHANGE_RATES_FILE_NAME' is unreadable or empty"
                )
            }
            val rateLines = rateFile.readLines().filter { it.isNotBlank() }
            if (rateLines.isEmpty()) {
                return MigrationBackupBundleResult(
                    false,
                    errorMessage = "Backup file '$EXCHANGE_RATES_FILE_NAME' contains no rows"
                )
            }
            val rateHeader = rateLines.first()
            if (!rateHeader.contains("Rate_Date") || !rateHeader.contains("Rate") || !rateHeader.contains("Source")) {
                return MigrationBackupBundleResult(
                    false,
                    errorMessage = "Invalid header in '$EXCHANGE_RATES_FILE_NAME': $rateHeader"
                )
            }
            val rateDataRowCount = rateLines.size - 1
            if (rateDataRowCount != manifest.exchangeRateCount) {
                return MigrationBackupBundleResult(
                    false,
                    errorMessage = "Exchange rate count in CSV ($rateDataRowCount) does not match manifest count (${manifest.exchangeRateCount})"
                )
            }

            MigrationBackupBundleResult(
                isValid = true,
                bundleDir = bundleDir,
                manifest = manifest,
                errorMessage = null
            )
        } catch (e: Exception) {
            val sanitized = sanitizeError(e.message)
            MigrationBackupBundleResult(
                isValid = false,
                errorMessage = "Backup validation failed: $sanitized"
            )
        }
    }

    private fun sanitizeError(rawMessage: String?): String {
        if (rawMessage.isNullOrBlank()) return "An unexpected error occurred during backup operation."
        val firstLine = rawMessage.lines().firstOrNull()?.trim() ?: "An error occurred"
        return firstLine
            .replace(Regex("at [a-zA-Z0-9_$.]+\\(.*\\)"), "")
            .take(120)
            .ifBlank { "Operation failed" }
    }
}
