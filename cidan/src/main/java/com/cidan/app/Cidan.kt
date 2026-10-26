package com.cidan.app

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.cidan.app.navigation.CidanNavGraph
import com.cidan.app.navigation.Screen
import com.cidan.app.ui.theme.AppColors
import com.cidan.app.ui.theme.CidanTheme

/**
 * CIDAN Regulatory AI Compliance SDK for Android.
 *
 * Provides single-line initialization methods for Jetpack Compose and traditional Android apps:
 *
 * In Jetpack Compose:
 * ```kotlin
 * CidanApp()
 * // or
 * Cidan.application()
 * ```
 *
 * In traditional Android Activities:
 * ```kotlin
 * Cidan.launch(context)
 * ```
 */
object Cidan {

    /**
     * Composable function to integrate CIDAN into any Compose hierarchy.
     */
    @Composable
    fun application(
        navController: NavHostController = rememberNavController(),
        startDestination: String = Screen.Login.route
    ) {
        CidanTheme {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = AppColors.Background
            ) {
                CidanNavGraph(
                    navController = navController,
                    startDestination = startDestination
                )
            }
        }
    }

    /**
     * Launches CIDAN in a dedicated full-screen Activity.
     */
    fun launch(context: Context) {
        val intent = Intent(context, CidanActivity::class.java)
        context.startActivity(intent)
    }
}

/**
 * Top-level Composable alias for [Cidan.application].
 */
@Composable
fun CidanApp(
    navController: NavHostController = rememberNavController(),
    startDestination: String = Screen.Login.route
) {
    Cidan.application(
        navController = navController,
        startDestination = startDestination
    )
}
