package com.freevpn.androidtv.storage

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Builds an [EncryptedSharedPreferences] backed by the Android Keystore. Some low-end / rooted TV
 * firmwares have a flaky Keystore — if encryption setup throws we fall back to a regular (non-secret
 * but still app-private) SharedPreferences so the app keeps working. The data stored here is
 * low-sensitivity (a device UUID and an optional API key), so this degraded mode is acceptable.
 */
internal object SecurePreferencesFactory {

    private const val TAG = "SecurePrefs"

    fun create(context: Context, fileName: String): SharedPreferences {
        return try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            EncryptedSharedPreferences.create(
                context,
                fileName,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
            )
        } catch (t: Throwable) {
            // Includes GeneralSecurityException, IOException and OEM Keystore quirks.
            Log.w(TAG, "Encrypted prefs unavailable, falling back to plain prefs: ${t.message}")
            context.getSharedPreferences("${fileName}_plain", Context.MODE_PRIVATE)
        }
    }
}
