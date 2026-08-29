package com.example.data.repository

import android.util.Log
import com.example.data.model.HouseholdDto
import com.example.data.model.HouseholdInviteDto
import com.example.data.model.HouseholdMemberDto
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class FirestoreHouseholdRepository(
    private val authRepository: AuthRepository,
    private val snapshotSource: FirestoreSnapshotSource = DefaultFirestoreSnapshotSource(),
    private val firestoreSupplier: () -> FirebaseFirestore? = {
        try {
            FirebaseFirestore.getInstance()
        } catch (_: Exception) {
            null
        }
    }
) : HouseholdRepository {

    override suspend fun createHousehold(name: String): Result<HouseholdDto> = withContext(Dispatchers.IO) {
        val authState = authRepository.authState.value
        val signedInUser = authState as? AuthState.SignedIn
            ?: return@withContext Result.failure(
                IllegalStateException("User must be authenticated to create a household")
            )

        if (signedInUser.userUid.isBlank()) {
            return@withContext Result.failure(
                IllegalStateException("User UID cannot be blank")
            )
        }

        val trimmedName = name.trim()
        if (trimmedName.isEmpty()) {
            return@withContext Result.failure(
                IllegalArgumentException("Household name cannot be empty")
            )
        }

        if (trimmedName.length !in 2..50) {
            return@withContext Result.failure(
                IllegalArgumentException("Household name must be between 2 and 50 characters")
            )
        }

        // Check if an active household membership already exists
        val resolution = snapshotSource.resolveHouseholdId(signedInUser.userUid)
        if (resolution is HouseholdResolutionResult.Success) {
            return@withContext Result.failure(
                IllegalStateException("User is already an active member of household '${resolution.householdId}'")
            )
        }

        val firestore = firestoreSupplier()
            ?: return@withContext Result.failure(
                IllegalStateException("Firestore instance is not available")
            )

        return@withContext try {
            val householdDocRef = firestore.collection("households").document()
            val householdId = householdDocRef.id
            val memberDocRef = householdDocRef.collection("members").document(signedInUser.userUid)

            val now = System.currentTimeMillis()

            val householdDto = HouseholdDto(
                householdId = householdId,
                name = trimmedName,
                createdByUid = signedInUser.userUid,
                createdAt = now,
                updatedAt = now
            )

            val memberDto = HouseholdMemberDto(
                uid = signedInUser.userUid,
                email = signedInUser.email,
                displayName = signedInUser.displayName,
                role = "owner",
                status = "ACTIVE",
                joinedAt = now,
                invitedByUid = null
            )

            householdDocRef.set(householdDto.toMap()).await()
            memberDocRef.set(memberDto.toMap()).await()

            Result.success(householdDto)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun observeHousehold(householdId: String): Flow<HouseholdDto?> {
        if (householdId.isBlank()) {
            return flowOf(null)
        }

        val firestore = firestoreSupplier() ?: return flowOf(null)

        return callbackFlow {
            val docRef = firestore.collection("households").document(householdId)
            val registration = docRef.addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(null)
                    return@addSnapshotListener
                }

                if (snapshot == null || !snapshot.exists() || snapshot.data == null) {
                    trySend(null)
                    return@addSnapshotListener
                }

                try {
                    val household = HouseholdDto.fromMap(snapshot.data ?: emptyMap(), snapshot.id)
                    trySend(household)
                } catch (_: Exception) {
                    trySend(null)
                }
            }

            awaitClose {
                registration.remove()
            }
        }.flowOn(Dispatchers.IO)
    }

    override fun observeHouseholdMembers(householdId: String): Flow<List<HouseholdMemberDto>> {
        if (householdId.isBlank()) {
            return flowOf(emptyList())
        }

        val firestore = firestoreSupplier() ?: return flowOf(emptyList())

        return callbackFlow {
            val collectionRef = firestore.collection("households")
                .document(householdId)
                .collection("members")

            val registration = collectionRef.addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                if (snapshot == null || snapshot.isEmpty) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                try {
                    val members = snapshot.documents.mapNotNull { doc ->
                        doc.data?.let { data ->
                            HouseholdMemberDto.fromMap(data, doc.id)
                        }
                    }
                    trySend(members)
                } catch (_: Exception) {
                    trySend(emptyList())
                }
            }

            awaitClose {
                registration.remove()
            }
        }.flowOn(Dispatchers.IO)
    }

    override suspend fun sendInvite(
        householdId: String,
        householdName: String,
        inviteeEmail: String
    ): Result<HouseholdInviteDto> = withContext(Dispatchers.IO) {
        val authState = authRepository.authState.value
        val signedInUser = authState as? AuthState.SignedIn
            ?: return@withContext Result.failure(
                IllegalStateException("User must be authenticated to send an invitation")
            )

        if (householdId.isBlank()) {
            return@withContext Result.failure(
                IllegalArgumentException("Household ID cannot be blank")
            )
        }

        val normalizedInviteeEmail = inviteeEmail.trim().lowercase()
        if (normalizedInviteeEmail.isEmpty() || !normalizedInviteeEmail.contains("@")) {
            return@withContext Result.failure(
                IllegalArgumentException("Invalid invitee email address")
            )
        }

        val inviterEmail = signedInUser.email?.trim()?.lowercase()
        if (inviterEmail != null && inviterEmail == normalizedInviteeEmail) {
            return@withContext Result.failure(
                IllegalArgumentException("You cannot invite yourself to the household")
            )
        }

        val firestore = firestoreSupplier()
            ?: return@withContext Result.failure(
                IllegalStateException("Firestore instance is not available")
            )

        try {
            // Verify household exists
            val householdDoc = firestore.collection("households")
                .document(householdId)
                .get()
                .await()

            if (!householdDoc.exists()) {
                return@withContext Result.failure(
                    IllegalStateException("Household does not exist")
                )
            }

            // Check if inviter is OWNER of the household
            val inviterMemberDoc = firestore.collection("households")
                .document(householdId)
                .collection("members")
                .document(signedInUser.userUid)
                .get()
                .await()

            if (!inviterMemberDoc.exists()) {
                return@withContext Result.failure(
                    IllegalStateException("You are not a member of this household")
                )
            }

            val inviterRole = inviterMemberDoc.getString("role")?.lowercase()
            if (inviterRole != "owner") {
                return@withContext Result.failure(
                    IllegalStateException("Only the household owner can send invitations")
                )
            }

            // Check active members count and existing membership
            val membersSnapshot = firestore.collection("households")
                .document(householdId)
                .collection("members")
                .whereEqualTo("status", "ACTIVE")
                .get()
                .await()

            val activeMembers = membersSnapshot.documents

            // Max 2 active members capacity check
            if (activeMembers.size >= 2) {
                return@withContext Result.failure(
                    IllegalStateException("Household has reached maximum capacity (2 members)")
                )
            }

            // Existing member protection check
            val isAlreadyMember = activeMembers.any { doc ->
                val email = doc.getString("email")?.trim()?.lowercase()
                email == normalizedInviteeEmail
            }
            if (isAlreadyMember) {
                return@withContext Result.failure(
                    IllegalStateException("User is already an active member of this household")
                )
            }

            // Prevent duplicate active/pending invites for same household + email
            val now = System.currentTimeMillis()
            val existingInvitesSnapshot = firestore.collection("invitations")
                .whereEqualTo("householdId", householdId)
                .whereEqualTo("inviteeEmail", normalizedInviteeEmail)
                .whereEqualTo("status", "PENDING")
                .get()
                .await()

            val hasActivePendingInvite = existingInvitesSnapshot.documents.any { doc ->
                val expiresAt = doc.getLong("expiresAt") ?: 0L
                expiresAt > now
            }

            if (hasActivePendingInvite) {
                return@withContext Result.failure(
                    IllegalStateException("An active pending invitation already exists for this email")
                )
            }

            val inviteDocRef = firestore.collection("invitations").document()
            val inviteId = inviteDocRef.id
            val sevenDaysMillis = 7L * 24 * 60 * 60 * 1000
            val expiresAt = now + sevenDaysMillis

            val inviteDto = HouseholdInviteDto(
                inviteId = inviteId,
                householdId = householdId,
                householdName = householdName.trim().ifEmpty { "Household" },
                inviterUid = signedInUser.userUid,
                inviterEmail = signedInUser.email,
                inviterDisplayName = signedInUser.displayName,
                inviteeEmail = normalizedInviteeEmail,
                targetRole = "member",
                status = "PENDING",
                createdAt = now,
                expiresAt = expiresAt,
                respondedAt = null
            )

            inviteDocRef.set(inviteDto.toMap()).await()
            Result.success(inviteDto)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun observeIncomingInvites(userEmail: String): Flow<List<HouseholdInviteDto>> {
        val normalizedEmail = userEmail.trim().lowercase()
        if (normalizedEmail.isBlank()) {
            return flowOf(emptyList())
        }

        val firestore = firestoreSupplier() ?: return flowOf(emptyList())

        return callbackFlow {
            val query = firestore.collection("invitations")
                .whereEqualTo("inviteeEmail", normalizedEmail)
                .whereEqualTo("status", "PENDING")

            val registration = query.addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(
                        "SyncDiag",
                        "observeIncomingInvites error for email: $normalizedEmail (code=${error.code}): ${error.message}",
                        error
                    )
                    SyncDiagnosticsHolder.recordError(
                        userUid = authRepository.getCurrentUserUid(),
                        operation = "observeIncomingInvites",
                        throwable = error
                    )
                    close(error)
                    return@addSnapshotListener
                }

                if (snapshot == null || snapshot.isEmpty) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                val now = System.currentTimeMillis()
                val activeInvites = snapshot.documents.mapNotNull { doc ->
                    doc.data?.let { data ->
                        HouseholdInviteDto.fromMap(data, doc.id)
                    }
                }.filter { invite ->
                    (invite.expiresAt ?: 0L) > now
                }.sortedByDescending { it.createdAt ?: 0L }

                trySend(activeInvites)
            }

            awaitClose {
                registration.remove()
            }
        }.flowOn(Dispatchers.IO)
    }

    override suspend fun acceptInvite(inviteId: String): Result<Unit> = withContext(Dispatchers.IO) {
        val authState = authRepository.authState.value
        val signedInUser = authState as? AuthState.SignedIn
            ?: return@withContext Result.failure(
                IllegalStateException("User must be authenticated to accept an invitation")
            )

        if (inviteId.isBlank()) {
            return@withContext Result.failure(
                IllegalArgumentException("Invite ID cannot be blank")
            )
        }

        // Multi-household check: ensure user is not already an active member of any household
        val resolution = snapshotSource.resolveHouseholdId(signedInUser.userUid)
        if (resolution is HouseholdResolutionResult.Success) {
            return@withContext Result.failure(
                IllegalStateException("User is already an active member of household '${resolution.householdId}'")
            )
        }

        val firestore = firestoreSupplier()
            ?: return@withContext Result.failure(
                IllegalStateException("Firestore instance is not available")
            )

        try {
            val inviteDocRef = firestore.collection("invitations").document(inviteId)
            val inviteDoc = inviteDocRef.get().await()
            if (!inviteDoc.exists()) {
                return@withContext Result.failure(
                    IllegalStateException("Invitation not found")
                )
            }

            val inviteData = HouseholdInviteDto.fromMap(inviteDoc.data ?: emptyMap(), inviteDoc.id)
            val targetHouseholdId = inviteData.householdId
                ?: return@withContext Result.failure(
                    IllegalStateException("Invitation contains invalid household ID")
                )

            val status = inviteData.status ?: "PENDING"
            if (status != "PENDING") {
                return@withContext Result.failure(
                    IllegalStateException("Invitation is no longer pending (status: $status)")
                )
            }

            val now = System.currentTimeMillis()
            val expiresAt = inviteData.expiresAt ?: 0L
            if (expiresAt <= now) {
                return@withContext Result.failure(
                    IllegalStateException("Invitation has expired")
                )
            }

            val userEmail = signedInUser.email?.trim()?.lowercase()
            val inviteeEmail = inviteData.inviteeEmail?.trim()?.lowercase()
            if (userEmail == null || userEmail != inviteeEmail) {
                return@withContext Result.failure(
                    IllegalStateException("This invitation was not addressed to your email")
                )
            }

            firestore.runTransaction { transaction ->
                val inviteSnapshot = transaction.get(inviteDocRef)
                if (!inviteSnapshot.exists()) {
                    throw IllegalStateException("Invitation not found")
                }

                val invite = HouseholdInviteDto.fromMap(inviteSnapshot.data ?: emptyMap(), inviteSnapshot.id)
                val txStatus = invite.status ?: "PENDING"
                if (txStatus != "PENDING") {
                    throw IllegalStateException("Invitation is no longer pending (status: $txStatus)")
                }

                val txExpiresAt = invite.expiresAt ?: 0L
                if (txExpiresAt <= now) {
                    throw IllegalStateException("Invitation has expired")
                }

                val txInviteeEmail = invite.inviteeEmail?.trim()?.lowercase()
                if (userEmail != txInviteeEmail) {
                    throw IllegalStateException("This invitation was not addressed to your email")
                }

                val householdId = invite.householdId
                    ?: throw IllegalStateException("Invitation contains invalid household ID")

                val memberDocRef = firestore.collection("households")
                    .document(householdId)
                    .collection("members")
                    .document(signedInUser.userUid)

                val newMember = HouseholdMemberDto(
                    uid = signedInUser.userUid,
                    email = signedInUser.email,
                    displayName = signedInUser.displayName,
                    role = "member",
                    status = "ACTIVE",
                    joinedAt = now,
                    invitedByUid = invite.inviterUid,
                    inviteId = inviteId
                )

                // Update invitation status to ACCEPTED
                transaction.update(
                    inviteDocRef,
                    mapOf(
                        "status" to "ACCEPTED",
                        "respondedAt" to now
                    )
                )

                // Create member document
                transaction.set(memberDocRef, newMember.toMap())
            }.await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun declineInvite(inviteId: String): Result<Unit> = withContext(Dispatchers.IO) {
        val authState = authRepository.authState.value
        val signedInUser = authState as? AuthState.SignedIn
            ?: return@withContext Result.failure(
                IllegalStateException("User must be authenticated to decline an invitation")
            )

        if (inviteId.isBlank()) {
            return@withContext Result.failure(
                IllegalArgumentException("Invite ID cannot be blank")
            )
        }

        val firestore = firestoreSupplier()
            ?: return@withContext Result.failure(
                IllegalStateException("Firestore instance is not available")
            )

        try {
            val inviteDocRef = firestore.collection("invitations").document(inviteId)

            firestore.runTransaction { transaction ->
                val inviteSnapshot = transaction.get(inviteDocRef)
                if (!inviteSnapshot.exists()) {
                    throw IllegalStateException("Invitation not found")
                }

                val invite = HouseholdInviteDto.fromMap(inviteSnapshot.data ?: emptyMap(), inviteSnapshot.id)
                val status = invite.status ?: "PENDING"
                if (status != "PENDING") {
                    throw IllegalStateException("Invitation is no longer pending (status: $status)")
                }

                val userEmail = signedInUser.email?.trim()?.lowercase()
                val inviteeEmail = invite.inviteeEmail?.trim()?.lowercase()
                if (userEmail == null || userEmail != inviteeEmail) {
                    throw IllegalStateException("This invitation was not addressed to your email")
                }

                val now = System.currentTimeMillis()
                transaction.update(
                    inviteDocRef,
                    mapOf(
                        "status" to "DECLINED",
                        "respondedAt" to now
                    )
                )
            }.await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
