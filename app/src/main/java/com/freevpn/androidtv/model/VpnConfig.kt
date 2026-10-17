package com.freevpn.androidtv.model

/**
 * VPN configuration for a chosen server. [configContent] is the raw WireGuard config and is
 * SENSITIVE — toString() is overridden so it can never leak into logs.
 */
data class VpnConfig(
    val serverId: String,
    val protocol: VpnProtocol,
    val configContent: String,
) {
    override fun toString(): String =
        "VpnConfig(serverId=$serverId, protocol=$protocol, configContent=<redacted>)"
}

enum class VpnProtocol {
    WIREGUARD,
    UNKNOWN;

    companion object {
        fun fromWire(raw: String?): VpnProtocol = when (raw?.trim()?.uppercase()) {
            "WIREGUARD", "WG" -> WIREGUARD
            else -> UNKNOWN
        }
    }
}
