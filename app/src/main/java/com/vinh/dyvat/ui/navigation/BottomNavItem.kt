package com.vinh.dyvat.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Store
import androidx.compose.ui.graphics.vector.ImageVector

data class BottomNavItem(
    val title: String,
    val icon: ImageVector,
    val route: String
)

val bottomNavItems = listOf(
    BottomNavItem(
        title = "Trang chủ",
        icon = Icons.Default.Home,
        route = Screen.Home.route
    ),
    BottomNavItem(
        title = "Sản phẩm",
        icon = Icons.Default.Store,
        route = Screen.Products.route
    ),
    BottomNavItem(
        title = "Nhập",
        icon = Icons.Default.ShoppingCart,
        route = Screen.PurchaseList.route
    ),
    BottomNavItem(
        title = "Bán",
        icon = Icons.Default.AttachMoney,
        route = Screen.Sales.route
    ),
    BottomNavItem(
        title = "Kho",
        icon = Icons.Default.Inventory,
        route = Screen.Inventory.route
    ),
    BottomNavItem(
        title = "Thống kê",
        icon = Icons.Default.BarChart,
        route = Screen.Statistics.route
    )
)
