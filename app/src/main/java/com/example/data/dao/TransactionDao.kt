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
    @Query("""
        SELECT * FROM transactions 
        WHERE ((:householdId IS NULL AND householdId IS NULL) OR householdId = :householdId)
        ORDER BY date DESC, createdAt DESC
    """)
    fun getAllTransactions(householdId: String? = null): Flow<List<TransactionEntity>>

    @Query("""
        SELECT * FROM transactions 
        WHERE ((:householdId IS NULL AND householdId IS NULL) OR householdId = :householdId)
        AND date >= :startDate AND date <= :endDate 
        ORDER BY date DESC, createdAt DESC
    """)
    fun getTransactionsInRange(startDate: String, endDate: String, householdId: String? = null): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getTransactionById(id: String): TransactionEntity?

    @Query("SELECT * FROM transactions WHERE (conversionStatus IS NULL OR (conversionStatus != 'PENDING' AND conversionStatus NOT LIKE 'PENDING\\_%' ESCAPE '\\' AND conversionStatus != 'FAILED' AND conversionStatus NOT LIKE 'FAILED\\_%' ESCAPE '\\')) AND (conversionStatus IS NULL OR conversionStatus = 'UNVERIFIED' OR exchangeRateSource IS NULL OR exchangeRateSource != 'BNR_OFFICIAL')")
    suspend fun getUnverifiedTransactions(): List<TransactionEntity>

    @Query("SELECT * FROM transactions WHERE conversionStatus = 'PENDING' OR conversionStatus LIKE 'PENDING\\_%' ESCAPE '\\' OR conversionStatus = 'FAILED' OR conversionStatus LIKE 'FAILED\\_%' ESCAPE '\\'")
    suspend fun getRetryablePendingTransactions(): List<TransactionEntity>

    @Query("SELECT * FROM transactions WHERE conversionStatus = 'PENDING' OR conversionStatus LIKE 'PENDING\\_%' ESCAPE '\\' OR conversionStatus = 'FAILED' OR conversionStatus LIKE 'FAILED\\_%' ESCAPE '\\'")
    suspend fun getPendingTransactions(): List<TransactionEntity>

    @Query("""
        SELECT * FROM transactions 
        WHERE ((:householdId IS NULL AND householdId IS NULL) OR householdId = :householdId)
        ORDER BY date DESC, createdAt DESC
    """)
    suspend fun getAllTransactionsList(householdId: String? = null): List<TransactionEntity>

    @Query("""
        SELECT description FROM transactions 
        WHERE ((:householdId IS NULL AND householdId IS NULL) OR householdId = :householdId)
        AND description IS NOT NULL AND TRIM(description) != '' AND LOWER(description) LIKE '%' || LOWER(:query) || '%' 
        GROUP BY description 
        ORDER BY MAX(createdAt) DESC, COUNT(*) DESC 
        LIMIT :limit
    """)
    suspend fun getDescriptionSuggestions(query: String, limit: Int = 8, householdId: String? = null): List<String>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: TransactionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllTransactions(transactions: List<TransactionEntity>)

    @Delete
    suspend fun deleteTransaction(transaction: TransactionEntity)

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteTransactionById(id: String)

    @Query("""
        DELETE FROM transactions 
        WHERE ((:householdId IS NULL AND householdId IS NULL) OR householdId = :householdId)
    """)
    suspend fun deleteTransactionsByHousehold(householdId: String? = null)

    @Query("""
        DELETE FROM transactions 
        WHERE ((:householdId IS NULL AND householdId IS NULL) OR householdId = :householdId)
    """)
    suspend fun deleteAllTransactions(householdId: String? = null)
}

