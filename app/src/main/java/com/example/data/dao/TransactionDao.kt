package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.TransactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions ORDER BY date DESC, createdAt DESC")
    fun getAllTransactions(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE date >= :startDate AND date <= :endDate ORDER BY date DESC, createdAt DESC")
    fun getTransactionsInRange(startDate: String, endDate: String): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getTransactionById(id: String): TransactionEntity?

    @Query("SELECT * FROM transactions WHERE (conversionStatus IS NULL OR (conversionStatus != 'PENDING' AND conversionStatus != 'FAILED')) AND (conversionStatus IS NULL OR conversionStatus = 'UNVERIFIED' OR exchangeRateSource IS NULL OR exchangeRateSource != 'BNR_OFFICIAL')")
    suspend fun getUnverifiedTransactions(): List<TransactionEntity>

    @Query("SELECT * FROM transactions WHERE conversionStatus = 'PENDING'")
    suspend fun getPendingTransactions(): List<TransactionEntity>

    @Query("SELECT * FROM transactions ORDER BY date DESC, createdAt DESC")
    suspend fun getAllTransactionsList(): List<TransactionEntity>

    @Query("SELECT description FROM transactions WHERE description IS NOT NULL AND TRIM(description) != '' AND LOWER(description) LIKE '%' || LOWER(:query) || '%' GROUP BY description ORDER BY MAX(createdAt) DESC, COUNT(*) DESC LIMIT :limit")
    suspend fun getDescriptionSuggestions(query: String, limit: Int = 8): List<String>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: TransactionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllTransactions(transactions: List<TransactionEntity>)

    @Delete
    suspend fun deleteTransaction(transaction: TransactionEntity)

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteTransactionById(id: String)

    @Query("DELETE FROM transactions")
    suspend fun deleteAllTransactions()
}
