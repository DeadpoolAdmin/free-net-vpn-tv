package com.freevpn.androidtv.ui.welcome

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.freevpn.androidtv.R
import com.freevpn.androidtv.ui.common.AppBackground
import com.freevpn.androidtv.ui.common.AppLogo
import com.freevpn.androidtv.ui.common.PrimaryPillButton
import com.freevpn.androidtv.ui.common.appViewModelFactory
import com.freevpn.androidtv.ui.theme.AccentCyan
import com.freevpn.androidtv.ui.theme.Primary
import com.freevpn.androidtv.ui.theme.TextTertiary

/**
 * Branded launch / splash screen. While the device authenticates in the background it shows only the
 * hero logo, wordmark and a quiet loading indicator — a native-app boot screen, not a marketing page.
 */
@Composable
fun WelcomeScreen(
    onAuthenticated: () -> Unit,
    viewModel: WelcomeViewModel = viewModel(factory = appViewModelFactory()),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { viewModel.authenticated.collect { onAuthenticated() } }

    AppBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 80.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Hero logo with an ambient glow behind it.
            Box(
                modifier = Modifier.size(300.dp),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(
                            Brush.radialGradient(listOf(Primary.copy(alpha = 0.22f), Color.Transparent)),
                            CircleShape,
                        ),
                )
                AppLogo(size = 224.dp)
            }
            Spacer(Modifier.height(30.dp))

            Text(
                text = stringResource(R.string.subtagline),
                style = MaterialTheme.typography.titleMedium,
                color = TextTertiary,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(40.dp))

            when (state) {
                WelcomeUiState.Loading -> WelcomeLoading()
                WelcomeUiState.Error -> WelcomeError(onRetry = viewModel::authenticate)
            }
        }
    }
}

/** Three pulsing dots plus a status line shown while device authentication is in flight. */
@Composable
private fun WelcomeLoading() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            repeat(3) { i -> PulsingDot(delayMs = i * 160) }
        }
        Spacer(Modifier.height(22.dp))
        Text(
            text = stringResource(R.string.welcome_preparing),
            style = MaterialTheme.typography.titleMedium,
            color = TextTertiary,
            textAlign = TextAlign.Center,
        )
    }
}

/** Shown when authentication fails so the user can retry instead of being stuck on a spinner. */
@Composable
private fun WelcomeError(onRetry: () -> Unit) {
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { focusRequester.requestFocus() } }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = stringResource(R.string.welcome_auth_failed),
            style = MaterialTheme.typography.titleMedium,
            color = TextTertiary,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = 600.dp),
        )
        Spacer(Modifier.height(48.dp))
        PrimaryPillButton(
            text = stringResource(R.string.home_retry),
            onClick = onRetry,
            modifier = Modifier.focusRequester(focusRequester),
            fontSize = 30.sp,
            horizontal = 72.dp,
            vertical = 26.dp,
        )
    }
}

@Composable
private fun PulsingDot(delayMs: Int) {
    // A single infinite transition drives one dot; the per-dot [delayMs] phase-shifts the start so the
    // three dots pulse in a staggered wave. The dot scales up and brightens, then eases back down.
    val transition = rememberInfiniteTransition(label = "welcome-dot")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 640, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
            initialStartOffset = StartOffset(delayMs),
        ),
        label = "welcome-dot-pulse",
    )
    // Scale (layout stays a fixed 12dp, so nothing shifts) and alpha ride the same 0..1 progress.
    Spacer(
        modifier = Modifier
            .size(12.dp)
            .scale(0.55f + 0.45f * progress)
            .alpha(0.35f + 0.65f * progress)
            .background(AccentCyan, CircleShape),
    )
}
