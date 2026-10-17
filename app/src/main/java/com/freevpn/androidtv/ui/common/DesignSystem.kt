package com.freevpn.androidtv.ui.common

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.freevpn.androidtv.R
import com.freevpn.androidtv.ui.theme.AccentCyan
import com.freevpn.androidtv.ui.theme.BgDarkDeep
import com.freevpn.androidtv.ui.theme.BgDarkGradientLight
import com.freevpn.androidtv.ui.theme.BgDarkGradientMid
import com.freevpn.androidtv.ui.theme.OnPrimary
import com.freevpn.androidtv.ui.theme.Primary
import com.freevpn.androidtv.ui.theme.TextDim
import com.freevpn.androidtv.ui.theme.TextPrimary
import com.freevpn.androidtv.ui.theme.TextSecondary

/** Translucent white at the given alpha — the building block for cards and pills in this design. */
fun whiteA(alpha: Float): Color = Color.White.copy(alpha = alpha)

/** The shared cyan focus ring color (rgba(110,255,211,0.5) in the design). */
val FocusRing: Color = AccentCyan.copy(alpha = 0.5f)

/**
 * Full-screen app background: the radial dark gradient plus the faint ambient art (concentric rings,
 * corner glows, dotted grid). [content] sits on top.
 *
 * Everything here is purely decorative and never changes, so the gradient/glow [Brush]es and the dot
 * geometry are built once per size inside [drawWithCache] and only the recorded draw is replayed —
 * no per-frame allocations, and a single background layer instead of a separate full-screen Canvas.
 */
@Composable
fun AppBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .drawWithCache {
                val background = Brush.radialGradient(
                    0f to BgDarkGradientLight,
                    0.46f to BgDarkGradientMid,
                    1f to BgDarkDeep,
                    center = Offset(size.width * 0.5f, size.height * 0.33f),
                    radius = maxOf(size.width, size.height) * 1.15f,
                )

                val cx = size.width * 0.5f
                val cy = size.height * 0.46f
                val ringStroke = Stroke(1.dp.toPx())

                val g1Center = Offset(size.width * 0.2f, size.height * 0.1f)
                val g1Radius = 450.dp.toPx()
                val g1 = Brush.radialGradient(
                    listOf(Primary.copy(alpha = 0.16f), Color.Transparent),
                    center = g1Center, radius = g1Radius,
                )
                val g2Center = Offset(size.width * 0.9f, size.height * 0.96f)
                val g2Radius = 380.dp.toPx()
                val g2 = Brush.radialGradient(
                    listOf(AccentCyan.copy(alpha = 0.08f), Color.Transparent),
                    center = g2Center, radius = g2Radius,
                )

                // Pre-compute the faint dotted grid once instead of looping on every draw.
                val step = 64.dp.toPx()
                val dotRadius = 0.6.dp.toPx()
                val dotColor = Color.White.copy(alpha = 0.025f)
                val width = size.width
                val height = size.height
                val dots = buildList {
                    var y = step
                    while (y < height) {
                        var x = step
                        while (x < width) {
                            add(Offset(x, y))
                            x += step
                        }
                        y += step
                    }
                }

                onDrawBehind {
                    drawRect(background)

                    drawCircle(AccentCyan.copy(alpha = 0.05f), 750.dp.toPx(), Offset(cx, cy), style = ringStroke)
                    drawCircle(Primary.copy(alpha = 0.08f), 550.dp.toPx(), Offset(cx, cy), style = ringStroke)
                    drawCircle(AccentCyan.copy(alpha = 0.07f), 380.dp.toPx(), Offset(cx, cy), style = ringStroke)

                    drawCircle(brush = g1, radius = g1Radius, center = g1Center)
                    drawCircle(brush = g2, radius = g2Radius, center = g2Center)

                    dots.forEach { drawCircle(dotColor, radius = dotRadius, center = it) }
                }
            },
    ) {
        content()
    }
}

/** The gradient app logo tile with the shield-check glyph. */
@Composable
fun BrandLogo(
    size: Dp = 46.dp,
    corner: Dp = 13.dp,
    iconSize: Dp = 24.dp,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(size)
            .background(
                brush = Brush.linearGradient(listOf(Primary, AccentCyan)),
                shape = RoundedCornerShape(corner),
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_shield_check),
            contentDescription = null,
            tint = OnPrimary,
            modifier = Modifier.size(iconSize),
        )
    }
}

/**
 * The full-colour VPN shield brand logo (cyan crest + keyhole mark + "VPN" wordmark) for hero
 * placements like the Welcome splash and the About screen. Unlike [BrandLogo] this is the complete
 * standalone mark, drawn with [Image] so its cyan/black colours are preserved (no tinting). The
 * source is the supplied raster artwork (res/drawable-nodpi/ic_app_logo.png); [size] bounds it as a
 * square box and the aspect ratio is kept (ContentScale.Fit).
 */
@Composable
fun AppLogo(
    size: Dp,
    modifier: Modifier = Modifier,
) {
    Image(
        painter = painterResource(R.drawable.ic_app_logo),
        contentDescription = null,
        modifier = modifier.size(size),
    )
}

/** The small "FREE" pill. */
@Composable
fun FreeBadge(
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 14.sp,
    horizontal: Dp = 11.dp,
    vertical: Dp = 5.dp,
) {
    Text(
        text = stringResource(R.string.free_badge),
        color = OnPrimary,
        fontFamily = com.freevpn.androidtv.ui.theme.DmSans,
        fontWeight = FontWeight.ExtraBold,
        fontSize = fontSize,
        letterSpacing = 1.4.sp,
        modifier = modifier
            .background(AccentCyan, RoundedCornerShape(999.dp))
            .padding(horizontal = horizontal, vertical = vertical),
    )
}

