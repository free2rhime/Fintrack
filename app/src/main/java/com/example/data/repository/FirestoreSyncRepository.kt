package com.example.data.repository

import androidx.room.withTransaction
import com.example.data.db.FinTrackDatabase
import com.example.data.model.CategoryDto
import com.example.data.model.CategoryEntity
import com.example.data.model.TransactionDto
import com.example.data.model.TransactionEntity
import com.example.data.model.toEntity
import com.google.firebase.firestore.FirebaseFirestore
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

    suspend fun resolveHouseholdId(userUid: String): String?

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

    override suspend fun resolveHouseholdId(userUid: String): String? {
        val firestore = firestoreSupplier() ?: return null
        return try {
            val querySnapshot = firestore.collectionGroup("members")
                .whereEqualTo("uid", userUid)
                .whereEqualTo("status", "ACTIVE")
                .get()
                .await()

            val firstDoc = querySnapshot.documents.firstOrNull()
            firstDoc?.reference?.parent?.parent?.id
        } catch (_: Exception) {
            null
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
        val firestore = firestoreSupplier() ?: return 0
        return try {
            val snapshot = firestore.collection("households")
                .document(householdId)
                .collection("transactions")
                .limit(100)
                .get()
                .await()
            snapshot.size()
        } catch (_: Exception) {
            0
        }
    }

    override suspend fun getRemoteCategoryCount(householdId: String): Int {
        val firestore = firestoreSupplier() ?: return 0
        return try {
            val snapshot = firestore.collection("households")
                .document(householdId)
                .collection("categories")
                .limit(100)
                .get()
                .await()
            snapshot.size()
        } catch (_: Exception) {
            0
        }
    }

    override suspend fun getRemoteExchangeRateCount(householdId: String): Int {
        val firestore = firestoreSupplier() ?: return 0
        return try {
            val snapshot = firestore.collection("households")
                .document(householdId)
                .collection("exchangeRates")
                .limit(100)
                .get()
                .await()
            snapshot.size()
        } catch (_: Exception) {
            0
        }
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
}

class FirestoreSyncRepository(
    private val database: FinTrackDatabase,
    private val snapshotSource: FirestoreSnapshotSource = DefaultFirestoreSnapshotSource(),
    private val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) {
    private val _syncStatusState = MutableStateFlow("Signed out")
    val syncStatusState: StateFlow<String> = _syncStatusState.asStateFlow()

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

    private var txListenerHandle: ListenerRegistrationHandle? = null
    private var catListenerHandle: ListenerRegistrationHandle? = null

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

    suspend fun startSync(userUid: String, requestedHouseholdId: String? = null): String {
        val resolvedHouseholdId = requestedHouseholdId
            ?: snapshotSource.resolveHouseholdId(userUid)
            ?: "household_$userUid"

        // Prevent duplicate listener registration
        if (isListening && activeUserUid == userUid && activeHouseholdId == resolvedHouseholdId) {
            return resolvedHouseholdId
        }

        // Remove old listeners before switching households or re-registering
        stopSync()

        activeUserUid = userUid
        activeHouseholdId = resolvedHouseholdId
        _syncStatusState.value = "Syncing..."

        txListenerHandle = snapshotSource.listenToTransactions(
            householdId = resolvedHouseholdId,
            onSnapshot = { docs ->
                if (!isSuppressed) {
                    coroutineScope.launch {
                        processTransactionSnapshot(docs)
                    }
                }
            },
            onError = { _ ->
                _syncStatusState.value = "Offline"
            }
        )

        catListenerHandle = snapshotSource.listenToCategories(
            householdId = resolvedHouseholdId,
            onSnapshot = { docs ->
                if (!isSuppressed) {
                    coroutineScope.launch {
                        processCategorySnapshot(docs)
                    }
                }
            },
            onError = { _ ->
                _syncStatusState.value = "Offline"
            }
        )

        isListening = true
        listenerCount = 2
        _syncStatusState.value = "Synced"

        return resolvedHouseholdId
    }

    fun stopSync() {
        txListenerHandle?.remove()
        txListenerHandle = null
        catListenerHandle?.remove()
        catListenerHandle = null

        activeUserUid = null
        activeHouseholdId = null
        isListening = false
        listenerCount = 0
        _syncStatusState.value = "Signed out"
    }

    suspend fun processTransactionSnapshot(docs: List<Pair<String, Map<String, Any?>>>) {
        if (isSuppressed) {
            return
        }

        database.withTransaction {
            val txDao = database.transactionDao()
            val toUpsert = mutableListOf<TransactionEntity>()

            for ((docId, map) in docs) {
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
            val catDao = database.categoryDao()
            val toUpsert = mutableListOf<CategoryEntity>()

            for ((docId, map) in docs) {
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
}
