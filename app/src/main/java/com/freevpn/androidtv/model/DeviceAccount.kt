package com.freevpn.androidtv.model

import kotlinx.serialization.Serializable

/**
 * The device / subscription record the backend returns from `POST /api/device`. Shown on the
 * Account screen and cached so it survives navigation and relaunch.
 */
@Serializable
data class DeviceAccount(
    val id: Int,
    val deviceId: String,
    val userId: String,
    val expireDate: String? = null,
    val packageName: String? = null,
    val createdAt: String? = null,
)
