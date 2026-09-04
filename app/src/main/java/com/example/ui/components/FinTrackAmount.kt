package com.example.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import com.example.ui.theme.CardTitleAmount
import com.example.ui.theme.ExpenseCoral
import com.example.ui.theme.IncomeEmerald
import com.example.ui.theme.MicroMetadata
import com.example.ui.theme.Space4
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

enum class AmountSemanticType {
    INCOME,
    EXPENSE,
    NEUTRAL
}

/**
 * Reusable financial amount presentation component for FinTrack Design System v1.
 * Pure presentation: receives pre-formatted or raw numeric strings and renders them with
 * tabular numerals, semantic coloring, and TalkBack accessibility descriptions.
 */
@Composable
fun FinTrackAmount(
    amountPrimary: String,
    modifier: Modifier = Modifier,
    currencyPrimary: String = "RON",
    amountSecondary: String? = null,
    currencySecondary: String? = "EUR",
    type: AmountSemanticType = AmountSemanticType.NEUTRAL,
    showSign: Boolean = true,
    textStyle: TextStyle = CardTitleAmount,
    secondaryTextStyle: TextStyle = MicroMetadata,
    horizontalAlignment: Alignment.Horizontal = Alignment.Start
) {
    val (sign, color, typeDesc) = when (type) {
        AmountSemanticType.INCOME -> Triple(if (showSign) "+ " else "", IncomeEmerald, "Income")
        AmountSemanticType.EXPENSE -> Triple(if (showSign) "- " else "", ExpenseCoral, "Expense")
        AmountSemanticType.NEUTRAL -> Triple("", TextPrimary, "Amount")
    }

    val fullPrimary = "$sign$amountPrimary $currencyPrimary"
    val fullSecondary = if (amountSecondary != null && currencySecondary != null) {
        "≈ $amountSecondary $currencySecondary"
    } else null

    val accessibleDescription = buildString {
        append("$typeDesc: $fullPrimary")
        if (fullSecondary != null) {
            append(", converted $fullSecondary")
        }
    }

    Column(
        modifier = modifier.semantics { contentDescription = accessibleDescription },
        horizontalAlignment = horizontalAlignment
    ) {
        Text(
            text = fullPrimary,
            style = textStyle,
            color = color
        )
        if (fullSecondary != null) {
            Text(
                text = fullSecondary,
                style = secondaryTextStyle,
                color = TextSecondary
            )
        }
    }
}
