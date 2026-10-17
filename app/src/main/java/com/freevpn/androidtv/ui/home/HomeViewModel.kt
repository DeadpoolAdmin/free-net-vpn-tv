package com.freevpn.androidtv.ui.home

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.freevpn.androidtv.R
import com.freevpn.androidtv.data.ServerRepository
import com.freevpn.androidtv.model.AccessStatus
import com.freevpn.androidtv.model.VpnServer
import com.freevpn.androidtv.storage.ConfigStorage
import com.freevpn.androidtv.ui.common.messageResOrNull
import com.freevpn.androidtv.vpn.VpnConnectionState
import com.freevpn.androidtv.vpn.VpnManager
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

/**
 * Drives the main VPN screen. Holds the selected server, performs the free-access gate before
 * requesting VPN permission, and exposes the live connection state/timer from [VpnManager].
 *
 * The actual permission dialog + connect call happen in the Activity (only an Activity can show the
 * system dialog); this VM emits a [HomeEvent.RequestPermission] that the screen forwards to it.
 */
class HomeViewModel(
    private val vpnManager: VpnManager,
    private val serverRepository: ServerRepository,
    private val configStorage: ConfigStorage,
) : ViewModel() {

    val connectionState: StateFlow<VpnConnectionState> = vpnManager.state
    val elapsedSeconds: StateFlow<Long> = vpnManager.elapsedSeconds

    private val _selectedServer = MutableStateFlow<VpnServer?>(null)
    val selectedServer = _selectedServer.asStateFlow()

    /** Persistent banner string-res (e.g. LIMITED). null = no banner. */
    private val _accessBanner = MutableStateFlow<Int?>(null)
    val accessBanner: StateFlow<Int?> = _accessBanner.asStateFlow()

    private val _events = Channel<HomeEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    private var servers: List<VpnServer> = emptyList()
    private var autoConnectChecked = false

    init {
        viewModelScope.launch {
            servers = serverRepository.getServers()
            configStorage.lastServerId.collect { id ->
                _selectedServer.value = resolveServer(id)
            }
        }
    }

    private fun resolveServer(id: String?): VpnServer? =
        servers.firstOrNull { it.id == id }
            ?: servers.firstOrNull { it.isRecommended }
            ?: servers.firstOrNull()

    /** The primary Connect/Disconnect/Cancel button. */
    fun onPrimaryAction() {
        when (connectionState.value) {
            is VpnConnectionState.Connected,
            is VpnConnectionState.Connecting -> vpnManager.disconnect()
            is VpnConnectionState.Disconnecting -> Unit // ignore until settled
            else -> startConnectFlow()
        }
    }

    private fun startConnectFlow() {
        val server = _selectedServer.value
        if (server == null) {
            viewModelScope.launch { _events.send(HomeEvent.OpenServers) }
            return
        }
        viewModelScope.launch {
            val access = vpnManager.checkAccess()
            _accessBanner.value = access.status.messageResOrNull()
            if (access.canConnect) {
                _events.send(HomeEvent.RequestPermission(server))
            } else {
                _events.send(HomeEvent.ShowMessage(access.status.messageResOrNull() ?: R.string.error_unknown))
            }
        }
    }

    /** Called once when the screen first appears; honors the auto-connect setting. */
    fun maybeAutoConnect() {
        if (autoConnectChecked) return
        autoConnectChecked = true
        viewModelScope.launch {
            val enabled = configStorage.autoConnect.first()
            if (enabled && connectionState.value is VpnConnectionState.Disconnected) {
                if (_selectedServer.value == null) _selectedServer.value = resolveServer(null)
                startConnectFlow()
            }
        }
    }

    fun reconcile() = vpnManager.reconcile()
}

sealed interface HomeEvent {
    data class RequestPermission(val server: VpnServer) : HomeEvent
    data object OpenServers : HomeEvent
    data class ShowMessage(@StringRes val res: Int) : HomeEvent
}
