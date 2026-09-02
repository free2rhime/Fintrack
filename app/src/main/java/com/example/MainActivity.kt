package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.repository.AuthState
import com.example.data.util.CsvExporter
import com.example.ui.MainViewModel
import com.example.ui.MigrationUiState
import com.example.ui.components.MigrationConflictDialog
import com.example.ui.components.MigrationPreviewDialog
import com.example.ui.components.MigrationProgressDialog
import com.example.ui.components.MigrationResultDialog
import com.example.ui.components.TransactionFormDialog
import com.example.ui.navigation.FinTrackBottomNavigation
import com.example.ui.screens.AnalyticsScreen
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.CategoriesScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.TransactionsScreen
import com.example.ui.theme.FinTrackTheme

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels {
        val app = application as FinTrackApplication
        com.example.ui.MainViewModelFactory(app.container, app)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
            val systemDark = isSystemInDarkTheme()
            val isDarkTheme = when (themeMode) {
                "light" -> false
                "dark" -> true
                else -> systemDark
            }

            FinTrackTheme(darkTheme = isDarkTheme) {
                FinTrackApp(viewModel = viewModel)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.syncPendingConversions()
    }
}

@Composable
fun FinTrackApp(viewModel: MainViewModel) {
    val authState by viewModel.authState.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val filterSettings by viewModel.filterSettings.collectAsStateWithLifecycle()
    val metrics by viewModel.dashboardMetrics.collectAsStateWithLifecycle()
    val filteredTxs by viewModel.filteredTransactions.collectAsStateWithLifecycle()
    val periodFilteredTxs by viewModel.periodFilteredTransactions.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val categoryExpenseShares by viewModel.categoryExpenseShares.collectAsStateWithLifecycle()
    val categoryIncomeShares by viewModel.categoryIncomeShares.collectAsStateWithLifecycle()
    val monthlyDataPoints by viewModel.monthlyDataPoints.collectAsStateWithLifecycle()
    val smartInsights by viewModel.smartInsights.collectAsStateWithLifecycle()
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val syncStatus by viewModel.syncStatus.collectAsStateWithLifecycle()
    val migrationUiState by viewModel.migrationUiState.collectAsStateWithLifecycle()
    val canManageCategories by viewModel.canManageCategories.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.userNotification) {
        uiState.userNotification?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.dismissNotification()
        }
    }

    if (authState !is AuthState.SignedIn) {
        AuthScreen(
            authState = authState,
            onSignInWithGoogle = { idToken -> viewModel.signInWithGoogle(idToken) },
            onSignInWithTestUid = { testUid -> viewModel.signInWithTestUid(testUid) },
            onAuthError = { errorMsg -> viewModel.setAuthError(errorMsg) },
            onClearError = { viewModel.clearAuthError() }
        )
    } else {
        val signedInState = authState as AuthState.SignedIn
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            bottomBar = {
                FinTrackBottomNavigation(
                    selectedTabIndex = uiState.selectedTab,
                    onTabSelected = { viewModel.selectTab(it) }
                )
            },
            snackbarHost = { SnackbarHost(snackbarHostState) },
            modifier = Modifier
                .fillMaxSize()
                .testTag("main_app_scaffold")
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                when (uiState.selectedTab) {
                    0 -> DashboardScreen(
                        metrics = metrics,
                        filterSettings = filterSettings,
                        monthlyDataPoints = monthlyDataPoints,
                        categoryShares = categoryExpenseShares,
                        smartInsights = smartInsights,
                        onPeriodSelected = { viewModel.updateSelectedPeriod(it) },
                        onCurrencyChanged = { viewModel.updateSelectedCurrency(it) },
                        syncStatus = syncStatus
                    )

                    1 -> TransactionsScreen(
                        transactions = filteredTxs,
                        categories = categories,
                        filterSettings = filterSettings,
                        onPeriodSelected = { viewModel.updateSelectedPeriod(it) },
                        onCurrencyChanged = { viewModel.updateSelectedCurrency(it) },
                        onTypeFilterSelected = { type -> viewModel.updateSelectedTypeFilter(type) },
                        onCategoryFilterSelected = { type, cat -> viewModel.updateCategoryFilter(type, cat) },
                        onSearchQueryChanged = { viewModel.updateSearchQuery(it) },
                        onAddTransactionClicked = { viewModel.openNewTransactionDialog("Expense") },
                        onDuplicateClicked = { tx -> viewModel.openDuplicateTransactionDialog(tx) },
                        onEditClicked = { tx -> viewModel.openEditTransactionDialog(tx) },
                        onDeleteClicked = { tx -> viewModel.deleteTransaction(tx) }
                    )

                    2 -> AnalyticsScreen(
                        metrics = metrics,
                        filterSettings = filterSettings,
                        categoryExpenseShares = categoryExpenseShares,
                        categoryIncomeShares = categoryIncomeShares,
                        monthlyDataPoints = monthlyDataPoints,
                        insights = smartInsights,
                        onPeriodSelected = { viewModel.updateSelectedPeriod(it) },
                        onCurrencyChanged = { viewModel.updateSelectedCurrency(it) }
                    )

                    3 -> CategoriesScreen(
                        categories = categories,
                        canManageCategories = canManageCategories,
                        onAddCategory = { name, type, sub -> viewModel.addCategory(name, type, sub) },
                        onUpdateCategoryGroup = { oldName, newName, type -> viewModel.updateCategoryGroup(oldName, newName, type) },
                        onDeleteCategoryGroup = { name, type -> viewModel.deleteCategoryGroup(name, type) },
                        onUpdateSubcategory = { id, sub -> viewModel.updateSubcategory(id, sub) },
                        onDeleteSubcategory = { id -> viewModel.deleteSubcategory(id) }
                    )

                    4 -> {
                        val context = LocalContext.current
                        val allTxs by viewModel.allTransactions.collectAsStateWithLifecycle()
                        val currentHousehold by viewModel.currentHousehold.collectAsStateWithLifecycle()
                        val currentUserMembership by viewModel.currentUserMembership.collectAsStateWithLifecycle()
                        val householdMembers by viewModel.householdMembers.collectAsStateWithLifecycle()
                        val incomingInvites by viewModel.incomingInvites.collectAsStateWithLifecycle()
                        val isInvitationProcessing by viewModel.isInvitationProcessing.collectAsStateWithLifecycle()
                        val invitationError by viewModel.invitationError.collectAsStateWithLifecycle()
                        val householdCreationUiState by viewModel.householdCreationUiState.collectAsStateWithLifecycle()
                        SettingsScreen(
                            filterSettings = filterSettings,
                            themeMode = themeMode,
                            currentUid = signedInState.userUid,
                            currentUserEmail = signedInState.email,
                            currentHousehold = currentHousehold,
                            currentUserMembership = currentUserMembership,
                            householdMembers = householdMembers,
                            incomingInvites = incomingInvites,
                            isInvitationProcessing = isInvitationProcessing,
                            invitationError = invitationError,
                            onSendInvite = { email -> viewModel.sendInvite(email) },
                            onAcceptInvite = { inviteId -> viewModel.acceptInvite(inviteId) },
                            onDeclineInvite = { inviteId -> viewModel.declineInvite(inviteId) },
                            onClearInviteError = { viewModel.clearInvitationError() },
                            onSignOut = { viewModel.signOut() },
                            onCurrencyChanged = { viewModel.updateSelectedCurrency(it) },
                            onThemeModeChanged = { viewModel.updateThemeMode(it) },
                            onExportCsv = { CsvExporter.exportTransactionsToCsv(context, allTxs) },
                            onImportCsv = { uri -> viewModel.importCsv(context, uri) },
                            onRetryPendingConversions = { viewModel.retryPendingConversions() },
                            pendingRetryResult = uiState.pendingRetryResult,
                            onDismissRetryResult = { viewModel.dismissRetryResultDialog() },
                            onRunBnrDiagnostic = { viewModel.runBnrDiagnostic() },
                            debugDiagnosticResult = uiState.debugDiagnosticResult,
                            onDismissDebugDiagnostic = { viewModel.dismissDebugDiagnostic() },
                            isRetryingPending = uiState.isRetryingPending,
                            onStartMigration = { viewModel.startMigrationPreflight() },
                            householdCreationUiState = householdCreationUiState,
                            onCreateHousehold = { name -> viewModel.createHousehold(name) },
                            onResetHouseholdCreationState = { viewModel.resetHouseholdCreationState() }
                        )
                    }
                }

                // Migration Dialogs (Stage 3B)
                when (val mState = migrationUiState) {
                    is MigrationUiState.Preview -> {
                        MigrationPreviewDialog(
                            previewState = mState.preview,
                            onConfirm = { viewModel.confirmAndExecuteMigration() },
                            onCancel = { viewModel.cancelMigrationPreview() }
                        )
                    }
                    is MigrationUiState.Conflict -> {
                        MigrationConflictDialog(
                            conflictState = mState.conflict,
                            onDismiss = { viewModel.dismissMigrationDialogs() }
                        )
                    }
                    is MigrationUiState.Uploading -> {
                        MigrationProgressDialog(
                            progressState = mState.progress
                        )
                    }
                    is MigrationUiState.Success -> {
                        MigrationResultDialog(
                            resultState = mState.result,
                            onDismiss = { viewModel.dismissMigrationDialogs() }
                        )
                    }
                    is MigrationUiState.Failure -> {
                        MigrationResultDialog(
                            resultState = mState.failure,
                            onDismiss = { viewModel.dismissMigrationDialogs() }
                        )
                    }
                    else -> {}
                }

                // Transaction Form Dialog for Create / Edit / Duplicate
                if (uiState.showTransactionDialog) {
                    TransactionFormDialog(
                        initialTransaction = uiState.activeTransactionForEdit,
                        isDuplicateMode = uiState.isDuplicateMode,
                        categories = categories,
                        onDismiss = { viewModel.dismissTransactionDialog() },
                        onSearchDescriptions = { query -> viewModel.getDescriptionSuggestions(query) },
                        onSave = { id, date, desc, amt, type, acc, cat, sub, dest ->
                            viewModel.saveTransaction(id, date, desc, amt, type, acc, cat, sub, dest)
                        }
                    )
                }

                // CSV Import Preview Dialog
                uiState.csvPreviewData?.let { preview ->
                    val context = LocalContext.current
                    com.example.ui.components.CsvImportPreviewDialog(
                        previewData = preview,
                        onDuplicateModeChanged = { mode -> viewModel.updateCsvDuplicateMode(mode) },
                        onConfirmImport = { viewModel.executeCsvImport(context) },
                        onDismiss = { viewModel.dismissCsvPreview() }
                    )
                }

                // CSV Import Result Dialog
                uiState.csvImportFinalResult?.let { result ->
                    com.example.ui.components.CsvImportResultDialog(
                        result = result,
                        onDismiss = { viewModel.dismissCsvResult() }
                    )
                }
            }
        }
    }
}
