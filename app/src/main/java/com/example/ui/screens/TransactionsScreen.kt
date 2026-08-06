package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.CategoryEntity
import com.example.data.model.FilterSettings
import com.example.data.model.TransactionEntity
import com.example.data.util.NumberFormatter
import com.example.ui.components.CurrencyToggle
import com.example.ui.components.PeriodSelectorChipRow
import com.example.ui.components.TransactionCardItem

@Composable
fun TransactionsScreen(
    transactions: List<TransactionEntity>,
    categories: List<CategoryEntity>,
    filterSettings: FilterSettings,
    onPeriodSelected: (String) -> Unit,
    onCurrencyChanged: (String) -> Unit,
    onCategoryFilterSelected: (String, String?) -> Unit,
    onSearchQueryChanged: (String) -> Unit,
    onAddTransactionClicked: () -> Unit,
    onDuplicateClicked: (TransactionEntity) -> Unit,
    onEditClicked: (TransactionEntity) -> Unit,
    onDeleteClicked: (TransactionEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf(filterSettings.searchQuery) }

    // Group transactions by date
    val groupedByDate = remember(transactions, searchQuery) {
        val filtered = if (searchQuery.isBlank()) transactions else {
            val q = searchQuery.lowercase()
            transactions.filter {
                it.description.lowercase().contains(q) ||
                        it.category.lowercase().contains(q) ||
                        it.subCategory.lowercase().contains(q) ||
                        it.account.lowercase().contains(q)
            }
        }
        filtered.groupBy { it.date }
    }

    val allCategoriesList = remember(categories, transactions) {
        val catNames = categories.map { it.name }.filter { it.isNotBlank() }
        val txCatNames = transactions.map { it.category }.filter { it.isNotBlank() }
        (catNames + txCatNames).distinct()
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddTransactionClicked,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("fab_add_transaction")
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Add Transaction")
            }
        },
        modifier = modifier.fillMaxSize()
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Header Title & Currency Toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Transactions",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
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
                placeholder = { Text("Search description, category...") },
                leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = "Search") },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = {
                            searchQuery = ""
                            onSearchQueryChanged("")
                        }) {
                            Icon(imageVector = Icons.Default.Clear, contentDescription = "Clear")
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .testTag("search_transactions_input"),
                shape = RoundedCornerShape(16.dp),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Period Selector Chips
            PeriodSelectorChipRow(
                selectedPeriod = filterSettings.selectedPeriod,
                onPeriodSelected = onPeriodSelected
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Category Filter Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val activeCategoryFilter = filterSettings.selectedExpenseCategory ?: filterSettings.selectedIncomeCategory
                val isAllSelected = activeCategoryFilter.isNullOrBlank()

                FilterChip(
                    selected = isAllSelected,
                    onClick = { onCategoryFilterSelected("Expense", null) },
                    label = { Text("All Categories", fontWeight = if (isAllSelected) FontWeight.Bold else FontWeight.Normal) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )

                allCategoriesList.forEach { catName ->
                    val isSelected = activeCategoryFilter == catName
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            if (isSelected) {
                                onCategoryFilterSelected("Expense", null)
                            } else {
                                onCategoryFilterSelected("Expense", catName)
                            }
                        },
                        label = { Text(catName, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Transaction Cards grouped by Date
            if (groupedByDate.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "No transactions found",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Try adjusting filters or tap + to record a new transaction.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    groupedByDate.forEach { (date, dateGroupTxs) ->
                        // Calculate Daily Total
                        val useRon = filterSettings.selectedCurrency == "RON"
                        val dayIncome = dateGroupTxs.filter { it.type == "Income" }
                            .sumOf { if (useRon) it.amountRON else it.amountEUR }
                        val dayExpense = dateGroupTxs.filter { it.type == "Expense" }
                            .sumOf { if (useRon) it.amountRON else it.amountEUR }

                        item(key = "header_$date") {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = date,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )

                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant
                                ) {
                                    Text(
                                        text = "Day: +${NumberFormatter.formatAmount(dayIncome)} / -${NumberFormatter.formatAmount(dayExpense)} ${filterSettings.selectedCurrency}",
                                        style = MaterialTheme.typography.labelSmall,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
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
