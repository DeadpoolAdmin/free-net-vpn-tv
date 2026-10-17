package com.freevpn.androidtv.vpn

import android.net.VpnService
import android.os.ParcelFileDescriptor
import com.freevpn.androidtv.model.VpnConfig
import java.io.Closeable

/**
 * Abstraction over the actual tunnel implementation. The seam lets the MVP run on [SimulatedTunnel]
 * while keeping a one-line path to the production [GoBackendTunnel] (official wireguard-android).
 *
 * The caller (the VpnService) creates the [VpnService.Builder] — only a VpnService can — and passes
 * it in; the implementation adds addresses/routes/DNS and calls `establish()`.
 */
interface WireGuardTunnelManager {

    /**
     * Bring the tunnel up. Implementations configure [builder], call `builder.establish()`, and
     * return a [TunnelHandle] that owns whatever must be released on [down].
     *
     * @param protect excludes a raw socket fd from the VPN routing (i.e. `VpnService.protect`). A
     *   real tunnel MUST protect its own underlying UDP socket, otherwise its encrypted packets would
     *   be routed back into the tun and the tunnel would deadlock. Returns true on success.
     * @throws Exception if the tunnel cannot be established (the service maps this to ERROR).
     */
    suspend fun up(
        config: VpnConfig,
        builder: VpnService.Builder,
        protect: (Int) -> Boolean,
    ): TunnelHandle

    /** Tear the tunnel down and release all resources held by [handle]. */
    suspend fun down(handle: TunnelHandle)
}

/**
 * Opaque handle to a live tunnel. [tunFd] is the established interface fd (null only in degraded
 * test scenarios). [closeable] releases the backend-specific resources (drainer thread, native
 * tunnel, the fd) exactly once.
 */
class TunnelHandle(
    val tunFd: ParcelFileDescriptor?,
    private val closeable: Closeable,
) : Closeable {
    override fun close() = closeable.close()
}
