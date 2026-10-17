package com.freevpn.androidtv.vpn

import android.Manifest
import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.VpnService
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.freevpn.androidtv.MainActivity
import com.freevpn.androidtv.R
import com.freevpn.androidtv.VpnApp
import com.freevpn.androidtv.model.VpnServer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Real Android [VpnService]. Owns the foreground notification and the tun interface lifecycle, and
 * delegates the actual tunnel to a [WireGuardTunnelManager] (SimulatedTunnel in the MVP). It reports
 * every transition back to [VpnManager], which is the single writer of the shared state.
 *
 * Only a VpnService can create a [VpnService.Builder], so the builder is created here and passed to
 * the tunnel manager.
 */
class MyVpnService : VpnService() {

    private val container by lazy { (application as VpnApp).container }
    private val tunnelManager get() = container.tunnelManager
    private val vpnManager get() = container.vpnManager

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var handle: TunnelHandle? = null
    private var activeServer: VpnServer? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_CONNECT -> handleConnect()
            ACTION_DISCONNECT -> handleDisconnect()
            else -> handleDisconnect()
        }
        // We manage reconnect ourselves; do NOT let the system auto-restart (avoids loops).
        return START_NOT_STICKY
    }

    private fun handleConnect() {
        val server = vpnManager.pendingServer
        val config = vpnManager.pendingConfig

        // Must enter the foreground within ~5s of startForegroundService — do it first.
        startForeground(NOTIF_ID, buildConnectingNotification(server))

        if (server == null || config == null) {
            Log.e(TAG, "handleConnect: missing pending server/config")
            vpnManager.onServiceError(ErrorReason.Unknown, server)
            stopVpn()
            return
        }

        activeServer = server
        scope.launch {
            try {
                val builder = this@MyVpnService.Builder().apply {
                    setSession(getString(R.string.app_name))
                    // Keep the app's own traffic off the tunnel so operational logs still reach the
                    // backend and we avoid routing loops.
                    runCatching { addDisallowedApplication(packageName) }
                        .onFailure { if (it !is PackageManager.NameNotFoundException) throw it }
                }
                val newHandle = tunnelManager.up(config, builder, ::protect)
                handle = newHandle
                NotificationManagerCompat.from(this@MyVpnService)
                    .notifyIfAllowed(NOTIF_ID, buildConnectedNotification(server))
                vpnManager.onServiceConnected(server)
            } catch (e: Exception) {
                Log.e(TAG, "tunnel up failed: ${e.message}")
                vpnManager.onServiceError(ErrorReason.TunnelSetupFailed, server)
                stopVpn()
            }
        }
    }

    private fun handleDisconnect() {
        vpnManager.onServiceDisconnecting()
        val server = activeServer
        scope.launch {
            handle?.let { runCatching { tunnelManager.down(it) } }
            handle = null
            vpnManager.onServiceDisconnected(server)
            stopForegroundCompat()
            stopSelf()
        }
    }

    /** System revoked our VPN (another VPN started, or the user revoked permission). Tear down. */
    override fun onRevoke() {
        Log.i(TAG, "onRevoke: VPN permission revoked by system")
        handleDisconnect()
    }

    override fun onDestroy() {
        runCatching { handle?.close() }
        handle = null
        scope.cancel()
        super.onDestroy()
    }

    private fun stopVpn() {
        scope.launch {
            handle?.let { runCatching { tunnelManager.down(it) } }
            handle = null
            stopForegroundCompat()
            stopSelf()
        }
    }

    private fun stopForegroundCompat() {
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
    }

    /* ----------------------------- Notifications ----------------------------- */

    private fun buildConnectingNotification(server: VpnServer?): Notification =
        baseNotification()
            .setContentTitle(getString(R.string.notif_connecting_title))
            .setContentText(server?.locationLabel ?: getString(R.string.app_name))
            .build()

    private fun buildConnectedNotification(server: VpnServer): Notification =
        baseNotification()
            .setContentTitle(getString(R.string.notif_connected_title))
            .setContentText(getString(R.string.notif_connected_text, server.country, server.city))
            .addAction(
                0,
                getString(R.string.notif_action_disconnect),
                servicePendingIntent(disconnectIntent(this)),
            )
            .build()

    private fun baseNotification(): NotificationCompat.Builder =
        NotificationCompat.Builder(this, VpnApp.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_vpn_status)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setShowWhen(false)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setContentIntent(activityPendingIntent())

    private fun activityPendingIntent(): PendingIntent {
        val intent = Intent(this, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        return PendingIntent.getActivity(this, 0, intent, pendingIntentFlags())
    }

    private fun servicePendingIntent(intent: Intent): PendingIntent =
        PendingIntent.getService(this, 1, intent, pendingIntentFlags())

    private fun pendingIntentFlags(): Int =
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE

    /**
     * Posting a notification requires the POST_NOTIFICATIONS runtime permission on Android 13+ (it is
     * granted implicitly on older versions). We check explicitly and skip the update if it isn't
     * granted — the ongoing foreground-service notification from [startForeground] still shows either
     * way, so the user is never left without status. The runCatching is a belt-and-suspenders guard
     * against OEM quirks that can still throw.
     */
    private fun NotificationManagerCompat.notifyIfAllowed(id: Int, notification: Notification) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this@MyVpnService, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            Log.d(TAG, "notify skipped: POST_NOTIFICATIONS not granted")
            return
        }
        runCatching { notify(id, notification) }
            .onFailure { Log.d(TAG, "notify skipped: ${it.message}") }
    }

    companion object {
        const val ACTION_CONNECT = "com.freevpn.androidtv.action.CONNECT"
        const val ACTION_DISCONNECT = "com.freevpn.androidtv.action.DISCONNECT"
        const val EXTRA_SERVER_ID = "server_id"
        private const val NOTIF_ID = 1001
        private const val TAG = "MyVpnService"

        fun connectIntent(context: Context, server: VpnServer): Intent =
            Intent(context, MyVpnService::class.java)
                .setAction(ACTION_CONNECT)
                .putExtra(EXTRA_SERVER_ID, server.id)

        fun disconnectIntent(context: Context): Intent =
            Intent(context, MyVpnService::class.java).setAction(ACTION_DISCONNECT)
    }
}
