package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import com.example.ui.theme.CardTitleAmount
import com.example.ui.theme.FinTrackMotion
import com.example.ui.theme.FinTrackTheme
import com.example.ui.theme.MicroMetadata
import com.example.ui.theme.isReducedMotionEnabled

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
        AmountSemanticType.INCOME -> Triple(if (showSign) "+ " else "", FinTrackTheme.colors.income, "Income")
        AmountSemanticType.EXPENSE -> Triple(if (showSign) "- " else "", FinTrackTheme.colors.expense, "Expense")
        AmountSemanticType.NEUTRAL -> Triple("", MaterialTheme.colorScheme.onSurface, "Amount")
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

    val isReducedMotion = isReducedMotionEnabled()

    Column(
        modifier = modifier.semantics(mergeDescendants = true) { contentDescription = accessibleDescription },
        horizontalAlignment = horizontalAlignment
    ) {
        AnimatedContent(
            targetState = fullPrimary,
            transitionSpec = {
                if (isReducedMotion) {
                    ContentTransform(EnterTransition.None, ExitTransition.None)
                } else {
                    FinTrackMotion.contentFade()
                }
            },
            label = "amount_primary_anim"
        ) { text ->
            Text(
                text = text,
                style = textStyle,
                color = color
            )
        }
        if (fullSecondary != null) {
            AnimatedContent(
                targetState = fullSecondary,
                transitionSpec = {
                    if (isReducedMotion) {
                        ContentTransform(EnterTransition.None, ExitTransition.None)
                    } else {
                        FinTrackMotion.contentFade()
                    }
                },
                label = "amount_secondary_anim"
            ) { text ->
                Text(
                    text = text,
                    style = secondaryTextStyle,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
