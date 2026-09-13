package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.snap
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.example.data.model.CategoryEntity
import com.example.data.model.FilterSettings
import com.example.data.model.TransactionEntity
import com.example.data.util.NumberFormatter
import com.example.ui.components.ButtonVariant
import com.example.ui.components.CurrencyToggle
import com.example.ui.components.FinTrackButton
import com.example.ui.components.FinTrackEmptyState
import com.example.ui.components.FinTrackSegmentedControl
import com.example.ui.components.TransactionCardItem
import com.example.ui.theme.BodyRegular
import com.example.ui.theme.CardTitleAmount
import com.example.ui.theme.FinTrackMotion
import com.example.ui.theme.FinTrackTheme
import com.example.ui.theme.LabelBadgeMedium
import com.example.ui.theme.MicroMetadata
import com.example.ui.theme.RadiusLarge
import com.example.ui.theme.RadiusMedium
import com.example.ui.theme.RadiusSmall
import com.example.ui.theme.SectionHeadline
import com.example.ui.theme.ShapeFloatingActionButton
import com.example.ui.theme.ShapeGroupedContainer
import com.example.ui.theme.ShapeGroupedItemBottom
import com.example.ui.theme.ShapeGroupedItemMiddle
import com.example.ui.theme.ShapePill
import com.example.ui.theme.ShapeSquircleIcon
import com.example.ui.theme.Space12
import com.example.ui.theme.Space16
import com.example.ui.theme.Space2
import com.example.ui.theme.DividerThicknessHairline
import com.example.ui.theme.MaxContentWidthTablet
import com.example.ui.theme.SpacingBottomNavContent
import com.example.ui.theme.Space24
import com.example.ui.theme.Space4
import com.example.ui.theme.Space8
import com.example.ui.theme.isReducedMotionEnabled
import com.example.ui.theme.tactilePress
import kotlinx.coroutines.launch
fun formatLocalizedDateHeader(isoDate: String): String {
    return try {
        val parsed = java.time.LocalDate.parse(isoDate)
        val date = java.util.Date.from(parsed.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant())
        java.text.DateFormat.getDateInstance(java.text.DateFormat.MEDIUM, java.util.Locale.getDefault()).format(date)
    } catch (e: Exception) {
        isoDate
    }
}

