package com.example.data.repository

import android.util.Log
import com.example.data.dao.SyncOutboxDao
import com.example.data.db.FinTrackDatabase
import com.example.data.model.SyncOutboxEntity
import com.example.data.model.toFirestoreMap
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeout

class OutboundSyncEngine(
    private val database: FinTrackDatabase,
    private val syncOutboxDao: SyncOutboxDao,
    private val snapshotSource: FirestoreSnapshotSource,
    private val syncStatusProvider: () -> SyncStatus,
    private val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.IO),
    private val operationTimeoutMs: Long = 10_000L
) {
    companion object {
        private const val TAG = "OutboundSyncEngine"
        private const val BATCH_SIZE = 20
    }

    private val processMutex = Mutex()
    private var lifecycleJob: Job? = null
    private var isStarted = false

    @Synchronized
    fun start(syncStatusFlow: StateFlow<SyncStatus>? = null) {
        if (isStarted) return
        isStarted = true

        lifecycleJob = coroutineScope.launch {
            startupRecovery()
            if (isStarted) {
                processPendingQueue()
            }
        }
    }

    fun stop() {
        isStarted = false

        lifecycleJob?.cancel()
        lifecycleJob = null

        Log.d(TAG, "OutboundSyncEngine stopped")
    }

    fun notifyPending() {
        if (!isStarted) return

        lifecycleJob?.cancel()
        lifecycleJob = coroutineScope.launch {
            if (isStarted) {
                processPendingQueue()
            }
        }
    }

    suspend fun startupRecovery(): Int {
        val recovered = syncOutboxDao.resetInProgressToPending()
        if (recovered > 0) {
            Log.i(TAG, "Startup recovery reset $recovered in-progress outbox items to PENDING")
        }
        return recovered
    }

    suspend fun processPendingQueue(): Int {
        if (!processMutex.tryLock()) {
            // Already processing in another coroutine, the conflated channel ensures next run
            return 0
        }

        try {
            var processedTotal = 0
            while (true) {
                val currentStatus = syncStatusProvider()
                val activeHouseholdId = when (currentStatus) {
                    is SyncStatus.Synced -> currentStatus.householdId
                    else -> {
                        Log.d(TAG, "Queue processing halted - sync not in Synced state: $currentStatus")
                        break
                    }
                }

                if (activeHouseholdId.isNullOrBlank()) {
                    Log.w(TAG, "Queue processing halted - active householdId is null or blank")
                    break
                }

                val batch = syncOutboxDao.getPendingBatch(BATCH_SIZE)
                if (batch.isEmpty()) {
                    break
                }

                Log.d(TAG, "Fetched ${batch.size} pending outbox items for processing")

                for (item in batch) {
                    val statusBeforeItem = syncStatusProvider()
                    if (statusBeforeItem !is SyncStatus.Synced) {
                        Log.d(TAG, "SyncStatus changed during batch processing, pausing queue: $statusBeforeItem")
                        return processedTotal
                    }

                    val success = processSingleItem(item, activeHouseholdId)
                    if (success) {
                        processedTotal++
                    } else {
                        // Stop processing further items on retryable/fatal error to preserve strict FIFO order
                        Log.d(TAG, "Halting batch processing due to item error to preserve FIFO ordering")
                        return processedTotal
                    }
                }
            }
            return processedTotal
        } finally {
            processMutex.unlock()
        }
    }

    private suspend fun processSingleItem(item: SyncOutboxEntity, householdId: String): Boolean {
        syncOutboxDao.markInProgress(item.id)
        Log.d(TAG, "Processing outbox item: id=${item.id}, entity=${item.entityType}, entityId=${item.entityId}, op=${item.operation}")

        return try {
            withTimeout(operationTimeoutMs) {
                routeAndExecute(item, householdId)
            }
            syncOutboxDao.markSuccess(item.id)
            Log.d(TAG, "Successfully processed and marked SUCCESS for item ${item.id}")
            true
        } catch (e: CancellationException) {
            Log.d(TAG, "Processing cancelled for item ${item.id}")
            syncOutboxDao.markPending(item.id)
            throw e
        } catch (e: Exception) {
            handleItemFailure(item, e)
            false
        }
    }

    private suspend fun routeAndExecute(item: SyncOutboxEntity, householdId: String) {
        when (item.entityType.uppercase()) {
            "TRANSACTION" -> {
                when (item.operation.uppercase()) {
                    "UPSERT" -> {
                        val tx = database.transactionDao().getTransactionById(item.entityId)
                        if (tx != null) {
                            val payload = tx.toFirestoreMap(householdId)
                            snapshotSource.upsertTransaction(householdId, tx.id, payload)
                        } else {
                            // If local transaction was deleted before sync ran, issue soft delete
                            snapshotSource.deleteTransaction(householdId, item.entityId)
                        }
                    }
                    "DELETE" -> {
                        snapshotSource.deleteTransaction(householdId, item.entityId)
                    }
                    else -> throw IllegalArgumentException("Unknown TRANSACTION operation: ${item.operation}")
                }
            }
            "CATEGORY" -> {
                when (item.operation.uppercase()) {
                    "UPSERT" -> {
                        val cat = database.categoryDao().getCategoryById(item.entityId)
                        if (cat != null) {
                            if (cat.householdId != null && cat.householdId != householdId) {
                                Log.w(TAG, "Skipping outbound CATEGORY ${cat.id}: entity householdId (${cat.householdId}) does not match active householdId ($householdId)")
                                return
                            }
                            val targetHouseholdId = cat.householdId ?: householdId
                            val payload = cat.toFirestoreMap(targetHouseholdId)
                            snapshotSource.upsertCategory(targetHouseholdId, cat.id, payload)
                        } else {
                            // If local category was deleted before sync ran, issue soft delete
                            snapshotSource.deleteCategory(householdId, item.entityId)
                        }
                    }
                    "DELETE" -> {
                        snapshotSource.deleteCategory(householdId, item.entityId)
                    }
                    else -> throw IllegalArgumentException("Unknown CATEGORY operation: ${item.operation}")
                }
            }
            "EXCHANGE_RATE" -> {
                when (item.operation.uppercase()) {
                    "UPSERT" -> {
                        val rate = database.exchangeRateDao().getRateForDate(item.entityId)
                        if (rate != null) {
                            val payload = rate.toFirestoreMap(householdId)
                            snapshotSource.upsertExchangeRate(householdId, rate.date, payload)
                        } else {
                            snapshotSource.deleteExchangeRate(householdId, item.entityId)
                        }
                    }
                    "DELETE" -> {
                        snapshotSource.deleteExchangeRate(householdId, item.entityId)
                    }
                    else -> throw IllegalArgumentException("Unknown EXCHANGE_RATE operation: ${item.operation}")
                }
            }
            else -> throw IllegalArgumentException("Unknown entityType: ${item.entityType}")
        }
    }

    private suspend fun handleItemFailure(item: SyncOutboxEntity, e: Exception) {
        val errorMessage = e.message ?: e.javaClass.simpleName
        val isPermissionDenied = isPermissionDenied(e)
        val isUnauthenticated = isUnauthenticated(e)
        val isTimeout = isTimeout(e)
        val isUnavailable = isUnavailable(e)

        Log.w(TAG, "Item ${item.id} failed with error: $errorMessage (permDenied=$isPermissionDenied, unauth=$isUnauthenticated, timeout=$isTimeout, unavail=$isUnavailable)")

        when {
            isPermissionDenied -> {
                syncOutboxDao.markFailed(
                    id = item.id,
                    errorCode = "PERMISSION_DENIED",
                    errorMessage = errorMessage,
                    retryCount = item.retryCount + 1
                )
            }
            isUnauthenticated -> {
                syncOutboxDao.recordRetryFailure(
                    id = item.id,
                    errorCode = "UNAUTHENTICATED",
                    errorMessage = errorMessage
                )
            }
            isTimeout -> {
                syncOutboxDao.recordRetryFailure(
                    id = item.id,
                    errorCode = "TIMEOUT",
                    errorMessage = "Operation timed out: $errorMessage"
                )
            }
            isUnavailable -> {
                syncOutboxDao.recordRetryFailure(
                    id = item.id,
                    errorCode = "UNAVAILABLE",
                    errorMessage = errorMessage
                )
            }
            else -> {
                syncOutboxDao.recordRetryFailure(
                    id = item.id,
                    errorCode = "UNKNOWN_ERROR",
                    errorMessage = errorMessage
                )
            }
        }
    }

    private fun isPermissionDenied(e: Exception): Boolean {
        if (e is SecurityException) return true
        val msg = e.message ?: ""
        return msg.contains("PERMISSION_DENIED", ignoreCase = true) ||
                msg.contains("permission-denied", ignoreCase = true) ||
                msg.contains("permission denied", ignoreCase = true) ||
                msg.contains("insufficient permissions", ignoreCase = true)
    }

    private fun isUnauthenticated(e: Exception): Boolean {
        val msg = e.message ?: ""
        return msg.contains("UNAUTHENTICATED", ignoreCase = true) ||
                msg.contains("unauthenticated", ignoreCase = true) ||
                msg.contains("auth expired", ignoreCase = true)
    }

    private fun isTimeout(e: Exception): Boolean {
        val msg = e.message ?: ""
        return e is kotlinx.coroutines.TimeoutCancellationException ||
                msg.contains("DEADLINE_EXCEEDED", ignoreCase = true) ||
                msg.contains("timed out", ignoreCase = true)
    }

    private fun isUnavailable(e: Exception): Boolean {
        val msg = e.message ?: ""
        return msg.contains("UNAVAILABLE", ignoreCase = true) ||
                msg.contains("unavailable", ignoreCase = true) ||
                msg.contains("503", ignoreCase = true)
    }
}
