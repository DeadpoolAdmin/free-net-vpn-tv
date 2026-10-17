package com.freevpn.androidtv.ui.welcome

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.freevpn.androidtv.data.AccountRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlin.system.measureTimeMillis

/**
 * On entry, logs in and registers this device with the backend (POST /api/device). On success the
 * device/account record is cached and we navigate straight to the connect (Home) screen; on failure
 * we surface a retry rather than stranding the user on an endless spinner.
 */
class WelcomeViewModel(private val accountRepository: AccountRepository) : ViewModel() {

    private val _state = MutableStateFlow(WelcomeUiState.Loading)
    val state = _state.asStateFlow()

    private val _authenticated = Channel<Unit>(Channel.BUFFERED)
    val authenticated = _authenticated.receiveAsFlow()

    init { authenticate() }

    /**
     * (Re)run login + device registration. Signals navigation on success, shows an error otherwise.
     * On success we hold the branded splash for at least [MIN_SPLASH_MS] so it never flashes by when
     * auth resolves quickly.
     */
    fun authenticate() {
        _state.value = WelcomeUiState.Loading
        viewModelScope.launch {
            var success = false
            val elapsed = measureTimeMillis {
                success = accountRepository.registerDevice() != null
            }
            if (success) {
                delay((MIN_SPLASH_MS - elapsed).coerceAtLeast(0))
                _authenticated.send(Unit)
            } else {
                _state.value = WelcomeUiState.Error
            }
        }
    }

    private companion object {
        /** Minimum time the branded splash stays visible before navigating to Home. */
        const val MIN_SPLASH_MS = 5_000L
    }
}

enum class WelcomeUiState { Loading, Error }
