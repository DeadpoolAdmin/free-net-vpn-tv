package com.freevpn.androidtv.vpn

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.util.Log
import androidx.core.content.ContextCompat
import com.freevpn.androidtv.data.AccessRepository
import com.freevpn.androidtv.data.VpnRepository
import com.freevpn.androidtv.model.FreeAccessStatus
import com.freevpn.androidtv.model.VpnConfig
import com.freevpn.androidtv.model.VpnServer
import com.freevpn.androidtv.storage.ConfigStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Mediator between the UI and [MyVpnService]. It is the ONLY writer of [VpnStateHolder], owns the
 * connection timer, gates connects on free-access status, and runs the bounded auto-reconnect.
 *
 * Flow:  UI → ViewModel → VpnManager.connect() → startForegroundService → MyVpnService
 *        MyVpnService → VpnManager.onService*() → VpnStateHolder → StateFlow → UI
 *
 * The service reads [pendingServer]/[pendingConfig] directly (same process) so the raw VPN config
 * never travels through an Intent.
 */
class VpnManager(
    private val appContext: Context,
    private val vpnRepository: VpnRepository,
    private val accessRepository: AccessRepository,
    private val configStorage: ConfigStorage,
    private val scope: CoroutineScope,
) {
    val state: StateFlow<VpnConnectionState> = VpnStateHolder.state

    private val _elapsedSeconds = MutableStateFlow(0L)
    val elapsedSeconds: StateFlow<Long> = _elapsedSeconds.asStateFlow()

    // Hand-off read by MyVpnService (same process). Config kept out of Intents for safety.
    @Volatile internal var pendingServer: VpnServer? = null
        private set
    @Volatile internal var pendingConfig: VpnConfig? = null
        private set

    /** The server the user wants connected; null after an explicit disconnect. Drives reconnect. */
    @Volatile private var desiredServer: VpnServer? = null
    @Volatile private var connectStartMs: Long = 0L

    /** True only while a tunnel is actually established (set by the service). */
    @Volatile internal var tunnelActive: Boolean = false
        private set

    private var timerJob: Job? = null
    private var reconnectJob: Job? = null
    private val reconnect = ReconnectController()

    private val autoReconnect = MutableStateFlow(true)
    private val showNotification = MutableStateFlow(true)

    private var started = false

    /** Called once from the Application. Caches settings and registers connectivity monitoring. */
    fun start() {
        if (started) return
        started = true
        scope.launch { configStorage.autoReconnect.collect { autoReconnect.value = it } }
        scope.launch { configStorage.showNotification.collect { showNotification.value = it } }
        registerNetworkCallback()
    }

    fun showNotificationEnabled(): Boolean = showNotification.value

    /** Free-access gate. UI calls this before requesting VPN permission. Never a payment flow. */
    suspend fun checkAccess(): FreeAccessStatus = accessRepository.getAccess()

    /**
     * Begin connecting to [server]. Must be called only after VPN permission is granted. Ignored if
     * a connect/disconnect is already in progress (prevents multiple simultaneous attempts).
     */
    fun connect(server: VpnServer) {
        when (state.value) {
            is VpnConnectionState.Connecting,
            is VpnConnectionState.Connected,
            is VpnConnectionState.Disconnecting -> {
                Log.w(TAG, "connect() ignored; state is ${state.value}")
                return
            }
            else -> Unit
        }
        desiredServer = server
        reconnect.reset()
        startConnect(server)
    }

    fun disconnect() {
        desiredServer = null
        reconnectJob?.cancel()
        reconnect.reset()
        if (state.value is VpnConnectionState.Disconnected) return
        appContext.startService(MyVpnService.disconnectIntent(appContext))
    }

    /** Called by the Activity when the user denies the system VPN permission dialog. */
    fun reportPermissionDenied() {
        VpnStateHolder.set(VpnConnectionState.Error(ErrorReason.PermissionDenied))
    }

    /** Reconcile state after TV sleep/wake: if we claim Connected but no tunnel is live, reset. */
    fun reconcile() {
        if (state.value is VpnConnectionState.Connected && !tunnelActive) {
            Log.w(TAG, "reconcile: Connected but tunnel is gone; resetting to Disconnected")
            stopTimer()
            VpnStateHolder.set(VpnConnectionState.Disconnected)
        }
    }

    private fun startConnect(server: VpnServer) {
        VpnStateHolder.set(VpnConnectionState.Connecting)
        scope.launch {
            val config = try {
                vpnRepository.connect(server)
            } catch (e: Exception) {
                Log.w(TAG, "connect/registration failed: ${e.message}")
                failOrReconnect(server, ErrorReason.ConfigFetchFailed)
                return@launch
            }
            pendingServer = server
            pendingConfig = config
            // Hand off to the foreground service which owns the VpnService.Builder.
            ContextCompat.startForegroundService(appContext, MyVpnService.connectIntent(appContext, server))
        }
    }

    /* ----------------------- Callbacks from MyVpnService (same process) ----------------------- */

    internal fun onServiceConnected(server: VpnServer) {
        connectStartMs = System.currentTimeMillis()
        tunnelActive = true
        reconnect.reset()
        VpnStateHolder.set(VpnConnectionState.Connected(server, connectStartMs))
        startTimer()
        scope.launch { vpnRepository.sendConnectLog(server.id, "WIREGUARD", connectStartMs, success = true) }
    }

    internal fun onServiceDisconnecting() {
        stopTimer()
        VpnStateHolder.set(VpnConnectionState.Disconnecting)
    }

    internal fun onServiceDisconnected(server: VpnServer?) {
        val endMs = System.currentTimeMillis()
        val startMs = connectStartMs
        tunnelActive = false
        stopTimer()
        pendingServer = null
        pendingConfig = null
        VpnStateHolder.set(VpnConnectionState.Disconnected)
        if (server != null) {
            // Tell the backend to remove this device's peer from the server.
            scope.launch { vpnRepository.disconnect(server) }
            if (startMs > 0L) {
                scope.launch { vpnRepository.sendDisconnectLog(server.id, startMs, endMs) }
            }
        }
        connectStartMs = 0L
    }

    internal fun onServiceError(reason: ErrorReason, server: VpnServer?) {
        tunnelActive = false
        stopTimer()
        scope.launch {
            vpnRepository.sendConnectLog(
                serverId = server?.id ?: "unknown",
                protocol = "WIREGUARD",
                startedAtMs = System.currentTimeMillis(),
                success = false,
            )
        }
        if (server != null && reason != ErrorReason.PermissionDenied && reason != ErrorReason.AccessDenied) {
            failOrReconnect(server, reason)
        } else {
            VpnStateHolder.set(VpnConnectionState.Error(reason))
        }
    }

    /* ----------------------------- Bounded reconnect ----------------------------- */

    private fun failOrReconnect(server: VpnServer, reason: ErrorReason) {
        if (autoReconnect.value) {
            val delayMs = reconnect.nextDelayMs()
            if (delayMs != null) {
                Log.i(TAG, "scheduling reconnect in ${delayMs}ms (reason=$reason)")
                VpnStateHolder.set(VpnConnectionState.Connecting)
                reconnectJob?.cancel()
                reconnectJob = scope.launch {
                    delay(delayMs)
                    if (isActive && desiredServer?.id == server.id && hasConnectivity()) {
                        startConnect(server)
                    } else {
                        VpnStateHolder.set(VpnConnectionState.Error(reason))
                    }
                }
                return
            }
            Log.w(TAG, "reconnect budget exhausted; giving up")
        }
        VpnStateHolder.set(VpnConnectionState.Error(reason))
    }

    private fun registerNetworkCallback() {
        val cm = appContext.getSystemService(ConnectivityManager::class.java) ?: return
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        runCatching {
            cm.registerNetworkCallback(request, object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    // If we are mid-error waiting on connectivity, nudge a reconnect.
                    val server = desiredServer ?: return
                    if (autoReconnect.value && state.value is VpnConnectionState.Error && !tunnelActive) {
                        reconnect.reset()
                        startConnect(server)
                    }
                }
            })
        }
    }

    private fun hasConnectivity(): Boolean {
        val cm = appContext.getSystemService(ConnectivityManager::class.java) ?: return true
        val net = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(net) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    /* ----------------------------- Connection timer ----------------------------- */

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = scope.launch {
            while (isActive) {
                val base = connectStartMs
                _elapsedSeconds.value =
                    if (base > 0L) ((System.currentTimeMillis() - base) / 1000).coerceAtLeast(0L) else 0L
                delay(1_000L)
            }
        }
    }

    private fun stopTimer() {
        timerJob?.cancel()
        timerJob = null
        _elapsedSeconds.value = 0L
    }

    private companion object {
        const val TAG = "VpnManager"
    }
}
