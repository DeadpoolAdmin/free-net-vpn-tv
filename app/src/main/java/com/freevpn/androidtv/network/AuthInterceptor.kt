package com.freevpn.androidtv.network

import com.freevpn.androidtv.data.AuthRepository
import okhttp3.Interceptor
import okhttp3.Response

/**
 * Attaches the backend JWT as `Authorization: Bearer <token>` to every request. The token is
 * obtained/cached by [AuthRepository]. On a 401 it drops the cached token, re-logs in once, and
 * retries — so an expired token self-heals.
 */
class AuthInterceptor(
    private val authRepository: AuthRepository,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val token = authRepository.bearerBlocking()
        val response = chain.proceed(original.withBearer(token))

        if (response.code == 401 && token != null) {
            response.close()
            authRepository.clearToken()
            val fresh = authRepository.bearerBlocking()
            return chain.proceed(original.withBearer(fresh))
        }
        return response
    }

    private fun okhttp3.Request.withBearer(token: String?) =
        if (token.isNullOrBlank()) this
        else newBuilder().header("Authorization", "Bearer $token").build()
}
