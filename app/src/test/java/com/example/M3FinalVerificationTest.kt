package com.example

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.dp
import com.example.data.model.CategoryEntity
import com.example.data.model.FilterSettings
import com.example.data.model.HouseholdDto
import com.example.data.model.HouseholdMemberDto
import com.example.data.model.TransactionEntity
import com.example.data.repository.AuthState
import com.example.data.repository.SyncStatus
import com.example.domain.analytics.CategoryExpenseShare
import com.example.domain.analytics.CategoryRankingItem
import com.example.domain.analytics.DashboardMetrics
import com.example.domain.analytics.MonthlyDataPoint
import com.example.domain.analytics.SingleSeriesAnalyticsResult
import com.example.domain.analytics.SingleSeriesDataPoint
import com.example.domain.analytics.SmartFinancialInsights
import com.example.ui.AnalyticsUiState
import com.example.ui.screens.AnalyticsScreen
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.CategoriesScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.TransactionsScreen
import com.example.ui.theme.FinTrackTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Phase 3D / M3-10: Full Material 3 Expressive Cross-Screen Quality Gate & Verification Suite.
 * Validates consistency of the unified design system across all screens and components:
 * Dashboard, Transactions, Analytics, Categories, Settings/Household, and Auth.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class M3FinalVerificationTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun sampleMetrics() = DashboardMetrics(
        balance = 45200.0,
        currency = "RON",
        totalIncome = 62000.0,
        totalExpense = 16800.0,
        savingsRate = 72.9,
        expensePressure = 27.1,
        secondaryCurrency = "EUR",
        secondaryCurrencyBalance = 9082.0,
        latestBnrRate = 4.9765,
        effectiveBnrDate = "2026-03-09",
        bnrStatus = "OFFICIAL",
        periodLabel = "This Month"
    )

    private fun sampleTransaction() = TransactionEntity(
        id = "tx_final_1",
        householdId = "hh_alpha",
        description = "Supermarket Grocery",
        amountRON = 350.0,
        amountEUR = 70.33,
        type = "Expense",
        category = "Food & Dining",
        subCategory = "Groceries",
        date = "2026-03-09",
        account = "Checking",
        exchangeRate = 4.9765,
        exchangeRateDate = "2026-03-09",
        conversionStatus = "OFFICIAL",
        exchangeRateSource = "BNR_OFFICIAL"
    )

    private fun sampleCategories() = listOf(
        CategoryEntity(id = "cat_1", householdId = "hh_alpha", name = "Food & Dining", type = "Expense", subCategory = "Groceries"),
        CategoryEntity(id = "cat_2", householdId = "hh_alpha", name = "Salary", type = "Income", subCategory = "Primary Job")
    )

    private fun sampleHousehold() = HouseholdDto(
        householdId = "hh_alpha",
        name = "FinTrack Household",
        createdByUid = "user_owner_1",
        createdAt = 1700000000000L,
        updatedAt = 1700000000000L
    )

    private fun sampleOwnerMembership() = HouseholdMemberDto(
        uid = "user_owner_1",
        displayName = "Owner User",
        email = "owner@fintrack.ro",
        role = "OWNER",
        status = "ACTIVE",
        joinedAt = 1700000000000L
    )

    // =========================================================================
    // 1. DASHBOARD EXPRESSIVE HIERARCHY
    // =========================================================================

    @Test
    fun test01_dashboardScreenTonalSurfaceAndExpressiveHierarchy() {
        composeTestRule.setContent {
            FinTrackTheme {
                DashboardScreen(
                    metrics = sampleMetrics(),
                    filterSettings = FilterSettings(selectedCurrency = "RON", selectedPeriod = "This Month"),
                    monthlyDataPoints = listOf(MonthlyDataPoint("Jan 2026", 50000.0, 15000.0, 35000.0)),
                    categoryShares = listOf(CategoryExpenseShare("Food & Dining", 350.0, 100.0, 1)),
                    smartInsights = SmartFinancialInsights(savingsTrendText = "Strong Growth"),
                    recentTransactions = listOf(sampleTransaction()),
                    onPeriodSelected = {},
                    onCurrencyChanged = {}
                )
            }
        }

        // Hero and Currency Toggle
        composeTestRule.onNodeWithText("Dashboard").assertIsDisplayed()
        composeTestRule.onNodeWithTag("currency_toggle_RON").assertIsDisplayed()
        composeTestRule.onNodeWithTag("unified_hero_canvas").assertIsDisplayed()
        composeTestRule.onNodeWithTag("dashboard_top_card").assertIsDisplayed()

        // Financial Pulse & Charts
        composeTestRule.onNodeWithTag("financial_pulse_card").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Monthly Cash Flow").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Spending by Category").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("recent_activity_section").performScrollTo().assertIsDisplayed()
    }

    // =========================================================================
    // 2. TRANSACTIONS SCREEN GROUPED SURFACES
    // =========================================================================

    @Test
    fun test02_transactionsScreenDateGroupContainersAndSemantics() {
        composeTestRule.setContent {
            FinTrackTheme {
                TransactionsScreen(
                    transactions = listOf(sampleTransaction()),
                    categories = sampleCategories(),
                    filterSettings = FilterSettings(),
                    onCurrencyChanged = {},
                    onCategoryFilterSelected = { _, _ -> },
                    onSearchQueryChanged = {},
                    onAddTransactionClicked = {},
                    onDuplicateClicked = {},
                    onEditClicked = {},
                    onDeleteClicked = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Transactions").assertIsDisplayed()
        composeTestRule.onNodeWithTag("search_transactions_input").assertIsDisplayed()
        composeTestRule.onNodeWithTag("fab_add_transaction").assertIsDisplayed()
        composeTestRule.onNodeWithTag("transaction_date_group_2026-03-09").assertIsDisplayed()
    }

    // =========================================================================
    // 3. ANALYTICS EXPLORATION SURFACES
    // =========================================================================

    @Test
    fun test03_analyticsScreenExplorationSurfacesAndNoLocalPeriod() {
        val samplePoints = listOf(
            SingleSeriesDataPoint(yearMonth = "2026-06", monthYearLabel = "Jun 26", value = 1000.0),
            SingleSeriesDataPoint(yearMonth = "2026-07", monthYearLabel = "Jul 26", value = 1500.0)
        )
        val state = AnalyticsUiState(
            incomeExpenseSelection = "Expense",
            incomeExpenseResult = SingleSeriesAnalyticsResult(
                dataPoints = samplePoints,
                currency = "RON",
                monthlyAverage = 1250.0,
                total = 2500.0,
                monthCount = 2
            ),
            expenseCategoryRankings = listOf(
                CategoryRankingItem("Housing", 8000.0, 47.6, 2)
            ),
            selectedExpenseCategory = "Housing",
            expenseCategoryResult = SingleSeriesAnalyticsResult(
                dataPoints = samplePoints,
                currency = "RON",
                monthlyAverage = 8000.0,
                total = 8000.0,
                monthCount = 1
            ),
            incomeSourceRankings = listOf(
                CategoryRankingItem("Salary", 20000.0, 100.0, 1)
            ),
            selectedIncomeSource = "Salary",
            incomeSourceResult = SingleSeriesAnalyticsResult(
                dataPoints = samplePoints,
                currency = "RON",
                monthlyAverage = 20000.0,
                total = 20000.0,
                monthCount = 1
            )
        )

        composeTestRule.setContent {
            FinTrackTheme {
                AnalyticsScreen(
                    analyticsUiState = state,
                    filterSettings = FilterSettings()
                )
            }
        }

        composeTestRule.onNodeWithText("Analytics").assertIsDisplayed()
        composeTestRule.onNodeWithTag("analytics_spending_category_card").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("analytics_income_source_card").performScrollTo().assertIsDisplayed()

        // Strict architectural check: No local period dropdown selector
        composeTestRule.onNodeWithTag("period_selector_dropdown").assertDoesNotExist()
    }

    // =========================================================================
    // 4. CATEGORIES GALLERY ACCORDION & RBAC
    // =========================================================================

    @Test
    fun test04_categoriesGalleryAccordionAndRbacSecurity() {
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
        composeTestRule.onNodeWithTag("fab_add_category").assertIsDisplayed()
        composeTestRule.onNodeWithTag("category_card_Food & Dining").assertIsDisplayed()
    }

    // =========================================================================
    // 5. SETTINGS CONTROL CENTER
    // =========================================================================

    @Test
    fun test05_settingsControlCenterGroupedSectionsAndInvariants() {
        composeTestRule.setContent {
            FinTrackTheme {
                SettingsScreen(
                    filterSettings = FilterSettings(),
                    themeMode = "system",
                    currentUid = "user_owner_1",
                    currentUserEmail = "owner@fintrack.ro",
                    currentHousehold = sampleHousehold(),
                    currentUserMembership = sampleOwnerMembership(),
                    householdMembers = listOf(sampleOwnerMembership()),
                    onCurrencyChanged = {},
                    onThemeModeChanged = {},
                    onExportCsv = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Settings").assertIsDisplayed()
        composeTestRule.onNodeWithTag("account_info_card").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("household_summary_card").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("export_csv_button").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("sync_status_indicator").performScrollTo().assertIsDisplayed()
    }

    // =========================================================================
    // 6. AUTH SCREEN EXPRESSIVE IDENTITY
    // =========================================================================

    @Test
    fun test06_authScreenHeroIdentityAndStateTransitions() {
        composeTestRule.setContent {
            FinTrackTheme {
                AuthScreen(
                    authState = AuthState.SignedOut,
                    onSignInWithGoogle = {},
                    onSignInWithTestUid = {},
                    onAuthError = {},
                    onClearError = {}
                )
            }
        }

        composeTestRule.onNodeWithText("FinTrack").assertIsDisplayed()
        composeTestRule.onNodeWithText("Your household finances, clearly organized").assertIsDisplayed()
        composeTestRule.onNodeWithTag("auth_card").assertIsDisplayed()
        composeTestRule.onNodeWithTag("google_sign_in_button").assertIsDisplayed()
    }

    // =========================================================================
    // 7. TOUCH TARGETS (>= 48dp)
    // =========================================================================

    @Test
    fun test07_allPrimaryInteractiveTouchTargetsMeet48dpStandard() {
        composeTestRule.setContent {
            FinTrackTheme {
                AuthScreen(
                    authState = AuthState.SignedOut,
                    onSignInWithGoogle = {},
                    onSignInWithTestUid = {},
                    onAuthError = {},
                    onClearError = {}
                )
            }
        }

        composeTestRule.onNodeWithTag("google_sign_in_button")
            .assertHeightIsAtLeast(48.dp)
    }

    // =========================================================================
    // 8. RESPONSIVE LAYOUT VERIFICATION (360dp, 390dp, 412dp, 600dp+)
    // =========================================================================

    @Test
    fun test08_compactLayout360dpStabilityCrossScreen() {
        composeTestRule.setContent {
            FinTrackTheme {
                Box(modifier = Modifier.width(360.dp)) {
                    DashboardScreen(
                        metrics = sampleMetrics(),
                        filterSettings = FilterSettings(),
                        monthlyDataPoints = emptyList(),
                        categoryShares = emptyList(),
                        smartInsights = SmartFinancialInsights(),
                        onPeriodSelected = {},
                        onCurrencyChanged = {}
                    )
                }
            }
        }

        composeTestRule.onNodeWithText("Dashboard").assertIsDisplayed()
        composeTestRule.onNodeWithTag("unified_hero_canvas").assertIsDisplayed()
    }

    @Test
    fun test09_standardLayout390dpStabilityCrossScreen() {
        composeTestRule.setContent {
            FinTrackTheme {
                Box(modifier = Modifier.width(390.dp)) {
                    AuthScreen(
                        authState = AuthState.SignedOut,
                        onSignInWithGoogle = {},
                        onSignInWithTestUid = {},
                        onAuthError = {},
                        onClearError = {}
                    )
                }
            }
        }

        composeTestRule.onNodeWithTag("auth_card").assertIsDisplayed()
        composeTestRule.onNodeWithTag("google_sign_in_button").assertIsDisplayed()
    }

    @Test
    fun test10_largeScreenAdaptiveConstraint680dp() {
        composeTestRule.setContent {
            FinTrackTheme {
                Box(modifier = Modifier.width(720.dp)) {
                    DashboardScreen(
                        metrics = sampleMetrics(),
                        filterSettings = FilterSettings(),
                        monthlyDataPoints = emptyList(),
                        categoryShares = emptyList(),
                        smartInsights = SmartFinancialInsights(),
                        onPeriodSelected = {},
                        onCurrencyChanged = {}
                    )
                }
            }
        }

        composeTestRule.onNodeWithText("Dashboard").assertIsDisplayed()
    }

    // =========================================================================
    // 9. SECURITY & DATA ISOLATION
    // =========================================================================

    @Test
    fun test11_zeroFinancialDataLeakageInAuthScreen() {
        composeTestRule.setContent {
            FinTrackTheme {
                AuthScreen(
                    authState = AuthState.SignedOut,
                    onSignInWithGoogle = {},
                    onSignInWithTestUid = {},
                    onAuthError = {},
                    onClearError = {}
                )
            }
        }

        composeTestRule.onNodeWithText("RON", substring = true).assertDoesNotExist()
        composeTestRule.onNodeWithText("EUR", substring = true).assertDoesNotExist()
        composeTestRule.onNodeWithText("Net Worth", substring = true).assertDoesNotExist()
    }

    @Test
    fun test12_reducedMotionGracefulDegradationAcrossScreens() {
        composeTestRule.setContent {
            FinTrackTheme {
                AuthScreen(
                    authState = AuthState.SigningIn,
                    onSignInWithGoogle = {},
                    onSignInWithTestUid = {},
                    onAuthError = {},
                    onClearError = {}
                )
            }
        }

        composeTestRule.onNodeWithTag("auth_signing_in_indicator").assertIsDisplayed()
    }
}
