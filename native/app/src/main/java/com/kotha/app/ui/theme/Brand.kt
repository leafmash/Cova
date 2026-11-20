package com.kotha.app.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Shape

@Composable
fun brandBrush(): Brush {
    val colors = CovaTheme.colors
    return remember(colors.brandStart, colors.brandEnd) {
        Brush.linearGradient(listOf(colors.brandStart, colors.brandEnd))
    }
}

@Composable
fun bubbleBrush(): Brush {
    val colors = CovaTheme.colors
    return remember(colors.bubbleMine, colors.bubbleMineEnd) {
        Brush.linearGradient(listOf(colors.bubbleMine, colors.bubbleMineEnd))
    }
}

object CallPalette {
    val Night = Color(0xFF0A0B18)
    val Deep = Color(0xFF16134A)
    val Glow = Color(0xFF6C5CFF)
    val Accept = Color(0xFF2FBF76)
    val Reject = Color(0xFFE5484D)
}

@Composable
fun Modifier.coveBackdrop(): Modifier {
    val colors = CovaTheme.colors
    val glow = colors.glow
    val aurora = colors.aurora
    val ripple = colors.ripple
    return this.drawBehind {
        val topCenter = Offset(size.width * 0.05f, size.height * 0.02f)
        val topRadius = size.maxDimension * 0.62f
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(glow.copy(alpha = 0.22f), Color.Transparent),
                center = topCenter,
                radius = topRadius
            ),
            radius = topRadius,
            center = topCenter
        )
        val bottomCenter = Offset(size.width * 1.0f, size.height * 0.98f)
        val bottomRadius = size.maxDimension * 0.5f
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(aurora.copy(alpha = 0.12f), Color.Transparent),
                center = bottomCenter,
                radius = bottomRadius
            ),
            radius = bottomRadius,
            center = bottomCenter
        )
        val origin = Offset(size.width * 0.12f, size.height * 1.04f)
        val step = 72.dp.toPx()
        val stroke = Stroke(width = 1.dp.toPx())
        for (i in 1..7) {
            drawCircle(color = ripple, radius = step * i, center = origin, style = stroke)
        }
    }
}

@Composable
fun Modifier.glass(shape: Shape): Modifier {
    val colors = CovaTheme.colors
    return this
        .clip(shape)
        .background(colors.glassFill, shape)
        .border(1.dp, colors.glassBorder, shape)
}

@Composable
fun Modifier.glowShadow(shape: Shape, elevation: Dp = 14.dp): Modifier {
    val glow = CovaTheme.colors.glow
    return this.shadow(elevation, shape, ambientColor = glow, spotColor = glow)
}
