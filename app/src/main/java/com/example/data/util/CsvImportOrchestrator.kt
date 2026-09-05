package com.example.data.util

import android.content.Context
import android.net.Uri
import com.example.data.model.TransactionEntity
import com.example.data.repository.CategoryRepository
import com.example.data.repository.TransactionRepository
import com.example.data.service.BnrRateResult
import com.example.data.service.ExchangeRateService
import kotlinx.coroutines.CancellationException
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
    suspend fun parseAndValidateFromUri(
        context: Context,
        uri: Uri,
        householdId: String? = null,
        userId: String = "local_user",
        createdByUid: String? = null
    ): CsvImportParseResult {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
            val csvContent = inputStream?.bufferedReader()?.use { it.readText() } ?: ""
            parseAndValidateFromContent(
                csvContent = csvContent,
                duplicateMode = CsvDuplicateMode.SKIP_EXISTING,
                householdId = householdId,
                userId = userId,
                createdByUid = createdByUid
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            CsvImportParseResult.Failure(e.message ?: "Unknown error reading CSV")
        }
    }

    suspend fun parseAndValidateFromContent(
        csvContent: String,
        duplicateMode: CsvDuplicateMode = CsvDuplicateMode.SKIP_EXISTING,
        householdId: String? = null,
        userId: String = "local_user",
        createdByUid: String? = null
    ): CsvImportParseResult {
        return try {
            if (csvContent.isBlank()) {
                return CsvImportParseResult.EmptyFile()
            }

            val allExistingTxs = transactionRepository.getAllTransactionsList()
            val currentCategories = categoryRepository.getAllCategoriesList(householdId)

            val initialPreview = CsvImporter.parseAndValidate(
                csvContent = csvContent,
                existingTransactions = allExistingTxs,
                existingCategories = currentCategories,
                duplicateMode = duplicateMode,
                householdId = householdId,
                userId = userId,
                createdByUid = createdByUid
            )

            val resolvedPreview = resolveBnrRatesForPreview(initialPreview)
            CsvImportParseResult.Success(resolvedPreview)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            CsvImportParseResult.Failure(e.message ?: "Unknown error reading CSV")
        }
    }

    private suspend fun resolveBnrRatesForPreview(initialPreview: CsvPreviewData): CsvPreviewData {
        if (initialPreview.validTransactionsToImport.isEmpty()) {
            return initialPreview
        }

        // 1. Identify distinct dates needing BNR rate resolution
        val datesNeedingResolution = initialPreview.validTransactionsToImport
            .filter { !(it.conversionStatus == "OFFICIAL" && it.exchangeRateSource == "BNR_OFFICIAL") }
            .map { it.date }
            .distinct()

        // 2. Resolve each distinct date once via TransactionRepository / ExchangeRateService
        val rateMap = mutableMapOf<String, BnrRateResult>()
        for (date in datesNeedingResolution) {
            val rateResult = transactionRepository.getOfficialRate(date)
            rateMap[date] = rateResult
        }

        // 3. Apply resolved rates to valid transactions
        val updatedTransactions = initialPreview.validTransactionsToImport.map { tx ->
            if (tx.conversionStatus == "OFFICIAL" && tx.exchangeRateSource == "BNR_OFFICIAL") {
                // Preserve explicit valid official CSV rate
                tx
            } else {
                val bnrResult = rateMap[tx.date]
                if (bnrResult != null && bnrResult.status == "OFFICIAL" && bnrResult.rate > 0.0) {
                    val rate = bnrResult.rate
                    val effectiveDate = bnrResult.effectiveDate.ifBlank { tx.date }
                    val amountEur = ExchangeRateService.calculateAmountEUR(tx.amountRON, rate)
                    tx.copy(
                        amountEUR = amountEur,
                        exchangeRate = rate,
                        exchangeRateDate = effectiveDate,
                        exchangeRateSource = "BNR_OFFICIAL",
                        conversionStatus = "OFFICIAL"
                    )
                } else {
                    // Safe PENDING state on failure / missing rate / future date
                    tx.copy(
                        amountEUR = 0.0,
                        exchangeRate = 0.0,
                        exchangeRateDate = tx.date,
                        exchangeRateSource = "NONE",
                        conversionStatus = "PENDING"
                    )
                }
            }
        }

        // 4. Re-calculate preview status counts
        val officialCount = updatedTransactions.count { it.conversionStatus == "OFFICIAL" }
        val unverifiedCount = updatedTransactions.count { it.conversionStatus == "UNVERIFIED" }
        val pendingCount = updatedTransactions.count { it.conversionStatus == "PENDING" }

        return initialPreview.copy(
            validTransactionsToImport = updatedTransactions,
            officialCount = officialCount,
            unverifiedCount = unverifiedCount,
            pendingCount = pendingCount
        )
    }

    suspend fun updateDuplicateMode(currentPreview: CsvPreviewData, mode: CsvDuplicateMode): CsvPreviewData {
        val proposedUpdatesCount = if (mode == CsvDuplicateMode.UPDATE_EXISTING) currentPreview.existingIdsCount else 0
        val proposedSkipsCount = if (mode == CsvDuplicateMode.SKIP_EXISTING) currentPreview.existingIdsCount else 0

        return currentPreview.copy(
            duplicateMode = mode,
            proposedUpdatesCount = proposedUpdatesCount,
            proposedSkipsCount = proposedSkipsCount
        )
    }

    suspend fun executeImport(
        preview: CsvPreviewData,
        cacheDir: File,
        householdId: String? = null,
        userId: String = "local_user",
        createdByUid: String? = null
    ): CsvImportFinalResult {
        return try {
            val allExistingTxs = transactionRepository.getAllTransactionsList()
            val backupFile = File(cacheDir, "fintrack_pre_import_backup_${System.currentTimeMillis()}.csv")

            transactionRepository.executeAtomicCsvImport(
                previewData = preview,
                backupFile = backupFile,
                allExistingTransactions = allExistingTxs,
                householdId = householdId,
                userId = userId,
                createdByUid = createdByUid
            )
        } catch (e: CancellationException) {
            throw e
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
