package com.cidan.app.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

object AppColors {
    // Primary Palette
    val Primary = Color(0xFF4F46E5) // Vibrant Indigo / Purple
    val PrimaryHover = Color(0xFF4338CA)
    val PrimaryLight = Color(0xFFEEF0FF)
    val PrimarySoft = Color(0xFFF3F2FD)

    // Backgrounds
    val Background = Color(0xFFFAFBFD)
    val Surface = Color.White
    val ToggleBackground = Color(0xFFF1F3F9)

    // Text Colors
    val TextPrimary = Color(0xFF0F172A) // Dark Slate / Navy
    val TextSecondary = Color(0xFF64748B) // Slate Grey
    val TextMuted = Color(0xFF94A3B8) // Light Grey
    val TextLink = Color(0xFF4F46E5)

    // Borders & Dividers
    val Border = Color(0xFFE2E8F0)
    val BorderFocus = Color(0xFF4F46E5)
    val BorderError = Color(0xFFEF4444)

    // Functional / Status
    val Success = Color(0xFF10B981)
    val SuccessSoft = Color(0xFFECFDF5)
    val Error = Color(0xFFEF4444)
    val Warning = Color(0xFFF59E0B)

    // Gradients
    val PrimaryGradient = Brush.linearGradient(
        colors = listOf(
            Color(0xFF5046E5),
            Color(0xFF4338CA)
        )
    )

    val BackgroundWave = Brush.verticalGradient(
        colors = listOf(
            Color(0x00FFFFFF),
            Color(0x1A6366F1)
        )
    )
}
