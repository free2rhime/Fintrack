package com.example.ui.screens

import android.content.Context
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import com.example.data.repository.AuthState
import com.example.data.repository.GoogleSignInConfigProvider
import com.example.ui.components.ButtonVariant
import com.example.ui.components.FinTrackButton
import com.example.ui.theme.BodyRegular
import com.example.ui.theme.FinTrackMotion
import com.example.ui.theme.HeroFinancialDisplay
import com.example.ui.theme.LabelBadgeMedium
import com.example.ui.theme.MicroMetadata
import com.example.ui.theme.RadiusMedium
import com.example.ui.theme.SectionHeadline
import com.example.ui.theme.ShapeFloatingActionButton
import com.example.ui.theme.ShapeGroupedContainer
import com.example.ui.theme.ShapePill
import com.example.ui.theme.ShapeSmall
import com.example.ui.theme.Space12
import com.example.ui.theme.Space16
import com.example.ui.theme.Space20
import com.example.ui.theme.Space24
import com.example.ui.theme.Space32
import com.example.ui.theme.Space4
import com.example.ui.theme.Space8
import com.example.ui.theme.isReducedMotionEnabled
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.launch
import java.security.MessageDigest
import java.util.UUID

@Composable
fun AuthScreen(
    authState: AuthState,
    onSignInWithGoogle: (idToken: String) -> Unit,
    onSignInWithTestUid: (testUid: String) -> Unit,
    onAuthError: (errorMessage: String) -> Unit,
    onClearError: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var testUidInput by remember { mutableStateOf("user_account_1") }
    val isReducedMotion = isReducedMotionEnabled() || LocalInspectionMode.current

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .testTag("auth_screen_surface"),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 440.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(Space24),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Expressive Hero Identity Badge
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), ShapeFloatingActionButton),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Lock,
                        contentDescription = "Authentication Lock",
                        modifier = Modifier.size(36.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.height(Space16))

                Text(
                    text = "FinTrack",
                    style = HeroFinancialDisplay,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.semantics { heading() }
                )

                Spacer(modifier = Modifier.height(Space4))

                Text(
                    text = "Your household finances, clearly organized",
                    style = BodyRegular,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(Space32))

                // Expressive Grouped Container Surface
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("auth_card"),
                    shape = ShapeGroupedContainer,
                    color = MaterialTheme.colorScheme.surfaceContainerLow
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(Space24),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        AnimatedContent(
                            targetState = authState,
                            transitionSpec = {
                                if (isReducedMotion) {
                                    ContentTransform(
                                        targetContentEnter = EnterTransition.None,
                                        initialContentExit = ExitTransition.None
                                    )
                                } else {
                                    (fadeIn(animationSpec = FinTrackMotion.InteractiveSpring) +
                                            scaleIn(initialScale = 0.98f, animationSpec = FinTrackMotion.InteractiveSpring))
                                        .togetherWith(
                                            fadeOut(animationSpec = FinTrackMotion.interactiveSpring()) +
                                                    scaleOut(targetScale = 0.98f, animationSpec = FinTrackMotion.interactiveSpring())
                                        )
                                }
                            },
                            label = "AuthStateTransition"
                        ) { state ->
                            when (state) {
                                is AuthState.SigningIn -> {
                                    Column(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        CircularProgressIndicator(
                                            modifier = Modifier
                                                .size(40.dp)
                                                .testTag("auth_signing_in_indicator"),
                                            color = MaterialTheme.colorScheme.primary,
                                            strokeWidth = 3.dp
                                        )
                                        Spacer(modifier = Modifier.height(Space16))
                                        Text(
                                            text = "Signing in securely...",
                                            style = BodyRegular,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }

                                is AuthState.AuthError -> {
                                    Column(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(48.dp)
                                                .background(MaterialTheme.colorScheme.errorContainer, ShapeSmall),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.Warning,
                                                contentDescription = "Auth Error",
                                                tint = MaterialTheme.colorScheme.error,
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(Space12))
                                        Text(
                                            text = "Authentication Error",
                                            style = SectionHeadline,
                                            color = MaterialTheme.colorScheme.error,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(modifier = Modifier.height(Space8))
                                        Text(
                                            text = state.message,
                                            style = BodyRegular,
                                            textAlign = TextAlign.Center,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.testTag("auth_error_message")
                                        )
                                        Spacer(modifier = Modifier.height(Space20))
                                        FinTrackButton(
                                            text = "Try Again",
                                            onClick = onClearError,
                                            variant = ButtonVariant.PRIMARY,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .testTag("auth_retry_button")
                                        )
                                    }
                                }

                                else -> { // SignedOut or default
                                    Column(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = "Sign in to view and manage your financial records. Your data is isolated and protected by account identity.",
                                            style = BodyRegular,
                                            textAlign = TextAlign.Center,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )

                                        Spacer(modifier = Modifier.height(Space20))

                                        // Google Sign In Button
                                        FinTrackButton(
                                            onClick = {
                                                scope.launch {
                                                    triggerGoogleSignIn(
                                                        context = context,
                                                        onSignInWithGoogle = onSignInWithGoogle,
                                                        onAuthError = onAuthError
                                                    )
                                                }
                                            },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .testTag("google_sign_in_button"),
                                            variant = ButtonVariant.PRIMARY
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Filled.AccountCircle,
                                                    contentDescription = "Google Icon",
                                                    modifier = Modifier.size(22.dp),
                                                    tint = MaterialTheme.colorScheme.onPrimary
                                                )
                                                Spacer(modifier = Modifier.width(Space8))
                                                Text(
                                                    text = "Sign in with Google",
                                                    style = LabelBadgeMedium,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Test / Switch Account Section (Debug Builds Only)
                        if (com.example.BuildConfig.DEBUG) {
                            Spacer(modifier = Modifier.height(Space24))
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                                thickness = 1.dp
                            )
                            Spacer(modifier = Modifier.height(Space16))
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "Direct Account Sign-In (UID Testing & Cache Isolation)",
                                    style = MicroMetadata,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(Space8))
                                OutlinedTextField(
                                    value = testUidInput,
                                    onValueChange = { testUidInput = it },
                                    label = { Text("Account UID", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                                    singleLine = true,
                                    shape = RoundedCornerShape(RadiusMedium),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("test_uid_input_field")
                                )
                                Spacer(modifier = Modifier.height(Space8))
                                FinTrackButton(
                                    text = "Sign In as $testUidInput",
                                    onClick = {
                                        if (testUidInput.isNotBlank()) {
                                            onSignInWithTestUid(testUidInput.trim())
                                        }
                                    },
                                    variant = ButtonVariant.SECONDARY,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("test_uid_sign_in_button")
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private suspend fun triggerGoogleSignIn(
    context: Context,
    onSignInWithGoogle: (idToken: String) -> Unit,
    onAuthError: (errorMessage: String) -> Unit
) {
    val webClientId = GoogleSignInConfigProvider.getWebClientId(context)
    if (webClientId == null) {
        onAuthError("Google Sign-In is not configured. Web Client ID is missing.")
        return
    }

    try {
        val credentialManager = CredentialManager.create(context)
        val rawNonce = UUID.randomUUID().toString()
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(rawNonce.toByteArray())
        val hashedNonce = digest.fold("") { str, it -> str + "%02x".format(it) }

        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(webClientId)
            .setNonce(hashedNonce)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        val result = credentialManager.getCredential(
            request = request,
            context = context
        )

        val credential = result.credential
        if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
            val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
            val idToken = googleIdTokenCredential.idToken
            onSignInWithGoogle(idToken)
        } else {
            onAuthError("Google sign-in credential type not supported.")
        }
    } catch (e: GetCredentialException) {
        onAuthError("Sign-in cancelled or unavailable. Please check Google Play Services and Firebase Console Web Client ID configuration.")
    } catch (e: Exception) {
        onAuthError("Google sign-in failed. Please verify device configuration.")
    }
}
