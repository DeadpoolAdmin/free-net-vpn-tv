package com.freevpn.androidtv

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.freevpn.androidtv.ui.account.AccountViewModel
import com.freevpn.androidtv.ui.activation.ActivationViewModel
import com.freevpn.androidtv.ui.home.HomeViewModel
import com.freevpn.androidtv.ui.servers.ServersViewModel
import com.freevpn.androidtv.ui.settings.SettingsViewModel
import com.freevpn.androidtv.ui.welcome.WelcomeViewModel

/** Constructs ViewModels from the [AppContainer] (manual DI). */
class AppViewModelFactory(private val container: AppContainer) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = when {
        modelClass.isAssignableFrom(WelcomeViewModel::class.java) ->
            WelcomeViewModel(container.accountRepository)

        modelClass.isAssignableFrom(AccountViewModel::class.java) ->
            AccountViewModel(container.accountRepository)

        modelClass.isAssignableFrom(ActivationViewModel::class.java) ->
            ActivationViewModel(container.deviceRepository)

        modelClass.isAssignableFrom(HomeViewModel::class.java) ->
            HomeViewModel(container.vpnManager, container.serverRepository, container.configStorage)

        modelClass.isAssignableFrom(ServersViewModel::class.java) ->
            ServersViewModel(container.serverRepository, container.configStorage)

        modelClass.isAssignableFrom(SettingsViewModel::class.java) ->
            SettingsViewModel(container.configStorage, container.deviceRepository)

        else -> throw IllegalArgumentException("Unknown ViewModel: ${modelClass.name}")
    } as T
}
