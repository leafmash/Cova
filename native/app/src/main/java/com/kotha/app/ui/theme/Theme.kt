package com.kotha.app.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

@Immutable
data class CovaColors(
    val bubbleMine: Color,
    val bubbleMineEnd: Color,
    val brandStart: Color,
    val brandEnd: Color,
    val glow: Color,
    val lantern: Color,
    val onLantern: Color,
    val ripple: Color,
    val onBubbleMine: Color,
    val bubbleTheirs: Color,
    val onBubbleTheirs: Color,
    val chatBackground: Color,
    val tick: Color,
    val success: Color,
    val hairline: Color,
    val glassBar: Color,
    val missed: Color,
    val accentSoft: Color,
    val aurora: Color,
    val glassFill: Color,
    val glassBorder: Color
)

private val DarkCovaColors = CovaColors(
    bubbleMine = Color(0xFF7C5CFF),
    bubbleMineEnd = Color(0xFF4B54F0),
    brandStart = Color(0xFFAB92FF),
    brandEnd = Color(0xFF5562F5),
    glow = Color(0xFF6C5CFF),
    lantern = Color(0xFFFFB547),
    onLantern = Color(0xFF241500),
    ripple = Color(0x0FB8B0FF),
    onBubbleMine = Color(0xFFFFFFFF),
    bubbleTheirs = Color(0xFF161A33),
    onBubbleTheirs = Color(0xFFEEF0FB),
    chatBackground = Color(0xFF07080F),
    tick = Color(0xFF7DF0C4),
    success = Color(0xFF45E0A4),
    hairline = Color(0x14FFFFFF),
    glassBar = Color(0xF2161A33),
    missed = Color(0xFFFF6B7F),
    accentSoft = Color(0x29A99BFF),
    aurora = Color(0xFF4FD8F0),
    glassFill = Color(0x14FFFFFF),
    glassBorder = Color(0x1FFFFFFF)
)

private val LightCovaColors = CovaColors(
    bubbleMine = Color(0xFF7458FA),
    bubbleMineEnd = Color(0xFF4F55EA),
    brandStart = Color(0xFF8A6DFF),
    brandEnd = Color(0xFF4F55EA),
    glow = Color(0xFF8A7DFF),
    lantern = Color(0xFFFFB547),
    onLantern = Color(0xFF241500),
    ripple = Color(0x145B4BF5),
    onBubbleMine = Color(0xFFFFFFFF),
    bubbleTheirs = Color(0xFFFFFFFF),
    onBubbleTheirs = Color(0xFF14142B),
    chatBackground = Color(0xFFF2F0FA),
    tick = Color(0xFF8DF5CF),
    success = Color(0xFF14A06A),
    hairline = Color(0x14141436),
    glassBar = Color(0xF2FFFFFF),
    missed = Color(0xFFE23B52),
    accentSoft = Color(0x1F5A48F2),
    aurora = Color(0xFF1BA9D6),
    glassFill = Color(0xB3FFFFFF),
    glassBorder = Color(0x1F5A48F2)
)

private val LocalCovaColors = staticCompositionLocalOf { DarkCovaColors }

object CovaTheme {
    val colors: CovaColors
        @Composable
        @ReadOnlyComposable
        get() = LocalCovaColors.current
}

@Composable
fun CovaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }
    CompositionLocalProvider(LocalCovaColors provides if (darkTheme) DarkCovaColors else LightCovaColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = CovaTypography,
            shapes = CovaShapes,
            content = content
        )
    }
}
