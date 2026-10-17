package com.freevpn.androidtv.ui.servers

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.freevpn.androidtv.R
import com.freevpn.androidtv.model.ServerStatus
import com.freevpn.androidtv.model.VpnServer
import com.freevpn.androidtv.ui.common.AppBackground
import com.freevpn.androidtv.ui.common.CountryFlag
import com.freevpn.androidtv.ui.common.DesignCard
import com.freevpn.androidtv.ui.common.ScreenHeader
import com.freevpn.androidtv.ui.common.SecondaryPillButton
import com.freevpn.androidtv.ui.common.appViewModelFactory
import com.freevpn.androidtv.ui.common.whiteA
import com.freevpn.androidtv.ui.theme.AccentCyan
import com.freevpn.androidtv.ui.theme.DmSans
import com.freevpn.androidtv.ui.theme.StatusGreen
import com.freevpn.androidtv.ui.theme.StatusOrange
import com.freevpn.androidtv.ui.theme.StatusRed
import com.freevpn.androidtv.ui.theme.StatusGray
import com.freevpn.androidtv.ui.theme.TextDim
import com.freevpn.androidtv.ui.theme.TextPrimary
import com.freevpn.androidtv.ui.theme.TextSecondary
import com.freevpn.androidtv.ui.theme.TextTertiary

@Composable
fun ServersScreen(
    onServerChosen: () -> Unit,
    onBack: () -> Unit,
    viewModel: ServersViewModel = viewModel(factory = appViewModelFactory()),
) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    val firstItemFocus = remember { FocusRequester() }
    var showHint by remember { mutableStateOf(false) }

    // Countries with their sub-locations (cities). The backend returns a flat server list, so we group
    // by country here; each country is an expandable header revealing its cities.
    val groups = remember(ui.servers) { ui.servers.groupByCountry() }
    val expandedCountries = remember { mutableStateMapOf<String, Boolean>() }

    LaunchedEffect(Unit) { viewModel.serverChosen.collect { onServerChosen() } }
    LaunchedEffect(ui.loading, ui.servers.size) {
        if (!ui.loading && ui.servers.isNotEmpty()) {
            runCatching { firstItemFocus.requestFocus() }
        }
    }

    AppBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 110.dp, vertical = 56.dp),
        ) {
            ScreenHeader(
                title = stringResource(R.string.servers_title),
                onBack = onBack,
                trailing = {
                    SecondaryPillButton(
                        text = stringResource(R.string.servers_refresh),
                        onClick = viewModel::refresh,
                        leadingIcon = R.drawable.ic_refresh,
                        horizontal = 26.dp,
                        vertical = 13.dp,
                    )
                },
            )

            Spacer(Modifier.height(10.dp))
            Legend(modifier = Modifier.padding(start = 76.dp))
            Spacer(Modifier.height(26.dp))

            when {
                ui.loading -> Text(
                    text = stringResource(R.string.activation_waiting),
                    style = MaterialTheme.typography.titleMedium,
                    color = TextTertiary,
                )

                ui.servers.isEmpty() -> Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(
                        text = stringResource(R.string.servers_empty),
                        style = MaterialTheme.typography.titleMedium,
                        color = TextTertiary,
                    )
                    SecondaryPillButton(
                        text = stringResource(R.string.servers_refresh),
                        onClick = viewModel::refresh,
                        leadingIcon = R.drawable.ic_refresh,
                    )
                }

                else -> LazyColumn(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    // Top/bottom breathing room so the focused row's 3dp focus border is never clipped by
                    // the list bounds (the first and last items especially).
                    contentPadding = PaddingValues(top = 18.dp, bottom = 32.dp),
                ) {
                    groups.forEachIndexed { index, group ->
                        item(key = "country:${group.country}") {
                            CountryGroupHeader(
                                group = group,
                                expanded = expandedCountries[group.country] == true,
                                hasSelected = group.servers.any { it.id == ui.selectedId },
                                onToggle = {
                                    val open = expandedCountries[group.country] == true
                                    expandedCountries[group.country] = !open
                                },
                                modifier = if (index == 0) {
                                    Modifier.focusRequester(firstItemFocus)
                                } else Modifier,
                            )
                        }
                        if (expandedCountries[group.country] == true) {
                            items(group.servers, key = { "server:${it.id}" }) { server ->
                                ServerRow(
                                    server = server,
                                    isSelected = server.id == ui.selectedId,
                                    asSubLocation = true,
                                    onClick = {
                                        if (server.status.isConnectable) {
                                            showHint = false
                                            viewModel.onSelect(server)
                                        } else {
                                            showHint = true
                                        }
                                    },
                                )
                            }
                        }
                    }
                }
            }

            if (showHint) {
                Spacer(Modifier.height(22.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(
                        painter = painterResource(R.drawable.ic_warning),
                        contentDescription = null,
                        tint = StatusRed,
                        modifier = Modifier.size(22.dp),
                    )
                    Text(
                        text = stringResource(R.string.servers_unavailable_hint),
                        style = MaterialTheme.typography.titleSmall,
                        color = StatusRed,
                    )
                }
            }
        }
    }
}

