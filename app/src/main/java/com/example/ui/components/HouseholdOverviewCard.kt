package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.HouseholdDto
import com.example.data.model.HouseholdMemberDto

@Composable
fun HouseholdOverviewCard(
    household: HouseholdDto,
    currentUserMembership: HouseholdMemberDto?,
    householdMembers: List<HouseholdMemberDto>,
    currentUid: String? = null,
    onInviteMemberClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val sortedMembers = remember(householdMembers) {
        householdMembers.sortedWith(
            compareBy<HouseholdMemberDto> { member ->
                when (member.role?.trim()?.uppercase()) {
                    "OWNER" -> 0
                    "ADMIN" -> 1
                    "MEMBER" -> 2
                    else -> 3
                }
            }.thenBy { member ->
                member.displayName ?: member.email ?: member.uid ?: ""
            }
        )
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("household_summary_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row: Household Name and Current User Role
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    Icon(
                        imageVector = Icons.Default.Home,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = household.name ?: "Active Household",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.testTag("household_name_text")
                    )
                }

                val roleText = currentUserMembership?.role?.trim()?.uppercase() ?: "MEMBER"
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = roleText,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                            .testTag("household_user_role_text")
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Household ID: ${household.householdId ?: "Unknown"}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.testTag("household_id_text")
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Members: ${householdMembers.size}",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.testTag("household_member_count_text")
            )

            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                thickness = 1.dp
            )
            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Household Members",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (sortedMembers.isEmpty()) {
                Text(
                    text = "No household members found",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            } else {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    sortedMembers.forEach { member ->
                        val isCurrentUser = (currentUid != null && member.uid == currentUid) ||
                            (currentUserMembership?.uid != null && member.uid == currentUserMembership.uid)

                        val rawName = member.displayName?.takeIf { it.isNotBlank() }
                            ?: member.email?.takeIf { it.isNotBlank() }
                            ?: member.uid?.takeIf { it.isNotBlank() }
                            ?: "Unknown Member"

                        val memberDisplayName = if (isCurrentUser) "$rawName (You)" else rawName
                        val roleBadge = member.role?.trim()?.uppercase() ?: "MEMBER"
                        val statusBadge = member.status?.trim()?.uppercase() ?: "ACTIVE"

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surface,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("household_member_row")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f, fill = false)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (isCurrentUser) MaterialTheme.colorScheme.primaryContainer
                                                else MaterialTheme.colorScheme.surfaceVariant
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Person,
                                            contentDescription = null,
                                            tint = if (isCurrentUser) MaterialTheme.colorScheme.onPrimaryContainer
                                            else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(10.dp))

                                    Column {
                                        Text(
                                            text = memberDisplayName,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = if (isCurrentUser) FontWeight.Bold else FontWeight.Medium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        if (member.displayName?.isNotBlank() == true && member.email?.isNotBlank() == true) {
                                            Text(
                                                text = member.email!!,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }

                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Status Badge (ACTIVE / PENDING)
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = if (statusBadge == "ACTIVE") {
                                            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f)
                                        } else {
                                            MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.7f)
                                        }
                                    ) {
                                        Text(
                                            text = statusBadge,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (statusBadge == "ACTIVE") {
                                                MaterialTheme.colorScheme.onSecondaryContainer
                                            } else {
                                                MaterialTheme.colorScheme.onTertiaryContainer
                                            },
                                            modifier = Modifier
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                                .testTag("household_member_status")
                                        )
                                    }

                                    // Role Badge (OWNER / ADMIN / MEMBER)
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = when (roleBadge) {
                                            "OWNER" -> MaterialTheme.colorScheme.primaryContainer
                                            "ADMIN" -> MaterialTheme.colorScheme.tertiaryContainer
                                            else -> MaterialTheme.colorScheme.surfaceVariant
                                        }
                                    ) {
                                        Text(
                                            text = roleBadge,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = when (roleBadge) {
                                                "OWNER" -> MaterialTheme.colorScheme.onPrimaryContainer
                                                "ADMIN" -> MaterialTheme.colorScheme.onTertiaryContainer
                                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                                            },
                                            modifier = Modifier
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                                .testTag("household_member_role")
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            val isOwner = currentUserMembership?.role?.trim()?.uppercase() == "OWNER"
            if (isOwner) {
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onInviteMemberClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("invite_member_button"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Invite Member",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
