package com.freevpn.androidtv

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.VpnService
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.tv.material3.Surface
import androidx.tv.material3.SurfaceDefaults
import androidx.tv.material3.MaterialTheme
import androidx.navigation.compose.rememberNavController
import com.freevpn.androidtv.model.VpnServer
import com.freevpn.androidtv.navigation.AppNavGraph
import com.freevpn.androidtv.ui.theme.DesignScale
import com.freevpn.androidtv.ui.theme.FreeVpnTheme

/**
 * Single-Activity host. Owns the system VPN consent flow ([VpnService.prepare]) because only an
 * Activity can display the system dialog. The result is routed back through [VpnManager] (the shared
 * state holder), so screens update regardless of which composable triggered the connect.
 */
class MainActivity : ComponentActivity() {

    private val container get() = (application as VpnApp).container

    /** The server awaiting the permission result. */
    private var pendingServer: VpnServer? = null

    private val vpnPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val server = pendingServer
            pendingServer = null
            if (result.resultCode == RESULT_OK && server != null) {
                container.vpnManager.connect(server)
            } else {
                container.vpnManager.reportPermissionDenied()
            }
        }

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* non-fatal */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        maybeRequestNotificationPermission()

        setContent {
            FreeVpnTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    colors = SurfaceDefaults.colors(
                        containerColor = MaterialTheme.colorScheme.background,
                        contentColor = MaterialTheme.colorScheme.onBackground,
                    ),
                ) {
                    DesignScale {
                        val navController = rememberNavController()
                        AppNavGraph(
                            navController = navController,
                            onRequestVpnPermission = ::requestVpnPermission,
                        )
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Recover after TV sleep/wake or process changes.
        container.vpnManager.reconcile()
    }

    /** Triggers the system VPN consent dialog, or connects directly if already granted. */
    private fun requestVpnPermission(server: VpnServer) {
        pendingServer = server
        val prepareIntent: Intent? = VpnService.prepare(this)
        if (prepareIntent == null) {
            pendingServer = null
            container.vpnManager.connect(server)
        } else {
            vpnPermissionLauncher.launch(prepareIntent)
        }
    }

    private fun maybeRequestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                this, Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}
