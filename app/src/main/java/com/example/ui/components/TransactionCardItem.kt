package com.example.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import com.example.data.model.TransactionEntity
import com.example.data.util.NumberFormatter
import com.example.ui.theme.ExpenseCoral
import com.example.ui.theme.IncomeEmerald

/**
 * Resolves a semantic category icon based on category keywords.
 */
fun resolveCategoryIcon(category: String, isIncome: Boolean): ImageVector {
    val lower = category.lowercase()
    return when {
        lower.contains("food") || lower.contains("dining") || lower.contains("grocer") || lower.contains("restaurant") || lower.contains("cafe") -> Icons.Default.Restaurant
        lower.contains("transport") || lower.contains("uber") || lower.contains("transit") || lower.contains("ride") -> Icons.Default.DirectionsCar
        lower.contains("fuel") || lower.contains("gas") -> Icons.Default.LocalGasStation
        lower.contains("house") || lower.contains("housing") || lower.contains("rent") || lower.contains("utilit") -> Icons.Default.Home
        lower.contains("salary") || lower.contains("income") || lower.contains("pay") || lower.contains("freelance") || lower.contains("bonus") -> Icons.Default.Payments
        lower.contains("entertain") || lower.contains("movie") || lower.contains("leisure") || lower.contains("subscript") -> Icons.Default.Movie
        lower.contains("health") || lower.contains("pharmacy") || lower.contains("medic") || lower.contains("gym") || lower.contains("fitness") -> Icons.Default.MedicalServices
        lower.contains("shop") || lower.contains("electr") || lower.contains("clothing") -> Icons.Default.ShoppingBag
        isIncome -> Icons.Default.ArrowDownward
        else -> Icons.Default.Receipt
    }
}

/**
 * Transaction card item presentation wrapper.
 * Preserves existing API and test tags ("transaction_item_<id>", "tx_duplicate_<id>", "tx_delete_<id>")
 * while rendering through FinTrackTransactionRow.
 */
@Composable
fun TransactionCardItem(
    transaction: TransactionEntity,
    selectedCurrency: String,
    onDuplicateClicked: (TransactionEntity) -> Unit,
    onEditClicked: (TransactionEntity) -> Unit,
    onDeleteClicked: (TransactionEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val isIncome = transaction.type == "Income"
    val useRon = selectedCurrency == "RON"

    val formattedRon = NumberFormatter.formatAmount(transaction.amountRON)
    val formattedEur = NumberFormatter.formatAmount(transaction.amountEUR)

    val primaryAmount = if (useRon) formattedRon else formattedEur
    val primaryCurrency = if (useRon) "RON" else "EUR"

    val hasOfficialEur = transaction.conversionStatus == "OFFICIAL" && transaction.exchangeRateSource == "BNR_OFFICIAL" && transaction.exchangeRate > 0.0

    val (secondaryAmount, secondaryCurrency) = if (hasOfficialEur) {
        val sign = if (isIncome) "+" else "-"
        if (useRon) {
            "$sign$formattedEur" to "EUR"
        } else {
            "$sign$formattedRon" to "RON"
        }
    } else {
        null to null
    }

    val (statusLabel, statusVariant) = when {
        hasOfficialEur -> null to null
        transaction.conversionStatus == "PENDING" || transaction.conversionStatus?.startsWith("PENDING_") == true -> {
            val label = if (transaction.conversionStatus?.contains("PUBLISHED") == true) "EUR Pending (Future)" else "EUR Pending"
            label to BadgeVariant.WARNING
        }
        transaction.conversionStatus == "FAILED" || transaction.conversionStatus?.startsWith("FAILED_") == true -> {
            "EUR Failed" to BadgeVariant.ERROR
        }
        transaction.conversionStatus == "UNVERIFIED" || (transaction.exchangeRateSource != null && transaction.exchangeRateSource != "BNR_OFFICIAL") -> {
            "Unverified Rate" to BadgeVariant.WARNING
        }
        else -> null to null
    }

    val categoryText = transaction.subCategory.ifBlank { transaction.category }
    val accountText = getAccountDisplayLabel(transaction.account)
    val categoryIcon = resolveCategoryIcon(transaction.category, isIncome)

    FinTrackTransactionRow(
        description = transaction.description,
        categoryName = categoryText,
        dateFormatted = transaction.date,
        accountName = accountText,
        amountPrimaryFormatted = primaryAmount,
        isIncome = isIncome,
        amountSecondaryFormatted = secondaryAmount,
        primaryCurrency = primaryCurrency,
        secondaryCurrency = secondaryCurrency ?: "EUR",
        statusLabel = statusLabel,
        statusVariant = statusVariant,
        categoryIcon = categoryIcon,
        categoryColor = if (isIncome) IncomeEmerald else ExpenseCoral,
        onClick = { onEditClicked(transaction) },
        onDuplicateClick = { onDuplicateClicked(transaction) },
        onEditClick = { onEditClicked(transaction) },
        onDeleteClick = { onDeleteClicked(transaction) },
        duplicateTestTag = "tx_duplicate_${transaction.id}",
        deleteTestTag = "tx_delete_${transaction.id}",
        modifier = modifier.testTag("transaction_item_${transaction.id}")
    )
}

