package com.example.data.repository

import kotlinx.coroutines.flow.StateFlow

sealed interface AuthState {
    object SignedOut : AuthState
    object SigningIn : AuthState
    data class SignedIn(
        val userUid: String,
        val email: String? = null,
        val displayName: String? = null
    ) : AuthState
    data class AuthError(val message: String) : AuthState
}

interface AuthRepository {
    val authState: StateFlow<AuthState>
    suspend fun signInWithGoogleCredential(idToken: String): Result<String>
    suspend fun signInWithTestUid(testUid: String, email: String? = null, displayName: String? = null): Result<String>
    suspend fun signOut()
    fun getCurrentUserUid(): String?
    fun clearError()
    fun setAuthError(message: String)
}
