package com.example.data.repository

import android.util.Log
import androidx.room.withTransaction
import com.example.data.db.FinTrackDatabase
import com.example.data.model.CategoryDto
import com.example.data.model.CategoryEntity
import com.example.data.model.TransactionDto
import com.example.data.model.TransactionEntity
import com.example.data.model.toEntity
import com.google.firebase.firestore.AggregateSource
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

interface ListenerRegistrationHandle {
    fun remove()
}

sealed class SyncStatus {
    object SignedOut : SyncStatus() {
        override fun toString(): String = "Signed out"
    }
    object NoHousehold : SyncStatus() {
        override fun toString(): String = "No active household"
    }
    object Connecting : SyncStatus() {
        override fun toString(): String = "Syncing..."
    }
    data class Synced(val householdId: String? = null) : SyncStatus() {
        override fun toString(): String = "Synced"
    }
    object Offline : SyncStatus() {
        override fun toString(): String = "Offline"
    }
    object PermissionDenied : SyncStatus() {
        override fun toString(): String = "Permission denied"
    }
}

sealed class HouseholdResolutionResult {
    data class Success(val householdId: String) : HouseholdResolutionResult()
    object NoHousehold : HouseholdResolutionResult()
    object PermissionDenied : HouseholdResolutionResult()
    data class Failure(val error: String) : HouseholdResolutionResult()
}

enum class SyncConflictType {
    UPDATE_VS_UPDATE,
    DELETE_VS_UPDATE,
    UPDATE_VS_DELETE
}

data class SyncConflictEvent(
    val entityType: String,
    val entityId: String,
    val conflictType: SyncConflictType,
    val localOperation: String,
    val remoteIsDeleted: Boolean,
    val details: String = ""
)

interface FirestoreSnapshotSource {
    fun listenToTransactions(
        householdId: String,
        onSnapshot: (List<Pair<String, Map<String, Any?>>>) -> Unit,
        onError: (Exception) -> Unit
    ): ListenerRegistrationHandle

    fun listenToCategories(
        householdId: String,
        onSnapshot: (List<Pair<String, Map<String, Any?>>>) -> Unit,
        onError: (Exception) -> Unit
    ): ListenerRegistrationHandle

    suspend fun resolveHouseholdId(userUid: String): HouseholdResolutionResult

    suspend fun getHouseholdMembership(householdId: String, userUid: String): Map<String, Any?>? {
        return null
    }

    suspend fun getActiveMigrationSession(householdId: String): Map<String, Any?>? {
        return null
    }

    suspend fun getRemoteTransactionCount(householdId: String): Int {
        return 0
    }

    suspend fun getRemoteCategoryCount(householdId: String): Int {
        return 0
    }

    suspend fun getRemoteExchangeRateCount(householdId: String): Int {
        return 0
    }

    suspend fun createMigrationStateDoc(householdId: String, migrationId: String, data: Map<String, Any?>): Boolean {
        return false
    }

    suspend fun updateMigrationStateDoc(householdId: String, migrationId: String, updates: Map<String, Any?>): Boolean {
        return false
    }

    suspend fun uploadCategoriesBatch(householdId: String, categories: List<Map<String, Any?>>): Boolean {
        return false
    }

    suspend fun uploadExchangeRatesBatch(householdId: String, rates: List<Map<String, Any?>>): Boolean {
        return false
    }

    suspend fun uploadTransactionsBatch(householdId: String, transactions: List<Map<String, Any?>>): Boolean {
        return false
    }

    suspend fun upsertTransaction(householdId: String, transactionId: String, data: Map<String, Any?>) {}

    suspend fun deleteTransaction(householdId: String, transactionId: String) {}

    suspend fun upsertCategory(householdId: String, categoryId: String, data: Map<String, Any?>) {}

    suspend fun deleteCategory(householdId: String, categoryId: String) {}

    suspend fun upsertExchangeRate(householdId: String, exchangeRateId: String, data: Map<String, Any?>) {}

    suspend fun deleteExchangeRate(householdId: String, exchangeRateId: String) {}
}

data class HouseholdMemberInfo(
    val role: String,
    val status: String
)

sealed class HouseholdVerificationResult {
    data class Success(val memberInfo: HouseholdMemberInfo) : HouseholdVerificationResult()
    data class Failure(val error: String) : HouseholdVerificationResult()
}

