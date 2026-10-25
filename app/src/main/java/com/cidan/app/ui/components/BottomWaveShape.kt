package com.cidan.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.cidan.app.ui.theme.AppColors

@Composable
fun BottomWave(
    modifier: Modifier = Modifier,
    height: Dp = 180.dp,
    primaryColor: Color = AppColors.Primary
) {
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
    ) {
        val w = size.width
        val h = size.height

        // First softer wave
        val path1 = Path().apply {
            moveTo(0f, h * 0.4f)
            quadraticTo(w * 0.25f, h * 0.1f, w * 0.6f, h * 0.5f)
            quadraticTo(w * 0.85f, h * 0.8f, w, h * 0.3f)
            lineTo(w, h)
            lineTo(0f, h)
            close()
        }
        drawPath(path1, color = primaryColor.copy(alpha = 0.08f))

        // Second slightly more prominent wave
        val path2 = Path().apply {
            moveTo(0f, h * 0.65f)
            quadraticTo(w * 0.4f, h * 0.45f, w * 0.7f, h * 0.75f)
            quadraticTo(w * 0.88f, h * 0.9f, w, h * 0.6f)
            lineTo(w, h)
            lineTo(0f, h)
            close()
        }
        drawPath(path2, color = primaryColor.copy(alpha = 0.12f))
    }
}
