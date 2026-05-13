package com.vinh.dyvat.ui.navigation

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Login : Screen("login")
    object Home : Screen("home")
    object Products : Screen("products")
    object ProductDetail : Screen("products/{productId}") {
        fun createRoute(productId: String) = "products/$productId"
    }
    object ProductForm : Screen("products/form?productId={productId}") {
        fun createRoute(productId: String? = null) =
            if (productId != null) "products/form?productId=$productId" else "products/form"
    }
    object ProductImport : Screen("products/import")
    object PurchaseList : Screen("purchase")
    object PurchaseDetail : Screen("purchase/{ticketId}") {
        fun createRoute(ticketId: String) = "purchase/$ticketId"
    }
    object PurchaseForm : Screen("purchase/form")
    object AddPurchaseItem : Screen("purchase/add-item?purchaseDate={purchaseDate}") {
        fun createRoute(purchaseDate: String) = "purchase/add-item?purchaseDate=$purchaseDate"
    }
    object EditPurchaseItem : Screen("purchase/edit-item/{itemId}?purchaseDate={purchaseDate}") {
        fun createRoute(itemId: Int, purchaseDate: String) = "purchase/edit-item/$itemId?purchaseDate=$purchaseDate"
    }
    object Sales : Screen("sales")
    object SaleDetail : Screen("sales/{ticketId}") {
        fun createRoute(ticketId: String) = "sales/$ticketId"
    }
    object SaleForm : Screen("sales/form")
    object AddSaleItem : Screen("sales/add-item?saleDate={saleDate}") {
        fun createRoute(saleDate: String) = "sales/add-item?saleDate=$saleDate"
    }
    object EditSaleItem : Screen("sales/edit-item/{itemId}?saleDate={saleDate}") {
        fun createRoute(itemId: Int, saleDate: String) = "sales/edit-item/$itemId?saleDate=$saleDate"
    }
    object Inventory : Screen("inventory")
    object InventoryLotDetail : Screen("inventory/{ticketId}") {
        fun createRoute(ticketId: String) = "inventory/$ticketId"
    }
    object Statistics : Screen("statistics")
    object Settings : Screen("settings")

    object Categories : Screen("settings/categories")
    object Units : Screen("settings/units")
    object Suppliers : Screen("settings/suppliers")
}
