package com.cidan.app.data.services

import android.util.Base64
import android.util.Log
import com.cidan.app.data.config.SupabaseConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.Socket
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.net.ssl.SSLSocket
import javax.net.ssl.SSLSocketFactory

/**
 * EmailService provides live OTP email dispatch using Gmail SMTP over SSL / TLS.
 *
 * Configured with credentials provided in SupabaseConfig:
 * - Host: smtp.gmail.com
 * - Ports: 465 (SSL Direct) and 587 (STARTTLS fallback)
 * - Username: unknownhacker3d@gmail.com
 * - Sender: CIDAN Regulatory AI
 */
object EmailService {

    private const val TAG = "CIDAN_EMAIL"

    suspend fun sendOtpEmail(toEmail: String, otpCode: String): Result<Boolean> = withContext(Dispatchers.IO) {
        val cleanEmail = toEmail.trim()
        Log.d(TAG, "Initiating OTP email delivery to: $cleanEmail")

        // 1. Try SSL Direct on Port 465 first (fastest, direct encryption)
        try {
            sendOverSslPort465(cleanEmail, otpCode)
            Log.i(TAG, "OTP email sent successfully via SSL (port 465) to $cleanEmail")
            return@withContext Result.success(true)
        } catch (e: Exception) {
            Log.w(TAG, "Port 465 direct SSL attempt failed: ${e.message}. Trying STARTTLS on port 587...")
        }

        // 2. Fallback to STARTTLS on Port 587
        try {
            sendOverStartTlsPort587(cleanEmail, otpCode)
            Log.i(TAG, "OTP email sent successfully via STARTTLS (port 587) to $cleanEmail")
            return@withContext Result.success(true)
        } catch (e: Exception) {
            Log.e(TAG, "Both SMTP delivery methods failed for $cleanEmail: ${e.message}", e)
            return@withContext Result.failure(e)
        }
    }

    private fun sendOverSslPort465(toEmail: String, otpCode: String) {
        val socketFactory = SSLSocketFactory.getDefault() as SSLSocketFactory
        val socket = socketFactory.createSocket(SupabaseConfig.SMTP_HOST, SupabaseConfig.SMTP_SSL_PORT) as SSLSocket
        socket.soTimeout = 12000

        socket.use { sslSock ->
            val reader = BufferedReader(InputStreamReader(sslSock.inputStream, StandardCharsets.UTF_8))
            val writer = BufferedWriter(OutputStreamWriter(sslSock.outputStream, StandardCharsets.UTF_8))

            fun readResponse(): String {
                val sb = StringBuilder()
                while (true) {
                    val line = reader.readLine() ?: break
                    sb.append(line).append("\n")
                    if (line.length >= 4 && line[3] == ' ') break
                    if (line.length == 3) break
                }
                return sb.toString()
            }

            fun sendCmd(cmd: String): String {
                writer.write(cmd + "\r\n")
                writer.flush()
                return readResponse()
            }

            // 1. Read greeting (220)
            val greeting = readResponse()
            if (!greeting.startsWith("220")) throw IllegalStateException("Bad greeting: $greeting")

            // 2. EHLO
            sendCmd("EHLO localhost")

            // 3. AUTH LOGIN
            val authResp = sendCmd("AUTH LOGIN")
            if (!authResp.startsWith("334")) throw IllegalStateException("AUTH LOGIN rejected: $authResp")

            // 4. Send base64 username
            val userB64 = Base64.encodeToString(
                SupabaseConfig.SMTP_USERNAME.toByteArray(StandardCharsets.UTF_8),
                Base64.NO_WRAP
            )
            val userResp = sendCmd(userB64)
            if (!userResp.startsWith("334")) throw IllegalStateException("Username rejected: $userResp")

            // 5. Send base64 password (stripping spaces)
            val cleanPass = SupabaseConfig.SMTP_PASSWORD.replace(" ", "")
            val passB64 = Base64.encodeToString(
                cleanPass.toByteArray(StandardCharsets.UTF_8),
                Base64.NO_WRAP
            )
            val passResp = sendCmd(passB64)
            if (!passResp.startsWith("235")) throw IllegalStateException("Password rejected: $passResp")

            // 6. MAIL FROM
            val mailFromResp = sendCmd("MAIL FROM:<${SupabaseConfig.SMTP_USERNAME}>")
            if (!mailFromResp.startsWith("250")) throw IllegalStateException("MAIL FROM rejected: $mailFromResp")

            // 7. RCPT TO
            val rcptResp = sendCmd("RCPT TO:<$toEmail>")
            if (!rcptResp.startsWith("250")) throw IllegalStateException("RCPT TO rejected: $rcptResp")

            // 8. DATA
            val dataResp = sendCmd("DATA")
            if (!dataResp.startsWith("354")) throw IllegalStateException("DATA rejected: $dataResp")

            // 9. Send headers and body
            val rfcDate = SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", Locale.US).format(Date())
            val htmlContent = buildOtpHtml(otpCode)

            val emailPayload = buildString {
                append("From: \"${SupabaseConfig.SMTP_SENDER_NAME}\" <${SupabaseConfig.SMTP_USERNAME}>\r\n")
                append("To: <$toEmail>\r\n")
                append("Subject: Your CIDAN Verification Code: $otpCode\r\n")
                append("Date: $rfcDate\r\n")
                append("MIME-Version: 1.0\r\n")
                append("Content-Type: text/html; charset=UTF-8\r\n")
                append("Content-Transfer-Encoding: 8bit\r\n")
                append("\r\n")
                append(htmlContent)
                append("\r\n.\r\n")
            }

            writer.write(emailPayload)
            writer.flush()

            val endResp = readResponse()
            if (!endResp.startsWith("250")) throw IllegalStateException("Sending payload failed: $endResp")

            // 10. QUIT
            sendCmd("QUIT")
        }
    }

