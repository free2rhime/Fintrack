package com.example.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.example.ui.MigrationConflictState
import com.example.ui.MigrationPreviewState
import com.example.ui.MigrationProgressState
import com.example.ui.MigrationResultState
import com.example.ui.theme.BodyRegular
import com.example.ui.theme.CobaltBlue
import com.example.ui.theme.ExpenseContainer
import com.example.ui.theme.ExpenseCoral
import com.example.ui.theme.IncomeEmerald
import com.example.ui.theme.LabelBadgeMedium
import com.example.ui.theme.MicroMetadata
import com.example.ui.theme.RadiusLarge
import com.example.ui.theme.RadiusMedium
import com.example.ui.theme.RadiusXLarge
import com.example.ui.theme.SectionHeadline
import com.example.ui.theme.Space12
import com.example.ui.theme.Space16
import com.example.ui.theme.Space4
import com.example.ui.theme.Space8
import com.example.ui.theme.SurfaceContainerDark
import com.example.ui.theme.SurfaceContainerHighDark
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Stage 3B / Stage 7 Step 4 - Migration Preview Dialog
 * Displays target household name & ID, verified role, record counts, verified backup metadata,
 * explicit acknowledgment checkbox, and exposes Proceed (enabled upon check) and Cancel actions.
 */
