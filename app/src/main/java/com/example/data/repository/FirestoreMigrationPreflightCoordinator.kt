package com.example.data.repository

import com.example.data.db.FinTrackDatabase
import com.example.data.model.MigrationStateDto
import com.example.data.model.MigrationStateEntity
import com.example.data.util.CsvBackupManager
import kotlinx.coroutines.flow.first
import java.io.File
import java.util.UUID

enum class ConflictReason {
    ACTIVE_MIGRATION_IN_PROGRESS,
    EXISTING_REMOTE_DATA_DETECTED,
    INSUFFICIENT_PERMISSIONS,
    INVALID_STATE,
    BACKUP_INVALID
}

data class LocalMigrationCounts(
    val transactionsCount: Int,
    val categoriesCount: Int,
    val exchangeRatesCount: Int
) {
    val totalCount: Int get() = transactionsCount + categoriesCount + exchangeRatesCount
}

sealed class PreflightValidationResult {
    data class Ready(
        val householdId: String,
        val userUid: String,
        val memberInfo: HouseholdMemberInfo,
        val backupBundlePath: String?,
        val localCounts: LocalMigrationCounts
    ) : PreflightValidationResult()

    data class Conflict(
        val reason: ConflictReason,
        val details: String
    ) : PreflightValidationResult()

    data class Failure(
        val sanitizedError: String
    ) : PreflightValidationResult()
}

sealed class MigrationSessionCreationResult {
    data class Success(
        val migrationId: String,
        val entity: MigrationStateEntity,
        val dto: MigrationStateDto
    ) : MigrationSessionCreationResult()

    data class Failure(
        val sanitizedError: String
    ) : MigrationSessionCreationResult()
}