@Composable
fun TransactionsScreen(
    transactions: List<TransactionEntity>,
    categories: List<CategoryEntity>,
    filterSettings: FilterSettings,
    onPeriodSelected: (String) -> Unit = {},
    onCurrencyChanged: (String) -> Unit,
    onTypeFilterSelected: (String) -> Unit = {},
    onCategoryFilterSelected: (String, String?) -> Unit,
    onSearchQueryChanged: (String) -> Unit,
    onAddTransactionClicked: () -> Unit,
    onDuplicateClicked: (TransactionEntity) -> Unit,
    onEditClicked: (TransactionEntity) -> Unit,
    onDeleteClicked: (TransactionEntity) -> Unit,
    showFilters: Boolean = true,
    onToggleFilters: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val haptic = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()
    val isReducedMotion = isReducedMotionEnabled()
    val listState = rememberLazyListState()
    var searchQuery by remember(filterSettings.searchQuery) { mutableStateOf(filterSettings.searchQuery) }
    var localShowFilters by rememberSaveable { mutableStateOf(showFilters) }
    val effectiveShowFilters = if (onToggleFilters != null) showFilters else localShowFilters
    var transactionPendingDelete by remember { mutableStateOf<TransactionEntity?>(null) }

    // Group transactions by date (filtered via ViewModel StateFlow)
    val groupedByDate = remember(transactions) {
        transactions.groupBy { it.date }
    }

    val filteredCategoriesList = remember(categories, filterSettings.selectedType) {
        if (filterSettings.selectedType == "All") {
            categories.map { it.name }.filter { it.isNotBlank() }.distinct()
        } else {
            categories.filter { it.type == filterSettings.selectedType }.map { it.name }.filter { it.isNotBlank() }.distinct()
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0.dp),
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddTransactionClicked,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = ShapeFloatingActionButton,
                modifier = Modifier
                    .tactilePress()
                    .testTag("fab_add_transaction")
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Add Transaction")
            }
        },
        modifier = modifier.fillMaxSize()
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = MaxContentWidthTablet)
            ) {
            // Header Title & Currency Toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Space16, vertical = Space8),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Transactions",
                    style = SectionHeadline,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.semantics { heading() }
                )

                CurrencyToggle(
                    selectedCurrency = filterSettings.selectedCurrency,
                    onCurrencyChanged = onCurrencyChanged
                )
            }

            // Search Bar full-width row
            OutlinedTextField(
                value = searchQuery,
                onValueChange = {
                    searchQuery = it
                    onSearchQueryChanged(it)
                },
                placeholder = {
                    Text(
                        text = "Search description, category...",
                        style = BodyRegular,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = {
                            searchQuery = ""
                            onSearchQueryChanged("")
                        }) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Clear search",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                    cursorColor = MaterialTheme.colorScheme.primary
                ),
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Search
                ),
                keyboardActions = KeyboardActions(
                    onSearch = {
                        keyboardController?.hide()
                        onSearchQueryChanged(searchQuery)
                    }
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Space16, vertical = Space2)
                    .testTag("search_transactions_input"),
                shape = RoundedCornerShape(RadiusLarge),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(Space4))

            // Active filter count and hide/show toggle
            val activeCategoryFilter = filterSettings.selectedExpenseCategory ?: filterSettings.selectedIncomeCategory
            val activeFilterCount = (if (filterSettings.selectedType != "All") 1 else 0) +
                (if (!activeCategoryFilter.isNullOrBlank()) 1 else 0)

            val filterButtonLabel = if (effectiveShowFilters) {
                "Hide filters"
            } else if (activeFilterCount > 0) {
                "Filters • $activeFilterCount"
            } else {
                "Filters"
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Space16, vertical = Space4),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        if (onToggleFilters != null) {
                            onToggleFilters()
                        } else {
                            localShowFilters = !localShowFilters
                        }
                    },
                    shape = ShapePill,
                    color = if (!effectiveShowFilters && activeFilterCount > 0) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer,
                    modifier = Modifier
                        .defaultMinSize(minHeight = 48.dp)
                        .tactilePress()
                        .testTag("toggle_filters_button")
                        .semantics {
                            this.role = Role.Button
                            this.contentDescription = filterButtonLabel
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = Space12, vertical = Space4),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Space4)
                    ) {
                        Icon(
                            imageVector = Icons.Default.FilterList,
                            contentDescription = null,
                            tint = if (!effectiveShowFilters && activeFilterCount > 0) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = filterButtonLabel,
                            style = LabelBadgeMedium,
                            fontWeight = if (!effectiveShowFilters && activeFilterCount > 0) FontWeight.Bold else FontWeight.Medium,
                            color = if (!effectiveShowFilters && activeFilterCount > 0) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            AnimatedVisibility(
                visible = effectiveShowFilters,
                enter = if (isReducedMotion) fadeIn(snap()) else fadeIn(FinTrackMotion.contentEntranceSpec()) + expandVertically(FinTrackMotion.contentEntranceSpec()),
                exit = if (isReducedMotion) fadeOut(snap()) else fadeOut(FinTrackMotion.contentChangeSpec()) + shrinkVertically(FinTrackMotion.contentChangeSpec())
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Type Filter Segmented Control (All / Income / Expense)
                    FinTrackSegmentedControl(
                        items = listOf("All", "Income", "Expense"),
                        selectedIndex = when (filterSettings.selectedType) {
                            "Income" -> 1
                            "Expense" -> 2
                            else -> 0
                        },
                        onItemSelected = { index ->
                            onTypeFilterSelected(
                                when (index) {
                                    1 -> "Income"
                                    2 -> "Expense"
                                    else -> "All"
                                }
                            )
                        },
                        isCompact = true,
                        modifier = Modifier.padding(horizontal = Space16)
                    )

                    Spacer(modifier = Modifier.height(Space4))

                    // Category Filter Chips
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = Space16),
                        horizontalArrangement = Arrangement.spacedBy(Space8)
                    ) {
                        val isAllSelected = activeCategoryFilter.isNullOrBlank()

                        FilterChip(
                            selected = isAllSelected,
                            onClick = {
                                if (!isAllSelected) {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                }
                                onCategoryFilterSelected(filterSettings.selectedType, null)
                            },
                            shape = ShapePill,
                            border = null,
                            modifier = Modifier.tactilePress(),
                            label = {
                                Text(
                                    text = "All Categories",
                                    fontWeight = if (isAllSelected) FontWeight.Bold else FontWeight.Medium
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                                containerColor = MaterialTheme.colorScheme.surfaceContainer,
                                labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )

                        filteredCategoriesList.forEach { catName ->
                            val isSelected = activeCategoryFilter == catName
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    if (isSelected) {
                                        onCategoryFilterSelected(filterSettings.selectedType, null)
                                    } else {
                                        onCategoryFilterSelected(filterSettings.selectedType, catName)
                                    }
                                },
                                shape = ShapePill,
                                border = null,
                                modifier = Modifier.tactilePress(),
                                label = {
                                    Text(
                                        text = catName,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                                    labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(Space8))
                }
            }

            // Transaction Cards grouped by Date
            if (groupedByDate.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(Space24),
                    contentAlignment = Alignment.Center
                ) {
                    FinTrackEmptyState(
                        title = "No transactions found",
                        description = "Try adjusting filters or tap + to record a new transaction.",
                        icon = Icons.AutoMirrored.Filled.ReceiptLong,
                        iconTint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        actionLabel = "Add Transaction",
                        onActionClick = onAddTransactionClicked
                    )
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = Space16),
                    verticalArrangement = Arrangement.spacedBy(Space16)
                ) {
                    items(
                        items = groupedByDate.entries.toList(),
                        key = { (date, _) -> "group_$date" }
                    ) { (date, dateGroupTxs) ->
                        // Calculate Daily Total
                        val useRon = filterSettings.selectedCurrency == "RON"
                        val dayIncome = dateGroupTxs.filter {
                            it.type == "Income" && (useRon || (it.conversionStatus == "OFFICIAL" && it.exchangeRateSource == "BNR_OFFICIAL" && it.exchangeRate > 0.0))
                        }.sumOf { if (useRon) it.amountRON else it.amountEUR }
                        val dayExpense = dateGroupTxs.filter {
                            it.type == "Expense" && (useRon || (it.conversionStatus == "OFFICIAL" && it.exchangeRateSource == "BNR_OFFICIAL" && it.exchangeRate > 0.0))
                        }.sumOf { if (useRon) it.amountRON else it.amountEUR }

                        Surface(
                            shape = ShapeGroupedContainer,
                            color = MaterialTheme.colorScheme.surfaceContainer,
                            modifier = Modifier
                                .fillMaxWidth()
                                .animateItem()
                                .testTag("transaction_date_group_$date")
                        ) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                // Integrated Day Header
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = Space16, vertical = Space12),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = formatLocalizedDateHeader(date),
                                        style = SectionHeadline,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )

                                    Surface(
                                        shape = RoundedCornerShape(RadiusSmall),
                                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                        modifier = Modifier.semantics {
                                            contentDescription = "Day total: income +${NumberFormatter.formatAmount(dayIncome)}, expense -${NumberFormatter.formatAmount(dayExpense)} ${filterSettings.selectedCurrency}"
                                        }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = Space8, vertical = Space4),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(Space4)
                                         ) {
                                            Text(
                                                text = "+${NumberFormatter.formatAmount(dayIncome)}",
                                                style = MicroMetadata,
                                                fontWeight = FontWeight.SemiBold,
                                                color = FinTrackTheme.colors.income
                                            )
                                            Text(
                                                text = "/",
                                                style = MicroMetadata,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                            )
                                            Text(
                                                text = "-${NumberFormatter.formatAmount(dayExpense)} ${filterSettings.selectedCurrency}",
                                                style = MicroMetadata,
                                                fontWeight = FontWeight.SemiBold,
                                                color = FinTrackTheme.colors.expense
                                            )
                                        }
                                    }
                                }

                                HorizontalDivider(
                                    modifier = Modifier.padding(horizontal = Space16),
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                                    thickness = DividerThicknessHairline
                                )

                                dateGroupTxs.forEachIndexed { index, tx ->
                                    val isLast = index == dateGroupTxs.size - 1
                                    val isSingle = dateGroupTxs.size == 1
                                    val itemShape = when {
                                        isSingle -> ShapeGroupedItemBottom
                                        isLast -> ShapeGroupedItemBottom
                                        else -> ShapeGroupedItemMiddle
                                    }

                                    TransactionCardItem(
                                        transaction = tx,
                                        selectedCurrency = filterSettings.selectedCurrency,
                                        onDuplicateClicked = onDuplicateClicked,
                                        onEditClicked = onEditClicked,
                                        onDeleteClicked = { transactionPendingDelete = it },
                                        shape = itemShape,
                                        showDivider = !isLast,
                                        containerColor = Color.Transparent
                                    )
                                }
                            }
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(SpacingBottomNavContent))
                    }
                }
            }
        }

            // Floating "Jump to latest transaction" pill
            val showJumpToLatest by remember {
                derivedStateOf {
                    listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 0
                }
            }

            AnimatedVisibility(
                visible = showJumpToLatest,
                enter = if (isReducedMotion) fadeIn(snap()) else fadeIn(FinTrackMotion.microTween()) + slideInVertically(FinTrackMotion.contentEntranceSpec()) { it / 2 },
                exit = if (isReducedMotion) fadeOut(snap()) else fadeOut(FinTrackMotion.microTween()) + slideOutVertically(FinTrackMotion.contentChangeSpec()) { it / 2 },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 72.dp)
            ) {
                Surface(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        coroutineScope.launch {
                            if (isReducedMotion) {
                                listState.scrollToItem(0)
                            } else {
                                listState.animateScrollToItem(0)
                            }
                        }
                    },
                    shape = ShapePill,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    shadowElevation = 4.dp,
                    tonalElevation = 4.dp,
                    modifier = Modifier
                        .defaultMinSize(minHeight = 48.dp, minWidth = 48.dp)
                        .tactilePress()
                        .testTag("btn_jump_to_latest")
                        .semantics {
                            this.role = Role.Button
                            this.contentDescription = "Jump to latest transaction"
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = Space16, vertical = Space8),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Space8)
                    ) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowUp,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Jump to latest",
                            style = LabelBadgeMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        // Delete Confirmation Dialog for transaction list row action
        if (transactionPendingDelete != null) {
            val tx = transactionPendingDelete!!
            AlertDialog(
                onDismissRequest = { transactionPendingDelete = null },
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
                        text = "Delete transaction?",
                        style = SectionHeadline,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Text(
                        text = "This transaction will be permanently deleted. This action cannot be undone.",
                        style = BodyRegular,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                confirmButton = {
                    FinTrackButton(
                        text = "Delete",
                        onClick = {
                            transactionPendingDelete = null
                            onDeleteClicked(tx)
                        },
                        variant = ButtonVariant.DESTRUCTIVE,
                        modifier = Modifier
                            .testTag("confirm_delete_transaction_button")
                            .testTag("tx_confirm_delete_button")
                    )
                },
                dismissButton = {
                    TextButton(
                        onClick = { transactionPendingDelete = null },
                        modifier = Modifier.testTag("cancel_delete_transaction_button")
                    ) {
                        Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                modifier = Modifier.testTag("delete_transaction_dialog")
            )
        }
    }
}

