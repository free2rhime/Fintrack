package com.example.data.repository

import android.util.Log
import com.example.data.dao.SyncOutboxDao
import com.example.data.db.FinTrackDatabase
import com.example.data.model.SyncOutboxEntity
import com.example.data.model.toFirestoreMap
import java.util.concurrent.atomic.AtomicBoolean
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
    private val operationTimeoutMs: Long = 10_000L,
    val baseRetryDelayMs: Long = DEFAULT_BASE_RETRY_DELAY_MS,
    val maxRetryDelayMs: Long = DEFAULT_MAX_RETRY_DELAY_MS,
    val maxRetries: Int = DEFAULT_MAX_RETRIES
) {
    companion object {
        private const val TAG = "OutboundSyncEngine"
        private const val BATCH_SIZE = 20
        const val DEFAULT_BASE_RETRY_DELAY_MS = 1_000L
        const val DEFAULT_MAX_RETRY_DELAY_MS = 30_000L
        const val DEFAULT_MAX_RETRIES = 5
    }

    private val processMutex = Mutex()
    private val isPendingSignal = AtomicBoolean(false)
    private var activeJob: Job? = null
    var isStarted: Boolean = false
        private set

    var onOutboxStateChanged: (() -> Unit)? = null

    fun calculateRetryDelay(
        retryCount: Int,
        baseDelayMs: Long = baseRetryDelayMs,
        maxDelayMs: Long = maxRetryDelayMs
    ): Long {
        if (retryCount <= 0 || baseDelayMs <= 0L) return 0L
        val exponent = (retryCount - 1).coerceAtMost(30)
        val multiplier = 1L shl exponent
        val delay = baseDelayMs * multiplier
        return if (delay < 0L || delay > maxDelayMs) maxDelayMs else delay
    }

    @Synchronized
    fun start(syncStatusFlow: StateFlow<SyncStatus>? = null): Job? {
        if (isStarted) {
            return notifyPending()
        }
        isStarted = true

        val job = coroutineScope.launch {
            startupRecovery()
            if (isStarted) {
                processPendingQueue()
            }
        }
        activeJob = job
        return job
    }

    fun stop() {
        isStarted = false
        activeJob?.cancel()
        activeJob = null
        Log.d(TAG, "OutboundSyncEngine stopped")
    }

    fun notifyPending(): Job? {
        onOutboxStateChanged?.invoke()
        if (!isStarted) return null
        isPendingSignal.set(true)
        val job = coroutineScope.launch {
            if (isStarted) {
                processPendingQueue()
            }
        }
        activeJob = job
        return job
    }

    suspend fun awaitIdle() {
        activeJob?.join()
        processMutex.withLock { }
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
            // Already processing in another coroutine; isPendingSignal ensures next pass
            return 0
        }

        try {
            var processedTotal = 0
            while (true) {
                isPendingSignal.set(false)

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
                    if (isPendingSignal.get()) {
                        continue
                    }
                    break
                }

                Log.d(TAG, "Fetched ${batch.size} pending outbox items for processing")

                for (item in batch) {
                    val statusBeforeItem = syncStatusProvider()
                    if (statusBeforeItem !is SyncStatus.Synced) {
                        Log.d(TAG, "SyncStatus changed during batch processing, pausing queue: $statusBeforeItem")
                        return processedTotal
                    }

                    if (item.retryCount > 0 && baseRetryDelayMs > 0L && item.lastAttemptAt != null) {
                        val elapsed = System.currentTimeMillis() - item.lastAttemptAt
                        val requiredDelay = calculateRetryDelay(item.retryCount)
                        val remaining = requiredDelay - elapsed
                        if (remaining > 0L) {
                            Log.d(TAG, "Applying backoff delay of ${remaining}ms for item ${item.id} (retryCount=${item.retryCount})")
                            kotlinx.coroutines.delay(remaining)
                        }
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
            onOutboxStateChanged?.invoke()
        }
    }

    private suspend fun processSingleItem(item: SyncOutboxEntity, householdId: String): Boolean {
        syncOutboxDao.markInProgress(item.id)
        onOutboxStateChanged?.invoke()
        Log.d(TAG, "Processing outbox item: id=${item.id}, entity=${item.entityType}, entityId=${item.entityId}, op=${item.operation}")

        return try {
            if (operationTimeoutMs > 0L) {
                withTimeout(operationTimeoutMs) {
                    routeAndExecute(item, householdId)
                }
            } else {
                routeAndExecute(item, householdId)
            }
            syncOutboxDao.markSuccess(item.id)
            Log.d(TAG, "Successfully processed and marked SUCCESS for item ${item.id}")
            onOutboxStateChanged?.invoke()
            true
        } catch (e: CancellationException) {
            Log.d(TAG, "Processing cancelled for item ${item.id}")
            syncOutboxDao.markPending(item.id)
            onOutboxStateChanged?.invoke()
            throw e
        } catch (e: Exception) {
            handleItemFailure(item, e)
            onOutboxStateChanged?.invoke()
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
                            if (tx.householdId != null && tx.householdId != householdId) {
                                Log.w(TAG, "Skipping outbound TRANSACTION ${tx.id}: entity householdId (${tx.householdId}) does not match active householdId ($householdId)")
                                return
                            }
                            val targetHouseholdId = tx.householdId ?: householdId
                            val payload = tx.toFirestoreMap(targetHouseholdId)
                            snapshotSource.upsertTransaction(targetHouseholdId, tx.id, payload)
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

        val errorCode = when {
            isPermissionDenied -> "PERMISSION_DENIED"
            isUnauthenticated -> "UNAUTHENTICATED"
            isTimeout -> "TIMEOUT"
            isUnavailable -> "UNAVAILABLE"
            else -> "UNKNOWN_ERROR"
        }

        val nextRetryCount = item.retryCount + 1

        if (isPermissionDenied) {
            syncOutboxDao.markFailed(
                id = item.id,
                errorCode = "PERMISSION_DENIED",
                errorMessage = errorMessage,
                retryCount = nextRetryCount
            )
        } else if (nextRetryCount >= maxRetries) {
            Log.w(TAG, "Item ${item.id} reached max retry threshold ($maxRetries), marking FAILED with errorCode=$errorCode")
            syncOutboxDao.markFailed(
                id = item.id,
                errorCode = errorCode,
                errorMessage = "Exceeded max retries ($maxRetries): $errorMessage",
                retryCount = nextRetryCount
            )
        } else {
            syncOutboxDao.recordRetryFailure(
                id = item.id,
                errorCode = errorCode,
                errorMessage = errorMessage
            )
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
