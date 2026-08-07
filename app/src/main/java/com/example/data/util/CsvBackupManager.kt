package com.example.data.util

import com.example.data.model.TransactionEntity
import java.io.File

data class BackupValidationResult(
    val isValid: Boolean,
    val errorMessage: String? = null
)

object CsvBackupManager {

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
            BackupValidationResult(false, "Backup creation error: ${e.message}")
        }
    }
}
