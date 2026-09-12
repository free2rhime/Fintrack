package com.example.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback

/**
 * Reusable currency selector for FinTrack Design System.
 * Delegates to FinTrackSegmentedControl with RON and EUR, providing smooth pill transition,
 * primary selection, spring-based tactile press feedback, haptic feedback on change,
 * 48dp minimum touch target, selectableGroup container, and accessible selected semantics.
 */
@Composable
fun FinTrackCurrencySelector(
    selectedCurrency: String,
    onCurrencyChanged: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val currencies = listOf("RON", "EUR")
    val selectedIndex = currencies.indexOf(selectedCurrency).let { if (it >= 0) it else 0 }

    FinTrackSegmentedControl(
        items = currencies,
        selectedIndex = selectedIndex,
        onItemSelected = { index ->
            val curr = currencies[index]
            if (curr != selectedCurrency) {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            }
            onCurrencyChanged(curr)
        },
        modifier = modifier,
        fillMaxWidth = false,
        itemTestTag = { _, curr -> "currency_toggle_$curr" }
    )
}

/**
 * Backward-compatibility wrapper for existing screens and callers.
 */
@Composable
fun CurrencyToggle(
    selectedCurrency: String,
    onCurrencyChanged: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    FinTrackCurrencySelector(
        selectedCurrency = selectedCurrency,
        onCurrencyChanged = onCurrencyChanged,
        modifier = modifier
    )
}