class HouseholdVerificationHelper(
    private val snapshotSource: FirestoreSnapshotSource
) {
    suspend fun verifyHouseholdAdminOrOwner(householdId: String?, userUid: String?): HouseholdVerificationResult {
        if (householdId.isNullOrBlank()) {
            return HouseholdVerificationResult.Failure("Invalid household ID: Household ID must not be blank")
        }
        if (userUid.isNullOrBlank()) {
            return HouseholdVerificationResult.Failure("Invalid user ID: User UID must not be blank")
        }

        return try {
            val memberData = snapshotSource.getHouseholdMembership(householdId, userUid)
                ?: return HouseholdVerificationResult.Failure("Membership record not found for user in household '$householdId'")

            val rawStatus = memberData["status"] as? String
            val status = rawStatus?.trim()?.uppercase()
            if (status != "ACTIVE") {
                return HouseholdVerificationResult.Failure("User membership status is '${rawStatus ?: "UNKNOWN"}', but must be 'ACTIVE'")
            }

            val rawRole = memberData["role"] as? String
            val normalizedRole = rawRole?.trim()?.uppercase()
            if (normalizedRole != "OWNER" && normalizedRole != "ADMIN") {
                return HouseholdVerificationResult.Failure("Insufficient permissions: Role '${rawRole ?: "UNKNOWN"}' is not authorized. Must be OWNER or ADMIN")
            }

            HouseholdVerificationResult.Success(
                HouseholdMemberInfo(
                    role = normalizedRole,
                    status = "ACTIVE"
                )
            )
        } catch (e: Exception) {
            // Sanitized error string strictly omitting stack traces or raw class details
            val sanitized = e.message?.lines()?.firstOrNull()?.take(100) ?: "Unexpected verification failure"
            HouseholdVerificationResult.Failure("Household verification error: $sanitized")
        }
    }
}

