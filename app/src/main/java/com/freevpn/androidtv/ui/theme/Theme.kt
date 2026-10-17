package com.freevpn.androidtv.ui.theme

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.darkColorScheme

/** App-wide Compose-for-TV theme. Dark, high-contrast, premium VPN aesthetic. */
@Composable
fun FreeVpnTheme(content: @Composable () -> Unit) {
    val colorScheme = darkColorScheme(
        primary = Primary,
        onPrimary = OnPrimary,
        primaryContainer = AccentCyan,
        onPrimaryContainer = OnPrimary,
        secondary = AccentCyan,
        onSecondary = OnPrimary,
        secondaryContainer = AccentCyan,
        onSecondaryContainer = OnPrimary,
        background = BgDark,
        onBackground = TextPrimary,
        surface = SurfaceVariant,
        onSurface = TextPrimary,
        surfaceVariant = SurfaceMedium,
        onSurfaceVariant = TextDim,
        error = StatusRed,
        onError = TextPrimary,
    )
    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        content = content,
    )
}

/** The design canvas width, in design pixels. Every screen is authored against this. */
const val DesignWidth = 1920f

/**
 * Maps the fixed 1920×1080 design space onto whatever real screen the app runs on by overriding the
 * ambient [Density]. After this, `1920.dp` always equals the full screen width and `*.sp` text scales
 * proportionally — so the dp/sp values lifted straight from the prototype render 1:1 on any TV,
 * regardless of the device's reported density.
 */
@Composable
fun DesignScale(content: @Composable () -> Unit) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val widthPx = constraints.maxWidth.toFloat().takeIf { it > 0f } ?: DesignWidth
        val density = widthPx / DesignWidth
        CompositionLocalProvider(
            LocalDensity provides Density(density = density, fontScale = 1f),
            content = content,
        )
    }
}
