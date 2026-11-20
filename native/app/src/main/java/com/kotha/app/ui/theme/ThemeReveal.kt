package com.kotha.app.ui.theme

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.IntSize
import kotlin.math.hypot
import kotlin.math.max

@Stable
class ThemeRevealState {
    var snapshot by mutableStateOf<ImageBitmap?>(null)
    var origin by mutableStateOf(Offset.Zero)
}

@Stable
class ThemeController(val dark: Boolean, val toggle: (Offset) -> Unit)

val LocalThemeController = staticCompositionLocalOf { ThemeController(false) { } }

@Composable
fun ThemeRevealOverlay(state: ThemeRevealState) {
    val snapshot = state.snapshot ?: return
    val progress = remember(snapshot) { Animatable(0f) }
    LaunchedEffect(snapshot) {
        progress.animateTo(1f, tween(durationMillis = 520, easing = FastOutSlowInEasing))
        state.snapshot = null
    }
    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
    ) {
        drawImage(
            image = snapshot,
            dstSize = IntSize(size.width.toInt(), size.height.toInt())
        )
        val origin = state.origin
        val reach = max(
            max(hypot(origin.x, origin.y), hypot(size.width - origin.x, origin.y)),
            max(hypot(origin.x, size.height - origin.y), hypot(size.width - origin.x, size.height - origin.y))
        )
        drawCircle(
            color = Color.Black,
            radius = reach * progress.value,
            center = origin,
            blendMode = BlendMode.Clear
        )
    }
}
