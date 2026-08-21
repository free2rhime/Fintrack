package com.example.data.repository

import com.example.data.model.HouseholdDto
import com.example.data.model.HouseholdInviteDto
import com.example.data.model.HouseholdMemberDto
import kotlinx.coroutines.flow.Flow

interface HouseholdRepository {
    suspend fun createHousehold(
        name: String
    ): Result<HouseholdDto>

    fun observeHousehold(
        householdId: String
    ): Flow<HouseholdDto?>

    fun observeHouseholdMembers(
        householdId: String
    ): Flow<List<HouseholdMemberDto>>

    suspend fun sendInvite(
        householdId: String,
        householdName: String,
        inviteeEmail: String
    ): Result<HouseholdInviteDto>

    fun observeIncomingInvites(
        userEmail: String
    ): Flow<List<HouseholdInviteDto>>

    suspend fun acceptInvite(
        inviteId: String
    ): Result<Unit>

    suspend fun declineInvite(
        inviteId: String
    ): Result<Unit>
}
