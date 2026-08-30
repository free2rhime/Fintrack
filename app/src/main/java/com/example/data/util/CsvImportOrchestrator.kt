package com.example.data.util

import android.content.Context
import android.net.Uri
import com.example.data.repository.CategoryRepository
import com.example.data.repository.TransactionRepository
import java.io.File

sealed interface CsvImportParseResult {
    data class Success(val preview: CsvPreviewData) : CsvImportParseResult
    data class EmptyFile(val message: String = "CSV file is empty or could not be read.") : CsvImportParseResult
    data class Failure(val message: String) : CsvImportParseResult
}

class CsvImportOrchestrator(
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository
) {
    suspend fun parseAndValidateFromUri(context: Context, uri: Uri): CsvImportParseResult {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
            val csvContent = inputStream?.bufferedReader()?.use { it.readText() } ?: ""
            if (csvContent.isBlank()) {
                return CsvImportParseResult.EmptyFile()
            }

            val allExistingTxs = transactionRepository.getAllTransactionsList()
            val currentCategories = categoryRepository.getAllCategoriesList()

            val preview = CsvImporter.parseAndValidate(
                csvContent = csvContent,
                existingTransactions = allExistingTxs,
                existingCategories = currentCategories,
                duplicateMode = CsvDuplicateMode.SKIP_EXISTING
            )

            CsvImportParseResult.Success(preview)
        } catch (e: Exception) {
            CsvImportParseResult.Failure(e.message ?: "Unknown error reading CSV")
        }
    }

    suspend fun updateDuplicateMode(currentPreview: CsvPreviewData, mode: CsvDuplicateMode): CsvPreviewData {
        val allExistingTxs = transactionRepository.getAllTransactionsList()
        val currentCategories = categoryRepository.getAllCategoriesList()

        return CsvImporter.parseAndValidate(
            csvContent = currentPreview.rawCsvContent,
            existingTransactions = allExistingTxs,
            existingCategories = currentCategories,
            duplicateMode = mode
        )
    }

    suspend fun executeImport(preview: CsvPreviewData, cacheDir: File): CsvImportFinalResult {
        return try {
            val allExistingTxs = transactionRepository.getAllTransactionsList()
            val backupFile = File(cacheDir, "fintrack_pre_import_backup_${System.currentTimeMillis()}.csv")

            transactionRepository.executeAtomicCsvImport(
                previewData = preview,
                backupFile = backupFile,
                allExistingTransactions = allExistingTxs
            )
        } catch (e: Exception) {
            CsvImportFinalResult(
                success = false,
                insertedCount = 0,
                updatedCount = 0,
                skippedCount = 0,
                failedCount = preview.validTransactionsToImport.size,
                categoriesCreatedCount = 0,
                subcategoriesCreatedCount = 0,
                pendingCount = 0,
                unverifiedCount = 0,
                errorMessage = "Execution error: ${e.message}"
            )
        }
    }
}
