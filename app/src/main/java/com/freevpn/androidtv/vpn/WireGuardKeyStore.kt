package com.freevpn.androidtv.vpn

import android.content.Context
import com.freevpn.androidtv.storage.SecurePreferencesFactory
import com.wireguard.crypto.Key
import com.wireguard.crypto.KeyPair

/**
 * Persists this device's WireGuard keypair. The key is generated once and reused, so reconnecting
 * registers the same public key with the backend instead of accumulating a new peer each time. The
 * private key is stored in encrypted prefs and never leaves the device.
 */
class WireGuardKeyStore(context: Context) {

    private val prefs = SecurePreferencesFactory.create(context.applicationContext, FILE_NAME)

    fun getOrCreate(): KeyPair {
        prefs.getString(KEY_PRIVATE, null)?.let { stored ->
            runCatching { return KeyPair(Key.fromBase64(stored)) }
        }
        val keyPair = KeyPair()
        prefs.edit().putString(KEY_PRIVATE, keyPair.privateKey.toBase64()).apply()
        return keyPair
    }

    private companion object {
        const val FILE_NAME = "wg_key_secure_prefs"
        const val KEY_PRIVATE = "wg_private_key"
    }
}