@Composable
private fun Legend(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        LegendItem(StatusGreen, stringResource(R.string.servers_status_available))
        LegendItem(StatusOrange, stringResource(R.string.servers_status_busy))
        LegendItem(StatusGray, stringResource(R.string.servers_status_unavailable))
    }
}

@Composable
private fun LegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(Modifier.size(9.dp).background(color, RoundedCornerShape(50)))
        Text(text = label, style = MaterialTheme.typography.bodySmall, color = TextDim)
    }
}

@Composable
private fun ServerRow(
    server: VpnServer,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    // When true this row is a city nested under a country header: indented, no flag, lighter card, and
    // labelled by city only (the country is already shown by the header above it).
    asSubLocation: Boolean = false,
) {
    val dimmed = server.status == ServerStatus.UNAVAILABLE
    DesignCard(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().then(if (asSubLocation) Modifier.padding(start = 46.dp) else Modifier),
        containerColor = if (asSubLocation) whiteA(0.02f) else whiteA(0.04f),
        focusedBorderColor = AccentCyan.copy(alpha = 0.5f),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .alpha(if (dimmed) 0.45f else 1f)
                .padding(vertical = if (asSubLocation) 16.dp else 22.dp)
                .padding(start = 18.dp, end = 30.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(if (asSubLocation) 18.dp else 24.dp),
        ) {
            // selected accent bar (reserves width either way to avoid shift)
            Box(
                Modifier
                    .size(width = 5.dp, height = if (asSubLocation) 40.dp else 58.dp)
                    .background(
                        if (isSelected) AccentCyan else Color.Transparent,
                        RoundedCornerShape(999.dp),
                    )
            )

            // flag (top-level rows only — sub-locations inherit their country's flag from the header)
            if (asSubLocation) {
                Box(
                    Modifier.size(10.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(Modifier.size(7.dp).background(whiteA(0.35f), RoundedCornerShape(50)))
                }
            } else {
                CountryFlag(country = server.country, size = 58.dp, corner = 15.dp)
            }

            // name + status
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = if (asSubLocation) server.city else server.locationLabel,
                        fontFamily = DmSans,
                        fontWeight = FontWeight.Bold,
                        fontSize = if (asSubLocation) 23.sp else 27.sp,
                        color = TextPrimary,
                    )
                    if (server.isRecommended) RecommendedBadge()
                }
                Spacer(Modifier.height(5.dp))
                val (statusLabel, statusColor) = server.status.display()
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(Modifier.size(9.dp).background(statusColor, RoundedCornerShape(50)))
                    Text(
                        text = statusLabel,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = statusColor,
                    )
                }
            }

            // load
            Column(modifier = Modifier.width(220.dp)) {
                val load = server.loadPercent ?: 0
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Load", style = MaterialTheme.typography.labelSmall, fontSize = 17.sp, color = TextDim)
                    Text("$load%", style = MaterialTheme.typography.labelSmall, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = TextSecondary)
                }
                Spacer(Modifier.height(7.dp))
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(7.dp)
                        .background(whiteA(0.1f), RoundedCornerShape(999.dp)),
                ) {
                    Box(
                        Modifier
                            .fillMaxWidth(fraction = (load / 100f).coerceIn(0f, 1f))
                            .height(7.dp)
                            .background(loadColor(load), RoundedCornerShape(999.dp)),
                    )
                }
            }

            // selected check
            Box(Modifier.size(28.dp), contentAlignment = Alignment.Center) {
                if (isSelected) {
                    Icon(
                        painter = painterResource(R.drawable.ic_check_circle_filled),
                        contentDescription = null,
                        tint = Color.Unspecified,
                        modifier = Modifier.size(28.dp),
                    )
                }
            }
        }
    }
}

