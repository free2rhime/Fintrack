package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.FilterSettings
import com.example.data.model.HouseholdDto
import com.example.data.model.HouseholdInviteDto
import com.example.data.model.HouseholdMemberDto
import com.example.data.repository.PendingRetryResult
import com.example.data.service.BnrDiagnosticResult
import com.example.ui.HouseholdCreationUiState
import com.example.ui.components.CreateHouseholdDialog
import com.example.ui.components.CurrencyToggle
import com.example.ui.components.HouseholdOverviewCard
import com.example.ui.components.InviteMemberDialog
import com.example.ui.theme.ExpenseRed

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.net.Uri
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.repository.SyncDiagnosticsHolder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SettingsScreen(
    filterSettings: FilterSettings,
    themeMode: String,
    currentUid: String? = null,
    currentUserEmail: String? = null,
    currentHousehold: HouseholdDto? = null,
    currentUserMembership: HouseholdMemberDto? = null,
    householdMembers: List<HouseholdMemberDto> = emptyList(),
    incomingInvites: List<HouseholdInviteDto> = emptyList(),
    isInvitationProcessing: Boolean = false,
    invitationError: String? = null,
    onSendInvite: (String) -> Unit = {},
    onAcceptInvite: (String) -> Unit = {},
    onDeclineInvite: (String) -> Unit = {},
    onClearInviteError: () -> Unit = {},
    onSignOut: () -> Unit = {},
    onCurrencyChanged: (String) -> Unit,
    onThemeModeChanged: (String) -> Unit,
    onExportCsv: () -> Unit,
    onImportCsv: (Uri) -> Unit = {},
    onSeedDemoData: () -> Unit,
    onResetData: () -> Unit,
    onRetryPendingConversions: () -> Unit = {},
    pendingRetryResult: PendingRetryResult? = null,
    onDismissRetryResult: () -> Unit = {},
    onRunBnrDiagnostic: () -> Unit = {},
    debugDiagnosticResult: BnrDiagnosticResult? = null,
    onDismissDebugDiagnostic: () -> Unit = {},
    isRetryingPending: Boolean = false,
    onStartMigration: () -> Unit = {},
    householdCreationUiState: HouseholdCreationUiState = HouseholdCreationUiState.Idle,
    onCreateHousehold: (String) -> Unit = {},
    onResetHouseholdCreationState: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var showInviteDialog by remember { mutableStateOf(false) }
    var showCreateHouseholdDialog by remember { mutableStateOf(false) }
    var showSyncDiagnosticDialog by remember { mutableStateOf(false) }

    LaunchedEffect(householdCreationUiState) {
        if (householdCreationUiState is HouseholdCreationUiState.Success) {
            showCreateHouseholdDialog = false
            onResetHouseholdCreationState()
        }
    }

    if (showCreateHouseholdDialog) {
        CreateHouseholdDialog(
            isCreating = householdCreationUiState is HouseholdCreationUiState.Creating,
            errorMessage = (householdCreationUiState as? HouseholdCreationUiState.Error)?.message,
            onCreateHousehold = { name ->
                onCreateHousehold(name)
            },
            onDismiss = {
                showCreateHouseholdDialog = false
                onResetHouseholdCreationState()
            }
        )
    }

    if (showInviteDialog) {
        InviteMemberDialog(
            isLoading = isInvitationProcessing,
            errorMessage = invitationError,
            onSendInvite = { email ->
                onSendInvite(email)
                showInviteDialog = false
            },
            onDismiss = {
                showInviteDialog = false
                onClearInviteError()
            }
        )
    }

    val csvPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { onImportCsv(it) }
    }
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
            .padding(bottom = 80.dp)
    ) {
        // Header
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Preferences & System",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // ACCOUNT & AUTHENTICATION CARD
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("account_info_card"),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Account Identity & Security",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Firebase UID: ${currentUid ?: "Not Signed In"}",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.testTag("account_uid_text")
                )

                if (!currentUserEmail.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Email: $currentUserEmail",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedButton(
                    onClick = onSignOut,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .testTag("sign_out_button"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Sign Out", fontWeight = FontWeight.Bold)
                }
            }
        }

        // PENDING INVITATIONS CARD
        if (!currentUserEmail.isNullOrBlank() && incomingInvites.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("pending_invitations_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Email,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Pending Invitations",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        incomingInvites.forEach { invite ->
                            val inviteId = invite.inviteId.orEmpty()
                            val householdName = invite.householdName.orEmpty().ifEmpty { "Household" }
                            val inviterEmail = invite.inviterEmail.orEmpty().ifEmpty { "Unknown" }
                            val formattedExpiry = remember(invite.expiresAt) {
                                val exp = invite.expiresAt
                                if (exp != null && exp > 0L) {
                                    val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                                    sdf.format(Date(exp))
                                } else {
                                    "7 days"
                                }
                            }

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("pending_invite_item"),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = householdName,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.testTag("invite_household_name")
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "From: $inviterEmail",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.testTag("invite_inviter_email")
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "Expires: $formattedExpiry",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.testTag("invite_expires_at")
                                    )

                                    Spacer(modifier = Modifier.height(10.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Button(
                                            onClick = { onAcceptInvite(inviteId) },
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(36.dp)
                                                .testTag("accept_invite_button"),
                                            shape = RoundedCornerShape(8.dp),
                                            enabled = !isInvitationProcessing
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "Accept",
                                                style = MaterialTheme.typography.labelLarge,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }

                                        OutlinedButton(
                                            onClick = { onDeclineInvite(inviteId) },
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(36.dp)
                                                .testTag("decline_invite_button"),
                                            shape = RoundedCornerShape(8.dp),
                                            enabled = !isInvitationProcessing
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "Decline",
                                                style = MaterialTheme.typography.labelLarge,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        if (currentHousehold != null) {
            Spacer(modifier = Modifier.height(16.dp))

            HouseholdOverviewCard(
                household = currentHousehold,
                currentUserMembership = currentUserMembership,
                householdMembers = householdMembers,
                currentUid = currentUid,
                onInviteMemberClick = { showInviteDialog = true }
            )
        } else {
            Spacer(modifier = Modifier.height(16.dp))

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("household_setup_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Home,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Household Setup",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Create a household to enable cloud synchronization of your financial data.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = { showCreateHouseholdDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .testTag("create_household_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Create Household", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // CURRENCY SELECTION CARD
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Display Currency",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Primary transactions recorded in RON",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    CurrencyToggle(
                        selectedCurrency = filterSettings.selectedCurrency,
                        onCurrencyChanged = onCurrencyChanged
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // THEME MODE CARD
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Palette,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Appearance Theme",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        selected = themeMode == "dark",
                        onClick = { onThemeModeChanged("dark") },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 3)
                    ) {
                        Text("Dark")
                    }

                    SegmentedButton(
                        selected = themeMode == "light",
                        onClick = { onThemeModeChanged("light") },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 3)
                    ) {
                        Text("Light")
                    }

                    SegmentedButton(
                        selected = themeMode == "system",
                        onClick = { onThemeModeChanged("system") },
                        shape = SegmentedButtonDefaults.itemShape(index = 2, count = 3)
                    ) {
                        Text("System")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // BACKEND ARCHITECTURE & DATA MODEL SPECIFICATION CARD
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("architecture_info_card"),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Storage,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Backend Architecture & Sync Specification",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "• Architecture Choice: Offline-First Hybrid Architecture (Room SQLite + Exchange Rate Caching + Cloud Sync Adapter)",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "• Historical Currency Conversion: Every transaction permanently computes and freezes the EUR value using the exact BNR exchange rate valid on that transaction date. Historical entries are never modified by future rate changes.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "• Cloud Readiness: Local database tables are modeled with UUID keys, updatedAt timestamps, and user isolation for sync adapters with Firebase Firestore, Cloud SQL, or Supabase.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(10.dp))

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = Icons.Default.CloudDone, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Status: 100% Offline-First Active with Zero Latency",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // CSV EXPORT DATA CARD
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Data Export & Reports",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Export all recorded transactions, exchange rates, and category metadata as a standard CSV file.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = onExportCsv,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("export_csv_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(imageVector = Icons.Default.FileDownload, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Export Transactions to CSV", fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedButton(
                    onClick = { csvPickerLauncher.launch("*/*") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("import_csv_button"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(imageVector = Icons.Default.Storage, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Import Transactions from CSV", fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // EUR EXCHANGE RATE CONVERSION CARD
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "EUR Exchange Rate Synchronization",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Retry fetching official BNR rates for pending EUR transaction conversions.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = onRetryPendingConversions,
                    enabled = !isRetryingPending,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("retry_eur_conversions_button"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(imageVector = Icons.Default.Sync, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (isRetryingPending) "Syncing..." else "Retry EUR Conversions", fontWeight = FontWeight.Bold)
                }

                if (com.example.BuildConfig.DEBUG) {
                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedButton(
                        onClick = onRunBnrDiagnostic,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("run_bnr_diagnostic_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Run BNR Endpoint Diagnostic (Debug)", fontWeight = FontWeight.SemiBold)
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedButton(
                        onClick = { showSyncDiagnosticDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("view_sync_diagnostic_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("View Sync Diagnostics (Debug)", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // CLOUD HOUSEHOLD MIGRATION CARD
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("cloud_migration_card"),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CloudUpload,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Cloud Household Migration",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Migrate your local financial data (transactions, custom categories, and exchange rates) to your active household cloud repository with preflight safety checks.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = onStartMigration,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("start_migration_button"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(imageVector = Icons.Default.CloudUpload, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Start Household Migration", fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // DEMO DATA & MAINTENANCE
        var showSeedDialog by remember { mutableStateOf(false) }
        var showResetDialog by remember { mutableStateOf(false) }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Database Management",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedButton(
                    onClick = { showSeedDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("seed_demo_data_button"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(imageVector = Icons.Default.Refresh, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Re-Seed Financial Demo Transactions")
                }

                Spacer(modifier = Modifier.height(10.dp))

                Button(
                    onClick = { showResetDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("reset_data_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ExpenseRed)
                ) {
                    Icon(imageVector = Icons.Default.DeleteForever, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Clear All Local Data", fontWeight = FontWeight.Bold)
                }
            }
        }

        if (showSeedDialog) {
            androidx.compose.material3.AlertDialog(
                onDismissRequest = { showSeedDialog = false },
                title = { Text("Load Demo Data") },
                text = { Text("Are you sure you want to load sample demo financial transactions? This will populate your database with representative transaction history.") },
                confirmButton = {
                    Button(
                        onClick = {
                            onSeedDemoData()
                            showSeedDialog = false
                        }
                    ) {
                        Text("Load Demo Data")
                    }
                },
                dismissButton = {
                    OutlinedButton(onClick = { showSeedDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        if (showResetDialog) {
            androidx.compose.material3.AlertDialog(
                onDismissRequest = { showResetDialog = false },
                title = { Text("Clear All Local Data") },
                text = { Text("Are you sure you want to delete all local transactions and data? This action cannot be undone.") },
                confirmButton = {
                    Button(
                        onClick = {
                            onResetData()
                            showResetDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ExpenseRed)
                    ) {
                        Text("Clear All Data", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    OutlinedButton(onClick = { showResetDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
        if (pendingRetryResult != null) {
            androidx.compose.material3.AlertDialog(
                onDismissRequest = onDismissRetryResult,
                title = { Text("EUR Conversions Retry Result", fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("• Pending before retry: ${pendingRetryResult.pendingBefore}")
                        Text("• Converted successfully: ${pendingRetryResult.convertedSuccessfully}")
                        Text("• Still pending: ${pendingRetryResult.stillPending}")
                        Text("• Failed: ${pendingRetryResult.failedCount}")
                        Text("• Main failure reason: ${pendingRetryResult.mainFailureReason ?: "None"}")
                    }
                },
                confirmButton = {
                    Button(onClick = onDismissRetryResult) {
                        Text("OK")
                    }
                }
            )
        }

        if (debugDiagnosticResult != null) {
            androidx.compose.material3.AlertDialog(
                onDismissRequest = onDismissDebugDiagnostic,
                title = { Text("BNR Endpoint Diagnostic (Debug)", fontWeight = FontWeight.Bold) },
                text = {
                    Column(
                        modifier = Modifier.verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text("• Reachable: ${debugDiagnosticResult.isReachable}")
                        Text("• HTTP Status: ${debugDiagnosticResult.httpStatus}")
                        Text("• Failure Category: ${debugDiagnosticResult.failureCategory}")
                        Text("• Publication Dates Parsed: ${debugDiagnosticResult.publicationDatesParsed}")
                        Text("• EUR Rate Found: ${debugDiagnosticResult.eurRateFound}")
                        Text("• Latest Publication Date: ${debugDiagnosticResult.latestPublicationDate ?: "N/A"}")
                        Text("• Requested URL: ${debugDiagnosticResult.requestedUrl}")
                        Text("• Final URL: ${debugDiagnosticResult.finalUrl}")
                        Text("• Content-Type: ${debugDiagnosticResult.contentType ?: "N/A"}")
                        Text("• Content-Encoding: ${debugDiagnosticResult.contentEncoding ?: "None"}")
                        Text("• Response Size: ${debugDiagnosticResult.responseByteCount} bytes")
                        Text("• Is HTML: ${debugDiagnosticResult.isHtml}")
                        Text("• XML Declaration: ${debugDiagnosticResult.hasXmlDeclaration}")
                        Text("• Root Element: ${debugDiagnosticResult.rootLocalName ?: "N/A"} (NS: ${debugDiagnosticResult.rootNamespaceUri ?: "None"})")
                        Text("• Element Counts: Cubes=${debugDiagnosticResult.cubeElementCount}, Rates=${debugDiagnosticResult.rateElementCount}, EUR=${debugDiagnosticResult.eurRateElementCount}")
                        Text("• Stages: A:${if (debugDiagnosticResult.stageA_httpConnection) "PASS" else "FAIL"} B:${if (debugDiagnosticResult.stageB_bodyObtained) "PASS" else "FAIL"} C:${if (debugDiagnosticResult.stageC_xmlOpened) "PASS" else "FAIL"} D:${if (debugDiagnosticResult.stageD_cubeFound) "PASS" else "FAIL"} E:${if (debugDiagnosticResult.stageE_rateFound) "PASS" else "FAIL"} F:${if (debugDiagnosticResult.stageF_eurFound) "PASS" else "FAIL"} G:${if (debugDiagnosticResult.stageG_validRatesProduced) "PASS" else "FAIL"}")
                        if (!debugDiagnosticResult.sanitizedPreview.isNullOrBlank()) {
                            Text("• Preview: ${debugDiagnosticResult.sanitizedPreview}")
                        }
                    }
                },
                confirmButton = {
                    Button(onClick = onDismissDebugDiagnostic) {
                        Text("OK")
                    }
                }
            )
        }

        if (showSyncDiagnosticDialog) {
            val diagnosticRecord by SyncDiagnosticsHolder.lastError.collectAsStateWithLifecycle()
            val clipboardManager = LocalClipboardManager.current
            var copiedToast by remember { mutableStateOf(false) }

            androidx.compose.material3.AlertDialog(
                onDismissRequest = {
                    showSyncDiagnosticDialog = false
                    copiedToast = false
                },
                title = { Text("Sync Diagnostics (Debug)", fontWeight = FontWeight.Bold) },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (diagnosticRecord == null) {
                            Text("No sync errors currently recorded in this session.")
                        } else {
                            val record = diagnosticRecord!!
                            Text("• Timestamp: ${record.formattedTime}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                            Text("• Operation: ${record.operation}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                            Text("• Exception Code: ${record.exceptionCode ?: "N/A"}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                            Text("• User UID: ${record.userUid ?: "None"}", style = MaterialTheme.typography.bodySmall)
                            Text("• Household ID: ${record.householdId ?: "None"}", style = MaterialTheme.typography.bodySmall)
                            Text("• Message: ${record.exceptionMessage ?: "None"}", style = MaterialTheme.typography.bodySmall)
                            if (!record.stackTraceSnippet.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("• Stack Trace Snippet:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = record.stackTraceSnippet,
                                        style = MaterialTheme.typography.labelSmall,
                                        modifier = Modifier.padding(8.dp)
                                    )
                                }
                            }
                            if (copiedToast) {
                                Text(
                                    text = "Copied to clipboard!",
                                    color = MaterialTheme.colorScheme.primary,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (diagnosticRecord != null) {
                            val fullText = buildString {
                                val r = diagnosticRecord!!
                                appendLine("--- FinTrack Sync Diagnostics ---")
                                appendLine("Timestamp: ${r.formattedTime}")
                                appendLine("Operation: ${r.operation}")
                                appendLine("Exception Code: ${r.exceptionCode}")
                                appendLine("User UID: ${r.userUid}")
                                appendLine("Household ID: ${r.householdId}")
                                appendLine("Message: ${r.exceptionMessage}")
                                appendLine("Stack Trace:\n${r.stackTraceSnippet}")
                            }
                            Button(
                                onClick = {
                                    clipboardManager.setText(AnnotatedString(fullText))
                                    copiedToast = true
                                }
                            ) {
                                Text("Copy")
                            }
                        }
                        OutlinedButton(onClick = {
                            showSyncDiagnosticDialog = false
                            copiedToast = false
                        }) {
                            Text("Close")
                        }
                    }
                }
            )
        }
    }
}
