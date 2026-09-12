package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
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
import com.example.ui.theme.FinTrackMotion
import com.example.ui.theme.FinTrackTheme
import com.example.ui.theme.HeroFinancialDisplay
import com.example.ui.theme.LabelBadgeMedium
import com.example.ui.theme.MicroMetadata
import com.example.ui.theme.RadiusMedium
import com.example.ui.theme.RadiusXLarge
import com.example.ui.theme.SectionHeadline
import com.example.ui.theme.ShapeExtraLarge
import com.example.ui.theme.ShapeGroupedContainer
import com.example.ui.theme.ShapeModalSheet
import com.example.ui.theme.ShapePill
import com.example.ui.theme.ShapeSquircleIcon
import com.example.ui.theme.Space12
import com.example.ui.theme.Space16
import com.example.ui.theme.Space2
import com.example.ui.theme.Space20
import com.example.ui.theme.Space4
import com.example.ui.theme.Space8
import com.example.ui.theme.isReducedMotionEnabled
import com.example.ui.theme.tactilePress
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

/**
 * Expressive segmented control for switching transaction type (Expense / Income).
 * Provides shape-morphing feedback, semantic coloration (Coral / Emerald),
 * 48dp minimum touch targets, and full accessibility semantics.
 */
@Composable
fun ExpressiveTypeSegmentedControl(
    selectedType: String,
    onTypeSelected: (String) -> Unit,
    reducedMotion: Boolean,
    modifier: Modifier = Modifier
) {
    val isExpense = selectedType == "Expense"

    Surface(
        shape = ShapePill,
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .padding(4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .selectableGroup(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Expense Tab
            val expenseBg by animateColorAsState(
                targetValue = if (isExpense) FinTrackTheme.colors.expenseContainer else Color.Transparent,
                animationSpec = if (reducedMotion) snap() else FinTrackMotion.selectionSpring(),
                label = "expense_tab_bg"
            )
            val expenseTextColor by animateColorAsState(
                targetValue = if (isExpense) FinTrackTheme.colors.expense else MaterialTheme.colorScheme.onSurfaceVariant,
                animationSpec = if (reducedMotion) snap() else FinTrackMotion.selectionSpring(),
                label = "expense_tab_text"
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .defaultMinSize(minHeight = 48.dp)
                    .clip(ShapePill)
                    .background(expenseBg)
                    .tactilePress(targetScale = 0.98f, enabled = !reducedMotion)
                    .clickable(role = Role.Tab) { onTypeSelected("Expense") }
                    .semantics {
                        this.selected = isExpense
                        this.role = Role.Tab
                        this.contentDescription = "Expense type"
                    },
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Space4)
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowDownward,
                        contentDescription = null,
                        tint = expenseTextColor,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Expense",
                        style = LabelBadgeMedium,
                        fontWeight = if (isExpense) FontWeight.Bold else FontWeight.Medium,
                        color = expenseTextColor
                    )
                }
            }

            Spacer(modifier = Modifier.width(Space4))

            // Income Tab
            val incomeBg by animateColorAsState(
                targetValue = if (!isExpense) FinTrackTheme.colors.incomeContainer else Color.Transparent,
                animationSpec = if (reducedMotion) snap() else FinTrackMotion.selectionSpring(),
                label = "income_tab_bg"
            )
            val incomeTextColor by animateColorAsState(
                targetValue = if (!isExpense) FinTrackTheme.colors.income else MaterialTheme.colorScheme.onSurfaceVariant,
                animationSpec = if (reducedMotion) snap() else FinTrackMotion.selectionSpring(),
                label = "income_tab_text"
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .defaultMinSize(minHeight = 48.dp)
                    .clip(ShapePill)
                    .background(incomeBg)
                    .tactilePress(targetScale = 0.98f, enabled = !reducedMotion)
                    .clickable(role = Role.Tab) { onTypeSelected("Income") }
                    .semantics {
                        this.selected = !isExpense
                        this.role = Role.Tab
                        this.contentDescription = "Income type"
                    },
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Space4)
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowUpward,
                        contentDescription = null,
                        tint = incomeTextColor,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Income",
                        style = LabelBadgeMedium,
                        fontWeight = if (!isExpense) FontWeight.Bold else FontWeight.Medium,
                        color = incomeTextColor
                    )
                }
            }
        }
    }
}