    private fun sendOverStartTlsPort587(toEmail: String, otpCode: String) {
        val rawSocket = Socket(SupabaseConfig.SMTP_HOST, SupabaseConfig.SMTP_PORT)
        rawSocket.soTimeout = 12000

        var reader = BufferedReader(InputStreamReader(rawSocket.inputStream, StandardCharsets.UTF_8))
        var writer = BufferedWriter(OutputStreamWriter(rawSocket.outputStream, StandardCharsets.UTF_8))

        fun readResponse(): String {
            val sb = StringBuilder()
            while (true) {
                val line = reader.readLine() ?: break
                sb.append(line).append("\n")
                if (line.length >= 4 && line[3] == ' ') break
                if (line.length == 3) break
            }
            return sb.toString()
        }

        fun sendCmd(cmd: String): String {
            writer.write(cmd + "\r\n")
            writer.flush()
            return readResponse()
        }

        // 1. Initial greeting
        val greeting = readResponse()
        if (!greeting.startsWith("220")) throw IllegalStateException("587 Greeting failed: $greeting")

        // 2. EHLO
        sendCmd("EHLO localhost")

        // 3. STARTTLS
        val tlsResp = sendCmd("STARTTLS")
        if (!tlsResp.startsWith("220")) throw IllegalStateException("STARTTLS failed: $tlsResp")

        // 4. Upgrade socket to SSL
        val sslSocketFactory = SSLSocketFactory.getDefault() as SSLSocketFactory
        val sslSocket = sslSocketFactory.createSocket(
            rawSocket,
            SupabaseConfig.SMTP_HOST,
            SupabaseConfig.SMTP_PORT,
            true
        ) as SSLSocket
        sslSocket.startHandshake()

        reader = BufferedReader(InputStreamReader(sslSocket.inputStream, StandardCharsets.UTF_8))
        writer = BufferedWriter(OutputStreamWriter(sslSocket.outputStream, StandardCharsets.UTF_8))

        // 5. Post-TLS EHLO
        sendCmd("EHLO localhost")

        // 6. AUTH LOGIN
        val authResp = sendCmd("AUTH LOGIN")
        if (!authResp.startsWith("334")) throw IllegalStateException("587 AUTH LOGIN failed: $authResp")

        val userB64 = Base64.encodeToString(
            SupabaseConfig.SMTP_USERNAME.toByteArray(StandardCharsets.UTF_8),
            Base64.NO_WRAP
        )
        val userResp = sendCmd(userB64)
        if (!userResp.startsWith("334")) throw IllegalStateException("587 User failed: $userResp")

        val cleanPass = SupabaseConfig.SMTP_PASSWORD.replace(" ", "")
        val passB64 = Base64.encodeToString(
            cleanPass.toByteArray(StandardCharsets.UTF_8),
            Base64.NO_WRAP
        )
        val passResp = sendCmd(passB64)
        if (!passResp.startsWith("235")) throw IllegalStateException("587 Pass failed: $passResp")

        // 7. MAIL FROM & RCPT TO
        val mailFromResp = sendCmd("MAIL FROM:<${SupabaseConfig.SMTP_USERNAME}>")
        if (!mailFromResp.startsWith("250")) throw IllegalStateException("587 MAIL FROM failed: $mailFromResp")

        val rcptResp = sendCmd("RCPT TO:<$toEmail>")
        if (!rcptResp.startsWith("250")) throw IllegalStateException("587 RCPT TO failed: $rcptResp")

        // 8. DATA
        val dataResp = sendCmd("DATA")
        if (!dataResp.startsWith("354")) throw IllegalStateException("587 DATA failed: $dataResp")

        val rfcDate = SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", Locale.US).format(Date())
        val htmlContent = buildOtpHtml(otpCode)

        val emailPayload = buildString {
            append("From: \"${SupabaseConfig.SMTP_SENDER_NAME}\" <${SupabaseConfig.SMTP_USERNAME}>\r\n")
            append("To: <$toEmail>\r\n")
            append("Subject: Your CIDAN Verification Code: $otpCode\r\n")
            append("Date: $rfcDate\r\n")
            append("MIME-Version: 1.0\r\n")
            append("Content-Type: text/html; charset=UTF-8\r\n")
            append("Content-Transfer-Encoding: 8bit\r\n")
            append("\r\n")
            append(htmlContent)
            append("\r\n.\r\n")
        }

        writer.write(emailPayload)
        writer.flush()

        val endResp = readResponse()
        if (!endResp.startsWith("250")) throw IllegalStateException("587 Payload failed: $endResp")

        sendCmd("QUIT")
        sslSocket.close()
        rawSocket.close()
    }

