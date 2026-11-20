package com.kotha.app.ui.screens.chat

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import com.kotha.app.ui.theme.CovaTheme
import com.kotha.app.ui.theme.rememberReducedMotion
import kotlinx.coroutines.delay

private val reactions = listOf("👍", "❤️", "😂", "😮", "😢", "🙏")

private val ShadowInset = 14.dp

@Composable
fun ReactionPicker(current: String?, mine: Boolean, onReact: (String) -> Unit) {
    val density = LocalDensity.current
    val reduced = rememberReducedMotion()
    val edge = with(density) { 8.dp.roundToPx() }
    val gap = with(density) { 6.dp.roundToPx() }
    val topLimit = with(density) { 104.dp.roundToPx() }
    val inset = with(density) { ShadowInset.roundToPx() }
    val positioner = remember(mine, edge, gap, topLimit, inset) {
        object : PopupPositionProvider {
            override fun calculatePosition(
                anchorBounds: IntRect,
                windowSize: IntSize,
                layoutDirection: LayoutDirection,
                popupContentSize: IntSize
            ): IntOffset {
                val rawX = if (mine) anchorBounds.right - popupContentSize.width + inset else anchorBounds.left - inset
                val maxX = (windowSize.width - popupContentSize.width - edge).coerceAtLeast(edge)
                val above = anchorBounds.top - popupContentSize.height + inset - gap
                val rawY = if (above >= topLimit) above else anchorBounds.bottom - inset + gap
                val maxY = (windowSize.height - popupContentSize.height - edge).coerceAtLeast(edge)
                return IntOffset(rawX.coerceIn(edge, maxX), rawY.coerceIn(edge, maxY))
            }
        }
    }
    val appear = remember { Animatable(if (reduced) 1f else 0f) }
    LaunchedEffect(Unit) {
        if (!reduced) appear.animateTo(1f, spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessMediumLow))
    }
    Popup(
        popupPositionProvider = positioner,
        properties = PopupProperties(focusable = false, clippingEnabled = false)
    ) {
        Box(modifier = Modifier.padding(ShadowInset)) {
            Surface(
                shape = RoundedCornerShape(32.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHighest,
                shadowElevation = 10.dp,
                border = BorderStroke(1.dp, CovaTheme.colors.hairline),
                modifier = Modifier.graphicsLayer {
                    val progress = appear.value
                    val scale = 0.6f + 0.4f * progress
                    scaleX = scale
                    scaleY = scale
                    alpha = progress.coerceIn(0f, 1f)
                    transformOrigin = TransformOrigin(if (mine) 0.9f else 0.1f, 1f)
                }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    reactions.forEachIndexed { index, emoji ->
                        ReactionOption(
                            emoji = emoji,
                            selected = current == emoji,
                            index = index,
                            reduced = reduced,
                            onClick = { onReact(emoji) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ReactionOption(emoji: String, selected: Boolean, index: Int, reduced: Boolean, onClick: () -> Unit) {
    val pop = remember { Animatable(if (reduced) 1f else 0f) }
    LaunchedEffect(Unit) {
        if (!reduced) {
            delay(50L + index * 34L)
            pop.animateTo(1f, spring(dampingRatio = 0.42f, stiffness = Spring.StiffnessMedium))
        }
    }
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val lift by animateFloatAsState(
        targetValue = if (pressed) 1.32f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "reactionLift"
    )
    Box(
        modifier = Modifier
            .graphicsLayer {
                val scale = pop.value * lift
                scaleX = scale
                scaleY = scale
            }
            .size(44.dp)
            .clip(CircleShape)
            .background(if (selected) CovaTheme.colors.accentSoft else Color.Transparent)
            .clickable(interactionSource = source, indication = null, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(text = emoji, fontSize = 26.sp)
    }
}