class FirestoreMigrationPreflightCoordinator(
    private val database: FinTrackDatabase,
    private val snapshotSource: FirestoreSnapshotSource,
    private val backupManager: CsvBackupManager = CsvBackupManager
) {
    private val verificationHelper = HouseholdVerificationHelper(snapshotSource)

    suspend fun validatePreflight(
        householdId: String?,
        userUid: String?,
        backupBundleDir: File? = null
    ): PreflightValidationResult {
        if (householdId.isNullOrBlank()) {
            return PreflightValidationResult.Failure("Invalid household ID: Household ID must not be blank")
        }
        if (userUid.isNullOrBlank()) {
            return PreflightValidationResult.Failure("Invalid user ID: User UID must not be blank")
        }

        return try {
            // 1. Authorization check (active OWNER or ADMIN)
            val authResult = verificationHelper.verifyHouseholdAdminOrOwner(householdId, userUid)
            val memberInfo = when (authResult) {
                is HouseholdVerificationResult.Success -> authResult.memberInfo
                is HouseholdVerificationResult.Failure -> {
                    return if (authResult.error.contains("Insufficient permissions") || authResult.error.contains("status")) {
                        PreflightValidationResult.Conflict(
                            ConflictReason.INSUFFICIENT_PERMISSIONS,
                            authResult.error
                        )
                    } else {
                        PreflightValidationResult.Failure(authResult.error)
                    }
                }
            }

            // 2. Backup validation check (if backup bundle is provided)
            var validatedBackupPath: String? = null
            if (backupBundleDir != null) {
                val backupResult = backupManager.validateMigrationBackupBundle(backupBundleDir)
                if (!backupResult.isValid) {
                    return PreflightValidationResult.Conflict(
                        ConflictReason.BACKUP_INVALID,
                        backupResult.errorMessage ?: "Backup bundle validation failed"
                    )
                }
                validatedBackupPath = backupBundleDir.absolutePath
            }

            // 3. Active migration check
            val activeSession = snapshotSource.getActiveMigrationSession(householdId)
            if (activeSession != null) {
                val existingMigrationId = activeSession["migrationId"] as? String ?: "unknown"
                val stage = activeSession["stage"] as? String ?: "IN_PROGRESS"
                return PreflightValidationResult.Conflict(
                    ConflictReason.ACTIVE_MIGRATION_IN_PROGRESS,
                    "An active migration session ($existingMigrationId) in stage '$stage' is currently in progress for household '$householdId'."
                )
            }

            // 4. Remote transaction conflict check
            val remoteTxCount = snapshotSource.getRemoteTransactionCount(householdId)
            if (remoteTxCount > 0) {
                return PreflightValidationResult.Conflict(
                    ConflictReason.EXISTING_REMOTE_DATA_DETECTED,
                    "Conflicting remote transaction data detected in household '$householdId' ($remoteTxCount documents found). Preflight aborted to prevent data collision."
                )
            }

            // 5. Remote category conflict check
            val remoteCatCount = snapshotSource.getRemoteCategoryCount(householdId)
            if (remoteCatCount > 0) {
                return PreflightValidationResult.Conflict(
                    ConflictReason.EXISTING_REMOTE_DATA_DETECTED,
                    "Conflicting remote category data detected in household '$householdId' ($remoteCatCount documents found). Preflight aborted to prevent data collision."
                )
            }

            // 6. Local entity counts calculation
            val localTxCount = database.transactionDao().getAllTransactionsList().size
            val localCatCount = database.categoryDao().getAllCategories().first().size
            val localRateCount = database.exchangeRateDao().getAllOfficialRates().size

            PreflightValidationResult.Ready(
                householdId = householdId,
                userUid = userUid,
                memberInfo = memberInfo,
                backupBundlePath = validatedBackupPath,
                localCounts = LocalMigrationCounts(
                    transactionsCount = localTxCount,
                    categoriesCount = localCatCount,
                    exchangeRatesCount = localRateCount
                )
            )
        } catch (e: Exception) {
            val sanitized = e.message?.lines()?.firstOrNull()
                ?.replace(Regex("at [a-zA-Z0-9_$.]+\\(.*\\)"), "")
                ?.take(100)
                ?: "Unexpected error during migration preflight validation"
            PreflightValidationResult.Failure("Preflight validation error: $sanitized")
        }
    }

    suspend fun createMigrationSession(
        preflightReady: PreflightValidationResult.Ready,
        migrationIdOverride: String? = null
    ): MigrationSessionCreationResult {
        return try {
            val migrationId = migrationIdOverride ?: ("mig_" + UUID.randomUUID().toString())
            val now = System.currentTimeMillis()
            val totalCount = preflightReady.localCounts.totalCount

            val entity = MigrationStateEntity(
                migrationId = migrationId,
                householdId = preflightReady.householdId,
                initiatedByUid = preflightReady.userUid,
                stage = "PREFLIGHT",
                processedCount = 0,
                totalCount = totalCount,
                currentPhase = "INITIALIZED",
                lastProcessedId = null,
                lastError = null,
                backupPath = preflightReady.backupBundlePath,
                createdAt = now,
                updatedAt = now
            )

            val dto = MigrationStateDto.fromEntity(entity)

            val remoteMap = mapOf<String, Any?>(
                "migrationId" to migrationId,
                "householdId" to preflightReady.householdId,
                "initiatedByUid" to preflightReady.userUid,
                "stage" to "PREFLIGHT",
                "processedCount" to 0,
                "totalCount" to totalCount,
                "currentPhase" to "INITIALIZED",
                "lastProcessedId" to null,
                "lastError" to null,
                "backupPath" to preflightReady.backupBundlePath,
                "createdAt" to now,
                "updatedAt" to now
            )

            // 1. Create remote Firestore document
            val remoteSuccess = snapshotSource.createMigrationStateDoc(
                householdId = preflightReady.householdId,
                migrationId = migrationId,
                data = remoteMap
            )

            if (!remoteSuccess) {
                return MigrationSessionCreationResult.Failure("Failed to initialize remote migrationState document for household '${preflightReady.householdId}'")
            }

            // 2. Persist local Room record
            database.migrationStateDao().insertMigrationState(entity)

            MigrationSessionCreationResult.Success(
                migrationId = migrationId,
                entity = entity,
                dto = dto
            )
        } catch (e: Exception) {
            val sanitized = e.message?.lines()?.firstOrNull()
                ?.replace(Regex("at [a-zA-Z0-9_$.]+\\(.*\\)"), "")
                ?.take(100)
                ?: "Unexpected error during migrationState creation"
            MigrationSessionCreationResult.Failure("Migration session initialization error: $sanitized")
        }
    }
}
