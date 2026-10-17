package com.freevpn.androidtv.ui.common

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Icon
import com.freevpn.androidtv.R
import com.freevpn.androidtv.ui.theme.Primary
import com.freevpn.androidtv.ui.theme.TextSecondary

/**
 * A real flag for the given [country], rendered as a rounded tile. Uses bundled vector artwork for the
 * countries the app ships and falls back to a neutral globe tile for anything unmapped (or null). The
 * flag fills the tile (cropped) with a faint border so light-coloured flags read cleanly against the
 * dark background.
 */
@Composable
fun CountryFlag(
    country: String?,
    modifier: Modifier = Modifier,
    size: Dp = 58.dp,
    corner: Dp = 15.dp,
) {
    val shape = RoundedCornerShape(corner)
    val flag = flagResFor(country)
    if (flag != null) {
        Image(
            painter = painterResource(flag),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = modifier
                .size(size)
                .clip(shape)
                .border(1.dp, whiteA(0.12f), shape),
        )
    } else {
        Box(
            modifier = modifier
                .size(size)
                .background(Primary.copy(alpha = 0.18f), shape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_globe),
                contentDescription = null,
                tint = TextSecondary,
                modifier = Modifier.size(size * 0.5f),
            )
        }
    }
}

private fun flagResFor(country: String?): Int? = when (country?.lowercase()) {
    "germany" -> R.drawable.flag_de
    "netherlands" -> R.drawable.flag_nl
    "united states" -> R.drawable.flag_us
    "united kingdom" -> R.drawable.flag_gb
    "france" -> R.drawable.flag_fr
    "sweden" -> R.drawable.flag_se
    else -> null
}
