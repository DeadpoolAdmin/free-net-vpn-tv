package com.freevpn.androidtv.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.freevpn.androidtv.data.DeviceRepository
import com.freevpn.androidtv.storage.ConfigStorage
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Free-only settings. No paid/subscription/upgrade options exist. */
class SettingsViewModel(
    private val configStorage: ConfigStorage,
    private val deviceRepository: DeviceRepository,
) : ViewModel() {

    data class UiState(
        val autoConnect: Boolean = false,
        val autoReconnect: Boolean = true,
        val showNotification: Boolean = true,
    )

    val ui = combine(
        configStorage.autoConnect,
        configStorage.autoReconnect,
        configStorage.showNotification,
    ) { autoConnect, autoReconnect, showNotification ->
        UiState(autoConnect, autoReconnect, showNotification)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UiState())

    private val _activationCleared = Channel<Unit>(Channel.BUFFERED)
    val activationCleared = _activationCleared.receiveAsFlow()

    fun setAutoConnect(value: Boolean) = viewModelScope.launch { configStorage.setAutoConnect(value) }
    fun setAutoReconnect(value: Boolean) = viewModelScope.launch { configStorage.setAutoReconnect(value) }
    fun setShowNotification(value: Boolean) = viewModelScope.launch { configStorage.setShowNotification(value) }

    fun clearActivation() {
        deviceRepository.clearActivation()
        viewModelScope.launch { _activationCleared.send(Unit) }
    }
}
