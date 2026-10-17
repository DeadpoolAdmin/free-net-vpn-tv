package com.freevpn.androidtv.model

/**
 * Result of POST /devices/activate. The app is anonymous-free by default; activation is only shown
 * when [status] is PENDING (a code must be confirmed on the web/mobile portal).
 */
data class DeviceActivation(
    val status: ActivationStatus,
    /** Human-readable code the user types on the portal (when status == PENDING). */
    val deviceCode: String? = null,
    /** Optional URL of the portal where the user enters the code. */
    val activationUrl: String? = null,
    /** Seconds until [deviceCode] expires, if provided. */
    val expiresInSeconds: Long? = null,
    /** Credential returned once the device is activated; used as the X-Api-Key header. */
    val apiKey: String? = null,
)

enum class ActivationStatus {
    /** Backend does not require activation — user may continue straight to servers. */
    NOT_REQUIRED,
    /** A code was issued; waiting for the user to confirm it on the portal. */
    PENDING,
    /** Device is activated. */
    ACTIVATED,
    /** The previously issued code/activation expired. */
    EXPIRED,
    UNKNOWN;

    companion object {
        fun fromWire(raw: String?): ActivationStatus = when (raw?.trim()?.uppercase()) {
            "NOT_REQUIRED", "NONE" -> NOT_REQUIRED
            "PENDING", "WAITING" -> PENDING
            "ACTIVATED", "OK", "ACTIVE" -> ACTIVATED
            "EXPIRED" -> EXPIRED
            else -> UNKNOWN
        }
    }
}
