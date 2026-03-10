package com.tritech.hopon.ui.rideDiscovery.core

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

/**
 * Retrofit service for tracking endpoints.
 *
 * Base path: /api/v1/tracking/
 */
interface TrackingService {

    /**
     * Push the driver's current location for a carpool post.
     *
     * @param postId  The CarpoolPost MongoDB `_id`.
     */
    @POST("tracking/{postId}")
    suspend fun updateTracking(
        @Path("postId") postId: String,
        @Body request: ApiUpdateTrackingRequest
    ): ApiTracking

    /**
     * Fetch the latest tracking entry for a carpool post.
     *
     * @param postId  The CarpoolPost MongoDB `_id`.
     */
    @GET("tracking/{postId}")
    suspend fun getTracking(@Path("postId") postId: String): ApiTracking?
}
