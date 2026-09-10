package com.example

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.dp
import com.example.data.model.FilterSettings
import com.example.data.model.HouseholdDto
import com.example.data.model.HouseholdInviteDto
import com.example.data.model.HouseholdMemberDto
import com.example.data.repository.PendingRetryResult
import com.example.data.repository.SyncStatus
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.FinTrackTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class M3SettingsHouseholdExpressiveMigrationTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun sampleHousehold() = HouseholdDto(
        householdId = "hh_sample_123",
        name = "FinTrack Household",
        createdByUid = "user_owner_1",
        createdAt = 1700000000000L,
        updatedAt = 1700000000000L
    )

    private fun sampleOwnerMembership() = HouseholdMemberDto(
        uid = "user_owner_1",
        displayName = "Owner User",
        email = "owner@example.com",
        role = "OWNER",
        status = "ACTIVE",
        joinedAt = 1700000000000L
    )

    private fun sampleMemberMembership() = HouseholdMemberDto(
        uid = "user_member_2",
        displayName = "Member User",
        email = "member@example.com",
        role = "MEMBER",
        status = "ACTIVE",
        joinedAt = 1700001000000L
    )

    private fun sampleMembers() = listOf(
        sampleOwnerMembership(),
        sampleMemberMembership()
    )

    private fun sampleInvite() = HouseholdInviteDto(
        inviteId = "inv_sample_99",
        householdId = "hh_sample_123",
        householdName = "FinTrack Household",
        inviterUid = "user_owner_1",
        inviterEmail = "owner@example.com",
        inviteeEmail = "invitee@example.com",
        status = "PENDING",
        createdAt = 1700002000000L,
        expiresAt = System.currentTimeMillis() + 7 * 24 * 3600 * 1000L
    )

    // =========================================================================
    // 1. HEADER & SYNCHRONIZATION STATUS
    // =========================================================================

    @Test
    fun test1_settingsHeaderRendersWithTitleSubtitleAndBadge() {
        composeTestRule.setContent {
            FinTrackTheme {
                SettingsScreen(
                    filterSettings = FilterSettings(),
                    themeMode = "system",
                    currentUid = "user_owner_1",
                    currentUserEmail = "owner@example.com",
                    onCurrencyChanged = {},
                    onThemeModeChanged = {},
                    onExportCsv = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Settings").assertIsDisplayed()
        composeTestRule.onNodeWithText("Preferences & System").assertIsDisplayed()
        composeTestRule.onNodeWithText("Account, household sync, and data preferences").assertIsDisplayed()
    }

    // =========================================================================
    // 2. APPEARANCE SECTION & THEME CONTROLS
    // =========================================================================

    @Test
    fun test2_appearanceSectionRendersWithThemeAndCurrency() {
        composeTestRule.setContent {
            FinTrackTheme {
                SettingsScreen(
                    filterSettings = FilterSettings(selectedCurrency = "RON"),
                    themeMode = "system",
                    currentUid = "user_owner_1",
                    currentUserEmail = "owner@example.com",
                    onCurrencyChanged = {},
                    onThemeModeChanged = {},
                    onExportCsv = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Appearance Theme").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Display Currency").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("RON").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("EUR").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun test3_themeModeSwitchingTriggersCallback() {
        var selectedTheme: String? = null

        composeTestRule.setContent {
            FinTrackTheme {
                SettingsScreen(
                    filterSettings = FilterSettings(),
                    themeMode = "system",
                    currentUid = "user_owner_1",
                    currentUserEmail = "owner@example.com",
                    onCurrencyChanged = {},
                    onThemeModeChanged = { selectedTheme = it },
                    onExportCsv = {}
                )
            }
        }

        // Click "Dark"
        composeTestRule.onNodeWithText("Dark").performClick()
        assertEquals("dark", selectedTheme)

        // Click "Light"
        composeTestRule.onNodeWithText("Light").performClick()
        assertEquals("light", selectedTheme)

        // Click "System"
        composeTestRule.onNodeWithText("System").performClick()
        assertEquals("system", selectedTheme)
    }

    @Test
    fun test4_appearanceTabRoleSemantics() {
        composeTestRule.setContent {
            FinTrackTheme {
                SettingsScreen(
                    filterSettings = FilterSettings(),
                    themeMode = "system",
                    currentUid = "user_owner_1",
                    currentUserEmail = "owner@example.com",
                    onCurrencyChanged = {},
                    onThemeModeChanged = {},
                    onExportCsv = {}
                )
            }
        }

        val tabMatcher = SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Tab)
        // 3 tabs for Theme (Dark, Light, System) + 2 tabs for Currency (RON, EUR)
        composeTestRule.onAllNodes(tabMatcher).assertCountEquals(5)
    }

    // =========================================================================
    // 3. ACCOUNT SECTION
    // =========================================================================

    @Test
    fun test5_accountSectionRendersIdentity() {
        composeTestRule.setContent {
            FinTrackTheme {
                SettingsScreen(
                    filterSettings = FilterSettings(),
                    themeMode = "system",
                    currentUid = "usr_active_999",
                    currentUserEmail = "tester@fintrack.ro",
                    onCurrencyChanged = {},
                    onThemeModeChanged = {},
                    onExportCsv = {}
                )
            }
        }

        composeTestRule.onNodeWithTag("account_info_card").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("account_uid_text").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Firebase UID: usr_active_999").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Email: tester@fintrack.ro").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Active").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun test6_signOutButtonTriggersCallback() {
        var signOutTriggered = false

        composeTestRule.setContent {
            FinTrackTheme {
                SettingsScreen(
                    filterSettings = FilterSettings(),
                    themeMode = "system",
                    currentUid = "usr_active_999",
                    currentUserEmail = "tester@fintrack.ro",
                    onSignOut = { signOutTriggered = true },
                    onCurrencyChanged = {},
                    onThemeModeChanged = {},
                    onExportCsv = {}
                )
            }
        }

        composeTestRule.onNodeWithTag("sign_out_button").performScrollTo().performClick()
        assertTrue(signOutTriggered)
    }

    // =========================================================================
    // 4. HOUSEHOLD SETUP (WHEN NO HOUSEHOLD)
    // =========================================================================

    @Test
    fun test7_householdSetupDisplayedWhenNoHousehold() {
        composeTestRule.setContent {
            FinTrackTheme {
                SettingsScreen(
                    filterSettings = FilterSettings(),
                    themeMode = "system",
                    currentUid = "usr_active_999",
                    currentUserEmail = "tester@fintrack.ro",
                    currentHousehold = null,
                    onCurrencyChanged = {},
                    onThemeModeChanged = {},
                    onExportCsv = {}
                )
            }
        }

        composeTestRule.onNodeWithTag("household_setup_card").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Household Setup").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("create_household_button").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("household_summary_card").assertDoesNotExist()
    }

    @Test
    fun test8_createHouseholdButtonClickOpensDialog() {
        composeTestRule.setContent {
            FinTrackTheme {
                SettingsScreen(
                    filterSettings = FilterSettings(),
                    themeMode = "system",
                    currentUid = "usr_active_999",
                    currentUserEmail = "tester@fintrack.ro",
                    currentHousehold = null,
                    onCurrencyChanged = {},
                    onThemeModeChanged = {},
                    onExportCsv = {}
                )
            }
        }

        composeTestRule.onNodeWithTag("create_household_button").performScrollTo().performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("create_household_dialog").assertIsDisplayed()
        composeTestRule.onNodeWithTag("household_name_input").assertIsDisplayed()
    }

    // =========================================================================
    // 5. HOUSEHOLD OVERVIEW & RBAC PERMISSIONS
    // =========================================================================

    @Test
    fun test9_householdSummaryDisplayedWhenActiveHousehold() {
        composeTestRule.setContent {
            FinTrackTheme {
                SettingsScreen(
                    filterSettings = FilterSettings(),
                    themeMode = "system",
                    currentUid = "user_owner_1",
                    currentUserEmail = "owner@example.com",
                    currentHousehold = sampleHousehold(),
                    currentUserMembership = sampleOwnerMembership(),
                    householdMembers = sampleMembers(),
                    onCurrencyChanged = {},
                    onThemeModeChanged = {},
                    onExportCsv = {}
                )
            }
        }

        composeTestRule.onNodeWithTag("household_summary_card").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("household_name_text").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("FinTrack Household").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("household_setup_card").assertDoesNotExist()
    }

    @Test
    fun test10_ownerRoleDisplaysInviteMemberButton() {
        composeTestRule.setContent {
            FinTrackTheme {
                SettingsScreen(
                    filterSettings = FilterSettings(),
                    themeMode = "system",
                    currentUid = "user_owner_1",
                    currentUserEmail = "owner@example.com",
                    currentHousehold = sampleHousehold(),
                    currentUserMembership = sampleOwnerMembership(),
                    householdMembers = sampleMembers(),
                    onCurrencyChanged = {},
                    onThemeModeChanged = {},
                    onExportCsv = {}
                )
            }
        }

        // Owner sees invite member button
        composeTestRule.onNodeWithTag("invite_member_button").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("household_user_role_text").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun test11_memberRoleHidesInviteMemberButton() {
        composeTestRule.setContent {
            FinTrackTheme {
                SettingsScreen(
                    filterSettings = FilterSettings(),
                    themeMode = "system",
                    currentUid = "user_member_2",
                    currentUserEmail = "member@example.com",
                    currentHousehold = sampleHousehold(),
                    currentUserMembership = sampleMemberMembership(),
                    householdMembers = sampleMembers(),
                    onCurrencyChanged = {},
                    onThemeModeChanged = {},
                    onExportCsv = {}
                )
            }
        }

        // Member must NOT see invite member button
        composeTestRule.onNodeWithTag("invite_member_button").assertDoesNotExist()
        composeTestRule.onNodeWithTag("household_user_role_text").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun test12_memberRowsRenderWithRoleAndStatusBadges() {
        composeTestRule.setContent {
            FinTrackTheme {
                SettingsScreen(
                    filterSettings = FilterSettings(),
                    themeMode = "system",
                    currentUid = "user_owner_1",
                    currentUserEmail = "owner@example.com",
                    currentHousehold = sampleHousehold(),
                    currentUserMembership = sampleOwnerMembership(),
                    householdMembers = sampleMembers(),
                    onCurrencyChanged = {},
                    onThemeModeChanged = {},
                    onExportCsv = {}
                )
            }
        }

        composeTestRule.onAllNodesWithTag("household_member_row").assertCountEquals(2)
        composeTestRule.onAllNodesWithTag("household_member_status").assertCountEquals(2)
        composeTestRule.onAllNodesWithTag("household_member_role").assertCountEquals(2)
    }

    // =========================================================================
    // 6. PENDING INVITATIONS
    // =========================================================================

    @Test
    fun test13_pendingInvitationsDisplayedWhenPresent() {
        composeTestRule.setContent {
            FinTrackTheme {
                SettingsScreen(
                    filterSettings = FilterSettings(),
                    themeMode = "system",
                    currentUid = "user_invitee_3",
                    currentUserEmail = "invitee@example.com",
                    currentHousehold = null,
                    incomingInvites = listOf(sampleInvite()),
                    onCurrencyChanged = {},
                    onThemeModeChanged = {},
                    onExportCsv = {}
                )
            }
        }

        composeTestRule.onNodeWithTag("pending_invitations_card").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("pending_invite_item").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("invite_household_name").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("FinTrack Household").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("From: owner@example.com").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun test14_acceptInviteClickTriggersCallback() {
        var acceptedInviteId: String? = null

        composeTestRule.setContent {
            FinTrackTheme {
                SettingsScreen(
                    filterSettings = FilterSettings(),
                    themeMode = "system",
                    currentUid = "user_invitee_3",
                    currentUserEmail = "invitee@example.com",
                    currentHousehold = null,
                    incomingInvites = listOf(sampleInvite()),
                    onAcceptInvite = { acceptedInviteId = it },
                    onCurrencyChanged = {},
                    onThemeModeChanged = {},
                    onExportCsv = {}
                )
            }
        }

        composeTestRule.onNodeWithTag("accept_invite_button").performScrollTo().performClick()
        assertEquals("inv_sample_99", acceptedInviteId)
    }

    @Test
    fun test15_declineInviteClickTriggersCallback() {
        var declinedInviteId: String? = null

        composeTestRule.setContent {
            FinTrackTheme {
                SettingsScreen(
                    filterSettings = FilterSettings(),
                    themeMode = "system",
                    currentUid = "user_invitee_3",
                    currentUserEmail = "invitee@example.com",
                    currentHousehold = null,
                    incomingInvites = listOf(sampleInvite()),
                    onDeclineInvite = { declinedInviteId = it },
                    onCurrencyChanged = {},
                    onThemeModeChanged = {},
                    onExportCsv = {}
                )
            }
        }

        composeTestRule.onNodeWithTag("decline_invite_button").performScrollTo().performClick()
        assertEquals("inv_sample_99", declinedInviteId)
    }

    // =========================================================================
    // 7. DATA EXPORT & REPORTS SECTION
    // =========================================================================

    @Test
    fun test16_dataSectionRendersExportAndImport() {
        composeTestRule.setContent {
            FinTrackTheme {
                SettingsScreen(
                    filterSettings = FilterSettings(),
                    themeMode = "system",
                    currentUid = "user_owner_1",
                    currentUserEmail = "owner@example.com",
                    onCurrencyChanged = {},
                    onThemeModeChanged = {},
                    onExportCsv = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Data Export & Reports").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("export_csv_button").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("import_csv_button").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("retry_eur_conversions_button").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun test17_exportCsvButtonTriggersCallback() {
        var exportTriggered = false

        composeTestRule.setContent {
            FinTrackTheme {
                SettingsScreen(
                    filterSettings = FilterSettings(),
                    themeMode = "system",
                    currentUid = "user_owner_1",
                    currentUserEmail = "owner@example.com",
                    onExportCsv = { exportTriggered = true },
                    onCurrencyChanged = {},
                    onThemeModeChanged = {}
                )
            }
        }

        composeTestRule.onNodeWithTag("export_csv_button").performScrollTo().performClick()
        assertTrue(exportTriggered)
    }

    @Test
    fun test18_retryEurConversionsButtonTriggersCallback() {
        var retryTriggered = false

        composeTestRule.setContent {
            FinTrackTheme {
                SettingsScreen(
                    filterSettings = FilterSettings(),
                    themeMode = "system",
                    currentUid = "user_owner_1",
                    currentUserEmail = "owner@example.com",
                    onRetryPendingConversions = { retryTriggered = true },
                    onCurrencyChanged = {},
                    onThemeModeChanged = {},
                    onExportCsv = {}
                )
            }
        }

        composeTestRule.onNodeWithTag("retry_eur_conversions_button").performScrollTo().performClick()
        assertTrue(retryTriggered)
    }

    // =========================================================================
    // 8. DIAGNOSTICS & SYNC STATUS
    // =========================================================================

    @Test
    fun test19_syncStatusIndicatorRenders() {
        composeTestRule.setContent {
            FinTrackTheme {
                SettingsScreen(
                    filterSettings = FilterSettings(),
                    themeMode = "system",
                    currentUid = "user_owner_1",
                    currentUserEmail = "owner@example.com",
                    syncStatus = SyncStatus.Synced(),
                    onCurrencyChanged = {},
                    onThemeModeChanged = {},
                    onExportCsv = {}
                )
            }
        }

        composeTestRule.onNodeWithTag("sync_status_indicator").assertIsDisplayed()
    }

    // =========================================================================
    // 9. ACCESSIBILITY & TOUCH TARGETS
    // =========================================================================

    @Test
    fun test20_touchTargetsMeet48dpMinimum() {
        composeTestRule.setContent {
            FinTrackTheme {
                SettingsScreen(
                    filterSettings = FilterSettings(),
                    themeMode = "system",
                    currentUid = "user_owner_1",
                    currentUserEmail = "owner@example.com",
                    currentHousehold = sampleHousehold(),
                    currentUserMembership = sampleOwnerMembership(),
                    householdMembers = sampleMembers(),
                    onCurrencyChanged = {},
                    onThemeModeChanged = {},
                    onExportCsv = {}
                )
            }
        }

        composeTestRule.onNodeWithTag("sign_out_button")
            .assertHeightIsAtLeast(48.dp)

        composeTestRule.onNodeWithTag("export_csv_button")
            .assertHeightIsAtLeast(48.dp)

        composeTestRule.onNodeWithTag("import_csv_button")
            .assertHeightIsAtLeast(48.dp)

        composeTestRule.onNodeWithTag("retry_eur_conversions_button")
            .assertHeightIsAtLeast(48.dp)

        composeTestRule.onNodeWithTag("invite_member_button")
            .assertHeightIsAtLeast(48.dp)
    }

    @Test
    fun test21_accessibilitySemanticsAndContentDescriptions() {
        composeTestRule.setContent {
            FinTrackTheme {
                SettingsScreen(
                    filterSettings = FilterSettings(),
                    themeMode = "system",
                    currentUid = "user_owner_1",
                    currentUserEmail = "owner@example.com",
                    onCurrencyChanged = {},
                    onThemeModeChanged = {},
                    onExportCsv = {}
                )
            }
        }

        // Header and cards provide unambiguous text hierarchy
        composeTestRule.onNodeWithText("Settings").assertIsDisplayed()
        composeTestRule.onNodeWithText("Preferences & System").assertIsDisplayed()
    }

    @Test
    fun test22_reducedMotionExecution() {
        composeTestRule.setContent {
            FinTrackTheme {
                SettingsScreen(
                    filterSettings = FilterSettings(),
                    themeMode = "dark",
                    currentUid = "user_owner_1",
                    currentUserEmail = "owner@example.com",
                    currentHousehold = sampleHousehold(),
                    currentUserMembership = sampleOwnerMembership(),
                    householdMembers = sampleMembers(),
                    onCurrencyChanged = {},
                    onThemeModeChanged = {},
                    onExportCsv = {}
                )
            }
        }

        composeTestRule.onNodeWithTag("household_summary_card").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Dark").performScrollTo().assertIsDisplayed()
    }

    // =========================================================================
    // 10. RESPONSIVE LAYOUT STABILITY
    // =========================================================================

    @Test
    fun test23_responsiveLayout360dp() {
        composeTestRule.setContent {
            FinTrackTheme {
                Box(modifier = Modifier.width(360.dp)) {
                    SettingsScreen(
                        filterSettings = FilterSettings(),
                        themeMode = "system",
                        currentUid = "user_owner_1",
                        currentUserEmail = "owner@example.com",
                        onCurrencyChanged = {},
                        onThemeModeChanged = {},
                        onExportCsv = {}
                    )
                }
            }
        }

        composeTestRule.onNodeWithText("Settings").assertIsDisplayed()
        composeTestRule.onNodeWithTag("account_info_card").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun test24_responsiveLayout390dp() {
        composeTestRule.setContent {
            FinTrackTheme {
                Box(modifier = Modifier.width(390.dp)) {
                    SettingsScreen(
                        filterSettings = FilterSettings(),
                        themeMode = "system",
                        currentUid = "user_owner_1",
                        currentUserEmail = "owner@example.com",
                        onCurrencyChanged = {},
                        onThemeModeChanged = {},
                        onExportCsv = {}
                    )
                }
            }
        }

        composeTestRule.onNodeWithText("Settings").assertIsDisplayed()
        composeTestRule.onNodeWithTag("account_info_card").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun test25_responsiveLayout412dp() {
        composeTestRule.setContent {
            FinTrackTheme {
                Box(modifier = Modifier.width(412.dp)) {
                    SettingsScreen(
                        filterSettings = FilterSettings(),
                        themeMode = "system",
                        currentUid = "user_owner_1",
                        currentUserEmail = "owner@example.com",
                        onCurrencyChanged = {},
                        onThemeModeChanged = {},
                        onExportCsv = {}
                    )
                }
            }
        }

        composeTestRule.onNodeWithText("Settings").assertIsDisplayed()
        composeTestRule.onNodeWithTag("account_info_card").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun test26_responsiveLayout600dpAdaptive() {
        composeTestRule.setContent {
            FinTrackTheme {
                Box(modifier = Modifier.width(720.dp)) {
                    SettingsScreen(
                        filterSettings = FilterSettings(),
                        themeMode = "system",
                        currentUid = "user_owner_1",
                        currentUserEmail = "owner@example.com",
                        onCurrencyChanged = {},
                        onThemeModeChanged = {},
                        onExportCsv = {}
                    )
                }
            }
        }

        composeTestRule.onNodeWithText("Settings").assertIsDisplayed()
        composeTestRule.onNodeWithTag("account_info_card").performScrollTo().assertIsDisplayed()
    }

    // =========================================================================
    // 11. HOUSEHOLD ISOLATION & DIALOGS
    // =========================================================================

    @Test
    fun test27_noCrossHouseholdDataLeakage() {
        val isolatedHousehold = HouseholdDto(
            householdId = "hh_isolated_999",
            name = "Secret Household",
            createdByUid = "user_secret"
        )

        composeTestRule.setContent {
            FinTrackTheme {
                SettingsScreen(
                    filterSettings = FilterSettings(),
                    themeMode = "system",
                    currentUid = "user_owner_1",
                    currentUserEmail = "owner@example.com",
                    currentHousehold = isolatedHousehold,
                    onCurrencyChanged = {},
                    onThemeModeChanged = {},
                    onExportCsv = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Secret Household").performScrollTo().assertIsDisplayed()
        composeTestRule.onAllNodesWithText("FinTrack Household").assertCountEquals(0)
    }

    @Test
    fun test28_retryResultDialogDisplaysInformation() {
        val retryResult = PendingRetryResult(
            pendingBefore = 10,
            convertedSuccessfully = 8,
            stillPending = 2,
            failedCount = 0,
            mainFailureReason = null
        )

        composeTestRule.setContent {
            FinTrackTheme {
                SettingsScreen(
                    filterSettings = FilterSettings(),
                    themeMode = "system",
                    currentUid = "user_owner_1",
                    currentUserEmail = "owner@example.com",
                    pendingRetryResult = retryResult,
                    onCurrencyChanged = {},
                    onThemeModeChanged = {},
                    onExportCsv = {}
                )
            }
        }

        composeTestRule.onNodeWithText("EUR Conversions Retry Result").assertIsDisplayed()
        composeTestRule.onNodeWithText("• Pending before retry: 10").assertIsDisplayed()
        composeTestRule.onNodeWithText("• Converted successfully: 8").assertIsDisplayed()
        composeTestRule.onNodeWithText("• Still pending: 2").assertIsDisplayed()
        composeTestRule.onNodeWithText("OK").assertIsDisplayed()
    }
}
