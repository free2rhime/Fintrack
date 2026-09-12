package com.example.ui.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Settings
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
import androidx.compose.ui.graphics.graphicsLayer
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
import com.example.ui.theme.ShapeNavigationCapsule
import com.example.ui.theme.ShapePill
import com.example.ui.theme.isReducedMotionEnabled
import kotlin.math.roundToInt

val NavItemLabelStyle = TextStyle(
    fontFamily = FontFamily.Default,
    fontWeight = FontWeight.Bold,
    fontSize = 12.sp,
    lineHeight = 16.sp,
    letterSpacing = (-0.2).sp
)

enum class BottomNavItem(
    val title: String,
    val icon: ImageVector,
    val tabIndex: Int,
    val unselectedIcon: ImageVector = icon
) {
    Dashboard("Dashboard", Icons.Default.Dashboard, 0, Icons.Outlined.Dashboard),
    Transactions("Transactions", Icons.AutoMirrored.Filled.ReceiptLong, 1, Icons.AutoMirrored.Outlined.ReceiptLong),
    Analytics("Analytics", Icons.Default.Analytics, 2, Icons.Outlined.Analytics),
    Categories("Categories", Icons.Default.Category, 3, Icons.Outlined.Category),
    Settings("Settings", Icons.Default.Settings, 4, Icons.Outlined.Settings)
}

