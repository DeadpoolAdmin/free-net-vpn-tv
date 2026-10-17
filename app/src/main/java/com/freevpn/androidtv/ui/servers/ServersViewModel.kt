package com.freevpn.androidtv.ui.servers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.freevpn.androidtv.data.ServerRepository
import com.freevpn.androidtv.model.VpnServer
import com.freevpn.androidtv.storage.ConfigStorage
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Loads the free server list and persists the user's selection. */
class ServersViewModel(
    private val serverRepository: ServerRepository,
    private val configStorage: ConfigStorage,
) : ViewModel() {

    data class UiState(
        val loading: Boolean = true,
        val servers: List<VpnServer> = emptyList(),
        val selectedId: String? = null,
    )

    private val _ui = MutableStateFlow(UiState())
    val ui = _ui.asStateFlow()

    private val _serverChosen = Channel<Unit>(Channel.BUFFERED)
    val serverChosen = _serverChosen.receiveAsFlow()

    init {
        load()
        viewModelScope.launch {
            configStorage.lastServerId.collect { id -> _ui.update { it.copy(selectedId = id) } }
        }
    }

    /** Initial load — uses the repository cache when available, so a revisit is instant. */
    fun load() = fetch(forceRefresh = false)

    /** User-triggered Refresh — always bypasses the cache and hits the network. */
    fun refresh() = fetch(forceRefresh = true)

    private fun fetch(forceRefresh: Boolean) {
        // Only show the loading state when there's nothing to display yet; on a cached revisit or a
        // refresh-over-existing-list we keep the current rows on screen instead of flashing "Waiting…".
        _ui.update { it.copy(loading = it.servers.isEmpty()) }
        viewModelScope.launch {
            val list = serverRepository.getServers(forceRefresh)
            _ui.update { it.copy(loading = false, servers = list) }
        }
    }

    fun onSelect(server: VpnServer) {
        if (!server.status.isConnectable) return // unavailable servers can't be chosen
        viewModelScope.launch {
            configStorage.setLastServerId(server.id)
            _serverChosen.send(Unit)
        }
    }
}
