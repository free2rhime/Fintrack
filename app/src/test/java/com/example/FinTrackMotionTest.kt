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
        assertEquals(0, FinTrackMotion.DurationInstant)
        assertEquals(120, FinTrackMotion.DurationMicro)
        assertEquals(150, FinTrackMotion.DurationFast)
        assertEquals(200, FinTrackMotion.DurationStandard)
        assertEquals(220, FinTrackMotion.DurationV2Standard)
        assertEquals(250, FinTrackMotion.DurationEmphasized)
        assertEquals(350, FinTrackMotion.DurationV2Emphasized)
        assertEquals(600, FinTrackMotion.DurationChartSweep)
        assertEquals(1000, FinTrackMotion.DurationSyncSpin)
        assertEquals(FastOutSlowInEasing, FinTrackMotion.StandardEasing)
        assertEquals(FastOutSlowInEasing, FinTrackMotion.StandardDecelerate)
        assertEquals(LinearEasing, FinTrackMotion.LinearCurve)
        assertNotNull(FinTrackMotion.EmphasizedCubic)
    }

    @Test
    fun testSpringSpecs() {
        assertNotNull(FinTrackMotion.InteractiveSpring)
        assertNotNull(FinTrackMotion.InteractiveSpringDp)
        assertNotNull(FinTrackMotion.ContentSpring)
        assertNotNull(FinTrackMotion.ContentSpringDp)
        assertNotNull(FinTrackMotion.interactiveSpring<Float>())
        assertNotNull(FinTrackMotion.contentSpring<Float>())
    }

    @Test
    fun testTweenSpecs() {
        val micro = FinTrackMotion.microTween<Float>()
        assertEquals(120, micro.durationMillis)

        val fast = FinTrackMotion.fastTween<Float>()
        assertEquals(150, fast.durationMillis)
        assertEquals(FastOutSlowInEasing, fast.easing)

        val standard = FinTrackMotion.standardTween<Float>()
        assertEquals(200, standard.durationMillis)
        assertEquals(FastOutSlowInEasing, standard.easing)

        val standardV2 = FinTrackMotion.standardV2Tween<Float>()
        assertEquals(220, standardV2.durationMillis)

        val emphasized = FinTrackMotion.emphasizedTween<Float>()
        assertEquals(250, emphasized.durationMillis)
        assertEquals(FastOutSlowInEasing, emphasized.easing)

        val emphasizedV2 = FinTrackMotion.emphasizedV2Tween<Float>()
        assertEquals(350, emphasizedV2.durationMillis)
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
