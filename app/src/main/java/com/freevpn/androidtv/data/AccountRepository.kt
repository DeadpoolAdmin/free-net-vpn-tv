package com.freevpn.androidtv.data

import android.util.Log
import com.freevpn.androidtv.model.DeviceAccount
import com.freevpn.androidtv.network.CreateDeviceRequest
import com.freevpn.androidtv.network.VpnApi
import com.freevpn.androidtv.storage.AccountStorage
import com.freevpn.androidtv.storage.DeviceStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

/**
 * Registers this device for the backend user and exposes the resulting [DeviceAccount].
 *
 * Flow: log in (admin) to get a session → POST /api/device with this install's device id + the
 * session's user id → cache and persist the returned record. The Account screen observes [account].
 */
class AccountRepository(
    private val api: VpnApi,
    private val authRepository: AuthRepository,
    private val deviceStorage: DeviceStorage,
    private val accountStorage: AccountStorage,
) {

    private val _account = MutableStateFlow(accountStorage.get())
    val account: StateFlow<DeviceAccount?> = _account.asStateFlow()

    /**
     * Authenticates, then registers this device. Returns the account on success, or null if either
     * step fails (the caller surfaces a retry).
     */
    suspend fun registerDevice(): DeviceAccount? = withContext(Dispatchers.IO) {
        val session = authRepository.authenticate() ?: return@withContext null
        runCatching {
            api.registerDevice(
                CreateDeviceRequest(
                    deviceId = deviceStorage.getOrCreateDeviceId(),
                    userId = session.userId,
                )
            ).toDomain()
        }.onSuccess { account ->
            _account.value = account
            accountStorage.save(account)
        }.onFailure {
            Log.w(TAG, "device registration failed: ${it.message}")
        }.getOrNull()
    }

    private companion object {
        const val TAG = "AccountRepository"
    }
}
