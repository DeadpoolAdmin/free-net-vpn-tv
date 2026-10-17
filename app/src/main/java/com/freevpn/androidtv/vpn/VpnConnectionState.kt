package com.freevpn.androidtv.vpn

import com.freevpn.androidtv.model.VpnServer

/**
 * The five VPN states the UI reacts to. [Connected] carries the active server and the epoch the
 * tunnel came up (so the timer and notification can be reconstructed after recomposition).
 */
sealed interface VpnConnectionState {

    data object Disconnected : VpnConnectionState

    data object Connecting : VpnConnectionState

    data class Connected(val server: VpnServer, val sinceEpochMs: Long) : VpnConnectionState

    data object Disconnecting : VpnConnectionState

    data class Error(val reason: ErrorReason) : VpnConnectionState

    val isBusy: Boolean get() = this is Connecting || this is Disconnecting
    val isConnected: Boolean get() = this is Connected
}

enum class ErrorReason {
    PermissionDenied,
    ConfigFetchFailed,
    TunnelSetupFailed,
    NetworkLost,
    AccessDenied,
    Unknown,
}
