package com.freevpn.androidtv.data

import android.util.Log
import com.freevpn.androidtv.model.FreeAccessStatus
import com.freevpn.androidtv.network.VpnApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Reports the FREE access / fair-usage status. Never drives a payment flow.
 *
 * On backend failure it fails OPEN (ALLOWED) so a transient backend hiccup does not lock a free
 * user out. Abuse protection still works whenever the backend is reachable and returns a real state.
 */
class AccessRepository(private val api: VpnApi) {

    suspend fun getAccess(): FreeAccessStatus = withContext(Dispatchers.IO) {
        try {
            api.getAccess().toDomain()
        } catch (e: Exception) {
            Log.w(TAG, "Access status unavailable, assuming ALLOWED: ${e.message}")
            LocalFallback.access()
        }
    }

    private companion object {
        const val TAG = "AccessRepository"
    }
}
