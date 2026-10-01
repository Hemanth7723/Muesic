package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.EmeraldHifi
import com.example.ui.theme.LosslessGold
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonPink
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun AudioVisualizerWave(
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    barCount: Int = 18,
    maxHeight: Dp = 48.dp,
    barWidth: Dp = 3.dp
) {
    val infiniteTransition = rememberInfiniteTransition(label = "liquid_wave")

    // Smooth continuous liquid phase loop
    val wavePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1350, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "primary_wave"
    )

    val harmonicPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 850, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "harmonic_wave"
    )

    val barBrush = remember {
        Brush.verticalGradient(
            colors = listOf(NeonCyan, ElectricViolet, NeonPink)
        )
    }

    val calculatedWidth = remember(barCount, barWidth) {
        barWidth * barCount + 3.dp * (barCount - 1).coerceAtLeast(0)
    }

    Canvas(
        modifier = modifier
            .height(maxHeight)
            .width(calculatedWidth)
    ) {
        val totalWidth = size.width
        val canvasHeight = size.height
        val barWidthPx = barWidth.toPx()
        val spaceBetween = if (barCount > 1) {
            ((totalWidth - barWidthPx * barCount) / (barCount - 1)).coerceAtLeast(0f)
        } else {
            0f
        }

        val cornerRadius = CornerRadius(barWidthPx / 2f, barWidthPx / 2f)

        for (i in 0 until barCount) {
            val left = i * (barWidthPx + spaceBetween)
            val barHeightFraction = if (isPlaying) {
                val wave1 = sin(wavePhase.toDouble() + i * 0.44).toFloat()
                val wave2 = cos(harmonicPhase.toDouble() + i * 0.72).toFloat()
                val blended = (wave1 * 0.55f + wave2 * 0.45f)
                (0.18f + 0.78f * abs(blended)).coerceIn(0.12f, 1f)
            } else {
                0.14f
            }

            val barHeightPx = canvasHeight * barHeightFraction
            val top = canvasHeight - barHeightPx

            drawRoundRect(
                brush = barBrush,
                topLeft = Offset(left, top),
                size = Size(barWidthPx, barHeightPx),
                cornerRadius = cornerRadius
            )
        }
    }
}

@Composable
fun HifiBadge(
    quality: String,
    modifier: Modifier = Modifier
) {
    val isLossless = quality.contains("Lossless") || quality.contains("FLAC")
    val badgeColor = if (isLossless) LosslessGold else EmeraldHifi
    val bg = if (isLossless) Color(0x33FFB703) else Color(0x2610B981)

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bg)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = Icons.Default.GraphicEq,
            contentDescription = null,
            tint = badgeColor,
            modifier = Modifier.height(12.dp)
        )
        Text(
            text = quality,
            color = badgeColor,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, letterSpacing = 0.5.sp)
        )
    }
}

@Composable
fun PrivacyShieldBadge(
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(Color(0x2E00F5D4))
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Lock,
            contentDescription = "Zero Tracking",
            tint = NeonCyan,
            modifier = Modifier.height(13.dp)
        )
        Text(
            text = "E2EE • No Ads • 100% Private",
            color = NeonCyan,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp)
        )
    }
}