    private fun buildOtpHtml(otpCode: String): String {
        return """
            <!DOCTYPE html>
            <html>
            <head>
              <meta charset="utf-8">
              <style>
                body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; background-color: #F8FAFC; margin: 0; padding: 24px; color: #1E293B; }
                .card { max-width: 500px; margin: 0 auto; background: #FFFFFF; border-radius: 16px; border: 1px solid #E2E8F0; overflow: hidden; box-shadow: 0 4px 18px rgba(0,0,0,0.06); }
                .header { background: linear-gradient(135deg, #0B192C 0%, #1E3E62 100%); padding: 30px 24px; text-align: center; }
                .header h1 { margin: 0; color: #FFFFFF; font-size: 24px; letter-spacing: 0.5px; font-weight: 700; }
                .header p { margin: 6px 0 0 0; color: #94A3B8; font-size: 13px; letter-spacing: 1px; text-transform: uppercase; }
                .content { padding: 32px 28px; text-align: center; }
                .headline { font-size: 18px; font-weight: 600; color: #0F172A; margin-bottom: 12px; }
                .desc { font-size: 14px; color: #64748B; line-height: 1.6; margin-bottom: 24px; }
                .otp-box { display: inline-block; background: #F1F5F9; border: 2px dashed #0066FF; border-radius: 12px; padding: 16px 36px; margin-bottom: 24px; }
                .otp-code { font-size: 36px; font-weight: 800; letter-spacing: 8px; color: #0066FF; font-family: monospace, monospace; }
                .security-alert { background: #FEF3C7; border: 1px solid #FCD34D; border-radius: 8px; padding: 12px 16px; font-size: 13px; color: #92400E; margin-bottom: 24px; text-align: left; }
                .footer { background: #F8FAFC; padding: 20px 24px; text-align: center; border-top: 1px solid #E2E8F0; font-size: 12px; color: #94A3B8; }
              </style>
            </head>
            <body>
              <div class="card">
                <div class="header">
                  <h1>CIDAN</h1>
                  <p>Regulatory AI & Compliance Engine</p>
                </div>
                <div class="content">
                  <div class="headline">Official Authorization Code</div>
                  <div class="desc">
                    A verification request was initiated for your bank email.<br>
                    Enter this one-time code to authenticate your session:
                  </div>
                  <div class="otp-box">
                    <div class="otp-code">$otpCode</div>
                  </div>
                  <div class="security-alert">
                    <strong>Security Notice:</strong> This code will expire in <strong>10 minutes</strong>. Never share this code with anyone, including bank representatives or CIDAN personnel.
                  </div>
                </div>
                <div class="footer">
                  This is an automated system security notification from CIDAN Regulatory AI.<br>
                  © 2026 CIDAN Inc. All rights reserved.
                </div>
              </div>
            </body>
            </html>
        """.trimIndent()
    }
}
