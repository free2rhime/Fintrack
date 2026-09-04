package com.example.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.example.ui.theme.BodyRegular
import com.example.ui.theme.CobaltBlue
import com.example.ui.theme.ExpenseCoral
import com.example.ui.theme.LabelBadgeMedium
import com.example.ui.theme.MicroMetadata
import com.example.ui.theme.RadiusMedium
import com.example.ui.theme.RadiusXLarge
import com.example.ui.theme.SectionHeadline
import com.example.ui.theme.Space16
import com.example.ui.theme.Space8
import com.example.ui.theme.SurfaceContainerDark
import com.example.ui.theme.SurfaceContainerHighDark
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun CreateHouseholdDialog(
    isCreating: Boolean = false,
    errorMessage: String? = null,
    onCreateHousehold: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var localError by remember { mutableStateOf<String?>(null) }

    fun validateAndSubmit() {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) {
            localError = "Household name cannot be empty"
            return
        }
        if (trimmed.length !in 2..50) {
            localError = "Household name must be between 2 and 50 characters"
            return
        }
        localError = null
        onCreateHousehold(trimmed)
    }

    AlertDialog(
        onDismissRequest = {
            if (!isCreating) onDismiss()
        },
        title = {
            Text(
                text = "Create Household",
                style = SectionHeadline,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Enter a name for your new household to enable cloud synchronization of your financial data.",
                    style = BodyRegular,
                    color = TextSecondary
                )

                Spacer(modifier = Modifier.height(Space16))

                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        if (localError != null) localError = null
                    },
                    label = { Text("Household Name", color = TextSecondary) },
                    placeholder = { Text("e.g. My Family Budget", color = TextSecondary.copy(alpha = 0.6f)) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Home,
                            contentDescription = null,
                            tint = CobaltBlue
                        )
                    },
                    singleLine = true,
                    isError = localError != null || errorMessage != null,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = { validateAndSubmit() }
                    ),
                    enabled = !isCreating,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("household_name_input"),
                    shape = RoundedCornerShape(RadiusMedium),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CobaltBlue,
                        unfocusedBorderColor = SurfaceContainerHighDark,
                        errorBorderColor = ExpenseCoral,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedContainerColor = SurfaceContainerDark,
                        unfocusedContainerColor = SurfaceContainerDark
                    )
                )

                val displayErr = localError ?: errorMessage
                if (displayErr != null) {
                    Spacer(modifier = Modifier.height(Space8))
                    Text(
                        text = displayErr,
                        color = ExpenseCoral,
                        style = MicroMetadata,
                        modifier = Modifier.testTag("create_household_error_text")
                    )
                }
            }
        },
        confirmButton = {
            FinTrackButton(
                onClick = { validateAndSubmit() },
                enabled = !isCreating,
                modifier = Modifier.testTag("confirm_create_household_button"),
                variant = ButtonVariant.PRIMARY
            ) {
                if (isCreating) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = Color.White
                    )
                } else {
                    Text("Create", style = LabelBadgeMedium, fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            FinTrackButton(
                text = "Cancel",
                onClick = onDismiss,
                enabled = !isCreating,
                modifier = Modifier.testTag("cancel_create_household_button"),
                variant = ButtonVariant.SECONDARY
            )
        },
        modifier = Modifier.testTag("create_household_dialog"),
        shape = RoundedCornerShape(RadiusXLarge),
        containerColor = SurfaceDark,
        titleContentColor = TextPrimary,
        textContentColor = TextSecondary
    )
}

