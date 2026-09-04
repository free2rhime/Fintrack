package com.example.ui.components

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import androidx.compose.ui.unit.sp
import com.example.data.model.HouseholdDto
import com.example.data.model.HouseholdMemberDto
import com.example.ui.theme.BodyRegular
import com.example.ui.theme.CardTitleAmount
import com.example.ui.theme.CobaltBlue
import com.example.ui.theme.IncomeContainer
import com.example.ui.theme.IncomeEmerald
import com.example.ui.theme.LabelBadgeMedium
import com.example.ui.theme.MicroMetadata
import com.example.ui.theme.RadiusLarge
import com.example.ui.theme.RadiusMedium
import com.example.ui.theme.RadiusSmall
import com.example.ui.theme.SectionHeadline
import com.example.ui.theme.Space12
import com.example.ui.theme.Space16
import com.example.ui.theme.Space4
import com.example.ui.theme.Space8
import com.example.ui.theme.SurfaceContainerDark
import com.example.ui.theme.SurfaceContainerHighDark
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WarningAmber

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

    FinTrackCard(
        modifier = modifier
            .fillMaxWidth()
            .testTag("household_summary_card"),
        containerColor = SurfaceDark,
        shape = RoundedCornerShape(RadiusLarge),
        border = BorderStroke(1.dp, SurfaceContainerHighDark.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
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
                            .size(36.dp)
                            .clip(RoundedCornerShape(RadiusMedium))
                            .background(CobaltBlue.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Home,
                            contentDescription = null,
                            tint = CobaltBlue,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(Space12))
                    Text(
                        text = household.name ?: "Active Household",
                        style = CardTitleAmount,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.testTag("household_name_text")
                    )
                }

                val roleText = currentUserMembership?.role?.trim()?.uppercase() ?: "MEMBER"
                val (roleBg, roleFg) = when (roleText) {
                    "OWNER" -> IncomeContainer to IncomeEmerald
                    "ADMIN" -> CobaltBlue.copy(alpha = 0.15f) to CobaltBlue
                    else -> SurfaceContainerHighDark to TextSecondary
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

            Spacer(modifier = Modifier.height(Space12))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Household ID: ${household.householdId ?: "Unknown"}",
                    style = MicroMetadata,
                    color = TextSecondary,
                    modifier = Modifier.testTag("household_id_text")
                )

                Text(
                    text = "Members: ${householdMembers.size}",
                    style = MicroMetadata,
                    fontWeight = FontWeight.SemiBold,
                    color = TextSecondary,
                    modifier = Modifier.testTag("household_member_count_text")
                )
            }

            Spacer(modifier = Modifier.height(Space12))
            HorizontalDivider(
                color = SurfaceContainerHighDark.copy(alpha = 0.5f),
                thickness = 1.dp
            )
            Spacer(modifier = Modifier.height(Space12))

            Text(
                text = "Household Members",
                style = SectionHeadline.copy(fontSize = 15.sp),
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(Space8))

            if (sortedMembers.isEmpty()) {
                Text(
                    text = "No household members found",
                    style = BodyRegular,
                    color = TextSecondary,
                    modifier = Modifier.padding(vertical = Space8)
                )
            } else {
                Column(
                    verticalArrangement = Arrangement.spacedBy(Space8),
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
                            shape = RoundedCornerShape(RadiusMedium),
                            color = SurfaceContainerDark,
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
                                                if (isCurrentUser) CobaltBlue.copy(alpha = 0.2f)
                                                else SurfaceContainerHighDark
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Person,
                                            contentDescription = null,
                                            tint = if (isCurrentUser) CobaltBlue else TextSecondary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(10.dp))

                                    Column {
                                        Text(
                                            text = memberDisplayName,
                                            style = BodyRegular,
                                            fontWeight = if (isCurrentUser) FontWeight.Bold else FontWeight.Medium,
                                            color = TextPrimary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        if (member.displayName?.isNotBlank() == true && member.email?.isNotBlank() == true) {
                                            Text(
                                                text = member.email!!,
                                                style = MicroMetadata,
                                                color = TextSecondary,
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
                                        IncomeContainer to IncomeEmerald
                                    } else {
                                        WarningAmber.copy(alpha = 0.15f) to WarningAmber
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
                                        "OWNER" -> IncomeContainer to IncomeEmerald
                                        "ADMIN" -> CobaltBlue.copy(alpha = 0.15f) to CobaltBlue
                                        else -> SurfaceContainerHighDark to TextSecondary
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
