package com.example.data.repository

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class SyncDiagnosticRecord(
    val userUid: String?,
    val operation: String,
    val householdId: String? = null,
    val exceptionCode: String?,
    val exceptionMessage: String?,
    val stackTraceSnippet: String?,
    val timestampMillis: Long = System.currentTimeMillis()
) {
    val formattedTime: String
        get() = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(timestampMillis))
}

object SyncDiagnosticsHolder {
    private val _lastError = MutableStateFlow<SyncDiagnosticRecord?>(null)
    val lastError: StateFlow<SyncDiagnosticRecord?> = _lastError.asStateFlow()

    fun recordError(
        userUid: String?,
        operation: String,
        householdId: String? = null,
        throwable: Throwable
    ) {
        val codeStr = if (throwable is com.google.firebase.firestore.FirebaseFirestoreException) {
            "${throwable.code.name} (${throwable.code.value()})"
        } else {
            throwable.javaClass.simpleName
        }
        val stackTrace = throwable.stackTraceToString().lines().take(15).joinToString("\n")
        _lastError.value = SyncDiagnosticRecord(
            userUid = userUid,
            operation = operation,
            householdId = householdId,
            exceptionCode = codeStr,
            exceptionMessage = throwable.message ?: "No message",
            stackTraceSnippet = stackTrace,
            timestampMillis = System.currentTimeMillis()
        )
    }

    fun clear() {
        _lastError.value = null
    }
}
