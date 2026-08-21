package com.example.data.repository

import android.content.Context

/**
 * Safely resolves Google Sign-In Web Client ID configuration without compile-time
 * dependency on R.string.default_web_client_id.
 */
object GoogleSignInConfigProvider {

    /**
     * Resolves the Web Client ID dynamically from resources.
     *
     * Returns null if:
     * - The resource identifier does not exist (resId == 0)
     * - The resolved string value is blank
     * - The resolved string value starts with "YOUR_WEB_CLIENT_ID" (unconfigured placeholder)
     */
    fun getWebClientId(context: Context): String? {
        return try {
            val resId = context.resources.getIdentifier(
                "default_web_client_id",
                "string",
                context.packageName
            )
            if (resId == 0) {
                return null
            }
            val value = context.getString(resId).trim()
            if (value.isBlank() || value.startsWith("YOUR_WEB_CLIENT_ID")) {
                null
            } else {
                value
            }
        } catch (_: Exception) {
            null
        }
    }
}
