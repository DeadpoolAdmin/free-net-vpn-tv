package com.freevpn.androidtv

import android.app.Application
import com.freevpn.androidtv.data.AccessRepository
import com.freevpn.androidtv.data.AccountRepository
import com.freevpn.androidtv.data.AuthRepository
import com.freevpn.androidtv.data.DeviceRepository
import com.freevpn.androidtv.data.ServerRepository
import com.freevpn.androidtv.data.VpnRepository
import com.freevpn.androidtv.network.ApiClient
import com.freevpn.androidtv.network.AuthInterceptor
import com.freevpn.androidtv.storage.AccountStorage
import com.freevpn.androidtv.storage.AccountStorageImpl
import com.freevpn.androidtv.storage.ConfigStorage
import com.freevpn.androidtv.storage.ConfigStorageImpl
import com.freevpn.androidtv.storage.DeviceStorage
import com.freevpn.androidtv.storage.DeviceStorageImpl
import com.freevpn.androidtv.storage.TokenStorage
import com.freevpn.androidtv.storage.TokenStorageImpl
import com.freevpn.androidtv.vpn.GoBackendTunnel
import com.freevpn.androidtv.vpn.SimulatedTunnel
import com.freevpn.androidtv.vpn.VpnManager
import com.freevpn.androidtv.vpn.WireGuardKeyStore
import com.freevpn.androidtv.vpn.WireGuardTunnelManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * Lightweight manual DI (ServiceLocator). Chosen over Hilt to keep the build fast and the Service's
 * access trivial on low-end TV boxes. Reachable everywhere via `(application as VpnApp).container`.
 */
class AppContainer(app: Application) {

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    // Storage
    val deviceStorage: DeviceStorage = DeviceStorageImpl(app)
    val tokenStorage: TokenStorage = TokenStorageImpl(app)
    val configStorage: ConfigStorage = ConfigStorageImpl(app)
    val accountStorage: AccountStorage = AccountStorageImpl(app)

    // Network — auth uses a token-free client (no interceptor) to avoid login recursion.
    private val authApi = ApiClient(baseUrl = BuildConfig.BASE_URL).api
    val authRepository = AuthRepository(
        authApi = authApi,
        tokenStorage = tokenStorage,
        deviceStorage = deviceStorage,
    )
    private val apiClient = ApiClient(
        baseUrl = BuildConfig.BASE_URL,
        authInterceptor = AuthInterceptor(authRepository),
    )

    // Repositories
    val serverRepository = ServerRepository(apiClient.api)
    val accountRepository = AccountRepository(apiClient.api, authRepository, deviceStorage, accountStorage)
    val deviceRepository = DeviceRepository(apiClient.api, deviceStorage, tokenStorage)
    val accessRepository = AccessRepository(apiClient.api)
    val wireGuardKeyStore = WireGuardKeyStore(app)
    val vpnRepository = VpnRepository(apiClient.api, deviceStorage, wireGuardKeyStore)

    // VPN
    val tunnelManager: WireGuardTunnelManager =
        if (BuildConfig.USE_SIMULATED_TUNNEL) SimulatedTunnel() else GoBackendTunnel(app)

    val vpnManager = VpnManager(
        appContext = app,
        vpnRepository = vpnRepository,
        accessRepository = accessRepository,
        configStorage = configStorage,
        scope = appScope,
    )

    val viewModelFactory: AppViewModelFactory by lazy { AppViewModelFactory(this) }
}
