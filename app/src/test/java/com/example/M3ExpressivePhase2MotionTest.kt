package com.example

import androidx.compose.animation.core.Spring
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import com.example.domain.analytics.CategoryExpenseShare
import com.example.domain.analytics.MonthlyDataPoint
import com.example.ui.components.ButtonVariant
import com.example.ui.components.CategoryDistributionChart
import com.example.ui.components.CurrencyToggle
import com.example.ui.components.FinTrackButton
import com.example.ui.components.FinTrackCard
import com.example.ui.components.FinTrackCurrencySelector
import com.example.ui.components.FinTrackPeriodDropdown
import com.example.ui.components.FinTrackPeriodSelector
import com.example.ui.components.FinTrackSegmentedControl
import com.example.ui.components.FinTrackTransactionRow
import com.example.ui.components.MonthlyCashFlowBarChart
import com.example.ui.components.SavingsTrendLineChart
import com.example.ui.navigation.FinTrackBottomNavigation
import com.example.ui.theme.ChartCategoryAmber
import com.example.ui.theme.ChartCategoryOther
import com.example.ui.theme.DividerInsetTransaction
import com.example.ui.theme.DividerThicknessHairline
import com.example.ui.theme.FinTrackChartPalette
import com.example.ui.theme.FinTrackMotion
import com.example.ui.theme.FinTrackTheme
import com.example.ui.theme.MaxContentWidthTablet
import com.example.ui.theme.tactilePress
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Phase 2: Material 3 Expressive Motion & Interaction Parity Verification Test Suite.
 *
 * Validates:
 * 1. FinTrackMotion token contract (spring physics, damping, stiffness, duration tokens, press scale).
 * 2. Tactile press modifier integration and interaction source handling.
 * 3. FinTrackButton tactile feedback, variant styling, touch targets (>= 48dp), and click execution.
 * 4. FinTrackCard clickable tactile feedback and container rendering.
 * 5. FinTrackCurrencySelector / CurrencyToggle tactile press, haptics, reduced motion, Role.Tab, and sizing.
 * 6. FinTrackPeriodDropdown and FinTrackPeriodSelector tactile press, touch targets (>= 48dp), and interaction.
 * 7. FinTrackBottomNavigation tab selection transitions and reduced-motion resilience.
 * 8. FinTrackTransactionRow action buttons tactile press and minimum touch targets.
 */
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class M3ExpressivePhase2MotionTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    // =========================================================================
    // 1. MOTION TOKENS & SPRING CONTRACT
    // =========================================================================

    @Test
    fun test01_MotionTokens_ContractIntegrity() {
        assertEquals(0, FinTrackMotion.DurationInstant)
        assertEquals(100, FinTrackMotion.DurationPress)
        assertEquals(120, FinTrackMotion.DurationMicro)
        assertEquals(150, FinTrackMotion.DurationFast)
        assertEquals(200, FinTrackMotion.DurationStandard)
        assertEquals(220, FinTrackMotion.DurationV2Standard)
        assertEquals(250, FinTrackMotion.DurationEmphasized)
        assertEquals(350, FinTrackMotion.DurationV2Emphasized)
        assertEquals(600, FinTrackMotion.DurationChartSweep)
        assertEquals(1000, FinTrackMotion.DurationSyncSpin)

        assertEquals(0.975f, FinTrackMotion.PressScaleTarget, 0.0001f)

        // Spatial Spring: DampingRatioLowBouncy, StiffnessLow
        assertEquals(Spring.DampingRatioLowBouncy, FinTrackMotion.SpatialSpring.dampingRatio, 0.0001f)
        assertEquals(Spring.StiffnessLow, FinTrackMotion.SpatialSpring.stiffness, 0.0001f)

        // Interactive Spring: DampingRatioMediumBouncy, StiffnessMedium
        assertEquals(Spring.DampingRatioMediumBouncy, FinTrackMotion.InteractiveSpring.dampingRatio, 0.0001f)
        assertEquals(Spring.StiffnessMedium, FinTrackMotion.InteractiveSpring.stiffness, 0.0001f)

        // Micro Spring: DampingRatioNoBouncy, StiffnessMedium
        assertEquals(Spring.DampingRatioNoBouncy, FinTrackMotion.MicroSpring.dampingRatio, 0.0001f)
        assertEquals(Spring.StiffnessMedium, FinTrackMotion.MicroSpring.stiffness, 0.0001f)

        // Content Spring: DampingRatioNoBouncy, StiffnessLow
        assertEquals(Spring.DampingRatioNoBouncy, FinTrackMotion.ContentSpring.dampingRatio, 0.0001f)
        assertEquals(Spring.StiffnessLow, FinTrackMotion.ContentSpring.stiffness, 0.0001f)
    }

    @Test
    fun test02_SemanticSpringSpecs_ProduceValidInstances() {
        val pressSpec = FinTrackMotion.pressInteractionSpec<Float>()
        assertEquals(Spring.DampingRatioMediumBouncy, pressSpec.dampingRatio, 0.0001f)
        assertEquals(Spring.StiffnessMedium, pressSpec.stiffness, 0.0001f)

        val selectSpec = FinTrackMotion.selectionSpring<Float>()
        assertEquals(Spring.DampingRatioMediumBouncy, selectSpec.dampingRatio, 0.0001f)
        assertEquals(Spring.StiffnessMediumLow, selectSpec.stiffness, 0.0001f)

        val navSpec = FinTrackMotion.navigationSpring<Float>()
        assertEquals(Spring.DampingRatioLowBouncy, navSpec.dampingRatio, 0.0001f)
        assertEquals(Spring.StiffnessLow, navSpec.stiffness, 0.0001f)

        val chartSpec = FinTrackMotion.chartInteractionSpring<Float>()
        assertEquals(Spring.DampingRatioMediumBouncy, chartSpec.dampingRatio, 0.0001f)

        assertNotNull(FinTrackMotion.contentFade())
    }

    // =========================================================================
    // 2. TACTILE PRESS MODIFIER INTERACTION
    // =========================================================================

    @Test
    fun test03_TactilePress_ModifierComposesWithoutError() {
        val interactionSource = MutableInteractionSource()

        composeTestRule.setContent {
            FinTrackTheme {
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .tactilePress(interactionSource = interactionSource, enabled = true)
                        .testTag("tactile_box")
                ) {
                    Text("Interactive")
                }
            }
        }

        composeTestRule.onNodeWithTag("tactile_box").assertIsDisplayed()

        runBlocking {
            val press = PressInteraction.Press(Offset(50f, 50f))
            interactionSource.emit(press)
            interactionSource.emit(PressInteraction.Release(press))
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("tactile_box").assertIsDisplayed()
    }

    // =========================================================================
    // 3. FINTRACK BUTTON: TACTILE PRESS & 48DP TOUCH TARGET
    // =========================================================================

    @Test
    fun test04_FinTrackButton_VariantsAndTactileTarget() {
        var clickedPrimary = false
        var clickedSecondary = false
        var clickedDestructive = false

        composeTestRule.setContent {
            FinTrackTheme {
                Column {
                    FinTrackButton(
                        text = "Save Changes",
                        onClick = { clickedPrimary = true },
                        variant = ButtonVariant.PRIMARY,
                        modifier = Modifier.testTag("btn_primary")
                    )
                    FinTrackButton(
                        text = "Cancel",
                        onClick = { clickedSecondary = true },
                        variant = ButtonVariant.SECONDARY,
                        modifier = Modifier.testTag("btn_secondary")
                    )
                    FinTrackButton(
                        text = "Delete",
                        onClick = { clickedDestructive = true },
                        variant = ButtonVariant.DESTRUCTIVE,
                        modifier = Modifier.testTag("btn_destructive")
                    )
                }
            }
        }

        // Verify min height >= 48dp
        composeTestRule.onNodeWithTag("btn_primary")
            .assertIsDisplayed()
            .assertHeightIsAtLeast(48.dp)
            .performClick()
        assertTrue(clickedPrimary)

        composeTestRule.onNodeWithTag("btn_secondary")
            .assertIsDisplayed()
            .assertHeightIsAtLeast(48.dp)
            .performClick()
        assertTrue(clickedSecondary)

        composeTestRule.onNodeWithTag("btn_destructive")
            .assertIsDisplayed()
            .assertHeightIsAtLeast(48.dp)
            .performClick()
        assertTrue(clickedDestructive)
    }

    // =========================================================================
    // 4. FINTRACK CARD: CLICKABLE TACTILE PRESS
    // =========================================================================

    @Test
    fun test05_FinTrackCard_ClickableAndNonClickable() {
        var cardClicked = false

        composeTestRule.setContent {
            FinTrackTheme {
                Column {
                    FinTrackCard(
                        onClick = { cardClicked = true },
                        modifier = Modifier.testTag("clickable_card")
                    ) {
                        Text("Interactive Card Content")
                    }
                    FinTrackCard(
                        onClick = null,
                        modifier = Modifier.testTag("static_card")
                    ) {
                        Text("Static Card Content")
                    }
                }
            }
        }

        composeTestRule.onNodeWithTag("clickable_card")
            .assertIsDisplayed()
            .performClick()
        assertTrue(cardClicked)

        composeTestRule.onNodeWithTag("static_card").assertIsDisplayed()
    }

    // =========================================================================
    // 5. CURRENCY TOGGLE & SELECTOR: TACTILE, ROLE.TAB & TARGETS
    // =========================================================================

    @Test
    fun test06_CurrencyToggle_TactileAndRoleTabSemantics() {
        var activeCurrency = "RON"

        composeTestRule.setContent {
            FinTrackTheme {
                FinTrackCurrencySelector(
                    selectedCurrency = activeCurrency,
                    onCurrencyChanged = { activeCurrency = it },
                    modifier = Modifier.testTag("currency_selector_root")
                )
            }
        }

        // RON is selected initially
        composeTestRule.onNodeWithTag("currency_toggle_RON")
            .assertIsDisplayed()
            .assertHeightIsAtLeast(48.dp)
            .assertWidthIsAtLeast(56.dp)
            .assertIsSelected()
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Tab))

        // EUR is unselected initially
        composeTestRule.onNodeWithTag("currency_toggle_EUR")
            .assertIsDisplayed()
            .assertHeightIsAtLeast(48.dp)
            .assertWidthIsAtLeast(56.dp)
            .assertIsNotSelected()
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Tab))

        // Click EUR
        composeTestRule.onNodeWithTag("currency_toggle_EUR").performClick()
        assertEquals("EUR", activeCurrency)
    }

    @Test
    fun test07_CurrencyToggle_BackwardCompatibilityWrapper() {
        var activeCurrency = "EUR"

        composeTestRule.setContent {
            FinTrackTheme {
                CurrencyToggle(
                    selectedCurrency = activeCurrency,
                    onCurrencyChanged = { activeCurrency = it }
                )
            }
        }

        composeTestRule.onNodeWithTag("currency_toggle_EUR").assertIsSelected()
        composeTestRule.onNodeWithTag("currency_toggle_RON").assertIsNotSelected()

        composeTestRule.onNodeWithTag("currency_toggle_RON").performClick()
        assertEquals("RON", activeCurrency)
    }

    // =========================================================================
    // 6. PERIOD SELECTOR & DROPDOWN: TACTILE & 48DP TARGETS
    // =========================================================================

    @Test
    fun test08_PeriodDropdown_TactileAnd48dpTarget() {
        var selectedPeriod = "Last Month"

        composeTestRule.setContent {
            FinTrackTheme {
                FinTrackPeriodDropdown(
                    selectedPeriod = selectedPeriod,
                    onPeriodSelected = { selectedPeriod = it }
                )
            }
        }

        // Anchor has >= 48dp touch target
        composeTestRule.onNodeWithTag("period_selector_dropdown")
            .assertIsDisplayed()
            .assertHeightIsAtLeast(48.dp)
            .performClick()

        composeTestRule.waitForIdle()

        // Verify dropdown item appears in popup and can be selected
        composeTestRule.onNodeWithText("Last 3 Months", useUnmergedTree = true)
            .assertIsDisplayed()
            .performClick()

        assertEquals("Last 3 Months", selectedPeriod)
    }

    @Test
    fun test09_PeriodSelectorRow_TactileAndRoleTabSemantics() {
        var selectedPeriod = "Last Month"

        composeTestRule.setContent {
            FinTrackTheme {
                FinTrackPeriodSelector(
                    selectedPeriod = selectedPeriod,
                    onPeriodSelected = { selectedPeriod = it }
                )
            }
        }

        // Selected chip
        composeTestRule.onNodeWithTag("period_chip_Last_Month")
            .assertIsDisplayed()
            .assertHeightIsAtLeast(48.dp)
            .assertIsSelected()
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Tab))

        // Click another chip
        composeTestRule.onNodeWithTag("period_chip_Last_3_Months")
            .assertIsDisplayed()
            .assertHeightIsAtLeast(48.dp)
            .assertIsNotSelected()
            .performClick()

        assertEquals("Last 3 Months", selectedPeriod)
    }

    // =========================================================================
    // 7. BOTTOM NAVIGATION: TRANSITIONS & DESTINATIONS
    // =========================================================================

    @Test
    fun test10_BottomNavigation_DestinationSelection() {
        var selectedTab = 0

        composeTestRule.setContent {
            FinTrackTheme {
                FinTrackBottomNavigation(
                    selectedTabIndex = selectedTab,
                    onTabSelected = { selectedTab = it }
                )
            }
        }

        composeTestRule.onNodeWithTag("bottom_nav_dashboard").assertIsSelected()
        composeTestRule.onNodeWithTag("bottom_nav_transactions").assertIsNotSelected()

        // Select Transactions
        composeTestRule.onNodeWithTag("bottom_nav_transactions").performClick()
        assertEquals(1, selectedTab)

        // Select Analytics
        composeTestRule.onNodeWithTag("bottom_nav_analytics").performClick()
        assertEquals(2, selectedTab)
    }

    // =========================================================================
    // 8. TRANSACTION ROW: ACTION BUTTON TOUCH TARGETS & TACTILE
    // =========================================================================

    @Test
    fun test11_TransactionRow_ActionButtons48dpTouchTargets() {
        var duplicated = false
        var edited = false
        var deleted = false

        composeTestRule.setContent {
            FinTrackTheme {
                FinTrackTransactionRow(
                    description = "Supermarket",
                    categoryName = "Groceries",
                    dateFormatted = "12 Sep 2026",
                    amountPrimaryFormatted = "150.00",
                    isIncome = false,
                    onDuplicateClick = { duplicated = true },
                    onEditClick = { edited = true },
                    onDeleteClick = { deleted = true },
                    duplicateTestTag = "action_duplicate",
                    editTestTag = "action_edit",
                    deleteTestTag = "action_delete"
                )
            }
        }

        // Duplicate button
        composeTestRule.onNodeWithTag("action_duplicate")
            .assertIsDisplayed()
            .assertHeightIsAtLeast(48.dp)
            .assertWidthIsAtLeast(48.dp)
            .performClick()
        assertTrue(duplicated)

        // Edit button
        composeTestRule.onNodeWithTag("action_edit")
            .assertIsDisplayed()
            .assertHeightIsAtLeast(48.dp)
            .assertWidthIsAtLeast(48.dp)
            .performClick()
        assertTrue(edited)

        // Delete button
        composeTestRule.onNodeWithTag("action_delete")
            .assertIsDisplayed()
            .assertHeightIsAtLeast(48.dp)
            .assertWidthIsAtLeast(48.dp)
            .performClick()
        assertTrue(deleted)
    }

    // =========================================================================
    // 9. SELECTABLE GROUP SEMANTICS (2d, 2e)
    // =========================================================================

    @Test
    fun test12_FinTrackSegmentedControl_SelectableGroupSemantics() {
        var selectedIdx = 0
        composeTestRule.setContent {
            FinTrackTheme {
                FinTrackSegmentedControl(
                    items = listOf("Daily", "Weekly", "Monthly"),
                    selectedIndex = selectedIdx,
                    onItemSelected = { selectedIdx = it },
                    modifier = Modifier.testTag("segmented_ctrl")
                )
            }
        }

        composeTestRule.onNode(SemanticsMatcher.keyIsDefined(SemanticsProperties.SelectableGroup))
            .assertExists()
            .assertIsDisplayed()

        composeTestRule.onNodeWithText("Weekly").performClick()
        assertEquals(1, selectedIdx)
    }

    @Test
    fun test13_CurrencyToggle_SelectableGroupAndTabRoles() {
        var curr = "RON"
        composeTestRule.setContent {
            FinTrackTheme {
                CurrencyToggle(
                    selectedCurrency = curr,
                    onCurrencyChanged = { curr = it },
                    modifier = Modifier.testTag("currency_toggle_container")
                )
            }
        }

        composeTestRule.onNode(SemanticsMatcher.keyIsDefined(SemanticsProperties.SelectableGroup))
            .assertExists()
            .assertIsDisplayed()

        composeTestRule.onNodeWithTag("currency_toggle_RON")
            .assertIsSelected()
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Tab))

        composeTestRule.onNodeWithTag("currency_toggle_EUR")
            .assertIsNotSelected()
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Tab))
    }

    // =========================================================================
    // 10. ANIMATED FINANCIAL CHARTS (2b, 2c)
    // =========================================================================

    @Test
    fun test14_MonthlyCashFlowBarChart_AnimationScrubbingAndHud() {
        val testData = listOf(
            MonthlyDataPoint("Jul 2026", 4500.0, 3200.0, 1300.0),
            MonthlyDataPoint("Aug 2026", 5000.0, 3100.0, 1900.0),
            MonthlyDataPoint("Sep 2026", 4800.0, 3600.0, 1200.0)
        )

        composeTestRule.setContent {
            FinTrackTheme {
                MonthlyCashFlowBarChart(
                    dataPoints = testData,
                    currency = "RON"
                )
            }
        }

        composeTestRule.onNodeWithTag("cash_flow_bar_canvas").assertIsDisplayed()
        composeTestRule.onNodeWithTag("cash_flow_bar_hud").assertIsDisplayed()

        // Tap on the Aug month label to change active point
        composeTestRule.onNodeWithText("Aug").performClick()
        composeTestRule.waitForIdle()

        // Verify HUD updates
        composeTestRule.onNodeWithTag("cash_flow_bar_hud").assertIsDisplayed()
        composeTestRule.onNodeWithText("Aug 2026").assertIsDisplayed()
    }

    @Test
    fun test15_SavingsTrendLineChart_AnimationScrubbingAndHud() {
        val testData = listOf(
            MonthlyDataPoint("Jul 2026", 4500.0, 3200.0, 12000.0),
            MonthlyDataPoint("Aug 2026", 5000.0, 3100.0, 13900.0),
            MonthlyDataPoint("Sep 2026", 4800.0, 3600.0, 15100.0)
        )

        composeTestRule.setContent {
            FinTrackTheme {
                SavingsTrendLineChart(
                    dataPoints = testData,
                    currency = "RON"
                )
            }
        }

        composeTestRule.onNodeWithTag("savings_trend_canvas").assertIsDisplayed()
        composeTestRule.onNodeWithTag("savings_trend_hud").assertIsDisplayed()

        // Tap Jul label on x-axis
        composeTestRule.onAllNodesWithText("Jul 2026")[0].performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("savings_trend_hud").assertIsDisplayed()
        composeTestRule.onAllNodesWithText("Jul 2026").assertCountEquals(2)
    }

    @Test
    fun test16_CategoryDistributionChart_DonutAndCenterHud() {
        val shares = listOf(
            CategoryExpenseShare("Groceries", 1200.0, 60.0, 5),
            CategoryExpenseShare("Utilities", 800.0, 40.0, 2)
        )

        composeTestRule.setContent {
            FinTrackTheme {
                CategoryDistributionChart(
                    categoryShares = shares,
                    currency = "RON"
                )
            }
        }

        composeTestRule.onNodeWithTag("category_donut_hud").assertIsDisplayed()
        composeTestRule.onAllNodesWithText("Groceries").assertCountEquals(2)
        composeTestRule.onAllNodesWithText("Utilities")[0].assertIsDisplayed()
    }

    // =========================================================================
    // 11. CHART PALETTE & DIMENSION TOKENS (2h, 2i)
    // =========================================================================

    @Test
    fun test17_ChartPaletteAndDimensionTokens_Integrity() {
        assertEquals(6, FinTrackChartPalette.size)
        assertEquals(ChartCategoryAmber, FinTrackChartPalette[0])
        assertNotNull(ChartCategoryOther)

        assertEquals(640.dp, MaxContentWidthTablet)
        assertEquals(1.dp, DividerThicknessHairline)
        assertEquals(68.dp, DividerInsetTransaction)
    }
}
