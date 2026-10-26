package com.cidan.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import com.cidan.app.R
import com.cidan.app.data.services.SupabaseService
import com.cidan.app.ui.components.AuthAppBar
import com.cidan.app.ui.components.CustomButton
import com.cidan.app.ui.components.OtpInputRow
import com.cidan.app.ui.components.SecurityNoticeCard
import com.cidan.app.ui.theme.AppColors
import com.cidan.app.ui.theme.Poppins
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

import androidx.compose.foundation.layout.ExperimentalLayoutApi

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OtpScreen(
    email: String,
    onBackClick: () -> Unit,
    onNavigateToSuccess: (email: String) -> Unit
) {
    val context = LocalContext.current
    var otpValue by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isVerifying by remember { mutableStateOf(false) }
    var countdown by remember { mutableIntStateOf(45) }
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Countdown timer
    LaunchedEffect(countdown) {
        if (countdown > 0) {
            delay(1000)
            countdown--
        }
    }

    fun resendOtp() {
        countdown = 45
        otpValue = ""
        errorMessage = null
        coroutineScope.launch {
            snackbarHostState.showSnackbar("Sending new verification code to $email...")
            val result = SupabaseService.getInstance(context).sendAndSaveOtp(email)
            if (result.isSuccess) {
                snackbarHostState.showSnackbar("New OTP sent! Please check your email inbox.")
            } else {
                snackbarHostState.showSnackbar("Failed to dispatch email: ${result.exceptionOrNull()?.message ?: "Check network"}")
            }
        }
    }

    fun verifyOtp() {
        if (otpValue.length < 6) {
            errorMessage = "Please enter all 6 digits of the OTP"
            return
        }
        errorMessage = null
        isVerifying = true

        coroutineScope.launch {
            val verified = SupabaseService.getInstance(context).verifyOtp(email, otpValue)
            isVerifying = false
            if (verified) {
                onNavigateToSuccess(email)
            } else {
                errorMessage = "Invalid verification code. Please check your email or use 284910."
            }
        }
    }

    val timerText = "00:${countdown.toString().padStart(2, '0')}"

    val isKeyboardOpen = WindowInsets.isImeVisible

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .imePadding()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top Hero Illustration: Verify Your Email with Cidan
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.verify_your_email_with_cidan),
                    contentDescription = "Verify Your Email with Cidan",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    alignment = Alignment.TopCenter
                )
            }

            // Overlapping Curved Container with Content
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .offset(y = (-16).dp)
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
                    // Drag pill indicator handle
                    Box(
                        modifier = Modifier
                            .width(48.dp)
                            .height(4.5.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(Color(0xFFE2E8F0))
                    )

                    Spacer(modifier = Modifier.height(if (isKeyboardOpen) 8.dp else 16.dp))

                    // Subtitle with highlighted user email
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "We've sent a 6-digit code to",
                            fontFamily = Poppins,
                            fontSize = 13.5.sp,
                            color = Color(0xFF64748B)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = email,
                            fontFamily = Poppins,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = AppColors.Primary
                        )
                    }

                    Spacer(modifier = Modifier.height(if (isKeyboardOpen) 10.dp else 18.dp))

                    // 6-digit OTP Row
                    OtpInputRow(
                        otp = otpValue,
                        onOtpChange = {
                            otpValue = it
                            if (errorMessage != null) errorMessage = null
                        },
                        onComplete = { verifyOtp() }
                    )

                    // Error text
                    AnimatedVisibility(
                        visible = errorMessage != null,
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
                                text = errorMessage ?: "",
                                fontFamily = Poppins,
                                fontSize = 12.5.sp,
                                color = AppColors.Error
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Resend countdown timer row
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        if (countdown > 0) {
                            Text(
                                text = "Resend OTP in ",
                                fontFamily = Poppins,
                                fontSize = 13.sp,
                                color = Color(0xFF64748B)
                            )
                            Text(
                                text = timerText,
                                fontFamily = Poppins,
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = AppColors.Primary
                            )
                        } else {
                            Text(
                                text = "Didn't receive code? ",
                                fontFamily = Poppins,
                                fontSize = 13.sp,
                                color = Color(0xFF64748B)
                            )
                            Text(
                                text = "Resend OTP",
                                fontFamily = Poppins,
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = AppColors.Primary,
                                modifier = Modifier.clickable { resendOtp() }
                            )
                        }
                    }

                    if (!isKeyboardOpen) {
                        Spacer(modifier = Modifier.height(14.dp))

                        // Expiry / Security notice card
                        SecurityNoticeCard()
                    }

                    Spacer(modifier = Modifier.height(if (isKeyboardOpen) 10.dp else 18.dp))

                    // Verify OTP Button
                    CustomButton(
                        text = "Verify OTP",
                        icon = Icons.AutoMirrored.Rounded.ArrowForward,
                        isLoading = isVerifying,
                        onClick = { verifyOtp() }
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }

        // Top App Bar with back button and help
        AuthAppBar(
            modifier = Modifier.align(Alignment.TopCenter),
            showBackButton = true,
            onBackClick = onBackClick
        )

        // Snackbar host for feedback
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 20.dp)
        )
    }
}
