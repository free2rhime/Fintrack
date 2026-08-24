package com.example

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import com.example.data.model.FilterSettings
import com.example.data.model.HouseholdDto
import com.example.data.model.HouseholdMemberDto
import com.example.ui.components.CreateHouseholdDialog
import com.example.ui.screens.SettingsScreen
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class HouseholdCreationUiTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testSettingsScreenWithoutHouseholdDisplaysHouseholdSetupCard() {
        var createHouseholdClicked = false

        composeTestRule.setContent {
            SettingsScreen(
                filterSettings = FilterSettings(),
                themeMode = "system",
                currentUid = "user_123",
                currentUserEmail = "user@example.com",
                currentHousehold = null,
                currentUserMembership = null,
                onCurrencyChanged = {},
                onThemeModeChanged = {},
                onExportCsv = {},
                onSeedDemoData = {},
                onResetData = {},
                onCreateHousehold = { createHouseholdClicked = true }
            )
        }

        composeTestRule.onNodeWithTag("household_setup_card").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("create_household_button").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Household Setup").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Create Household").performScrollTo().assertIsDisplayed()

        // Clicking Create Household opens the dialog
        composeTestRule.onNodeWithTag("create_household_button").performClick()
        composeTestRule.onNodeWithTag("create_household_dialog").assertIsDisplayed()
    }

    @Test
    fun testSettingsScreenWithActiveHouseholdDisplaysHouseholdSummaryCard() {
        val activeHousehold = HouseholdDto(
            householdId = "hh_456",
            name = "Family Budget",
            createdByUid = "user_123",
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        val membership = HouseholdMemberDto(
            uid = "user_123",
            role = "owner",
            status = "ACTIVE",
            joinedAt = System.currentTimeMillis()
        )

        composeTestRule.setContent {
            SettingsScreen(
                filterSettings = FilterSettings(),
                themeMode = "system",
                currentUid = "user_123",
                currentUserEmail = "user@example.com",
                currentHousehold = activeHousehold,
                currentUserMembership = membership,
                householdMembers = listOf(membership),
                onCurrencyChanged = {},
                onThemeModeChanged = {},
                onExportCsv = {},
                onSeedDemoData = {},
                onResetData = {}
            )
        }

        composeTestRule.onNodeWithTag("household_summary_card").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("household_name_text").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Family Budget").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("household_setup_card").assertDoesNotExist()
    }

    @Test
    fun testCreateHouseholdDialogValidationAndSubmission() {
        var createdName: String? = null
        var dismissed = false

        composeTestRule.setContent {
            CreateHouseholdDialog(
                isCreating = false,
                errorMessage = null,
                onCreateHousehold = { createdName = it },
                onDismiss = { dismissed = true }
            )
        }

        composeTestRule.onNodeWithTag("create_household_dialog").assertIsDisplayed()

        // Click create with empty input -> validation error
        composeTestRule.onNodeWithTag("confirm_create_household_button").performClick()
        composeTestRule.onNodeWithTag("create_household_error_text").assertIsDisplayed()
        composeTestRule.onNodeWithText("Household name cannot be empty").assertIsDisplayed()
        assertEquals(null, createdName)

        // Enter valid household name
        composeTestRule.onNodeWithTag("household_name_input").performTextInput("Our Home")
        composeTestRule.onNodeWithTag("confirm_create_household_button").performClick()
        assertEquals("Our Home", createdName)
    }

    @Test
    fun testCreateHouseholdDialogDisplaysError() {
        composeTestRule.setContent {
            CreateHouseholdDialog(
                isCreating = false,
                errorMessage = "Household name already in use",
                onCreateHousehold = {},
                onDismiss = {}
            )
        }

        composeTestRule.onNodeWithTag("create_household_error_text").assertIsDisplayed()
        composeTestRule.onNodeWithText("Household name already in use").assertIsDisplayed()
    }
}
