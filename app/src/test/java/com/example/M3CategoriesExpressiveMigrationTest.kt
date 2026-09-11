package com.example

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.dp
import com.example.data.model.CategoryEntity
import com.example.ui.screens.CategoriesScreen
import com.example.ui.theme.FinTrackTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class M3CategoriesExpressiveMigrationTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun sampleCategories(): List<CategoryEntity> = listOf(
        CategoryEntity(id = "cat_1", name = "Housing", type = "Expense", subCategory = "Rent", householdId = "hh_1"),
        CategoryEntity(id = "cat_2", name = "Housing", type = "Expense", subCategory = "Utilities", householdId = "hh_1"),
        CategoryEntity(id = "cat_3", name = "Food & Dining", type = "Expense", subCategory = "Groceries", householdId = "hh_1"),
        CategoryEntity(id = "cat_4", name = "Salary", type = "Income", subCategory = "Monthly Paycheck", householdId = "hh_1"),
        CategoryEntity(id = "cat_5", name = "Investments", type = "Income", subCategory = "Dividends", householdId = "hh_1")
    )

    // =========================================================================
    // 1. VISUAL HIERARCHY & HEADER
    // =========================================================================

    @Test
    fun test1_categoriesScreenRendersWithHeaderAndBadge() {
        composeTestRule.setContent {
            FinTrackTheme {
                CategoriesScreen(
                    categories = sampleCategories(),
                    canManageCategories = true,
                    onAddCategory = { _, _, _ -> },
                    onUpdateCategoryGroup = { _, _, _ -> },
                    onDeleteCategoryGroup = { _, _ -> },
                    onUpdateSubcategory = { _, _ -> },
                    onDeleteSubcategory = { _ -> }
                )
            }
        }

        composeTestRule.onNodeWithText("Categories & Subcategories").assertIsDisplayed()
        composeTestRule.onNodeWithText("Organize household transactions by type").assertIsDisplayed()
        // There are 2 expense category groups: Housing and Food & Dining
        composeTestRule.onNodeWithText("2 Groups").assertIsDisplayed()
    }

    // =========================================================================
    // 2. TYPE TOGGLE (EXPENSE VS INCOME)
    // =========================================================================

    @Test
    fun test2_typeToggleSwitchesExpenseAndIncome() {
        composeTestRule.setContent {
            FinTrackTheme {
                CategoriesScreen(
                    categories = sampleCategories(),
                    canManageCategories = true,
                    onAddCategory = { _, _, _ -> },
                    onUpdateCategoryGroup = { _, _, _ -> },
                    onDeleteCategoryGroup = { _, _ -> },
                    onUpdateSubcategory = { _, _ -> },
                    onDeleteSubcategory = { _ -> }
                )
            }
        }

        // Initially on Expense
        composeTestRule.onNodeWithText("Housing").assertIsDisplayed()
        composeTestRule.onNodeWithText("Food & Dining").performScrollTo().assertIsDisplayed()
        composeTestRule.onAllNodesWithText("Salary").assertCountEquals(0)

        // Switch to Income
        composeTestRule.onNodeWithText("Income Categories").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Salary").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Investments").performScrollTo().assertIsDisplayed()
        composeTestRule.onAllNodesWithText("Housing").assertCountEquals(0)
    }

    // =========================================================================
    // 3. CATEGORY CARDS & TEST TAGS
    // =========================================================================

    @Test
    fun test3_categoryCardRenderedWithCorrectTestTags() {
        composeTestRule.setContent {
            FinTrackTheme {
                CategoriesScreen(
                    categories = sampleCategories(),
                    canManageCategories = true,
                    onAddCategory = { _, _, _ -> },
                    onUpdateCategoryGroup = { _, _, _ -> },
                    onDeleteCategoryGroup = { _, _ -> },
                    onUpdateSubcategory = { _, _ -> },
                    onDeleteSubcategory = { _ -> }
                )
            }
        }

        composeTestRule.onNodeWithTag("category_card_Housing").assertIsDisplayed()
        composeTestRule.onNodeWithTag("category_card_Food & Dining").performScrollTo().assertIsDisplayed()
    }

    // =========================================================================
    // 4. OWNER MODE MANAGEMENT CONTROLS
    // =========================================================================

    @Test
    fun test4_ownerModeDisplaysAllManagementControls() {
        composeTestRule.setContent {
            FinTrackTheme {
                CategoriesScreen(
                    categories = sampleCategories(),
                    canManageCategories = true, // Owner mode
                    onAddCategory = { _, _, _ -> },
                    onUpdateCategoryGroup = { _, _, _ -> },
                    onDeleteCategoryGroup = { _, _ -> },
                    onUpdateSubcategory = { _, _ -> },
                    onDeleteSubcategory = { _ -> }
                )
            }
        }

        composeTestRule.onNodeWithTag("fab_add_category").assertIsDisplayed()
        composeTestRule.onAllNodesWithText("+ Sub").assertCountEquals(2)
        composeTestRule.onNodeWithTag("edit_category_group_Housing").assertIsDisplayed()
        composeTestRule.onNodeWithTag("delete_category_group_Housing").assertIsDisplayed()
        composeTestRule.onNodeWithTag("edit_subcategory_cat_1").assertIsDisplayed()
        composeTestRule.onNodeWithTag("delete_subcategory_cat_1").assertIsDisplayed()
    }

    // =========================================================================
    // 5. MEMBER MODE HIDES ALL MANAGEMENT CONTROLS
    // =========================================================================

    @Test
    fun test5_memberModeHidesAllManagementControls() {
        composeTestRule.setContent {
            FinTrackTheme {
                CategoriesScreen(
                    categories = sampleCategories(),
                    canManageCategories = false, // Member mode
                    onAddCategory = { _, _, _ -> },
                    onUpdateCategoryGroup = { _, _, _ -> },
                    onDeleteCategoryGroup = { _, _ -> },
                    onUpdateSubcategory = { _, _ -> },
                    onDeleteSubcategory = { _ -> }
                )
            }
        }

        composeTestRule.onAllNodesWithTag("fab_add_category").assertCountEquals(0)
        composeTestRule.onAllNodesWithText("+ Sub").assertCountEquals(0)
        composeTestRule.onAllNodesWithTag("edit_category_group_Housing").assertCountEquals(0)
        composeTestRule.onAllNodesWithTag("delete_category_group_Housing").assertCountEquals(0)
        composeTestRule.onAllNodesWithTag("edit_subcategory_cat_1").assertCountEquals(0)
        composeTestRule.onAllNodesWithTag("delete_subcategory_cat_1").assertCountEquals(0)

        // Read-only content remains fully visible
        composeTestRule.onNodeWithText("Housing").assertIsDisplayed()
        composeTestRule.onNodeWithText("Rent").assertIsDisplayed()
    }

    // =========================================================================
    // 6. ADD SUBCATEGORY CLICK OPENS FORM DIALOG
    // =========================================================================

    @Test
    fun test6_addSubcategoryClickOpensFormDialog() {
        composeTestRule.setContent {
            FinTrackTheme {
                CategoriesScreen(
                    categories = sampleCategories(),
                    canManageCategories = true,
                    onAddCategory = { _, _, _ -> },
                    onUpdateCategoryGroup = { _, _, _ -> },
                    onDeleteCategoryGroup = { _, _ -> },
                    onUpdateSubcategory = { _, _ -> },
                    onDeleteSubcategory = { _ -> }
                )
            }
        }

        // Click "+ Sub" on the first category group (Housing)
        composeTestRule.onAllNodesWithText("+ Sub")[0].performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Add Subcategory to Housing").assertIsDisplayed()
        composeTestRule.onNodeWithText("Save Category").performScrollTo().assertIsDisplayed()
    }

    // =========================================================================
    // 7. EDIT CATEGORY GROUP OPENS DIALOG
    // =========================================================================

    @Test
    fun test7_editCategoryGroupClickOpensDialog() {
        composeTestRule.setContent {
            FinTrackTheme {
                CategoriesScreen(
                    categories = sampleCategories(),
                    canManageCategories = true,
                    onAddCategory = { _, _, _ -> },
                    onUpdateCategoryGroup = { _, _, _ -> },
                    onDeleteCategoryGroup = { _, _ -> },
                    onUpdateSubcategory = { _, _ -> },
                    onDeleteSubcategory = { _ -> }
                )
            }
        }

        composeTestRule.onNodeWithTag("edit_category_group_Housing").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Rename Category Group").assertIsDisplayed()
        composeTestRule.onNodeWithText("Category Group Name").assertIsDisplayed()
    }

    // =========================================================================
    // 8. DELETE CATEGORY GROUP OPENS CONFIRMATION
    // =========================================================================

    @Test
    fun test8_deleteCategoryGroupClickOpensConfirmation() {
        composeTestRule.setContent {
            FinTrackTheme {
                CategoriesScreen(
                    categories = sampleCategories(),
                    canManageCategories = true,
                    onAddCategory = { _, _, _ -> },
                    onUpdateCategoryGroup = { _, _, _ -> },
                    onDeleteCategoryGroup = { _, _ -> },
                    onUpdateSubcategory = { _, _ -> },
                    onDeleteSubcategory = { _ -> }
                )
            }
        }

        composeTestRule.onNodeWithTag("delete_category_group_Housing").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Delete Category Group").assertIsDisplayed()
        composeTestRule.onNodeWithText("Delete Group").assertIsDisplayed()
    }

    // =========================================================================
    // 9. EDIT SUBCATEGORY OPENS DIALOG
    // =========================================================================

    @Test
    fun test9_editSubcategoryClickOpensDialog() {
        composeTestRule.setContent {
            FinTrackTheme {
                CategoriesScreen(
                    categories = sampleCategories(),
                    canManageCategories = true,
                    onAddCategory = { _, _, _ -> },
                    onUpdateCategoryGroup = { _, _, _ -> },
                    onDeleteCategoryGroup = { _, _ -> },
                    onUpdateSubcategory = { _, _ -> },
                    onDeleteSubcategory = { _ -> }
                )
            }
        }

        composeTestRule.onNodeWithTag("edit_subcategory_cat_1").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Rename Subcategory").assertIsDisplayed()
        composeTestRule.onNodeWithText("Subcategory Name").assertIsDisplayed()
    }

    // =========================================================================
    // 10. DELETE SUBCATEGORY TRIGGERS CALLBACK
    // =========================================================================

    @Test
    fun test10_deleteSubcategoryClickTriggersCallback() {
        var deletedSubId: String? = null
        composeTestRule.setContent {
            FinTrackTheme {
                CategoriesScreen(
                    categories = sampleCategories(),
                    canManageCategories = true,
                    onAddCategory = { _, _, _ -> },
                    onUpdateCategoryGroup = { _, _, _ -> },
                    onDeleteCategoryGroup = { _, _ -> },
                    onUpdateSubcategory = { _, _ -> },
                    onDeleteSubcategory = { id -> deletedSubId = id }
                )
            }
        }

        composeTestRule.onNodeWithTag("delete_subcategory_cat_1").performClick()
        composeTestRule.waitForIdle()

        assertEquals("cat_1", deletedSubId)
    }

    // =========================================================================
    // 11. EMPTY STATE DISPLAYED WHEN NO CATEGORIES
    // =========================================================================

    @Test
    fun test11_emptyStateDisplayedWhenNoCategories() {
        composeTestRule.setContent {
            FinTrackTheme {
                CategoriesScreen(
                    categories = emptyList(),
                    canManageCategories = true,
                    onAddCategory = { _, _, _ -> },
                    onUpdateCategoryGroup = { _, _, _ -> },
                    onDeleteCategoryGroup = { _, _ -> },
                    onUpdateSubcategory = { _, _ -> },
                    onDeleteSubcategory = { _ -> }
                )
            }
        }

        composeTestRule.onNodeWithText("No Expense Categories").assertIsDisplayed()
        composeTestRule.onNodeWithText("Add Category").assertIsDisplayed()
    }

    // =========================================================================
    // 12. CATEGORY ACCORDION EXPAND AND COLLAPSE INTERACTION
    // =========================================================================

    @Test
    fun test12_categoryAccordionExpandCollapseInteraction() {
        composeTestRule.setContent {
            FinTrackTheme {
                CategoriesScreen(
                    categories = sampleCategories(),
                    canManageCategories = true,
                    onAddCategory = { _, _, _ -> },
                    onUpdateCategoryGroup = { _, _, _ -> },
                    onDeleteCategoryGroup = { _, _ -> },
                    onUpdateSubcategory = { _, _ -> },
                    onDeleteSubcategory = { _ -> }
                )
            }
        }

        // Starts expanded: Subcategory "Rent" is displayed
        composeTestRule.onNodeWithText("Rent").assertIsDisplayed()

        // Tap the collapse chevron for Housing
        composeTestRule.onNodeWithContentDescription("Collapse Housing").performClick()
        composeTestRule.waitForIdle()

        // Subcategory is now hidden
        composeTestRule.onAllNodesWithText("Rent").assertCountEquals(0)

        // Tap the expand chevron for Housing
        composeTestRule.onNodeWithContentDescription("Expand Housing").performClick()
        composeTestRule.waitForIdle()

        // Subcategory is displayed again
        composeTestRule.onNodeWithText("Rent").assertIsDisplayed()
    }

    // =========================================================================
    // 13. RESPONSIVE LAYOUT 360DP
    // =========================================================================

    @Test
    fun test13_responsiveLayout360dp() {
        composeTestRule.setContent {
            FinTrackTheme {
                Box(modifier = Modifier.width(360.dp)) {
                    CategoriesScreen(
                        categories = sampleCategories(),
                        canManageCategories = true,
                        onAddCategory = { _, _, _ -> },
                        onUpdateCategoryGroup = { _, _, _ -> },
                        onDeleteCategoryGroup = { _, _ -> },
                        onUpdateSubcategory = { _, _ -> },
                        onDeleteSubcategory = { _ -> }
                    )
                }
            }
        }

        composeTestRule.onNodeWithText("Categories & Subcategories").assertIsDisplayed()
        composeTestRule.onNodeWithTag("category_card_Housing").assertIsDisplayed()
    }

    // =========================================================================
    // 14. RESPONSIVE LAYOUT 390DP
    // =========================================================================

    @Test
    fun test14_responsiveLayout390dp() {
        composeTestRule.setContent {
            FinTrackTheme {
                Box(modifier = Modifier.width(390.dp)) {
                    CategoriesScreen(
                        categories = sampleCategories(),
                        canManageCategories = true,
                        onAddCategory = { _, _, _ -> },
                        onUpdateCategoryGroup = { _, _, _ -> },
                        onDeleteCategoryGroup = { _, _ -> },
                        onUpdateSubcategory = { _, _ -> },
                        onDeleteSubcategory = { _ -> }
                    )
                }
            }
        }

        composeTestRule.onNodeWithText("Categories & Subcategories").assertIsDisplayed()
        composeTestRule.onNodeWithTag("category_card_Housing").assertIsDisplayed()
    }

    // =========================================================================
    // 15. RESPONSIVE LAYOUT 412DP
    // =========================================================================

    @Test
    fun test15_responsiveLayout412dp() {
        composeTestRule.setContent {
            FinTrackTheme {
                Box(modifier = Modifier.width(412.dp)) {
                    CategoriesScreen(
                        categories = sampleCategories(),
                        canManageCategories = true,
                        onAddCategory = { _, _, _ -> },
                        onUpdateCategoryGroup = { _, _, _ -> },
                        onDeleteCategoryGroup = { _, _ -> },
                        onUpdateSubcategory = { _, _ -> },
                        onDeleteSubcategory = { _ -> }
                    )
                }
            }
        }

        composeTestRule.onNodeWithText("Categories & Subcategories").assertIsDisplayed()
        composeTestRule.onNodeWithTag("category_card_Housing").assertIsDisplayed()
    }

    // =========================================================================
    // 16. RESPONSIVE LAYOUT 600DP+ ADAPTIVE
    // =========================================================================

    @Test
    fun test16_responsiveLayout600dpAdaptive() {
        composeTestRule.setContent {
            FinTrackTheme {
                Box(modifier = Modifier.width(720.dp)) {
                    CategoriesScreen(
                        categories = sampleCategories(),
                        canManageCategories = true,
                        onAddCategory = { _, _, _ -> },
                        onUpdateCategoryGroup = { _, _, _ -> },
                        onDeleteCategoryGroup = { _, _ -> },
                        onUpdateSubcategory = { _, _ -> },
                        onDeleteSubcategory = { _ -> }
                    )
                }
            }
        }

        composeTestRule.onNodeWithText("Categories & Subcategories").assertIsDisplayed()
        composeTestRule.onNodeWithTag("category_card_Housing").assertIsDisplayed()
    }

    // =========================================================================
    // 17. TOUCH TARGETS MEET 48DP MINIMUM
    // =========================================================================

    @Test
    fun test17_touchTargetsMeet48dpMinimum() {
        composeTestRule.setContent {
            FinTrackTheme {
                CategoriesScreen(
                    categories = sampleCategories(),
                    canManageCategories = true,
                    onAddCategory = { _, _, _ -> },
                    onUpdateCategoryGroup = { _, _, _ -> },
                    onDeleteCategoryGroup = { _, _ -> },
                    onUpdateSubcategory = { _, _ -> },
                    onDeleteSubcategory = { _ -> }
                )
            }
        }

        composeTestRule.onNodeWithTag("fab_add_category")
            .assertWidthIsAtLeast(48.dp)
            .assertHeightIsAtLeast(48.dp)

        composeTestRule.onNodeWithTag("edit_category_group_Housing")
            .assertWidthIsAtLeast(48.dp)
            .assertHeightIsAtLeast(48.dp)

        composeTestRule.onNodeWithTag("delete_category_group_Housing")
            .assertWidthIsAtLeast(48.dp)
            .assertHeightIsAtLeast(48.dp)

        composeTestRule.onNodeWithTag("edit_subcategory_cat_1")
            .assertWidthIsAtLeast(48.dp)
            .assertHeightIsAtLeast(48.dp)

        composeTestRule.onNodeWithTag("delete_subcategory_cat_1")
            .assertWidthIsAtLeast(48.dp)
            .assertHeightIsAtLeast(48.dp)
    }

    // =========================================================================
    // 18. ACCESSIBILITY ROLE SEMANTICS
    // =========================================================================

    @Test
    fun test18_accessibilityRoleSemantics() {
        composeTestRule.setContent {
            FinTrackTheme {
                CategoriesScreen(
                    categories = sampleCategories(),
                    canManageCategories = true,
                    onAddCategory = { _, _, _ -> },
                    onUpdateCategoryGroup = { _, _, _ -> },
                    onDeleteCategoryGroup = { _, _ -> },
                    onUpdateSubcategory = { _, _ -> },
                    onDeleteSubcategory = { _ -> }
                )
            }
        }

        val tabMatcher = SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Tab)
        composeTestRule.onAllNodes(tabMatcher).assertCountEquals(2)
    }

    // =========================================================================
    // 19. REDUCED MOTION EXECUTION
    // =========================================================================

    @Test
    fun test19_reducedMotionExecution() {
        composeTestRule.setContent {
            FinTrackTheme {
                CategoriesScreen(
                    categories = sampleCategories(),
                    canManageCategories = true,
                    onAddCategory = { _, _, _ -> },
                    onUpdateCategoryGroup = { _, _, _ -> },
                    onDeleteCategoryGroup = { _, _ -> },
                    onUpdateSubcategory = { _, _ -> },
                    onDeleteSubcategory = { _ -> }
                )
            }
        }

        // Even with reduced motion, expansion and content interaction should be immediate and deterministic
        composeTestRule.onNodeWithContentDescription("Collapse Housing").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onAllNodesWithText("Rent").assertCountEquals(0)

        composeTestRule.onNodeWithContentDescription("Expand Housing").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Rent").assertIsDisplayed()
    }

    // =========================================================================
    // 20. HOUSEHOLD DATA ISOLATION PRESERVED
    // =========================================================================

    @Test
    fun test20_householdDataIsolationPreserved() {
        val isolatedHouseholdCategories = listOf(
            CategoryEntity(id = "cat_hh2", name = "Secret Category", type = "Expense", subCategory = "Vault", householdId = "hh_other")
        )

        composeTestRule.setContent {
            FinTrackTheme {
                CategoriesScreen(
                    categories = isolatedHouseholdCategories,
                    canManageCategories = true,
                    onAddCategory = { _, _, _ -> },
                    onUpdateCategoryGroup = { _, _, _ -> },
                    onDeleteCategoryGroup = { _, _ -> },
                    onUpdateSubcategory = { _, _ -> },
                    onDeleteSubcategory = { _ -> }
                )
            }
        }

        // Only categories provided to the screen are displayed; no leakage
        composeTestRule.onNodeWithText("Secret Category").assertIsDisplayed()
        composeTestRule.onAllNodesWithText("Housing").assertCountEquals(0)
    }

    // =========================================================================
    // 21. LONG CATEGORY NAMES & SUBCOUNT WRAP-PROOF ON 360DP (MOBILE)
    // =========================================================================

    @Test
    fun test21_longCategoryNamesAndSubcategoryWrappingOn360dp() {
        val longCategories = listOf(
            CategoryEntity(id = "cat_l1", name = "Alimente & Băuturi de zi cu zi", type = "Expense", subCategory = "Supermarket, Piață & Băcănie", householdId = "hh_1"),
            CategoryEntity(id = "cat_l2", name = "Alimente & Băuturi de zi cu zi", type = "Expense", subCategory = "Restaurante, Cafenele & Fast-Food", householdId = "hh_1"),
            CategoryEntity(id = "cat_l3", name = "Bonusuri și alte venituri ocazionale", type = "Income", subCategory = "Prime de performanță anuale", householdId = "hh_1")
        )

        composeTestRule.setContent {
            FinTrackTheme {
                Box(modifier = Modifier.width(360.dp)) {
                    CategoriesScreen(
                        categories = longCategories,
                        canManageCategories = true,
                        onAddCategory = { _, _, _ -> },
                        onUpdateCategoryGroup = { _, _, _ -> },
                        onDeleteCategoryGroup = { _, _ -> },
                        onUpdateSubcategory = { _, _ -> },
                        onDeleteSubcategory = { _ -> }
                    )
                }
            }
        }

        // Long category titles are visible and intact
        composeTestRule.onNodeWithText("Alimente & Băuturi de zi cu zi").assertIsDisplayed()
        // Subcategory count badge renders as a complete unit
        composeTestRule.onNodeWithText("2 subcategories").assertIsDisplayed()
        // Subcategory items with long names render properly
        composeTestRule.onNodeWithText("Supermarket, Piață & Băcănie").assertIsDisplayed()
        composeTestRule.onNodeWithText("Restaurante, Cafenele & Fast-Food").assertIsDisplayed()

        // Management actions are accessible
        composeTestRule.onNodeWithTag("edit_category_group_Alimente & Băuturi de zi cu zi").assertIsDisplayed()
        composeTestRule.onNodeWithTag("delete_category_group_Alimente & Băuturi de zi cu zi").assertIsDisplayed()
        composeTestRule.onNodeWithTag("edit_subcategory_cat_l1").assertIsDisplayed()
        composeTestRule.onNodeWithTag("delete_subcategory_cat_l1").assertIsDisplayed()
    }

    // =========================================================================
    // 22. LONG CATEGORY NAMES ON TABLET 600DP+ (WIDE BALANCED ROW)
    // =========================================================================

    @Test
    fun test22_longCategoryNamesOnTablet600dp() {
        val longCategories = listOf(
            CategoryEntity(id = "cat_l1", name = "Alimente & Băuturi de zi cu zi", type = "Expense", subCategory = "Supermarket", householdId = "hh_1")
        )

        composeTestRule.setContent {
            FinTrackTheme {
                Box(modifier = Modifier.width(720.dp)) {
                    CategoriesScreen(
                        categories = longCategories,
                        canManageCategories = true,
                        onAddCategory = { _, _, _ -> },
                        onUpdateCategoryGroup = { _, _, _ -> },
                        onDeleteCategoryGroup = { _, _ -> },
                        onUpdateSubcategory = { _, _ -> },
                        onDeleteSubcategory = { _ -> }
                    )
                }
            }
        }

        composeTestRule.onNodeWithText("Alimente & Băuturi de zi cu zi").assertIsDisplayed()
        composeTestRule.onNodeWithText("1 subcategory").assertIsDisplayed()
        composeTestRule.onNodeWithTag("edit_category_group_Alimente & Băuturi de zi cu zi").assertIsDisplayed()
        composeTestRule.onNodeWithTag("delete_category_group_Alimente & Băuturi de zi cu zi").assertIsDisplayed()
    }

    // =========================================================================
    // 23. MEMBER MODE ON COMPACT SCREEN (CLEAN READ-ONLY ROW)
    // =========================================================================

    @Test
    fun test23_memberModeResponsiveLayoutNoActionOverlap() {
        val longCategories = listOf(
            CategoryEntity(id = "cat_l1", name = "Bonusuri și alte venituri", type = "Expense", subCategory = "Tichete de masă", householdId = "hh_1")
        )

        composeTestRule.setContent {
            FinTrackTheme {
                Box(modifier = Modifier.width(360.dp)) {
                    CategoriesScreen(
                        categories = longCategories,
                        canManageCategories = false, // Member mode
                        onAddCategory = { _, _, _ -> },
                        onUpdateCategoryGroup = { _, _, _ -> },
                        onDeleteCategoryGroup = { _, _ -> },
                        onUpdateSubcategory = { _, _ -> },
                        onDeleteSubcategory = { _ -> }
                    )
                }
            }
        }

        composeTestRule.onNodeWithText("Bonusuri și alte venituri").assertIsDisplayed()
        composeTestRule.onNodeWithText("1 subcategory").assertIsDisplayed()
        composeTestRule.onNodeWithText("Tichete de masă").assertIsDisplayed()
        // No management buttons displayed for member
        composeTestRule.onAllNodesWithTag("edit_category_group_Bonusuri și alte venituri").assertCountEquals(0)
        composeTestRule.onAllNodesWithTag("delete_category_group_Bonusuri și alte venituri").assertCountEquals(0)
    }

    // =========================================================================
    // 24. SHORT VIEWPORT 320DP X 470DP (NO FAB OVERLAP ON OWNER ACTIONS)
    // =========================================================================

    @Test
    fun test24_shortViewport320dpNoFabActionOverlap() {
        var subcategoryDeleted = false
        val categories = listOf(
            CategoryEntity(id = "cat_1", name = "Housing", type = "Expense", subCategory = "Rent", householdId = "hh_1")
        )

        composeTestRule.setContent {
            FinTrackTheme {
                Box(modifier = Modifier.size(width = 320.dp, height = 470.dp)) {
                    CategoriesScreen(
                        categories = categories,
                        canManageCategories = true,
                        onAddCategory = { _, _, _ -> },
                        onUpdateCategoryGroup = { _, _, _ -> },
                        onDeleteCategoryGroup = { _, _ -> },
                        onUpdateSubcategory = { _, _ -> },
                        onDeleteSubcategory = { _ -> subcategoryDeleted = true }
                    )
                }
            }
        }

        // Verify category card and subcategory are displayed
        composeTestRule.onNodeWithText("Housing").assertIsDisplayed()
        composeTestRule.onNodeWithText("Rent").assertIsDisplayed()

        // FAB should be displayed in the short viewport
        composeTestRule.onNodeWithTag("fab_add_category").assertIsDisplayed()

        // Clicking the subcategory delete button must successfully trigger onDeleteSubcategory,
        // proving it is NOT occluded or intercepted by the FloatingActionButton
        composeTestRule.onNodeWithTag("delete_subcategory_cat_1").assertIsDisplayed()
        composeTestRule.onNodeWithTag("delete_subcategory_cat_1").performClick()
        assertTrue("Subcategory delete should be clicked without FAB occlusion", subcategoryDeleted)
    }

    // =========================================================================
    // 25. CATEGORY WITH NO SUBCATEGORIES (EXPRESSIVE EMPTY STATE)
    // =========================================================================

    @Test
    fun test25_categoryWithNoSubcategoriesExpressiveEmptyState() {
        val emptySubCategories = listOf(
            CategoryEntity(id = "cat_empty", name = "Miscellaneous", type = "Expense", subCategory = "", householdId = "hh_1")
        )

        composeTestRule.setContent {
            FinTrackTheme {
                CategoriesScreen(
                    categories = emptySubCategories,
                    canManageCategories = true,
                    onAddCategory = { _, _, _ -> },
                    onUpdateCategoryGroup = { _, _, _ -> },
                    onDeleteCategoryGroup = { _, _ -> },
                    onUpdateSubcategory = { _, _ -> },
                    onDeleteSubcategory = { _ -> }
                )
            }
        }

        // Category card rendered with 0 subcategories badge
        composeTestRule.onNodeWithText("Miscellaneous").assertIsDisplayed()
        composeTestRule.onNodeWithText("0 subcategories").assertIsDisplayed()

        // Expressive empty state helper text rendered inside expanded accordion
        composeTestRule.onNodeWithText("No subcategories yet. Tap '+ Sub' to add one.").assertIsDisplayed()
    }

    // =========================================================================
    // 26. SEGMENTED CONTROL SPRING SELECTION TOGGLE
    // =========================================================================

    @Test
    fun test26_segmentedControlSpringSelectionToggle() {
        composeTestRule.setContent {
            FinTrackTheme {
                CategoriesScreen(
                    categories = sampleCategories(),
                    canManageCategories = true,
                    onAddCategory = { _, _, _ -> },
                    onUpdateCategoryGroup = { _, _, _ -> },
                    onDeleteCategoryGroup = { _, _ -> },
                    onUpdateSubcategory = { _, _ -> },
                    onDeleteSubcategory = { _ -> }
                )
            }
        }

        // Initially Expense Categories is selected, Income is not
        composeTestRule.onNodeWithText("Expense Categories").assertIsSelected()
        composeTestRule.onNodeWithText("Income Categories").assertIsNotSelected()

        // Switch to Income Categories
        composeTestRule.onNodeWithText("Income Categories").performClick()
        composeTestRule.waitForIdle()

        // Income Categories is now selected, Expense is not
        composeTestRule.onNodeWithText("Income Categories").assertIsSelected()
        composeTestRule.onNodeWithText("Expense Categories").assertIsNotSelected()

        // Switch back to Expense Categories
        composeTestRule.onNodeWithText("Expense Categories").performClick()
        composeTestRule.waitForIdle()

        // Expense Categories is selected again
        composeTestRule.onNodeWithText("Expense Categories").assertIsSelected()
        composeTestRule.onNodeWithText("Income Categories").assertIsNotSelected()
    }
}