@Composable
fun MigrationPreviewDialog(
    previewState: MigrationPreviewState,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isAcknowledged by remember { mutableStateOf(false) }

    val formattedBackupTime = remember(previewState.backupTimestamp) {
        val ts = previewState.backupTimestamp
        if (ts != null && ts > 0) {
            SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(ts))
        } else {
            "Verified on device"
        }
    }

    AlertDialog(
        onDismissRequest = onCancel,
        modifier = modifier.testTag("migration_preview_dialog"),
        containerColor = SurfaceDark,
        shape = RoundedCornerShape(RadiusXLarge),
        titleContentColor = TextPrimary,
        textContentColor = TextSecondary,
        icon = {
            Icon(
                imageVector = Icons.Default.CloudUpload,
                contentDescription = "Cloud Migration Preview",
                tint = CobaltBlue,
                modifier = Modifier.size(32.dp)
            )
        },
        title = {
            Text(
                text = "Migration Preview",
                style = SectionHeadline,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(Space12)
            ) {
                Text(
                    text = "Review the local records that will be uploaded to your household cloud repository:",
                    style = BodyRegular,
                    color = TextSecondary
                )

                // Household & Role Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = SurfaceContainerDark
                    ),
                    shape = RoundedCornerShape(RadiusLarge)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(Space12),
                        verticalArrangement = Arrangement.spacedBy(Space8)
                    ) {
                        // Household Name (Prominently displayed)
                        val displayName = previewState.householdName?.takeIf { it.isNotBlank() } ?: previewState.householdId
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Household Name:",
                                style = LabelBadgeMedium,
                                color = TextSecondary
                            )
                            Text(
                                text = displayName,
                                style = BodyRegular,
                                fontWeight = FontWeight.Bold,
                                color = CobaltBlue,
                                modifier = Modifier.testTag("preview_household_name")
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Household ID:",
                                style = LabelBadgeMedium,
                                color = TextSecondary
                            )
                            Text(
                                text = previewState.householdId,
                                style = BodyRegular,
                                fontWeight = FontWeight.Normal,
                                color = TextSecondary,
                                modifier = Modifier.testTag("preview_household_id")
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Verified Role:",
                                style = LabelBadgeMedium,
                                color = TextSecondary
                            )
                            Text(
                                text = previewState.userRole,
                                style = BodyRegular,
                                fontWeight = FontWeight.SemiBold,
                                color = CobaltBlue,
                                modifier = Modifier.testTag("preview_user_role")
                            )
                        }
                    }
                }

                // Record Counts Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = SurfaceContainerDark
                    ),
                    shape = RoundedCornerShape(RadiusLarge)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(Space12),
                        verticalArrangement = Arrangement.spacedBy(Space8)
                    ) {
                        Text(
                            text = "Records to Migrate",
                            style = LabelBadgeMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        RecordCountRow(label = "Transactions", count = previewState.transactionsCount)
                        RecordCountRow(label = "Categories", count = previewState.categoriesCount)
                        RecordCountRow(label = "Exchange Rates", count = previewState.exchangeRatesCount)

                        Spacer(modifier = Modifier.height(Space4))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Total Records:",
                                style = BodyRegular,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                text = "${previewState.totalRecords}",
                                style = BodyRegular,
                                fontWeight = FontWeight.Bold,
                                color = CobaltBlue
                            )
                        }
                    }
                }

                // Backup Status Banner
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = SurfaceContainerDark
                    ),
                    shape = RoundedCornerShape(RadiusLarge)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(Space12),
                        verticalArrangement = Arrangement.spacedBy(Space4)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Backup Validated",
                                tint = IncomeEmerald,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(Space8))
                            Text(
                                text = "Preflight Backup Validated",
                                style = LabelBadgeMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        }

                        Text(
                            text = "Created: $formattedBackupTime",
                            style = BodyRegular,
                            color = TextSecondary,
                            modifier = Modifier.testTag("preview_backup_timestamp")
                        )

                        if (!previewState.backupBundlePath.isNullOrBlank()) {
                            Text(
                                text = "Path: ${previewState.backupBundlePath}",
                                style = MicroMetadata,
                                color = TextMuted,
                                modifier = Modifier.testTag("preview_backup_path")
                            )
                        }
                    }
                }

                // Explicit Confirmation Checkbox (Safety acknowledgment)
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isAcknowledged = !isAcknowledged }
                        .testTag("migration_acknowledgment_card"),
                    colors = CardDefaults.cardColors(
                        containerColor = SurfaceContainerDark
                    ),
                    shape = RoundedCornerShape(RadiusLarge)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(Space12),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = isAcknowledged,
                            onCheckedChange = { isAcknowledged = it },
                            modifier = Modifier.testTag("migration_acknowledgment_checkbox"),
                            colors = CheckboxDefaults.colors(
                                checkedColor = CobaltBlue,
                                uncheckedColor = SurfaceContainerHighDark,
                                checkmarkColor = Color.White
                            )
                        )
                        Spacer(modifier = Modifier.width(Space8))
                        Text(
                            text = "I understand local FinTrack data will be uploaded to this shared household.",
                            style = BodyRegular,
                            fontWeight = FontWeight.Medium,
                            color = TextPrimary
                        )
                    }
                }
            }
        },
        confirmButton = {
            FinTrackButton(
                onClick = onConfirm,
                enabled = isAcknowledged,
                modifier = Modifier.testTag("migration_preview_confirm_button"),
                variant = ButtonVariant.PRIMARY
            ) {
                Text("Proceed with Migration", style = LabelBadgeMedium, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            FinTrackButton(
                text = "Cancel",
                onClick = onCancel,
                modifier = Modifier.testTag("migration_preview_cancel_button"),
                variant = ButtonVariant.SECONDARY
            )
        }
    )
}

@Composable
private fun RecordCountRow(label: String, count: Int) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = label,
            style = BodyRegular,
            color = TextSecondary
        )
        Text(
            text = "$count",
            style = BodyRegular,
            fontWeight = FontWeight.Medium,
            color = TextPrimary
        )
    }
}

/**
 * Stage 3B - Migration Conflict Dialog
 * Displays sanitized conflict reason and details, and exposes a single dismiss action.
 */
