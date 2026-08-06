package com.example.data.repository

import com.example.data.dao.TransactionDao
import com.example.data.model.TransactionEntity
import com.example.data.service.ExchangeRateService
import kotlinx.coroutines.flow.Flow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class TransactionRepository(
    private val transactionDao: TransactionDao,
    private val exchangeRateService: ExchangeRateService
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
        val rate = exchangeRateService.getHistoricalRate(date)
        val amountEUR = Math.round((amountRON / rate) * 100.0) / 100.0

        val now = System.currentTimeMillis()
        val transaction = TransactionEntity(
            id = id ?: UUID.randomUUID().toString(),
            date = date,
            description = description.trim(),
            amountRON = amountRON,
            amountEUR = amountEUR,
            exchangeRate = rate,
            exchangeRateDate = date,
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
        val rate = exchangeRateService.getHistoricalRate(todayStr)
        val amountEUR = Math.round((source.amountRON / rate) * 100.0) / 100.0

        return source.copy(
            id = UUID.randomUUID().toString(),
            date = todayStr,
            amountEUR = amountEUR,
            exchangeRate = rate,
            exchangeRateDate = todayStr,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
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
