package com.freevpn.androidtv.data

import android.util.Log
import com.freevpn.androidtv.model.VpnServer
import com.freevpn.androidtv.network.VpnApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Loads the free VPN server list. Acts as the single source of truth: the first successful fetch is
 * cached in memory (this repository is an app-scoped singleton), so re-opening the server screen is
 * instant instead of re-fetching every time. On network failure or timeout it degrades to the last
 * cached list, or to [LocalFallback], so the screen always has something to show.
 */
class ServerRepository(private val api: VpnApi) {

    @Volatile
    private var cached: List<VpnServer>? = null

    /**
     * @param forceRefresh bypass the cache and hit the network (e.g. the user tapping Refresh).
     *
     * The network call is bounded by [FETCH_TIMEOUT_MS] so a slow or unreachable backend degrades to a
     * fallback quickly instead of hanging the UI. Only genuine results are cached — a failed/timed-out
     * fetch never poisons the cache, so the next visit transparently retries the network.
     */
    suspend fun getServers(forceRefresh: Boolean = false): List<VpnServer> {
        if (!forceRefresh) cached?.let { return it } // instant, no dispatcher hop, when already loaded

        return withContext(Dispatchers.IO) {
            val fresh = withTimeoutOrNull(FETCH_TIMEOUT_MS) {
                runCatching { api.getServers().map { it.toDomain() } }
                    .onFailure { Log.w(TAG, "Server list fetch failed: ${it.message}") }
                    .getOrNull()
            }
            if (fresh != null) {
                cached = fresh
                fresh
            } else {
                Log.w(TAG, "Server list unavailable, using cached/local fallback")
                cached ?: LocalFallback.servers()
            }
        }
    }

    private companion object {
        const val TAG = "ServerRepository"
        const val FETCH_TIMEOUT_MS = 4000L
    }
}