/** A country and its sub-locations (cities). Built from the flat backend list by [groupByCountry]. */
private data class CountryGroup(
    val country: String,
    val servers: List<VpnServer>,
)

/**
 * Group the flat server list into countries, preserving first-appearance order of both countries and
 * the cities within each (groupBy keeps encounter order). A city that is [ServerStatus.AVAILABLE]
 * sorts ahead of busy/unavailable ones so the best sub-location is on top when a country expands.
 */
private fun List<VpnServer>.groupByCountry(): List<CountryGroup> =
    groupBy { it.country }.map { (country, servers) ->
        CountryGroup(
            country = country,
            servers = servers.sortedByDescending { it.status == ServerStatus.AVAILABLE },
        )
    }

@Composable
private fun CountryGroupHeader(
    group: CountryGroup,
    expanded: Boolean,
    hasSelected: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val count = group.servers.size
    val availableCount = group.servers.count { it.status == ServerStatus.AVAILABLE }
    val summaryColor = when {
        availableCount > 0 -> StatusGreen
        group.servers.any { it.status == ServerStatus.BUSY } -> StatusOrange
        else -> StatusGray
    }
    DesignCard(
        onClick = onToggle,
        modifier = modifier.fillMaxWidth(),
        focusedBorderColor = AccentCyan.copy(alpha = 0.5f),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 20.dp)
                .padding(start = 18.dp, end = 30.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            // selected accent bar — highlighted when one of this country's cities is the chosen server
            Box(
                Modifier
                    .size(width = 5.dp, height = 58.dp)
                    .background(
                        if (hasSelected) AccentCyan else Color.Transparent,
                        RoundedCornerShape(999.dp),
                    )
            )

            CountryFlag(country = group.country, size = 58.dp, corner = 15.dp)

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = group.country,
                    fontFamily = DmSans,
                    fontWeight = FontWeight.Bold,
                    fontSize = 27.sp,
                    color = TextPrimary,
                )
                Spacer(Modifier.height(5.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(Modifier.size(9.dp).background(summaryColor, RoundedCornerShape(50)))
                    Text(
                        text = if (count == 1) "1 location" else "$count locations",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = TextSecondary,
                    )
                    if (availableCount > 0) {
                        Text(
                            text = "· $availableCount available",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextDim,
                        )
                    }
                }
            }

            // expand/collapse chevron — points right when collapsed, rotates down when expanded
            Icon(
                painter = painterResource(R.drawable.ic_chevron_right),
                contentDescription = if (expanded) "Collapse" else "Expand",
                tint = TextSecondary,
                modifier = Modifier
                    .size(26.dp)
                    .rotate(if (expanded) 90f else 0f),
            )
        }
    }
}

@Composable
private fun RecommendedBadge() {
    Text(
        text = stringResource(R.string.servers_recommended).uppercase(),
        fontFamily = com.freevpn.androidtv.ui.theme.Inter,
        fontWeight = FontWeight.Bold,
        fontSize = 13.sp,
        letterSpacing = 0.5.sp,
        color = AccentCyan,
        modifier = Modifier
            .background(AccentCyan.copy(alpha = 0.12f), RoundedCornerShape(999.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp),
    )
}

private fun ServerStatus.display(): Pair<String, Color> = when (this) {
    ServerStatus.AVAILABLE -> "Available" to StatusGreen
    ServerStatus.BUSY -> "Busy" to StatusOrange
    ServerStatus.UNAVAILABLE -> "Unavailable" to StatusGray
}

private fun loadColor(load: Int): Color = when {
    load >= 85 -> StatusRed
    load >= 60 -> StatusOrange
    else -> StatusGreen
}
