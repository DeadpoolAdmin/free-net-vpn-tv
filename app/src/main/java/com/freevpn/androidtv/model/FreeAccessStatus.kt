package com.freevpn.androidtv.model

/**
 * Result of GET /user/access. Controls fair-usage / abuse protection for the FREE service.
 * This NEVER drives any payment or upgrade flow — the app is free.
 */
data class FreeAccessStatus(
    val status: AccessStatus,
    val message: String? = null,
    /** When BLOCKED / MAINTENANCE, seconds to wait before retrying, if the backend provided it. */
    val retryAfterSeconds: Long? = null,
) {
    /** Whether a connection attempt is permitted right now. */
    val canConnect: Boolean get() = status == AccessStatus.ALLOWED || status == AccessStatus.LIMITED

    companion object {
        /** Safe default used by LocalFallback when the backend is unreachable. */
        val ALLOWED = FreeAccessStatus(AccessStatus.ALLOWED)
    }
}

enum class AccessStatus {
    ALLOWED,
    LIMITED,
    BLOCKED,
    SERVER_UNAVAILABLE,
    MAINTENANCE,
    /** Unparseable/unknown — treated as not connectable for safety. */
    UNKNOWN;

    companion object {
        fun fromWire(raw: String?): AccessStatus = when (raw?.trim()?.uppercase()) {
            "ALLOWED" -> ALLOWED
            "LIMITED" -> LIMITED
            "BLOCKED" -> BLOCKED
            "SERVER_UNAVAILABLE", "NO_SERVERS" -> SERVER_UNAVAILABLE
            "MAINTENANCE" -> MAINTENANCE
            else -> UNKNOWN
        }
    }
}
