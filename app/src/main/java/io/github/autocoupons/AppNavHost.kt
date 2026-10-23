package io.github.autocoupons

import android.net.Uri
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import io.github.autocoupons.feature.auth.LoginScreen
import io.github.autocoupons.feature.auth.RegisterScreen
import io.github.autocoupons.feature.browser.BrowserScreen
import io.github.autocoupons.feature.browser.BrowserViewModel
import io.github.autocoupons.feature.home.HomeScreen

object Routes {
    const val LOGIN = "auth/login"
    const val REGISTER = "auth/register"
    const val HOME = "home"
    const val BROWSER = "browser/{${BrowserViewModel.ARG_RETAILER}}?${BrowserViewModel.ARG_URL}={${BrowserViewModel.ARG_URL}}"

    fun browser(retailerId: String, url: String? = null) =
        "browser/$retailerId" + (url?.let { "?${BrowserViewModel.ARG_URL}=${Uri.encode(it)}" } ?: "")
}

@Composable
fun AppNavHost(signedIn: Boolean, navController: NavHostController = rememberNavController()) {
    NavHost(
        navController = navController,
        startDestination = if (signedIn) Routes.HOME else Routes.LOGIN,
        enterTransition = { fadeIn(tween(180)) },
        exitTransition = { fadeOut(tween(120)) },
    ) {
        composable(Routes.LOGIN) {
            LoginScreen(
                onSignedIn = { navController.enterHome() },
                onCreateAccount = { navController.navigate(Routes.REGISTER) { launchSingleTop = true } },
            )
        }
        composable(Routes.REGISTER) {
            RegisterScreen(
                onRegistered = { navController.enterHome() },
                onBackToLogin = {
                    if (!navController.popBackStack(Routes.LOGIN, inclusive = false)) {
                        navController.navigate(Routes.LOGIN) { popUpTo(Routes.REGISTER) { inclusive = true } }
                    }
                },
            )
        }
        composable(Routes.HOME) {
            HomeScreen(
                onOpenBrowser = { retailerId, url -> navController.navigate(Routes.browser(retailerId, url)) { launchSingleTop = true } },
                onSignedOut = {
                    navController.navigate(Routes.LOGIN) { popUpTo(Routes.HOME) { inclusive = true } }
                },
            )
        }
        composable(
            Routes.BROWSER,
            arguments = listOf(
                navArgument(BrowserViewModel.ARG_RETAILER) { type = NavType.StringType },
                navArgument(BrowserViewModel.ARG_URL) {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
            ),
        ) {
            BrowserScreen(onClose = { navController.popBackStack() })
        }
    }
}

/** Home becomes the root so Back from Home exits instead of returning to the auth forms. */
private fun NavHostController.enterHome() {
    navigate(Routes.HOME) {
        popUpTo(graph.id) { inclusive = true }
        launchSingleTop = true
    }
}
