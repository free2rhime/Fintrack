package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.ui.theme.CardTitleAmount
import com.example.ui.theme.ExpenseContainer
import com.example.ui.theme.ExpenseCoral
import com.example.ui.theme.FinTrackMotion
import com.example.ui.theme.IncomeContainer
import com.example.ui.theme.IncomeEmerald
import com.example.ui.theme.MicroMetadata
import com.example.ui.theme.ShapeGroupedItemSingle
import com.example.ui.theme.Space12
import com.example.ui.theme.Space16
import com.example.ui.theme.Space2
import com.example.ui.theme.Space4
import com.example.ui.theme.Space8
import com.example.ui.theme.isReducedMotionEnabled

/**
 * Reusable presentation row for transactions in FinTrack Material 3 Expressive Design System.
 * Displays transaction details with grouped geometry, semantic color tokens, tabular numerals,
 * tactile press feedback, and TalkBack accessibility.
 */
@Composable
fun FinTrackTransactionRow(
    description: String,
    categoryName: String,
    dateFormatted: String = "",
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
    shape: Shape = ShapeGroupedItemSingle,
    containerColor: Color = Color.Transparent,
    showDivider: Boolean = false,
    onClick: (() -> Unit)? = null,
    onDuplicateClick: (() -> Unit)? = null,
    onEditClick: (() -> Unit)? = null,
    onDeleteClick: (() -> Unit)? = null,
    duplicateTestTag: String? = null,
    deleteTestTag: String? = null,
    editTestTag: String? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val isReducedMotion = isReducedMotionEnabled()

    val scale by animateFloatAsState(
        targetValue = if (isPressed && !isReducedMotion && onClick != null) FinTrackMotion.PressScaleTarget else 1.0f,
        animationSpec = if (isReducedMotion) snap() else FinTrackMotion.pressInteractionSpec(),
        label = "tx_row_scale"
    )

    val accessibleDesc = "${if (isIncome) "Income" else "Expense"}: $description, ${if (isIncome) "+" else "-"}$amountPrimaryFormatted $primaryCurrency, $categoryName, ${if (dateFormatted.isNotBlank()) dateFormatted else ""}"

    Column(modifier = modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }
                .clip(shape)
                .background(containerColor)
                .then(
                    if (onClick != null) {
                        Modifier.clickable(
                            interactionSource = interactionSource,
                            indication = ripple(),
                            onClick = onClick
                        )
                    } else Modifier
                )
                .semantics {
                    contentDescription = accessibleDesc
                    if (onClick != null) {
                        role = Role.Button
                    }
                }
                .padding(horizontal = Space16, vertical = Space12)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 44.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Category Icon Container (40dp organic squircle, tonal semantic container)
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                color = if (isIncome) IncomeContainer else ExpenseContainer
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
                            color = MaterialTheme.colorScheme.onSurface,
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
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                                    .size(48.dp)
                                    .then(if (duplicateTestTag != null) Modifier.testTag(duplicateTestTag) else Modifier)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = "Duplicate transaction",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        if (onEditClick != null) {
                            Spacer(modifier = Modifier.width(Space4))
                            IconButton(
                                onClick = onEditClick,
                                modifier = Modifier
                                    .size(48.dp)
                                    .then(if (editTestTag != null) Modifier.testTag(editTestTag) else Modifier)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Edit transaction",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        if (onDeleteClick != null) {
                            Spacer(modifier = Modifier.width(Space4))
                            IconButton(
                                onClick = onDeleteClick,
                                modifier = Modifier
                                    .size(48.dp)
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

        if (showDivider) {
            HorizontalDivider(
                modifier = Modifier.padding(start = 68.dp, end = Space16),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                thickness = 0.5.dp
            )
        }
    }
}

