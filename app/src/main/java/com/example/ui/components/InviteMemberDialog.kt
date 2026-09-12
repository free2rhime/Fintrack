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
import androidx.compose.material.icons.filled.Email
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.ui.theme.BodyRegular
import com.example.ui.theme.LabelBadgeMedium
import com.example.ui.theme.MicroMetadata
import com.example.ui.theme.RadiusMedium
import com.example.ui.theme.RadiusXLarge
import com.example.ui.theme.SectionHeadline
import com.example.ui.theme.Space16
import com.example.ui.theme.Space8

@Composable
fun InviteMemberDialog(
    isLoading: Boolean = false,
    errorMessage: String? = null,
    onSendInvite: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var localError by remember { mutableStateOf<String?>(null) }

    fun validateAndSubmit() {
        val trimmed = email.trim()
        if (trimmed.isBlank()) {
            localError = "Email address cannot be empty"
            return
        }
        val emailPattern = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
        if (!trimmed.matches(Regex(emailPattern))) {
            localError = "Please enter a valid email address"
            return
        }
        localError = null
        onSendInvite(trimmed)
    }

    AlertDialog(
        onDismissRequest = {
            if (!isLoading) onDismiss()
        },
        title = {
            Text(
                text = "Invite Household Member",
                style = SectionHeadline,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.semantics { heading() }
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Enter the email of the person you want to invite to your household.",
                    style = BodyRegular,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(Space16))

                OutlinedTextField(
                    value = email,
                    onValueChange = {
                        email = it
                        if (localError != null) localError = null
                    },
                    label = { Text("Invitee Email", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                    placeholder = { Text("partner@example.com", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Email,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    singleLine = true,
                    isError = localError != null || errorMessage != null,
                    supportingText = {
                        val displayErr = localError ?: errorMessage
                        if (displayErr != null) {
                            Text(
                                text = displayErr,
                                color = MaterialTheme.colorScheme.error,
                                style = MicroMetadata
                            )
                        }
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Send
                    ),
                    keyboardActions = KeyboardActions(
                        onSend = { validateAndSubmit() }
                    ),
                    enabled = !isLoading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("invite_email_input"),
                    shape = RoundedCornerShape(RadiusMedium),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                        errorBorderColor = MaterialTheme.colorScheme.error,
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer
                    )
                )
            }
        },
        confirmButton = {
            FinTrackButton(
                onClick = { validateAndSubmit() },
                enabled = !isLoading,
                modifier = Modifier.testTag("send_invite_button"),
                variant = ButtonVariant.PRIMARY
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Send Invite", style = LabelBadgeMedium, fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            FinTrackButton(
                text = "Cancel",
                onClick = onDismiss,
                enabled = !isLoading,
                modifier = Modifier.testTag("cancel_invite_button"),
                variant = ButtonVariant.SECONDARY
            )
        },
        modifier = Modifier.testTag("invite_member_dialog"),
        shape = RoundedCornerShape(RadiusXLarge),
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        titleContentColor = MaterialTheme.colorScheme.onSurface,
        textContentColor = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