/**
 * Material 3 Expressive Add / Edit / Duplicate Transaction creation experience.
 *
 * Preserves every functional and test contract:
 * - Direct Dialog presentation for headless Robolectric compatibility
 * - Strict RON-native transaction input semantics with decoupled RON indicator
 * - Autocomplete suggestions with debounce and testTag contracts
 * - Category / Subcategory auto-assignment contracts
 * - Date picker and Account selection
 * - Conditional Income Destination selector
 * - Optional Delete action in Edit mode (hidden in Add and Duplicate modes)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionFormDialog(
    initialTransaction: TransactionEntity?,
    isDuplicateMode: Boolean,
    categories: List<CategoryEntity>,
    onDismiss: () -> Unit,
    onSearchDescriptions: (suspend (String) -> List<String>)? = null,
    onDelete: (() -> Unit)? = null,
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
    val reducedMotion = isReducedMotionEnabled()

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
    var showDeleteConfirmation by remember { mutableStateOf(false) }

    // Description autocomplete suggestions
    var suggestions by remember { mutableStateOf<List<String>>(emptyList()) }
    var suggestionsExpanded by remember { mutableStateOf(false) }
    var isSuggestionSelected by remember { mutableStateOf(false) }

    LaunchedEffect(description) {
        if (isSuggestionSelected) {
            return@LaunchedEffect
        }
        val query = description.trim()
        if (query.isNotEmpty() && onSearchDescriptions != null) {
            delay(200)
            val results = onSearchDescriptions(query)
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
        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
        disabledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
        errorContainerColor = MaterialTheme.colorScheme.surfaceContainer,
        focusedBorderColor = MaterialTheme.colorScheme.primary,
        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
        errorBorderColor = MaterialTheme.colorScheme.error,
        focusedLabelColor = MaterialTheme.colorScheme.primary,
        unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
        disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f),
        errorLabelColor = MaterialTheme.colorScheme.error,
        focusedTextColor = MaterialTheme.colorScheme.onSurface,
        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
        disabledTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
        errorTextColor = MaterialTheme.colorScheme.onSurface,
        focusedTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
        unfocusedTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
        disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f),
        cursorColor = MaterialTheme.colorScheme.primary
    )

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 540.dp)
                .padding(vertical = Space8),
            shape = ShapeExtraLarge,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Space20)
                    .verticalScroll(rememberScrollState())
            ) {
                // Expressive Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Space12)
                    ) {
                        Surface(
                            shape = ShapeSquircleIcon,
                            color = when {
                                isDuplicateMode -> MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                                initialTransaction != null -> MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                                else -> (if (type == "Income") FinTrackTheme.colors.income else FinTrackTheme.colors.expense).copy(alpha = 0.14f)
                            },
                            modifier = Modifier.size(44.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = when {
                                        isDuplicateMode -> Icons.Default.ContentCopy
                                        initialTransaction != null -> Icons.Default.Edit
                                        else -> if (type == "Income") Icons.Default.ArrowUpward else Icons.Default.ArrowDownward
                                    },
                                    contentDescription = null,
                                    tint = when {
                                        isDuplicateMode -> MaterialTheme.colorScheme.primary
                                        initialTransaction != null -> MaterialTheme.colorScheme.primary
                                        else -> if (type == "Income") FinTrackTheme.colors.income else FinTrackTheme.colors.expense
                                    },
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = when {
                                    isDuplicateMode -> "Duplicate Transaction"
                                    initialTransaction != null -> "Edit Transaction"
                                    else -> "Add Transaction"
                                },
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.semantics { heading() }
                            )
                            if (isDuplicateMode) {
                                Spacer(modifier = Modifier.height(Space2))
                                Text(
                                    text = "Date auto-updated to today ($todayStr)",
                                    style = MicroMetadata,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            } else {
                                Spacer(modifier = Modifier.height(Space2))
                                Text(
                                    text = if (initialTransaction != null) "Update transaction details" else "Record a new family entry",
                                    style = MicroMetadata,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(48.dp)
                            .tactilePress(enabled = !reducedMotion)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(Space16))

                // Expressive Type Toggle (Expense / Income)
                ExpressiveTypeSegmentedControl(
                    selectedType = type,
                    onTypeSelected = { newType ->
                        if (newType != type) {
                            type = newType
                            if (newType == "Expense") {
                                destination = ""
                                subCategory = ""
                                val firstMatch = categories.find { it.type == "Expense" }
                                if (firstMatch != null) category = firstMatch.name
                            } else {
                                subCategory = ""
                                val firstMatch = categories.find { it.type == "Income" }
                                if (firstMatch != null) category = firstMatch.name
                            }
                        }
                    },
                    reducedMotion = reducedMotion,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(Space16))

                // Hero Amount Section (RON-Native Creation Focal Point)
                Surface(
                    shape = ShapeGroupedContainer,
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = Space16, vertical = Space12)
                    ) {
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
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                )
                            },
                            leadingIcon = {
                                Text(
                                    text = if (type == "Income") "+ " else "- ",
                                    style = HeroFinancialDisplay,
                                    color = if (type == "Income") FinTrackTheme.colors.income else FinTrackTheme.colors.expense,
                                    modifier = Modifier.padding(start = Space12)
                                )
                            },
                            trailingIcon = {
                                Surface(
                                    shape = ShapePill,
                                    color = MaterialTheme.colorScheme.surfaceContainerHighest,
                                    modifier = Modifier.padding(end = Space8)
                                ) {
                                    Text(
                                        text = "RON",
                                        style = LabelBadgeMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.padding(horizontal = Space12, vertical = Space4)
                                    )
                                }
                            },
                            isError = amountError,
                            supportingText = if (amountError) {
                                {
                                    Text(
                                        text = "Please enter a valid amount greater than 0",
                                        color = MaterialTheme.colorScheme.error,
                                        style = MicroMetadata
                                    )
                                }
                            } else null,
                            textStyle = HeroFinancialDisplay.copy(
                                color = if (type == "Income") FinTrackTheme.colors.income else FinTrackTheme.colors.expense
                            ),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                                errorContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                                errorBorderColor = MaterialTheme.colorScheme.error,
                                focusedLabelColor = MaterialTheme.colorScheme.primary,
                                unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                errorLabelColor = MaterialTheme.colorScheme.error,
                                cursorColor = if (type == "Income") FinTrackTheme.colors.income else FinTrackTheme.colors.expense
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("tx_input_amount"),
                            shape = RoundedCornerShape(RadiusMedium)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(Space16))

                // Description with Autocomplete suggestions
                ExposedDropdownMenuBox(
                    expanded = suggestionsExpanded,
                    onExpandedChange = { suggestionsExpanded = it }
                ) {
                    OutlinedTextField(
                        value = description,
                        onValueChange = {
                            isSuggestionSelected = false
                            description = it
                            descError = false
                        },
                        label = { Text("Description") },
                        isError = descError,
                        supportingText = if (descError) {
                            {
                                Text(
                                    text = "Description is required",
                                    color = MaterialTheme.colorScheme.error,
                                    style = MicroMetadata
                                )
                            }
                        } else null,
                        singleLine = true,
                        colors = textFieldColors,
                        textStyle = BodyRegular.copy(color = MaterialTheme.colorScheme.onSurface),
                        modifier = Modifier
                            .menuAnchor(MenuAnchorType.PrimaryEditable, enabled = true)
                            .fillMaxWidth()
                            .testTag("tx_input_desc"),
                        shape = RoundedCornerShape(RadiusMedium)
                    )

                    if (suggestions.isNotEmpty()) {
                        ExposedDropdownMenu(
                            expanded = suggestionsExpanded,
                            onDismissRequest = { suggestionsExpanded = false },
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            shape = ShapeGroupedContainer
                        ) {
                            suggestions.forEachIndexed { index, sug ->
                                DropdownMenuItem(
                                    text = { Text(sug, color = MaterialTheme.colorScheme.onSurface, style = BodyRegular) },
                                    onClick = {
                                        isSuggestionSelected = true
                                        description = sug
                                        suggestions = emptyList()
                                        suggestionsExpanded = false
                                    },
                                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("tx_description_suggestion_$index")
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(Space16))

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
                        textStyle = BodyRegular.copy(color = MaterialTheme.colorScheme.onSurface),
                        modifier = Modifier
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true)
                            .fillMaxWidth(),
                        shape = RoundedCornerShape(RadiusMedium)
                    )

                    ExposedDropdownMenu(
                        expanded = subCategoryExpanded,
                        onDismissRequest = { subCategoryExpanded = false },
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        shape = ShapeGroupedContainer
                    ) {
                        availableSubcategories.forEach { subName ->
                            DropdownMenuItem(
                                text = { Text(subName, color = MaterialTheme.colorScheme.onSurface, style = BodyRegular) },
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
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    colors = textFieldColors,
                    textStyle = BodyRegular.copy(color = MaterialTheme.colorScheme.onSurface),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(RadiusMedium)
                )

                Spacer(modifier = Modifier.height(Space16))

                // Date & Account selectors (Responsive row with 48dp minimum touch targets)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Space12)
                ) {
                    // Date Picker trigger
                    Box(modifier = Modifier.weight(1f)) {
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
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            },
                            colors = textFieldColors,
                            textStyle = BodyRegular.copy(color = MaterialTheme.colorScheme.onSurface),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showDatePicker = true },
                            shape = RoundedCornerShape(RadiusMedium)
                        )
                    }

                    // Account Dropdown
                    Box(modifier = Modifier.weight(1f)) {
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
                                textStyle = BodyRegular.copy(color = MaterialTheme.colorScheme.onSurface),
                                modifier = Modifier
                                    .menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true)
                                    .fillMaxWidth(),
                                shape = RoundedCornerShape(RadiusMedium)
                            )

                            ExposedDropdownMenu(
                                expanded = accountExpanded,
                                onDismissRequest = { accountExpanded = false },
                                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                shape = ShapeGroupedContainer
                            ) {
                                accounts.forEach { acc ->
                                    DropdownMenuItem(
                                        text = { Text(getAccountDisplayLabel(acc), color = MaterialTheme.colorScheme.onSurface, style = BodyRegular) },
                                        onClick = {
                                            account = acc
                                            accountExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                // Income Destination Field (Animated Visibility, Optional for Income)
                AnimatedVisibility(
                    visible = type == "Income",
                    enter = if (reducedMotion) fadeIn(snap()) else fadeIn(FinTrackMotion.standardTween()) + expandVertically(),
                    exit = if (reducedMotion) fadeOut(snap()) else fadeOut(FinTrackMotion.standardTween()) + shrinkVertically()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = Space16)
                    ) {
                        Text(
                            text = "Destination (Optional)",
                            style = LabelBadgeMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(Space8))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .selectableGroup(),
                            horizontalArrangement = Arrangement.spacedBy(Space8)
                        ) {
                            listOf("Bubu", "Piticania").forEach { destName ->
                                val isSelected = destination == destName
                                val destBg by animateColorAsState(
                                    targetValue = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainer,
                                    animationSpec = if (reducedMotion) snap() else FinTrackMotion.selectionSpring(),
                                    label = "dest_bg_$destName"
                                )
                                val destTextColor by animateColorAsState(
                                    targetValue = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    animationSpec = if (reducedMotion) snap() else FinTrackMotion.selectionSpring(),
                                    label = "dest_text_$destName"
                                )
                                Surface(
                                    onClick = {
                                        destination = if (destination == destName) "" else destName
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .defaultMinSize(minHeight = 48.dp)
                                        .tactilePress(targetScale = 0.98f, enabled = !reducedMotion)
                                        .semantics {
                                            this.selected = isSelected
                                            this.role = Role.Tab
                                        },
                                    shape = ShapePill,
                                    color = destBg,
                                    border = null
                                ) {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier.padding(horizontal = Space12, vertical = Space8)
                                    ) {
                                        Text(
                                            text = destName,
                                            style = LabelBadgeMedium,
                                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                            color = destTextColor
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Destructive Action: Delete Transaction (Exposed strictly in Edit Mode, hidden in Add and Duplicate)
                if (initialTransaction != null && !isDuplicateMode && onDelete != null) {
                    Spacer(modifier = Modifier.height(Space20))

                    Surface(
                        onClick = { showDeleteConfirmation = true },
                        shape = ShapePill,
                        color = MaterialTheme.colorScheme.errorContainer,
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 48.dp)
                            .tactilePress(targetScale = 0.98f, enabled = !reducedMotion)
                            .testTag("tx_delete_button")
                            .semantics {
                                this.role = Role.Button
                                this.contentDescription = "Delete Transaction"
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = Space16, vertical = Space12),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(Space8))
                            Text(
                                text = "Delete Transaction",
                                style = LabelBadgeMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onErrorContainer
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

    // Material 3 Delete Confirmation Dialog
    if (showDeleteConfirmation && onDelete != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            shape = ShapeGroupedContainer,
            icon = {
                Surface(
                    shape = ShapeSquircleIcon,
                    color = MaterialTheme.colorScheme.errorContainer,
                    modifier = Modifier.size(48.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            },
            title = {
                Text(
                    text = "Delete Transaction",
                    style = SectionHeadline,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to delete this transaction? This action cannot be undone.",
                    style = BodyRegular,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                FinTrackButton(
                    text = "Delete",
                    onClick = {
                        showDeleteConfirmation = false
                        onDelete()
                    },
                    variant = ButtonVariant.DESTRUCTIVE,
                    modifier = Modifier.testTag("tx_confirm_delete_button")
                )
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmation = false }) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        )
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
                    Text("OK", color = MaterialTheme.colorScheme.primary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}
