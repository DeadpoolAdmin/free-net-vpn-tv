package com.freevpn.androidtv.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
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
import com.freevpn.androidtv.BuildConfig
import com.freevpn.androidtv.R
import com.freevpn.androidtv.model.VpnServer
import com.freevpn.androidtv.ui.common.AppLogo
import com.freevpn.androidtv.ui.common.AppBackground
import com.freevpn.androidtv.ui.common.WorldDotMap
import com.freevpn.androidtv.ui.common.CountryFlag
import com.freevpn.androidtv.ui.common.DesignCard
import com.freevpn.androidtv.ui.common.Eyebrow
import com.freevpn.androidtv.ui.common.FocusRing
import com.freevpn.androidtv.ui.common.SecondaryPillButton
import com.freevpn.androidtv.ui.common.appViewModelFactory
import com.freevpn.androidtv.ui.common.formatDuration
import com.freevpn.androidtv.ui.common.whiteA
import com.freevpn.androidtv.ui.theme.AccentCyan
import com.freevpn.androidtv.ui.theme.AccentCyanDark
import com.freevpn.androidtv.ui.theme.DmSans
import com.freevpn.androidtv.ui.theme.Primary
import com.freevpn.androidtv.ui.theme.StatusOrange
import com.freevpn.androidtv.ui.theme.StatusRed
import com.freevpn.androidtv.ui.theme.TextDim
import com.freevpn.androidtv.ui.theme.TextPrimary
import com.freevpn.androidtv.ui.theme.TextTertiary
import com.freevpn.androidtv.vpn.VpnConnectionState

private val SimBannerGold = Color(0xFFC9A86A)
private val ErrorText = Color(0xFFF2828A)

@Composable
fun HomeScreen(
    onRequestVpnPermission: (VpnServer) -> Unit,
    onOpenServers: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenAccount: () -> Unit,
    viewModel: HomeViewModel = viewModel(factory = appViewModelFactory()),
) {
    val state by viewModel.connectionState.collectAsStateWithLifecycle()
    val elapsed by viewModel.elapsedSeconds.collectAsStateWithLifecycle()
    val selectedServer by viewModel.selectedServer.collectAsStateWithLifecycle()
    val accessBanner by viewModel.accessBanner.collectAsStateWithLifecycle()

    @Suppress("UNUSED_VARIABLE")
    var transientMessage by remember { mutableStateOf<Int?>(null) }
    val connectFocus = remember { FocusRequester() }
    var connectFocused by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is HomeEvent.RequestPermission -> onRequestVpnPermission(event.server)
                is HomeEvent.OpenServers -> onOpenServers()
                is HomeEvent.ShowMessage -> transientMessage = event.res
            }
        }
    }
    LaunchedEffect(Unit) {
        runCatching { connectFocus.requestFocus() }
        viewModel.maybeAutoConnect()
    }

    val isConnected = state is VpnConnectionState.Connected
    val isConnecting = state is VpnConnectionState.Connecting
    val isDisconnecting = state is VpnConnectionState.Disconnecting
    val isError = state is VpnConnectionState.Error
    val elapsedTime = formatDuration(if (isConnected) elapsed else 0L)

    AppBackground {
        // Ambient dotted world map behind the content (above the gradient, below the UI).
        WorldDotMap()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 80.dp, end = 80.dp, top = 56.dp),
        ) {
            // ===== TOP HEADER =====
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AppLogo(size = 104.dp)
                StatusPill(state)
            }

            // ===== CENTER HERO =====
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                accessBanner?.let { res ->
                    AccessBanner(res)
                    Spacer(Modifier.height(30.dp))
                }

                // Connect button with focus ring
                Box(
                    modifier = Modifier
                        .size(392.dp)
                        .focusRequester(connectFocus)
                        .onFocusChanged { connectFocused = it.isFocused }
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) { viewModel.onPrimaryAction() },
                    contentAlignment = Alignment.Center,
                ) {
                    if (connectFocused) {
                        Box(Modifier.size(388.dp).border(3.dp, FocusRing, CircleShape))
                    }
                    PremiumConnectButton(
                        isConnected = isConnected,
                        isConnecting = isConnecting,
                        isDisconnecting = isDisconnecting,
                        isError = isError,
                        elapsedTime = elapsedTime,
                    )
                }

                Spacer(Modifier.height(30.dp))

                // Hero subtitle
                Box(modifier = Modifier.height(36.dp), contentAlignment = Alignment.Center) {
                    val country = selectedServer?.country ?: "—"
                    val city = selectedServer?.city ?: "—"
                    Text(
                        text = when {
                            isConnected -> "Your connection is private · $country — $city"
                            isConnecting -> "Establishing a secure tunnel…"
                            isDisconnecting -> "Closing the tunnel…"
                            isError -> stringResource(R.string.error_tunnel_failed)
                            else -> stringResource(R.string.connect_securely_free)
                        },
                        style = MaterialTheme.typography.bodyLarge,
                        fontSize = 25.sp,
                        color = when {
                            isConnected -> AccentCyan
                            isError -> ErrorText
                            else -> TextTertiary
                        },
                    )
                }

                Spacer(Modifier.height(30.dp))

                LocationCard(server = selectedServer, onClick = onOpenServers)

                Spacer(Modifier.height(32.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    SecondaryPillButton(
                        text = stringResource(R.string.home_change_server),
                        onClick = onOpenServers,
                        leadingIcon = R.drawable.ic_globe,
                    )
                    SecondaryPillButton(
                        text = stringResource(R.string.home_account),
                        onClick = onOpenAccount,
                        leadingIcon = R.drawable.ic_account,
                    )
                    SecondaryPillButton(
                        text = stringResource(R.string.home_settings),
                        onClick = onOpenSettings,
                        leadingIcon = R.drawable.ic_settings_gear,
                    )
                }
            }

            // ===== SIM BANNER =====
            if (BuildConfig.USE_SIMULATED_TUNNEL) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 14.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_warning),
                        contentDescription = null,
                        tint = SimBannerGold,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = stringResource(R.string.home_sim_banner),
                        style = MaterialTheme.typography.bodySmall,
                        color = SimBannerGold,
                    )
                }
            }
        }
    }
}

