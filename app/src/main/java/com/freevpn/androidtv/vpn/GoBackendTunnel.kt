package com.freevpn.androidtv.vpn

import android.content.Context
import android.net.VpnService
import android.util.Log
import com.freevpn.androidtv.model.VpnConfig
import com.wireguard.android.backend.GoBackend
import com.wireguard.config.Config
import java.io.BufferedReader
import java.io.Closeable
import java.io.StringReader
import java.net.Inet6Address

/**
 * Real WireGuard tunnel backed by the bundled wireguard-go userspace (com.wireguard.android:tunnel).
 *
 * We keep our own [MyVpnService] as the single VpnService (it owns the foreground notification and
 * state reporting), so we DON'T use GoBackend's own service. Instead we:
 *   1. parse the client config and configure + establish the tun via the passed-in [builder];
 *   2. hand the tun fd to wireguard-go (`wgTurnOn`) with the userspace config string;
 *   3. protect wireguard-go's underlying UDP socket so its packets don't loop back into the tun.
 *
 * Steps 2–3 use GoBackend's private native methods (the same ones GoBackend itself calls). A
 * [GoBackend] instance is constructed once purely to load the `libwg-go` native library.
 *
 * NOTE: this path requires a reachable WireGuard server and has to be exercised on a real device —
 * it cannot be unit-tested on the JVM (no native lib, no tun).
 */
class GoBackendTunnel(private val appContext: Context) : WireGuardTunnelManager {

    /** Constructed lazily, only to trigger loading of the wireguard-go native library. */
    private val nativeLoader by lazy { GoBackend(appContext) }

    override suspend fun up(
        config: VpnConfig,
        builder: VpnService.Builder,
        protect: (Int) -> Boolean,
    ): TunnelHandle {
        // Ensure libwg-go is loaded before any native call.
        nativeLoader

        val wg = Config.parse(BufferedReader(StringReader(config.configContent)))

        // ---- configure the tun from the [Interface] + [Peer] sections ----
        val iface = wg.getInterface()
        iface.addresses.forEach { builder.addAddress(it.address, it.mask) }
        iface.dnsServers.forEach { builder.addDnsServer(it) }
        builder.setMtu(iface.mtu.orElse(DEFAULT_MTU))

        // Only route an address family the interface can actually carry. Configs commonly list
        // AllowedIPs = 0.0.0.0/0, ::/0 — but if the interface has no IPv6 address, installing the ::/0
        // route black-holes all IPv6 traffic (and IPv6-preferring apps like YouTube then hang and
        // report "no internet"). In that case we skip IPv6 routes and let IPv6 use the normal network.
        val hasIpv6Address = iface.addresses.any { it.address is Inet6Address }
        wg.peers.forEach { peer ->
            peer.allowedIps.forEach { net ->
                if (net.address !is Inet6Address || hasIpv6Address) {
                    builder.addRoute(net.address, net.mask)
                } else {
                    Log.i(TAG, "Skipping IPv6 route ${net.address.hostAddress}/${net.mask} (no IPv6 address on tun)")
                }
            }
        }

        val tun = builder.establish()
            ?: throw IllegalStateException("establish() returned null (VPN permission revoked?)")

        // ---- hand the tun to wireguard-go ----
        // toWgUserspaceString() is exactly the cross-platform userspace config (hex keys, endpoint,
        // allowed_ip, keepalive, …) that wgTurnOn expects.
        val handle = try {
            wgTurnOn(INTERFACE_NAME, tun.detachFd(), wg.toWgUserspaceString())
        } catch (e: Exception) {
            runCatching { tun.close() }
            throw IllegalStateException("wgTurnOn failed: ${e.message}", e)
        }
        if (handle < 0) {
            throw IllegalStateException("wgTurnOn returned an error handle ($handle)")
        }

        // ---- protect wireguard-go's UDP socket(s) so they egress the real network ----
        runCatching { wgGetSocketV4(handle) }.getOrNull()?.let { if (it >= 0) protect(it) }
        runCatching { wgGetSocketV6(handle) }.getOrNull()?.let { if (it >= 0) protect(it) }

        Log.i(TAG, "WireGuard tunnel up for ${config.serverId} (handle=$handle)")
        return TunnelHandle(
            tunFd = null, // ownership of the fd was transferred to wireguard-go via detachFd()
            closeable = Closeable {
                runCatching { wgTurnOff(handle) }
                    .onFailure { Log.w(TAG, "wgTurnOff failed: ${it.message}") }
            },
        )
    }

    override suspend fun down(handle: TunnelHandle) {
        handle.close()
        Log.i(TAG, "WireGuard tunnel torn down")
    }

    /* ---- reflection into GoBackend's private static native methods ---- */

    private fun wgTurnOn(ifName: String, tunFd: Int, settings: String): Int =
        M_TURN_ON.invoke(null, ifName, tunFd, settings) as Int

    private fun wgTurnOff(handle: Int) {
        M_TURN_OFF.invoke(null, handle)
    }

    private fun wgGetSocketV4(handle: Int): Int = M_SOCKET_V4.invoke(null, handle) as Int
    private fun wgGetSocketV6(handle: Int): Int = M_SOCKET_V6.invoke(null, handle) as Int

    private companion object {
        const val TAG = "GoBackendTunnel"
        const val INTERFACE_NAME = "wg0"
        const val DEFAULT_MTU = 1280

        private val goBackend: Class<*> = Class.forName("com.wireguard.android.backend.GoBackend")

        val M_TURN_ON = goBackend.getDeclaredMethod(
            "wgTurnOn", String::class.java, Int::class.javaPrimitiveType, String::class.java,
        ).apply { isAccessible = true }

        val M_TURN_OFF = goBackend.getDeclaredMethod(
            "wgTurnOff", Int::class.javaPrimitiveType,
        ).apply { isAccessible = true }

        val M_SOCKET_V4 = goBackend.getDeclaredMethod(
            "wgGetSocketV4", Int::class.javaPrimitiveType,
        ).apply { isAccessible = true }

        val M_SOCKET_V6 = goBackend.getDeclaredMethod(
            "wgGetSocketV6", Int::class.javaPrimitiveType,
        ).apply { isAccessible = true }
    }
}
