package com.freevpn.androidtv.ui.home

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import com.freevpn.androidtv.R
import com.freevpn.androidtv.ui.theme.AccentCyan
import com.freevpn.androidtv.ui.theme.DmSans
import com.freevpn.androidtv.ui.theme.Primary
import com.freevpn.androidtv.ui.theme.StatusRed
import com.freevpn.androidtv.ui.theme.TextPrimary
import com.freevpn.androidtv.ui.theme.TextTertiary

private const val SIZE = 360

/**
 * The large circular connect control. One composable, five visual states (disconnected, connecting,
 * connected, disconnecting, error). Every state is fully static — no animations — for the smoothest
 * possible rendering on low-power TV hardware.
 */
@Composable
fun PremiumConnectButton(
    modifier: Modifier = Modifier,
    isConnected: Boolean = false,
    isConnecting: Boolean = false,
    isDisconnecting: Boolean = false,
    isError: Boolean = false,
    elapsedTime: String = "00:00:00",
) {
    Box(
        modifier = modifier.size(SIZE.dp),
        contentAlignment = Alignment.Center,
    ) {
        when {
            isConnected -> ConnectedVisual(elapsedTime)
            isConnecting -> ConnectingVisual()
            isDisconnecting -> DisconnectingVisual()
            isError -> ErrorVisual()
            else -> DisconnectedVisual()
        }
    }
}

@Composable
private fun ConnectedVisual(elapsedTime: String) {
    Box(
        modifier = Modifier
            .size(256.dp)
            .border(2.dp, AccentCyan, CircleShape)
            .background(
                brush = Brush.radialGradient(
                    listOf(AccentCyan.copy(alpha = 0.3f), AccentCyan.copy(alpha = 0.06f)),
                ),
                shape = CircleShape,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Icon(
                painter = painterResource(R.drawable.ic_shield_check),
                contentDescription = null,
                tint = AccentCyan,
                modifier = Modifier.size(58.dp),
            )
            Spacer(Modifier.height(10.dp))
            Text(
                text = elapsedTime,
                fontFamily = DmSans,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 38.sp,
                letterSpacing = 1.sp,
                color = TextPrimary,
            )
        }
    }
}

@Composable
private fun ConnectingVisual() {
    val spin = rememberSpin()
    SpinnerRing(rotation = { spin.value }, baseColor = Primary.copy(alpha = 0.16f), arcColor = AccentCyan)
    Box(
        modifier = Modifier
            .size(248.dp)
            .background(
                Brush.radialGradient(listOf(Primary.copy(alpha = 0.18f), Color.Transparent)),
                CircleShape,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Text(
                text = "Connecting",
                fontFamily = DmSans,
                fontWeight = FontWeight.Bold,
                fontSize = 28.sp,
                color = TextPrimary,
            )
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                repeat(3) { Dot() }
            }
        }
    }
}

@Composable
private fun DisconnectingVisual() {
    val spin = rememberSpin()
    SpinnerRing(rotation = { spin.value }, baseColor = Color.White.copy(alpha = 0.1f), arcColor = TextTertiary)
    Text(
        text = "Disconnecting",
        fontFamily = DmSans,
        fontWeight = FontWeight.Bold,
        fontSize = 26.sp,
        color = TextTertiary,
    )
}

@Composable
private fun ErrorVisual() {
    Box(Modifier.size(SIZE.dp).border(1.dp, StatusRed.copy(alpha = 0.2f), CircleShape))
    Box(
        modifier = Modifier
            .size(256.dp)
            .border(2.dp, StatusRed, CircleShape)
            .background(
                Brush.radialGradient(
                    listOf(StatusRed.copy(alpha = 0.22f), StatusRed.copy(alpha = 0.04f)),
                ),
                CircleShape,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Icon(
                painter = painterResource(R.drawable.ic_error_info),
                contentDescription = null,
                tint = StatusRed,
                modifier = Modifier.size(56.dp),
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = "Try again",
                fontFamily = DmSans,
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp,
                color = TextPrimary,
            )
        }
    }
}

@Composable
private fun DisconnectedVisual() {
    Box(Modifier.size(SIZE.dp).border(1.dp, Primary.copy(alpha = 0.2f), CircleShape))
    Box(Modifier.size(308.dp).border(1.dp, Primary.copy(alpha = 0.28f), CircleShape))
    Box(
        modifier = Modifier
            .size(256.dp)
            .border(2.dp, Primary, CircleShape)
            .background(
                Brush.radialGradient(
                    listOf(Primary.copy(alpha = 0.28f), Primary.copy(alpha = 0.05f)),
                ),
                CircleShape,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Icon(
                painter = painterResource(R.drawable.ic_power),
                contentDescription = null,
                tint = TextPrimary,
                modifier = Modifier.size(60.dp),
            )
            Spacer(Modifier.height(14.dp))
            Text(
                text = "Connect",
                fontFamily = DmSans,
                fontWeight = FontWeight.Bold,
                fontSize = 32.sp,
                color = TextPrimary,
            )
        }
    }
}

/** Continuous 0→360° rotation that drives the connecting / disconnecting spinner. */
@Composable
private fun rememberSpin() = rememberInfiniteTransition(label = "spinner").animateFloat(
    initialValue = 0f,
    targetValue = 360f,
    animationSpec = infiniteRepeatable(tween(1050, easing = LinearEasing), RepeatMode.Restart),
    label = "spin",
)

/**
 * A faint full ring with a brighter rotating arc — the connecting / disconnecting spinner. The arc is
 * spun by reading [rotation] inside a graphicsLayer (draw phase), so the animation never recomposes
 * this composable; only the layer's transform updates each frame.
 */
@Composable
private fun SpinnerRing(rotation: () -> Float, baseColor: Color, arcColor: Color) {
    Box(Modifier.size(300.dp), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(color = baseColor, style = Stroke(width = 4.dp.toPx()))
        }
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { rotationZ = rotation() },
        ) {
            drawArc(
                color = arcColor,
                startAngle = -90f,
                sweepAngle = 120f,
                useCenter = false,
                style = Stroke(width = 4.dp.toPx()),
            )
        }
    }
}

@Composable
private fun Dot() {
    Box(
        modifier = Modifier
            .size(9.dp)
            .background(AccentCyan, CircleShape),
    )
}
