package com.freevpn.androidtv.data

import android.util.Log
import com.freevpn.androidtv.BuildConfig
import com.freevpn.androidtv.model.VpnConfig
import com.freevpn.androidtv.model.VpnProtocol
import com.freevpn.androidtv.model.VpnServer
import com.freevpn.androidtv.network.ConnectLogRequest
import com.freevpn.androidtv.network.DisconnectLogRequest
import com.freevpn.androidtv.network.VpnApi
import com.freevpn.androidtv.network.VpnConnectRequest
import com.freevpn.androidtv.network.VpnConnectResponse
import com.freevpn.androidtv.network.VpnDisconnectRequest
import com.freevpn.androidtv.storage.DeviceStorage
import com.freevpn.androidtv.vpn.WireGuardKeyStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Talks to the backend's VPN endpoints and sends privacy-safe operational logs.
 *
 * Connect model (standard WireGuard): the device generates its own keypair via [WireGuardKeyStore]
 * and sends only the PUBLIC key. The backend registers that peer on the server and returns the peer
 * details (assigned IP, server public key, endpoint, DNS). We assemble the wg-quick config locally
 * from those plus our private key — which never leaves the device.
 */
class VpnRepository(
    private val api: VpnApi,
    private val deviceStorage: DeviceStorage,
    private val keyStore: WireGuardKeyStore,
) {

    /** Register this device on [server] and return the tunnel config. @throws on backend failure. */
    suspend fun connect(server: VpnServer): VpnConfig = withContext(Dispatchers.IO) {
        val keyPair = keyStore.getOrCreate()
        val resp = api.connect(
            VpnConnectRequest(
                clientPublicKey = keyPair.publicKey.toBase64(),
                serverId = server.id.toInt(),
                deviceId = deviceStorage.getOrCreateDeviceId(),
            )
        )
        val config = buildWgQuickConfig(privateKey = keyPair.privateKey.toBase64(), resp = resp)
        VpnConfig(server.id, VpnProtocol.WIREGUARD, config)
    }

    /** Assemble a wg-quick config from our private key + the backend's peer details. */
    private fun buildWgQuickConfig(privateKey: String, resp: VpnConnectResponse): String {
        require(resp.assignedIp.isNotBlank()) { "Backend returned no assigned_ip" }
        require(resp.serverPublicKey.isNotBlank()) { "Backend returned no server_public_key" }
        require(resp.endpoint.isNotBlank()) { "Backend returned no endpoint" }
        val address = if (resp.assignedIp.contains('/')) resp.assignedIp else "${resp.assignedIp}/32"
        return buildString {
            appendLine("[Interface]")
            appendLine("PrivateKey = $privateKey")
            appendLine("Address = $address")
            if (resp.dnsServers.isNotBlank()) appendLine("DNS = ${resp.dnsServers}")
            appendLine()
            appendLine("[Peer]")
            appendLine("PublicKey = ${resp.serverPublicKey}")
            appendLine("Endpoint = ${resp.endpoint}")
            appendLine("AllowedIPs = 0.0.0.0/0, ::/0")
            appendLine("PersistentKeepalive = 25")
        }
    }

    /** Remove this device's peer from [server]. Best-effort; never throws. */
    suspend fun disconnect(server: VpnServer) {
        runCatching {
            withContext(Dispatchers.IO) {
                api.disconnect(
                    VpnDisconnectRequest(
                        serverId = server.id.toInt(),
                        deviceId = deviceStorage.getOrCreateDeviceId(),
                    )
                )
            }
        }.onFailure { Log.w(TAG, "disconnect call failed: ${it.message}") }
    }

    /** Fire-and-forget; telemetry must never affect the connection or crash the app. */
    suspend fun sendConnectLog(serverId: String, protocol: String, startedAtMs: Long, success: Boolean) {
        runCatching {
            withContext(Dispatchers.IO) {
                api.connectLog(
                    ConnectLogRequest(
                        deviceId = deviceStorage.getOrCreateDeviceId(),
                        serverId = serverId,
                        protocol = protocol,
                        startedAtEpochMs = startedAtMs,
                        success = success,
                        appVersion = BuildConfig.VERSION_NAME,
                    )
                )
            }
        }.onFailure { Log.d(TAG, "connect-log skipped: ${it.message}") }
    }

    suspend fun sendDisconnectLog(serverId: String, startedAtMs: Long, endedAtMs: Long) {
        runCatching {
            withContext(Dispatchers.IO) {
                api.disconnectLog(
                    DisconnectLogRequest(
                        deviceId = deviceStorage.getOrCreateDeviceId(),
                        serverId = serverId,
                        startedAtEpochMs = startedAtMs,
                        endedAtEpochMs = endedAtMs,
                        durationSeconds = ((endedAtMs - startedAtMs) / 1000).coerceAtLeast(0),
                        appVersion = BuildConfig.VERSION_NAME,
                    )
                )
            }
        }.onFailure { Log.d(TAG, "disconnect-log skipped: ${it.message}") }
    }

    private companion object {
        const val TAG = "VpnRepository"
    }
}
