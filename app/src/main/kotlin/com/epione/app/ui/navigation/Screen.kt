package com.epione.app.ui.navigation

/** Clés des arguments de navigation. */
object NavArgs {
    const val FINESS_ET = "finessEt"
}

/** Destinations de l'application. */
sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object Detail : Screen("detail/{${NavArgs.FINESS_ET}}") {
        fun buildRoute(finessEt: String) = "detail/$finessEt"
    }
}
