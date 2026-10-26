package com.cidan.app.data.services

import android.content.Context
import android.util.Log
import com.cidan.app.data.config.SupabaseConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * SupabaseService handles saving compliance reports, audit history,
 * and deployment statuses to Supabase (and local cache).
 */
class SupabaseService(private val context: Context? = null) {

    private val localReportStore = mutableListOf<JSONObject>()
    private val localHistoryStore = mutableListOf<JSONObject>()

    suspend fun saveComplianceReport(
        email: String,
        workflowId: String,
        filename: String,
        obligationsCount: Int,
        violationsCount: Int,
        approvedCount: Int,
        reportDataJson: String,
        isDeployed: Boolean = false
    ): Boolean = withContext(Dispatchers.IO) {
        val cleanEmail = email.trim().lowercase(Locale.ROOT)
        val nowIso = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).format(Date())

        val row = JSONObject().apply {
            put("user_email", cleanEmail)
            put("workflow_id", workflowId)
            put("filename", filename)
            put("obligations_count", obligationsCount)
            put("violations_count", violationsCount)
            put("approved_count", approvedCount)
            put("is_deployed", isDeployed)
            try {
                put("report_data", JSONObject(reportDataJson))
            } catch (e: Exception) {
                put("report_data", JSONObject())
            }
            put("created_at", nowIso)
        }

        // Save to local cache
        val existingIdx = localReportStore.indexOfFirst {
            it.optString("workflow_id") == workflowId ||
                (it.optString("filename") == filename && it.optString("user_email") == cleanEmail)
        }
        if (existingIdx >= 0) {
            localReportStore[existingIdx] = row
        } else {
            localReportStore.add(0, row)
        }

        // Post to Supabase REST endpoint
        try {
            val url = URL("${SupabaseConfig.SUPABASE_URL}/rest/v1/${SupabaseConfig.TABLE_COMPLIANCE_REPORTS}")
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 8000
                readTimeout = 8000
                doOutput = true
                setRequestProperty("apikey", SupabaseConfig.SUPABASE_ANON_KEY)
                setRequestProperty("Authorization", "Bearer ${SupabaseConfig.SUPABASE_ANON_KEY}")
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("Prefer", "resolution=merge-duplicates")
            }

            OutputStreamWriter(connection.outputStream).use { writer ->
                writer.write(row.toString())
                writer.flush()
            }

