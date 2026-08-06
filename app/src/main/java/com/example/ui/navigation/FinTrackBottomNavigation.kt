package com.example.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag

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

@Composable
fun FinTrackBottomNavigation(
    selectedTabIndex: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    NavigationBar(
        modifier = modifier.testTag("bottom_navigation_bar")
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
                label = { Text(item.title) },
                modifier = Modifier.testTag("bottom_nav_${item.title.lowercase()}")
            )
        }
    }
}
