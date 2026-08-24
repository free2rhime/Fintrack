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
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp

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
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Enter a name for your new household to enable cloud synchronization of your financial data.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        if (localError != null) localError = null
                    },
                    label = { Text("Household Name") },
                    placeholder = { Text("e.g. My Family Budget") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Home,
                            contentDescription = null
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
                    shape = RoundedCornerShape(12.dp)
                )

                val displayErr = localError ?: errorMessage
                if (displayErr != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = displayErr,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.testTag("create_household_error_text")
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { validateAndSubmit() },
                enabled = !isCreating,
                modifier = Modifier.testTag("confirm_create_household_button"),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (isCreating) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Create", fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isCreating,
                modifier = Modifier.testTag("cancel_create_household_button")
            ) {
                Text("Cancel")
            }
        },
        modifier = Modifier.testTag("create_household_dialog"),
        shape = RoundedCornerShape(20.dp)
    )
}
