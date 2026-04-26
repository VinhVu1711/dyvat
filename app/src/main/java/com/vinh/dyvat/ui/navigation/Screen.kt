package com.vinh.dyvat.ui.navigation

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Login : Screen("login")
    object Import : Screen("import")
    object Sales : Screen("sales")
    object Statistics : Screen("statistics")
    object Settings : Screen("settings")

    object Categories : Screen("settings/categories")
    object Units : Screen("settings/units")
    object Suppliers : Screen("settings/suppliers")
}
