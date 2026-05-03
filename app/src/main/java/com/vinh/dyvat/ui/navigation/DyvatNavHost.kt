package com.vinh.dyvat.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
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
import com.vinh.dyvat.ui.screens.purchase.AddPurchaseItemScreen
import com.vinh.dyvat.ui.screens.purchase.PurchaseListScreen
import com.vinh.dyvat.ui.screens.purchase.PurchaseDetailScreen
import com.vinh.dyvat.ui.screens.purchase.PurchaseFormScreen
import com.vinh.dyvat.ui.screens.purchase.PurchaseViewModel
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

        composable(Screen.Products.route) { backStackEntry ->
            val shouldRefreshProducts by backStackEntry.savedStateHandle
                .getStateFlow("products_should_refresh", false)
                .collectAsState()
            ProductsScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToDetail = { productId ->
                    navController.navigate(Screen.ProductDetail.createRoute(productId))
                },
                onNavigateToAdd = {
                    navController.navigate(Screen.ProductForm.createRoute())
                },
                showBackButton = false,
                refreshSignal = shouldRefreshProducts,
                onRefreshHandled = {
                    backStackEntry.savedStateHandle["products_should_refresh"] = false
                }
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
                },
                onProductDeleted = {
                    navController.getBackStackEntry(Screen.Products.route)
                        .savedStateHandle["products_should_refresh"] = true
                    navController.popBackStack()
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
                onNavigateBack = { navController.popBackStack() },
                onProductSaved = {
                    navController.getBackStackEntry(Screen.Products.route)
                        .savedStateHandle["products_should_refresh"] = true
                    navController.popBackStack()
                }
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

        composable(Screen.PurchaseForm.route) { backStackEntry ->
            val viewModel: PurchaseViewModel = hiltViewModel(backStackEntry)
            val formState by viewModel.formState.collectAsState()

            // Save products and suppliers to SavedStateHandle before navigating
            LaunchedEffect(formState.availableProducts, formState.suppliers) {
                val productsJson = formState.availableProducts.map {
                    mapOf(
                        "id" to it.product.id,
                        "name" to it.product.name,
                        "code" to it.product.code,
                        "categoryId" to it.product.categoryId,
                        "categoryName" to it.categoryName,
                        "unitId" to it.product.unitId,
                        "unitName" to it.unitName,
                        "supplierId" to it.product.supplierId,
                        "supplierName" to it.supplierName,
                        "defaultPurchasePriceVnd" to it.product.defaultPurchasePriceVnd
                    )
                }
                val suppliersList = formState.suppliers.map {
                    mapOf("id" to it.id, "name" to it.name)
                }
                backStackEntry.savedStateHandle["available_products"] = productsJson
                backStackEntry.savedStateHandle["suppliers_list"] = suppliersList
            }

            PurchaseFormScreen(
                onNavigateBack = { navController.popBackStack() },
                navController = navController,
                viewModel = viewModel
            )
        }

        composable(
            route = Screen.AddPurchaseItem.route,
            arguments = listOf(navArgument("purchaseDate") { type = NavType.StringType })
        ) { backStackEntry ->
            // Use parent ViewModel to share state with PurchaseFormScreen
            val parentEntry = remember(backStackEntry) {
                navController.getBackStackEntry(Screen.PurchaseForm.route)
            }
            val viewModel: PurchaseViewModel = hiltViewModel(parentEntry)
            val purchaseDate = backStackEntry.arguments?.getString("purchaseDate") ?: ""

            // Get products and suppliers from parent ViewModel state
            val formState by viewModel.formState.collectAsState()
            val productsData = remember(formState.availableProducts) {
                formState.availableProducts.map {
                    mapOf(
                        "id" to it.product.id,
                        "name" to it.product.name,
                        "code" to it.product.code,
                        "categoryId" to it.product.categoryId,
                        "categoryName" to it.categoryName,
                        "unitId" to it.product.unitId,
                        "unitName" to it.unitName,
                        "supplierId" to it.product.supplierId,
                        "supplierName" to it.supplierName,
                        "defaultPurchasePriceVnd" to it.product.defaultPurchasePriceVnd
                    )
                }
            }
            val suppliersData = remember(formState.suppliers) {
                formState.suppliers.map { mapOf("id" to it.id, "name" to it.name) }
            }

            AddPurchaseItemScreen(
                purchaseDate = purchaseDate,
                suppliersData = suppliersData,
                availableProductsData = productsData,
                onProductAdded = { productId, productName, supplierId, supplierName, unitId, unitName, quantity, expiryDate, price ->
                    navController.previousBackStackEntry?.savedStateHandle?.apply {
                        set("added_product_id", productId)
                        set("added_product_name", productName)
                        set("added_supplier_id", supplierId)
                        set("added_supplier_name", supplierName)
                        set("added_unit_id", unitId)
                        set("added_unit_name", unitName)
                        set("added_quantity", quantity)
                        set("added_expiry_date", expiryDate)
                        set("added_price", price)
                    }
                    navController.popBackStack()
                },
                onNavigateBack = { navController.popBackStack() },
                viewModel = viewModel
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
