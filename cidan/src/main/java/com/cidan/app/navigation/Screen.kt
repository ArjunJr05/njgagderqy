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
    object ComplianceDashboard : Screen("compliance_dashboard/{email}/{fileName}?showPdfFirst={showPdfFirst}") {
        fun createRoute(
            email: String,
            fileName: String = "CIDAN_Regulatory_Circular.pdf",
            showPdfFirst: Boolean = false
        ) = "compliance_dashboard/$email/${android.net.Uri.encode(fileName)}?showPdfFirst=$showPdfFirst"
    }
}
