package com.freevpn.androidtv.model

/**
 * A selectable free VPN server/location. Domain model (not a wire DTO) — the network layer maps
 * the backend JSON into this so unknown enum values never crash the app.
 */
data class VpnServer(
    val id: String,
    val country: String,
    val city: String,
    val isRecommended: Boolean = false,
    val status: ServerStatus = ServerStatus.AVAILABLE,
    /** 0..100, or null if the backend didn't report it. */
    val loadPercent: Int? = null,
    /** Round-trip time in milliseconds, or null if unknown. */
    val pingMs: Int? = null,
) {
    val locationLabel: String get() = "$country — $city"
}

enum class ServerStatus {
    AVAILABLE,
    BUSY,
    UNAVAILABLE;

    /** A server the user is allowed to attempt a connection to. */
    val isConnectable: Boolean get() = this == AVAILABLE || this == BUSY

    companion object {
        fun fromWire(raw: String?): ServerStatus = when (raw?.trim()?.uppercase()) {
            "AVAILABLE" -> AVAILABLE
            "BUSY", "LIMITED" -> BUSY
            else -> UNAVAILABLE
        }
    }
}