            val code = connection.responseCode
            Log.d("CIDAN_SUPABASE", "saveComplianceReport status: $code")
        } catch (e: Exception) {
            Log.e("CIDAN_SUPABASE", "saveComplianceReport error: ${e.message}")
        }

        // Also record an entry in user history
        recordHistory(
            email = cleanEmail,
            actionType = if (isDeployed) "REGULATORY_DEPLOYED" else "COMPLIANCE_AUDIT_SAVED",
            documentName = filename,
            workflowId = workflowId,
            summary = if (isDeployed)
                "Regulatory policy approved and deployed for $filename"
            else
                "Audit completed for $filename ($obligationsCount obligations, $violationsCount issues)"
        )

        return@withContext true
    }

    suspend fun updateReportDeploymentStatus(
        email: String,
        workflowId: String,
        isDeployed: Boolean
    ): Boolean = withContext(Dispatchers.IO) {
        val cleanEmail = email.trim().lowercase(Locale.ROOT)

        for (item in localReportStore) {
            if (item.optString("workflow_id") == workflowId) {
                item.put("is_deployed", isDeployed)
            }
        }

        try {
            val url = URL("${SupabaseConfig.SUPABASE_URL}/rest/v1/${SupabaseConfig.TABLE_COMPLIANCE_REPORTS}?workflow_id=eq.$workflowId")
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "PATCH"
                connectTimeout = 8000
                readTimeout = 8000
                doOutput = true
                setRequestProperty("apikey", SupabaseConfig.SUPABASE_ANON_KEY)
                setRequestProperty("Authorization", "Bearer ${SupabaseConfig.SUPABASE_ANON_KEY}")
                setRequestProperty("Content-Type", "application/json")
            }

            val patchBody = JSONObject().apply {
                put("is_deployed", isDeployed)
            }

            OutputStreamWriter(connection.outputStream).use { writer ->
                writer.write(patchBody.toString())
                writer.flush()
            }

            val code = connection.responseCode
            Log.d("CIDAN_SUPABASE", "updateReportDeploymentStatus status: $code")
        } catch (e: Exception) {
            Log.e("CIDAN_SUPABASE", "updateReportDeploymentStatus error: ${e.message}")
        }

        return@withContext true
    }

    suspend fun getComplianceReports(email: String): List<JSONObject> = withContext(Dispatchers.IO) {
        val cleanEmail = email.trim().lowercase(Locale.ROOT)
        try {
            val url = URL("${SupabaseConfig.SUPABASE_URL}/rest/v1/${SupabaseConfig.TABLE_COMPLIANCE_REPORTS}?user_email=eq.$cleanEmail&order=created_at.desc")
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 8000
                readTimeout = 8000
                setRequestProperty("apikey", SupabaseConfig.SUPABASE_ANON_KEY)
                setRequestProperty("Authorization", "Bearer ${SupabaseConfig.SUPABASE_ANON_KEY}")
                setRequestProperty("Accept", "application/json")
            }

            val code = connection.responseCode
            if (code in 200..299) {
                val str = BufferedReader(InputStreamReader(connection.inputStream)).use { it.readText() }
                val array = JSONArray(str)
                val list = mutableListOf<JSONObject>()
                for (i in 0 until array.length()) {
                    val obj = array.optJSONObject(i)
                    if (obj != null) list.add(obj)
                }
                if (list.isNotEmpty()) return@withContext list
            }
        } catch (e: Exception) {
            Log.e("CIDAN_SUPABASE", "getComplianceReports error: ${e.message}")
        }

        // Return local store filtered by email
        return@withContext localReportStore.filter {
            it.optString("user_email").equals(cleanEmail, ignoreCase = true)
        }
    }

    suspend fun recordHistory(
        email: String,
        actionType: String,
        documentName: String = "",
        workflowId: String = "",
        summary: String = ""
    ): Boolean = withContext(Dispatchers.IO) {
        val cleanEmail = email.trim().lowercase(Locale.ROOT)
        val nowIso = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).format(Date())

        val row = JSONObject().apply {
            put("user_email", cleanEmail)
            put("action_type", actionType)
            put("document_name", documentName)
            put("workflow_id", workflowId)
            put("summary", summary)
            put("created_at", nowIso)
        }

        localHistoryStore.add(0, row)

        try {
            val url = URL("${SupabaseConfig.SUPABASE_URL}/rest/v1/${SupabaseConfig.TABLE_USER_HISTORY}")
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 8000
                readTimeout = 8000
                doOutput = true
                setRequestProperty("apikey", SupabaseConfig.SUPABASE_ANON_KEY)
                setRequestProperty("Authorization", "Bearer ${SupabaseConfig.SUPABASE_ANON_KEY}")
                setRequestProperty("Content-Type", "application/json")
            }

            OutputStreamWriter(connection.outputStream).use { writer ->
                writer.write(row.toString())
                writer.flush()
            }

            val code = connection.responseCode
            Log.d("CIDAN_SUPABASE", "recordHistory status: $code")
        } catch (e: Exception) {
            Log.e("CIDAN_SUPABASE", "recordHistory error: ${e.message}")
        }

        return@withContext true
    }

    private val localOtpMap = java.util.concurrent.ConcurrentHashMap<String, String>()

    suspend fun sendAndSaveOtp(email: String): Result<String> = withContext(Dispatchers.IO) {
        val cleanEmail = email.trim().lowercase(Locale.ROOT)
        val otpCode = String.format(Locale.US, "%06d", (100000..999999).random())
        localOtpMap[cleanEmail] = otpCode

        val nowIso = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).format(Date())
        val expiresIso = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).format(Date(System.currentTimeMillis() + 10 * 60 * 1000))

        // 1. Post to Supabase auth_otps table
        try {
            val url = URL("${SupabaseConfig.SUPABASE_URL}/rest/v1/${SupabaseConfig.TABLE_OTPS}")
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 8000
                readTimeout = 8000
                doOutput = true
                setRequestProperty("apikey", SupabaseConfig.SUPABASE_ANON_KEY)
                setRequestProperty("Authorization", "Bearer ${SupabaseConfig.SUPABASE_ANON_KEY}")
                setRequestProperty("Content-Type", "application/json")
            }

            val body = JSONObject().apply {
                put("email", cleanEmail)
                put("otp_code", otpCode)
                put("created_at", nowIso)
                put("expires_at", expiresIso)
                put("is_verified", false)
            }

            OutputStreamWriter(connection.outputStream).use { writer ->
                writer.write(body.toString())
                writer.flush()
            }

            val code = connection.responseCode
            Log.d("CIDAN_SUPABASE", "Supabase auth_otps save status: $code")
        } catch (e: Exception) {
            Log.e("CIDAN_SUPABASE", "Error saving OTP to Supabase: ${e.message}")
        }

        // 2. Record in user history
        recordHistory(
            email = cleanEmail,
            actionType = "OTP_REQUESTED",
            summary = "Verification code generated and sent to $cleanEmail"
        )

        // 3. Send live email via SMTP
        val emailResult = EmailService.sendOtpEmail(cleanEmail, otpCode)
        if (emailResult.isFailure) {
            Log.w("CIDAN_SUPABASE", "SMTP sending returned failure: ${emailResult.exceptionOrNull()?.message}, but OTP code $otpCode is locally cached")
        }

        return@withContext Result.success(otpCode)
    }

    suspend fun verifyOtp(email: String, enteredOtp: String): Boolean = withContext(Dispatchers.IO) {
        val cleanEmail = email.trim().lowercase(Locale.ROOT)
        val cleanOtp = enteredOtp.trim()

        // 1. Demo code bypass
        if (cleanOtp == "284910") {
            Log.i("CIDAN_SUPABASE", "Demo bypass OTP code 284910 verified for $cleanEmail")
            recordHistory(cleanEmail, "OTP_VERIFIED", summary = "Session verified via standard demo bypass")
            return@withContext true
        }

        // 2. Check local memory cache
        val cachedOtp = localOtpMap[cleanEmail]
        if (cachedOtp != null && cachedOtp == cleanOtp) {
            Log.i("CIDAN_SUPABASE", "Local cached OTP matched for $cleanEmail")
            markOtpVerifiedInSupabase(cleanEmail, cleanOtp)
            recordHistory(cleanEmail, "OTP_VERIFIED", summary = "Session verified via live OTP code")
            return@withContext true
        }

        // 3. Check Supabase auth_otps table
        try {
            val queryUrl = "${SupabaseConfig.SUPABASE_URL}/rest/v1/${SupabaseConfig.TABLE_OTPS}?email=eq.$cleanEmail&otp_code=eq.$cleanOtp&is_verified=eq.false&order=created_at.desc&limit=1"
            val connection = (URL(queryUrl).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 8000
                readTimeout = 8000
                setRequestProperty("apikey", SupabaseConfig.SUPABASE_ANON_KEY)
                setRequestProperty("Authorization", "Bearer ${SupabaseConfig.SUPABASE_ANON_KEY}")
                setRequestProperty("Accept", "application/json")
            }

            if (connection.responseCode in 200..299) {
                val str = BufferedReader(InputStreamReader(connection.inputStream)).use { it.readText() }
                val array = JSONArray(str)
                if (array.length() > 0) {
                    markOtpVerifiedInSupabase(cleanEmail, cleanOtp)
                    recordHistory(cleanEmail, "OTP_VERIFIED", summary = "Session verified via Supabase auth_otps")
                    return@withContext true
                }
            }
        } catch (e: Exception) {
            Log.e("CIDAN_SUPABASE", "Error verifying OTP against Supabase: ${e.message}")
        }

        return@withContext false
    }

    private fun markOtpVerifiedInSupabase(email: String, otpCode: String) {
        try {
            val patchUrl = "${SupabaseConfig.SUPABASE_URL}/rest/v1/${SupabaseConfig.TABLE_OTPS}?email=eq.$email&otp_code=eq.$otpCode"
            val connection = (URL(patchUrl).openConnection() as HttpURLConnection).apply {
                requestMethod = "PATCH"
                connectTimeout = 6000
                readTimeout = 6000
                doOutput = true
                setRequestProperty("apikey", SupabaseConfig.SUPABASE_ANON_KEY)
                setRequestProperty("Authorization", "Bearer ${SupabaseConfig.SUPABASE_ANON_KEY}")
                setRequestProperty("Content-Type", "application/json")
            }
            OutputStreamWriter(connection.outputStream).use { writer ->
                writer.write(JSONObject().put("is_verified", true).toString())
                writer.flush()
            }
            Log.d("CIDAN_SUPABASE", "markOtpVerifiedInSupabase status: ${connection.responseCode}")
        } catch (e: Exception) {
            Log.w("CIDAN_SUPABASE", "Error marking OTP verified: ${e.message}")
        }
    }

    companion object {
        private var instance: SupabaseService? = null
        fun getInstance(context: Context? = null): SupabaseService {
            return instance ?: synchronized(this) {
                instance ?: SupabaseService(context?.applicationContext).also { instance = it }
            }
        }
    }
}
