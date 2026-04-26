package com.vinh.dyvat.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.ui.graphics.vector.ImageVector

data class BottomNavItem(
    val title: String,
    val icon: ImageVector,
    val route: String
)

val bottomNavItems = listOf(
    BottomNavItem(
        title = "Nhập hàng",
        icon = Icons.Default.ShoppingCart,
        route = Screen.Import.route
    ),
    BottomNavItem(
        title = "Bán hàng",
        icon = Icons.Default.AttachMoney,
        route = Screen.Sales.route
    ),
    BottomNavItem(
        title = "Thống kê",
        icon = Icons.Default.BarChart,
        route = Screen.Statistics.route
    ),
    BottomNavItem(
        title = "Cài đặt",
        icon = Icons.Default.Settings,
        route = Screen.Settings.route
    )
)
