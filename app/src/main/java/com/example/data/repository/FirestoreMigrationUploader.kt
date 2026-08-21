package com.example.data.repository

import com.example.data.db.FinTrackDatabase
import com.example.data.model.CategoryEntity
import com.example.data.model.ExchangeRateEntity
import com.example.data.model.MigrationStateEntity
import com.example.data.model.TransactionEntity
import kotlinx.coroutines.flow.first

sealed class MigrationUploadResult {
    data class Success(
        val migrationId: String,
        val categoriesUploaded: Int,
        val ratesUploaded: Int,
        val transactionsUploaded: Int,
        val totalProcessed: Int
    ) : MigrationUploadResult()

    data class Failure(
        val migrationId: String,
        val stage: String,
        val sanitizedError: String
    ) : MigrationUploadResult()
}

class FirestoreMigrationUploader(
    private val database: FinTrackDatabase,
    private val snapshotSource: FirestoreSnapshotSource,
    private val syncRepository: FirestoreSyncRepository,
    private val batchSize: Int = 100
) {
    suspend fun executeMigration(
        householdId: String,
        userUid: String,
        migrationId: String,
        backupBundlePath: String? = null,
        onProgress: ((stage: String, processed: Int, total: Int) -> Unit)? = null
    ): MigrationUploadResult {
        // 1. Enable listener suppression before any uploads
        syncRepository.enableSuppression()

        var currentStage = "PREFLIGHT"
        var processedCount = 0

        val migrationResult = try {
            // ==========================================
            // EXECUTION REVALIDATION & STALE PROTECTION
            // ==========================================
            val resolvedHousehold = snapshotSource.resolveHouseholdId(userUid)
            if (resolvedHousehold !is HouseholdResolutionResult.Success || resolvedHousehold.householdId != householdId) {
                val errorMsg = "Execution revalidation failed: Active household mismatch or user no longer belongs to household '$householdId'"
                failMigration(migrationId, householdId, userUid, currentStage, errorMsg, 0, 0, backupBundlePath)
                return MigrationUploadResult.Failure(migrationId, currentStage, errorMsg)
            }

            val memberData = snapshotSource.getHouseholdMembership(householdId, userUid)
            val status = (memberData?.get("status") as? String)?.trim()?.uppercase()
            val role = (memberData?.get("role") as? String)?.trim()?.uppercase()
            if (status != "ACTIVE" || (role != "OWNER" && role != "ADMIN")) {
                val errorMsg = "Execution revalidation failed: User '$userUid' is no longer an active OWNER or ADMIN in household '$householdId'"
                failMigration(migrationId, householdId, userUid, currentStage, errorMsg, 0, 0, backupBundlePath)
                return MigrationUploadResult.Failure(migrationId, currentStage, errorMsg)
            }

            val activeSession = snapshotSource.getActiveMigrationSession(householdId)
            val activeMigrationId = activeSession?.get("migrationId") as? String
            if (activeSession != null && activeMigrationId != null && activeMigrationId != migrationId) {
                val errorMsg = "Execution revalidation failed: Migration lock held by another active session '$activeMigrationId'"
                failMigration(migrationId, householdId, userUid, currentStage, errorMsg, 0, 0, backupBundlePath)
                return MigrationUploadResult.Failure(migrationId, currentStage, errorMsg)
            }

            // Load local data
            val localCategories = database.categoryDao().getAllCategories().first()
            val localRates = database.exchangeRateDao().getAllOfficialRates()
            val localTransactions = database.transactionDao().getAllTransactionsList()
            val totalCount = localCategories.size + localRates.size + localTransactions.size

            // Update initial state if backup was created
            if (backupBundlePath != null) {
                currentStage = "BACKUP_CREATED"
                updateState(
                    migrationId = migrationId,
                    householdId = householdId,
                    userUid = userUid,
                    stage = "BACKUP_CREATED",
                    phase = "BACKUP_VALIDATED",
                    processedCount = 0,
                    totalCount = totalCount,
                    backupPath = backupBundlePath,
                    onProgress = onProgress
                )
            }

            // ==========================================
            // STAGE 1: UPLOAD CATEGORIES
            // ==========================================
            currentStage = "CATEGORIES_UPLOADING"
            updateState(
                migrationId = migrationId,
                householdId = householdId,
                userUid = userUid,
                stage = "CATEGORIES_UPLOADING",
                phase = "UPLOADING_CATEGORIES",
                processedCount = processedCount,
                totalCount = totalCount,
                backupPath = backupBundlePath,
                onProgress = onProgress
            )

            val categoryChunks = localCategories.chunked(batchSize)
            for (chunk in categoryChunks) {
                val payloads = chunk.map { cat ->
                    mapOf(
                        "categoryId" to cat.id,
                        "householdId" to householdId,
                        "name" to cat.name,
                        "type" to cat.type,
                        "subCategory" to cat.subCategory,
                        "createdByUid" to (cat.userId.takeIf { it.isNotBlank() && it != "local_user" } ?: userUid),
                        "createdAt" to cat.createdAt,
                        "updatedAt" to cat.updatedAt,
                        "isDeleted" to cat.isDeleted,
                        "migrationId" to migrationId
                    )
                }

                val uploadSuccess = snapshotSource.uploadCategoriesBatch(householdId, payloads)
                if (!uploadSuccess) {
                    val errorMsg = "Failed to upload category batch for household '$householdId'"
                    failMigration(migrationId, householdId, userUid, currentStage, errorMsg, processedCount, totalCount, backupBundlePath)
                    return MigrationUploadResult.Failure(migrationId, currentStage, errorMsg)
                }

                processedCount += chunk.size
                updateState(
                    migrationId = migrationId,
                    householdId = householdId,
                    userUid = userUid,
                    stage = "CATEGORIES_UPLOADING",
                    phase = "UPLOADING_CATEGORIES",
                    processedCount = processedCount,
                    totalCount = totalCount,
                    lastProcessedId = chunk.last().id,
                    backupPath = backupBundlePath,
                    onProgress = onProgress
                )
            }

            // ==========================================
            // STAGE 2: UPLOAD EXCHANGE RATES
            // ==========================================
            currentStage = "RATES_UPLOADING"
            updateState(
                migrationId = migrationId,
                householdId = householdId,
                userUid = userUid,
                stage = "RATES_UPLOADING",
                phase = "UPLOADING_EXCHANGE_RATES",
                processedCount = processedCount,
                totalCount = totalCount,
                backupPath = backupBundlePath,
                onProgress = onProgress
            )

            val rateChunks = localRates.chunked(batchSize)
            for (chunk in rateChunks) {
                val payloads = chunk.map { rate ->
                    val reqDate = rate.requestedDate.ifBlank { rate.date }
                    val effDate = rate.effectiveDate.ifBlank { rate.date }
                    mapOf(
                        "requestedDate" to reqDate,
                        "effectiveDate" to effDate,
                        "rate" to rate.rate,
                        "source" to rate.source,
                        "fetchedAt" to rate.fetchedAt,
                        "status" to rate.status,
                        "householdId" to householdId,
                        "migrationId" to migrationId,
                        "rates" to mapOf("EUR" to rate.rate)
                    )
                }

                val uploadSuccess = snapshotSource.uploadExchangeRatesBatch(householdId, payloads)
                if (!uploadSuccess) {
                    val errorMsg = "Failed to upload exchange rate batch for household '$householdId'"
                    failMigration(migrationId, householdId, userUid, currentStage, errorMsg, processedCount, totalCount, backupBundlePath)
                    return MigrationUploadResult.Failure(migrationId, currentStage, errorMsg)
                }

                processedCount += chunk.size
                updateState(
                    migrationId = migrationId,
                    householdId = householdId,
                    userUid = userUid,
                    stage = "RATES_UPLOADING",
                    phase = "UPLOADING_EXCHANGE_RATES",
                    processedCount = processedCount,
                    totalCount = totalCount,
                    lastProcessedId = chunk.last().date,
                    backupPath = backupBundlePath,
                    onProgress = onProgress
                )
            }

            // ==========================================
            // STAGE 3: UPLOAD TRANSACTIONS
            // ==========================================
            currentStage = "TRANSACTIONS_UPLOADING"
            updateState(
                migrationId = migrationId,
                householdId = householdId,
                userUid = userUid,
                stage = "TRANSACTIONS_UPLOADING",
                phase = "UPLOADING_TRANSACTIONS",
                processedCount = processedCount,
                totalCount = totalCount,
                backupPath = backupBundlePath,
                onProgress = onProgress
            )

            val transactionChunks = localTransactions.chunked(batchSize)
            for (chunk in transactionChunks) {
                val payloads = chunk.map { tx ->
                    mapOf(
                        "transactionId" to tx.id,
                        "householdId" to householdId,
                        "createdByUid" to (tx.userId.takeIf { it.isNotBlank() && it != "local_user" } ?: userUid),
                        "transactionDate" to tx.date,
                        "description" to tx.description,
                        "amountRon" to tx.amountRON,
                        "amountEur" to tx.amountEUR,
                        "exchangeRate" to tx.exchangeRate,
                        "exchangeRateDate" to tx.exchangeRateDate,
                        "type" to tx.type,
                        "account" to tx.account,
                        "category" to tx.category,
                        "subCategory" to tx.subCategory,
                        "destination" to tx.destination,
                        "createdAt" to tx.createdAt,
                        "updatedAt" to tx.updatedAt,
                        "exchangeRateSource" to tx.exchangeRateSource,
                        "conversionStatus" to tx.conversionStatus,
                        "categoryId" to tx.categoryId,
                        "subCategoryId" to tx.subCategoryId,
                        "isDeleted" to tx.isDeleted,
                        "migrationId" to migrationId,
                        "exchangeRateMetadata" to mapOf(
                            "source" to tx.exchangeRateSource,
                            "status" to tx.conversionStatus,
                            "rate" to tx.exchangeRate,
                            "effectiveDate" to tx.exchangeRateDate
                        )
                    )
                }

                val uploadSuccess = snapshotSource.uploadTransactionsBatch(householdId, payloads)
                if (!uploadSuccess) {
                    val errorMsg = "Failed to upload transaction batch for household '$householdId'"
                    failMigration(migrationId, householdId, userUid, currentStage, errorMsg, processedCount, totalCount, backupBundlePath)
                    return MigrationUploadResult.Failure(migrationId, currentStage, errorMsg)
                }

                processedCount += chunk.size
                updateState(
                    migrationId = migrationId,
                    householdId = householdId,
                    userUid = userUid,
                    stage = "TRANSACTIONS_UPLOADING",
                    phase = "UPLOADING_TRANSACTIONS",
                    processedCount = processedCount,
                    totalCount = totalCount,
                    lastProcessedId = chunk.last().id,
                    backupPath = backupBundlePath,
                    onProgress = onProgress
                )
            }

            // ==========================================
            // STAGE 4: VERIFICATION
            // ==========================================
            currentStage = "VERIFYING"
            updateState(
                migrationId = migrationId,
                householdId = householdId,
                userUid = userUid,
                stage = "VERIFYING",
                phase = "VERIFYING_REMOTE_COUNTS",
                processedCount = processedCount,
                totalCount = totalCount,
                backupPath = backupBundlePath,
                onProgress = onProgress
            )

            val remoteCatCount = snapshotSource.getRemoteCategoryCount(householdId)
            val remoteRateCount = snapshotSource.getRemoteExchangeRateCount(householdId)
            val remoteTxCount = snapshotSource.getRemoteTransactionCount(householdId)

            if (remoteCatCount != localCategories.size ||
                remoteRateCount != localRates.size ||
                remoteTxCount != localTransactions.size
            ) {
                val mismatchError = "Verification mismatch: Expected ${localCategories.size} categories, ${localRates.size} rates, ${localTransactions.size} transactions, but found $remoteCatCount categories, $remoteRateCount rates, $remoteTxCount transactions remotely"
                failMigration(migrationId, householdId, userUid, currentStage, mismatchError, processedCount, totalCount, backupBundlePath)
                return MigrationUploadResult.Failure(migrationId, currentStage, mismatchError)
            }

            // ==========================================
            // STAGE 5: COMPLETED
            // ==========================================
            currentStage = "COMPLETED"
            updateState(
                migrationId = migrationId,
                householdId = householdId,
                userUid = userUid,
                stage = "COMPLETED",
                phase = "COMPLETED",
                processedCount = totalCount,
                totalCount = totalCount,
                backupPath = backupBundlePath,
                onProgress = onProgress
            )

            MigrationUploadResult.Success(
                migrationId = migrationId,
                categoriesUploaded = localCategories.size,
                ratesUploaded = localRates.size,
                transactionsUploaded = localTransactions.size,
                totalProcessed = totalCount
            )
        } catch (e: Exception) {
            val sanitized = sanitizeErrorMessage(e.message)
            failMigration(migrationId, householdId, userUid, currentStage, sanitized, processedCount, 0, backupBundlePath)
            MigrationUploadResult.Failure(migrationId, currentStage, sanitized)
        } finally {
            // Clean up: Disable listener suppression
            syncRepository.disableSuppression()
        }

        return migrationResult
    }

    private suspend fun updateState(
        migrationId: String,
        householdId: String,
        userUid: String,
        stage: String,
        phase: String,
        processedCount: Int,
        totalCount: Int,
        lastProcessedId: String? = null,
        backupPath: String? = null,
        error: String? = null,
        onProgress: ((stage: String, processed: Int, total: Int) -> Unit)? = null
    ) {
        onProgress?.invoke(stage, processedCount, totalCount)
        val now = System.currentTimeMillis()
        val entity = MigrationStateEntity(
            migrationId = migrationId,
            householdId = householdId,
            initiatedByUid = userUid,
            stage = stage,
            processedCount = processedCount,
            totalCount = totalCount,
            currentPhase = phase,
            lastProcessedId = lastProcessedId,
            lastError = error,
            backupPath = backupPath,
            createdAt = now,
            updatedAt = now
        )

        // Update local Room entity
        database.migrationStateDao().insertMigrationState(entity)

        // Update remote document
        val remoteMap = mapOf<String, Any?>(
            "migrationId" to migrationId,
            "householdId" to householdId,
            "initiatedByUid" to userUid,
            "stage" to stage,
            "processedCount" to processedCount,
            "totalCount" to totalCount,
            "currentPhase" to phase,
            "lastProcessedId" to lastProcessedId,
            "lastError" to error,
            "backupPath" to backupPath,
            "updatedAt" to now
        )
        snapshotSource.updateMigrationStateDoc(householdId, migrationId, remoteMap)
    }

    private suspend fun failMigration(
        migrationId: String,
        householdId: String,
        userUid: String,
        stage: String,
        error: String,
        processedCount: Int,
        totalCount: Int,
        backupPath: String?
    ) {
        updateState(
            migrationId = migrationId,
            householdId = householdId,
            userUid = userUid,
            stage = "FAILED",
            phase = "FAILED_$stage",
            processedCount = processedCount,
            totalCount = totalCount,
            backupPath = backupPath,
            error = error
        )
    }

    private fun sanitizeErrorMessage(rawMessage: String?): String {
        if (rawMessage.isNullOrBlank()) return "Unknown migration error occurred"
        return rawMessage.lines().firstOrNull()
            ?.replace(Regex("at [a-zA-Z0-9_$.]+\\(.*\\)"), "")
            ?.take(120)
            ?: "Unknown migration error"
    }
}
