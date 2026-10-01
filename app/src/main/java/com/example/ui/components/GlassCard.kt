package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.GlassBorderCyan
import com.example.ui.theme.GlassCard
import com.example.ui.theme.LocalAppThemeColors
import com.example.ui.theme.NeonCyan

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(20.dp),
    backgroundColor: Color = Color.Unspecified,
    borderBrush: Brush? = null,
    borderWidth: Dp = 1.dp,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val appTheme = LocalAppThemeColors.current
    val effectiveBackgroundColor = if (backgroundColor != Color.Unspecified) backgroundColor else appTheme.cardGlass
    val effectiveBorderBrush = borderBrush ?: Brush.linearGradient(
        colors = if (appTheme.isLight) {
            listOf(
                Color(0x2800796B),
                Color(0x1F0F172A),
                Color(0x100F172A)
            )
        } else {
            listOf(
                GlassBorderCyan,
                GlassBorder,
                Color(0x15FFFFFF)
            )
        }
    )

    val cardModifier = if (onClick != null) {
        modifier
            .clip(shape)
            .clickable(onClick = onClick)
    } else {
        modifier.clip(shape)
    }

    Box(
        modifier = cardModifier
            .background(effectiveBackgroundColor)
            .border(BorderStroke(borderWidth, effectiveBorderBrush), shape)
            .padding(contentPadding),
        content = content
    )
}

@Composable
fun GlassPill(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: (@Composable () -> Unit)? = null
) {
    val appTheme = LocalAppThemeColors.current
    val animatedBg by animateColorAsState(
        targetValue = if (isSelected) appTheme.pillBackgroundSelected else appTheme.pillBackground,
        animationSpec = tween(220, easing = FastOutSlowInEasing),
        label = "pill_bg"
    )
    val animatedBorder by animateColorAsState(
        targetValue = if (isSelected) appTheme.pillBorderSelected else appTheme.pillBorder,
        animationSpec = tween(220, easing = FastOutSlowInEasing),
        label = "pill_border"
    )
    val animatedTextColor by animateColorAsState(
        targetValue = if (isSelected) appTheme.accent else appTheme.textSecondary,
        animationSpec = tween(220, easing = FastOutSlowInEasing),
        label = "pill_text"
    )

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(50),
        color = animatedBg,
        border = BorderStroke(1.dp, animatedBorder),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                icon()
                Spacer(modifier = Modifier.padding(start = 6.dp))
            }
            Text(
                text = text,
                color = animatedTextColor,
                style = MaterialTheme.typography.labelMedium
            )
        }
    }
}
