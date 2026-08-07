package com.example.ui.components

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.example.data.util.CsvDuplicateMode
import com.example.data.util.CsvImportFinalResult
import com.example.data.util.CsvPreviewData
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
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
            .padding(16.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("CSV Import Preview", fontWeight = FontWeight.Bold)
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
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MetricCard(
                        title = "Total Rows",
                        value = "${previewData.totalRows}",
                        modifier = Modifier.weight(1f)
                    )
                    MetricCard(
                        title = "Valid Rows",
                        value = "${previewData.validRowsCount}",
                        textColor = IncomeGreen,
                        modifier = Modifier.weight(1f)
                    )
                    MetricCard(
                        title = "Invalid Rows",
                        value = "${previewData.invalidRowsCount}",
                        textColor = if (previewData.invalidRowsCount > 0) ExpenseRed else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // RON TOTALS
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MetricCard(
                        title = "Total Income (RON)",
                        value = String.format(Locale.US, "%.2f RON", previewData.totalRonIncome),
                        textColor = IncomeGreen,
                        modifier = Modifier.weight(1f)
                    )
                    MetricCard(
                        title = "Total Expense (RON)",
                        value = String.format(Locale.US, "%.2f RON", previewData.totalRonExpense),
                        textColor = ExpenseRed,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // DUPLICATE HANDLING MODE SELECTOR
                Text(
                    text = "Duplicate Transaction Handling:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))

                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        selected = previewData.duplicateMode == CsvDuplicateMode.SKIP_EXISTING,
                        onClick = { onDuplicateModeChanged(CsvDuplicateMode.SKIP_EXISTING) },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                        modifier = Modifier.testTag("segmented_skip_existing")
                    ) {
                        Text("Skip Existing")
                    }
                    SegmentedButton(
                        selected = previewData.duplicateMode == CsvDuplicateMode.UPDATE_EXISTING,
                        onClick = { onDuplicateModeChanged(CsvDuplicateMode.UPDATE_EXISTING) },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                        modifier = Modifier.testTag("segmented_update_existing")
                    ) {
                        Text("Update Existing")
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "New Records to Insert:",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                text = "${previewData.newIdsCount}",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = if (previewData.duplicateMode == CsvDuplicateMode.SKIP_EXISTING)
                                    "Existing Records (To Skip):" else "Existing Records (To Update):",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                text = "${previewData.existingIdsCount}",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = if (previewData.duplicateMode == CsvDuplicateMode.UPDATE_EXISTING && previewData.existingIdsCount > 0)
                                    MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // CONVERSION STATUS BREAKDOWN
                Text(
                    text = "Exchange Rate Conversion Breakdown:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    StatusChip("OFFICIAL: ${previewData.officialCount}", IncomeGreen, Modifier.weight(1f))
                    StatusChip("UNVERIFIED: ${previewData.unverifiedCount}", MaterialTheme.colorScheme.tertiary, Modifier.weight(1f))
                    StatusChip("PENDING: ${previewData.pendingCount}", MaterialTheme.colorScheme.error, Modifier.weight(1f))
                }

                // MISSING CATEGORIES SECTION
                if (previewData.missingCategories.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "New Categories / Subcategories to be Created (${previewData.missingCategories.size}):",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            previewData.missingCategories.forEach { item ->
                                Text(
                                    text = "• [${item.type}] ${item.name} → ${item.subCategory}",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }

                // ROW ERRORS SECTION
                if (previewData.rowErrors.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Warning, contentDescription = null, tint = ExpenseRed)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Row Validation Errors / Exclusions (${previewData.rowErrors.size}):",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = ExpenseRed
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Card(
                        colors = CardDefaults.cardColors(containerColor = ExpenseRed.copy(alpha = 0.1f)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            previewData.rowErrors.forEach { err ->
                                Text(
                                    text = "Row ${err.rowNumber} [${err.field}]: ${err.message}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = ExpenseRed
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (previewData.duplicateMode == CsvDuplicateMode.UPDATE_EXISTING && previewData.existingIdsCount > 0) {
                        showUpdateConfirmation = true
                    } else {
                        onConfirmImport()
                    }
                },
                enabled = previewData.validRowsCount > 0,
                modifier = Modifier.testTag("confirm_import_button")
            ) {
                Text("Confirm & Import (${previewData.validRowsCount})")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )

    // SECONDARY CONFIRMATION FOR UPDATE EXISTING MODE
    if (showUpdateConfirmation) {
        AlertDialog(
            onDismissRequest = { showUpdateConfirmation = false },
            title = { Text("Confirm Overwrite") },
            text = {
                Text(
                    "You have selected 'Update Existing' mode. Importing will overwrite data for ${previewData.existingIdsCount} matching transaction record(s) currently in your database.\n\nA backup will be created and validated before writing. Are you sure you want to proceed?"
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showUpdateConfirmation = false
                        onConfirmImport()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Proceed with Update")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showUpdateConfirmation = false }) {
                    Text("Cancel")
                }
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
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (result.success) {
                    Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = IncomeGreen)
                } else {
                    Icon(imageVector = Icons.Default.ErrorOutline, contentDescription = null, tint = ExpenseRed)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (result.success) "Import Successful" else "Import Failed",
                    fontWeight = FontWeight.Bold
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
                        style = MaterialTheme.typography.bodyMedium,
                        color = ExpenseRed,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }

                if (result.success) {
                    Text(
                        text = "Transactions have been processed and saved into local storage.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    // RESULT BREAKDOWN CARDS
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            ResultRow("Transactions Inserted:", "${result.insertedCount}", IncomeGreen)
                            ResultRow("Transactions Updated:", "${result.updatedCount}", MaterialTheme.colorScheme.primary)
                            ResultRow("Transactions Skipped:", "${result.skippedCount}", MaterialTheme.colorScheme.onSurfaceVariant)
                            ResultRow("Rows Failed / Excluded:", "${result.failedCount}", if (result.failedCount > 0) ExpenseRed else MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(8.dp))
                            ResultRow("Categories Created:", "${result.categoriesCreatedCount}", MaterialTheme.colorScheme.secondary)
                            ResultRow("Subcategories Created:", "${result.subcategoriesCreatedCount}", MaterialTheme.colorScheme.secondary)
                            Spacer(modifier = Modifier.height(8.dp))
                            ResultRow("Pending Conversions:", "${result.pendingCount}", MaterialTheme.colorScheme.error)
                            ResultRow("Unverified Conversions:", "${result.unverifiedCount}", MaterialTheme.colorScheme.tertiary)
                        }
                    }

                    if (!result.backupFilePath.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Backup Verified & Saved At:\n${result.backupFilePath}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                modifier = Modifier.testTag("close_import_result_button")
            ) {
                Text("Close")
            }
        }
    )
}

@Composable
private fun MetricCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier,
    textColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Text(title, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(2.dp))
            Text(value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = textColor)
        }
    }
}

@Composable
private fun StatusChip(
    text: String,
    color: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(6.dp),
        color = color.copy(alpha = 0.15f),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.4f))
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(vertical = 6.dp, horizontal = 4.dp)) {
            Text(text, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = color)
        }
    }
}

@Composable
private fun ResultRow(
    label: String,
    value: String,
    valueColor: androidx.compose.ui.graphics.Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall)
        Text(value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = valueColor)
    }
}
