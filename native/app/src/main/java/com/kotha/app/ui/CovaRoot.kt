package com.kotha.app.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.remember
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.kotha.app.data.session.SessionState
import com.kotha.app.ui.navigation.CovaNavHost
import com.kotha.app.ui.screens.auth.AuthScreen
import kotlinx.coroutines.delay

private enum class RootKind { Loading, Auth, Main }

private const val SPLASH_MIN_MS = 1_800L

@Composable
fun CovaRoot(session: SessionState) {
    val kind = when (session) {
        SessionState.Loading -> RootKind.Loading
        SessionState.SignedOut -> RootKind.Auth
        is SessionState.SignedIn -> RootKind.Main
    }
    var splashTimeUp by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (!splashTimeUp) {
            delay(SPLASH_MIN_MS)
            splashTimeUp = true
        }
    }
    val showSplash = !splashTimeUp || kind == RootKind.Loading
    Box(modifier = Modifier.fillMaxSize()) {
        Crossfade(targetState = kind, label = "root") { target ->
            when (target) {
                RootKind.Loading -> Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background))
                RootKind.Auth -> AuthScreen()
                RootKind.Main -> CovaNavHost()
            }
        }
        AnimatedVisibility(
            visible = showSplash,
            exit = fadeOut(tween(420))
        ) {
            CovaSplash(
                modifier = Modifier.clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {}
                )
            )
        }
    }
}
