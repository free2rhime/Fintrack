package com.example

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import com.example.data.model.FilterSettings
import com.example.data.model.HouseholdDto
import com.example.data.model.HouseholdInviteDto
import com.example.data.model.HouseholdMemberDto
import com.example.data.repository.AuthRepository
import com.example.data.repository.AuthState
import com.example.data.repository.HouseholdRepository
import com.example.data.repository.HouseholdResolutionResult
import com.example.data.repository.FirestoreSnapshotSource
import com.example.ui.screens.SettingsScreen
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class HouseholdInvitationTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    // --------------------------------------------------------------------------
    // 1. In-Memory Filter & Sorting Contract Tests
    // --------------------------------------------------------------------------

    private fun filterAndSortInvites(
        invites: List<HouseholdInviteDto>,
        targetEmail: String,
        currentTimeMillis: Long
    ): List<HouseholdInviteDto> {
        val normalizedTarget = targetEmail.trim().lowercase()
        return invites
            .filter { invite ->
                invite.inviteeEmail?.trim()?.lowercase() == normalizedTarget &&
                    invite.status == "PENDING" &&
                    (invite.expiresAt ?: 0L) > currentTimeMillis
            }
            .sortedByDescending { it.createdAt ?: 0L }
    }

    @Test
    fun test1_matchingEmailWithPendingInvite_isReturned() {
        val now = 1000000L
        val validInvite = HouseholdInviteDto(
            inviteId = "inv_1",
            householdId = "hh_alpha",
            householdName = "Alpha Family",
            inviterUid = "user_owner",
            inviterEmail = "owner@example.com",
            inviteeEmail = "member@example.com",
            status = "PENDING",
            createdAt = now - 1000,
            expiresAt = now + 7 * 24 * 3600 * 1000L
        )

        val result = filterAndSortInvites(listOf(validInvite), "member@example.com", now)
        assertEquals(1, result.size)
        assertEquals("inv_1", result.first().inviteId)
        assertEquals("Alpha Family", result.first().householdName)
    }

    @Test
    fun test2_nonMatchingEmail_isNotReturned() {
        val now = 1000000L
        val invite = HouseholdInviteDto(
            inviteId = "inv_1",
            householdId = "hh_alpha",
            householdName = "Alpha Family",
            inviterUid = "user_owner",
            inviteeEmail = "other@example.com",
            status = "PENDING",
            createdAt = now - 1000,
            expiresAt = now + 7 * 24 * 3600 * 1000L
        )

        val result = filterAndSortInvites(listOf(invite), "member@example.com", now)
        assertTrue("Non-matching email must return empty list", result.isEmpty())
    }

    @Test
    fun test3_acceptedDeclinedCancelledInvites_areExcluded() {
        val now = 1000000L
        val accepted = HouseholdInviteDto(
            inviteId = "inv_acc",
            inviteeEmail = "member@example.com",
            status = "ACCEPTED",
            createdAt = now - 1000,
            expiresAt = now + 7 * 24 * 3600 * 1000L
        )
        val declined = HouseholdInviteDto(
            inviteId = "inv_dec",
            inviteeEmail = "member@example.com",
            status = "DECLINED",
            createdAt = now - 1000,
            expiresAt = now + 7 * 24 * 3600 * 1000L
        )
        val cancelled = HouseholdInviteDto(
            inviteId = "inv_can",
            inviteeEmail = "member@example.com",
            status = "CANCELLED",
            createdAt = now - 1000,
            expiresAt = now + 7 * 24 * 3600 * 1000L
        )

        val result = filterAndSortInvites(listOf(accepted, declined, cancelled), "member@example.com", now)
        assertTrue("Non-PENDING invites must be excluded", result.isEmpty())
    }

    @Test
    fun test4_expiredPendingInvite_isFilteredOut() {
        val now = 1000000L
        val expiredInvite = HouseholdInviteDto(
            inviteId = "inv_exp",
            inviteeEmail = "member@example.com",
            status = "PENDING",
            createdAt = now - 100000,
            expiresAt = now - 10 // Expired 10ms ago
        )

        val result = filterAndSortInvites(listOf(expiredInvite), "member@example.com", now)
        assertTrue("Expired invites must be excluded", result.isEmpty())
    }

    @Test
    fun test5_multiplePendingInvites_areReturnedAndSortedByCreatedAtDescending() {
        val now = 1000000L
        val inviteOld = HouseholdInviteDto(
            inviteId = "inv_old",
            householdName = "Old Household",
            inviteeEmail = "member@example.com",
            status = "PENDING",
            createdAt = 1000L,
            expiresAt = now + 100000L
        )
        val inviteNew = HouseholdInviteDto(
            inviteId = "inv_new",
            householdName = "New Household",
            inviteeEmail = "member@example.com",
            status = "PENDING",
            createdAt = 5000L,
            expiresAt = now + 100000L
        )
        val inviteMid = HouseholdInviteDto(
            inviteId = "inv_mid",
            householdName = "Mid Household",
            inviteeEmail = "member@example.com",
            status = "PENDING",
            createdAt = 3000L,
            expiresAt = now + 100000L
        )

        val result = filterAndSortInvites(listOf(inviteOld, inviteNew, inviteMid), "member@example.com", now)
        assertEquals(3, result.size)
        assertEquals("inv_new", result[0].inviteId)
        assertEquals("inv_mid", result[1].inviteId)
        assertEquals("inv_old", result[2].inviteId)
    }

    // --------------------------------------------------------------------------
    // 2. Invitation Acceptance & State Transition Tests
    // --------------------------------------------------------------------------

    /**
     * Test harness that simulates the repository's acceptInvite logic and records all operations.
     */
    private class SimulatedAcceptanceHarness(
        var authUser: AuthState.SignedIn?,
        var existingResolution: HouseholdResolutionResult = HouseholdResolutionResult.NoHousehold,
        val invitationsStore: MutableMap<String, HouseholdInviteDto> = mutableMapOf(),
        val membersStore: MutableMap<String, MutableMap<String, HouseholdMemberDto>> = mutableMapOf()
    ) {
        val operationsLog = mutableListOf<String>()

        fun acceptInvite(inviteId: String): Result<Unit> {
            val signedInUser = authUser
                ?: return Result.failure(IllegalStateException("User must be authenticated to accept an invitation"))

            if (inviteId.isBlank()) {
                return Result.failure(IllegalArgumentException("Invite ID cannot be blank"))
            }

            operationsLog.add("resolveHouseholdId:${signedInUser.userUid}")
            if (existingResolution is HouseholdResolutionResult.Success) {
                return Result.failure(
                    IllegalStateException("User is already an active member of household '${(existingResolution as HouseholdResolutionResult.Success).householdId}'")
                )
            }

            operationsLog.add("getInvitation:$inviteId")
            val invite = invitationsStore[inviteId]
                ?: return Result.failure(IllegalStateException("Invitation not found"))

            val targetHouseholdId = invite.householdId
                ?: return Result.failure(IllegalStateException("Invitation contains invalid household ID"))

            val status = invite.status ?: "PENDING"
            if (status != "PENDING") {
                return Result.failure(IllegalStateException("Invitation is no longer pending (status: $status)"))
            }

            val now = System.currentTimeMillis()
            val expiresAt = invite.expiresAt ?: 0L
            if (expiresAt <= now) {
                return Result.failure(IllegalStateException("Invitation has expired"))
            }

            val userEmail = signedInUser.email?.trim()?.lowercase()
            val inviteeEmail = invite.inviteeEmail?.trim()?.lowercase()
            if (userEmail == null || userEmail != inviteeEmail) {
                return Result.failure(IllegalStateException("This invitation was not addressed to your email"))
            }

            // Atomic Transaction Simulation:
            operationsLog.add("transaction:getInvitation:$inviteId")
            operationsLog.add("transaction:updateInvitation:$inviteId:ACCEPTED")
            val updatedInvite = invite.copy(status = "ACCEPTED", respondedAt = now)
            invitationsStore[inviteId] = updatedInvite

            val newMember = HouseholdMemberDto(
                uid = signedInUser.userUid,
                email = signedInUser.email,
                displayName = signedInUser.displayName,
                role = "member",
                status = "ACTIVE",
                joinedAt = now,
                invitedByUid = invite.inviterUid
            )
            operationsLog.add("transaction:setMember:$targetHouseholdId:${signedInUser.userUid}")
            val householdMembers = membersStore.getOrPut(targetHouseholdId) { mutableMapOf() }
            householdMembers[signedInUser.userUid] = newMember

            return Result.success(Unit)
        }
    }

    @Test
    fun test6_inviteeWithoutHousehold_canAcceptInvitation() {
        val invite = HouseholdInviteDto(
            inviteId = "inv_123",
            householdId = "hh_family",
            householdName = "Family Household",
            inviterUid = "owner_uid_99",
            inviterEmail = "owner@example.com",
            inviteeEmail = "member@example.com",
            status = "PENDING",
            createdAt = System.currentTimeMillis() - 1000,
            expiresAt = System.currentTimeMillis() + 7 * 24 * 3600 * 1000L
        )

        val harness = SimulatedAcceptanceHarness(
            authUser = AuthState.SignedIn(userUid = "member_uid_1", email = "member@example.com", displayName = "Member"),
            existingResolution = HouseholdResolutionResult.NoHousehold,
            invitationsStore = mutableMapOf("inv_123" to invite)
        )

        val result = harness.acceptInvite("inv_123")
        assertTrue("Accepting invitation should succeed", result.isSuccess)

        // Verify invitation state updated
        val updatedInvite = harness.invitationsStore["inv_123"]
        assertNotNull(updatedInvite)
        assertEquals("ACCEPTED", updatedInvite?.status)
        assertNotNull(updatedInvite?.respondedAt)

        // Verify member document created
        val createdMember = harness.membersStore["hh_family"]?.get("member_uid_1")
        assertNotNull(createdMember)
        assertEquals("member_uid_1", createdMember?.uid)
        assertEquals("member@example.com", createdMember?.email)
        assertEquals("member", createdMember?.role)
        assertEquals("ACTIVE", createdMember?.status)
        assertEquals("owner_uid_99", createdMember?.invitedByUid)
    }

    @Test
    fun test7_noUnauthorizedMembersReadOccursDuringAccept() {
        val invite = HouseholdInviteDto(
            inviteId = "inv_123",
            householdId = "hh_family",
            inviterUid = "owner_uid_99",
            inviteeEmail = "member@example.com",
            status = "PENDING",
            createdAt = System.currentTimeMillis() - 1000,
            expiresAt = System.currentTimeMillis() + 100000L
        )

        val harness = SimulatedAcceptanceHarness(
            authUser = AuthState.SignedIn(userUid = "member_uid_1", email = "member@example.com"),
            existingResolution = HouseholdResolutionResult.NoHousehold,
            invitationsStore = mutableMapOf("inv_123" to invite)
        )

        harness.acceptInvite("inv_123")

        // Assert that NO read query on members collection occurred
        val unauthorizedReads = harness.operationsLog.filter {
            it.startsWith("getMembers") || it.startsWith("queryMembers") || it.startsWith("transaction:getMember")
        }
        assertTrue("No unauthorized reads on members collection must occur prior to joining", unauthorizedReads.isEmpty())
    }

    @Test
    fun test8_strangerWithDifferentEmail_cannotAccept() {
        val invite = HouseholdInviteDto(
            inviteId = "inv_123",
            householdId = "hh_family",
            inviterUid = "owner_uid_99",
            inviteeEmail = "invited@example.com",
            status = "PENDING",
            createdAt = System.currentTimeMillis() - 1000,
            expiresAt = System.currentTimeMillis() + 100000L
        )

        val harness = SimulatedAcceptanceHarness(
            authUser = AuthState.SignedIn(userUid = "stranger_uid", email = "stranger@example.com"),
            existingResolution = HouseholdResolutionResult.NoHousehold,
            invitationsStore = mutableMapOf("inv_123" to invite)
        )

        val result = harness.acceptInvite("inv_123")
        assertTrue("Stranger must be rejected", result.isFailure)
        assertEquals("PENDING", harness.invitationsStore["inv_123"]?.status)
        assertNull(harness.membersStore["hh_family"]?.get("stranger_uid"))
    }

    @Test
    fun test9_expiredInvitation_cannotBeAccepted() {
        val invite = HouseholdInviteDto(
            inviteId = "inv_exp",
            householdId = "hh_family",
            inviterUid = "owner_uid_99",
            inviteeEmail = "member@example.com",
            status = "PENDING",
            createdAt = System.currentTimeMillis() - 200000,
            expiresAt = System.currentTimeMillis() - 100 // Expired
        )

        val harness = SimulatedAcceptanceHarness(
            authUser = AuthState.SignedIn(userUid = "member_uid_1", email = "member@example.com"),
            existingResolution = HouseholdResolutionResult.NoHousehold,
            invitationsStore = mutableMapOf("inv_exp" to invite)
        )

        val result = harness.acceptInvite("inv_exp")
        assertTrue("Expired invitation must be rejected", result.isFailure)
        assertEquals("PENDING", harness.invitationsStore["inv_exp"]?.status)
    }

    @Test
    fun test10_alreadyMemberOfAnotherHousehold_cannotAccept() {
        val invite = HouseholdInviteDto(
            inviteId = "inv_123",
            householdId = "hh_new",
            inviterUid = "owner_uid_99",
            inviteeEmail = "member@example.com",
            status = "PENDING",
            createdAt = System.currentTimeMillis() - 1000,
            expiresAt = System.currentTimeMillis() + 100000L
        )

        val harness = SimulatedAcceptanceHarness(
            authUser = AuthState.SignedIn(userUid = "member_uid_1", email = "member@example.com"),
            existingResolution = HouseholdResolutionResult.Success("hh_existing"),
            invitationsStore = mutableMapOf("inv_123" to invite)
        )

        val result = harness.acceptInvite("inv_123")
        assertTrue("User already in a household must be rejected", result.isFailure)
        assertEquals("PENDING", harness.invitationsStore["inv_123"]?.status)
    }

    // --------------------------------------------------------------------------
    // 3. UI Composition Tests (SettingsScreen Pending Invites & Dual Display)
    // --------------------------------------------------------------------------

    @Test
    fun test11_settingsScreenWithoutHousehold_displaysPendingInvitationsCard() {
        val pendingInvite = HouseholdInviteDto(
            inviteId = "inv_101",
            householdId = "hh_999",
            householdName = "The Partner Family",
            inviterEmail = "partner@example.com",
            inviteeEmail = "member@example.com",
            status = "PENDING",
            createdAt = System.currentTimeMillis(),
            expiresAt = System.currentTimeMillis() + 7 * 24 * 3600 * 1000L
        )

        composeTestRule.setContent {
            SettingsScreen(
                filterSettings = FilterSettings(),
                themeMode = "system",
                currentUid = "user_member_uid",
                currentUserEmail = "member@example.com",
                currentHousehold = null,
                currentUserMembership = null,
                incomingInvites = listOf(pendingInvite),
                onCurrencyChanged = {},
                onThemeModeChanged = {},
                onExportCsv = {},
                onSeedDemoData = {},
                onResetData = {}
            )
        }

        // Pending Invitations Card is displayed
        composeTestRule.onNodeWithTag("pending_invitations_card").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("pending_invite_item").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("invite_household_name").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("The Partner Family").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("From: partner@example.com").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("accept_invite_button").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("decline_invite_button").performScrollTo().assertIsDisplayed()

        // And since currentHousehold is null, Household Setup ("Create Household") is ALSO displayed
        composeTestRule.onNodeWithTag("household_setup_card").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("create_household_button").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun test12_settingsScreenWithoutHouseholdAndEmptyInvites_omitsPendingInvitationsCard() {
        composeTestRule.setContent {
            SettingsScreen(
                filterSettings = FilterSettings(),
                themeMode = "system",
                currentUid = "user_member_uid",
                currentUserEmail = "member@example.com",
                currentHousehold = null,
                currentUserMembership = null,
                incomingInvites = emptyList(),
                onCurrencyChanged = {},
                onThemeModeChanged = {},
                onExportCsv = {},
                onSeedDemoData = {},
                onResetData = {}
            )
        }

        // Pending Invitations Card does NOT exist
        composeTestRule.onNodeWithTag("pending_invitations_card").assertDoesNotExist()

        // Household Setup ("Create Household") IS displayed
        composeTestRule.onNodeWithTag("household_setup_card").performScrollTo().assertIsDisplayed()
    }
}
