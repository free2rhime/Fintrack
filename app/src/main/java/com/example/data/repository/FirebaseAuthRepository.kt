package com.example.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await

class FirebaseAuthRepository(
    private val firebaseAuthSupplier: () -> FirebaseAuth? = {
        try {
            FirebaseAuth.getInstance()
        } catch (_: Exception) {
            null
        }
    }
) : AuthRepository {

    private val _authState = MutableStateFlow<AuthState>(AuthState.SignedOut)
    override val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private var authStateListener: FirebaseAuth.AuthStateListener? = null

    init {
        val auth = firebaseAuthSupplier()
        if (auth != null) {
            try {
                val currentUser = auth.currentUser
                if (currentUser != null) {
                    _authState.value = AuthState.SignedIn(
                        userUid = currentUser.uid,
                        email = currentUser.email,
                        displayName = currentUser.displayName
                    )
                }

                authStateListener = FirebaseAuth.AuthStateListener { firebase ->
                    val user = firebase.currentUser
                    if (user != null) {
                        _authState.value = AuthState.SignedIn(
                            userUid = user.uid,
                            email = user.email,
                            displayName = user.displayName
                        )
                    } else {
                        if (_authState.value !is AuthState.AuthError && _authState.value !is AuthState.SigningIn) {
                            _authState.value = AuthState.SignedOut
                        }
                    }
                }
                auth.addAuthStateListener(authStateListener!!)
            } catch (_: Exception) {
                _authState.value = AuthState.SignedOut
            }
        } else {
            _authState.value = AuthState.SignedOut
        }
    }

    override suspend fun signInWithGoogleCredential(idToken: String): Result<String> {
        _authState.value = AuthState.SigningIn
        val auth = firebaseAuthSupplier()
        if (auth == null) {
            val errorMsg = "Firebase Authentication is not configured on this device."
            _authState.value = AuthState.AuthError(errorMsg)
            return Result.failure(Exception(errorMsg))
        }

        return try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val authResult = auth.signInWithCredential(credential).await()
            val user = authResult.user
            if (user != null) {
                val state = AuthState.SignedIn(
                    userUid = user.uid,
                    email = user.email,
                    displayName = user.displayName
                )
                _authState.value = state
                Result.success(user.uid)
            } else {
                val errorMsg = "Authentication failed: User profile unavailable."
                _authState.value = AuthState.AuthError(errorMsg)
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            val sanitizedMsg = sanitizeAuthErrorMessage(e)
            _authState.value = AuthState.AuthError(sanitizedMsg)
            Result.failure(Exception(sanitizedMsg))
        }
    }

    override suspend fun signInWithTestUid(
        testUid: String,
        email: String?,
        displayName: String?
    ): Result<String> {
        _authState.value = AuthState.SigningIn
        val state = AuthState.SignedIn(
            userUid = testUid,
            email = email ?: "$testUid@example.com",
            displayName = displayName ?: "User $testUid"
        )
        _authState.value = state
        return Result.success(testUid)
    }

    override suspend fun signOut() {
        try {
            firebaseAuthSupplier()?.signOut()
        } catch (_: Exception) {}
        _authState.value = AuthState.SignedOut
    }

    override fun getCurrentUserUid(): String? {
        return (_authState.value as? AuthState.SignedIn)?.userUid
            ?: try { firebaseAuthSupplier()?.currentUser?.uid } catch (_: Exception) { null }
    }

    override fun clearError() {
        if (_authState.value is AuthState.AuthError) {
            _authState.value = AuthState.SignedOut
        }
    }

    private fun sanitizeAuthErrorMessage(e: Exception): String {
        val msg = e.localizedMessage ?: ""
        return when {
            msg.contains("API_NOT_CONNECTED") -> "Google Play Services is not connected."
            msg.contains("DEVELOPER_ERROR") -> "Google Sign-In configuration error. Please verify SHA fingerprint and Web Client ID in Firebase Console."
            msg.contains("NETWORK_ERROR") -> "Network connection error. Please try again."
            msg.contains("INVALID_ID_TOKEN") -> "Invalid authentication token received."
            else -> "Google sign-in could not be completed. Please try again."
        }
    }
}
