package com.example

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.dp
import com.example.data.model.FilterSettings
import com.example.data.model.HouseholdDto
import com.example.data.model.HouseholdMemberDto
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.FinTrackTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Focused verification tests for Settings Screen Header UI Refinement:
 * 1. "Settings" title is present as the primary header title.
 * 2. Settings icon is completely removed.
 * 3. Subtitle "Preferences & System" is absent.
 * 4. Descriptive text "Account, household sync, and data preferences" is absent.
 * 5. Currency converter is absent from the Settings header.
 * 6. Existing Settings sections remain intact and accessible.
 * 7. OWNER vs MEMBER access restrictions (Categories, Developer) remain strictly enforced.
 * 8. Responsive widths (360dp, 390dp, 412dp) and Light/Dark themes render cleanly.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class SettingsHeaderRefinementTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun sampleHousehold() = HouseholdDto(
        householdId = "hh_test_1",
        name = "Familia Popescu",
        createdByUid = "user_owner_1",
        createdAt = 1700000000000L,
        updatedAt = 1700000000000L
    )

    private fun sampleOwnerMembership() = HouseholdMemberDto(
        uid = "user_owner_1",
        displayName = "Ion Popescu",
        email = "owner@fintrack.ro",
        role = "OWNER",
        joinedAt = 1700000000000L
    )

    private fun sampleMemberMembership() = HouseholdMemberDto(
        uid = "user_member_2",
        displayName = "Maria Popescu",
        email = "member@fintrack.ro",
        role = "MEMBER",
        joinedAt = 1700000000000L
    )

    @Test
    fun test01_settingsTitleIsPresentAndHeaderClutterIsAbsent() {
        composeTestRule.setContent {
            FinTrackTheme {
                SettingsScreen(
                    filterSettings = FilterSettings(),
                    themeMode = "system",
                    currentUid = "user_owner_1",
                    currentUserEmail = "owner@fintrack.ro",
                    currentHousehold = sampleHousehold(),
                    currentUserMembership = sampleOwnerMembership(),
                    onThemeModeChanged = {},
                    onExportCsv = {}
                )
            }
        }

        // 1. Settings title is displayed
        composeTestRule.onNodeWithText("Settings").assertIsDisplayed()

        // 2. Subtitle "Preferences & System" is absent
        composeTestRule.onNodeWithText("Preferences & System").assertDoesNotExist()

        // 3. Description "Account, household sync, and data preferences" is absent
        composeTestRule.onNodeWithText("Account, household sync, and data preferences").assertDoesNotExist()

        // 4. Currency converter is absent from the Settings header
        composeTestRule.onNodeWithTag("currency_toggle_RON").assertDoesNotExist()
        composeTestRule.onNodeWithTag("currency_toggle_EUR").assertDoesNotExist()
        composeTestRule.onNodeWithTag("currency_selector").assertDoesNotExist()
        composeTestRule.onNodeWithText("Display Currency").assertDoesNotExist()
    }

    @Test
    fun test02_existingSectionsRemainAvailableBelowSimplifiedHeader() {
        composeTestRule.setContent {
            FinTrackTheme {
                SettingsScreen(
                    filterSettings = FilterSettings(),
                    themeMode = "system",
                    currentUid = "user_owner_1",
                    currentUserEmail = "owner@fintrack.ro",
                    currentHousehold = sampleHousehold(),
                    currentUserMembership = sampleOwnerMembership(),
                    householdMembers = listOf(sampleOwnerMembership()),
                    onThemeModeChanged = {},
                    onExportCsv = {}
                )
            }
        }

        // Sync status indicator
        composeTestRule.onNodeWithTag("sync_status_indicator").assertIsDisplayed()

        // Appearance Theme section
        composeTestRule.onNodeWithText("Appearance Theme").performScrollTo().assertIsDisplayed()

        // Account Information section
        composeTestRule.onNodeWithTag("account_info_card").performScrollTo().assertIsDisplayed()

        // Household Summary section
        composeTestRule.onNodeWithTag("household_summary_card").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun test03_ownerRbacControlsRemainAccessible() {
        composeTestRule.setContent {
            FinTrackTheme {
                SettingsScreen(
                    filterSettings = FilterSettings(),
                    themeMode = "system",
                    currentUid = "user_owner_1",
                    currentUserEmail = "owner@fintrack.ro",
                    currentHousehold = sampleHousehold(),
                    currentUserMembership = sampleOwnerMembership(),
                    householdMembers = listOf(sampleOwnerMembership()),
                    onThemeModeChanged = {},
                    onExportCsv = {}
                )
            }
        }

        // OWNER should see Categories and Developer entries
        composeTestRule.onNodeWithTag("settings_item_categories").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("settings_item_developer").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun test04_memberRbacControlsRemainRestricted() {
        composeTestRule.setContent {
            FinTrackTheme {
                SettingsScreen(
                    filterSettings = FilterSettings(),
                    themeMode = "system",
                    currentUid = "user_member_2",
                    currentUserEmail = "member@fintrack.ro",
                    currentHousehold = sampleHousehold(),
                    currentUserMembership = sampleMemberMembership(),
                    householdMembers = listOf(sampleOwnerMembership(), sampleMemberMembership()),
                    onThemeModeChanged = {},
                    onExportCsv = {}
                )
            }
        }

        // MEMBER should NOT see Categories and Developer entries
        composeTestRule.onNodeWithTag("settings_item_categories").assertDoesNotExist()
        composeTestRule.onNodeWithTag("settings_item_developer").assertDoesNotExist()
    }

    @Test
    fun test05_responsiveLayout360dp() {
        composeTestRule.setContent {
            FinTrackTheme {
                Box(modifier = Modifier.width(360.dp).fillMaxSize()) {
                    SettingsScreen(
                        filterSettings = FilterSettings(),
                        themeMode = "system",
                        currentUid = "user_owner_1",
                        currentUserEmail = "owner@fintrack.ro",
                        currentHousehold = sampleHousehold(),
                        currentUserMembership = sampleOwnerMembership(),
                        onThemeModeChanged = {},
                        onExportCsv = {}
                    )
                }
            }
        }

        composeTestRule.onNodeWithText("Settings").assertIsDisplayed()
        composeTestRule.onNodeWithText("Preferences & System").assertDoesNotExist()
        composeTestRule.onNodeWithTag("sync_status_indicator").assertIsDisplayed()
    }

    @Test
    fun test06_responsiveLayout390dp() {
        composeTestRule.setContent {
            FinTrackTheme {
                Box(modifier = Modifier.width(390.dp).fillMaxSize()) {
                    SettingsScreen(
                        filterSettings = FilterSettings(),
                        themeMode = "system",
                        currentUid = "user_owner_1",
                        currentUserEmail = "owner@fintrack.ro",
                        currentHousehold = sampleHousehold(),
                        currentUserMembership = sampleOwnerMembership(),
                        onThemeModeChanged = {},
                        onExportCsv = {}
                    )
                }
            }
        }

        composeTestRule.onNodeWithText("Settings").assertIsDisplayed()
        composeTestRule.onNodeWithText("Preferences & System").assertDoesNotExist()
        composeTestRule.onNodeWithTag("sync_status_indicator").assertIsDisplayed()
    }

    @Test
    fun test07_responsiveLayout412dp() {
        composeTestRule.setContent {
            FinTrackTheme {
                Box(modifier = Modifier.width(412.dp).fillMaxSize()) {
                    SettingsScreen(
                        filterSettings = FilterSettings(),
                        themeMode = "system",
                        currentUid = "user_owner_1",
                        currentUserEmail = "owner@fintrack.ro",
                        currentHousehold = sampleHousehold(),
                        currentUserMembership = sampleOwnerMembership(),
                        onThemeModeChanged = {},
                        onExportCsv = {}
                    )
                }
            }
        }

        composeTestRule.onNodeWithText("Settings").assertIsDisplayed()
        composeTestRule.onNodeWithText("Preferences & System").assertDoesNotExist()
        composeTestRule.onNodeWithTag("sync_status_indicator").assertIsDisplayed()
    }

    @Test
    fun test08_lightThemeRendering() {
        composeTestRule.setContent {
            FinTrackTheme(darkTheme = false) {
                SettingsScreen(
                    filterSettings = FilterSettings(),
                    themeMode = "light",
                    currentUid = "user_owner_1",
                    currentUserEmail = "owner@fintrack.ro",
                    currentHousehold = sampleHousehold(),
                    currentUserMembership = sampleOwnerMembership(),
                    onThemeModeChanged = {},
                    onExportCsv = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Settings").assertIsDisplayed()
        composeTestRule.onNodeWithText("Preferences & System").assertDoesNotExist()
        composeTestRule.onNodeWithTag("sync_status_indicator").assertIsDisplayed()
    }

    @Test
    fun test09_darkThemeRendering() {
        composeTestRule.setContent {
            FinTrackTheme(darkTheme = true) {
                SettingsScreen(
                    filterSettings = FilterSettings(),
                    themeMode = "dark",
                    currentUid = "user_owner_1",
                    currentUserEmail = "owner@fintrack.ro",
                    currentHousehold = sampleHousehold(),
                    currentUserMembership = sampleOwnerMembership(),
                    onThemeModeChanged = {},
                    onExportCsv = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Settings").assertIsDisplayed()
        composeTestRule.onNodeWithText("Preferences & System").assertDoesNotExist()
        composeTestRule.onNodeWithTag("sync_status_indicator").assertIsDisplayed()
    }
}
