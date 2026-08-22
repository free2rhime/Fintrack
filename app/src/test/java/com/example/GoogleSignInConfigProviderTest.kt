package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.repository.FirebaseAuthRepository
import com.example.data.repository.GoogleSignInConfigProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class GoogleSignInConfigProviderTest {

    @Test
    fun testDefaultPlaceholderReturnsNull() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        // The default string resource is "YOUR_WEB_CLIENT_ID.apps.googleusercontent.com"
        val resolved = GoogleSignInConfigProvider.getWebClientId(context)
        assertNull("Placeholder starting with YOUR_WEB_CLIENT_ID must return null", resolved)
        
        // Also test through FirebaseAuthRepository helper
        val resolvedFromRepo = FirebaseAuthRepository.getWebClientId(context)
        assertNull("FirebaseAuthRepository helper must also return null for placeholder", resolvedFromRepo)
    }

    @Test
    fun testValidWebClientIdReturnsString() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val validClientId = "123456789-abcdef.apps.googleusercontent.com"
        val mockResources = object : android.content.res.Resources(
            context.resources.assets,
            context.resources.displayMetrics,
            context.resources.configuration
        ) {
            override fun getIdentifier(name: String?, defType: String?, defPackage: String?): Int {
                return if (name == "default_web_client_id") 12345 else super.getIdentifier(name, defType, defPackage)
            }
            override fun getString(id: Int): String {
                return if (id == 12345) validClientId else super.getString(id)
            }
        }
        val testContext = object : android.content.ContextWrapper(context) {
            override fun getResources(): android.content.res.Resources = mockResources
        }
        val resolved = GoogleSignInConfigProvider.getWebClientId(testContext)
        assertEquals(validClientId, resolved)
    }

    @Test
    fun testBlankWebClientIdReturnsNull() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val mockResources = object : android.content.res.Resources(
            context.resources.assets,
            context.resources.displayMetrics,
            context.resources.configuration
        ) {
            override fun getIdentifier(name: String?, defType: String?, defPackage: String?): Int {
                return if (name == "default_web_client_id") 12345 else super.getIdentifier(name, defType, defPackage)
            }
            override fun getString(id: Int): String {
                return if (id == 12345) "   " else super.getString(id)
            }
        }
        val testContext = object : android.content.ContextWrapper(context) {
            override fun getResources(): android.content.res.Resources = mockResources
        }
        val resolved = GoogleSignInConfigProvider.getWebClientId(testContext)
        assertNull("Blank web client ID must return null", resolved)
    }
}

