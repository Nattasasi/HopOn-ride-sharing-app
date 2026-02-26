package com.tritech.hopon.ui.rideDiscovery.core

import retrofit2.http.Body
import retrofit2.http.POST

/**
 * Retrofit service for authentication endpoints.
 *
 * Base path: /api/v1/auth/
 */
interface AuthService {

    /** Register a new user account. */
    @POST("auth/register")
    suspend fun register(@Body request: ApiRegisterRequest): ApiAuthResponse

    /** Log in and receive JWT + refresh token. */
    @POST("auth/login")
    suspend fun login(@Body request: ApiLoginRequest): ApiAuthResponse

    /** Exchange a refresh token for a new access token. */
    @POST("auth/refresh")
    suspend fun refresh(@Body request: ApiRefreshRequest): ApiRefreshResponse
}
