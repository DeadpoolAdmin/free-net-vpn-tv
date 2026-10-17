package com.freevpn.androidtv.network

import com.freevpn.androidtv.model.AccessStatus
import com.freevpn.androidtv.model.ActivationStatus
import com.freevpn.androidtv.model.DeviceAccount
import com.freevpn.androidtv.model.DeviceActivation
import com.freevpn.androidtv.model.FreeAccessStatus
import com.freevpn.androidtv.model.ServerStatus
import com.freevpn.androidtv.model.VpnServer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/* ----------------------------- Wire DTOs (FreeNet backend JSON) ----------------------------- */

/** Login: POST /login with this device's id -> { token, userId, deviceId }. */
@Serializable
data class LoginRequest(val deviceId: String)

/** Register this device for the logged-in user: POST /api/device (bearer-authed). */
@Serializable
data class CreateDeviceRequest(
    @SerialName("device_id") val deviceId: String,
    @SerialName("user_id") val userId: String,
)

/** Device/subscription record returned by POST /api/device. */
@Serializable
data class DeviceResponseDto(
    val id: Int = 0,
    @SerialName("device_id") val deviceId: String = "",
    @SerialName("user_id") val userId: String = "",
    @SerialName("expire_date") val expireDate: String? = null,
    @SerialName("package") val packageName: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
) {
    fun toDomain(): DeviceAccount = DeviceAccount(
        id = id,
        deviceId = deviceId,
        userId = userId,
        expireDate = expireDate,
        packageName = packageName,
        createdAt = createdAt,
    )
}

@Serializable
data class AuthResponse(
    val token: String = "",
    val userId: String = "",
    val deviceId: String = "",
)

/** A VPN server/location from GET /api/vpn. */
@Serializable
data class ServerDto(
    val id: Int,
    val name: String = "",
    val country: String = "",
    val city: String? = null,
    val flag: String = "",
    @SerialName("is_free") val isFree: Boolean = false,
    val load: Int = 0,
    @SerialName("is_available") val isAvailable: Boolean = true,
) {
    fun toDomain(): VpnServer = VpnServer(
        id = id.toString(),
        country = country,
        city = city ?: name,
        isRecommended = isFree,
        status = when {
            !isAvailable -> ServerStatus.UNAVAILABLE
            load >= 80 -> ServerStatus.BUSY
            else -> ServerStatus.AVAILABLE
        },
        loadPercent = load,
        pingMs = null,
    )
}

/**
 * Connect: POST /api/vpn/connect — standard WireGuard model. The device generates its own keypair,
 * sends only the PUBLIC key (the private key never leaves the device), and the backend registers that
 * peer on the server and returns the peer's connection details. The device then assembles the
 * wg-quick config locally from its private key + these fields.
 */
@Serializable
data class VpnConnectRequest(
    @SerialName("client_public_key") val clientPublicKey: String,
    @SerialName("server_id") val serverId: Int,
    @SerialName("device_id") val deviceId: String,
)

@Serializable
data class VpnConnectResponse(
    /** Tunnel IP assigned to this device, CIDR form e.g. "10.10.0.14/32". */
    @SerialName("assigned_ip") val assignedIp: String = "",
    /** The server peer's public key. */
    @SerialName("server_public_key") val serverPublicKey: String = "",
    /** The server endpoint as host:port, e.g. "167.86.123.162:51820". */
    val endpoint: String = "",
    /** Comma-separated DNS servers, e.g. "1.1.1.1, 8.8.8.8". */
    @SerialName("dns_servers") val dnsServers: String = "",
)

/** Disconnect: POST /api/vpn/disconnect — removes this device's peer from the server. */
@Serializable
data class VpnDisconnectRequest(
    @SerialName("server_id") val serverId: Int,
    @SerialName("device_id") val deviceId: String,
)

@Serializable
data class AccessDto(
    val status: String? = null,
    val message: String? = null,
    val retryAfterSeconds: Long? = null,
) {
    fun toDomain(): FreeAccessStatus = FreeAccessStatus(
        status = AccessStatus.fromWire(status),
        message = message,
        retryAfterSeconds = retryAfterSeconds,
    )
}

@Serializable
data class DeviceActivateRequest(
    val deviceId: String,
    val deviceName: String? = null,
    val appVersion: String? = null,
)

@Serializable
data class DeviceActivationDto(
    val status: String? = null,
    val deviceCode: String? = null,
    val activationUrl: String? = null,
    val expiresInSeconds: Long? = null,
    val apiKey: String? = null,
) {
    fun toDomain(): DeviceActivation = DeviceActivation(
        status = ActivationStatus.fromWire(status),
        deviceCode = deviceCode,
        activationUrl = activationUrl,
        expiresInSeconds = expiresInSeconds,
        apiKey = apiKey,
    )
}

/* ----------------------------- Operational logs (privacy-safe) -----------------------------
 * NOTE: these carry only operational metadata. Never include browsing/DNS/traffic content. */

@Serializable
data class ConnectLogRequest(
    val deviceId: String,
    val serverId: String,
    val protocol: String,
    val startedAtEpochMs: Long,
    val success: Boolean,
    val appVersion: String,
)

@Serializable
data class DisconnectLogRequest(
    val deviceId: String,
    val serverId: String,
    val startedAtEpochMs: Long,
    val endedAtEpochMs: Long,
    val durationSeconds: Long,
    val appVersion: String,
)
