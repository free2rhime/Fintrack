package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "sync_outbox")
data class SyncOutboxEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val entityType: String, // "TRANSACTION", "CATEGORY"
    val entityId: String,
    val operation: String, // "UPSERT", "DELETE"
    val status: String = "PENDING", // "PENDING", "IN_PROGRESS", "ACKNOWLEDGED", "CONFLICT"
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val retryCount: Int = 0,
    val lastAttemptAt: Long? = null,
    val errorCode: String? = null,
    val errorMessage: String? = null
)
