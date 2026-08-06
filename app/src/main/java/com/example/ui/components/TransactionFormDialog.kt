package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.data.model.CategoryEntity
import com.example.data.model.TransactionEntity
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionFormDialog(
    initialTransaction: TransactionEntity?,
    isDuplicateMode: Boolean,
    categories: List<CategoryEntity>,
    onDismiss: () -> Unit,
    onSave: (
        id: String?,
        date: String,
        description: String,
        amountRON: Double,
        type: String,
        account: String,
        category: String,
        subCategory: String,
        destination: String?
    ) -> Unit
) {
    val todayStr = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()) }

    var type by remember { mutableStateOf(initialTransaction?.type ?: "Expense") }
    var amountText by remember {
        mutableStateOf(if (initialTransaction != null) initialTransaction.amountRON.toString() else "")
    }
    var description by remember { mutableStateOf(initialTransaction?.description ?: "") }
    var date by remember {
        mutableStateOf(
            if (isDuplicateMode) todayStr else (initialTransaction?.date ?: todayStr)
        )
    }
    var account by remember { mutableStateOf(initialTransaction?.account ?: "Card") }
    var subCategory by remember { mutableStateOf(initialTransaction?.subCategory ?: "") }
    var category by remember {
        mutableStateOf(
            initialTransaction?.category ?: if (type == "Income") "💼 Salary" else "🍉 Food & Dining"
        )
    }
    var destination by remember { mutableStateOf(initialTransaction?.destination ?: "") }

    var amountError by remember { mutableStateOf(false) }
    var descError by remember { mutableStateOf(false) }

    // Filter subcategories by type
    val availableCategoryItems = remember(type, categories) {
        categories.filter { it.type == type }
    }
    val availableSubcategories = remember(availableCategoryItems) {
        availableCategoryItems.map { it.subCategory }.filter { it.isNotBlank() }.distinct()
    }

    var subCategoryExpanded by remember { mutableStateOf(false) }
    var accountExpanded by remember { mutableStateOf(false) }

    val accounts = listOf("Card", "Cash", "Meal Tickets")

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = when {
                                isDuplicateMode -> "Duplicate Transaction"
                                initialTransaction != null -> "Edit Transaction"
                                else -> "Add Transaction"
                            },
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        if (isDuplicateMode) {
                            Text(
                                text = "Date auto-updated to today ($todayStr)",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Type Toggle (Income / Expense)
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        selected = type == "Expense",
                        onClick = {
                            type = "Expense"
                            subCategory = ""
                            val firstMatch = categories.find { it.type == "Expense" }
                            if (firstMatch != null) category = firstMatch.name
                        },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                    ) {
                        Text("Expense", color = if (type == "Expense") ExpenseRed else MaterialTheme.colorScheme.onSurface)
                    }

                    SegmentedButton(
                        selected = type == "Income",
                        onClick = {
                            type = "Income"
                            subCategory = ""
                            val firstMatch = categories.find { it.type == "Income" }
                            if (firstMatch != null) category = firstMatch.name
                        },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                    ) {
                        Text("Income", color = if (type == "Income") IncomeGreen else MaterialTheme.colorScheme.onSurface)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Amount RON
                OutlinedTextField(
                    value = amountText,
                    onValueChange = {
                        amountText = it
                        amountError = false
                    },
                    label = { Text("Amount (RON)") },
                    isError = amountError,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("tx_input_amount"),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Description
                OutlinedTextField(
                    value = description,
                    onValueChange = {
                        description = it
                        descError = false
                    },
                    label = { Text("Description") },
                    isError = descError,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("tx_input_desc"),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Date
                OutlinedTextField(
                    value = date,
                    onValueChange = { date = it },
                    label = { Text("Date (YYYY-MM-DD)") },
                    trailingIcon = {
                        Icon(imageVector = Icons.Default.CalendarToday, contentDescription = "Date")
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Subcategory Dropdown (Primary Selection)
                ExposedDropdownMenuBox(
                    expanded = subCategoryExpanded,
                    onExpandedChange = { subCategoryExpanded = !subCategoryExpanded }
                ) {
                    OutlinedTextField(
                        value = subCategory,
                        onValueChange = { newSub ->
                            subCategory = newSub
                            val match = availableCategoryItems.find { it.subCategory.equals(newSub, ignoreCase = true) }
                            if (match != null) {
                                category = match.name
                            }
                        },
                        label = { Text("Subcategory (Select First)") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = subCategoryExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    ExposedDropdownMenu(
                        expanded = subCategoryExpanded,
                        onDismissRequest = { subCategoryExpanded = false }
                    ) {
                        availableSubcategories.forEach { subName ->
                            DropdownMenuItem(
                                text = { Text(subName) },
                                onClick = {
                                    subCategory = subName
                                    subCategoryExpanded = false
                                    val match = availableCategoryItems.find { it.subCategory == subName }
                                    if (match != null) {
                                        category = match.name
                                    }
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Auto-Completed Category Display (Read-Only)
                OutlinedTextField(
                    value = category,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Category (Auto-selected)") },
                    supportingText = { Text("Category is auto-assigned based on selected subcategory", style = MaterialTheme.typography.labelSmall) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Account Dropdown
                ExposedDropdownMenuBox(
                    expanded = accountExpanded,
                    onExpandedChange = { accountExpanded = !accountExpanded }
                ) {
                    OutlinedTextField(
                        value = account,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Account") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = accountExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    ExposedDropdownMenu(
                        expanded = accountExpanded,
                        onDismissRequest = { accountExpanded = false }
                    ) {
                        accounts.forEach { acc ->
                            DropdownMenuItem(
                                text = { Text(acc) },
                                onClick = {
                                    account = acc
                                    accountExpanded = false
                                }
                            )
                        }
                    }
                }

                if (type == "Income") {
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = destination,
                        onValueChange = { destination = it },
                        label = { Text("Destination Account (Optional)") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Save / Confirm Action Button
                Button(
                    onClick = {
                        val parsedAmount = amountText.toDoubleOrNull()
                        if (parsedAmount == null || parsedAmount <= 0.0) {
                            amountError = true
                            return@Button
                        }
                        if (description.isBlank()) {
                            descError = true
                            return@Button
                        }

                        onSave(
                            if (isDuplicateMode) null else initialTransaction?.id,
                            date,
                            description,
                            parsedAmount,
                            type,
                            account,
                            category,
                            subCategory,
                            if (type == "Income") destination else null
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("save_transaction_button"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (type == "Income") IncomeGreen else MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text(
                        text = if (isDuplicateMode) "Confirm Duplicated Entry" else "Save Transaction",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
