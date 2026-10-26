package com.cidan.app.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.cidan.app.ui.screens.LoginScreen
import com.cidan.app.ui.screens.OtpScreen
import com.cidan.app.ui.screens.UploadDocumentScreen

@Composable
fun CidanNavGraph(
    navController: NavHostController,
    startDestination: String = Screen.Login.route
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        // Login Screen
        composable(
            route = Screen.Login.route,
            exitTransition = {
                slideOutOfContainer(
                    AnimatedContentTransitionScope.SlideDirection.Left,
                    animationSpec = tween(350)
                ) + fadeOut()
            },
            popEnterTransition = {
                slideIntoContainer(
                    AnimatedContentTransitionScope.SlideDirection.Right,
                    animationSpec = tween(350)
                ) + fadeIn()
            }
        ) {
            LoginScreen(
                onNavigateToOtp = { email ->
                    navController.navigate(Screen.Otp.createRoute(email))
                }
            )
        }

        // OTP Screen
        composable(
            route = Screen.Otp.route,
            arguments = listOf(navArgument("email") { type = NavType.StringType }),
            enterTransition = {
                slideIntoContainer(
                    AnimatedContentTransitionScope.SlideDirection.Left,
                    animationSpec = tween(350)
                ) + fadeIn()
            },
            exitTransition = {
                fadeOut(animationSpec = tween(250))
            },
            popExitTransition = {
                slideOutOfContainer(
                    AnimatedContentTransitionScope.SlideDirection.Right,
                    animationSpec = tween(350)
                ) + fadeOut()
            }
        ) { backStackEntry ->
            val email = backStackEntry.arguments?.getString("email") ?: "ravi.sharma@abcbank.com"
            OtpScreen(
                email = email,
                onBackClick = { navController.popBackStack() },
                onNavigateToSuccess = { verifiedEmail ->
                    // Navigate directly to Upload Regulatory Document in place of Verification Successful
                    navController.navigate(Screen.UploadDocument.createRoute(verifiedEmail)) {
                        popUpTo(Screen.Login.route) { inclusive = false }
                    }
                }
            )
        }

        // Upload Regulatory Document Screen (Directly follows OTP verification)
        composable(
            route = Screen.UploadDocument.route,
            arguments = listOf(navArgument("email") { type = NavType.StringType }),
            enterTransition = {
                slideIntoContainer(
                    AnimatedContentTransitionScope.SlideDirection.Left,
                    animationSpec = tween(350)
                ) + fadeIn()
            },
            popExitTransition = {
                slideOutOfContainer(
                    AnimatedContentTransitionScope.SlideDirection.Right,
                    animationSpec = tween(350)
                ) + fadeOut()
            }
        ) { backStackEntry ->
            val email = backStackEntry.arguments?.getString("email") ?: "ravi.sharma@abcbank.com"
            UploadDocumentScreen(
                email = email,
                onCloseClick = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
                onNavigateToDashboard = { userEmail, file, showPdfFirst ->
                    navController.navigate(Screen.ComplianceDashboard.createRoute(userEmail, file, showPdfFirst))
                }
            )
        }

        // Compliance Dashboard Screen - The Last Page
        composable(
            route = Screen.ComplianceDashboard.route,
            arguments = listOf(
                navArgument("email") { type = NavType.StringType },
                navArgument("fileName") { type = NavType.StringType },
                navArgument("showPdfFirst") {
                    type = NavType.BoolType
                    defaultValue = false
                }
            ),
            enterTransition = {
                slideIntoContainer(
                    AnimatedContentTransitionScope.SlideDirection.Left,
                    animationSpec = tween(350)
                ) + fadeIn()
            },
            popExitTransition = {
                slideOutOfContainer(
                    AnimatedContentTransitionScope.SlideDirection.Right,
                    animationSpec = tween(350)
                ) + fadeOut()
            }
        ) { backStackEntry ->
            val email = backStackEntry.arguments?.getString("email") ?: "ravi.sharma@abcbank.com"
            val rawFileName = backStackEntry.arguments?.getString("fileName") ?: "CIDAN_Regulatory_Circular.pdf"
            val showPdfFirst = backStackEntry.arguments?.getBoolean("showPdfFirst") ?: false
            val decodedFileName = try {
                android.net.Uri.decode(rawFileName)
            } catch (e: Exception) {
                rawFileName
            }

            com.cidan.app.ui.screens.ComplianceDashboardScreen(
                email = email,
                fileName = decodedFileName,
                initialShowPdfModal = showPdfFirst,
                onBackClick = { navController.popBackStack() }
            )
        }
    }
}
