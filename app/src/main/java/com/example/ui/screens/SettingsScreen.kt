package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.snap
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
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
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
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
import com.example.data.repository.SyncStatus
import com.example.data.service.BnrDiagnosticResult
import com.example.ui.HouseholdCreationUiState
import com.example.ui.components.BadgeVariant
import com.example.ui.components.ButtonVariant
import com.example.ui.components.CreateHouseholdDialog
import com.example.ui.components.CurrencyToggle
import com.example.ui.components.FinTrackButton
import com.example.ui.components.FinTrackEmptyState
import com.example.ui.components.FinTrackSegmentedControl
import com.example.ui.components.FinTrackStatusBadge
import com.example.ui.components.FinTrackSyncStatus
import com.example.ui.components.HouseholdOverviewCard
import com.example.ui.components.InviteMemberDialog
import com.example.ui.theme.BodyRegular
import com.example.ui.theme.CardTitleAmount
import com.example.ui.theme.FinTrackMotion
import com.example.ui.theme.FinTrackTheme
import com.example.ui.theme.LabelBadgeMedium
import com.example.ui.theme.MaxContentWidthTablet
import com.example.ui.theme.MicroMetadata
import com.example.ui.theme.RadiusMedium
import com.example.ui.theme.RadiusSmall
import com.example.ui.theme.SectionHeadline
import com.example.ui.theme.ShapeExtraLarge
import com.example.ui.theme.ShapeGroupedContainer
import com.example.ui.theme.Space12
import com.example.ui.theme.Space16
import com.example.ui.theme.Space20
import com.example.ui.theme.Space4
import com.example.ui.theme.Space8
import com.example.ui.theme.SpacingBottomNavContent
import com.example.ui.theme.isReducedMotionEnabled
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Material 3 Expressive Settings & Household Control Center.
 *
 * Implements M3-8 visual and interaction modernization:
 * - Fluid grouped surface geometry (`ShapeGroupedContainer`)
 * - Continuous tonal sections for Appearance, Account, Household, Data, and Diagnostics
 * - Full RBAC preservation (Owner invite affordances vs Member read-only)
 * - Accessible >= 48dp touch targets and Role semantics
 * - Zero modification of persistence, synchronization, and BNR financial engines
 */
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
    syncStatus: SyncStatus = SyncStatus.SignedOut,
    modifier: Modifier = Modifier
) {
    val isReducedMotion = isReducedMotionEnabled()
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
            .background(MaterialTheme.colorScheme.background)
            .testTag("settings_screen_root"),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = MaxContentWidthTablet)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Space16, vertical = Space8)
                .padding(bottom = SpacingBottomNavContent),
            verticalArrangement = Arrangement.spacedBy(Space16)
        ) {
            // EXPRESSIVE HEADER
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(RadiusMedium))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(Space12))
                Column {
                    Text(
                        text = "Settings",
                        style = SectionHeadline,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.semantics { heading() }
                    )
                    Text(
                        text = "Preferences & System",
                        style = MicroMetadata,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Account, household sync, and data preferences",
                        style = MicroMetadata,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // SYSTEM SYNC STATUS
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start
            ) {
                FinTrackSyncStatus(
                    syncStatus = syncStatus,
                    modifier = Modifier.testTag("sync_status_indicator")
                )
            }

            // APPEARANCE & DISPLAY PREFERENCES SECTION
            Surface(
                shape = ShapeGroupedContainer,
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(Space16),
                    verticalArrangement = Arrangement.spacedBy(Space12)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(RadiusSmall))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Palette,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(Space8))
                        Column {
                            Text(
                                text = "Appearance Theme",
                                style = CardTitleAmount,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.semantics { heading() }
                            )
                            Text(
                                text = "Light / Dark / System visual styling",
                                style = MicroMetadata,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
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

                    Spacer(modifier = Modifier.height(Space4))
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant,
                        thickness = 1.dp
                    )
                    Spacer(modifier = Modifier.height(Space4))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f, fill = false)) {
                            Text(
                                text = "Display Currency",
                                style = CardTitleAmount,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(Space4))
                            Text(
                                text = "Primary transactions recorded in RON",
                                style = MicroMetadata,
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

            // ACCOUNT & AUTHENTICATION SECTION
            Surface(
                shape = ShapeGroupedContainer,
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("account_info_card")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(Space16),
                    verticalArrangement = Arrangement.spacedBy(Space12)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(RoundedCornerShape(RadiusSmall))
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(Space8))
                            Column {
                                Text(
                                    text = "Account Identity & Security",
                                    style = CardTitleAmount,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.semantics { heading() }
                                )
                                Text(
                                    text = "Authentication and session controls",
                                    style = MicroMetadata,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        if (currentUid != null) {
                            FinTrackStatusBadge(
                                label = "Active",
                                variant = BadgeVariant.SUCCESS
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(RadiusMedium),
                        color = MaterialTheme.colorScheme.surfaceContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(Space12),
                            verticalArrangement = Arrangement.spacedBy(Space4)
                        ) {
                            if (!currentUserEmail.isNullOrBlank()) {
                                Text(
                                    text = "Email: $currentUserEmail",
                                    style = BodyRegular,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.testTag("user_email_text")
                                )
                            }
                            Text(
                                text = "Firebase UID: ${currentUid ?: "Anonymous"}",
                                style = MicroMetadata,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier
                                    .testTag("account_uid_text")
                                    .testTag("user_uid_text")
                            )
                        }
                    }

                    FinTrackButton(
                        onClick = onSignOut,
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 48.dp)
                            .testTag("sign_out_button"),
                        variant = ButtonVariant.DESTRUCTIVE
                    ) {
                        Text(
                            text = "Sign Out",
                            style = LabelBadgeMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // PENDING INVITATIONS SECTION
            AnimatedVisibility(
                visible = !currentUserEmail.isNullOrBlank() && incomingInvites.isNotEmpty(),
                enter = if (isReducedMotion) fadeIn(snap()) else fadeIn(FinTrackMotion.contentEntranceSpec()) + expandVertically(FinTrackMotion.contentEntranceSpec()),
                exit = if (isReducedMotion) fadeOut(snap()) else fadeOut(FinTrackMotion.contentChangeSpec()) + shrinkVertically(FinTrackMotion.contentChangeSpec())
            ) {
                Surface(
                    shape = ShapeGroupedContainer,
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("pending_invitations_card")
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(Space16),
                        verticalArrangement = Arrangement.spacedBy(Space12)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(RoundedCornerShape(RadiusSmall))
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Email,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(Space8))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Pending Invitations",
                                    style = CardTitleAmount,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Household membership requests",
                                    style = MicroMetadata,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
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
                                    color = MaterialTheme.colorScheme.surfaceContainer
                                ) {
                                    Column(modifier = Modifier.padding(Space12)) {
                                        Text(
                                            text = householdName,
                                            style = CardTitleAmount,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.testTag("invite_household_name")
                                        )
                                        Spacer(modifier = Modifier.height(Space4))
                                        Text(
                                            text = "From: $inviterEmail",
                                            style = BodyRegular,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.testTag("invite_inviter_email")
                                        )
                                        Spacer(modifier = Modifier.height(Space4))
                                        Text(
                                            text = "Expires: $formattedExpiry",
                                            style = MicroMetadata,
                                            color = FinTrackTheme.colors.warning,
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
                                                    .defaultMinSize(minHeight = 48.dp)
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
                                                    .defaultMinSize(minHeight = 48.dp)
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
                Surface(
                    shape = ShapeGroupedContainer,
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("household_setup_card")
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(Space16),
                        verticalArrangement = Arrangement.spacedBy(Space12)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(RadiusMedium))
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Home,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(Space12))
                            Column {
                                Text(
                                    text = "Household Setup",
                                    style = CardTitleAmount,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Cloud synchronization directory",
                                    style = MicroMetadata,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Text(
                            text = "Create a household to enable cloud synchronization of your financial data.",
                            style = BodyRegular,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        FinTrackButton(
                            onClick = { showCreateHouseholdDialog = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .defaultMinSize(minHeight = 48.dp)
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

            // DATA EXPORT & REPORTS SECTION
            Surface(
                shape = ShapeGroupedContainer,
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(Space16),
                    verticalArrangement = Arrangement.spacedBy(Space12)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(RadiusMedium))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.FileDownload,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(Space12))
                        Column {
                            Text(
                                text = "Data Export & Reports",
                                style = CardTitleAmount,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.semantics { heading() }
                            )
                            Text(
                                text = "Export and import transaction data",
                                style = MicroMetadata,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Text(
                        text = "Export all recorded transactions, exchange rates, and category metadata as a standard CSV file.",
                        style = BodyRegular,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    FinTrackButton(
                        onClick = onExportCsv,
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 48.dp)
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
                            .defaultMinSize(minHeight = 48.dp)
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

                    Spacer(modifier = Modifier.height(Space4))
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant,
                        thickness = 1.dp
                    )
                    Spacer(modifier = Modifier.height(Space4))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(RadiusSmall))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Sync,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(Space8))
                        Column {
                            Text(
                                text = "EUR Exchange Rate Synchronization",
                                style = CardTitleAmount,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Official BNR exchange rates",
                                style = MicroMetadata,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Text(
                        text = "Retry fetching official BNR rates for pending EUR transaction conversions.",
                        style = BodyRegular,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    FinTrackButton(
                        onClick = onRetryPendingConversions,
                        enabled = !isRetryingPending,
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 48.dp)
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
                }
            }

            // SYSTEM & DIAGNOSTICS SECTION
            Surface(
                shape = ShapeGroupedContainer,
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(Space16),
                    verticalArrangement = Arrangement.spacedBy(Space12)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(RadiusSmall))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Sync,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(Space8))
                        Column {
                            Text(
                                text = "System & Diagnostics",
                                style = CardTitleAmount,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.semantics { heading() }
                            )
                            Text(
                                text = "Network synchronization and debug tools",
                                style = MicroMetadata,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    if (com.example.BuildConfig.DEBUG) {
                        FinTrackButton(
                            onClick = onRunBnrDiagnostic,
                            modifier = Modifier
                                .fillMaxWidth()
                                .defaultMinSize(minHeight = 48.dp)
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
                                .defaultMinSize(minHeight = 48.dp)
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
            shape = ShapeExtraLarge,
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            textContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
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
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "• Converted successfully: ${pendingRetryResult.convertedSuccessfully}",
                        style = BodyRegular,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "• Still pending: ${pendingRetryResult.stillPending}",
                        style = BodyRegular,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "• Failed: ${pendingRetryResult.failedCount}",
                        style = BodyRegular,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "• Main failure reason: ${pendingRetryResult.mainFailureReason ?: "None"}",
                        style = BodyRegular,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
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
            shape = ShapeExtraLarge,
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            textContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
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
                    Text("• Reachable: ${debugDiagnosticResult.isReachable}", style = MicroMetadata, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("• HTTP Status: ${debugDiagnosticResult.httpStatus}", style = MicroMetadata, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("• Failure Category: ${debugDiagnosticResult.failureCategory}", style = MicroMetadata, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("• Publication Dates Parsed: ${debugDiagnosticResult.publicationDatesParsed}", style = MicroMetadata, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("• EUR Rate Found: ${debugDiagnosticResult.eurRateFound}", style = MicroMetadata, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("• Latest Publication Date: ${debugDiagnosticResult.latestPublicationDate ?: "N/A"}", style = MicroMetadata, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("• Requested URL: ${debugDiagnosticResult.requestedUrl}", style = MicroMetadata, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("• Final URL: ${debugDiagnosticResult.finalUrl}", style = MicroMetadata, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("• Content-Type: ${debugDiagnosticResult.contentType ?: "N/A"}", style = MicroMetadata, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("• Content-Encoding: ${debugDiagnosticResult.contentEncoding ?: "None"}", style = MicroMetadata, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("• Response Size: ${debugDiagnosticResult.responseByteCount} bytes", style = MicroMetadata, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("• Is HTML: ${debugDiagnosticResult.isHtml}", style = MicroMetadata, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("• XML Declaration: ${debugDiagnosticResult.hasXmlDeclaration}", style = MicroMetadata, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("• Root Element: ${debugDiagnosticResult.rootLocalName ?: "N/A"} (NS: ${debugDiagnosticResult.rootNamespaceUri ?: "None"})", style = MicroMetadata, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("• Element Counts: Cubes=${debugDiagnosticResult.cubeElementCount}, Rates=${debugDiagnosticResult.rateElementCount}, EUR=${debugDiagnosticResult.eurRateElementCount}", style = MicroMetadata, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("• Stages: A:${if (debugDiagnosticResult.stageA_httpConnection) "PASS" else "FAIL"} B:${if (debugDiagnosticResult.stageB_bodyObtained) "PASS" else "FAIL"} C:${if (debugDiagnosticResult.stageC_xmlOpened) "PASS" else "FAIL"} D:${if (debugDiagnosticResult.stageD_cubeFound) "PASS" else "FAIL"} E:${if (debugDiagnosticResult.stageE_rateFound) "PASS" else "FAIL"} F:${if (debugDiagnosticResult.stageF_eurFound) "PASS" else "FAIL"} G:${if (debugDiagnosticResult.stageG_validRatesProduced) "PASS" else "FAIL"}", style = MicroMetadata, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (!debugDiagnosticResult.sanitizedPreview.isNullOrBlank()) {
                        Text("• Preview: ${debugDiagnosticResult.sanitizedPreview}", style = MicroMetadata, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
            shape = ShapeExtraLarge,
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            textContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
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
                        FinTrackEmptyState(
                            title = "No Sync Errors",
                            description = "No sync errors currently recorded in this session.",
                            icon = Icons.Default.CheckCircle,
                            iconTint = FinTrackTheme.colors.income,
                            compact = true,
                            modifier = Modifier.fillMaxWidth().padding(vertical = Space16)
                        )
                    } else {
                        val record = diagnosticRecord!!
                        Text("• Timestamp: ${record.formattedTime}", style = MicroMetadata, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        Text("• Operation: ${record.operation}", style = MicroMetadata, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        Text("• Exception Code: ${record.exceptionCode ?: "N/A"}", style = MicroMetadata, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                        Text("• User UID: ${record.userUid ?: "None"}", style = MicroMetadata, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("• Household ID: ${record.householdId ?: "None"}", style = MicroMetadata, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("• Message: ${record.exceptionMessage ?: "None"}", style = MicroMetadata, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        if (!record.stackTraceSnippet.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(Space4))
                            Text("• Stack Trace Snippet:", style = MicroMetadata, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            Surface(
                                shape = RoundedCornerShape(RadiusSmall),
                                color = MaterialTheme.colorScheme.surfaceContainer,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = record.stackTraceSnippet,
                                    style = MicroMetadata,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(Space8)
                                )
                            }
                        }
                        if (copiedToast) {
                            Text(
                                text = "Copied to clipboard!",
                                color = MaterialTheme.colorScheme.primary,
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
