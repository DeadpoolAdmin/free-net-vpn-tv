package com.freevpn.androidtv.ui.common

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import kotlin.math.hypot

/**
 * A halftone "location map": a grid of dots drawn only where a cell falls on land, forming a
 * recognisable world map behind the home content. The whole thing is decorative — continents are
 * deliberately low-poly (just enough to read as Earth), so a tiny set of lon/lat outlines replaces
 * any bitmap asset and scales crisply at any TV resolution.
 *
 * The dot field, positions and per-dot alpha are computed once per size inside [drawWithCache] and
 * only the recorded draw is replayed, so the screen's once-a-second recompositions (the connected
 * timer) never re-run the point-in-polygon work.
 *
 * @param color   dot colour (defaults to the brand teal).
 * @param peakAlpha brightness at the focal point; dots fade radially toward the edges.
 * @param focusX/[focusY] focal point of the radial fade, as a fraction of the size.
 */
@Composable
fun WorldDotMap(
    modifier: Modifier = Modifier,
    color: Color = MapDotTeal,
    peakAlpha: Float = 0.5f,
    focusX: Float = 0.5f,
    focusY: Float = 0.46f,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .drawWithCache {
                val w = size.width
                val h = size.height

                // Equirectangular world is 2:1; fit it to the width and centre it vertically.
                val mapH = w / 2f
                val mapTop = (h - mapH) / 2f
                val spacing = (w / 116f).coerceAtLeast(8f)
                val dotR = (spacing * 0.16f).coerceIn(1.1f, 2.6f)

                val cx = w * focusX
                val cy = h * focusY
                val maxD = hypot(w * 0.62f, h * 0.62f).coerceAtLeast(1f)

                val pts = ArrayList<Offset>(4096)
                val alphas = ArrayList<Float>(4096)

                var y = mapTop + spacing / 2f
                val mapBottom = mapTop + mapH
                while (y < mapBottom) {
                    val lat = 90f - ((y - mapTop) / mapH) * 180f
                    var x = spacing / 2f
                    while (x < w) {
                        val lon = (x / w) * 360f - 180f
                        if (isLand(lon, lat)) {
                            val falloff = (1f - hypot(x - cx, y - cy) / maxD).coerceIn(0f, 1f)
                            pts.add(Offset(x, y))
                            alphas.add(peakAlpha * (0.14f + 0.86f * falloff * falloff))
                        }
                        x += spacing
                    }
                    y += spacing
                }

                onDrawBehind {
                    for (i in pts.indices) {
                        drawCircle(color = color, radius = dotR, center = pts[i], alpha = alphas[i])
                    }
                }
            },
    )
}

/** Brand teal for the dotted map. */
val MapDotTeal: Color = Color(0xFF45E1D0)

private fun isLand(lon: Float, lat: Float): Boolean {
    for (ring in CONTINENTS) if (pointInRing(lon, lat, ring)) return true
    return false
}

/** Standard even-odd ray-cast point-in-polygon, operating directly on (lon, lat). */
private fun pointInRing(lon: Float, lat: Float, ring: FloatArray): Boolean {
    var inside = false
    val n = ring.size / 2
    var j = n - 1
    for (i in 0 until n) {
        val xi = ring[i * 2]; val yi = ring[i * 2 + 1]
        val xj = ring[j * 2]; val yj = ring[j * 2 + 1]
        if (((yi > lat) != (yj > lat)) &&
            (lon < (xj - xi) * (lat - yi) / (yj - yi) + xi)
        ) {
            inside = !inside
        }
        j = i
    }
    return inside
}

/**
 * Continent outlines as flat (longitude, latitude) vertex rings, in degrees. Low-poly on purpose:
 * this is ambient texture, not a survey map — it only needs to read as a world map.
 */
private val CONTINENTS: List<FloatArray> = listOf(
    // North America
    floatArrayOf(
        -168f, 66f, -160f, 71f, -130f, 70f, -95f, 72f, -80f, 68f, -64f, 60f, -52f, 47f,
        -66f, 44f, -70f, 41f, -75f, 35f, -81f, 25f, -97f, 18f, -105f, 22f, -112f, 29f,
        -117f, 33f, -124f, 40f, -129f, 50f, -140f, 60f, -150f, 59f, -165f, 60f,
    ),
    // Greenland
    floatArrayOf(-45f, 60f, -20f, 70f, -22f, 80f, -45f, 83f, -62f, 80f, -55f, 68f),
    // Central America
    floatArrayOf(-92f, 18f, -83f, 15f, -78f, 9f, -82f, 7f, -88f, 13f, -95f, 16f),
    // South America
    floatArrayOf(
        -80f, 8f, -60f, 10f, -50f, 2f, -35f, -6f, -38f, -13f, -48f, -25f, -58f, -35f,
        -65f, -45f, -72f, -52f, -74f, -45f, -71f, -30f, -72f, -18f, -78f, -5f, -80f, 2f,
    ),
    // Europe
    floatArrayOf(
        -10f, 37f, -9f, 44f, 0f, 49f, -5f, 58f, 10f, 64f, 25f, 70f, 42f, 66f, 45f, 50f,
        30f, 45f, 18f, 40f, 8f, 44f, -2f, 40f,
    ),
    // British Isles
    floatArrayOf(-6f, 50f, -3f, 54f, -6f, 58f, -8f, 55f, -6f, 51f),
    // Africa
    floatArrayOf(
        -17f, 15f, -17f, 26f, -5f, 36f, 11f, 37f, 25f, 32f, 35f, 31f, 43f, 12f, 51f, 12f,
        43f, -2f, 40f, -15f, 33f, -26f, 25f, -34f, 18f, -35f, 12f, -18f, 8f, 4f, -8f, 5f, -15f, 11f,
    ),
    // Madagascar
    floatArrayOf(44f, -16f, 50f, -15f, 50f, -25f, 45f, -25f),
    // Arabia
    floatArrayOf(35f, 30f, 48f, 29f, 58f, 22f, 52f, 15f, 43f, 12f, 35f, 24f),
    // Asia
    floatArrayOf(
        43f, 48f, 50f, 68f, 70f, 75f, 100f, 78f, 140f, 73f, 165f, 70f, 172f, 66f, 162f, 60f,
        145f, 58f, 138f, 52f, 135f, 48f, 127f, 40f, 122f, 30f, 108f, 22f, 100f, 16f, 95f, 20f,
        88f, 22f, 80f, 8f, 75f, 18f, 68f, 24f, 60f, 25f, 55f, 38f, 48f, 42f,
    ),
    // Southeast Asia / Indonesia
    floatArrayOf(98f, 6f, 106f, 1f, 118f, -4f, 122f, -8f, 132f, -4f, 128f, 2f, 118f, 6f, 105f, 8f),
    // Japan
    floatArrayOf(137f, 34f, 141f, 38f, 142f, 43f, 138f, 36f, 136f, 34f),
    // Australia
    floatArrayOf(
        114f, -22f, 122f, -18f, 130f, -12f, 137f, -12f, 143f, -12f, 150f, -24f, 153f, -30f,
        147f, -38f, 138f, -37f, 129f, -32f, 118f, -35f, 113f, -26f,
    ),
    // New Zealand
    floatArrayOf(172f, -41f, 174f, -37f, 178f, -39f, 176f, -46f, 170f, -46f),
)
