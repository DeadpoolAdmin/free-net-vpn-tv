package com.freevpn.androidtv.network

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

/**
 * FreeNet backend contract. The base URL is the API root (see [ApiClient]); the JWT bearer token is
 * injected by [AuthInterceptor], so auth headers are not declared here.
 */
interface VpnApi {

    /** Authenticate and obtain a JWT. Called by the auth layer on its own (token-free) client. */
    @POST("login")
    suspend fun login(@Body body: LoginRequest): AuthResponse

    /** Register this device for the logged-in user. Bearer token injected by [AuthInterceptor]. */
    @POST("api/device")
    suspend fun registerDevice(@Body body: CreateDeviceRequest): DeviceResponseDto

    /** List available VPN servers / locations. */
    @GET("api/vpn")
    suspend fun getServers(): List<ServerDto>

    /** Register this device's WireGuard public key on the chosen server and get its connection details. */
    @POST("api/vpn/connect")
    suspend fun connect(@Body body: VpnConnectRequest): VpnConnectResponse

    /** Remove this device's peer from the server. */
    @POST("api/vpn/disconnect")
    suspend fun disconnect(@Body body: VpnDisconnectRequest): Response<Unit>

    /** Activate this Android TV device (optional activation flow). */
    @POST("devices/activate")
    suspend fun activateDevice(@Body body: DeviceActivateRequest): DeviceActivationDto

    /** Free access / fair-usage status. Never drives any payment flow. */
    @GET("user/access")
    suspend fun getAccess(): AccessDto

    /** Fire-and-forget operational log; response body is ignored. */
    @POST("vpn/connect-log")
    suspend fun connectLog(@Body body: ConnectLogRequest): Response<Unit>

    @POST("vpn/disconnect-log")
    suspend fun disconnectLog(@Body body: DisconnectLogRequest): Response<Unit>
}
