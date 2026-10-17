package com.freevpn.androidtv.storage

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Non-secret user settings persisted with DataStore Preferences. Exposed as Flows so Compose can
 * observe them reactively.
 */
interface ConfigStorage {
    val autoConnect: Flow<Boolean>
    val autoReconnect: Flow<Boolean>
    val showNotification: Flow<Boolean>
    val lastServerId: Flow<String?>

    suspend fun setAutoConnect(value: Boolean)
    suspend fun setAutoReconnect(value: Boolean)
    suspend fun setShowNotification(value: Boolean)
    suspend fun setLastServerId(id: String?)
}

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "free_vpn_settings")

class ConfigStorageImpl(context: Context) : ConfigStorage {

    private val store = context.applicationContext.dataStore

    override val autoConnect: Flow<Boolean> = store.data.map { it[KEY_AUTO_CONNECT] ?: false }
    override val autoReconnect: Flow<Boolean> = store.data.map { it[KEY_AUTO_RECONNECT] ?: true }
    override val showNotification: Flow<Boolean> = store.data.map { it[KEY_SHOW_NOTIFICATION] ?: true }
    override val lastServerId: Flow<String?> = store.data.map { it[KEY_LAST_SERVER] }

    override suspend fun setAutoConnect(value: Boolean) {
        store.edit { it[KEY_AUTO_CONNECT] = value }
    }

    override suspend fun setAutoReconnect(value: Boolean) {
        store.edit { it[KEY_AUTO_RECONNECT] = value }
    }

    override suspend fun setShowNotification(value: Boolean) {
        store.edit { it[KEY_SHOW_NOTIFICATION] = value }
    }

    override suspend fun setLastServerId(id: String?) {
        store.edit { prefs ->
            if (id == null) prefs.remove(KEY_LAST_SERVER) else prefs[KEY_LAST_SERVER] = id
        }
    }

    private companion object {
        val KEY_AUTO_CONNECT = booleanPreferencesKey("auto_connect")
        val KEY_AUTO_RECONNECT = booleanPreferencesKey("auto_reconnect")
        val KEY_SHOW_NOTIFICATION = booleanPreferencesKey("show_notification")
        val KEY_LAST_SERVER = stringPreferencesKey("last_server_id")
    }
}
