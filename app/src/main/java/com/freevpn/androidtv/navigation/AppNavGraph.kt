package com.freevpn.androidtv.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.freevpn.androidtv.model.VpnServer
import com.freevpn.androidtv.ui.account.AccountScreen
import com.freevpn.androidtv.ui.activation.ActivationScreen
import com.freevpn.androidtv.ui.home.HomeScreen
import com.freevpn.androidtv.ui.servers.ServersScreen
import com.freevpn.androidtv.ui.settings.AboutScreen
import com.freevpn.androidtv.ui.settings.PrivacyScreen
import com.freevpn.androidtv.ui.settings.SettingsScreen
import com.freevpn.androidtv.ui.welcome.WelcomeScreen

@Composable
fun AppNavGraph(
    navController: NavHostController,
    onRequestVpnPermission: (VpnServer) -> Unit,
) {
    // A short, GPU-cheap cross-fade. The Navigation default is a 700 ms fade, during which BOTH the
    // outgoing and incoming screens are fully composed and each draw their full-screen background —
    // on low-power TV hardware that long double-render window is what makes route changes (e.g. into
    // the server list) feel sluggish. A quick fade keeps the swap snappy.
    val fade = tween<Float>(durationMillis = 140)
    NavHost(
        navController = navController,
        startDestination = Routes.WELCOME,
        enterTransition = { fadeIn(animationSpec = fade) },
        exitTransition = { fadeOut(animationSpec = fade) },
        popEnterTransition = { fadeIn(animationSpec = fade) },
        popExitTransition = { fadeOut(animationSpec = fade) },
    ) {

        composable(Routes.WELCOME) {
            WelcomeScreen(onAuthenticated = {
                navController.navigate(Routes.HOME) {
                    popUpTo(Routes.WELCOME) { inclusive = true }
                }
            })
        }

        composable(Routes.ACTIVATION) {
            ActivationScreen(onActivated = {
                navController.navigate(Routes.HOME) {
                    popUpTo(Routes.WELCOME) { inclusive = true }
                }
            })
        }

        composable(Routes.HOME) {
            HomeScreen(
                onRequestVpnPermission = onRequestVpnPermission,
                onOpenServers = { navController.navigate(Routes.SERVERS) },
                onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                onOpenAccount = { navController.navigate(Routes.ACCOUNT) },
            )
        }

        composable(Routes.ACCOUNT) {
            AccountScreen(onBack = { navController.popBackStack() })
        }

        composable(Routes.SERVERS) {
            ServersScreen(
                onServerChosen = { navController.popBackStack() },
                onBack = { navController.popBackStack() },
            )
        }

        composable(Routes.SETTINGS) {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onOpenPrivacy = { navController.navigate(Routes.PRIVACY) },
                onOpenAbout = { navController.navigate(Routes.ABOUT) },
                onActivationCleared = {
                    navController.navigate(Routes.WELCOME) {
                        popUpTo(0) { inclusive = true }
                    }
                },
            )
        }

        composable(Routes.PRIVACY) {
            PrivacyScreen(onBack = { navController.popBackStack() })
        }

        composable(Routes.ABOUT) {
            AboutScreen(onBack = { navController.popBackStack() })
        }
    }
}
