package com.epione.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.epione.app.ui.screen.about.AboutScreen
import com.epione.app.ui.screen.detail.DetailScreen
import com.epione.app.ui.screen.home.HomeScreen

@Composable
fun EpioneNavHost() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Screen.Home.route,
    ) {
        composable(Screen.Home.route) {
            HomeScreen(
                onEtablissementClick = { finessEt ->
                    navController.navigate(Screen.Detail.buildRoute(finessEt))
                },
                onAboutClick = {
                    navController.navigate(Screen.About.route)
                },
            )
        }

        composable(Screen.About.route) {
            AboutScreen(onBack = { navController.popBackStack() })
        }

        composable(
            route = Screen.Detail.route,
            arguments = listOf(
                navArgument(NavArgs.FINESS_ET) { type = NavType.StringType }
            ),
        ) {
            DetailScreen(
                onBack = { navController.popBackStack() },
            )
        }
    }
}
