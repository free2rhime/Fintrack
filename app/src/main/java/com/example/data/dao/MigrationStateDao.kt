package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.MigrationStateEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MigrationStateDao {
    @Query("SELECT * FROM migration_state WHERE migrationId = :migrationId")
    suspend fun getMigrationStateById(migrationId: String): MigrationStateEntity?

    @Query("SELECT * FROM migration_state WHERE householdId = :householdId ORDER BY updatedAt DESC LIMIT 1")
    suspend fun getLatestMigrationStateForHousehold(householdId: String): MigrationStateEntity?

    @Query("SELECT * FROM migration_state ORDER BY updatedAt DESC")
    fun getAllMigrationStates(): Flow<List<MigrationStateEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMigrationState(state: MigrationStateEntity)

    @Update
    suspend fun updateMigrationState(state: MigrationStateEntity)

    @Query("DELETE FROM migration_state WHERE migrationId = :migrationId")
    suspend fun deleteMigrationStateById(migrationId: String)

    @Query("DELETE FROM migration_state")
    suspend fun deleteAllMigrationStates()
}
