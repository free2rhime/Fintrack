package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.HouseholdDto
import com.example.data.model.HouseholdMemberDto
import com.example.ui.theme.BodyRegular
import com.example.ui.theme.CardTitleAmount
import com.example.ui.theme.FinTrackTheme
import com.example.ui.theme.LabelBadgeMedium
import com.example.ui.theme.MicroMetadata
import com.example.ui.theme.RadiusMedium
import com.example.ui.theme.RadiusSmall
import com.example.ui.theme.SectionHeadline
import com.example.ui.theme.ShapeGroupedContainer
import com.example.ui.theme.ShapeGroupedItemBottom
import com.example.ui.theme.ShapeGroupedItemMiddle
import com.example.ui.theme.ShapeGroupedItemSingle
import com.example.ui.theme.ShapeGroupedItemTop
import com.example.ui.theme.Space12
import com.example.ui.theme.Space16
import com.example.ui.theme.Space2
import com.example.ui.theme.Space4
import com.example.ui.theme.Space8
import com.example.ui.theme.TitleCard

/**
 * Material 3 Expressive Household Overview Surface.
 *
 * Visual representation of the active household, its member directory,
 * roles, permissions, and administrative invitations.
 */
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

    Surface(
        shape = ShapeGroupedContainer,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = modifier
            .fillMaxWidth()
            .testTag("household_summary_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Space16)
        ) {
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
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(RadiusMedium))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Home,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(Space12))
                    Column {
                        Text(
                            text = household.name ?: "Active Household",
                            style = CardTitleAmount,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .testTag("household_name_text")
                                .semantics { heading() }
                        )
                        Text(
                            text = "Household Synchronization",
                            style = MicroMetadata,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                val roleText = currentUserMembership?.role?.trim()?.uppercase() ?: "MEMBER"
                val (roleBg, roleFg) = when (roleText) {
                    "OWNER" -> FinTrackTheme.colors.incomeContainer to FinTrackTheme.colors.income
                    "ADMIN" -> MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) to MaterialTheme.colorScheme.primary
                    else -> MaterialTheme.colorScheme.surfaceContainerHigh to MaterialTheme.colorScheme.onSurfaceVariant
                }
                Surface(
                    shape = RoundedCornerShape(RadiusSmall),
                    color = roleBg
                ) {
                    Text(
                        text = roleText,
                        style = LabelBadgeMedium,
                        fontWeight = FontWeight.Bold,
                        color = roleFg,
                        modifier = Modifier
                            .padding(horizontal = Space8, vertical = Space4)
                            .testTag("household_user_role_text")
                    )
                }
            }

            Spacer(modifier = Modifier.height(Space16))

            // Metadata Row (Household ID & Member Count)
            Surface(
                shape = RoundedCornerShape(RadiusMedium),
                color = MaterialTheme.colorScheme.surfaceContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Space12, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Household ID: ${household.householdId ?: "Unknown"}",
                        style = MicroMetadata,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.testTag("household_id_text")
                    )

                    Text(
                        text = "Members: ${householdMembers.size}",
                        style = MicroMetadata,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.testTag("household_member_count_text")
                    )
                }
            }

            Spacer(modifier = Modifier.height(Space16))
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant,
                thickness = 1.dp
            )
            Spacer(modifier = Modifier.height(Space16))

            Text(
                text = "Household Members",
                style = TitleCard,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.semantics { heading() }
            )

            Spacer(modifier = Modifier.height(Space8))

            if (sortedMembers.isEmpty()) {
                Text(
                    text = "No household members found",
                    style = BodyRegular,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = Space8)
                )
            } else {
                Column(
                    verticalArrangement = Arrangement.spacedBy(Space2),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    sortedMembers.forEachIndexed { index, member ->
                        val isCurrentUser = (currentUid != null && member.uid == currentUid) ||
                            (currentUserMembership?.uid != null && member.uid == currentUserMembership.uid)

                        val rawName = member.displayName?.takeIf { it.isNotBlank() }
                            ?: member.email?.takeIf { it.isNotBlank() }
                            ?: member.uid?.takeIf { it.isNotBlank() }
                            ?: "Unknown Member"

                        val memberDisplayName = if (isCurrentUser) "$rawName (You)" else rawName
                        val roleBadge = member.role?.trim()?.uppercase() ?: "MEMBER"
                        val statusBadge = member.status?.trim()?.uppercase() ?: "ACTIVE"

                        val itemShape = when {
                            sortedMembers.size == 1 -> ShapeGroupedItemSingle
                            index == 0 -> ShapeGroupedItemTop
                            index == sortedMembers.lastIndex -> ShapeGroupedItemBottom
                            else -> ShapeGroupedItemMiddle
                        }

                        Surface(
                            shape = itemShape,
                            color = MaterialTheme.colorScheme.surfaceContainer,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("household_member_row")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = Space12, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f, fill = false)
                                ) {
                                     Box(
                                        modifier = Modifier
                                            .size(34.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (isCurrentUser) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                                else MaterialTheme.colorScheme.surfaceContainerHighest
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Person,
                                            contentDescription = null,
                                            tint = if (isCurrentUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(10.dp))

                                    Column(
                                        modifier = Modifier.semantics(mergeDescendants = true) { }
                                    ) {
                                        Text(
                                            text = memberDisplayName,
                                            style = BodyRegular,
                                            fontWeight = if (isCurrentUser) FontWeight.Bold else FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        if (member.displayName?.isNotBlank() == true && member.email?.isNotBlank() == true) {
                                            Text(
                                                text = member.email!!,
                                                style = MicroMetadata,
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
                                    val (statusBg, statusFg) = if (statusBadge == "ACTIVE") {
                                        FinTrackTheme.colors.incomeContainer to FinTrackTheme.colors.income
                                    } else {
                                        FinTrackTheme.colors.warningContainer to FinTrackTheme.colors.warning
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(RadiusSmall),
                                        color = statusBg
                                    ) {
                                        Text(
                                            text = statusBadge,
                                            style = MicroMetadata,
                                            color = statusFg,
                                            modifier = Modifier
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                                .testTag("household_member_status")
                                        )
                                    }

                                    // Role Badge (OWNER / ADMIN / MEMBER)
                                    val (memRoleBg, memRoleFg) = when (roleBadge) {
                                        "OWNER" -> FinTrackTheme.colors.incomeContainer to FinTrackTheme.colors.income
                                        "ADMIN" -> MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) to MaterialTheme.colorScheme.primary
                                        else -> MaterialTheme.colorScheme.surfaceContainerHighest to MaterialTheme.colorScheme.onSurfaceVariant
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(RadiusSmall),
                                        color = memRoleBg
                                    ) {
                                        Text(
                                            text = roleBadge,
                                            style = MicroMetadata,
                                            fontWeight = FontWeight.Bold,
                                            color = memRoleFg,
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
                Spacer(modifier = Modifier.height(Space16))
                FinTrackButton(
                    onClick = onInviteMemberClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 48.dp)
                        .testTag("invite_member_button"),
                    variant = ButtonVariant.PRIMARY,
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                ) {
                    Text(
                        text = "Invite Member",
                        style = LabelBadgeMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
