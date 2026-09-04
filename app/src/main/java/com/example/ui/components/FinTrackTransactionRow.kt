package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.ui.theme.CardTitleAmount
import com.example.ui.theme.ExpenseContainer
import com.example.ui.theme.ExpenseCoral
import com.example.ui.theme.IncomeContainer
import com.example.ui.theme.IncomeEmerald
import com.example.ui.theme.MicroMetadata
import com.example.ui.theme.RadiusLarge
import com.example.ui.theme.Space12
import com.example.ui.theme.Space2
import com.example.ui.theme.Space4
import com.example.ui.theme.Space8
import com.example.ui.theme.SurfaceContainerDark
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

/**
 * Reusable presentation row for transactions in FinTrack Design System v1.
 * Pure presentation component that displays transaction details with clear hierarchy,
 * semantic color tokens, tabular numerals, accessible descriptions, and optional actions.
 */
@Composable
fun FinTrackTransactionRow(
    description: String,
    categoryName: String,
    dateFormatted: String,
    amountPrimaryFormatted: String,
    isIncome: Boolean,
    modifier: Modifier = Modifier,
    accountName: String? = null,
    amountSecondaryFormatted: String? = null,
    primaryCurrency: String = "RON",
    secondaryCurrency: String = "EUR",
    statusLabel: String? = null,
    statusVariant: BadgeVariant? = null,
    categoryIcon: ImageVector = Icons.Default.Receipt,
    categoryColor: Color = if (isIncome) IncomeEmerald else ExpenseCoral,
    onClick: (() -> Unit)? = null,
    onDuplicateClick: (() -> Unit)? = null,
    onEditClick: (() -> Unit)? = null,
    onDeleteClick: (() -> Unit)? = null,
    duplicateTestTag: String? = null,
    deleteTestTag: String? = null,
    editTestTag: String? = null
) {
    FinTrackCard(
        modifier = modifier.fillMaxWidth(),
        containerColor = SurfaceContainerDark,
        shape = RoundedCornerShape(RadiusLarge),
        contentPadding = Space12,
        onClick = onClick
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Category Icon Container (40dp circle, tonal semantic container)
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            color = if (isIncome) IncomeContainer else ExpenseContainer,
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = categoryIcon,
                        contentDescription = null,
                        tint = categoryColor,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(Space12))

                // Description & Structured Metadata
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = description,
                        style = CardTitleAmount,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(Space2))

                    // Secondary hierarchy: Date · Account · Category
                    val metadataParts = buildList {
                        if (dateFormatted.isNotBlank()) add(dateFormatted)
                        if (!accountName.isNullOrBlank()) add(accountName)
                        if (categoryName.isNotBlank()) add(categoryName)
                    }

                    Text(
                        text = metadataParts.joinToString("  •  "),
                        style = MicroMetadata,
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    if (statusLabel != null) {
                        Spacer(modifier = Modifier.height(Space4))
                        FinTrackStatusBadge(
                            label = statusLabel,
                            variant = statusVariant ?: BadgeVariant.WARNING
                        )
                    }
                }

                Spacer(modifier = Modifier.width(Space8))

                // Financial Amount presentation with tabular numerals and explicit sign
                FinTrackAmount(
                    amountPrimary = amountPrimaryFormatted,
                    currencyPrimary = primaryCurrency,
                    amountSecondary = amountSecondaryFormatted,
                    currencySecondary = secondaryCurrency,
                    type = if (isIncome) AmountSemanticType.INCOME else AmountSemanticType.EXPENSE,
                    showSign = true,
                    textStyle = CardTitleAmount,
                    secondaryTextStyle = MicroMetadata,
                    horizontalAlignment = Alignment.End
                )
            }

            // Quick actions row (Edit / Duplicate / Delete)
            if (onDuplicateClick != null || onEditClick != null || onDeleteClick != null) {
                Spacer(modifier = Modifier.height(Space4))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (onDuplicateClick != null) {
                        IconButton(
                            onClick = onDuplicateClick,
                            modifier = Modifier
                                .size(36.dp)
                                .then(if (duplicateTestTag != null) Modifier.testTag(duplicateTestTag) else Modifier)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Duplicate transaction",
                                tint = TextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    if (onEditClick != null) {
                        Spacer(modifier = Modifier.width(Space4))
                        IconButton(
                            onClick = onEditClick,
                            modifier = Modifier
                                .size(36.dp)
                                .then(if (editTestTag != null) Modifier.testTag(editTestTag) else Modifier)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit transaction",
                                tint = TextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    if (onDeleteClick != null) {
                        Spacer(modifier = Modifier.width(Space4))
                        IconButton(
                            onClick = onDeleteClick,
                            modifier = Modifier
                                .size(36.dp)
                                .then(if (deleteTestTag != null) Modifier.testTag(deleteTestTag) else Modifier)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete transaction",
                                tint = ExpenseCoral,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

