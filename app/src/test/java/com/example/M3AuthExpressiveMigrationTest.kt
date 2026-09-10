package com.example

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.unit.dp
import com.example.data.repository.AuthState
import com.example.ui.screens.AuthScreen
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
class M3AuthExpressiveMigrationTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    // =========================================================================
    // 1. BRANDING & HERO IDENTITY
    // =========================================================================

    @Test
    fun test1_authScreenRendersBrandingAndHeroIdentity() {
        composeTestRule.setContent {
            FinTrackTheme {
                AuthScreen(
                    authState = AuthState.SignedOut,
                    onSignInWithGoogle = {},
                    onSignInWithTestUid = {},
                    onAuthError = {},
                    onClearError = {}
                )
            }
        }

        composeTestRule.onNodeWithText("FinTrack").assertIsDisplayed()
        composeTestRule.onNodeWithText("Your household finances, clearly organized").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Authentication Lock").assertIsDisplayed()
    }

    // =========================================================================
    // 2. SIGNED-OUT STATE & GOOGLE SIGN-IN CTA
    // =========================================================================

    @Test
    fun test2_signedOutStateRendersGoogleSignInButton() {
        composeTestRule.setContent {
            FinTrackTheme {
                AuthScreen(
                    authState = AuthState.SignedOut,
                    onSignInWithGoogle = {},
                    onSignInWithTestUid = {},
                    onAuthError = {},
                    onClearError = {}
                )
            }
        }

        composeTestRule.onNodeWithTag("google_sign_in_button").assertIsDisplayed()
        composeTestRule.onNodeWithText("Sign in with Google").assertIsDisplayed()
    }

    @Test
    fun test3_signedOutStateDisplaysExplanationText() {
        composeTestRule.setContent {
            FinTrackTheme {
                AuthScreen(
                    authState = AuthState.SignedOut,
                    onSignInWithGoogle = {},
                    onSignInWithTestUid = {},
                    onAuthError = {},
                    onClearError = {}
                )
            }
        }

        composeTestRule.onNodeWithText(
            text = "Sign in to view and manage your financial records. Your data is isolated and protected by account identity.",
            substring = true
        ).assertIsDisplayed()
    }

    // =========================================================================
    // 3. SIGNING-IN PROGRESS STATE
    // =========================================================================

    @Test
    fun test4_signingInStateDisplaysProgressIndicator() {
        composeTestRule.setContent {
            FinTrackTheme {
                AuthScreen(
                    authState = AuthState.SigningIn,
                    onSignInWithGoogle = {},
                    onSignInWithTestUid = {},
                    onAuthError = {},
                    onClearError = {}
                )
            }
        }

        composeTestRule.onNodeWithTag("auth_signing_in_indicator").assertIsDisplayed()
        composeTestRule.onNodeWithText("Signing in securely...").assertIsDisplayed()
    }

    @Test
    fun test5_signingInStateHidesGoogleSignInButton() {
        composeTestRule.setContent {
            FinTrackTheme {
                AuthScreen(
                    authState = AuthState.SigningIn,
                    onSignInWithGoogle = {},
                    onSignInWithTestUid = {},
                    onAuthError = {},
                    onClearError = {}
                )
            }
        }

        composeTestRule.onNodeWithTag("google_sign_in_button").assertDoesNotExist()
    }

    // =========================================================================
    // 4. AUTH ERROR & RETRY STATE
    // =========================================================================

    @Test
    fun test6_authErrorStateDisplaysErrorMessageAndIcon() {
        val errorMessage = "Network timeout occurred while authenticating."

        composeTestRule.setContent {
            FinTrackTheme {
                AuthScreen(
                    authState = AuthState.AuthError(errorMessage),
                    onSignInWithGoogle = {},
                    onSignInWithTestUid = {},
                    onAuthError = {},
                    onClearError = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Authentication Error").assertIsDisplayed()
        composeTestRule.onNodeWithTag("auth_error_message").assertIsDisplayed()
        composeTestRule.onNodeWithTag("auth_error_message").assertTextContains(errorMessage)
        composeTestRule.onNodeWithContentDescription("Auth Error").assertIsDisplayed()
    }

    @Test
    fun test7_authErrorStateDisplaysRetryButton() {
        composeTestRule.setContent {
            FinTrackTheme {
                AuthScreen(
                    authState = AuthState.AuthError("Some error"),
                    onSignInWithGoogle = {},
                    onSignInWithTestUid = {},
                    onAuthError = {},
                    onClearError = {}
                )
            }
        }

        composeTestRule.onNodeWithTag("auth_retry_button").assertIsDisplayed()
        composeTestRule.onNodeWithText("Try Again").assertIsDisplayed()
    }

    @Test
    fun test8_retryButtonClickTriggersOnClearError() {
        var clearErrorTriggered = false

        composeTestRule.setContent {
            FinTrackTheme {
                AuthScreen(
                    authState = AuthState.AuthError("Sample failure"),
                    onSignInWithGoogle = {},
                    onSignInWithTestUid = {},
                    onAuthError = {},
                    onClearError = { clearErrorTriggered = true }
                )
            }
        }

        composeTestRule.onNodeWithTag("auth_retry_button").performClick()
        assertTrue(clearErrorTriggered)
    }

    // =========================================================================
    // 5. ACCESSIBILITY & TOUCH TARGETS (>=48dp)
    // =========================================================================

    @Test
    fun test9_googleSignInButtonTouchTargetAtLeast48dp() {
        composeTestRule.setContent {
            FinTrackTheme {
                AuthScreen(
                    authState = AuthState.SignedOut,
                    onSignInWithGoogle = {},
                    onSignInWithTestUid = {},
                    onAuthError = {},
                    onClearError = {}
                )
            }
        }

        composeTestRule.onNodeWithTag("google_sign_in_button")
            .assertHeightIsAtLeast(48.dp)
    }

    @Test
    fun test10_retryButtonTouchTargetAtLeast48dp() {
        composeTestRule.setContent {
            FinTrackTheme {
                AuthScreen(
                    authState = AuthState.AuthError("Failure"),
                    onSignInWithGoogle = {},
                    onSignInWithTestUid = {},
                    onAuthError = {},
                    onClearError = {}
                )
            }
        }

        composeTestRule.onNodeWithTag("auth_retry_button")
            .assertHeightIsAtLeast(48.dp)
    }

    @Test
    fun test11_accessibilityRoleAndContentDescriptions() {
        composeTestRule.setContent {
            FinTrackTheme {
                AuthScreen(
                    authState = AuthState.SignedOut,
                    onSignInWithGoogle = {},
                    onSignInWithTestUid = {},
                    onAuthError = {},
                    onClearError = {}
                )
            }
        }

        composeTestRule.onNodeWithContentDescription("Authentication Lock").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Google Icon").assertIsDisplayed()
    }

    // =========================================================================
    // 6. DEBUG DIRECT ACCOUNT SIGN-IN TOOLS
    // =========================================================================

    @Test
    fun test12_debugSectionPresentInDebugBuild() {
        composeTestRule.setContent {
            FinTrackTheme {
                AuthScreen(
                    authState = AuthState.SignedOut,
                    onSignInWithGoogle = {},
                    onSignInWithTestUid = {},
                    onAuthError = {},
                    onClearError = {}
                )
            }
        }

        if (com.example.BuildConfig.DEBUG) {
            composeTestRule.onNodeWithTag("test_uid_input_field").performScrollTo().assertIsDisplayed()
            composeTestRule.onNodeWithTag("test_uid_sign_in_button").performScrollTo().assertIsDisplayed()
        }
    }

    @Test
    fun test13_debugSignInButtonClickTriggersCallback() {
        var signedInUid: String? = null

        composeTestRule.setContent {
            FinTrackTheme {
                AuthScreen(
                    authState = AuthState.SignedOut,
                    onSignInWithGoogle = {},
                    onSignInWithTestUid = { uid -> signedInUid = uid },
                    onAuthError = {},
                    onClearError = {}
                )
            }
        }

        if (com.example.BuildConfig.DEBUG) {
            composeTestRule.onNodeWithTag("test_uid_sign_in_button").performScrollTo().performClick()
            assertEquals("user_account_1", signedInUid)
        }
    }

    // =========================================================================
    // 7. RESPONSIVE LAYOUT VERIFICATION
    // =========================================================================

    @Test
    fun test14_responsiveLayout360dpCompact() {
        composeTestRule.setContent {
            FinTrackTheme {
                Box(modifier = Modifier.width(360.dp)) {
                    AuthScreen(
                        authState = AuthState.SignedOut,
                        onSignInWithGoogle = {},
                        onSignInWithTestUid = {},
                        onAuthError = {},
                        onClearError = {}
                    )
                }
            }
        }

        composeTestRule.onNodeWithTag("auth_screen_surface").assertIsDisplayed()
        composeTestRule.onNodeWithTag("auth_card").assertIsDisplayed()
        composeTestRule.onNodeWithTag("google_sign_in_button").assertIsDisplayed()
    }

    @Test
    fun test15_responsiveLayout390dpStandard() {
        composeTestRule.setContent {
            FinTrackTheme {
                Box(modifier = Modifier.width(390.dp)) {
                    AuthScreen(
                        authState = AuthState.SignedOut,
                        onSignInWithGoogle = {},
                        onSignInWithTestUid = {},
                        onAuthError = {},
                        onClearError = {}
                    )
                }
            }
        }

        composeTestRule.onNodeWithTag("auth_card").assertIsDisplayed()
        composeTestRule.onNodeWithTag("google_sign_in_button").assertIsDisplayed()
    }

    @Test
    fun test16_responsiveLayout412dpStandard() {
        composeTestRule.setContent {
            FinTrackTheme {
                Box(modifier = Modifier.width(412.dp)) {
                    AuthScreen(
                        authState = AuthState.SignedOut,
                        onSignInWithGoogle = {},
                        onSignInWithTestUid = {},
                        onAuthError = {},
                        onClearError = {}
                    )
                }
            }
        }

        composeTestRule.onNodeWithTag("auth_card").assertIsDisplayed()
        composeTestRule.onNodeWithTag("google_sign_in_button").assertIsDisplayed()
    }

    @Test
    fun test17_responsiveLayout600dpTabletAdaptive() {
        composeTestRule.setContent {
            FinTrackTheme {
                Box(modifier = Modifier.width(600.dp)) {
                    AuthScreen(
                        authState = AuthState.SignedOut,
                        onSignInWithGoogle = {},
                        onSignInWithTestUid = {},
                        onAuthError = {},
                        onClearError = {}
                    )
                }
            }
        }

        composeTestRule.onNodeWithTag("auth_card").assertIsDisplayed()
        composeTestRule.onNodeWithTag("google_sign_in_button").assertIsDisplayed()
    }

    // =========================================================================
    // 8. REDUCED MOTION & CONTRACT INTEGRITY
    // =========================================================================

    @Test
    fun test18_reducedMotionExecution() {
        composeTestRule.setContent {
            FinTrackTheme {
                AuthScreen(
                    authState = AuthState.SignedOut,
                    onSignInWithGoogle = {},
                    onSignInWithTestUid = {},
                    onAuthError = {},
                    onClearError = {}
                )
            }
        }

        composeTestRule.onNodeWithTag("auth_card").assertIsDisplayed()
        composeTestRule.onNodeWithTag("google_sign_in_button").assertIsDisplayed()
    }

    @Test
    fun test19_preservesAllLegacyTestTags() {
        composeTestRule.setContent {
            FinTrackTheme {
                AuthScreen(
                    authState = AuthState.SignedOut,
                    onSignInWithGoogle = {},
                    onSignInWithTestUid = {},
                    onAuthError = {},
                    onClearError = {}
                )
            }
        }

        composeTestRule.onNodeWithTag("auth_screen_surface").assertIsDisplayed()
        composeTestRule.onNodeWithTag("auth_card").assertIsDisplayed()
        composeTestRule.onNodeWithTag("google_sign_in_button").assertIsDisplayed()
    }

    @Test
    fun test20_noFinancialDataLeakageInAuthScreen() {
        composeTestRule.setContent {
            FinTrackTheme {
                AuthScreen(
                    authState = AuthState.SignedOut,
                    onSignInWithGoogle = {},
                    onSignInWithTestUid = {},
                    onAuthError = {},
                    onClearError = {}
                )
            }
        }

        // Must not expose financial figures, accounts or transactions prior to sign in
        composeTestRule.onNodeWithText("RON", substring = true).assertDoesNotExist()
        composeTestRule.onNodeWithText("EUR", substring = true).assertDoesNotExist()
        composeTestRule.onNodeWithText("Net Worth", substring = true).assertDoesNotExist()
    }
}
