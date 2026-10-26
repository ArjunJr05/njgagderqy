package com.cidan.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.MailOutline
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import com.cidan.app.R
import com.cidan.app.data.services.SupabaseService
import com.cidan.app.ui.components.AuthAppBar
import com.cidan.app.ui.components.CustomButton
import com.cidan.app.ui.theme.AppColors
import com.cidan.app.ui.theme.Poppins
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import android.util.Log

import androidx.compose.foundation.layout.ExperimentalLayoutApi

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LoginScreen(
    onNavigateToOtp: (email: String) -> Unit
) {
    val context = LocalContext.current
    var email by remember { mutableStateOf("ravi.sharma@abcbank.com") }
    var emailError by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    fun validateAndProceed() {
        val trimmed = email.trim()
        if (trimmed.isEmpty()) {
            emailError = "Please enter your bank email address"
            return
        }
        val emailRegex = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$".toRegex()
        if (!trimmed.matches(emailRegex)) {
            emailError = "Please enter a valid official bank email"
            return
        }
        emailError = null
        isLoading = true

        coroutineScope.launch {
            try {
                // Dispatch live verification email over SMTP & save to Supabase
                SupabaseService.getInstance(context).sendAndSaveOtp(trimmed)
            } catch (e: Exception) {
                Log.e("CIDAN_LOGIN", "Error during OTP dispatch: ${e.message}")
            } finally {
                isLoading = false
                onNavigateToOtp(trimmed)
            }
        }
    }

    val isKeyboardOpen = WindowInsets.isImeVisible

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .imePadding()
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Top Hero Illustration: Secure Banking Made Simple
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.secure_banking_made_simple),
                    contentDescription = "Secure Banking Made Simple",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    alignment = Alignment.TopCenter
                )
            }

            // Overlapping Curved Container with Form Controls (-14.dp translation)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .offset(y = (-14).dp)
                    .shadow(
                        elevation = 18.dp,
                        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
                        spotColor = Color(0x20000000),
                        ambientColor = Color(0x15000000)
                    )
                    .clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
                    .background(Color.White)
                    .padding(horizontal = 24.dp, vertical = if (isKeyboardOpen) 10.dp else 14.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Top grey pill indicator handle
                    Box(
                        modifier = Modifier
                            .width(48.dp)
                            .height(4.5.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(Color(0xFFE2E8F0))
                    )

                    Spacer(modifier = Modifier.height(if (isKeyboardOpen) 10.dp else 18.dp))

                    // Title
                    Text(
                        text = "Login with your bank email",
                        fontFamily = Poppins,
                        fontSize = if (isKeyboardOpen) 18.sp else 21.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A),
                        letterSpacing = (-0.3).sp,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(if (isKeyboardOpen) 2.dp else 4.dp))

                    // Subtitle
                    Text(
                        text = "Enter your official bank email to receive a secure OTP code",
                        fontFamily = Poppins,
                        fontSize = if (isKeyboardOpen) 12.sp else 13.5.sp,
                        color = Color(0xFF64748B),
                        lineHeight = if (isKeyboardOpen) 16.sp else 19.sp,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(if (isKeyboardOpen) 10.dp else 18.dp))

                    // Email Input Container with rounded purple badge icon
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.White)
                            .border(
                                width = 1.3.dp,
                                color = if (emailError != null) AppColors.Error else Color(0xFFE2E8F0),
                                shape = RoundedCornerShape(16.dp)
                            )
                            .padding(horizontal = 10.dp, vertical = 8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // Soft light-purple square badge with mail icon
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFFEEF2FF)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.MailOutline,
                                    contentDescription = null,
                                    tint = Color(0xFF4F46E5),
                                    modifier = Modifier.size(22.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Box(modifier = Modifier.weight(1f)) {
                                if (email.isEmpty()) {
                                    Text(
                                        text = "name@yourbank.com",
                                        fontFamily = Poppins,
                                        fontSize = 15.sp,
                                        color = Color(0xFF94A3B8)
                                    )
                                }
                                BasicTextField(
                                    value = email,
                                    onValueChange = {
                                        email = it
                                        if (emailError != null) emailError = null
                                    },
                                    singleLine = true,
                                    textStyle = androidx.compose.ui.text.TextStyle(
                                        fontFamily = Poppins,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color(0xFF0F172A)
                                    ),
                                    cursorBrush = SolidColor(AppColors.Primary),
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Email,
                                        imeAction = ImeAction.Done
                                    ),
                                    keyboardActions = KeyboardActions(onDone = { validateAndProceed() }),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }

                            if (email.isNotEmpty()) {
                                IconButton(
                                    onClick = {
                                        email = ""
                                        emailError = null
                                    },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Close,
                                        contentDescription = "Clear",
                                        tint = Color(0xFF94A3B8),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Validation error notice
                    AnimatedVisibility(
                        visible = emailError != null,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp, start = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.ErrorOutline,
                                contentDescription = null,
                                tint = AppColors.Error,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = emailError ?: "",
                                fontFamily = Poppins,
                                fontSize = 12.5.sp,
                                color = AppColors.Error
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(if (isKeyboardOpen) 12.dp else 18.dp))

                    // Primary Button: Send OTP Code
                    CustomButton(
                        text = "Send OTP Code",
                        icon = Icons.AutoMirrored.Rounded.ArrowForward,
                        isLoading = isLoading,
                        onClick = { validateAndProceed() }
                    )

                    if (!isKeyboardOpen) {
                        Spacer(modifier = Modifier.height(16.dp))

                        // Security/Encryption Note Card
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(AppColors.PrimarySoft)
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Shield,
                                contentDescription = null,
                                tint = AppColors.Primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Your information is encrypted and securely protected.",
                                fontFamily = Poppins,
                                fontSize = 12.sp,
                                color = AppColors.TextSecondary,
                                lineHeight = 16.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                }
            }
        }

        // Top App Bar with Help button
        AuthAppBar(
            modifier = Modifier.align(Alignment.TopCenter),
            showBackButton = false
        )
    }
}
