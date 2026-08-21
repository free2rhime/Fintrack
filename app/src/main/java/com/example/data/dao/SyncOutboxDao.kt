package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.SyncOutboxEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SyncOutboxDao {

    // ==========================================
    // A. Pending Queue Retrieval (FIFO Order)
    // ==========================================

    @Query("SELECT * FROM sync_outbox ORDER BY createdAt ASC")
    fun getAllOutboxEntries(): Flow<List<SyncOutboxEntity>>

    @Query("SELECT * FROM sync_outbox WHERE status = 'PENDING' ORDER BY createdAt ASC")
    suspend fun getPendingEntries(): List<SyncOutboxEntity>

    @Query("SELECT * FROM sync_outbox WHERE status = 'PENDING' ORDER BY createdAt ASC LIMIT :limit")
    suspend fun getPendingBatch(limit: Int): List<SyncOutboxEntity>

    @Query("SELECT COUNT(*) FROM sync_outbox WHERE status = 'PENDING'")
    suspend fun getPendingCount(): Int

    @Query("SELECT COUNT(*) FROM sync_outbox WHERE status = 'PENDING'")
    fun getPendingCountFlow(): Flow<Int>

    @Query("SELECT * FROM sync_outbox WHERE entityId = :entityId AND status = 'PENDING' ORDER BY createdAt DESC LIMIT 1")
    suspend fun getPendingEntryForEntity(entityId: String): SyncOutboxEntity?

    @Query("SELECT * FROM sync_outbox WHERE id = :id")
    suspend fun getEntryById(id: String): SyncOutboxEntity?

    // ==========================================
    // B. Status Transitions
    // ==========================================

    @Query("UPDATE sync_outbox SET status = 'IN_PROGRESS', lastAttemptAt = :lastAttemptAt, updatedAt = :updatedAt WHERE id = :id")
    suspend fun markInProgress(
        id: String,
        lastAttemptAt: Long = System.currentTimeMillis(),
        updatedAt: Long = System.currentTimeMillis()
    ): Int

    @Query("UPDATE sync_outbox SET status = 'PENDING', updatedAt = :updatedAt WHERE id = :id")
    suspend fun markPending(
        id: String,
        updatedAt: Long = System.currentTimeMillis()
    ): Int

    @Query("UPDATE sync_outbox SET status = 'ACKNOWLEDGED', updatedAt = :updatedAt WHERE id = :id")
    suspend fun markAcknowledged(
        id: String,
        updatedAt: Long = System.currentTimeMillis()
    ): Int

    @Query("UPDATE sync_outbox SET status = 'SUCCESS', updatedAt = :updatedAt WHERE id = :id")
    suspend fun markSuccess(
        id: String,
        updatedAt: Long = System.currentTimeMillis()
    ): Int

    @Query("UPDATE sync_outbox SET status = 'FAILED', errorCode = :errorCode, errorMessage = :errorMessage, retryCount = :retryCount, lastAttemptAt = :lastAttemptAt, updatedAt = :updatedAt WHERE id = :id")
    suspend fun markFailed(
        id: String,
        errorCode: String?,
        errorMessage: String?,
        retryCount: Int,
        lastAttemptAt: Long = System.currentTimeMillis(),
        updatedAt: Long = System.currentTimeMillis()
    ): Int

    // ==========================================
    // C. Startup Recovery
    // ==========================================

    @Query("UPDATE sync_outbox SET status = 'PENDING', updatedAt = :updatedAt WHERE status = 'IN_PROGRESS'")
    suspend fun resetInProgressToPending(
        updatedAt: Long = System.currentTimeMillis()
    ): Int

    // ==========================================
    // D. Retry Tracking
    // ==========================================

    @Query("UPDATE sync_outbox SET retryCount = retryCount + 1, lastAttemptAt = :lastAttemptAt, errorCode = :errorCode, errorMessage = :errorMessage, updatedAt = :updatedAt WHERE id = :id")
    suspend fun incrementRetryCount(
        id: String,
        lastAttemptAt: Long = System.currentTimeMillis(),
        errorCode: String? = null,
        errorMessage: String? = null,
        updatedAt: Long = System.currentTimeMillis()
    ): Int

    @Query("UPDATE sync_outbox SET status = 'PENDING', retryCount = retryCount + 1, lastAttemptAt = :lastAttemptAt, errorCode = :errorCode, errorMessage = :errorMessage, updatedAt = :updatedAt WHERE id = :id")
    suspend fun recordRetryFailure(
        id: String,
        errorCode: String?,
        errorMessage: String?,
        lastAttemptAt: Long = System.currentTimeMillis(),
        updatedAt: Long = System.currentTimeMillis()
    ): Int

    // ==========================================
    // E. Queue Cleanup
    // ==========================================

    @Query("DELETE FROM sync_outbox WHERE status = 'ACKNOWLEDGED'")
    suspend fun clearAcknowledgedEntries()

    @Query("DELETE FROM sync_outbox WHERE status = 'ACKNOWLEDGED'")
    suspend fun deleteAcknowledgedEntries(): Int

    @Query("DELETE FROM sync_outbox WHERE status = 'SUCCESS'")
    suspend fun deleteSuccessEntries(): Int

    @Query("DELETE FROM sync_outbox WHERE status IN ('ACKNOWLEDGED', 'SUCCESS') AND updatedAt < :cutoffTime")
    suspend fun deleteOldCompletedEntries(cutoffTime: Long): Int

    @Query("DELETE FROM sync_outbox WHERE status = 'FAILED' AND updatedAt < :cutoffTime")
    suspend fun deleteOldFailedEntries(cutoffTime: Long): Int

    @Query("DELETE FROM sync_outbox WHERE id = :id")
    suspend fun deleteOutboxEntryById(id: String)

    @Query("DELETE FROM sync_outbox WHERE entityId = :entityId")
    suspend fun deleteOutboxEntriesForEntity(entityId: String)

    @Query("DELETE FROM sync_outbox")
    suspend fun deleteAllOutboxEntries()

    // ==========================================
    // F. Duplicate Suppression Readiness
    // ==========================================

    @Query("SELECT * FROM sync_outbox WHERE entityType = :entityType AND entityId = :entityId AND status = 'PENDING' ORDER BY createdAt DESC LIMIT 1")
    suspend fun getPendingEntry(entityType: String, entityId: String): SyncOutboxEntity?

    @Query("SELECT * FROM sync_outbox WHERE entityType = :entityType AND entityId = :entityId AND status IN ('PENDING', 'IN_PROGRESS') ORDER BY createdAt DESC LIMIT 1")
    suspend fun getActiveEntry(entityType: String, entityId: String): SyncOutboxEntity?

    @Query("SELECT entityId FROM sync_outbox WHERE entityType = :entityType AND status IN ('PENDING', 'IN_PROGRESS')")
    suspend fun getActiveEntityIdsByType(entityType: String): List<String>

    @Query("DELETE FROM sync_outbox WHERE entityType = :entityType AND entityId = :entityId")
    suspend fun deleteEntriesForEntity(entityType: String, entityId: String): Int

    // ==========================================
    // G. Standard CRUD Operations
    // ==========================================

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOutboxEntry(entry: SyncOutboxEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllOutboxEntries(entries: List<SyncOutboxEntity>)

    @Update
    suspend fun updateOutboxEntry(entry: SyncOutboxEntity)
}

