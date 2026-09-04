package com.example.ui.screens

import android.content.Context
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
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
import androidx.compose.ui.platform.testTag
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
import com.example.ui.components.FinTrackCard
import com.example.ui.theme.BodyRegular
import com.example.ui.theme.CanvasDark
import com.example.ui.theme.CobaltBlue
import com.example.ui.theme.ExpenseCoral
import com.example.ui.theme.HeroFinancialDisplay
import com.example.ui.theme.LabelBadgeMedium
import com.example.ui.theme.MicroMetadata
import com.example.ui.theme.RadiusLarge
import com.example.ui.theme.RadiusMedium
import com.example.ui.theme.SectionHeadline
import com.example.ui.theme.Space12
import com.example.ui.theme.Space16
import com.example.ui.theme.Space20
import com.example.ui.theme.Space24
import com.example.ui.theme.Space32
import com.example.ui.theme.Space4
import com.example.ui.theme.Space8
import com.example.ui.theme.SurfaceContainerDark
import com.example.ui.theme.SurfaceContainerHighDark
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
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

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .testTag("auth_screen_surface"),
        color = CanvasDark
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(Space24),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .background(CobaltBlue.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Lock,
                    contentDescription = "Authentication Lock",
                    modifier = Modifier.size(32.dp),
                    tint = CobaltBlue
                )
            }

            Spacer(modifier = Modifier.height(Space16))

            Text(
                text = "FinTrack",
                style = HeroFinancialDisplay,
                color = TextPrimary
            )

            Spacer(modifier = Modifier.height(Space4))

            Text(
                text = "Secure Personal Finance Manager",
                style = BodyRegular,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(Space32))

            FinTrackCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("auth_card"),
                shape = RoundedCornerShape(RadiusLarge),
                containerColor = SurfaceDark,
                contentPadding = Space20
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    when (authState) {
                        is AuthState.SigningIn -> {
                            CircularProgressIndicator(
                                modifier = Modifier
                                    .size(36.dp)
                                    .testTag("auth_signing_in_indicator"),
                                color = CobaltBlue,
                                strokeWidth = 3.dp
                            )
                            Spacer(modifier = Modifier.height(Space16))
                            Text(
                                text = "Signing in securely...",
                                style = BodyRegular,
                                fontWeight = FontWeight.Medium,
                                color = TextPrimary
                            )
                        }

                        is AuthState.AuthError -> {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(ExpenseCoral.copy(alpha = 0.15f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Warning,
                                    contentDescription = "Auth Error",
                                    tint = ExpenseCoral,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(Space12))
                            Text(
                                text = "Authentication Error",
                                style = SectionHeadline,
                                color = ExpenseCoral,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(Space8))
                            Text(
                                text = authState.message,
                                style = BodyRegular,
                                textAlign = TextAlign.Center,
                                color = TextSecondary,
                                modifier = Modifier.testTag("auth_error_message")
                            )
                            Spacer(modifier = Modifier.height(Space20))
                            FinTrackButton(
                                text = "Try Again",
                                onClick = onClearError,
                                variant = ButtonVariant.PRIMARY,
                                modifier = Modifier.testTag("auth_retry_button")
                            )
                        }

                        else -> { // SignedOut or default
                            Text(
                                text = "Sign in to view and manage your financial records. Your data is isolated and protected by account identity.",
                                style = BodyRegular,
                                textAlign = TextAlign.Center,
                                color = TextSecondary
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
                                        modifier = Modifier.size(20.dp),
                                        tint = Color.White
                                    )
                                    Spacer(modifier = Modifier.width(Space8))
                                    Text(
                                        text = "Sign in with Google",
                                        style = LabelBadgeMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(Space24))

                            // Test / Switch Account Section (Debug Builds Only)
                            if (com.example.BuildConfig.DEBUG) {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "Direct Account Sign-In (UID Testing & Cache Isolation)",
                                        style = MicroMetadata,
                                        color = TextSecondary
                                    )
                                    Spacer(modifier = Modifier.height(Space8))
                                    OutlinedTextField(
                                        value = testUidInput,
                                        onValueChange = { testUidInput = it },
                                        label = { Text("Account UID", color = TextSecondary) },
                                        singleLine = true,
                                        shape = RoundedCornerShape(RadiusMedium),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = CobaltBlue,
                                            unfocusedBorderColor = SurfaceContainerHighDark,
                                            focusedTextColor = TextPrimary,
                                            unfocusedTextColor = TextPrimary,
                                            focusedContainerColor = SurfaceContainerDark,
                                            unfocusedContainerColor = SurfaceContainerDark
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
