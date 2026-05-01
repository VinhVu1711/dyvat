package com.vinh.dyvat.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.vinh.dyvat.ui.screens.auth.LoginScreen
import com.vinh.dyvat.ui.screens.auth.SplashScreen
import com.vinh.dyvat.ui.screens.categories.CategoriesScreen
import com.vinh.dyvat.ui.screens.home.HomeScreen
import com.vinh.dyvat.ui.screens.inventory.InventoryListScreen
import com.vinh.dyvat.ui.screens.inventory.InventoryDetailScreen
import com.vinh.dyvat.ui.screens.purchase.PurchaseListScreen
import com.vinh.dyvat.ui.screens.purchase.PurchaseDetailScreen
import com.vinh.dyvat.ui.screens.purchase.PurchaseFormScreen
import com.vinh.dyvat.ui.screens.products.ProductDetailScreen
import com.vinh.dyvat.ui.screens.products.ProductFormScreen
import com.vinh.dyvat.ui.screens.products.ProductsScreen
import com.vinh.dyvat.ui.screens.sales.SalesScreen
import com.vinh.dyvat.ui.screens.settings.SettingsScreen
import com.vinh.dyvat.ui.screens.statistics.StatisticsScreen
import com.vinh.dyvat.ui.screens.suppliers.SuppliersScreen
import com.vinh.dyvat.ui.screens.units.UnitsScreen

@Composable
fun DyvatNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    startDestination: String = Screen.Splash.route
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        composable(Screen.Splash.route) {
            SplashScreen(
                onNavigateToLogin = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                },
                onNavigateToMain = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Login.route) {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Home.route) {
            HomeScreen(
                onNavigate = { route ->
                    navController.navigate(route)
                }
            )
        }

        composable(Screen.Products.route) {
            ProductsScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToDetail = { productId ->
                    navController.navigate(Screen.ProductDetail.createRoute(productId))
                },
                onNavigateToAdd = {
                    navController.navigate(Screen.ProductForm.createRoute())
                },
                showBackButton = false
            )
        }

        composable(
            route = Screen.ProductDetail.route,
            arguments = listOf(navArgument("productId") { type = NavType.StringType })
        ) { backStackEntry ->
            val productId = backStackEntry.arguments?.getString("productId") ?: return@composable
            ProductDetailScreen(
                productId = productId,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToEdit = { id ->
                    navController.navigate(Screen.ProductForm.createRoute(id))
                }
            )
        }

        composable(
            route = Screen.ProductForm.route,
            arguments = listOf(
                navArgument("productId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val productId = backStackEntry.arguments?.getString("productId")
            ProductFormScreen(
                productId = productId,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.PurchaseList.route) {
            PurchaseListScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToDetail = { ticketId ->
                    navController.navigate(Screen.PurchaseDetail.createRoute(ticketId))
                },
                onNavigateToAdd = {
                    navController.navigate(Screen.PurchaseForm.route)
                },
                showBackButton = false
            )
        }

        composable(
            route = Screen.PurchaseDetail.route,
            arguments = listOf(navArgument("ticketId") { type = NavType.StringType })
        ) { backStackEntry ->
            val ticketId = backStackEntry.arguments?.getString("ticketId") ?: return@composable
            PurchaseDetailScreen(
                ticketId = ticketId,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.PurchaseForm.route) {
            PurchaseFormScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Sales.route) {
            SalesScreen()
        }

        composable(Screen.Inventory.route) {
            InventoryListScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToDetail = { ticketId ->
                    navController.navigate(Screen.InventoryLotDetail.createRoute(ticketId))
                },
                showBackButton = false
            )
        }

        composable(
            route = Screen.InventoryLotDetail.route,
            arguments = listOf(navArgument("ticketId") { type = NavType.StringType })
        ) { backStackEntry ->
            val ticketId = backStackEntry.arguments?.getString("ticketId") ?: return@composable
            InventoryDetailScreen(
                ticketId = ticketId,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Statistics.route) {
            StatisticsScreen()
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                onNavigateToCategories = { navController.navigate(Screen.Categories.route) },
                onNavigateToUnits = { navController.navigate(Screen.Units.route) },
                onNavigateToSuppliers = { navController.navigate(Screen.Suppliers.route) }
            )
        }

        composable(Screen.Categories.route) {
            CategoriesScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Units.route) {
            UnitsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Suppliers.route) {
            SuppliersScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
