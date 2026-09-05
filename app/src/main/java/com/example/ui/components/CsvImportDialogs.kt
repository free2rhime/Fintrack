package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
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
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import com.example.data.util.CsvDuplicateMode
import com.example.data.util.CsvImportFinalResult
import com.example.data.util.CsvPreviewData
import com.example.ui.theme.BodyRegular
import com.example.ui.theme.CardTitleAmount
import com.example.ui.theme.CobaltBlue
import com.example.ui.theme.ExpenseContainer
import com.example.ui.theme.ExpenseCoral
import com.example.ui.theme.IncomeContainer
import com.example.ui.theme.IncomeEmerald
import com.example.ui.theme.LabelBadgeMedium
import com.example.ui.theme.MicroMetadata
import com.example.ui.theme.RadiusLarge
import com.example.ui.theme.RadiusMedium
import com.example.ui.theme.RadiusSmall
import com.example.ui.theme.RadiusXLarge
import com.example.ui.theme.SectionHeadline
import com.example.ui.theme.Space12
import com.example.ui.theme.Space16
import com.example.ui.theme.Space2
import com.example.ui.theme.Space20
import com.example.ui.theme.Space4
import com.example.ui.theme.Space8
import com.example.ui.theme.SurfaceContainerDark
import com.example.ui.theme.SurfaceContainerHighDark
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WarningAmber
import java.util.Locale

