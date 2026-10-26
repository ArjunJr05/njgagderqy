package com.cidan.app.data.config

/**
 * Supabase and Virtusa Agent Backend Configuration
 *
 * Configured with live Supabase Project and Virtusa AI Agent endpoints
 * matching the reference implementation in D:\cidan.
 */
object SupabaseConfig {
    // =========================================================================
    // 1. SUPABASE PROJECT CREDENTIALS (Project Ref: vettckrwudnnvoalvjno)
    // =========================================================================
    const val SUPABASE_URL = "https://vettckrwudnnvoalvjno.supabase.co"
    const val SUPABASE_ANON_KEY =
        "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InZldHRja3J3dWRubnZvYWx2am5vIiwicm9sZSI6ImFub24iLCJpYXQiOjE3OTMwMDkyNTIsImV4cCI6MjEwODU4NTI1Mn0.rDEb0UWfphz3S4Dt8N1ZkeCx3TIuBt2Gw4WKz1XPW_w"

    // =========================================================================
    // 2. TABLE NAMES
    // =========================================================================
    const val TABLE_OTPS = "auth_otps"
    const val TABLE_USER_HISTORY = "user_history"
    const val TABLE_COMPLIANCE_REPORTS = "compliance_reports"

    // =========================================================================
    // 3. VIRTUSA AI AGENT BACKEND CREDENTIALS
    // =========================================================================
    const val AI_BASE_URL = "https://virtusa-agent.onrender.com"
    const val AI_AUTH_TOKEN = "0Zh3yr-Srn3juPBBl3IUSgV_2HDz-4WIrmP1uyDC_5E"

    // =========================================================================
    // 4. SMTP CREDENTIALS FOR LIVE OTP EMAILS
    // =========================================================================
    const val SMTP_HOST = "smtp.gmail.com"
    const val SMTP_PORT = 587
    const val SMTP_SSL_PORT = 465
    const val SMTP_USERNAME = "unknownhacker3d@gmail.com"
    const val SMTP_PASSWORD = "munr mayy tdfc mzyl"
    const val SMTP_SENDER_NAME = "CIDAN Regulatory AI"
}

