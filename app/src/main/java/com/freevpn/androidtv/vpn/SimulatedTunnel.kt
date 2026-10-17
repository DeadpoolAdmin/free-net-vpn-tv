package com.freevpn.androidtv.vpn

import android.net.VpnService
import android.util.Log
import com.freevpn.androidtv.model.VpnConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.Closeable
import java.io.FileInputStream

/**
 * MVP default tunnel. It establishes a REAL tun interface via [VpnService.Builder] — so the system
 * VPN key indicator, the permission dialog, and the foreground notification all behave exactly like
 * production — but it does NOT encrypt or forward traffic: packets entering the tun are read and
 * discarded (black-holed). This exercises the entire connect/disconnect/state/notification flow with
 * no WireGuard server required.
 *
 * IMPORTANT: while this tunnel is "connected" the device has NO internet (traffic is dropped). It is
 * gated behind BuildConfig.USE_SIMULATED_TUNNEL and surfaced as a debug banner. Replace with
 * [GoBackendTunnel] for real tunneling — see TODO.md.
 */
class SimulatedTunnel(
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
) : WireGuardTunnelManager {

    override suspend fun up(
        config: VpnConfig,
        builder: VpnService.Builder,
        protect: (Int) -> Boolean, // unused: the simulated tunnel has no real socket to protect
    ): TunnelHandle {
        // A minimal, valid TUN configuration. Routing 0.0.0.0/0 captures all traffic (then drops it).
        builder
            .setSession("Free VPN (simulated)")
            .addAddress("10.0.0.2", 32)
            .addDnsServer("1.1.1.1")
            .addRoute("0.0.0.0", 0)
            .setMtu(1280)

        // Simulate a handshake so the UI shows CONNECTING for a beat.
        delay(HANDSHAKE_DELAY_MS)

        val fd = builder.establish()
            ?: throw IllegalStateException("establish() returned null (permission revoked?)")

        // Drain the interface so the OS doesn't block writers on a full tun buffer.
        val drainJob = scope.launch {
            try {
                FileInputStream(fd.fileDescriptor).use { input ->
                    val buffer = ByteArray(BUFFER_SIZE)
                    while (isActive) {
                        val read = input.read(buffer)
                        if (read < 0) break
                        // Black-hole: intentionally discard.
                    }
                }
            } catch (e: Exception) {
                Log.d(TAG, "drainer stopped: ${e.message}")
            }
        }

        Log.i(TAG, "Simulated tunnel established for ${config.serverId} (traffic black-holed)")
        return TunnelHandle(
            tunFd = fd,
            closeable = Closeable {
                drainJob.cancel()
                runCatching { fd.close() }
            },
        )
    }

    override suspend fun down(handle: TunnelHandle) {
        handle.close()
        Log.i(TAG, "Simulated tunnel torn down")
    }

    private companion object {
        const val TAG = "SimulatedTunnel"
        const val HANDSHAKE_DELAY_MS = 800L
        const val BUFFER_SIZE = 32_767
    }
}
