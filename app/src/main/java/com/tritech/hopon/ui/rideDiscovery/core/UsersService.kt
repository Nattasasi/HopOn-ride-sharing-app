package com.tritech.hopon.ui.rideDiscovery.core

import retrofit2.http.GET
import retrofit2.http.Path

/**
 * Retrofit service for user profile endpoints.
 *
 * Base path: /api/v1/users/
 */
interface UsersService {

    /** Fetch a user's public profile by their MongoDB _id. */
    @GET("users/{id}")
    suspend fun getUser(@Path("id") userId: String): ApiUser
}
