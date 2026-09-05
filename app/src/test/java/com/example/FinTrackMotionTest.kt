package com.example

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.ui.components.BadgeVariant
import com.example.ui.components.CurrencyToggle
import com.example.ui.components.FinTrackSegmentedControl
import com.example.ui.components.FinTrackStatusBadge
import com.example.ui.theme.FinTrackMotion
import com.example.ui.theme.FinTrackTheme
import com.example.ui.theme.LocalFinTrackMotion
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class FinTrackMotionTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testMotionTokenConstants() {
        assertEquals(150, FinTrackMotion.DurationFast)
        assertEquals(200, FinTrackMotion.DurationStandard)
        assertEquals(250, FinTrackMotion.DurationEmphasized)
        assertEquals(1000, FinTrackMotion.DurationSyncSpin)
        assertEquals(FastOutSlowInEasing, FinTrackMotion.StandardEasing)
        assertEquals(LinearEasing, FinTrackMotion.LinearCurve)
    }

    @Test
    fun testTweenSpecs() {
        val fast = FinTrackMotion.fastTween<Float>()
        assertEquals(150, fast.durationMillis)
        assertEquals(FastOutSlowInEasing, fast.easing)

        val standard = FinTrackMotion.standardTween<Float>()
        assertEquals(200, standard.durationMillis)
        assertEquals(FastOutSlowInEasing, standard.easing)

        val emphasized = FinTrackMotion.emphasizedTween<Float>()
        assertEquals(250, emphasized.durationMillis)
        assertEquals(FastOutSlowInEasing, emphasized.easing)
    }

    @Test
    fun testContentFadeSpec() {
        val fade = FinTrackMotion.contentFade()
        assertNotNull(fade)
    }

    @Test
    fun testCompositionLocalMotionTokens() {
        composeTestRule.setContent {
            FinTrackTheme {
                val motion = LocalFinTrackMotion.current
                assertEquals(150, motion.durationFast)
                assertEquals(200, motion.durationStandard)
                assertEquals(250, motion.durationEmphasized)
                assertEquals(1000, motion.durationSyncSpin)
            }
        }
    }

    @Test
    fun testFinTrackStatusBadgeStaticStates() {
        composeTestRule.setContent {
            FinTrackTheme {
                FinTrackStatusBadge(label = "Synced", variant = BadgeVariant.SUCCESS)
                FinTrackStatusBadge(label = "Warning", variant = BadgeVariant.WARNING)
                FinTrackStatusBadge(label = "Error", variant = BadgeVariant.ERROR)
            }
        }

        composeTestRule.onNodeWithText("Synced").assertIsDisplayed()
        composeTestRule.onNodeWithText("Warning").assertIsDisplayed()
        composeTestRule.onNodeWithText("Error").assertIsDisplayed()
    }

    @Test
    fun testFinTrackStatusBadgeSyncingState() {
        composeTestRule.setContent {
            FinTrackTheme {
                FinTrackStatusBadge(label = "Syncing...", variant = BadgeVariant.SYNCING)
            }
        }

        composeTestRule.onNodeWithText("Syncing...").assertIsDisplayed()
    }

    @Test
    fun testCurrencyToggleWithMotionFoundation() {
        var selected = "RON"

        composeTestRule.setContent {
            FinTrackTheme {
                CurrencyToggle(
                    selectedCurrency = selected,
                    onCurrencyChanged = { selected = it }
                )
            }
        }

        composeTestRule.onNodeWithTag("currency_toggle_RON").assertIsDisplayed().assertIsSelected()
        composeTestRule.onNodeWithTag("currency_toggle_EUR").assertIsDisplayed()

        composeTestRule.onNodeWithTag("currency_toggle_EUR").performClick()
        assertEquals("EUR", selected)
    }

    @Test
    fun testFinTrackSegmentedControlWithMotionFoundation() {
        var selectedIndex = 0
        val items = listOf("Option A", "Option B", "Option C")

        composeTestRule.setContent {
            FinTrackTheme {
                FinTrackSegmentedControl(
                    items = items,
                    selectedIndex = selectedIndex,
                    onItemSelected = { selectedIndex = it }
                )
            }
        }

        composeTestRule.onNodeWithText("Option A").assertIsDisplayed()
        composeTestRule.onNodeWithText("Option B").assertIsDisplayed().performClick()
        assertEquals(1, selectedIndex)
    }
}
