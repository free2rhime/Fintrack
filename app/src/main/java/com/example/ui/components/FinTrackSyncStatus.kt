package com.example.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.GroupOff
import androidx.compose.material.icons.filled.Sync
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.data.repository.SyncStatus

/**
 * Reusable presentation component for SyncStatus in FinTrack Design System v1.
 * Pure presentation layer mapping each stable repository state to semantic tokens.
 * Completely decoupled from synchronization business logic, OutboundSyncEngine, and Room/Firestore.
 */
@Composable
fun FinTrackSyncStatus(
    syncStatus: SyncStatus,
    modifier: Modifier = Modifier,
    showPrefix: Boolean = true
) {
    val (statusLabel, badgeVariant, statusIcon) = when (syncStatus) {
        is SyncStatus.SignedOut -> Triple(
            "Signed out",
            BadgeVariant.NEUTRAL,
            Icons.Default.CloudOff
        )
        is SyncStatus.NoHousehold -> Triple(
            "No active household",
            BadgeVariant.WARNING,
            Icons.Default.GroupOff
        )
        is SyncStatus.Connecting -> Triple(
            "Syncing...",
            BadgeVariant.SYNCING,
            Icons.Default.Sync
        )
        is SyncStatus.Synced -> Triple(
            "Synced",
            BadgeVariant.SUCCESS,
            Icons.Default.CloudDone
        )
        is SyncStatus.PermissionDenied -> Triple(
            "Permission denied",
            BadgeVariant.ERROR,
            Icons.Default.ErrorOutline
        )
        is SyncStatus.Offline -> Triple(
            "Offline",
            BadgeVariant.WARNING,
            Icons.Default.CloudQueue
        )
    }

    val displayLabel = if (showPrefix) "Sync: $statusLabel" else statusLabel

    FinTrackStatusBadge(
        label = displayLabel,
        variant = badgeVariant,
        icon = statusIcon,
        modifier = modifier
    )
}
