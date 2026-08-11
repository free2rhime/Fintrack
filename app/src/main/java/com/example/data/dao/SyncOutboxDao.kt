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
    @Query("SELECT * FROM sync_outbox ORDER BY createdAt ASC")
    fun getAllOutboxEntries(): Flow<List<SyncOutboxEntity>>

    @Query("SELECT * FROM sync_outbox WHERE status = 'PENDING' ORDER BY createdAt ASC")
    suspend fun getPendingEntries(): List<SyncOutboxEntity>

    @Query("SELECT * FROM sync_outbox WHERE entityId = :entityId AND status = 'PENDING' ORDER BY createdAt DESC LIMIT 1")
    suspend fun getPendingEntryForEntity(entityId: String): SyncOutboxEntity?

    @Query("SELECT * FROM sync_outbox WHERE id = :id")
    suspend fun getEntryById(id: String): SyncOutboxEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOutboxEntry(entry: SyncOutboxEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllOutboxEntries(entries: List<SyncOutboxEntity>)

    @Update
    suspend fun updateOutboxEntry(entry: SyncOutboxEntity)

    @Query("DELETE FROM sync_outbox WHERE id = :id")
    suspend fun deleteOutboxEntryById(id: String)

    @Query("DELETE FROM sync_outbox WHERE entityId = :entityId")
    suspend fun deleteOutboxEntriesForEntity(entityId: String)

    @Query("DELETE FROM sync_outbox WHERE status = 'ACKNOWLEDGED'")
    suspend fun clearAcknowledgedEntries()

    @Query("DELETE FROM sync_outbox")
    suspend fun deleteAllOutboxEntries()
}
