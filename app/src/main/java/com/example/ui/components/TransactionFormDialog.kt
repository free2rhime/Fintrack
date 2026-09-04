package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.data.model.CategoryEntity
import com.example.data.model.TransactionEntity
import com.example.ui.theme.BodyRegular
import com.example.ui.theme.CardTitleAmount
import com.example.ui.theme.CobaltBlue
import com.example.ui.theme.ExpenseCoral
import com.example.ui.theme.HeroFinancialDisplay
import com.example.ui.theme.IncomeEmerald
import com.example.ui.theme.LabelBadgeMedium
import com.example.ui.theme.MicroMetadata
import com.example.ui.theme.RadiusMedium
import com.example.ui.theme.RadiusXLarge
import com.example.ui.theme.Space12
import com.example.ui.theme.Space16
import com.example.ui.theme.Space20
import com.example.ui.theme.Space4
import com.example.ui.theme.Space8
import com.example.ui.theme.SurfaceContainerDark
import com.example.ui.theme.SurfaceContainerHighDark
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

fun getAccountDisplayLabel(account: String): String =
    when (account) {
        "Meal Tickets" -> "Tichete de masa"
        else -> account
    }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionFormDialog(
    initialTransaction: TransactionEntity?,
    isDuplicateMode: Boolean,
    categories: List<CategoryEntity>,
    onDismiss: () -> Unit,
    onSearchDescriptions: (suspend (String) -> List<String>)? = null,
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
    val todayStr = remember { LocalDate.now(ZoneId.systemDefault()).toString() }

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

    // Description autocomplete suggestions
    var suggestions by remember { mutableStateOf<List<String>>(emptyList()) }
    var suggestionsExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(description) {
        if (description.trim().length >= 2 && onSearchDescriptions != null) {
            delay(200)
            val results = onSearchDescriptions(description.trim())
            suggestions = results.take(8)
            suggestionsExpanded = suggestions.isNotEmpty()
        } else {
            suggestions = emptyList()
            suggestionsExpanded = false
        }
    }

    // Material 3 DatePicker Dialog state
    var showDatePicker by remember { mutableStateOf(false) }
    val initialMillis = remember(date) {
        try {
            val parsed = LocalDate.parse(date, DateTimeFormatter.ISO_LOCAL_DATE)
            parsed.atStartOfDay(ZoneId.of("UTC")).toInstant().toEpochMilli()
        } catch (e: Exception) {
            System.currentTimeMillis()
        }
    }
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)

    // Category / Subcategory dropdown filtering
    val availableCategoryItems = remember(type, categories) {
        categories.filter { it.type == type }
    }
    val availableSubcategories = remember(availableCategoryItems) {
        availableCategoryItems.map { it.subCategory }.filter { it.isNotBlank() }.distinct()
    }

    var subCategoryExpanded by remember { mutableStateOf(false) }
    var accountExpanded by remember { mutableStateOf(false) }

    val accounts = listOf("Card", "Cash", "Meal Tickets")

    val textFieldColors = OutlinedTextFieldDefaults.colors(
        focusedContainerColor = SurfaceContainerDark,
        unfocusedContainerColor = SurfaceContainerDark,
        disabledContainerColor = SurfaceContainerDark,
        errorContainerColor = SurfaceContainerDark,
        focusedBorderColor = CobaltBlue,
        unfocusedBorderColor = SurfaceContainerHighDark,
        errorBorderColor = ExpenseCoral,
        focusedLabelColor = CobaltBlue,
        unfocusedLabelColor = TextSecondary,
        disabledLabelColor = TextMuted,
        errorLabelColor = ExpenseCoral,
        focusedTextColor = TextPrimary,
        unfocusedTextColor = TextPrimary,
        disabledTextColor = TextPrimary,
        errorTextColor = TextPrimary,
        focusedTrailingIconColor = TextSecondary,
        unfocusedTrailingIconColor = TextSecondary,
        disabledTrailingIconColor = TextMuted,
        cursorColor = CobaltBlue
    )

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 520.dp)
                .padding(vertical = Space8),
            shape = RoundedCornerShape(RadiusXLarge),
            color = SurfaceDark,
            tonalElevation = 4.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Space20)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = when {
                                isDuplicateMode -> "Duplicate Transaction"
                                initialTransaction != null -> "Edit Transaction"
                                else -> "Add Transaction"
                            },
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        if (isDuplicateMode) {
                            Spacer(modifier = Modifier.height(Space4))
                            Text(
                                text = "Date auto-updated to today ($todayStr)",
                                style = MicroMetadata,
                                color = CobaltBlue
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = TextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(Space16))

                // Type Toggle (Expense / Income)
                FinTrackSegmentedControl(
                    items = listOf("Expense", "Income"),
                    selectedIndex = if (type == "Expense") 0 else 1,
                    onItemSelected = { index ->
                        if (index == 0) {
                            type = "Expense"
                            destination = ""
                            subCategory = ""
                            val firstMatch = categories.find { it.type == "Expense" }
                            if (firstMatch != null) category = firstMatch.name
                        } else {
                            type = "Income"
                            subCategory = ""
                            val firstMatch = categories.find { it.type == "Income" }
                            if (firstMatch != null) category = firstMatch.name
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(Space16))

                // Amount RON — Hero Field
                OutlinedTextField(
                    value = amountText,
                    onValueChange = {
                        amountText = it
                        amountError = false
                    },
                    label = { Text("Amount (RON)") },
                    placeholder = {
                        Text(
                            text = "0.00",
                            style = HeroFinancialDisplay,
                            color = TextMuted
                        )
                    },
                    leadingIcon = {
                        Text(
                            text = if (type == "Income") "+ " else "- ",
                            style = HeroFinancialDisplay,
                            color = if (type == "Income") IncomeEmerald else ExpenseCoral,
                            modifier = Modifier.padding(start = Space12)
                        )
                    },
                    trailingIcon = {
                        Text(
                            text = "RON",
                            style = CardTitleAmount,
                            color = TextSecondary,
                            modifier = Modifier.padding(end = Space12)
                        )
                    },
                    isError = amountError,
                    supportingText = if (amountError) {
                        {
                            Text(
                                text = "Please enter a valid amount greater than 0",
                                color = ExpenseCoral,
                                style = MicroMetadata
                            )
                        }
                    } else null,
                    textStyle = HeroFinancialDisplay.copy(
                        color = if (type == "Income") IncomeEmerald else ExpenseCoral
                    ),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = SurfaceContainerDark,
                        unfocusedContainerColor = SurfaceContainerDark,
                        errorContainerColor = SurfaceContainerDark,
                        focusedBorderColor = CobaltBlue,
                        unfocusedBorderColor = SurfaceContainerHighDark,
                        errorBorderColor = ExpenseCoral,
                        focusedLabelColor = CobaltBlue,
                        unfocusedLabelColor = TextSecondary,
                        errorLabelColor = ExpenseCoral,
                        cursorColor = if (type == "Income") IncomeEmerald else ExpenseCoral
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("tx_input_amount"),
                    shape = RoundedCornerShape(RadiusMedium)
                )

                Spacer(modifier = Modifier.height(Space16))

                // Description with Autocomplete suggestions
                ExposedDropdownMenuBox(
                    expanded = suggestionsExpanded,
                    onExpandedChange = { suggestionsExpanded = it }
                ) {
                    OutlinedTextField(
                        value = description,
                        onValueChange = {
                            description = it
                            descError = false
                        },
                        label = { Text("Description") },
                        isError = descError,
                        supportingText = if (descError) {
                            {
                                Text(
                                    text = "Description is required",
                                    color = ExpenseCoral,
                                    style = MicroMetadata
                                )
                            }
                        } else null,
                        singleLine = true,
                        colors = textFieldColors,
                        textStyle = BodyRegular.copy(color = TextPrimary),
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                            .testTag("tx_input_desc"),
                        shape = RoundedCornerShape(RadiusMedium)
                    )

                    if (suggestions.isNotEmpty()) {
                        ExposedDropdownMenu(
                            expanded = suggestionsExpanded,
                            onDismissRequest = { suggestionsExpanded = false }
                        ) {
                            suggestions.forEach { sug ->
                                DropdownMenuItem(
                                    text = { Text(sug, color = TextPrimary, style = BodyRegular) },
                                    onClick = {
                                        description = sug
                                        suggestionsExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(Space16))

                // Date Picker trigger
                OutlinedTextField(
                    value = date,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Date (YYYY-MM-DD)") },
                    trailingIcon = {
                        IconButton(
                            onClick = { showDatePicker = true },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CalendarToday,
                                contentDescription = "Select Date",
                                tint = CobaltBlue
                            )
                        }
                    },
                    colors = textFieldColors,
                    textStyle = BodyRegular.copy(color = TextPrimary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showDatePicker = true },
                    shape = RoundedCornerShape(RadiusMedium)
                )

                Spacer(modifier = Modifier.height(Space16))

                // Income Destination Field (Optional for Income)
                if (type == "Income") {
                    Text(
                        text = "Destination (Optional)",
                        style = LabelBadgeMedium,
                        fontWeight = FontWeight.Medium,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(Space8))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(Space8)
                    ) {
                        listOf("Bubu", "Piticania").forEach { destName ->
                            val isSelected = destination == destName
                            Surface(
                                onClick = {
                                    destination = if (destination == destName) "" else destName
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .defaultMinSize(minHeight = 48.dp)
                                    .semantics {
                                        this.selected = isSelected
                                        this.role = Role.Tab
                                    },
                                shape = RoundedCornerShape(RadiusMedium),
                                color = if (isSelected) CobaltBlue else SurfaceContainerDark,
                                border = BorderStroke(
                                    1.dp,
                                    if (isSelected) CobaltBlue else SurfaceContainerHighDark
                                )
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.padding(horizontal = Space12, vertical = Space8)
                                ) {
                                    Text(
                                        text = destName,
                                        style = LabelBadgeMedium,
                                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                        color = if (isSelected) Color.White else TextSecondary
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(Space16))
                }

                // Subcategory Dropdown (Read-Only Selector)
                ExposedDropdownMenuBox(
                    expanded = subCategoryExpanded,
                    onExpandedChange = { subCategoryExpanded = !subCategoryExpanded }
                ) {
                    OutlinedTextField(
                        value = subCategory,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Subcategory (Select First)") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = subCategoryExpanded) },
                        colors = textFieldColors,
                        textStyle = BodyRegular.copy(color = TextPrimary),
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth(),
                        shape = RoundedCornerShape(RadiusMedium)
                    )

                    ExposedDropdownMenu(
                        expanded = subCategoryExpanded,
                        onDismissRequest = { subCategoryExpanded = false }
                    ) {
                        availableSubcategories.forEach { subName ->
                            DropdownMenuItem(
                                text = { Text(subName, color = TextPrimary, style = BodyRegular) },
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

                Spacer(modifier = Modifier.height(Space16))

                // Auto-Completed Category Display (Read-Only)
                OutlinedTextField(
                    value = category,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Category (Auto-selected)") },
                    supportingText = {
                        Text(
                            text = "Category is auto-assigned based on selected subcategory",
                            style = MicroMetadata,
                            color = TextSecondary
                        )
                    },
                    colors = textFieldColors,
                    textStyle = BodyRegular.copy(color = TextPrimary),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(RadiusMedium)
                )

                Spacer(modifier = Modifier.height(Space16))

                // Account Dropdown
                ExposedDropdownMenuBox(
                    expanded = accountExpanded,
                    onExpandedChange = { accountExpanded = !accountExpanded }
                ) {
                    OutlinedTextField(
                        value = getAccountDisplayLabel(account),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Account") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = accountExpanded) },
                        colors = textFieldColors,
                        textStyle = BodyRegular.copy(color = TextPrimary),
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth(),
                        shape = RoundedCornerShape(RadiusMedium)
                    )

                    ExposedDropdownMenu(
                        expanded = accountExpanded,
                        onDismissRequest = { accountExpanded = false }
                    ) {
                        accounts.forEach { acc ->
                            DropdownMenuItem(
                                text = { Text(getAccountDisplayLabel(acc), color = TextPrimary, style = BodyRegular) },
                                onClick = {
                                    account = acc
                                    accountExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(Space20))

                // Save / Cancel Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Space12),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FinTrackButton(
                        text = "Cancel",
                        onClick = onDismiss,
                        variant = ButtonVariant.SECONDARY,
                        modifier = Modifier.weight(1f)
                    )

                    FinTrackButton(
                        onClick = {
                            val parsedAmount = amountText.toDoubleOrNull()
                            if (parsedAmount == null || parsedAmount <= 0.0) {
                                amountError = true
                                return@FinTrackButton
                            }
                            if (description.isBlank()) {
                                descError = true
                                return@FinTrackButton
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
                                if (type == "Income" && destination.isNotBlank()) destination else null
                            )
                        },
                        variant = ButtonVariant.PRIMARY,
                        modifier = Modifier
                            .weight(2f)
                            .testTag("save_transaction_button")
                    ) {
                        Text(
                            text = if (isDuplicateMode) "Confirm Duplicated Entry" else "Save Transaction",
                            style = CardTitleAmount,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }

    // Material 3 Date Picker Dialog
    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        val selectedMillis = datePickerState.selectedDateMillis
                        if (selectedMillis != null) {
                            date = Instant.ofEpochMilli(selectedMillis)
                                .atZone(ZoneId.of("UTC"))
                                .toLocalDate()
                                .toString()
                        }
                        showDatePicker = false
                    }
                ) {
                    Text("OK", color = CobaltBlue)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

