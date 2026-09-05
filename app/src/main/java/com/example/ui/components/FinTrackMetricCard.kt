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
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.example.ui.theme.CardTitleAmount
import com.example.ui.theme.HeroFinancialDisplay
import com.example.ui.theme.LabelBadgeMedium
import com.example.ui.theme.MicroMetadata
import com.example.ui.theme.Space12
import com.example.ui.theme.Space16
import com.example.ui.theme.Space4
import com.example.ui.theme.Space8
import com.example.ui.theme.SurfaceContainerDark
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

/**
 * Reusable metric presentation component for FinTrack Design System v1.
 * Supports financial figures (income, expense, net balance, savings rates)
 * with strict tonal hierarchy and tabular numeral alignment.
 */
@Composable
fun FinTrackMetricCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier,
    supportingText: String? = null,
    icon: ImageVector? = null,
    iconTint: Color = TextSecondary,
    iconContainerColor: Color = SurfaceContainerDark,
    valueColor: Color = TextPrimary,
    isHeroDisplay: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    FinTrackCard(
        modifier = modifier.fillMaxWidth(),
        containerColor = SurfaceContainerDark,
        contentPadding = Space16,
        onClick = onClick
    ) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (icon != null) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(iconContainerColor, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = iconTint,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(Space8))
                }
                Text(
                    text = title,
                    style = LabelBadgeMedium,
                    color = TextSecondary
                )
            }

            Spacer(modifier = Modifier.height(Space8))

            Text(
                text = value,
                style = if (isHeroDisplay) HeroFinancialDisplay else CardTitleAmount,
                color = valueColor
            )

            if (supportingText != null) {
                Spacer(modifier = Modifier.height(Space4))
                Text(
                    text = supportingText,
                    style = MicroMetadata,
                    color = TextSecondary
                )
            }
        }
    }
}
