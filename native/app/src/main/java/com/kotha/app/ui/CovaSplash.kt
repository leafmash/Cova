package com.kotha.app.ui

import android.app.Activity
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.kotha.app.R
import com.kotha.app.ui.theme.BrandFont
import com.kotha.app.ui.theme.CallPalette
import com.kotha.app.ui.theme.rememberReducedMotion
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val TitleStart = Color(0xFFFFFFFF)
private val TitleEnd = Color(0xFFD9D5FF)
private val LoaderColor = Color(0xFFB8B0FF)

@Composable
fun CovaSplash(modifier: Modifier = Modifier) {
    val reduced = rememberReducedMotion()
    val view = LocalView.current
    DisposableEffect(view) {
        val window = (view.context as? Activity)?.window
        val controller = window?.let { WindowCompat.getInsetsController(it, view) }
        val statusLight = controller?.isAppearanceLightStatusBars
        val navLight = controller?.isAppearanceLightNavigationBars
        controller?.isAppearanceLightStatusBars = false
        controller?.isAppearanceLightNavigationBars = false
        onDispose {
            if (controller != null && statusLight != null) controller.isAppearanceLightStatusBars = statusLight
            if (controller != null && navLight != null) controller.isAppearanceLightNavigationBars = navLight
        }
    }

    val logoScale = remember { Animatable(0.88f) }
    val logoAlpha = remember { Animatable(0f) }
    val titleAlpha = remember { Animatable(0f) }
    val titleShift = remember { Animatable(10f) }
    val loaderAlpha = remember { Animatable(0f) }
    LaunchedEffect(reduced) {
        if (reduced) {
            logoScale.snapTo(1f)
            logoAlpha.snapTo(1f)
            titleAlpha.snapTo(1f)
            titleShift.snapTo(0f)
            loaderAlpha.snapTo(1f)
            return@LaunchedEffect
        }
        coroutineScope {
            launch { logoAlpha.animateTo(1f, tween(450, easing = FastOutSlowInEasing)) }
            launch { logoScale.animateTo(1f, tween(650, easing = FastOutSlowInEasing)) }
            launch {
                delay(300)
                titleAlpha.animateTo(1f, tween(500, easing = FastOutSlowInEasing))
            }
            launch {
                delay(300)
                titleShift.animateTo(0f, tween(550, easing = FastOutSlowInEasing))
            }
            launch {
                delay(700)
                loaderAlpha.animateTo(1f, tween(400))
            }
        }
    }

    val infinite = rememberInfiniteTransition(label = "splash")
    val glow by infinite.animateFloat(
        initialValue = 0.72f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2400, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "glow"
    )
    val sweep by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1500, easing = LinearEasing)),
        label = "sweep"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(CallPalette.Night, CallPalette.Deep, CallPalette.Night)))
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .offset(y = (-28).dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(176.dp)
                    .drawBehind {
                        val radius = 230.dp.toPx()
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(CallPalette.Glow.copy(alpha = 0.42f * glow), Color.Transparent),
                                center = center,
                                radius = radius
                            ),
                            radius = radius,
                            center = center,
                            alpha = logoAlpha.value
                        )
                    }
            ) {
                Image(
                    painter = painterResource(R.drawable.splash_icon),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            scaleX = logoScale.value
                            scaleY = logoScale.value
                            alpha = logoAlpha.value
                        }
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.app_name),
                modifier = Modifier.graphicsLayer {
                    alpha = titleAlpha.value
                    translationY = titleShift.value.dp.toPx()
                },
                style = MaterialTheme.typography.displaySmall.copy(
                    fontFamily = BrandFont,
                    fontWeight = FontWeight.Bold,
                    fontSize = 40.sp,
                    letterSpacing = 3.sp,
                    brush = Brush.horizontalGradient(listOf(TitleStart, TitleEnd))
                )
            )
        }
        Canvas(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 56.dp)
                .size(width = 44.dp, height = 3.dp)
                .graphicsLayer { alpha = loaderAlpha.value }
        ) {
            val corner = CornerRadius(size.height / 2f)
            drawRoundRect(color = LoaderColor.copy(alpha = 0.16f), size = size, cornerRadius = corner)
            val segment = size.width * 0.42f
            val x = -segment + (size.width + segment) * FastOutSlowInEasing.transform(sweep)
            clipRect {
                drawRoundRect(
                    color = LoaderColor.copy(alpha = 0.9f),
                    topLeft = Offset(x, 0f),
                    size = Size(segment, size.height),
                    cornerRadius = corner
                )
            }
        }
    }
}