@Composable
private fun StatusPill(state: VpnConnectionState) {
    val (label, color) = when (state) {
        is VpnConnectionState.Connected -> stringResource(R.string.state_connected) to AccentCyan
        is VpnConnectionState.Connecting -> stringResource(R.string.state_connecting) to Primary
        is VpnConnectionState.Disconnecting -> stringResource(R.string.state_disconnecting) to TextTertiary
        is VpnConnectionState.Error -> stringResource(R.string.state_error) to StatusRed
        else -> stringResource(R.string.state_disconnected) to TextDim
    }
    Row(
        modifier = Modifier
            .background(whiteA(0.05f), RoundedCornerShape(999.dp))
            .border(1.dp, whiteA(0.1f), RoundedCornerShape(999.dp))
            .padding(horizontal = 22.dp, vertical = 11.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(12.dp).background(color, CircleShape))
        Text(text = label, style = MaterialTheme.typography.titleSmall, color = TextPrimary)
    }
}

@Composable
private fun AccessBanner(res: Int) {
    val fg = when (res) {
        R.string.access_blocked -> StatusRed
        R.string.access_maintenance -> AccentCyanDark
        else -> StatusOrange // limited / server_unavailable
    }
    Row(
        modifier = Modifier
            .widthIn(max = 900.dp)
            .background(fg.copy(alpha = 0.1f), RoundedCornerShape(16.dp))
            .border(1.dp, fg.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
            .padding(horizontal = 30.dp, vertical = 18.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_warning),
            contentDescription = null,
            tint = fg,
            modifier = Modifier.size(26.dp),
        )
        Text(
            text = stringResource(res),
            style = MaterialTheme.typography.titleMedium,
            fontSize = 23.sp,
            color = Color(0xFFEDECF7),
        )
    }
}

@Composable
private fun LocationCard(server: VpnServer?, onClick: () -> Unit) {
    DesignCard(
        onClick = onClick,
        modifier = Modifier.widthIn(min = 560.dp),
        corner = 20.dp,
        containerColor = whiteA(0.045f),
        borderColor = whiteA(0.1f),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 28.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(22.dp),
        ) {
            CountryFlag(country = server?.country, size = 52.dp, corner = 14.dp)

            Column(modifier = Modifier.weight(1f)) {
                Eyebrow(text = stringResource(R.string.home_selected_location), fontSize = 16.sp)
                Spacer(Modifier.height(4.dp))
                Text(
                    text = server?.locationLabel ?: stringResource(R.string.home_no_server),
                    fontFamily = DmSans,
                    fontWeight = FontWeight.Bold,
                    fontSize = 26.sp,
                    color = TextPrimary,
                )
            }

            Box(Modifier.size(width = 1.dp, height = 52.dp).background(whiteA(0.1f)))

            Row(horizontalArrangement = Arrangement.spacedBy(26.dp)) {
                Stat(value = server?.loadPercent?.let { "$it%" } ?: "—", label = "load")
            }

            Icon(
                painter = painterResource(R.drawable.ic_chevron_right),
                contentDescription = null,
                tint = TextDim,
                modifier = Modifier.size(22.dp),
            )
        }
    }
}

@Composable
private fun Stat(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            fontFamily = DmSans,
            fontWeight = FontWeight.Bold,
            fontSize = 24.sp,
            color = TextPrimary,
        )
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = TextDim)
    }
}