@Composable
fun MigrationConflictDialog(
    conflictState: MigrationConflictState,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier.testTag("migration_conflict_dialog"),
        containerColor = SurfaceDark,
        shape = RoundedCornerShape(RadiusXLarge),
        titleContentColor = TextPrimary,
        textContentColor = TextSecondary,
        icon = {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = "Migration Conflict",
                tint = ExpenseCoral,
                modifier = Modifier.size(32.dp)
            )
        },
        title = {
            Text(
                text = "Migration Blocked",
                style = SectionHeadline,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(Space12)
            ) {
                Text(
                    text = "A preflight validation check prevented migration from proceeding:",
                    style = BodyRegular,
                    color = TextSecondary
                )

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = ExpenseContainer
                    ),
                    shape = RoundedCornerShape(RadiusLarge)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(Space12),
                        verticalArrangement = Arrangement.spacedBy(Space8)
                    ) {
                        Text(
                            text = conflictState.reason,
                            style = LabelBadgeMedium,
                            fontWeight = FontWeight.Bold,
                            color = ExpenseCoral
                        )
                        Text(
                            text = conflictState.details,
                            style = BodyRegular,
                            color = TextPrimary
                        )
                    }
                }
            }
        },
        confirmButton = {
            FinTrackButton(
                text = "Dismiss",
                onClick = onDismiss,
                modifier = Modifier.testTag("migration_conflict_dismiss_button"),
                variant = ButtonVariant.PRIMARY
            )
        }
    )
}

/**
 * Stage 3B - Migration Progress Dialog
 * Non-cancellable dialog displaying current migration stage, determinate progress bar,
 * and a clear listener suppression notice.
 */
@Composable
fun MigrationProgressDialog(
    progressState: MigrationProgressState,
    modifier: Modifier = Modifier
) {
    val progressFraction = if (progressState.totalCount > 0) {
        (progressState.processedCount.toFloat() / progressState.totalCount.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }

    AlertDialog(
        onDismissRequest = { /* Non-cancellable */ },
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false
        ),
        modifier = modifier.testTag("migration_progress_dialog"),
        containerColor = SurfaceDark,
        shape = RoundedCornerShape(RadiusXLarge),
        titleContentColor = TextPrimary,
        textContentColor = TextSecondary,
        icon = {
            Icon(
                imageVector = Icons.Default.Sync,
                contentDescription = "Migration In Progress",
                tint = CobaltBlue,
                modifier = Modifier.size(32.dp)
            )
        },
        title = {
            Text(
                text = "Migrating to Household Cloud",
                style = SectionHeadline,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(Space16),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Stage: ${progressState.stage}",
                    style = LabelBadgeMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = CobaltBlue
                )

                if (progressState.totalCount > 0) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(Space4)
                    ) {
                        LinearProgressIndicator(
                            progress = { progressFraction },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp),
                            color = IncomeEmerald,
                            trackColor = SurfaceContainerHighDark
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "${(progressFraction * 100).toInt()}%",
                                style = MicroMetadata,
                                color = TextSecondary
                            )
                            Text(
                                text = "${progressState.processedCount} / ${progressState.totalCount} records",
                                style = MicroMetadata,
                                color = TextSecondary
                            )
                        }
                    }
                } else {
                    CircularProgressIndicator(
                        modifier = Modifier.size(40.dp),
                        strokeWidth = 3.dp,
                        color = CobaltBlue
                    )
                }

                // Listener Suppression Notice
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = SurfaceContainerDark
                    ),
                    shape = RoundedCornerShape(RadiusMedium)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(Space12),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Listener Suppressed",
                            tint = TextMuted,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(Space8))
                        Text(
                            text = "Real-time sync listeners are suppressed during migration to prevent write amplification.",
                            style = MicroMetadata,
                            color = TextSecondary
                        )
                    }
                }
            }
        },
        confirmButton = { /* No actions during active upload */ }
    )
}

/**
 * Stage 3B - Migration Result Dialog
 * Shows success or failure summary, migration ID, uploaded record counts,
 * backup location when relevant, and exposes a dismiss action.
 */
