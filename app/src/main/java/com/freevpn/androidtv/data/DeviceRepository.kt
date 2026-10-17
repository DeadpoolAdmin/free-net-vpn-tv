package com.freevpn.androidtv.data

import android.os.Build
import android.util.Log
import com.freevpn.androidtv.BuildConfig
import com.freevpn.androidtv.model.ActivationStatus
import com.freevpn.androidtv.model.DeviceActivation
import com.freevpn.androidtv.network.DeviceActivateRequest
import com.freevpn.androidtv.network.VpnApi
import com.freevpn.androidtv.storage.DeviceStorage
import com.freevpn.androidtv.storage.TokenStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Handles (optional) device-code activation. The app is anonymous-free by default; activation is
 * surfaced only when the backend returns PENDING. On activation success the returned API key is
 * stored securely for [com.freevpn.androidtv.network.AuthInterceptor].
 */
class DeviceRepository(
    private val api: VpnApi,
    private val deviceStorage: DeviceStorage,
    private val tokenStorage: TokenStorage,
) {

    fun deviceId(): String = deviceStorage.getOrCreateDeviceId()

    fun isActivated(): Boolean = deviceStorage.isActivated()

    fun clearActivation() {
        deviceStorage.clearActivation()
        tokenStorage.clear()
    }

    /** User chose to continue without activating. Remember it so we don't prompt again. */
    fun continueAnonymous() {
        deviceStorage.setActivated(true)
    }

    /**
     * Requests/refreshes activation. Persists the activated flag + API key when the device becomes
     * ACTIVATED. When the backend is unreachable the device falls back to anonymous-free access
     * (NOT_REQUIRED) so the user is never blocked from a free service during dev.
     */
    suspend fun activate(): DeviceActivation = withContext(Dispatchers.IO) {
        try {
            val result = api.activateDevice(
                DeviceActivateRequest(
                    deviceId = deviceStorage.getOrCreateDeviceId(),
                    deviceName = Build.MODEL,
                    appVersion = BuildConfig.VERSION_NAME,
                )
            ).toDomain()

            when (result.status) {
                ActivationStatus.ACTIVATED -> {
                    deviceStorage.setActivated(true)
                    result.apiKey?.let { tokenStorage.setApiKey(it) }
                }
                ActivationStatus.NOT_REQUIRED -> deviceStorage.setActivated(true)
                else -> Unit
            }
            result
        } catch (e: Exception) {
            Log.w(TAG, "Activation unavailable, continuing as anonymous-free: ${e.message}")
            deviceStorage.setActivated(true)
            DeviceActivation(status = ActivationStatus.NOT_REQUIRED)
        }
    }

    private companion object {
        const val TAG = "DeviceRepository"
    }
}