/**
 * Floating Navigation Capsule for FinTrack Material 3 Expressive (M3-2).
 *
 * Visual & Spatial Architecture:
 * - Expressive Tonal Container: Elevated through M3 [surfaceContainerHigh] tone with soft ambient depth
 * - Organic geometry: Consumes [ShapeNavigationCapsule] (28dp curvature)
 * - Morphing selection indicator driven by centralized [FinTrackMotion] spring physics
 * - Tactile press interaction: Subtle physical depress (0.975f) on touch-down via [pressInteractionSpec]
 * - Dual-state icons: Solid/filled icon for active destination, outlined icon for inactive destinations
 * - Expressive icon response: Subtle spring emphasis and tint interpolation
 * - Dynamic selected destination: Icon + full label ("Transactions" never shortened)
 * - Inactive destinations: Icon-first presentation with full TalkBack semantics
 * - Guaranteed >= 48dp touch target across compact (360dp), medium (390dp), and expanded (412dp+) viewports
 * - Reduced-motion support: Graceful fallback to instant snap / linear fades
 * - System navigation bar / gesture insets preserved via [navigationBarsPadding]
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

        // Floating Capsule Surface (M3 Expressive Tonal Container)
        Surface(
            shape = FinTrackTheme.shapes.navigationCapsule,
            color = FinTrackTheme.colors.surfaceContainerHigh,
            tonalElevation = 3.dp,
            shadowElevation = 3.dp,
            modifier = Modifier
                .padding(horizontal = horizontalMargin)
                .widthIn(max = 520.dp)
                .fillMaxWidth()
                .height(58.dp)
                .testTag("bottom_navigation_bar")
        ) {
            val tabOffsets = remember { mutableStateMapOf<Int, Float>() }
            val tabWidths = remember { mutableStateMapOf<Int, Float>() }
            val indicatorShape = ShapePill

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 4.dp, vertical = 4.dp)
            ) {
                // Morphing Selection Indicator (Spatial spring movement across destinations)
                val targetLeft = tabOffsets[selectedTabIndex] ?: 0f
                val targetWidth = tabWidths[selectedTabIndex] ?: 0f

                val animatedLeft by animateFloatAsState(
                    targetValue = targetLeft,
                    animationSpec = if (reducedMotion) snap() else FinTrackMotion.selectionSpring(),
                    label = "floatingNavIndicatorLeft"
                )
                val animatedWidth by animateFloatAsState(
                    targetValue = targetWidth,
                    animationSpec = if (reducedMotion) snap() else FinTrackMotion.selectionSpring(),
                    label = "floatingNavIndicatorWidth"
                )

                if (animatedWidth > 0f) {
                    Box(
                        modifier = Modifier
                            .offset { IntOffset(animatedLeft.roundToInt(), 0) }
                            .width(with(LocalDensity.current) { animatedWidth.toDp() })
                            .fillMaxHeight()
                            .background(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = indicatorShape
                            )
                    )
                }

                // Row of 5 Primary Destinations (Strictly Preserved Order)
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BottomNavItem.values().forEach { item ->
                        val isSelected = selectedTabIndex == item.tabIndex
                        val targetWeight = if (isSelected) {
                            if (screenWidth < BreakpointCompactWidth) 1.75f else 1.65f
                        } else 1.0f

                        val itemWeight by animateFloatAsState(
                            targetValue = targetWeight,
                            animationSpec = if (reducedMotion) snap() else FinTrackMotion.contentSpring(),
                            label = "navItemWeight_${item.tabIndex}"
                        )

                        val interactionSource = remember { MutableInteractionSource() }
                        val isPressed by interactionSource.collectIsPressedAsState()

                        // Tactile press depression (M3 Expressive: 0.975f target)
                        val itemPressScale by animateFloatAsState(
                            targetValue = if (isPressed && !reducedMotion) FinTrackMotion.PressScaleTarget else 1.0f,
                            animationSpec = if (reducedMotion) snap() else FinTrackMotion.pressInteractionSpec(),
                            label = "navItemPressScale_${item.tabIndex}"
                        )

                        // Subtle expressive icon scale response
                        val iconScale by animateFloatAsState(
                            targetValue = if (isSelected && !reducedMotion) 1.08f else 1.0f,
                            animationSpec = if (reducedMotion) snap() else FinTrackMotion.interactiveSpring(),
                            label = "navIconScale_${item.tabIndex}"
                        )

                        val animatedIconColor by animateColorAsState(
                            targetValue = if (isSelected) {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.70f)
                            },
                            animationSpec = if (reducedMotion) snap() else FinTrackMotion.fastTween(),
                            label = "navIconColor_${item.tabIndex}"
                        )

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
                                        Modifier.background(
                                            color = MaterialTheme.colorScheme.primaryContainer,
                                            shape = indicatorShape
                                        )
                                    } else {
                                        Modifier
                                    }
                                )
                                .clip(indicatorShape)
                                .clickable(
                                    interactionSource = interactionSource,
                                    indication = null
                                ) {
                                    if (!isSelected) {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        onTabSelected(item.tabIndex)
                                    }
                                }
                                .graphicsLayer {
                                    scaleX = itemPressScale
                                    scaleY = itemPressScale
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
                                    .padding(horizontal = if (screenWidth < BreakpointCompactWidth) 3.dp else 6.dp)
                                    .fillMaxHeight()
                            ) {
                                val currentIcon = if (isSelected) item.icon else item.unselectedIcon
                                Icon(
                                    imageVector = currentIcon,
                                    contentDescription = null, // Semantics hoisted to parent tab container
                                    tint = animatedIconColor,
                                    modifier = Modifier
                                        .size(20.dp)
                                        .graphicsLayer {
                                            scaleX = iconScale
                                            scaleY = iconScale
                                        }
                                )

                                AnimatedVisibility(
                                    visible = isSelected,
                                    enter = fadeIn(animationSpec = if (reducedMotion) snap() else FinTrackMotion.microTween()) +
                                            expandHorizontally(
                                                animationSpec = if (reducedMotion) snap() else FinTrackMotion.interactiveSpring(),
                                                expandFrom = Alignment.Start
                                            ),
                                    exit = fadeOut(animationSpec = if (reducedMotion) snap() else FinTrackMotion.microTween()) +
                                            shrinkHorizontally(
                                                animationSpec = if (reducedMotion) snap() else FinTrackMotion.interactiveSpring(),
                                                shrinkTowards = Alignment.Start
                                            )
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Spacer(modifier = Modifier.width(if (screenWidth < BreakpointCompactWidth) 3.5.dp else 5.dp))
                                        Text(
                                            text = item.title,
                                            maxLines = 1,
                                            softWrap = false,
                                            style = NavItemLabelStyle,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer,
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