/**
 * Generic focusable card/row used across screens (location list rows, settings rows, the home
 * "selected location" card). Translucent fill with a cyan focus ring.
 *
 * Focus feedback is the border colour/width change only — no scale and no animation; the whole app is
 * deliberately static for the smoothest possible focus navigation on low-power TV boxes.
 */
@Composable
fun DesignCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    corner: Dp = 18.dp,
    containerColor: Color = whiteA(0.04f),
    focusedContainerColor: Color = whiteA(0.09f),
    borderColor: Color = whiteA(0.08f),
    focusedBorderColor: Color = FocusRing,
    borderWidth: Dp = 1.5.dp,
    content: @Composable BoxScope.() -> Unit,
) {
    val shape = RoundedCornerShape(corner)
    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier,
        shape = ClickableSurfaceDefaults.shape(shape),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = containerColor,
            focusedContainerColor = focusedContainerColor,
            pressedContainerColor = focusedContainerColor,
            contentColor = TextPrimary,
            focusedContentColor = TextPrimary,
            pressedContentColor = TextPrimary,
            disabledContainerColor = containerColor,
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1f),
        border = ClickableSurfaceDefaults.border(
            border = Border(BorderStroke(borderWidth, borderColor), shape = shape),
            focusedBorder = Border(BorderStroke(3.dp, focusedBorderColor), shape = shape),
        ),
        content = content,
    )
}

/** Square back button (chevron-left) used in screen headers. */
@Composable
fun BackButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    DesignCard(
        onClick = onClick,
        modifier = modifier.size(56.dp),
        corner = 16.dp,
        containerColor = whiteA(0.05f),
        focusedContainerColor = whiteA(0.1f),
        borderColor = whiteA(0.12f),
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Icon(
                painter = painterResource(R.drawable.ic_chevron_left),
                contentDescription = "Back",
                tint = TextPrimary,
                modifier = Modifier.size(24.dp),
            )
        }
    }
}

/** Standard screen header: back button + title, with an optional trailing slot (e.g. Refresh). */
@Composable
fun ScreenHeader(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    backButtonModifier: Modifier = Modifier,
    trailing: (@Composable RowScope.() -> Unit)? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            BackButton(onClick = onBack, modifier = backButtonModifier)
            Text(
                text = title,
                style = MaterialTheme.typography.displaySmall,
                color = TextPrimary,
            )
        }
        if (trailing != null) {
            Row(verticalAlignment = Alignment.CenterVertically, content = trailing)
        }
    }
}

/** Filled gradient pill — the primary call to action (Get Started, Check again). */
@Composable
fun PrimaryPillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 24.sp,
    horizontal: Dp = 44.dp,
    vertical: Dp = 20.dp,
) {
    val shape = RoundedCornerShape(999.dp)
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = ClickableSurfaceDefaults.shape(shape),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = Color.Transparent,
            focusedContainerColor = Color.Transparent,
            pressedContainerColor = Color.Transparent,
            contentColor = TextPrimary,
            focusedContentColor = TextPrimary,
            pressedContentColor = TextPrimary,
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1f),
        border = ClickableSurfaceDefaults.border(
            focusedBorder = Border(BorderStroke(4.dp, FocusRing), shape = shape),
        ),
    ) {
        Box(
            modifier = Modifier
                .background(Brush.linearGradient(listOf(Primary, com.freevpn.androidtv.ui.theme.PrimaryLight)), shape)
                .padding(horizontal = horizontal, vertical = vertical),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = text,
                fontFamily = com.freevpn.androidtv.ui.theme.DmSans,
                fontWeight = FontWeight.Bold,
                fontSize = fontSize,
                color = TextPrimary,
            )
        }
    }
}

/** Translucent pill — secondary actions (Change location, Settings, Continue without activating). */
@Composable
fun SecondaryPillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: Int? = null,
    fontSize: TextUnit = 22.sp,
    horizontal: Dp = 30.dp,
    vertical: Dp = 15.dp,
) {
    val shape = RoundedCornerShape(999.dp)
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = ClickableSurfaceDefaults.shape(shape),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = whiteA(0.05f),
            focusedContainerColor = whiteA(0.1f),
            pressedContainerColor = whiteA(0.1f),
            contentColor = TextSecondary,
            focusedContentColor = TextPrimary,
            pressedContentColor = TextPrimary,
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1f),
        border = ClickableSurfaceDefaults.border(
            border = Border(BorderStroke(1.dp, whiteA(0.12f)), shape = shape),
            focusedBorder = Border(BorderStroke(3.dp, FocusRing), shape = shape),
        ),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = horizontal, vertical = vertical),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (leadingIcon != null) {
                Icon(
                    painter = painterResource(leadingIcon),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                )
            }
            Text(
                text = text,
                fontFamily = com.freevpn.androidtv.ui.theme.Inter,
                fontWeight = FontWeight.SemiBold,
                fontSize = fontSize,
            )
        }
    }
}

/** Small uppercase eyebrow label (e.g. "OPTIONAL STEP", "LOCATION"). */
@Composable
fun Eyebrow(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = TextDim,
    fontSize: TextUnit = 16.sp,
    align: TextAlign? = null,
) {
    Text(
        text = text.uppercase(),
        modifier = modifier,
        color = color,
        fontFamily = com.freevpn.androidtv.ui.theme.Inter,
        fontWeight = FontWeight.SemiBold,
        fontSize = fontSize,
        letterSpacing = 1.8.sp,
        textAlign = align,
    )
}
