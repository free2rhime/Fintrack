package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.theme.CobaltBlue
import com.example.ui.theme.FinTrackMotion
import com.example.ui.theme.LabelBadgeMedium
import com.example.ui.theme.RadiusMedium
import com.example.ui.theme.RadiusSmall
import com.example.ui.theme.Space2
import com.example.ui.theme.Space4
import com.example.ui.theme.SurfaceContainerDark
import com.example.ui.theme.TextSecondary

/**
 * Reusable currency selector for FinTrack Design System v1.
 * Supports RON and EUR with 200ms pill transition, CobaltBlue selection,
 * 48dp minimum touch target, and accessible selected semantics.
 */
@Composable
fun FinTrackCurrencySelector(
    selectedCurrency: String,
    onCurrencyChanged: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(RadiusMedium))
            .background(SurfaceContainerDark)
            .padding(Space4),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val currencies = listOf("RON", "EUR")
        currencies.forEach { curr ->
            val isSelected = curr == selectedCurrency
            val bgBgColor by animateColorAsState(
                targetValue = if (isSelected) CobaltBlue else Color.Transparent,
                animationSpec = FinTrackMotion.standardTween(),
                label = "currency_selector_bg"
            )
            val textColor by animateColorAsState(
                targetValue = if (isSelected) Color.White else TextSecondary,
                animationSpec = FinTrackMotion.standardTween(),
                label = "currency_selector_text"
            )

            Box(
                modifier = Modifier
                    .defaultMinSize(minWidth = 56.dp, minHeight = 48.dp)
                    .clip(RoundedCornerShape(RadiusSmall))
                    .background(bgBgColor)
                    .clickable(role = Role.Tab) { onCurrencyChanged(curr) }
                    .semantics {
                        this.selected = isSelected
                        this.role = Role.Tab
                    }
                    .padding(horizontal = 14.dp, vertical = Space2)
                    .testTag("currency_toggle_$curr"),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = curr,
                    style = LabelBadgeMedium,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = textColor
                )
            }
        }
    }
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
