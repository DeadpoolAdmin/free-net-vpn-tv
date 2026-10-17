package com.freevpn.androidtv.ui.common

import androidx.annotation.StringRes
import com.freevpn.androidtv.R
import com.freevpn.androidtv.model.AccessStatus
import com.freevpn.androidtv.vpn.ErrorReason

/** Maps a VPN [ErrorReason] to a user-friendly, free-positioned message (never a payment prompt). */
@StringRes
fun ErrorReason.messageRes(): Int = when (this) {
    ErrorReason.PermissionDenied -> R.string.error_permission_denied
    ErrorReason.ConfigFetchFailed -> R.string.error_invalid_config
    ErrorReason.TunnelSetupFailed -> R.string.error_tunnel_failed
    ErrorReason.NetworkLost -> R.string.error_no_internet
    ErrorReason.AccessDenied -> R.string.access_blocked
    ErrorReason.Unknown -> R.string.error_unknown
}

/** Maps a fair-usage [AccessStatus] to a message, or null when no message is needed (ALLOWED). */
@StringRes
fun AccessStatus.messageResOrNull(): Int? = when (this) {
    AccessStatus.LIMITED -> R.string.access_limited
    AccessStatus.BLOCKED -> R.string.access_blocked
    AccessStatus.SERVER_UNAVAILABLE -> R.string.access_server_unavailable
    AccessStatus.MAINTENANCE -> R.string.access_maintenance
    AccessStatus.ALLOWED, AccessStatus.UNKNOWN -> null
}

/** Formats elapsed seconds as HH:MM:SS for the connection timer. */
fun formatDuration(totalSeconds: Long): String {
    val s = totalSeconds.coerceAtLeast(0)
    val hours = s / 3600
    val minutes = (s % 3600) / 60
    val seconds = s % 60
    return "%02d:%02d:%02d".format(hours, minutes, seconds)
}
