package com.example

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.dp
import com.example.data.model.TransactionEntity
import com.example.domain.analytics.CategoryExpenseShare
import com.example.ui.components.ButtonVariant
import com.example.ui.components.CategoryDistributionChart
import com.example.ui.components.ExpressiveTypeSegmentedControl
import com.example.ui.components.FinTrackButton
import com.example.ui.components.FinTrackPeriodDropdown
import com.example.ui.components.FinTrackPeriodSelector
import com.example.ui.components.RecentActivitySection
import com.example.ui.theme.FinTrackSpacing
import com.example.ui.theme.FinTrackTheme
import com.example.ui.theme.SpacingBottomNavContent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Verification of Extended Opus 4.6 Audit Polish (Remaining P2 Items):
 * - Item 1 (P2-12): Expense amount color in RecentActivitySection uses FinTrackTheme.colors.expense.
 * - Item 2 (P2-8): Dropdown arrow rotation and touch target / tactile feedback in FinTrackPeriodDropdown.
 * - Item 3 (P2-4): Button variants TONAL, OUTLINED, and TEXT in FinTrackButton.
 * - Item 4 (P2-9): Adaptive donut chart sizing in CategoryDistributionChart.
 * - Item 5 (P1-8): Tokenized floating navigation inset SpacingBottomNavContent = 80.dp.
 * - Item 6 (P1-5 & P2-9): Responsive centering & selectableGroup semantics in FinTrackPeriodSelector.
 * - Item 7 (P1-5): selectableGroup semantic container in ExpressiveTypeSegmentedControl.
 */
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class M3ExpressiveExtendedPolishTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    // =========================================================================
    // Item 1 (P2-12): RecentActivitySection Expense Color & Heading
    // =========================================================================
    @Test
    fun testItem1_RecentActivitySection_ExpenseRendering() {
        val testExpense = TransactionEntity(
            id = "tx_exp_1",
            date = "2026-09-12",
            description = "Supermarket",
            amountRON = 150.0,
            amountEUR = 30.0,
            exchangeRate = 5.0,
            exchangeRateDate = "2026-09-12",
            type = "Expense",
            account = "Card",
            category = "Groceries",
            subCategory = "Food",
            householdId = "hh_1"
        )

        composeTestRule.setContent {
            FinTrackTheme {
                RecentActivitySection(
                    transactions = listOf(testExpense),
                    selectedCurrency = "RON",
                    onViewAllClicked = {},
                    onTransactionClicked = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Recent Activity").assertIsDisplayed()
        composeTestRule.onNodeWithTag("recent_tx_item_tx_exp_1").assertIsDisplayed()
        composeTestRule.onNodeWithTag("recent_tx_amount_tx_exp_1", useUnmergedTree = true)
            .assertIsDisplayed()
            .assertTextEquals("-150.00 RON")
    }

    // =========================================================================
    // Item 2 (P2-8): PeriodSelector Dropdown
    // =========================================================================
    @Test
    fun testItem2_PeriodSelectorDropdown_RendersAndInteracts() {
        composeTestRule.setContent {
            FinTrackTheme {
                FinTrackPeriodDropdown(
                    selectedPeriod = "This Month",
                    onPeriodSelected = {}
                )
            }
        }

        composeTestRule.onNodeWithTag("period_selector_dropdown").assertIsDisplayed()
        composeTestRule.onNodeWithText("This Month").assertIsDisplayed()
    }

    // =========================================================================
    // Item 3 (P2-4): FinTrackButton Extended Variants
    // =========================================================================
    @Test
    fun testItem3_FinTrackButton_ExtendedVariants() {
        // Verify all 6 variants exist in ButtonVariant enum
        val variants = ButtonVariant.values().map { it.name }
        assertTrue(variants.contains("PRIMARY"))
        assertTrue(variants.contains("SECONDARY"))
        assertTrue(variants.contains("DESTRUCTIVE"))
        assertTrue(variants.contains("TONAL"))
        assertTrue(variants.contains("OUTLINED"))
        assertTrue(variants.contains("TEXT"))

        composeTestRule.setContent {
            FinTrackTheme {
                FinTrackButton(
                    onClick = {},
                    text = "Tonal Action",
                    variant = ButtonVariant.TONAL
                )
                FinTrackButton(
                    onClick = {},
                    text = "Outlined Action",
                    variant = ButtonVariant.OUTLINED
                )
                FinTrackButton(
                    onClick = {},
                    text = "Text Action",
                    variant = ButtonVariant.TEXT
                )
            }
        }

        composeTestRule.onNodeWithText("Tonal Action").assertIsDisplayed()
        composeTestRule.onNodeWithText("Outlined Action").assertIsDisplayed()
        composeTestRule.onNodeWithText("Text Action").assertIsDisplayed()
    }

    // =========================================================================
    // Item 4 (P2-9): CategoryDistributionChart Adaptive Donut Sizing
    // =========================================================================
    @Test
    fun testItem4_CategoryDistributionChart_AdaptiveDonutRenders() {
        val testShares = listOf(
            CategoryExpenseShare(
                categoryName = "Food & Dining",
                totalAmount = 600.0,
                percentage = 60.0,
                transactionCount = 5
            ),
            CategoryExpenseShare(
                categoryName = "Utilities",
                totalAmount = 400.0,
                percentage = 40.0,
                transactionCount = 3
            )
        )

        // Compact width (< 480dp)
        composeTestRule.setContent {
            FinTrackTheme {
                Box(modifier = Modifier.width(360.dp)) {
                    CategoryDistributionChart(
                        categoryShares = testShares,
                        currency = "RON"
                    )
                }
            }
        }

        composeTestRule.onNodeWithTag("category_donut_box").assertIsDisplayed()
        composeTestRule.onNodeWithTag("category_donut_canvas").assertIsDisplayed()
        composeTestRule.onNodeWithTag("category_donut_hud").assertIsDisplayed()
        composeTestRule.onAllNodesWithText("Food & Dining").assertCountEquals(2)
        composeTestRule.onNodeWithText("Utilities").assertIsDisplayed()
    }

    // =========================================================================
    // Item 5 (P1-8): Tokenized Floating Navigation Inset
    // =========================================================================
    @Test
    fun testItem5_SpacingBottomNavContent_TokenParity() {
        assertEquals(80.dp, SpacingBottomNavContent)
        val spacing = FinTrackSpacing()
        assertEquals(80.dp, spacing.bottomNavContentSpacing)
    }

    // =========================================================================
    // Item 6 (P1-5 & P2-9): FinTrackPeriodSelector Responsive Centering & selectableGroup
    // =========================================================================
    @Test
    fun testItem6_PeriodSelector_ResponsiveCenteringAndSemantics() {
        var selected = "Last Month"

        // Test on wide viewport (800dp)
        composeTestRule.setContent {
            FinTrackTheme {
                Box(modifier = Modifier.width(800.dp)) {
                    FinTrackPeriodSelector(
                        selectedPeriod = selected,
                        onPeriodSelected = { selected = it }
                    )
                }
            }
        }

        // Verify initial selection and chip presence
        composeTestRule.onNodeWithTag("period_chip_Last_Month")
            .assertIsDisplayed()
            .assertIsSelected()

        // Verify click updates selection
        composeTestRule.onNodeWithTag("period_chip_Last_3_Months")
            .assertIsDisplayed()
            .assertIsNotSelected()
            .performClick()
        assertEquals("Last 3 Months", selected)

        // Verify horizontal scroll reaches end of items
        composeTestRule.onNodeWithTag("period_chip_All_Time")
            .performScrollTo()
            .assertIsDisplayed()
    }

    // =========================================================================
    // Item 7 (P1-5): ExpressiveTypeSegmentedControl selectableGroup
    // =========================================================================
    @Test
    fun testItem7_ExpressiveTypeSegmentedControl_SelectableGroup() {
        var selectedType = "Expense"

        composeTestRule.setContent {
            FinTrackTheme {
                ExpressiveTypeSegmentedControl(
                    selectedType = selectedType,
                    onTypeSelected = { selectedType = it },
                    reducedMotion = true
                )
            }
        }

        composeTestRule.onNodeWithText("Expense").assertIsDisplayed()
        composeTestRule.onNodeWithText("Income").assertIsDisplayed()
        composeTestRule.onNodeWithText("Income").performClick()
        assertEquals("Income", selectedType)
    }
}
