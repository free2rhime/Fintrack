package com.example.data.repository

import com.example.data.model.TransactionEntity
import com.example.data.service.BnrDiagnosticResult
import com.example.data.service.BnrRateResult
import com.example.data.util.CsvImportFinalResult
import com.example.data.util.CsvPreviewData
import kotlinx.coroutines.flow.Flow
import java.io.File

data class PreparedRepairItem(
    val transaction: TransactionEntity,
    val officialRate: Double,
    val effectiveBnrDate: String
)

data class PendingRetryResult(
    val pendingBefore: Int,
    val convertedSuccessfully: Int,
    val stillPending: Int,
    val failedCount: Int,
    val mainFailureReason: String?
)

interface TransactionRepository {
    val allTransactions: Flow<List<TransactionEntity>>

    fun getTransactionsInRange(startDate: String, endDate: String): Flow<List<TransactionEntity>>

    suspend fun getTransactionById(id: String): TransactionEntity?

    suspend fun saveTransaction(
        id: String? = null,
        date: String,
        description: String,
        amountRON: Double,
        type: String,
        account: String,
        category: String,
        subCategory: String,
        destination: String? = null
    ): TransactionEntity

    suspend fun createDuplicateTemplate(source: TransactionEntity): TransactionEntity

    suspend fun getDescriptionSuggestions(query: String, limit: Int = 8): List<String>

    suspend fun insertBatchWithTransaction(transactions: List<TransactionEntity>)

    suspend fun getUnverifiedTransactions(): List<TransactionEntity>

    suspend fun getAllTransactionsList(): List<TransactionEntity>

    suspend fun syncPendingConversions(): PendingRetryResult

    suspend fun applyRepairBatch(repairs: List<PreparedRepairItem>): Int

    suspend fun applyRepairToTransactionAndCache(
        transaction: TransactionEntity,
        officialRate: Double,
        effectiveBnrDate: String
    )

    suspend fun insertTransaction(transaction: TransactionEntity)

    suspend fun deleteTransaction(transaction: TransactionEntity)

    suspend fun deleteTransactionById(id: String)

    suspend fun deleteAllTransactions()

    suspend fun insertBatch(transactions: List<TransactionEntity>)

    suspend fun getOfficialRate(date: String): BnrRateResult

    suspend fun runBnrDiagnostic(): BnrDiagnosticResult

    suspend fun executeAtomicCsvImport(
        previewData: CsvPreviewData,
        backupFile: File,
        allExistingTransactions: List<TransactionEntity>
    ): CsvImportFinalResult
}