@Composable
fun MigrationResultDialog(
    resultState: MigrationResultState,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    when (resultState) {
        is MigrationResultState.Success -> {
            AlertDialog(
                onDismissRequest = onDismiss,
                modifier = modifier.testTag("migration_result_dialog"),
                containerColor = SurfaceDark,
                shape = RoundedCornerShape(RadiusXLarge),
                titleContentColor = TextPrimary,
                textContentColor = TextSecondary,
                icon = {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Migration Succeeded",
                        tint = IncomeEmerald,
                        modifier = Modifier.size(36.dp)
                    )
                },
                title = {
                    Text(
                        text = "Migration Completed",
                        style = SectionHeadline,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(Space12)
                    ) {
                        Text(
                            text = "Your local records have been securely migrated and synced to the household cloud.",
                            style = BodyRegular,
                            color = TextSecondary
                        )

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = SurfaceContainerDark
                            ),
                            shape = RoundedCornerShape(RadiusLarge)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(Space12),
                                verticalArrangement = Arrangement.spacedBy(Space8)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Migration ID:",
                                        style = LabelBadgeMedium,
                                        color = TextSecondary
                                    )
                                    Text(
                                        text = resultState.migrationId.take(16),
                                        style = BodyRegular,
                                        fontWeight = FontWeight.SemiBold,
                                        color = TextPrimary
                                    )
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Transactions Uploaded:",
                                        style = LabelBadgeMedium,
                                        color = TextSecondary
                                    )
                                    Text(
                                        text = "${resultState.transactionsUploaded}",
                                        style = BodyRegular,
                                        fontWeight = FontWeight.Bold,
                                        color = IncomeEmerald
                                    )
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Categories Uploaded:",
                                        style = LabelBadgeMedium,
                                        color = TextSecondary
                                    )
                                    Text(
                                        text = "${resultState.categoriesUploaded}",
                                        style = BodyRegular,
                                        fontWeight = FontWeight.Medium,
                                        color = TextPrimary
                                    )
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Exchange Rates Uploaded:",
                                        style = LabelBadgeMedium,
                                        color = TextSecondary
                                    )
                                    Text(
                                        text = "${resultState.ratesUploaded}",
                                        style = BodyRegular,
                                        fontWeight = FontWeight.Medium,
                                        color = TextPrimary
                                    )
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Total Processed:",
                                        style = LabelBadgeMedium,
                                        color = TextSecondary
                                    )
                                    Text(
                                        text = "${resultState.totalProcessed}",
                                        style = BodyRegular,
                                        fontWeight = FontWeight.Bold,
                                        color = CobaltBlue
                                    )
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    FinTrackButton(
                        text = "Done",
                        onClick = onDismiss,
                        modifier = Modifier.testTag("migration_result_dismiss_button"),
                        variant = ButtonVariant.PRIMARY
                    )
                }
            )
        }
        is MigrationResultState.Failure -> {
            AlertDialog(
                onDismissRequest = onDismiss,
                modifier = modifier.testTag("migration_result_dialog"),
                containerColor = SurfaceDark,
                shape = RoundedCornerShape(RadiusXLarge),
                titleContentColor = TextPrimary,
                textContentColor = TextSecondary,
                icon = {
                    Icon(
                        imageVector = Icons.Default.Error,
                        contentDescription = "Migration Failed",
                        tint = ExpenseCoral,
                        modifier = Modifier.size(36.dp)
                    )
                },
                title = {
                    Text(
                        text = "Migration Failed",
                        style = SectionHeadline,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(Space12)
                    ) {
                        Text(
                            text = "An error occurred during the migration process. Local data remains safe.",
                            style = BodyRegular,
                            color = TextSecondary
                        )

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = ExpenseContainer
                            ),
                            shape = RoundedCornerShape(RadiusLarge)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(Space12),
                                verticalArrangement = Arrangement.spacedBy(Space8)
                            ) {
                                Text(
                                    text = "Failed Stage: ${resultState.stage}",
                                    style = LabelBadgeMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = ExpenseCoral
                                )
                                Text(
                                    text = resultState.sanitizedError,
                                    style = BodyRegular,
                                    color = TextPrimary
                                )
                            }
                        }

                        if (!resultState.backupBundlePath.isNullOrBlank()) {
                            Text(
                                text = "Safety Backup Bundle: ${resultState.backupBundlePath}",
                                style = MicroMetadata,
                                color = TextMuted
                            )
                        }
                    }
                },
                confirmButton = {
                    FinTrackButton(
                        text = "Dismiss",
                        onClick = onDismiss,
                        modifier = Modifier.testTag("migration_result_dismiss_button"),
                        variant = ButtonVariant.PRIMARY
                    )
                }
            )
        }
    }
}