class DefaultFirestoreSnapshotSource(
    private val firestoreSupplier: () -> FirebaseFirestore? = {
        try {
            FirebaseFirestore.getInstance()
        } catch (_: Exception) {
            null
        }
    }
) : FirestoreSnapshotSource {

    override fun listenToTransactions(
        householdId: String,
        onSnapshot: (List<Pair<String, Map<String, Any?>>>) -> Unit,
        onError: (Exception) -> Unit
    ): ListenerRegistrationHandle {
        val firestore = firestoreSupplier()
            ?: return object : ListenerRegistrationHandle { override fun remove() {} }

        val registration = firestore.collection("households")
            .document(householdId)
            .collection("transactions")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    onError(error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val docs = snapshot.documents.map { doc ->
                        Pair(doc.id, doc.data ?: emptyMap<String, Any?>())
                    }
                    onSnapshot(docs)
                }
            }

        return object : ListenerRegistrationHandle {
            override fun remove() {
                registration.remove()
            }
        }
    }

    override fun listenToCategories(
        householdId: String,
        onSnapshot: (List<Pair<String, Map<String, Any?>>>) -> Unit,
        onError: (Exception) -> Unit
    ): ListenerRegistrationHandle {
        val firestore = firestoreSupplier()
            ?: return object : ListenerRegistrationHandle { override fun remove() {} }

        val registration = firestore.collection("households")
            .document(householdId)
            .collection("categories")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    onError(error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val docs = snapshot.documents.map { doc ->
                        Pair(doc.id, doc.data ?: emptyMap<String, Any?>())
                    }
                    onSnapshot(docs)
                }
            }

        return object : ListenerRegistrationHandle {
            override fun remove() {
                registration.remove()
            }
        }
    }

    override suspend fun resolveHouseholdId(userUid: String): HouseholdResolutionResult {
        val firestore = firestoreSupplier()
            ?: return HouseholdResolutionResult.Failure("Firestore instance is not available")
        return try {
            val querySnapshot = firestore.collectionGroup("members")
                .whereEqualTo("uid", userUid)
                .whereEqualTo("status", "ACTIVE")
                .get()
                .await()

            val firstDoc = querySnapshot.documents.firstOrNull()
            val householdId = firstDoc?.reference?.parent?.parent?.id
            if (householdId != null) {
                HouseholdResolutionResult.Success(householdId)
            } else {
                HouseholdResolutionResult.NoHousehold
            }
        } catch (e: Exception) {
            val msg = e.message ?: ""
            if (msg.contains("PERMISSION_DENIED", ignoreCase = true) ||
                msg.contains("permission-denied", ignoreCase = true) ||
                msg.contains("permission denied", ignoreCase = true) ||
                e is SecurityException
            ) {
                HouseholdResolutionResult.PermissionDenied
            } else {
                val sanitized = e.message?.lines()?.firstOrNull()?.take(100) ?: "Resolution error"
                HouseholdResolutionResult.Failure(sanitized)
            }
        }
    }

    override suspend fun getHouseholdMembership(householdId: String, userUid: String): Map<String, Any?>? {
        val firestore = firestoreSupplier() ?: return null
        return try {
            val doc = firestore.collection("households")
                .document(householdId)
                .collection("members")
                .document(userUid)
                .get()
                .await()
            if (doc.exists()) doc.data else null
        } catch (_: Exception) {
            null
        }
    }

    override suspend fun getActiveMigrationSession(householdId: String): Map<String, Any?>? {
        val firestore = firestoreSupplier() ?: return null
        return try {
            val snapshot = firestore.collection("households")
                .document(householdId)
                .collection("migrationState")
                .whereIn("stage", listOf(
                    "PREFLIGHT",
                    "BACKUP_CREATED",
                    "CATEGORIES_UPLOADING",
                    "RATES_UPLOADING",
                    "TRANSACTIONS_UPLOADING",
                    "VERIFYING"
                ))
                .limit(1)
                .get()
                .await()
            val doc = snapshot.documents.firstOrNull()
            if (doc != null && doc.exists()) doc.data else null
        } catch (_: Exception) {
            null
        }
    }

    override suspend fun getRemoteTransactionCount(householdId: String): Int {
        val firestore = firestoreSupplier() ?: throw IllegalStateException("Firestore instance unavailable")
        val snapshot = firestore.collection("households")
            .document(householdId)
            .collection("transactions")
            .count()
            .get(AggregateSource.SERVER)
            .await()
        return snapshot.count.toInt()
    }

    override suspend fun getRemoteCategoryCount(householdId: String): Int {
        val firestore = firestoreSupplier() ?: throw IllegalStateException("Firestore instance unavailable")
        val snapshot = firestore.collection("households")
            .document(householdId)
            .collection("categories")
            .count()
            .get(AggregateSource.SERVER)
            .await()
        return snapshot.count.toInt()
    }

    override suspend fun getRemoteExchangeRateCount(householdId: String): Int {
        val firestore = firestoreSupplier() ?: throw IllegalStateException("Firestore instance unavailable")
        val snapshot = firestore.collection("households")
            .document(householdId)
            .collection("exchangeRates")
            .count()
            .get(AggregateSource.SERVER)
            .await()
        return snapshot.count.toInt()
    }

    override suspend fun createMigrationStateDoc(householdId: String, migrationId: String, data: Map<String, Any?>): Boolean {
        val firestore = firestoreSupplier() ?: return false
        return try {
            firestore.collection("households")
                .document(householdId)
                .collection("migrationState")
                .document(migrationId)
                .set(data)
                .await()
            true
        } catch (_: Exception) {
            false
        }
    }

    override suspend fun updateMigrationStateDoc(householdId: String, migrationId: String, updates: Map<String, Any?>): Boolean {
        val firestore = firestoreSupplier() ?: return false
        return try {
            firestore.collection("households")
                .document(householdId)
                .collection("migrationState")
                .document(migrationId)
                .update(updates)
                .await()
            true
        } catch (_: Exception) {
            false
        }
    }

    override suspend fun uploadCategoriesBatch(householdId: String, categories: List<Map<String, Any?>>): Boolean {
        val firestore = firestoreSupplier() ?: return false
        return try {
            val batch = firestore.batch()
            val collection = firestore.collection("households")
                .document(householdId)
                .collection("categories")
            for (cat in categories) {
                val catId = cat["categoryId"] as? String ?: continue
                val docRef = collection.document(catId)
                batch.set(docRef, cat)
            }
            batch.commit().await()
            true
        } catch (_: Exception) {
            false
        }
    }

    override suspend fun uploadExchangeRatesBatch(householdId: String, rates: List<Map<String, Any?>>): Boolean {
        val firestore = firestoreSupplier() ?: return false
        return try {
            val batch = firestore.batch()
            val collection = firestore.collection("households")
                .document(householdId)
                .collection("exchangeRates")
            for (rate in rates) {
                val requestedDate = rate["requestedDate"] as? String ?: continue
                val docRef = collection.document(requestedDate)
                batch.set(docRef, rate)
            }
            batch.commit().await()
            true
        } catch (_: Exception) {
            false
        }
    }

    override suspend fun uploadTransactionsBatch(householdId: String, transactions: List<Map<String, Any?>>): Boolean {
        val firestore = firestoreSupplier() ?: return false
        return try {
            val batch = firestore.batch()
            val collection = firestore.collection("households")
                .document(householdId)
                .collection("transactions")
            for (tx in transactions) {
                val txId = tx["transactionId"] as? String ?: continue
                val docRef = collection.document(txId)
                batch.set(docRef, tx)
            }
            batch.commit().await()
            true
        } catch (_: Exception) {
            false
        }
    }

    override suspend fun upsertTransaction(householdId: String, transactionId: String, data: Map<String, Any?>) {
        val firestore = firestoreSupplier() ?: throw IllegalStateException("Firestore instance unavailable")
        firestore.collection("households")
            .document(householdId)
            .collection("transactions")
            .document(transactionId)
            .set(data, SetOptions.merge())
            .await()
    }

    override suspend fun deleteTransaction(householdId: String, transactionId: String) {
        val firestore = firestoreSupplier() ?: throw IllegalStateException("Firestore instance unavailable")
        firestore.collection("households")
            .document(householdId)
            .collection("transactions")
            .document(transactionId)
            .set(
                mapOf(
                    "isDeleted" to true,
                    "updatedAt" to System.currentTimeMillis()
                ),
                SetOptions.merge()
            )
            .await()
    }

    override suspend fun upsertCategory(householdId: String, categoryId: String, data: Map<String, Any?>) {
        val firestore = firestoreSupplier() ?: throw IllegalStateException("Firestore instance unavailable")
        firestore.collection("households")
            .document(householdId)
            .collection("categories")
            .document(categoryId)
            .set(data, SetOptions.merge())
            .await()
    }

    override suspend fun deleteCategory(householdId: String, categoryId: String) {
        val firestore = firestoreSupplier() ?: throw IllegalStateException("Firestore instance unavailable")
        firestore.collection("households")
            .document(householdId)
            .collection("categories")
            .document(categoryId)
            .set(
                mapOf(
                    "isDeleted" to true,
                    "updatedAt" to System.currentTimeMillis()
                ),
                SetOptions.merge()
            )
            .await()
    }

    override suspend fun upsertExchangeRate(householdId: String, exchangeRateId: String, data: Map<String, Any?>) {
        val firestore = firestoreSupplier() ?: throw IllegalStateException("Firestore instance unavailable")
        firestore.collection("households")
            .document(householdId)
            .collection("exchangeRates")
            .document(exchangeRateId)
            .set(data, SetOptions.merge())
            .await()
    }

    override suspend fun deleteExchangeRate(householdId: String, exchangeRateId: String) {
        val firestore = firestoreSupplier() ?: throw IllegalStateException("Firestore instance unavailable")
        firestore.collection("households")
            .document(householdId)
            .collection("exchangeRates")
            .document(exchangeRateId)
            .delete()
            .await()
    }
}

