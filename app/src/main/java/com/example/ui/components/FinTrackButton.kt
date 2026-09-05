package com.example.ui.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.example.ui.theme.ExpenseCoral
import com.example.ui.theme.IncomeEmerald
import com.example.ui.theme.LabelBadgeMedium
import com.example.ui.theme.RadiusMedium
import com.example.ui.theme.Space12
import com.example.ui.theme.Space16
import com.example.ui.theme.Space8
import com.example.ui.theme.SurfaceContainerHighDark
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary

enum class ButtonVariant {
    PRIMARY,
    SECONDARY,
    DESTRUCTIVE
}

/**
 * Unified button primitive for FinTrack Design System v1.
 * Guarantees a minimum 48dp interactive touch target, strict M3 semantics,
 * and standard corner radii.
 */
@Composable
fun FinTrackButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: ButtonVariant = ButtonVariant.PRIMARY,
    enabled: Boolean = true,
    shape: Shape = RoundedCornerShape(RadiusMedium),
    leadingIcon: (@Composable () -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null,
    contentPadding: PaddingValues = PaddingValues(horizontal = Space16, vertical = Space12),
    content: @Composable RowScope.() -> Unit
) {
    val (containerColor, contentColor, disabledContainerColor, disabledContentColor) = when (variant) {
        ButtonVariant.PRIMARY -> ButtonColorsSet(
            containerColor = IncomeEmerald,
            contentColor = Color.White,
            disabledContainerColor = IncomeEmerald.copy(alpha = 0.38f),
            disabledContentColor = Color.White.copy(alpha = 0.38f)
        )
        ButtonVariant.SECONDARY -> ButtonColorsSet(
            containerColor = SurfaceContainerHighDark,
            contentColor = TextPrimary,
            disabledContainerColor = SurfaceContainerHighDark.copy(alpha = 0.38f),
            disabledContentColor = TextMuted
        )
        ButtonVariant.DESTRUCTIVE -> ButtonColorsSet(
            containerColor = ExpenseCoral,
            contentColor = Color.White,
            disabledContainerColor = ExpenseCoral.copy(alpha = 0.38f),
            disabledContentColor = Color.White.copy(alpha = 0.38f)
        )
    }

    Button(
        onClick = onClick,
        modifier = modifier.defaultMinSize(minHeight = 48.dp),
        enabled = enabled,
        shape = shape,
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor,
            disabledContainerColor = disabledContainerColor,
            disabledContentColor = disabledContentColor
        ),
        contentPadding = contentPadding
    ) {
        if (leadingIcon != null) {
            leadingIcon()
            Spacer(modifier = Modifier.width(Space8))
        }
        content()
        if (trailingIcon != null) {
            Spacer(modifier = Modifier.width(Space8))
            trailingIcon()
        }
    }
}

/**
 * Text convenience overload for FinTrackButton.
 */
@Composable
fun FinTrackButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: ButtonVariant = ButtonVariant.PRIMARY,
    enabled: Boolean = true,
    leadingIcon: (@Composable () -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null
) {
    FinTrackButton(
        onClick = onClick,
        modifier = modifier,
        variant = variant,
        enabled = enabled,
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon
    ) {
        Text(
            text = text,
            style = LabelBadgeMedium
        )
    }
}

private data class ButtonColorsSet(
    val containerColor: Color,
    val contentColor: Color,
    val disabledContainerColor: Color,
    val disabledContentColor: Color
)
