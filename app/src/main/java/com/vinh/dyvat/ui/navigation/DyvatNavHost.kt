package com.vinh.dyvat.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.vinh.dyvat.ui.screens.auth.LoginScreen
import com.vinh.dyvat.ui.screens.auth.SplashScreen
import com.vinh.dyvat.ui.screens.categories.CategoriesScreen
import com.vinh.dyvat.ui.screens.imports.ImportScreen
import com.vinh.dyvat.ui.screens.settings.SettingsScreen
import com.vinh.dyvat.ui.screens.sales.SalesScreen
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
                    navController.navigate(Screen.Import.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Login.route) {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(Screen.Import.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Import.route) {
            ImportScreen()
        }

        composable(Screen.Sales.route) {
            SalesScreen()
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
