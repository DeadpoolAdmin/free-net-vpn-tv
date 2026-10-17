package com.freevpn.androidtv.data

import android.util.Log
import com.freevpn.androidtv.network.LoginRequest
import com.freevpn.androidtv.network.VpnApi
import com.freevpn.androidtv.storage.DeviceStorage
import com.freevpn.androidtv.storage.TokenStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

/** A logged-in backend session: the JWT bearer token plus the user it belongs to. */
data class AuthSession(val token: String, val userId: String)

/**
 * Owns the backend session (JWT). Logs in with this device's id when there is no cached token and
 * hands the bearer token to [com.freevpn.androidtv.network.AuthInterceptor]. The login response's
 * userId is cached too, since registering the device needs it.
 *
 * [authApi] MUST be a token-free client (no AuthInterceptor) to avoid recursion: the interceptor
 * calls [bearerBlocking] → which logs in on this client → which must not need a token.
 */
class AuthRepository(
    private val authApi: VpnApi,
    private val tokenStorage: TokenStorage,
    private val deviceStorage: DeviceStorage,
) {

    /**
     * Returns the current session (cached token + userId), logging in if needed. Returns null when
     * login fails so callers can surface an error instead of proceeding tokenless.
     */
    suspend fun authenticate(): AuthSession? {
        cachedSession()?.let { return it }
        return runCatching { login() }
            .onFailure { Log.w(TAG, "auth failed: ${it.message}") }
            .getOrNull()
    }

    /** Cached token, or a blocking login. Safe to call from an OkHttp interceptor (runs off-main). */
    @Synchronized
    fun bearerBlocking(): String? =
        cachedSession()?.token ?: runCatching { runBlocking { login() } }
            .onFailure { Log.w(TAG, "login failed: ${it.message}") }
            .getOrNull()
            ?.token

    /** Drop the cached token (e.g. after a 401) so the next call re-logs in. */
    fun clearToken() = tokenStorage.setToken(null)

    private fun cachedSession(): AuthSession? {
        val token = tokenStorage.getToken()?.takeIf { it.isNotBlank() } ?: return null
        val userId = tokenStorage.getUserId()?.takeIf { it.isNotBlank() } ?: return null
        return AuthSession(token, userId)
    }

    /** Logs in with this device's id, persists token + userId, and returns the session. */
    private suspend fun login(): AuthSession = withContext(Dispatchers.IO) {
        val resp = authApi.login(LoginRequest(deviceStorage.getOrCreateDeviceId()))
        val token = resp.token.takeIf { it.isNotBlank() } ?: error("backend returned a blank token")
        tokenStorage.setToken(token)
        tokenStorage.setUserId(resp.userId)
        AuthSession(token, resp.userId)
    }

    private companion object {
        const val TAG = "AuthRepository"
    }
}
