package com.example.ui.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.BreakpointCompactWidth
import com.example.ui.theme.FinTrackMotion
import com.example.ui.theme.FinTrackTheme
import com.example.ui.theme.ShapePill
import com.example.ui.theme.isReducedMotionEnabled
import kotlin.math.roundToInt

val NavItemLabelStyle = TextStyle(
    fontFamily = FontFamily.Default,
    fontWeight = FontWeight.SemiBold,
    fontSize = 11.5.sp,
    lineHeight = 15.sp,
    letterSpacing = (-0.3).sp
)

enum class BottomNavItem(
    val title: String,
    val icon: ImageVector,
    val tabIndex: Int
) {
    Dashboard("Dashboard", Icons.Default.Dashboard, 0),
    Transactions("Transactions", Icons.Default.ReceiptLong, 1),
    Analytics("Analytics", Icons.Default.Analytics, 2),
    Categories("Categories", Icons.Default.Category, 3),
    Settings("Settings", Icons.Default.Settings, 4)
}

/**
 * Floating Navigation Bar for FinTrack Design System v2.
 *
 * Visual Direction: Precision Fintech (Floating, compact, tactile, restrained)
 * - Elevated floating capsule with subtle 1dp structural border
 * - Morphing selection indicator driven by Motion System v2 spring physics
 * - Dynamic selected destination: icon + full label ("Transactions" preserved)
 * - Inactive destinations: icon only with full accessible TalkBack semantics
 * - Guaranteed >= 48dp touch target across compact (360dp), medium (390dp), and expanded (412dp+) viewports
 * - System navigation bar / gesture insets handled via navigationBarsPadding
 */
@Composable
fun FinTrackBottomNavigation(
    selectedTabIndex: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val reducedMotion = isReducedMotionEnabled()

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(top = 4.dp, bottom = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        val screenWidth = maxWidth
        val horizontalMargin = if (screenWidth < BreakpointCompactWidth) 12.dp else 16.dp

        // Floating Capsule Surface
        Surface(
            shape = ShapePill,
            color = FinTrackTheme.colors.surfaceElevated,
            border = BorderStroke(1.dp, FinTrackTheme.colors.borderSubtle),
            shadowElevation = 4.dp,
            modifier = Modifier
                .padding(horizontal = horizontalMargin)
                .widthIn(max = 520.dp)
                .fillMaxWidth()
                .height(58.dp)
                .testTag("bottom_navigation_bar")
        ) {
            val tabOffsets = remember { mutableStateMapOf<Int, Float>() }
            val tabWidths = remember { mutableStateMapOf<Int, Float>() }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 4.dp, vertical = 4.dp)
            ) {
                // Morphing Selection Indicator (Transitions smoothly between destinations)
                val targetLeft = tabOffsets[selectedTabIndex] ?: 0f
                val targetWidth = tabWidths[selectedTabIndex] ?: 0f

                val animatedLeft by animateFloatAsState(
                    targetValue = targetLeft,
                    animationSpec = if (reducedMotion) snap() else FinTrackMotion.interactiveSpring(),
                    label = "floatingNavIndicatorLeft"
                )
                val animatedWidth by animateFloatAsState(
                    targetValue = targetWidth,
                    animationSpec = if (reducedMotion) snap() else FinTrackMotion.interactiveSpring(),
                    label = "floatingNavIndicatorWidth"
                )

                if (animatedWidth > 0f) {
                    Box(
                        modifier = Modifier
                            .offset { IntOffset(animatedLeft.roundToInt(), 0) }
                            .width(with(LocalDensity.current) { animatedWidth.toDp() })
                            .fillMaxHeight()
                            .background(
                                color = FinTrackTheme.colors.surfaceSelected,
                                shape = ShapePill
                            )
                            .border(
                                width = 1.dp,
                                color = FinTrackTheme.colors.borderActive.copy(alpha = 0.35f),
                                shape = ShapePill
                            )
                    )
                }

                // Row of 5 Primary Destinations
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BottomNavItem.values().forEach { item ->
                        val isSelected = selectedTabIndex == item.tabIndex
                        val targetWeight = if (isSelected) 1.65f else 1.0f

                        val itemWeight by animateFloatAsState(
                            targetValue = targetWeight,
                            animationSpec = if (reducedMotion) snap() else FinTrackMotion.interactiveSpring(),
                            label = "navItemWeight_${item.tabIndex}"
                        )

                        val iconColor = if (isSelected) {
                            FinTrackTheme.colors.brandAccent
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f)
                        }

                        val isInitialUnmeasured = tabWidths.isEmpty() && isSelected

                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .weight(itemWeight)
                                .fillMaxHeight()
                                .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                                .onGloballyPositioned { coordinates ->
                                    val position = coordinates.positionInParent()
                                    tabOffsets[item.tabIndex] = position.x
                                    tabWidths[item.tabIndex] = coordinates.size.width.toFloat()
                                }
                                .then(
                                    if (isInitialUnmeasured) {
                                        Modifier
                                            .background(
                                                color = FinTrackTheme.colors.surfaceSelected,
                                                shape = ShapePill
                                            )
                                            .border(
                                                width = 1.dp,
                                                color = FinTrackTheme.colors.borderActive.copy(alpha = 0.35f),
                                                shape = ShapePill
                                            )
                                    } else {
                                        Modifier
                                    }
                                )
                                .clip(ShapePill)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) {
                                    if (!isSelected) {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        onTabSelected(item.tabIndex)
                                    }
                                }
                                .semantics(mergeDescendants = true) {
                                    set(SemanticsProperties.Text, listOf(AnnotatedString(item.title)))
                                    this.contentDescription = item.title
                                    this.role = Role.Tab
                                    this.selected = isSelected
                                }
                                .testTag("bottom_nav_${item.title.lowercase()}")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                                modifier = Modifier
                                    .padding(horizontal = 6.dp)
                                    .fillMaxHeight()
                            ) {
                                Icon(
                                    imageVector = item.icon,
                                    contentDescription = null, // Semantics hoisted to parent tab container
                                    tint = iconColor,
                                    modifier = Modifier.size(20.dp)
                                )

                                AnimatedVisibility(
                                    visible = isSelected,
                                    enter = fadeIn(animationSpec = FinTrackMotion.microTween()) +
                                            expandHorizontally(
                                                animationSpec = if (reducedMotion) snap() else FinTrackMotion.interactiveSpring(),
                                                expandFrom = Alignment.Start
                                            ),
                                    exit = fadeOut(animationSpec = FinTrackMotion.microTween()) +
                                            shrinkHorizontally(
                                                animationSpec = if (reducedMotion) snap() else FinTrackMotion.interactiveSpring(),
                                                shrinkTowards = Alignment.Start
                                            )
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Spacer(modifier = Modifier.width(5.dp))
                                        Text(
                                            text = item.title,
                                            maxLines = 1,
                                            softWrap = false,
                                            style = NavItemLabelStyle,
                                            color = FinTrackTheme.colors.brandAccent,
                                            overflow = TextOverflow.Clip
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

