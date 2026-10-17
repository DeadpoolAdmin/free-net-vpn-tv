package com.freevpn.androidtv.storage

import android.content.Context
import java.util.UUID

/**
 * Persists the stable device identity and activation flag securely.
 * The device id is generated once on first run and reused for the lifetime of the install.
 */
interface DeviceStorage {
    /** Returns the stable device id, creating and persisting one on first access. */
    fun getOrCreateDeviceId(): String

    /** Whether this device has completed (optional) activation. */
    fun isActivated(): Boolean
    fun setActivated(activated: Boolean)

    /** Resets the activation flag (keeps the device id). */
    fun clearActivation()
}

class DeviceStorageImpl(context: Context) : DeviceStorage {

    private val prefs = SecurePreferencesFactory.create(context.applicationContext, FILE_NAME)

    override fun getOrCreateDeviceId(): String {
        prefs.getString(KEY_DEVICE_ID, null)?.let { return it }
        val newId = UUID.randomUUID().toString()
        prefs.edit().putString(KEY_DEVICE_ID, newId).apply()
        return newId
    }

    override fun isActivated(): Boolean = prefs.getBoolean(KEY_ACTIVATED, false)

    override fun setActivated(activated: Boolean) {
        prefs.edit().putBoolean(KEY_ACTIVATED, activated).apply()
    }

    override fun clearActivation() {
        prefs.edit().putBoolean(KEY_ACTIVATED, false).apply()
    }

    private companion object {
        const val FILE_NAME = "device_secure_prefs"
        const val KEY_DEVICE_ID = "device_id"
        const val KEY_ACTIVATED = "activated"
    }
}
