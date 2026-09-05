package com.example.data.repository

import androidx.room.RoomDatabase
import androidx.room.withTransaction
import com.example.data.dao.ExchangeRateDao
import com.example.data.dao.TransactionDao
import com.example.data.model.ExchangeRateEntity
import com.example.data.model.TransactionEntity
import com.example.data.service.BnrDiagnosticResult
import com.example.data.service.BnrRateResult
import com.example.data.service.ExchangeRateService
import com.example.data.util.CsvImportFinalResult
import com.example.data.util.CsvImporter
import com.example.data.util.CsvPreviewData
import com.example.data.db.FinTrackDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withContext
import java.io.File
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID

class RoomTransactionRepository(
    private val transactionDao: TransactionDao,
    private val exchangeRateService: ExchangeRateService,
    private val exchangeRateDao: ExchangeRateDao,
    private val database: RoomDatabase,
    private val syncOutboxDao: com.example.data.dao.SyncOutboxDao? = null,
    private val onOutboxMutated: (() -> Unit)? = null
) : TransactionRepository {
    private val syncMutex = Mutex()

    override fun getTransactions(householdId: String?): Flow<List<TransactionEntity>> {
        return transactionDao.getAllTransactions(householdId)
    }

    override fun getTransactionsInRange(startDate: String, endDate: String): Flow<List<TransactionEntity>> {
        return transactionDao.getTransactionsInRange(startDate, endDate)
    }

    override suspend fun getTransactionById(id: String): TransactionEntity? {
        return transactionDao.getTransactionById(id)
    }

    private suspend fun <T> executeWithTransaction(block: suspend () -> T): T {
        val result = try {
            database.withTransaction { block() }
        } catch (e: Exception) {
            if (e is UninitializedPropertyAccessException || e is UnsupportedOperationException || e is IllegalStateException) {
                block()
            } else {
                throw e
            }
        }
        onOutboxMutated?.invoke()
        return result
    }

    override suspend fun saveTransaction(
        id: String?,
        date: String,
        description: String,
        amountRON: Double,
        type: String,
        account: String,
        category: String,
        subCategory: String,
        destination: String?,
        userId: String,
        householdId: String?
    ): TransactionEntity {
        val bnrResult = try {
            exchangeRateService.getOfficialRate(date)
        } catch (e: Exception) {
            BnrRateResult(
                requestedDate = date,
                effectiveDate = date,
                rate = 0.0,
                source = "NONE",
                status = "XML_PARSE_ERROR",
                diagnostic = "Exception fetching rate: ${e.javaClass.simpleName}: ${e.message?.take(100)}"
            )
        }
        val isOfficial = bnrResult.status == "OFFICIAL" && bnrResult.rate > 0.0
        val amountEUR = if (isOfficial) ExchangeRateService.calculateAmountEUR(amountRON, bnrResult.rate) else 0.0
        val status = if (isOfficial) "OFFICIAL" else "PENDING"
        val rateSource = if (isOfficial) "BNR_OFFICIAL" else "NONE"

        val existingTx = if (id != null) getTransactionById(id) else null
        val now = System.currentTimeMillis()
        val transaction = TransactionEntity(
            id = id ?: UUID.randomUUID().toString(),
            userId = userId,
            date = date,
            description = description.trim(),
            amountRON = amountRON,
            amountEUR = amountEUR,
            exchangeRate = if (isOfficial) bnrResult.rate else 0.0,
            exchangeRateDate = if (isOfficial) bnrResult.effectiveDate else date,
            exchangeRateSource = rateSource,
            conversionStatus = status,
            type = type,
            account = account,
            category = category,
            subCategory = subCategory,
            destination = if (type == "Income") destination else null,
            createdAt = existingTx?.createdAt ?: now,
            updatedAt = now,
            householdId = householdId ?: existingTx?.householdId,
            createdByUid = existingTx?.createdByUid ?: if (userId != "local_user") userId else null
        )

        executeWithTransaction {
            transactionDao.insertTransaction(transaction)
            enqueueOutboxOperationInternal("TRANSACTION", transaction.id, "UPSERT", now)
        }
        return transaction
    }

    override suspend fun createDuplicateTemplate(source: TransactionEntity): TransactionEntity {
        val todayStr = LocalDate.now(ZoneId.systemDefault()).toString()

        // Clear old EUR metadata before obtaining new rate
        val clearedSource = source.copy(
            amountEUR = 0.0,
            exchangeRate = 0.0,
            exchangeRateDate = todayStr,
            exchangeRateSource = "NONE",
            conversionStatus = "PENDING"
        )

        val bnrResult = try {
            exchangeRateService.getOfficialRate(todayStr)
        } catch (e: Exception) {
            BnrRateResult(
                requestedDate = todayStr,
                effectiveDate = todayStr,
                rate = 0.0,
                source = "NONE",
                status = "XML_PARSE_ERROR",
                diagnostic = "Exception fetching rate: ${e.javaClass.simpleName}: ${e.message?.take(100)}"
            )
        }
        val isOfficial = bnrResult.status == "OFFICIAL" && bnrResult.rate > 0.0
        val amountEUR = if (isOfficial) ExchangeRateService.calculateAmountEUR(clearedSource.amountRON, bnrResult.rate) else 0.0
        val status = if (isOfficial) "OFFICIAL" else "PENDING"
        val rateSource = if (isOfficial) "BNR_OFFICIAL" else "NONE"

        val now = System.currentTimeMillis()
        return clearedSource.copy(
            id = UUID.randomUUID().toString(),
            date = todayStr,
            amountEUR = amountEUR,
            exchangeRate = if (isOfficial) bnrResult.rate else 0.0,
            exchangeRateDate = if (isOfficial) bnrResult.effectiveDate else todayStr,
            exchangeRateSource = rateSource,
            conversionStatus = status,
            destination = if (clearedSource.type == "Income") clearedSource.destination else null,
            createdAt = now,
            updatedAt = now
        )
    }

    override suspend fun getDescriptionSuggestions(query: String, limit: Int, householdId: String?): List<String> {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return emptyList()
        val escaped = escapeSqlLike(trimmed)
        return transactionDao.getDescriptionSuggestions(escaped, limit, householdId)
            .distinctBy { it.lowercase() }
    }

    private fun escapeSqlLike(input: String): String {
        return input
            .replace("\\", "\\\\")
            .replace("%", "\\%")
            .replace("_", "\\_")
    }

    override suspend fun insertBatchWithTransaction(transactions: List<TransactionEntity>) {
        if (transactions.isEmpty()) return
        executeWithTransaction {
            transactionDao.insertAllTransactions(transactions)
            val now = System.currentTimeMillis()
            for (tx in transactions) {
                enqueueOutboxOperationInternal("TRANSACTION", tx.id, "UPSERT", tx.updatedAt.takeIf { it > 0 } ?: now)
            }
        }
    }

    override suspend fun getUnverifiedTransactions(): List<TransactionEntity> {
        return transactionDao.getUnverifiedTransactions()
    }

    override suspend fun getAllTransactionsList(): List<TransactionEntity> {
        return transactionDao.getAllTransactionsList()
    }

    override suspend fun syncPendingConversions(): PendingRetryResult = withContext(Dispatchers.IO) {
        if (!syncMutex.tryLock()) {
            return@withContext PendingRetryResult(0, 0, 0, 0, "Sync already in progress")
        }
        try {
            val eligiblePending = transactionDao.getRetryablePendingTransactions()

            val pendingBefore = eligiblePending.size
            if (pendingBefore == 0) {
                return@withContext PendingRetryResult(0, 0, 0, 0, null)
            }

            var convertedSuccessfully = 0
            var stillPending = 0
            var failedCount = 0
            val failureReasonCounts = mutableMapOf<String, Int>()

            val successfullyConverted = mutableListOf<TransactionEntity>()
            val unresolvedDiagnostics = mutableListOf<TransactionEntity>()

            for (tx in eligiblePending) {
                val bnrResult = exchangeRateService.getOfficialRate(tx.date)
                if (bnrResult.status == "OFFICIAL" && bnrResult.rate > 0.0) {
                    val correctEUR = ExchangeRateService.calculateAmountEUR(tx.amountRON, bnrResult.rate)
                    val updated = tx.copy(
                        amountEUR = correctEUR,
                        exchangeRate = bnrResult.rate,
                        exchangeRateDate = bnrResult.effectiveDate,
                        exchangeRateSource = "BNR_OFFICIAL",
                        conversionStatus = "OFFICIAL",
                        updatedAt = System.currentTimeMillis()
                    )
                    successfullyConverted.add(updated)
                    convertedSuccessfully++
                } else if (bnrResult.status == "NOT_YET_PUBLISHED") {
                    stillPending++
                    failureReasonCounts["Date in future"] = (failureReasonCounts["Date in future"] ?: 0) + 1
                } else {
                    failedCount++
                    stillPending++
                    val reasonKey = mapBnrStatusToUserReason(bnrResult.status)
                    failureReasonCounts[reasonKey] = (failureReasonCounts[reasonKey] ?: 0) + 1

                    val updatedStatus = if (bnrResult.status.startsWith("PENDING_") || bnrResult.status.startsWith("FAILED_")) {
                        bnrResult.status
                    } else {
                        "PENDING_${bnrResult.status}"
                    }
                    val updated = tx.copy(
                        conversionStatus = updatedStatus,
                        updatedAt = System.currentTimeMillis()
                    )
                    unresolvedDiagnostics.add(updated)
                }
            }

            if (successfullyConverted.isNotEmpty()) {
                executeWithTransaction {
                    for (updated in successfullyConverted) {
                        transactionDao.insertTransaction(updated)
                        enqueueOutboxOperationInternal(
                            entityType = "TRANSACTION",
                            entityId = updated.id,
                            operation = "UPSERT",
                            timestamp = updated.updatedAt
                        )
                    }
                }
            }

            if (unresolvedDiagnostics.isNotEmpty()) {
                for (updated in unresolvedDiagnostics) {
                    transactionDao.insertTransaction(updated)
                }
            }

            val mainReason = failureReasonCounts.maxByOrNull { it.value }?.key

            PendingRetryResult(
                pendingBefore = pendingBefore,
                convertedSuccessfully = convertedSuccessfully,
                stillPending = stillPending,
                failedCount = failedCount,
                mainFailureReason = mainReason
            )
        } finally {
            syncMutex.unlock()
        }
    }

    private fun mapBnrStatusToUserReason(status: String): String {
        return when {
            status in listOf("NO_INTERNET_PERMISSION", "NO_NETWORK", "DNS_FAILURE", "TLS_FAILURE", "TIMEOUT") -> "Network Unavailable"
            status in listOf("XML_PARSE_ERROR", "EMPTY_RESPONSE", "HTTP_ERROR") -> "BNR Response Could Not Be Read"
            status in listOf("EUR_RATE_NOT_FOUND", "NO_APPLICABLE_DATE") -> "Rate Not Found for Date"
            status == "NOT_YET_PUBLISHED" -> "Date in Future"
            status == "INVALID_DATE" -> "Invalid Date"
            else -> "Conversion Pending"
        }
    }

    override suspend fun applyRepairBatch(repairs: List<PreparedRepairItem>): Int {
        if (repairs.isEmpty()) return 0
        executeWithTransaction {
            for (item in repairs) {
                applySingleRepairInternal(item)
            }
        }
        return repairs.size
    }

    private suspend fun applySingleRepairInternal(item: PreparedRepairItem) {
        val correctEUR = ExchangeRateService.calculateAmountEUR(item.transaction.amountRON, item.officialRate)
        val updatedTx = item.transaction.copy(
            amountEUR = correctEUR,
            exchangeRate = item.officialRate,
            exchangeRateDate = item.effectiveBnrDate,
            exchangeRateSource = "BNR_OFFICIAL",
            conversionStatus = "OFFICIAL",
            updatedAt = System.currentTimeMillis()
        )

        transactionDao.insertTransaction(updatedTx)
        enqueueOutboxOperationInternal("TRANSACTION", updatedTx.id, "UPSERT", updatedTx.updatedAt)

        // Clean up legacy unverified cache row if present
        exchangeRateDao.deleteUnverifiedRatesForDate(item.transaction.date)

        val rateEntity = ExchangeRateEntity(
            date = item.transaction.date,
            requestedDate = item.transaction.date,
            effectiveDate = item.effectiveBnrDate,
            rate = item.officialRate,
            source = "BNR_OFFICIAL",
            fetchedAt = System.currentTimeMillis(),
            status = "OFFICIAL"
        )
        exchangeRateDao.insertRate(rateEntity)
        enqueueOutboxOperationInternal("EXCHANGE_RATE", rateEntity.date, "UPSERT", rateEntity.fetchedAt)
    }

    override suspend fun applyRepairToTransactionAndCache(
        transaction: TransactionEntity,
        officialRate: Double,
        effectiveBnrDate: String
    ) {
        applyRepairBatch(listOf(PreparedRepairItem(transaction, officialRate, effectiveBnrDate)))
    }

    override suspend fun insertTransaction(transaction: TransactionEntity) {
        executeWithTransaction {
            transactionDao.insertTransaction(transaction)
            enqueueOutboxOperationInternal("TRANSACTION", transaction.id, "UPSERT", transaction.updatedAt)
        }
    }

    override suspend fun deleteTransaction(transaction: TransactionEntity) {
        executeWithTransaction {
            transactionDao.deleteTransaction(transaction)
            enqueueOutboxOperationInternal("TRANSACTION", transaction.id, "DELETE")
        }
    }

    override suspend fun deleteTransactionById(id: String) {
        executeWithTransaction {
            transactionDao.deleteTransactionById(id)
            enqueueOutboxOperationInternal("TRANSACTION", id, "DELETE")
        }
    }

    override suspend fun deleteAllTransactions() {
        executeWithTransaction {
            val allTx = transactionDao.getAllTransactionsList()
            val now = System.currentTimeMillis()
            for (tx in allTx) {
                enqueueOutboxOperationInternal("TRANSACTION", tx.id, "DELETE", now)
            }
            transactionDao.deleteAllTransactions()
        }
    }

    override suspend fun insertBatch(transactions: List<TransactionEntity>) {
        insertBatchWithTransaction(transactions)
    }

    private suspend fun enqueueOutboxOperationInternal(
        entityType: String,
        entityId: String,
        operation: String,
        timestamp: Long = System.currentTimeMillis()
    ) {
        val dao = syncOutboxDao ?: (database as? FinTrackDatabase)?.syncOutboxDao() ?: return
        val existingPending = dao.getPendingEntryForEntity(entityId)
        if (existingPending != null && existingPending.operation == operation) {
            dao.updateOutboxEntry(existingPending.copy(updatedAt = timestamp))
        } else {
            dao.insertOutboxEntry(
                com.example.data.model.SyncOutboxEntity(
                    entityType = entityType,
                    entityId = entityId,
                    operation = operation,
                    status = "PENDING",
                    createdAt = timestamp,
                    updatedAt = timestamp
                )
            )
        }
    }

    override suspend fun getOfficialRate(date: String): BnrRateResult {
        return exchangeRateService.getOfficialRate(date)
    }

    override suspend fun runBnrDiagnostic(): BnrDiagnosticResult {
        return exchangeRateService.runDebugDiagnostic()
    }

    override suspend fun executeAtomicCsvImport(
        previewData: CsvPreviewData,
        backupFile: File,
        allExistingTransactions: List<TransactionEntity>
    ): CsvImportFinalResult {
        val result = CsvImporter.executeAtomicImport(
            database = database as FinTrackDatabase,
            previewData = previewData,
            backupFile = backupFile,
            allExistingTransactions = allExistingTransactions
        )
        if (result.success) {
            onOutboxMutated?.invoke()
        }
        return result
    }
}