class FirestoreSyncRepository(
    private val database: FinTrackDatabase,
    private val snapshotSource: FirestoreSnapshotSource = DefaultFirestoreSnapshotSource(),
    private val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) {
    companion object {
        private const val TAG = "FirestoreSyncRepository"
    }

    var onConflictDetected: ((SyncConflictEvent) -> Unit)? = null

    private val _syncStatusState = MutableStateFlow<SyncStatus>(SyncStatus.SignedOut)
    val syncStatusState: StateFlow<SyncStatus> = _syncStatusState.asStateFlow()

    val outboundSyncEngine: OutboundSyncEngine by lazy {
        OutboundSyncEngine(
            database = database,
            syncOutboxDao = database.syncOutboxDao(),
            snapshotSource = snapshotSource,
            syncStatusProvider = { _syncStatusState.value },
            coroutineScope = coroutineScope
        )
    }

    private val verificationHelper = HouseholdVerificationHelper(snapshotSource)

    var activeUserUid: String? = null
        private set

    var activeHouseholdId: String? = null
        private set

    var isListening: Boolean = false
        private set

    var listenerCount: Int = 0
        private set

    var isSuppressed: Boolean = false
        private set

    var hasReceivedTxSnapshot: Boolean = false
        private set

    var hasReceivedCatSnapshot: Boolean = false
        private set

    val isHandshakeComplete: Boolean
        get() = hasReceivedTxSnapshot && hasReceivedCatSnapshot

    private var txListenerHandle: ListenerRegistrationHandle? = null
    private var catListenerHandle: ListenerRegistrationHandle? = null
    private val syncJobs = mutableListOf<kotlinx.coroutines.Job>()

    fun enableSuppression() {
        isSuppressed = true
    }

    fun disableSuppression() {
        isSuppressed = false
    }

    fun setSuppression(enabled: Boolean) {
        isSuppressed = enabled
    }

    suspend fun verifyHouseholdAdminOrOwner(householdId: String?, userUid: String?): HouseholdVerificationResult {
        return verificationHelper.verifyHouseholdAdminOrOwner(householdId, userUid)
    }

    suspend fun resolveHousehold(userUid: String): HouseholdResolutionResult {
        return snapshotSource.resolveHouseholdId(userUid)
    }

    private fun isPermissionDenied(e: Exception): Boolean {
        if (e is SecurityException) return true
        val msg = e.message ?: ""
        if (msg.contains("PERMISSION_DENIED", ignoreCase = true) ||
            msg.contains("permission-denied", ignoreCase = true) ||
            msg.contains("permission denied", ignoreCase = true) ||
            msg.contains("insufficient permissions", ignoreCase = true)
        ) {
            return true
        }
        val causeMsg = e.cause?.message ?: ""
        if (causeMsg.contains("PERMISSION_DENIED", ignoreCase = true) ||
            causeMsg.contains("permission-denied", ignoreCase = true) ||
            causeMsg.contains("permission denied", ignoreCase = true) ||
            causeMsg.contains("insufficient permissions", ignoreCase = true)
        ) {
            return true
        }
        return false
    }

    private fun checkHandshakeAndUpdateState() {
        if (hasReceivedTxSnapshot && hasReceivedCatSnapshot) {
            if (_syncStatusState.value == SyncStatus.Connecting) {
                _syncStatusState.value = SyncStatus.Synced(activeHouseholdId)
                outboundSyncEngine.start(_syncStatusState)
            }
        }
    }

    suspend fun startSync(userUid: String, requestedHouseholdId: String? = null): String? {
        val resolvedHouseholdId = if (requestedHouseholdId != null) {
            requestedHouseholdId
        } else {
            when (val resolution = snapshotSource.resolveHouseholdId(userUid)) {
                is HouseholdResolutionResult.Success -> resolution.householdId
                is HouseholdResolutionResult.NoHousehold -> {
                    stopSync()
                    _syncStatusState.value = SyncStatus.NoHousehold
                    return null
                }
                is HouseholdResolutionResult.PermissionDenied -> {
                    stopSync()
                    _syncStatusState.value = SyncStatus.PermissionDenied
                    return null
                }
                is HouseholdResolutionResult.Failure -> {
                    stopSync()
                    _syncStatusState.value = SyncStatus.Offline
                    return null
                }
            }
        }

        if (resolvedHouseholdId.isBlank()) {
            stopSync()
            _syncStatusState.value = SyncStatus.NoHousehold
            return null
        }

        // Prevent duplicate listener registration if already actively syncing
        if (isListening && activeUserUid == userUid && activeHouseholdId == resolvedHouseholdId &&
            (_syncStatusState.value is SyncStatus.Synced || _syncStatusState.value == SyncStatus.Connecting)
        ) {
            return resolvedHouseholdId
        }

        // Remove old listeners before switching households or re-registering
        stopSync()

        activeUserUid = userUid
        activeHouseholdId = resolvedHouseholdId
        _syncStatusState.value = SyncStatus.Connecting
        hasReceivedTxSnapshot = false
        hasReceivedCatSnapshot = false

        txListenerHandle = snapshotSource.listenToTransactions(
            householdId = resolvedHouseholdId,
            onSnapshot = { docs ->
                hasReceivedTxSnapshot = true
                checkHandshakeAndUpdateState()
                if (!isSuppressed) {
                    syncJobs += coroutineScope.launch {
                        processTransactionSnapshot(docs)
                    }
                }
            },
            onError = { ex ->
                if (isPermissionDenied(ex)) {
                    _syncStatusState.value = SyncStatus.PermissionDenied
                } else {
                    _syncStatusState.value = SyncStatus.Offline
                }
            }
        )

        catListenerHandle = snapshotSource.listenToCategories(
            householdId = resolvedHouseholdId,
            onSnapshot = { docs ->
                hasReceivedCatSnapshot = true
                checkHandshakeAndUpdateState()
                if (!isSuppressed) {
                    syncJobs += coroutineScope.launch {
                        processCategorySnapshot(docs)
                    }
                }
            },
            onError = { ex ->
                if (isPermissionDenied(ex)) {
                    _syncStatusState.value = SyncStatus.PermissionDenied
                } else {
                    _syncStatusState.value = SyncStatus.Offline
                }
            }
        )

        isListening = true
        listenerCount = 2

        return resolvedHouseholdId
    }

    fun stopSync() {
        syncJobs.forEach { it.cancel() }
        syncJobs.clear()

        txListenerHandle?.remove()
        txListenerHandle = null
        catListenerHandle?.remove()
        catListenerHandle = null

        outboundSyncEngine.stop()

        activeUserUid = null
        activeHouseholdId = null
        isListening = false
        listenerCount = 0
        hasReceivedTxSnapshot = false
        hasReceivedCatSnapshot = false
        _syncStatusState.value = SyncStatus.SignedOut
    }

    suspend fun processTransactionSnapshot(docs: List<Pair<String, Map<String, Any?>>>) {
        if (isSuppressed) {
            return
        }

        database.withTransaction {
            val activeOutboxIds = database.syncOutboxDao().getActiveEntityIdsByType("TRANSACTION").toSet()
            val txDao = database.transactionDao()
            val toUpsert = mutableListOf<TransactionEntity>()

            for ((docId, map) in docs) {
                if (activeOutboxIds.contains(docId)) {
                    // Shield active local mutation from remote snapshot overwrite
                    val activeEntry = database.syncOutboxDao().getActiveEntry("TRANSACTION", docId)
                    val localTx = txDao.getTransactionById(docId)
                    val dto = TransactionDto.fromMap(map, docId)
                    val conflictEvent = detectTransactionConflict(
                        docId = docId,
                        activeOperation = activeEntry?.operation ?: "UPSERT",
                        localEntity = localTx,
                        remoteDto = dto
                    )
                    if (conflictEvent != null) {
                        Log.w(
                            TAG,
                            "Sync conflict detected [${conflictEvent.conflictType}] for TRANSACTION '$docId'. " +
                            "Resolution: LOCAL_OUTBOX_PRECEDENCE (local active mutation preserved)."
                        )
                        onConflictDetected?.invoke(conflictEvent)
                    }
                    continue
                }
                val dto = TransactionDto.fromMap(map, docId)
                val entity = dto.toEntity(docId)
                if (entity == null) {
                    // Reject malformed remote document without modifying Room
                    continue
                }
                if (entity.isDeleted) {
                    txDao.deleteTransactionById(entity.id)
                } else {
                    toUpsert.add(entity)
                }
            }

            if (toUpsert.isNotEmpty()) {
                txDao.insertAllTransactions(toUpsert)
            }
        }
    }

    suspend fun processCategorySnapshot(docs: List<Pair<String, Map<String, Any?>>>) {
        if (isSuppressed) {
            return
        }

        database.withTransaction {
            val activeOutboxIds = database.syncOutboxDao().getActiveEntityIdsByType("CATEGORY").toSet()
            val catDao = database.categoryDao()
            val toUpsert = mutableListOf<CategoryEntity>()

            for ((docId, map) in docs) {
                if (activeOutboxIds.contains(docId)) {
                    // Shield active local mutation from remote snapshot overwrite
                    val activeEntry = database.syncOutboxDao().getActiveEntry("CATEGORY", docId)
                    val localCat = catDao.getCategoryById(docId)
                    val dto = CategoryDto.fromMap(map, docId)
                    val conflictEvent = detectCategoryConflict(
                        docId = docId,
                        activeOperation = activeEntry?.operation ?: "UPSERT",
                        localEntity = localCat,
                        remoteDto = dto
                    )
                    if (conflictEvent != null) {
                        Log.w(
                            TAG,
                            "Sync conflict detected [${conflictEvent.conflictType}] for CATEGORY '$docId'. " +
                            "Resolution: LOCAL_OUTBOX_PRECEDENCE (local active mutation preserved)."
                        )
                        onConflictDetected?.invoke(conflictEvent)
                    }
                    continue
                }
                val dto = CategoryDto.fromMap(map, docId)
                val entity = dto.toEntity(docId)
                if (entity == null) {
                    // Reject malformed remote category without modifying Room
                    continue
                }
                if (entity.isDeleted) {
                    catDao.deleteCategoryById(entity.id)
                } else {
                    toUpsert.add(entity)
                }
            }

            if (toUpsert.isNotEmpty()) {
                catDao.insertAllCategories(toUpsert)
            }
        }
    }

    private fun detectTransactionConflict(
        docId: String,
        activeOperation: String,
        localEntity: TransactionEntity?,
        remoteDto: TransactionDto
    ): SyncConflictEvent? {
        val remoteIsDeleted = remoteDto.isDeleted == true
        val isLocalDelete = activeOperation.equals("DELETE", ignoreCase = true) || (localEntity == null)
        val isLocalUpsert = activeOperation.equals("UPSERT", ignoreCase = true) || (!isLocalDelete && localEntity != null)

        val conflictType: SyncConflictType? = when {
            isLocalDelete && !remoteIsDeleted -> SyncConflictType.DELETE_VS_UPDATE
            isLocalUpsert && remoteIsDeleted -> SyncConflictType.UPDATE_VS_DELETE
            isLocalUpsert && !remoteIsDeleted -> {
                if (localEntity == null) {
                    SyncConflictType.UPDATE_VS_UPDATE
                } else {
                    val differs = localEntity.amountRON != remoteDto.amountRon ||
                            localEntity.amountEUR != remoteDto.amountEur ||
                            localEntity.exchangeRate != remoteDto.exchangeRate ||
                            localEntity.date != remoteDto.transactionDate ||
                            localEntity.description != remoteDto.description ||
                            localEntity.category != remoteDto.category ||
                            localEntity.subCategory != (remoteDto.subCategory ?: "") ||
                            localEntity.type != remoteDto.type ||
                            localEntity.account != remoteDto.account

                    if (differs) SyncConflictType.UPDATE_VS_UPDATE else null
                }
            }
            else -> null
        }

        return conflictType?.let { type ->
            SyncConflictEvent(
                entityType = "TRANSACTION",
                entityId = docId,
                conflictType = type,
                localOperation = activeOperation,
                remoteIsDeleted = remoteIsDeleted,
                details = "Remote snapshot differs from local pending mutation"
            )
        }
    }

    private fun detectCategoryConflict(
        docId: String,
        activeOperation: String,
        localEntity: CategoryEntity?,
        remoteDto: CategoryDto
    ): SyncConflictEvent? {
        val remoteIsDeleted = remoteDto.isDeleted == true
        val isLocalDelete = activeOperation.equals("DELETE", ignoreCase = true) || (localEntity == null)
        val isLocalUpsert = activeOperation.equals("UPSERT", ignoreCase = true) || (!isLocalDelete && localEntity != null)

        val conflictType: SyncConflictType? = when {
            isLocalDelete && !remoteIsDeleted -> SyncConflictType.DELETE_VS_UPDATE
            isLocalUpsert && remoteIsDeleted -> SyncConflictType.UPDATE_VS_DELETE
            isLocalUpsert && !remoteIsDeleted -> {
                if (localEntity == null) {
                    SyncConflictType.UPDATE_VS_UPDATE
                } else {
                    val differs = localEntity.name != remoteDto.name ||
                            localEntity.type != remoteDto.type ||
                            localEntity.subCategory != (remoteDto.subCategory ?: "")

                    if (differs) SyncConflictType.UPDATE_VS_UPDATE else null
                }
            }
            else -> null
        }

        return conflictType?.let { type ->
            SyncConflictEvent(
                entityType = "CATEGORY",
                entityId = docId,
                conflictType = type,
                localOperation = activeOperation,
                remoteIsDeleted = remoteIsDeleted,
                details = "Remote snapshot differs from local pending mutation"
            )
        }
    }
}
