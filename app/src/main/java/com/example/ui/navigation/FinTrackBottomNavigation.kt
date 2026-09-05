package com.example.ui.navigation

import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.ui.theme.CobaltBlue
import com.example.ui.theme.LabelBadgeMedium
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

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
 * Bottom Navigation component for FinTrack Design System v1.
 * Applies dark tonal background (SurfaceDark), CobaltBlue active indicator,
 * and strict accessible touch targets across all 5 primary destinations.
 */
@Composable
fun FinTrackBottomNavigation(
    selectedTabIndex: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    NavigationBar(
        modifier = modifier.testTag("bottom_navigation_bar"),
        containerColor = SurfaceDark
    ) {
        BottomNavItem.values().forEach { item ->
            val isSelected = selectedTabIndex == item.tabIndex
            NavigationBarItem(
                selected = isSelected,
                onClick = { onTabSelected(item.tabIndex) },
                icon = {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.title
                    )
                },
                label = {
                    Text(
                        text = item.title,
                        maxLines = 1,
                        softWrap = false,
                        style = LabelBadgeMedium,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Color.White,
                    selectedTextColor = TextPrimary,
                    indicatorColor = CobaltBlue,
                    unselectedIconColor = TextSecondary,
                    unselectedTextColor = TextSecondary
                ),
                modifier = Modifier
                    .defaultMinSize(minHeight = 48.dp)
                    .testTag("bottom_nav_${item.title.lowercase()}")
            )
        }
    }
}
