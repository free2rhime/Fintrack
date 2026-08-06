package com.example.data.repository

import androidx.room.RoomDatabase
import androidx.room.withTransaction
import com.example.data.dao.ExchangeRateDao
import com.example.data.dao.TransactionDao
import com.example.data.model.ExchangeRateEntity
import com.example.data.model.TransactionEntity
import com.example.data.service.ExchangeRateService
import kotlinx.coroutines.flow.Flow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

data class PreparedRepairItem(
    val transaction: TransactionEntity,
    val officialRate: Double,
    val effectiveBnrDate: String
)

class TransactionRepository(
    private val transactionDao: TransactionDao,
    private val exchangeRateService: ExchangeRateService,
    private val exchangeRateDao: ExchangeRateDao? = null,
    private val database: RoomDatabase? = null
) {
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
        val (amountEUR, status) = if (bnrResult.status == "OFFICIAL" && bnrResult.rate > 0.0) {
            Pair(
                ExchangeRateService.calculateAmountEUR(amountRON, bnrResult.rate),
                "OFFICIAL"
            )
        } else {
            Pair(0.0, "PENDING")
        }

        val now = System.currentTimeMillis()
        val transaction = TransactionEntity(
            id = id ?: UUID.randomUUID().toString(),
            date = date,
            description = description.trim(),
            amountRON = amountRON,
            amountEUR = amountEUR,
            exchangeRate = bnrResult.rate,
            exchangeRateDate = bnrResult.effectiveDate,
            exchangeRateSource = "BNR_OFFICIAL",
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
     * DUPLICATE TRANSACTION FEATURE (Mandatory Rule):
     * Copies all fields from source transaction, sets date to today's date,
     * recomputes EUR rate for today, assigns new UUID.
     */
    suspend fun createDuplicateTemplate(source: TransactionEntity): TransactionEntity {
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        val bnrResult = exchangeRateService.getOfficialRate(todayStr)
        val (amountEUR, status) = if (bnrResult.status == "OFFICIAL" && bnrResult.rate > 0.0) {
            Pair(
                ExchangeRateService.calculateAmountEUR(source.amountRON, bnrResult.rate),
                "OFFICIAL"
            )
        } else {
            Pair(0.0, "PENDING")
        }

        return source.copy(
            id = UUID.randomUUID().toString(),
            date = todayStr,
            amountEUR = amountEUR,
            exchangeRate = bnrResult.rate,
            exchangeRateDate = bnrResult.effectiveDate,
            exchangeRateSource = "BNR_OFFICIAL",
            conversionStatus = status,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
    }

    suspend fun getUnverifiedTransactions(): List<TransactionEntity> {
        return transactionDao.getUnverifiedTransactions()
    }

    suspend fun getAllTransactionsList(): List<TransactionEntity> {
        return transactionDao.getAllTransactionsList()
    }

    suspend fun syncPendingConversions(): Int {
        val pending = transactionDao.getPendingTransactions()
        var updatedCount = 0
        for (tx in pending) {
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
                updatedCount++
            }
        }
        return updatedCount
    }

    suspend fun applyRepairBatch(repairs: List<PreparedRepairItem>): Int {
        if (repairs.isEmpty()) return 0
        val db = database
        if (db != null) {
            db.withTransaction {
                for (item in repairs) {
                    applySingleRepairInternal(item)
                }
            }
        } else {
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
        exchangeRateDao?.deleteUnverifiedRatesForDate(item.transaction.date)

        val rateEntity = ExchangeRateEntity(
            date = item.transaction.date,
            requestedDate = item.transaction.date,
            effectiveDate = item.effectiveBnrDate,
            rate = item.officialRate,
            source = "BNR_OFFICIAL",
            fetchedAt = System.currentTimeMillis(),
            status = "OFFICIAL"
        )
        exchangeRateDao?.insertRate(rateEntity)
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
