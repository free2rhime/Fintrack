package com.example.ui.components

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.ui.theme.CardTitleAmount
import com.example.ui.theme.MicroMetadata
import com.example.ui.theme.RadiusLarge
import com.example.ui.theme.RadiusSmall
import com.example.ui.theme.Space12
import com.example.ui.theme.Space16
import com.example.ui.theme.Space2
import com.example.ui.theme.Space4
import com.example.ui.theme.Space8
import com.example.ui.theme.SurfaceContainerDark
import com.example.ui.theme.SurfaceContainerHighDark
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

/**
 * Reusable presentation row for transactions in FinTrack Design System v1.
 * Establishes the clean presentation structure for future phase migration without
 * altering existing TransactionCardItem action pathways or test tags.
 */
@Composable
fun FinTrackTransactionRow(
    description: String,
    categoryName: String,
    dateFormatted: String,
    amountPrimaryFormatted: String,
    isIncome: Boolean,
    modifier: Modifier = Modifier,
    amountSecondaryFormatted: String? = null,
    primaryCurrency: String = "RON",
    secondaryCurrency: String = "EUR",
    categoryIcon: ImageVector = Icons.Default.Receipt,
    categoryColor: Color = TextSecondary,
    onClick: (() -> Unit)? = null
) {
    FinTrackCard(
        modifier = modifier.fillMaxWidth(),
        containerColor = SurfaceContainerDark,
        shape = RoundedCornerShape(RadiusLarge),
        contentPadding = Space12,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Category Icon Container
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(categoryColor.copy(alpha = 0.15f), CircleShape),
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

            // Description & Metadata
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = description,
                    style = CardTitleAmount,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(Space2))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = categoryName,
                        style = MicroMetadata,
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.width(Space4))
                    Text(
                        text = "•",
                        style = MicroMetadata,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.width(Space4))
                    Text(
                        text = dateFormatted,
                        style = MicroMetadata,
                        color = TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.width(Space8))

            // Amounts
            FinTrackAmount(
                amountPrimary = amountPrimaryFormatted,
                currencyPrimary = primaryCurrency,
                amountSecondary = amountSecondaryFormatted,
                currencySecondary = secondaryCurrency,
                type = if (isIncome) AmountSemanticType.INCOME else AmountSemanticType.EXPENSE,
                showSign = true,
                horizontalAlignment = Alignment.End
            )
        }
    }
}
