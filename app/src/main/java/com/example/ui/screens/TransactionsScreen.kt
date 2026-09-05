package com.example.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.example.data.model.CategoryEntity
import com.example.data.model.FilterSettings
import com.example.data.model.TransactionEntity
import com.example.data.util.NumberFormatter
import com.example.ui.components.CurrencyToggle
import com.example.ui.components.FinTrackEmptyState
import com.example.ui.components.FinTrackSegmentedControl
import com.example.ui.components.PeriodSelectorChipRow
import com.example.ui.components.TransactionCardItem
import com.example.ui.theme.BodyRegular
import com.example.ui.theme.CardTitleAmount
import com.example.ui.theme.CobaltBlue
import com.example.ui.theme.ExpenseCoral
import com.example.ui.theme.IncomeEmerald
import com.example.ui.theme.MicroMetadata
import com.example.ui.theme.RadiusLarge
import com.example.ui.theme.RadiusMedium
import com.example.ui.theme.RadiusSmall
import com.example.ui.theme.SectionHeadline
import com.example.ui.theme.Space12
import com.example.ui.theme.Space16
import com.example.ui.theme.Space2
import com.example.ui.theme.Space24
import com.example.ui.theme.Space4
import com.example.ui.theme.Space8
import com.example.ui.theme.SurfaceContainerDark
import com.example.ui.theme.SurfaceContainerHighDark
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

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
    onPeriodSelected: (String) -> Unit,
    onCurrencyChanged: (String) -> Unit,
    onTypeFilterSelected: (String) -> Unit = {},
    onCategoryFilterSelected: (String, String?) -> Unit,
    onSearchQueryChanged: (String) -> Unit,
    onAddTransactionClicked: () -> Unit,
    onDuplicateClicked: (TransactionEntity) -> Unit,
    onEditClicked: (TransactionEntity) -> Unit,
    onDeleteClicked: (TransactionEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    var searchQuery by remember(filterSettings.searchQuery) { mutableStateOf(filterSettings.searchQuery) }

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
                containerColor = CobaltBlue,
                contentColor = Color.White,
                shape = RoundedCornerShape(RadiusLarge),
                modifier = Modifier.testTag("fab_add_transaction")
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
                    .widthIn(max = 680.dp)
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
                    color = TextPrimary
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
                        color = TextMuted
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = TextSecondary
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
                                tint = TextSecondary
                            )
                        }
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = SurfaceDark,
                    unfocusedContainerColor = SurfaceDark,
                    focusedBorderColor = CobaltBlue,
                    unfocusedBorderColor = SurfaceContainerHighDark,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    cursorColor = CobaltBlue
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

            Spacer(modifier = Modifier.height(Space8))

            // Period Selector Chips
            PeriodSelectorChipRow(
                selectedPeriod = filterSettings.selectedPeriod,
                onPeriodSelected = onPeriodSelected
            )

            Spacer(modifier = Modifier.height(Space8))

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
                modifier = Modifier.padding(horizontal = Space16)
            )

            Spacer(modifier = Modifier.height(Space8))

            // Category Filter Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = Space16),
                horizontalArrangement = Arrangement.spacedBy(Space8)
            ) {
                val activeCategoryFilter = filterSettings.selectedExpenseCategory ?: filterSettings.selectedIncomeCategory
                val isAllSelected = activeCategoryFilter.isNullOrBlank()

                FilterChip(
                    selected = isAllSelected,
                    onClick = { onCategoryFilterSelected(filterSettings.selectedType, null) },
                    shape = RoundedCornerShape(RadiusMedium),
                    border = null,
                    label = {
                        Text(
                            text = "All Categories",
                            fontWeight = if (isAllSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = CobaltBlue,
                        selectedLabelColor = Color.White,
                        containerColor = SurfaceContainerDark,
                        labelColor = TextSecondary
                    )
                )

                filteredCategoriesList.forEach { catName ->
                    val isSelected = activeCategoryFilter == catName
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            if (isSelected) {
                                onCategoryFilterSelected(filterSettings.selectedType, null)
                            } else {
                                onCategoryFilterSelected(filterSettings.selectedType, catName)
                            }
                        },
                        shape = RoundedCornerShape(RadiusMedium),
                        border = null,
                        label = {
                            Text(
                                text = catName,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = CobaltBlue,
                            selectedLabelColor = Color.White,
                            containerColor = SurfaceContainerDark,
                            labelColor = TextSecondary
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(Space12))

            // Transaction Cards grouped by Date
            if (groupedByDate.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(Space24),
                    contentAlignment = Alignment.Center
                ) {
                    FinTrackEmptyState(
                        title = "No transactions found",
                        description = "Try adjusting filters or tap + to record a new transaction.",
                        icon = Icons.Default.ReceiptLong,
                        iconTint = TextMuted,
                        actionLabel = "Add Transaction",
                        onActionClick = onAddTransactionClicked
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = Space16),
                    verticalArrangement = Arrangement.spacedBy(Space8)
                ) {
                    groupedByDate.forEach { (date, dateGroupTxs) ->
                        // Calculate Daily Total
                        val useRon = filterSettings.selectedCurrency == "RON"
                        val dayIncome = dateGroupTxs.filter {
                            it.type == "Income" && (useRon || (it.conversionStatus == "OFFICIAL" && it.exchangeRateSource == "BNR_OFFICIAL" && it.exchangeRate > 0.0))
                        }.sumOf { if (useRon) it.amountRON else it.amountEUR }
                        val dayExpense = dateGroupTxs.filter {
                            it.type == "Expense" && (useRon || (it.conversionStatus == "OFFICIAL" && it.exchangeRateSource == "BNR_OFFICIAL" && it.exchangeRate > 0.0))
                        }.sumOf { if (useRon) it.amountRON else it.amountEUR }

                        item(key = "header_$date") {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = Space8),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = formatLocalizedDateHeader(date),
                                    style = CardTitleAmount,
                                    color = TextPrimary
                                )

                                Surface(
                                    shape = RoundedCornerShape(RadiusSmall),
                                    color = SurfaceContainerDark,
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
                                            color = IncomeEmerald
                                        )
                                        Text(
                                            text = "/",
                                            style = MicroMetadata,
                                            color = TextMuted
                                        )
                                        Text(
                                            text = "-${NumberFormatter.formatAmount(dayExpense)} ${filterSettings.selectedCurrency}",
                                            style = MicroMetadata,
                                            fontWeight = FontWeight.SemiBold,
                                            color = ExpenseCoral
                                        )
                                    }
                                }
                            }
                        }

                        items(dateGroupTxs, key = { it.id }) { tx ->
                            TransactionCardItem(
                                transaction = tx,
                                selectedCurrency = filterSettings.selectedCurrency,
                                onDuplicateClicked = onDuplicateClicked,
                                onEditClicked = onEditClicked,
                                onDeleteClicked = onDeleteClicked
                            )
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(80.dp))
                    }
                }
            }
        }
        }
    }
}

