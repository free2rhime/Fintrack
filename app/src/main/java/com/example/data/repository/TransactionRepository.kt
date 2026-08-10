package com.example.data.repository

import androidx.room.RoomDatabase
import androidx.room.withTransaction
import com.example.data.dao.ExchangeRateDao
import com.example.data.dao.TransactionDao
import com.example.data.model.ExchangeRateEntity
import com.example.data.model.TransactionEntity
import com.example.data.service.ExchangeRateService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID

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

class TransactionRepository(
    private val transactionDao: TransactionDao,
    private val exchangeRateService: ExchangeRateService,
    private val exchangeRateDao: ExchangeRateDao,
    private val database: RoomDatabase
) {
    private val syncMutex = Mutex()

    val allTransactions: Flow<List<TransactionEntity>> = transactionDao.getAllTransactions()


    fun getTransactionsInRange(startDate: String, endDate: String): Flow<List<TransactionEntity>> {
        return transactionDao.getTransactionsInRange(startDate, endDate)
    }

    suspend fun getTransactionById(id: String): TransactionEntity? {
        return transactionDao.getTransactionById(id)
    }

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
    ): TransactionEntity {
        val bnrResult = exchangeRateService.getOfficialRate(date)
        val isOfficial = bnrResult.status == "OFFICIAL" && bnrResult.rate > 0.0
        val amountEUR = if (isOfficial) ExchangeRateService.calculateAmountEUR(amountRON, bnrResult.rate) else 0.0
        val status = if (isOfficial) "OFFICIAL" else "PENDING"
        val rateSource = if (isOfficial) "BNR_OFFICIAL" else "NONE"

        val now = System.currentTimeMillis()
        val transaction = TransactionEntity(
            id = id ?: UUID.randomUUID().toString(),
            date = date,
            description = description.trim(),
            amountRON = amountRON,
            amountEUR = amountEUR,
            exchangeRate = bnrResult.rate,
            exchangeRateDate = bnrResult.effectiveDate,
            exchangeRateSource = rateSource,
            conversionStatus = status,
            type = type,
            account = account,
            category = category,
            subCategory = subCategory,
            destination = if (type == "Income") destination else null,
            createdAt = if (id == null) now else (getTransactionById(id)?.createdAt ?: now),
            updatedAt = now
        )

        transactionDao.insertTransaction(transaction)
        return transaction
    }

    /**
     * DUPLICATE TRANSACTION FEATURE:
     * Copies all fields from source transaction, sets date to today's local date,
     * recomputes EUR rate for today, assigns new UUID.
     */
    suspend fun createDuplicateTemplate(source: TransactionEntity): TransactionEntity {
        val todayStr = LocalDate.now(ZoneId.systemDefault()).toString()
        val bnrResult = exchangeRateService.getOfficialRate(todayStr)
        val isOfficial = bnrResult.status == "OFFICIAL" && bnrResult.rate > 0.0
        val amountEUR = if (isOfficial) ExchangeRateService.calculateAmountEUR(source.amountRON, bnrResult.rate) else 0.0
        val status = if (isOfficial) "OFFICIAL" else "PENDING"
        val rateSource = if (isOfficial) "BNR_OFFICIAL" else "NONE"

        return source.copy(
            id = UUID.randomUUID().toString(),
            date = todayStr,
            amountEUR = amountEUR,
            exchangeRate = bnrResult.rate,
            exchangeRateDate = bnrResult.effectiveDate,
            exchangeRateSource = rateSource,
            conversionStatus = status,
            destination = if (source.type == "Income") source.destination else null,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
    }

    suspend fun getDescriptionSuggestions(query: String, limit: Int = 8): List<String> {
        if (query.trim().length < 2) return emptyList()
        return transactionDao.getDescriptionSuggestions(query.trim(), limit)
    }

    suspend fun insertBatchWithTransaction(transactions: List<TransactionEntity>) {
        if (transactions.isEmpty()) return
        database.withTransaction {
            transactionDao.insertAllTransactions(transactions)
        }
    }

    suspend fun getUnverifiedTransactions(): List<TransactionEntity> {
        return transactionDao.getUnverifiedTransactions()
    }

    suspend fun getAllTransactionsList(): List<TransactionEntity> {
        return transactionDao.getAllTransactionsList()
    }

    suspend fun syncPendingConversions(): PendingRetryResult = withContext(Dispatchers.IO) {
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
                    transactionDao.insertTransaction(updated)
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

    suspend fun applyRepairBatch(repairs: List<PreparedRepairItem>): Int {
        if (repairs.isEmpty()) return 0
        database.withTransaction {
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
    }

    suspend fun applyRepairToTransactionAndCache(
        transaction: TransactionEntity,
        officialRate: Double,
        effectiveBnrDate: String
    ) {
        applyRepairBatch(listOf(PreparedRepairItem(transaction, officialRate, effectiveBnrDate)))
    }

    suspend fun insertTransaction(transaction: TransactionEntity) {
        transactionDao.insertTransaction(transaction)
    }

    suspend fun deleteTransaction(transaction: TransactionEntity) {
        transactionDao.deleteTransaction(transaction)
    }

    suspend fun deleteTransactionById(id: String) {
        transactionDao.deleteTransactionById(id)
    }

    suspend fun deleteAllTransactions() {
        transactionDao.deleteAllTransactions()
    }

    suspend fun insertBatch(transactions: List<TransactionEntity>) {
        transactionDao.insertAllTransactions(transactions)
    }
}
