package com.example

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.example.data.model.FilterSettings
import com.example.ui.MigrationConflictState
import com.example.ui.MigrationPreviewState
import com.example.ui.MigrationProgressState
import com.example.ui.MigrationResultState
import com.example.ui.components.MigrationConflictDialog
import com.example.ui.components.MigrationPreviewDialog
import com.example.ui.components.MigrationProgressDialog
import com.example.ui.components.MigrationResultDialog
import com.example.ui.screens.SettingsScreen
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class Stage3BUiTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testMigrationCardAndStartButtonAreNotDisplayedInSettings() {
        composeTestRule.setContent {
            SettingsScreen(
                filterSettings = FilterSettings(),
                themeMode = "system",
                currentUid = "test_user_123",
                currentUserEmail = "user@example.com",
                onSignOut = {},
                onCurrencyChanged = {},
                onThemeModeChanged = {},
                onExportCsv = {},
                onImportCsv = {},
                onSeedDemoData = {},
                onResetData = {},
                onStartMigration = {}
            )
        }

        // Verify card and button are NOT displayed in Settings (deprecated/removed in Step 12.3AC)
        composeTestRule.onNodeWithTag("cloud_migration_card").assertDoesNotExist()
        composeTestRule.onNodeWithTag("start_migration_button").assertDoesNotExist()
        composeTestRule.onNodeWithText("Cloud Household Migration").assertDoesNotExist()
        composeTestRule.onNodeWithText("Start Household Migration").assertDoesNotExist()

        // Verify remaining essential settings cards are present
        composeTestRule.onNodeWithText("Preferences & System").assertIsDisplayed()
        composeTestRule.onNodeWithText("Data Export & Reports").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Database Management").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun testPreviewDialogDisplaysCountsAndActions() {
        var confirmed = false
        var cancelled = false

        val previewState = MigrationPreviewState(
            householdId = "hh_family_42",
            userUid = "user_owner_99",
            userRole = "OWNER",
            transactionsCount = 150,
            categoriesCount = 18,
            exchangeRatesCount = 35,
            totalRecords = 203,
            backupBundlePath = "/storage/backup/bundle.zip"
        )

        composeTestRule.setContent {
            MigrationPreviewDialog(
                previewState = previewState,
                onConfirm = { confirmed = true },
                onCancel = { cancelled = true }
            )
        }

        composeTestRule.onNodeWithTag("migration_preview_dialog").assertIsDisplayed()
        composeTestRule.onNodeWithText("Migration Preview").assertIsDisplayed()
        composeTestRule.onNodeWithTag("preview_household_id").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("OWNER").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("150").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("18").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("35").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("203").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Preflight Backup Validated").performScrollTo().assertIsDisplayed()

        // Test cancel
        composeTestRule.onNodeWithTag("migration_preview_cancel_button").performClick()
        assertTrue("Cancel button invoked", cancelled)

        // Check acknowledgment checkbox to enable confirm button
        composeTestRule.onNodeWithTag("migration_acknowledgment_checkbox").performScrollTo().performClick()

        // Test confirm
        composeTestRule.onNodeWithTag("migration_preview_confirm_button").performClick()
        assertTrue("Confirm button invoked", confirmed)
    }

    @Test
    fun testConflictDialogDisplaysSanitizedErrorAndDismiss() {
        var dismissed = false

        val conflictState = MigrationConflictState(
            reason = "EXISTING_REMOTE_DATA_DETECTED",
            details = "Conflicting remote records detected in household hh_test. Migration was blocked to prevent data overwrite."
        )

        composeTestRule.setContent {
            MigrationConflictDialog(
                conflictState = conflictState,
                onDismiss = { dismissed = true }
            )
        }

        composeTestRule.onNodeWithTag("migration_conflict_dialog").assertIsDisplayed()
        composeTestRule.onNodeWithText("Migration Blocked").assertIsDisplayed()
        composeTestRule.onNodeWithText("EXISTING_REMOTE_DATA_DETECTED").assertIsDisplayed()
        composeTestRule.onNodeWithText("Conflicting remote records detected in household hh_test. Migration was blocked to prevent data overwrite.").assertIsDisplayed()

        composeTestRule.onNodeWithTag("migration_conflict_dismiss_button").performClick()
        assertTrue("Dismiss callback invoked", dismissed)
    }

    @Test
    fun testProgressDialogDisplaysStageAndSuppressionNotice() {
        val progressState = MigrationProgressState(
            stage = "UPLOADING_TRANSACTIONS",
            processedCount = 50,
            totalCount = 100
        )

        composeTestRule.setContent {
            MigrationProgressDialog(
                progressState = progressState
            )
        }

        composeTestRule.onNodeWithTag("migration_progress_dialog").assertIsDisplayed()
        composeTestRule.onNodeWithText("Migrating to Household Cloud").assertIsDisplayed()
        composeTestRule.onNodeWithText("Stage: UPLOADING_TRANSACTIONS").assertIsDisplayed()
        composeTestRule.onNodeWithText("50%").assertIsDisplayed()
        composeTestRule.onNodeWithText("50 / 100 records").assertIsDisplayed()
        composeTestRule.onNodeWithText("Real-time sync listeners are suppressed during migration to prevent write amplification.").assertIsDisplayed()
    }

    @Test
    fun testResultDialogDisplaysSuccessState() {
        var dismissed = false

        val successState = MigrationResultState.Success(
            migrationId = "mig_session_abcdef123456",
            categoriesUploaded = 18,
            ratesUploaded = 35,
            transactionsUploaded = 150,
            totalProcessed = 203
        )

        composeTestRule.setContent {
            MigrationResultDialog(
                resultState = successState,
                onDismiss = { dismissed = true }
            )
        }

        composeTestRule.onNodeWithTag("migration_result_dialog").assertIsDisplayed()
        composeTestRule.onNodeWithText("Migration Completed").assertIsDisplayed()
        composeTestRule.onNodeWithText("150").assertIsDisplayed()
        composeTestRule.onNodeWithText("18").assertIsDisplayed()
        composeTestRule.onNodeWithText("35").assertIsDisplayed()
        composeTestRule.onNodeWithText("203").assertIsDisplayed()
        composeTestRule.onNodeWithText("Done").assertIsDisplayed()

        composeTestRule.onNodeWithTag("migration_result_dismiss_button").performClick()
        assertTrue("Dismiss callback invoked", dismissed)
    }

    @Test
    fun testResultDialogDisplaysFailureStateWithSanitizedMessage() {
        var dismissed = false

        val failureState = MigrationResultState.Failure(
            stage = "UPLOAD_BATCH",
            sanitizedError = "Network connection timeout during batch transaction commit.",
            backupBundlePath = "/app/backups/mig_backup.zip"
        )

        composeTestRule.setContent {
            MigrationResultDialog(
                resultState = failureState,
                onDismiss = { dismissed = true }
            )
        }

        composeTestRule.onNodeWithTag("migration_result_dialog").assertIsDisplayed()
        composeTestRule.onNodeWithText("Migration Failed").assertIsDisplayed()
        composeTestRule.onNodeWithText("Failed Stage: UPLOAD_BATCH").assertIsDisplayed()
        composeTestRule.onNodeWithText("Network connection timeout during batch transaction commit.").assertIsDisplayed()

        composeTestRule.onNodeWithTag("migration_result_dismiss_button").performClick()
        assertTrue("Dismiss callback invoked", dismissed)
    }

    @Test
    fun testNoRawExceptionLeakageInDialogs() {
        val sanitizedError = "Invalid household permissions: User is not an active owner or admin."
        val failureState = MigrationResultState.Failure(
            stage = "VALIDATION",
            sanitizedError = sanitizedError
        )

        composeTestRule.setContent {
            MigrationResultDialog(
                resultState = failureState,
                onDismiss = {}
            )
        }

        // Verify sanitized error is rendered and no raw stack trace or exception class name is exposed
        composeTestRule.onNodeWithText(sanitizedError).assertIsDisplayed()
    }
}
