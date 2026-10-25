package com.cidan.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cidan.app.ui.theme.AppColors
import com.cidan.app.ui.theme.Poppins

@Composable
fun OtpInputRow(
    otp: String,
    onOtpChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    length: Int = 6,
    onComplete: (String) -> Unit = {}
) {
    val focusRequesters = remember { List(length) { FocusRequester() } }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (i in 0 until length) {
            val char = otp.getOrNull(i)?.toString() ?: ""
            val isFocused = otp.length == i || (i == length - 1 && otp.length == length)
            val isFilled = char.isNotEmpty()

            val borderColor = when {
                isFilled -> AppColors.Primary
                isFocused -> AppColors.BorderFocus
                else -> AppColors.Border
            }

            Box(
                modifier = Modifier
                    .width(48.dp)
                    .height(56.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.White)
                    .border(
                        width = if (isFilled || isFocused) 1.6.dp else 1.2.dp,
                        color = borderColor,
                        shape = RoundedCornerShape(14.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                // Hidden/Overlay BasicTextField for direct digit input
                BasicTextField(
                    value = char,
                    onValueChange = { newText ->
                        val digits = newText.filter { it.isDigit() }
                        if (digits.length > 1) {
                            // Multi-digit paste
                            val combined = (otp.take(i) + digits).take(length)
                            onOtpChange(combined)
                            val nextFocus = (combined.length).coerceAtMost(length - 1)
                            focusRequesters[nextFocus].requestFocus()
                            if (combined.length == length) {
                                onComplete(combined)
                            }
                        } else if (digits.isNotEmpty()) {
                            val newOtp = otp.padEnd(length, ' ').toCharArray()
                            newOtp[i] = digits.last()
                            val trimmed = String(newOtp).trimEnd()
                            onOtpChange(trimmed)
                            if (i < length - 1) {
                                focusRequesters[i + 1].requestFocus()
                            }
                            if (trimmed.length == length) {
                                onComplete(trimmed)
                            }
                        } else {
                            if (otp.length > i) {
                                val updated = otp.removeRange(i, i + 1)
                                onOtpChange(updated)
                            }
                        }
                    },
                    modifier = Modifier
                        .focusRequester(focusRequesters[i])
                        .onKeyEvent { event ->
                            if (event.key == Key.Backspace && char.isEmpty() && i > 0) {
                                focusRequesters[i - 1].requestFocus()
                                if (otp.length >= i) {
                                    onOtpChange(otp.take(i - 1))
                                }
                                true
                            } else {
                                false
                            }
                        },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    singleLine = true,
                    textStyle = androidx.compose.ui.text.TextStyle(
                        fontFamily = Poppins,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = AppColors.TextPrimary,
                        textAlign = TextAlign.Center
                    )
                )
            }
        }
    }
}
