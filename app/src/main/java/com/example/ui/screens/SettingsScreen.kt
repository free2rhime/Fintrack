package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.FilterSettings
import com.example.data.model.HouseholdDto
import com.example.data.model.HouseholdInviteDto
import com.example.data.model.HouseholdMemberDto
import com.example.data.repository.PendingRetryResult
import com.example.data.repository.SyncDiagnosticsHolder
import com.example.data.service.BnrDiagnosticResult
import com.example.ui.HouseholdCreationUiState
import com.example.ui.components.BadgeVariant
import com.example.ui.components.ButtonVariant
import com.example.ui.components.CreateHouseholdDialog
import com.example.ui.components.CurrencyToggle
import com.example.ui.components.FinTrackButton
import com.example.ui.components.FinTrackCard
import com.example.ui.components.FinTrackSegmentedControl
import com.example.ui.components.FinTrackStatusBadge
import com.example.ui.components.HouseholdOverviewCard
import com.example.ui.components.InviteMemberDialog
import com.example.ui.theme.BodyRegular
import com.example.ui.theme.CanvasDark
import com.example.ui.theme.CardTitleAmount
import com.example.ui.theme.CobaltBlue
import com.example.ui.theme.ExpenseCoral
import com.example.ui.theme.LabelBadgeMedium
import com.example.ui.theme.MicroMetadata
import com.example.ui.theme.RadiusLarge
import com.example.ui.theme.RadiusMedium
import com.example.ui.theme.RadiusSmall
import com.example.ui.theme.RadiusXLarge
import com.example.ui.theme.SectionHeadline
import com.example.ui.theme.Space12
import com.example.ui.theme.Space16
import com.example.ui.theme.Space20
import com.example.ui.theme.Space4
import com.example.ui.theme.Space8
import com.example.ui.theme.SurfaceContainerDark
import com.example.ui.theme.SurfaceContainerHighDark
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WarningAmber
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

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(CanvasDark),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 680.dp)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Space16, vertical = Space20)
                .padding(bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(Space16)
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(RadiusMedium))
                        .background(CobaltBlue.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = null,
                        tint = CobaltBlue,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(Space12))
                Column {
                    Text(
                        text = "Preferences & System",
                        style = SectionHeadline,
                        color = TextPrimary
                    )
                    Text(
                        text = "Account, household sync, and data preferences",
                        style = MicroMetadata,
                        color = TextSecondary
                    )
                }
            }

            // ACCOUNT & AUTHENTICATION CARD
            FinTrackCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("account_info_card"),
                containerColor = SurfaceDark,
                shape = RoundedCornerShape(RadiusLarge),
                border = BorderStroke(1.dp, SurfaceContainerHighDark.copy(alpha = 0.5f))
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(Space12)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(CobaltBlue.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    tint = CobaltBlue,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(Space8))
                            Text(
                                text = "Account Identity & Security",
                                style = CardTitleAmount,
                                color = TextPrimary
                            )
                        }

                        if (currentUid != null) {
                            FinTrackStatusBadge(
                                label = "Active",
                                variant = BadgeVariant.SUCCESS
                            )
                        }
                    }

                    // User Identity Box
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(RadiusMedium),
                        color = SurfaceContainerDark
                    ) {
                        Column(modifier = Modifier.padding(Space12)) {
                            Text(
                                text = "Firebase UID: ${currentUid ?: "Not Signed In"}",
                                style = MicroMetadata,
                                fontWeight = FontWeight.Bold,
                                color = CobaltBlue,
                                modifier = Modifier.testTag("account_uid_text")
                            )

                            if (!currentUserEmail.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(Space4))
                                Text(
                                    text = "Email: $currentUserEmail",
                                    style = BodyRegular,
                                    color = TextSecondary
                                )
                            }
                        }
                    }

                    FinTrackButton(
                        onClick = onSignOut,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("sign_out_button"),
                        variant = ButtonVariant.DESTRUCTIVE,
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    ) {
                        Text(
                            text = "Sign Out",
                            style = LabelBadgeMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // PENDING INVITATIONS CARD
            if (!currentUserEmail.isNullOrBlank() && incomingInvites.isNotEmpty()) {
                FinTrackCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("pending_invitations_card"),
                    containerColor = SurfaceDark,
                    shape = RoundedCornerShape(RadiusLarge),
                    border = BorderStroke(1.dp, SurfaceContainerHighDark.copy(alpha = 0.5f))
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(Space12)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(CobaltBlue.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Email,
                                    contentDescription = null,
                                    tint = CobaltBlue,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(Space8))
                            Text(
                                text = "Pending Invitations",
                                style = CardTitleAmount,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            FinTrackStatusBadge(
                                label = "${incomingInvites.size} Pending",
                                variant = BadgeVariant.WARNING
                            )
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(Space12)) {
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

                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("pending_invite_item"),
                                    shape = RoundedCornerShape(RadiusMedium),
                                    color = SurfaceContainerDark
                                ) {
                                    Column(modifier = Modifier.padding(Space12)) {
                                        Text(
                                            text = householdName,
                                            style = CardTitleAmount,
                                            color = TextPrimary,
                                            modifier = Modifier.testTag("invite_household_name")
                                        )
                                        Spacer(modifier = Modifier.height(Space4))
                                        Text(
                                            text = "From: $inviterEmail",
                                            style = BodyRegular,
                                            color = TextSecondary,
                                            modifier = Modifier.testTag("invite_inviter_email")
                                        )
                                        Spacer(modifier = Modifier.height(Space4))
                                        Text(
                                            text = "Expires: $formattedExpiry",
                                            style = MicroMetadata,
                                            color = WarningAmber,
                                            modifier = Modifier.testTag("invite_expires_at")
                                        )

                                        Spacer(modifier = Modifier.height(Space12))

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(Space8)
                                        ) {
                                            FinTrackButton(
                                                onClick = { onAcceptInvite(inviteId) },
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .testTag("accept_invite_button"),
                                                variant = ButtonVariant.PRIMARY,
                                                enabled = !isInvitationProcessing,
                                                leadingIcon = {
                                                    Icon(
                                                        imageVector = Icons.Default.Check,
                                                        contentDescription = null,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }
                                            ) {
                                                Text(
                                                    text = "Accept",
                                                    style = LabelBadgeMedium,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }

                                            FinTrackButton(
                                                onClick = { onDeclineInvite(inviteId) },
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .testTag("decline_invite_button"),
                                                variant = ButtonVariant.DESTRUCTIVE,
                                                enabled = !isInvitationProcessing,
                                                leadingIcon = {
                                                    Icon(
                                                        imageVector = Icons.Default.Close,
                                                        contentDescription = null,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }
                                            ) {
                                                Text(
                                                    text = "Decline",
                                                    style = LabelBadgeMedium,
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

            // HOUSEHOLD SECTION
            if (currentHousehold != null) {
                HouseholdOverviewCard(
                    household = currentHousehold,
                    currentUserMembership = currentUserMembership,
                    householdMembers = householdMembers,
                    currentUid = currentUid,
                    onInviteMemberClick = { showInviteDialog = true }
                )
            } else {
                FinTrackCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("household_setup_card"),
                    containerColor = SurfaceDark,
                    shape = RoundedCornerShape(RadiusLarge),
                    border = BorderStroke(1.dp, SurfaceContainerHighDark.copy(alpha = 0.5f))
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(Space12)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(CobaltBlue.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Home,
                                    contentDescription = null,
                                    tint = CobaltBlue,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(Space8))
                            Text(
                                text = "Household Setup",
                                style = CardTitleAmount,
                                color = TextPrimary
                            )
                        }

                        Text(
                            text = "Create a household to enable cloud synchronization of your financial data.",
                            style = BodyRegular,
                            color = TextSecondary
                        )

                        FinTrackButton(
                            onClick = { showCreateHouseholdDialog = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("create_household_button"),
                            variant = ButtonVariant.PRIMARY,
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        ) {
                            Text(
                                text = "Create Household",
                                style = LabelBadgeMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // CURRENCY SELECTION CARD
            FinTrackCard(
                modifier = Modifier.fillMaxWidth(),
                containerColor = SurfaceDark,
                shape = RoundedCornerShape(RadiusLarge),
                border = BorderStroke(1.dp, SurfaceContainerHighDark.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f, fill = false)) {
                        Text(
                            text = "Display Currency",
                            style = CardTitleAmount,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(Space4))
                        Text(
                            text = "Primary transactions recorded in RON",
                            style = MicroMetadata,
                            color = TextSecondary
                        )
                    }

                    CurrencyToggle(
                        selectedCurrency = filterSettings.selectedCurrency,
                        onCurrencyChanged = onCurrencyChanged
                    )
                }
            }

            // THEME MODE CARD
            FinTrackCard(
                modifier = Modifier.fillMaxWidth(),
                containerColor = SurfaceDark,
                shape = RoundedCornerShape(RadiusLarge),
                border = BorderStroke(1.dp, SurfaceContainerHighDark.copy(alpha = 0.5f))
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(Space12)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(CobaltBlue.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Palette,
                                contentDescription = null,
                                tint = CobaltBlue,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(Space8))
                        Text(
                            text = "Appearance Theme",
                            style = CardTitleAmount,
                            color = TextPrimary
                        )
                    }

                    val themeOptions = listOf("Dark", "Light", "System")
                    val selectedIndex = when (themeMode) {
                        "light" -> 1
                        "system" -> 2
                        else -> 0
                    }

                    FinTrackSegmentedControl(
                        items = themeOptions,
                        selectedIndex = selectedIndex,
                        onItemSelected = { index ->
                            val selectedTheme = when (index) {
                                1 -> "light"
                                2 -> "system"
                                else -> "dark"
                            }
                            onThemeModeChanged(selectedTheme)
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // CSV EXPORT DATA CARD
            FinTrackCard(
                modifier = Modifier.fillMaxWidth(),
                containerColor = SurfaceDark,
                shape = RoundedCornerShape(RadiusLarge),
                border = BorderStroke(1.dp, SurfaceContainerHighDark.copy(alpha = 0.5f))
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(Space12)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(CobaltBlue.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.FileDownload,
                                contentDescription = null,
                                tint = CobaltBlue,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(Space8))
                        Text(
                            text = "Data Export & Reports",
                            style = CardTitleAmount,
                            color = TextPrimary
                        )
                    }

                    Text(
                        text = "Export all recorded transactions, exchange rates, and category metadata as a standard CSV file.",
                        style = BodyRegular,
                        color = TextSecondary
                    )

                    FinTrackButton(
                        onClick = onExportCsv,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("export_csv_button"),
                        variant = ButtonVariant.PRIMARY,
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.FileDownload,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    ) {
                        Text(
                            text = "Export Transactions to CSV",
                            style = LabelBadgeMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    FinTrackButton(
                        onClick = { csvPickerLauncher.launch("*/*") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("import_csv_button"),
                        variant = ButtonVariant.SECONDARY,
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Storage,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    ) {
                        Text(
                            text = "Import Transactions from CSV",
                            style = LabelBadgeMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // EUR EXCHANGE RATE CONVERSION CARD
            FinTrackCard(
                modifier = Modifier.fillMaxWidth(),
                containerColor = SurfaceDark,
                shape = RoundedCornerShape(RadiusLarge),
                border = BorderStroke(1.dp, SurfaceContainerHighDark.copy(alpha = 0.5f))
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(Space12)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(CobaltBlue.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Sync,
                                contentDescription = null,
                                tint = CobaltBlue,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(Space8))
                        Text(
                            text = "EUR Exchange Rate Synchronization",
                            style = CardTitleAmount,
                            color = TextPrimary
                        )
                    }

                    Text(
                        text = "Retry fetching official BNR rates for pending EUR transaction conversions.",
                        style = BodyRegular,
                        color = TextSecondary
                    )

                    FinTrackButton(
                        onClick = onRetryPendingConversions,
                        enabled = !isRetryingPending,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("retry_eur_conversions_button"),
                        variant = ButtonVariant.PRIMARY,
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Sync,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    ) {
                        Text(
                            text = if (isRetryingPending) "Syncing..." else "Retry EUR Conversions",
                            style = LabelBadgeMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (com.example.BuildConfig.DEBUG) {
                        FinTrackButton(
                            onClick = onRunBnrDiagnostic,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("run_bnr_diagnostic_button"),
                            variant = ButtonVariant.SECONDARY
                        ) {
                            Text(
                                text = "Run BNR Endpoint Diagnostic (Debug)",
                                style = LabelBadgeMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        FinTrackButton(
                            onClick = { showSyncDiagnosticDialog = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("view_sync_diagnostic_button"),
                            variant = ButtonVariant.SECONDARY
                        ) {
                            Text(
                                text = "View Sync Diagnostics (Debug)",
                                style = LabelBadgeMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }

    // Pending Retry Result Dialog
    if (pendingRetryResult != null) {
        AlertDialog(
            onDismissRequest = onDismissRetryResult,
            shape = RoundedCornerShape(RadiusXLarge),
            containerColor = SurfaceDark,
            titleContentColor = TextPrimary,
            textContentColor = TextSecondary,
            title = {
                Text(
                    text = "EUR Conversions Retry Result",
                    style = CardTitleAmount,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(Space8)) {
                    Text(
                        text = "• Pending before retry: ${pendingRetryResult.pendingBefore}",
                        style = BodyRegular,
                        color = TextSecondary
                    )
                    Text(
                        text = "• Converted successfully: ${pendingRetryResult.convertedSuccessfully}",
                        style = BodyRegular,
                        color = TextSecondary
                    )
                    Text(
                        text = "• Still pending: ${pendingRetryResult.stillPending}",
                        style = BodyRegular,
                        color = TextSecondary
                    )
                    Text(
                        text = "• Failed: ${pendingRetryResult.failedCount}",
                        style = BodyRegular,
                        color = TextSecondary
                    )
                    Text(
                        text = "• Main failure reason: ${pendingRetryResult.mainFailureReason ?: "None"}",
                        style = BodyRegular,
                        color = TextSecondary
                    )
                }
            },
            confirmButton = {
                FinTrackButton(
                    onClick = onDismissRetryResult,
                    variant = ButtonVariant.PRIMARY
                ) {
                    Text("OK", style = LabelBadgeMedium, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    // Debug BNR Diagnostic Result Dialog
    if (debugDiagnosticResult != null) {
        AlertDialog(
            onDismissRequest = onDismissDebugDiagnostic,
            shape = RoundedCornerShape(RadiusXLarge),
            containerColor = SurfaceDark,
            titleContentColor = TextPrimary,
            textContentColor = TextSecondary,
            title = {
                Text(
                    text = "BNR Endpoint Diagnostic (Debug)",
                    style = CardTitleAmount,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(Space4)
                ) {
                    Text("• Reachable: ${debugDiagnosticResult.isReachable}", style = MicroMetadata, color = TextSecondary)
                    Text("• HTTP Status: ${debugDiagnosticResult.httpStatus}", style = MicroMetadata, color = TextSecondary)
                    Text("• Failure Category: ${debugDiagnosticResult.failureCategory}", style = MicroMetadata, color = TextSecondary)
                    Text("• Publication Dates Parsed: ${debugDiagnosticResult.publicationDatesParsed}", style = MicroMetadata, color = TextSecondary)
                    Text("• EUR Rate Found: ${debugDiagnosticResult.eurRateFound}", style = MicroMetadata, color = TextSecondary)
                    Text("• Latest Publication Date: ${debugDiagnosticResult.latestPublicationDate ?: "N/A"}", style = MicroMetadata, color = TextSecondary)
                    Text("• Requested URL: ${debugDiagnosticResult.requestedUrl}", style = MicroMetadata, color = TextSecondary)
                    Text("• Final URL: ${debugDiagnosticResult.finalUrl}", style = MicroMetadata, color = TextSecondary)
                    Text("• Content-Type: ${debugDiagnosticResult.contentType ?: "N/A"}", style = MicroMetadata, color = TextSecondary)
                    Text("• Content-Encoding: ${debugDiagnosticResult.contentEncoding ?: "None"}", style = MicroMetadata, color = TextSecondary)
                    Text("• Response Size: ${debugDiagnosticResult.responseByteCount} bytes", style = MicroMetadata, color = TextSecondary)
                    Text("• Is HTML: ${debugDiagnosticResult.isHtml}", style = MicroMetadata, color = TextSecondary)
                    Text("• XML Declaration: ${debugDiagnosticResult.hasXmlDeclaration}", style = MicroMetadata, color = TextSecondary)
                    Text("• Root Element: ${debugDiagnosticResult.rootLocalName ?: "N/A"} (NS: ${debugDiagnosticResult.rootNamespaceUri ?: "None"})", style = MicroMetadata, color = TextSecondary)
                    Text("• Element Counts: Cubes=${debugDiagnosticResult.cubeElementCount}, Rates=${debugDiagnosticResult.rateElementCount}, EUR=${debugDiagnosticResult.eurRateElementCount}", style = MicroMetadata, color = TextSecondary)
                    Text("• Stages: A:${if (debugDiagnosticResult.stageA_httpConnection) "PASS" else "FAIL"} B:${if (debugDiagnosticResult.stageB_bodyObtained) "PASS" else "FAIL"} C:${if (debugDiagnosticResult.stageC_xmlOpened) "PASS" else "FAIL"} D:${if (debugDiagnosticResult.stageD_cubeFound) "PASS" else "FAIL"} E:${if (debugDiagnosticResult.stageE_rateFound) "PASS" else "FAIL"} F:${if (debugDiagnosticResult.stageF_eurFound) "PASS" else "FAIL"} G:${if (debugDiagnosticResult.stageG_validRatesProduced) "PASS" else "FAIL"}", style = MicroMetadata, color = TextSecondary)
                    if (!debugDiagnosticResult.sanitizedPreview.isNullOrBlank()) {
                        Text("• Preview: ${debugDiagnosticResult.sanitizedPreview}", style = MicroMetadata, color = TextSecondary)
                    }
                }
            },
            confirmButton = {
                FinTrackButton(
                    onClick = onDismissDebugDiagnostic,
                    variant = ButtonVariant.PRIMARY
                ) {
                    Text("OK", style = LabelBadgeMedium, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    // Sync Diagnostic Dialog
    if (showSyncDiagnosticDialog) {
        val diagnosticRecord by SyncDiagnosticsHolder.lastError.collectAsStateWithLifecycle()
        val clipboardManager = LocalClipboardManager.current
        var copiedToast by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = {
                showSyncDiagnosticDialog = false
                copiedToast = false
            },
            shape = RoundedCornerShape(RadiusXLarge),
            containerColor = SurfaceDark,
            titleContentColor = TextPrimary,
            textContentColor = TextSecondary,
            title = {
                Text(
                    text = "Sync Diagnostics (Debug)",
                    style = CardTitleAmount,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(Space8)
                ) {
                    if (diagnosticRecord == null) {
                        Text(
                            text = "No sync errors currently recorded in this session.",
                            style = BodyRegular,
                            color = TextSecondary
                        )
                    } else {
                        val record = diagnosticRecord!!
                        Text("• Timestamp: ${record.formattedTime}", style = MicroMetadata, fontWeight = FontWeight.Bold, color = TextPrimary)
                        Text("• Operation: ${record.operation}", style = MicroMetadata, fontWeight = FontWeight.Bold, color = TextPrimary)
                        Text("• Exception Code: ${record.exceptionCode ?: "N/A"}", style = MicroMetadata, color = ExpenseCoral, fontWeight = FontWeight.Bold)
                        Text("• User UID: ${record.userUid ?: "None"}", style = MicroMetadata, color = TextSecondary)
                        Text("• Household ID: ${record.householdId ?: "None"}", style = MicroMetadata, color = TextSecondary)
                        Text("• Message: ${record.exceptionMessage ?: "None"}", style = MicroMetadata, color = TextSecondary)
                        if (!record.stackTraceSnippet.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(Space4))
                            Text("• Stack Trace Snippet:", style = MicroMetadata, fontWeight = FontWeight.Bold, color = TextPrimary)
                            Surface(
                                shape = RoundedCornerShape(RadiusSmall),
                                color = SurfaceContainerDark,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = record.stackTraceSnippet,
                                    style = MicroMetadata,
                                    color = TextSecondary,
                                    modifier = Modifier.padding(Space8)
                                )
                            }
                        }
                        if (copiedToast) {
                            Text(
                                text = "Copied to clipboard!",
                                color = CobaltBlue,
                                style = LabelBadgeMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(Space8)) {
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
                        FinTrackButton(
                            onClick = {
                                clipboardManager.setText(AnnotatedString(fullText))
                                copiedToast = true
                            },
                            variant = ButtonVariant.PRIMARY
                        ) {
                            Text("Copy", style = LabelBadgeMedium, fontWeight = FontWeight.Bold)
                        }
                    }
                    FinTrackButton(
                        onClick = {
                            showSyncDiagnosticDialog = false
                            copiedToast = false
                        },
                        variant = ButtonVariant.SECONDARY
                    ) {
                        Text("Close", style = LabelBadgeMedium, fontWeight = FontWeight.Bold)
                    }
                }
            }
        )
    }
}
