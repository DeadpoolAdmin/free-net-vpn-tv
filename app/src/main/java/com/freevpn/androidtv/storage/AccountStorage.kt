package com.freevpn.androidtv.storage

import android.content.Context
import android.util.Log
import com.freevpn.androidtv.model.DeviceAccount
import kotlinx.serialization.json.Json

/** Persists the last [DeviceAccount] returned by the backend so the Account screen survives relaunch. */
interface AccountStorage {
    fun get(): DeviceAccount?
    fun save(account: DeviceAccount?)
}

class AccountStorageImpl(context: Context) : AccountStorage {

    private val prefs = SecurePreferencesFactory.create(context.applicationContext, FILE_NAME)
    private val json = Json { ignoreUnknownKeys = true }

    override fun get(): DeviceAccount? =
        prefs.getString(KEY_ACCOUNT, null)?.let {
            runCatching { json.decodeFromString(DeviceAccount.serializer(), it) }
                .onFailure { e -> Log.w(TAG, "could not decode cached account: ${e.message}") }
                .getOrNull()
        }

    override fun save(account: DeviceAccount?) {
        prefs.edit().apply {
            if (account == null) remove(KEY_ACCOUNT)
            else putString(KEY_ACCOUNT, json.encodeToString(DeviceAccount.serializer(), account))
        }.apply()
    }

    private companion object {
        const val TAG = "AccountStorage"
        const val FILE_NAME = "account_secure_prefs"
        const val KEY_ACCOUNT = "device_account"
    }
}