@Composable
fun CsvImportPreviewDialog(
    previewData: CsvPreviewData,
    onDuplicateModeChanged: (CsvDuplicateMode) -> Unit,
    onConfirmImport: () -> Unit,
    onDismiss: () -> Unit
) {
    var showUpdateConfirmation by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier
            .fillMaxWidth(0.95f)
            .widthIn(max = 560.dp)
            .padding(Space16),
        containerColor = SurfaceDark,
        shape = RoundedCornerShape(RadiusXLarge),
        titleContentColor = TextPrimary,
        textContentColor = TextSecondary,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = CobaltBlue,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(Space8))
                Text(
                    text = "CSV Import Preview",
                    style = SectionHeadline,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 480.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // METRICS SUMMARY GRID
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Space8)
                ) {
                    MetricCard(
                        title = "Total Rows",
                        value = "${previewData.totalRows}",
                        modifier = Modifier.weight(1f)
                    )
                    MetricCard(
                        title = "Valid Rows",
                        value = "${previewData.validRowsCount}",
                        textColor = IncomeEmerald,
                        modifier = Modifier.weight(1f)
                    )
                    MetricCard(
                        title = "Invalid Rows",
                        value = "${previewData.invalidRowsCount}",
                        textColor = if (previewData.invalidRowsCount > 0) ExpenseCoral else TextPrimary,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(Space8))

                // RON TOTALS
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Space8)
                ) {
                    MetricCard(
                        title = "Total Income (RON)",
                        value = String.format(Locale.US, "%.2f RON", previewData.totalRonIncome),
                        textColor = IncomeEmerald,
                        modifier = Modifier.weight(1f)
                    )
                    MetricCard(
                        title = "Total Expense (RON)",
                        value = String.format(Locale.US, "%.2f RON", previewData.totalRonExpense),
                        textColor = ExpenseCoral,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(Space16))

                // DUPLICATE HANDLING MODE SELECTOR
                Text(
                    text = "Duplicate Transaction Handling:",
                    style = LabelBadgeMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(Space8))

                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        selected = previewData.duplicateMode == CsvDuplicateMode.SKIP_EXISTING,
                        onClick = { onDuplicateModeChanged(CsvDuplicateMode.SKIP_EXISTING) },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                        modifier = Modifier.testTag("segmented_skip_existing"),
                        colors = SegmentedButtonDefaults.colors(
                            activeContainerColor = CobaltBlue,
                            activeContentColor = Color.White,
                            inactiveContainerColor = SurfaceContainerDark,
                            inactiveContentColor = TextSecondary,
                            activeBorderColor = CobaltBlue,
                            inactiveBorderColor = SurfaceContainerHighDark
                        )
                    ) {
                        Text("Skip Existing", style = LabelBadgeMedium)
                    }
                    SegmentedButton(
                        selected = previewData.duplicateMode == CsvDuplicateMode.UPDATE_EXISTING,
                        onClick = { onDuplicateModeChanged(CsvDuplicateMode.UPDATE_EXISTING) },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                        modifier = Modifier.testTag("segmented_update_existing"),
                        colors = SegmentedButtonDefaults.colors(
                            activeContainerColor = CobaltBlue,
                            activeContentColor = Color.White,
                            inactiveContainerColor = SurfaceContainerDark,
                            inactiveContentColor = TextSecondary,
                            activeBorderColor = CobaltBlue,
                            inactiveBorderColor = SurfaceContainerHighDark
                        )
                    ) {
                        Text("Update Existing", style = LabelBadgeMedium)
                    }
                }

                Spacer(modifier = Modifier.height(Space8))

                Surface(
                    shape = RoundedCornerShape(RadiusMedium),
                    color = SurfaceContainerDark,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(Space12)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "New Records to Insert:",
                                style = BodyRegular,
                                color = TextSecondary
                            )
                            Text(
                                text = "${previewData.newIdsCount}",
                                style = BodyRegular,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        }
                        Spacer(modifier = Modifier.height(Space4))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = if (previewData.duplicateMode == CsvDuplicateMode.SKIP_EXISTING)
                                    "Existing Records (To Skip):" else "Existing Records (To Update):",
                                style = BodyRegular,
                                color = TextSecondary
                            )
                            Text(
                                text = "${previewData.existingIdsCount}",
                                style = BodyRegular,
                                fontWeight = FontWeight.Bold,
                                color = if (previewData.duplicateMode == CsvDuplicateMode.UPDATE_EXISTING && previewData.existingIdsCount > 0)
                                    CobaltBlue else TextSecondary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(Space16))

                // CONVERSION STATUS BREAKDOWN
                Text(
                    text = "Exchange Rate Conversion Breakdown:",
                    style = LabelBadgeMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(Space8))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Space8)
                ) {
                    StatusChip("OFFICIAL: ${previewData.officialCount}", IncomeEmerald, Modifier.weight(1f))
                    StatusChip("UNVERIFIED: ${previewData.unverifiedCount}", WarningAmber, Modifier.weight(1f))
                    StatusChip("PENDING: ${previewData.pendingCount}", ExpenseCoral, Modifier.weight(1f))
                }

                // MISSING CATEGORIES SECTION
                if (previewData.missingCategories.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(Space16))
                    Text(
                        text = "New Categories / Subcategories to be Created (${previewData.missingCategories.size}):",
                        style = LabelBadgeMedium,
                        fontWeight = FontWeight.Bold,
                        color = CobaltBlue
                    )
                    Spacer(modifier = Modifier.height(Space8))
                    Card(
                        colors = CardDefaults.cardColors(containerColor = SurfaceContainerDark),
                        shape = RoundedCornerShape(RadiusMedium),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(Space12)) {
                            previewData.missingCategories.forEach { item ->
                                Text(
                                    text = "• [${item.type}] ${item.name} → ${item.subCategory}",
                                    style = BodyRegular,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextPrimary
                                )
                                Spacer(modifier = Modifier.height(Space2))
                            }
                        }
                    }
                }

                // ROW ERRORS SECTION
                if (previewData.rowErrors.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(Space16))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Warning, contentDescription = null, tint = ExpenseCoral, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(Space8))
                        Text(
                            text = "Row Validation Errors / Exclusions (${previewData.rowErrors.size}):",
                            style = LabelBadgeMedium,
                            fontWeight = FontWeight.Bold,
                            color = ExpenseCoral
                        )
                    }
                    Spacer(modifier = Modifier.height(Space8))
                    Card(
                        colors = CardDefaults.cardColors(containerColor = ExpenseContainer),
                        shape = RoundedCornerShape(RadiusMedium),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(Space12)) {
                            previewData.rowErrors.forEach { err ->
                                Text(
                                    text = "Row ${err.rowNumber} [${err.field}]: ${err.message}",
                                    style = BodyRegular,
                                    color = ExpenseCoral
                                )
                                Spacer(modifier = Modifier.height(Space4))
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            FinTrackButton(
                onClick = {
                    if (previewData.duplicateMode == CsvDuplicateMode.UPDATE_EXISTING && previewData.existingIdsCount > 0) {
                        showUpdateConfirmation = true
                    } else {
                        onConfirmImport()
                    }
                },
                enabled = previewData.validRowsCount > 0,
                modifier = Modifier.testTag("confirm_import_button"),
                variant = ButtonVariant.PRIMARY
            ) {
                Text("Confirm & Import (${previewData.validRowsCount})", style = LabelBadgeMedium, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            FinTrackButton(
                text = "Cancel",
                onClick = onDismiss,
                variant = ButtonVariant.SECONDARY
            )
        }
    )

    // SECONDARY CONFIRMATION FOR UPDATE EXISTING MODE
    if (showUpdateConfirmation) {
        AlertDialog(
            onDismissRequest = { showUpdateConfirmation = false },
            containerColor = SurfaceDark,
            shape = RoundedCornerShape(RadiusXLarge),
            titleContentColor = TextPrimary,
            textContentColor = TextSecondary,
            title = {
                Text("Confirm Overwrite", style = SectionHeadline, fontWeight = FontWeight.Bold, color = TextPrimary)
            },
            text = {
                Text(
                    text = "You have selected 'Update Existing' mode. Importing will overwrite data for ${previewData.existingIdsCount} matching transaction record(s) currently in your database.\n\nA backup will be created and validated before writing. Are you sure you want to proceed?",
                    style = BodyRegular,
                    color = TextSecondary
                )
            },
            confirmButton = {
                FinTrackButton(
                    text = "Proceed with Update",
                    onClick = {
                        showUpdateConfirmation = false
                        onConfirmImport()
                    },
                    variant = ButtonVariant.DESTRUCTIVE
                )
            },
            dismissButton = {
                FinTrackButton(
                    text = "Cancel",
                    onClick = { showUpdateConfirmation = false },
                    variant = ButtonVariant.SECONDARY
                )
            }
        )
    }
}

@Composable
fun CsvImportResultDialog(
    result: CsvImportFinalResult,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceDark,
        shape = RoundedCornerShape(RadiusXLarge),
        titleContentColor = TextPrimary,
        textContentColor = TextSecondary,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (result.success) {
                    Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = IncomeEmerald, modifier = Modifier.size(24.dp))
                } else {
                    Icon(imageVector = Icons.Default.ErrorOutline, contentDescription = null, tint = ExpenseCoral, modifier = Modifier.size(24.dp))
                }
                Spacer(modifier = Modifier.width(Space8))
                Text(
                    text = if (result.success) "Import Successful" else "Import Failed",
                    style = SectionHeadline,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                if (!result.errorMessage.isNullOrBlank()) {
                    Text(
                        text = result.errorMessage,
                        style = BodyRegular,
                        color = ExpenseCoral,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(Space12))
                }

                if (result.success) {
                    Text(
                        text = "Transactions have been processed and saved into local storage.",
                        style = BodyRegular,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(Space12))

                    // RESULT BREAKDOWN CARDS
                    Card(
                        colors = CardDefaults.cardColors(containerColor = SurfaceContainerDark),
                        shape = RoundedCornerShape(RadiusLarge),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(Space16)) {
                            ResultRow("Transactions Inserted:", "${result.insertedCount}", IncomeEmerald)
                            ResultRow("Transactions Updated:", "${result.updatedCount}", CobaltBlue)
                            ResultRow("Transactions Skipped:", "${result.skippedCount}", TextSecondary)
                            ResultRow("Rows Failed / Excluded:", "${result.failedCount}", if (result.failedCount > 0) ExpenseCoral else TextSecondary)
                            Spacer(modifier = Modifier.height(Space8))
                            ResultRow("Categories Created:", "${result.categoriesCreatedCount}", TextPrimary)
                            ResultRow("Subcategories Created:", "${result.subcategoriesCreatedCount}", TextPrimary)
                            Spacer(modifier = Modifier.height(Space8))
                            ResultRow("Pending Conversions:", "${result.pendingCount}", ExpenseCoral)
                            ResultRow("Unverified Conversions:", "${result.unverifiedCount}", WarningAmber)
                        }
                    }

                    if (!result.backupFilePath.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(Space12))
                        Text(
                            text = "Backup Verified & Saved At:\n${result.backupFilePath}",
                            style = MicroMetadata,
                            color = TextMuted
                        )
                    }
                }
            }
        },
        confirmButton = {
            FinTrackButton(
                text = "Close",
                onClick = onDismiss,
                modifier = Modifier.testTag("close_import_result_button"),
                variant = ButtonVariant.PRIMARY
            )
        }
    )
}

@Composable
private fun MetricCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier,
    textColor: Color = TextPrimary
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(RadiusMedium),
        colors = CardDefaults.cardColors(containerColor = SurfaceContainerDark)
    ) {
        Column(modifier = Modifier.padding(Space8)) {
            Text(title, style = MicroMetadata, color = TextSecondary)
            Spacer(modifier = Modifier.height(Space4))
            Text(value, style = CardTitleAmount, fontWeight = FontWeight.Bold, color = textColor)
        }
    }
}

@Composable
private fun StatusChip(
    text: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(RadiusSmall),
        color = color.copy(alpha = 0.15f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.4f))
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(vertical = Space4, horizontal = Space4)) {
            Text(text, style = MicroMetadata, fontWeight = FontWeight.Bold, color = color)
        }
    }
}

@Composable
private fun ResultRow(
    label: String,
    value: String,
    valueColor: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = Space4),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = BodyRegular, color = TextSecondary)
        Text(value, style = BodyRegular, fontWeight = FontWeight.Bold, color = valueColor)
    }
}
