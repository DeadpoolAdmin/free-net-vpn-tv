package com.freevpn.androidtv.ui.activation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.freevpn.androidtv.data.DeviceRepository
import com.freevpn.androidtv.model.ActivationStatus
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Device-code activation. Shows the code and polls for confirmation with a BOUNDED loop (never
 * infinite). The user can re-check manually or continue anonymously (free).
 */
class ActivationViewModel(private val deviceRepository: DeviceRepository) : ViewModel() {

    data class UiState(
        val loading: Boolean = true,
        val deviceCode: String? = null,
        val activationUrl: String? = null,
        val status: ActivationStatus = ActivationStatus.PENDING,
    )

    private val _ui = MutableStateFlow(UiState())
    val ui = _ui.asStateFlow()

    private val _activated = Channel<Unit>(Channel.BUFFERED)
    val activated = _activated.receiveAsFlow()

    private var pollJob: Job? = null

    init {
        requestCode()
    }

    /** Request a fresh code and (re)start bounded polling. */
    fun requestCode() {
        _ui.update { it.copy(loading = true) }
        viewModelScope.launch {
            apply(deviceRepository.activate())
            startPolling()
        }
    }

    /** Manual "Check again" from the remote. */
    fun checkNow() {
        viewModelScope.launch { apply(deviceRepository.activate()) }
    }

    /** Continue without activating — the app is free. */
    fun continueAnonymous() {
        deviceRepository.continueAnonymous()
        viewModelScope.launch { _activated.send(Unit) }
    }

    private fun startPolling() {
        pollJob?.cancel()
        pollJob = viewModelScope.launch {
            var attempts = 0
            while (isActive && attempts < MAX_POLLS) {
                delay(POLL_INTERVAL_MS)
                attempts++
                apply(deviceRepository.activate())
                if (_ui.value.status == ActivationStatus.ACTIVATED) break
            }
        }
    }

    private suspend fun apply(result: com.freevpn.androidtv.model.DeviceActivation) {
        _ui.update {
            it.copy(
                loading = false,
                deviceCode = result.deviceCode ?: it.deviceCode,
                activationUrl = result.activationUrl ?: it.activationUrl,
                status = result.status,
            )
        }
        when (result.status) {
            ActivationStatus.ACTIVATED, ActivationStatus.NOT_REQUIRED -> _activated.send(Unit)
            else -> Unit
        }
    }

    override fun onCleared() {
        pollJob?.cancel()
        super.onCleared()
    }

    private companion object {
        const val POLL_INTERVAL_MS = 5_000L
        const val MAX_POLLS = 60 // ~5 minutes, then stop polling (user can re-check manually)
    }
}
