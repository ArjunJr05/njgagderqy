package com.cidan.app.navigation

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Otp : Screen("otp/{email}") {
        fun createRoute(email: String) = "otp/$email"
    }
    object AuthSuccess : Screen("auth_success/{email}") {
        fun createRoute(email: String) = "auth_success/$email"
    }
    object UploadDocument : Screen("upload_document/{email}") {
        fun createRoute(email: String) = "upload_document/$email"
    }
}
