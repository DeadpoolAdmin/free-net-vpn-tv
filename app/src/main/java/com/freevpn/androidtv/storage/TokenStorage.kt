package com.freevpn.androidtv.storage

import android.content.Context

/**
 * Securely stores auth credentials: the backend JWT bearer token (cached after login) and the
 * optional activation API key. Kept separate from [DeviceStorage] so credentials can be cleared
 * independently.
 */
interface TokenStorage {
    fun getApiKey(): String?
    fun setApiKey(key: String?)

    /** The backend JWT bearer token (cached after login). */
    fun getToken(): String?
    fun setToken(token: String?)

    /** The logged-in user's id (from the login response); needed to register the device. */
    fun getUserId(): String?
    fun setUserId(userId: String?)

    fun clear()
}

class TokenStorageImpl(context: Context) : TokenStorage {

    private val prefs = SecurePreferencesFactory.create(context.applicationContext, FILE_NAME)

    override fun getApiKey(): String? = prefs.getString(KEY_API_KEY, null)

    override fun setApiKey(key: String?) {
        prefs.edit().apply {
            if (key.isNullOrBlank()) remove(KEY_API_KEY) else putString(KEY_API_KEY, key)
        }.apply()
    }

    override fun getToken(): String? = prefs.getString(KEY_TOKEN, null)

    override fun setToken(token: String?) {
        prefs.edit().apply {
            if (token.isNullOrBlank()) remove(KEY_TOKEN) else putString(KEY_TOKEN, token)
        }.apply()
    }

    override fun getUserId(): String? = prefs.getString(KEY_USER_ID, null)

    override fun setUserId(userId: String?) {
        prefs.edit().apply {
            if (userId.isNullOrBlank()) remove(KEY_USER_ID) else putString(KEY_USER_ID, userId)
        }.apply()
    }

    override fun clear() {
        prefs.edit().remove(KEY_API_KEY).remove(KEY_TOKEN).remove(KEY_USER_ID).apply()
    }

    private companion object {
        const val FILE_NAME = "token_secure_prefs"
        const val KEY_API_KEY = "api_key"
        const val KEY_TOKEN = "jwt_token"
        const val KEY_USER_ID = "user_id"
    }
}
