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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.TransactionEntity
import com.example.data.util.NumberFormatter
import com.example.ui.theme.BodyRegular
import com.example.ui.theme.CardTitleAmount
import com.example.ui.theme.FinTrackMotion
import com.example.ui.theme.FinTrackTheme
import com.example.ui.theme.LabelBadgeMedium
import com.example.ui.theme.MicroMetadata
import com.example.ui.theme.SectionHeadline
import com.example.ui.theme.ShapeGroupedContainer
import com.example.ui.theme.ShapeGroupedItemBottom
import com.example.ui.theme.ShapeGroupedItemMiddle
import com.example.ui.theme.ShapeGroupedItemSingle
import com.example.ui.theme.ShapeGroupedItemTop
import com.example.ui.theme.Space12
import com.example.ui.theme.Space16
import com.example.ui.theme.Space2
import com.example.ui.theme.Space24
import com.example.ui.theme.Space4
import com.example.ui.theme.Space8
import com.example.ui.theme.isReducedMotionEnabled

/**
 * RecentActivitySection — Material 3 Expressive Dashboard preview stream.
 *
 * Replaces independent 16dp cards with a continuous tonal container (ShapeGroupedContainer)
 * and tactile, differentiated row geometry with subtle inset dividers.
 *
 * Preserved invariants:
 * 1. TestTags: recent_activity_section, view_all_activity_button, recent_activity_empty_state,
 *    recent_tx_item_${id}, recent_tx_desc_${id}, recent_tx_amount_${id}
 * 2. Currency parity (RON and EUR with official/unverified status handling)
 * 3. Exact 3-item preview constraint
 * 4. TalkBack accessibility semantics & touch targets >= 48dp
 */
@Composable
fun RecentActivitySection(
    transactions: List<TransactionEntity>,
    selectedCurrency: String,
    onViewAllClicked: () -> Unit,
    onTransactionClicked: (TransactionEntity) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val previewTransactions = transactions.take(3)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("recent_activity_section")
    ) {
        // Section Header Row: Title + "View All Activity" Action Link
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = Space8),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Recent Activity",
                style = SectionHeadline,
                color = MaterialTheme.colorScheme.onSurface
            )

            TextButton(
                onClick = onViewAllClicked,
                modifier = Modifier
                    .defaultMinSize(minHeight = 48.dp)
                    .testTag("view_all_activity_button")
                    .semantics {
                        contentDescription = "View All Activity"
                        role = Role.Button
                    }
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Space4)
                ) {
                    Text(
                        text = "View All",
                        style = LabelBadgeMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = FinTrackTheme.colors.brandAccent
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = FinTrackTheme.colors.brandAccent,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        // Continuous Tonal Surface (M3 Expressive grouped list)
        Surface(
            shape = ShapeGroupedContainer,
            color = MaterialTheme.colorScheme.surfaceContainer,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (previewTransactions.isEmpty()) {
                // Calm, quiet empty state within continuous surface
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("recent_activity_empty_state")
                        .padding(Space24),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(FinTrackTheme.colors.surfaceSecondary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.ReceiptLong,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(Space8))
                        Text(
                            text = "No Recent Activity",
                            style = CardTitleAmount,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(Space4))
                        Text(
                            text = "Transactions will appear here as you record them.",
                            style = MicroMetadata,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                // Stream of transactions rendered as grouped items with inset hairline dividers
                Column(modifier = Modifier.fillMaxWidth()) {
                    previewTransactions.forEachIndexed { index, tx ->
                        val itemShape = when {
                            previewTransactions.size == 1 -> ShapeGroupedItemSingle
                            index == 0 -> ShapeGroupedItemTop
                            index == previewTransactions.size - 1 -> ShapeGroupedItemBottom
                            else -> ShapeGroupedItemMiddle
                        }

                        RecentTransactionPreviewItem(
                            transaction = tx,
                            selectedCurrency = selectedCurrency,
                            onClick = { onTransactionClicked(tx) },
                            shape = itemShape,
                            showDivider = index < previewTransactions.size - 1
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RecentTransactionPreviewItem(
    transaction: TransactionEntity,
    selectedCurrency: String,
    onClick: () -> Unit,
    shape: Shape,
    showDivider: Boolean,
    modifier: Modifier = Modifier
) {
    val isIncome = transaction.type == "Income"
    val useRon = selectedCurrency == "RON"

    val rawAmount = if (useRon) transaction.amountRON else transaction.amountEUR
    val formattedAmount = NumberFormatter.formatAmount(rawAmount)
    val signedAmountStr = if (isIncome) "+$formattedAmount $selectedCurrency" else "-$formattedAmount $selectedCurrency"

    val categoryIcon = resolveCategoryIcon(transaction.category, isIncome)
    val categoryColor = if (isIncome) FinTrackTheme.colors.income else FinTrackTheme.colors.expense
    val containerColor = if (isIncome) FinTrackTheme.colors.incomeContainer else FinTrackTheme.colors.expenseContainer

    val titleText = transaction.description.ifBlank { transaction.category }
    val subtitleText = if (transaction.description.isNotBlank()) {
        "${transaction.category} • ${transaction.date}"
    } else {
        transaction.date
    }

    val accessibleDesc = "${if (isIncome) "Income" else "Expense"}: $titleText, $signedAmountStr, ${transaction.date}"

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val isReducedMotion = isReducedMotionEnabled()

    val scale by animateFloatAsState(
        targetValue = if (isPressed && !isReducedMotion) FinTrackMotion.PressScaleTarget else 1.0f,
        animationSpec = if (isReducedMotion) snap() else FinTrackMotion.pressInteractionSpec(),
        label = "recent_tx_scale"
    )

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 56.dp)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }
                .clip(shape)
                .clickable(
                    interactionSource = interactionSource,
                    indication = ripple(),
                    onClick = onClick
                )
                .semantics {
                    contentDescription = accessibleDesc
                    role = Role.Button
                }
                .testTag("recent_tx_item_${transaction.id}")
                .padding(horizontal = Space16, vertical = Space12),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Category Visual Anchor (40dp organic squircle)
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(containerColor),
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

            // Transaction Info: Title & Subtitle
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = Space8)
            ) {
                Text(
                    text = titleText,
                    style = BodyRegular,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.testTag("recent_tx_desc_${transaction.id}")
                )
                Spacer(modifier = Modifier.height(Space2))
                Text(
                    text = subtitleText,
                    style = MicroMetadata,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Signed Amount & Optional Status
            Column(
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = signedAmountStr,
                    style = CardTitleAmount,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isIncome) FinTrackTheme.colors.income else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    modifier = Modifier.testTag("recent_tx_amount_${transaction.id}")
                )

                // If in EUR and conversion is unverified or pending, render concise micro status
                if (!useRon && transaction.conversionStatus != "OFFICIAL") {
                    val statusText = when {
                        transaction.conversionStatus?.startsWith("PENDING") == true -> "Pending"
                        transaction.conversionStatus?.startsWith("FAILED") == true -> "Failed"
                        else -> "Unverified"
                    }
                    Text(
                        text = statusText,
                        style = MicroMetadata,
                        color = FinTrackTheme.colors.healthWarning
                    )
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
