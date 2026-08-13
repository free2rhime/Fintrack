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
}

class FirestoreSyncRepository(
    private val database: FinTrackDatabase,
    private val snapshotSource: FirestoreSnapshotSource = DefaultFirestoreSnapshotSource(),
    private val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) {
    private val _syncStatusState = MutableStateFlow("Signed out")
    val syncStatusState: StateFlow<String> = _syncStatusState.asStateFlow()

    var activeUserUid: String? = null
        private set

    var activeHouseholdId: String? = null
        private set

    var isListening: Boolean = false
        private set

    var listenerCount: Int = 0
        private set

    private var txListenerHandle: ListenerRegistrationHandle? = null
    private var catListenerHandle: ListenerRegistrationHandle? = null

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
                coroutineScope.launch {
                    processTransactionSnapshot(docs)
                }
            },
            onError = { _ ->
                _syncStatusState.value = "Offline"
            }
        )

        catListenerHandle = snapshotSource.listenToCategories(
            householdId = resolvedHouseholdId,
            onSnapshot = { docs ->
                coroutineScope.launch {
                    processCategorySnapshot(docs)
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