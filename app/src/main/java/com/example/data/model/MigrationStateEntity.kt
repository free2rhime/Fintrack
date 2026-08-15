package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "migration_state")
data class MigrationStateEntity(
    @PrimaryKey
    val migrationId: String,
    val householdId: String,
    val initiatedByUid: String,
    val stage: String,
    val processedCount: Int = 0,
    val totalCount: Int = 0,
    val currentPhase: String = "",
    val lastProcessedId: String? = null,
    val lastError: String? = null,
    val backupPath: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
