package com.freevpn.androidtv

import android.app.Application
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationManagerCompat

/**
 * Application entry point. Builds the [AppContainer], creates the (silent) VPN status notification
 * channel, and starts the [com.freevpn.androidtv.vpn.VpnManager] connectivity monitoring.
 */
class VpnApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        createNotificationChannel()
        container.vpnManager.start()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannelCompat.Builder(CHANNEL_ID, NotificationManagerCompat.IMPORTANCE_LOW)
            .setName(getString(R.string.notif_channel_name))
            .setDescription(getString(R.string.notif_channel_desc))
            .setShowBadge(false)
            .build()
        NotificationManagerCompat.from(this).createNotificationChannel(channel)
    }

    companion object {
        const val CHANNEL_ID = "vpn_status"
    }
}
