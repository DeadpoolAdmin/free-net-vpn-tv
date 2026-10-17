package com.freevpn.androidtv.vpn

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Process-wide single source of truth for the VPN state. Both [MyVpnService] (via [VpnManager]) and
 * the UI live in the same process, so a conflated [StateFlow] is the simplest correct bridge: a
 * screen that mounts mid-connection immediately reads the current value — no IPC, no sticky
 * broadcasts. Only [VpnManager] is allowed to write (the setter is internal to the module).
 */
object VpnStateHolder {

    private val _state = MutableStateFlow<VpnConnectionState>(VpnConnectionState.Disconnected)
    val state: StateFlow<VpnConnectionState> = _state.asStateFlow()

    internal fun set(newState: VpnConnectionState) {
        _state.value = newState
    }
}
