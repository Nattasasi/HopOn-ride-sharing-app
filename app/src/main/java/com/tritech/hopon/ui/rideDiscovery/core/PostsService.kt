package com.tritech.hopon.ui.rideDiscovery.core

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Retrofit service for CarpoolPost (ride) endpoints.
 *
 * Base path: /api/v1/posts/
 */
interface PostsService {

    /**
     * Fetch nearby posts filtered by location and optional radius.
     *
     * @param lat  Latitude of the search origin.
     * @param lng  Longitude of the search origin.
     * @param radius  Search radius in kilometres (default handled server-side).
     */
    @GET("posts")
    suspend fun getNearbyPosts(
        @Query("lat") lat: Double,
        @Query("lng") lng: Double,
        @Query("radius") radius: Double? = null
    ): List<ApiCarpoolPost>

    /** Fetch all posts created by the currently authenticated user. */
    @GET("posts/me")
    suspend fun getMyPosts(): List<ApiCarpoolPost>

    /** Create a new carpool post. Requires auth JWT. */
    @POST("posts")
    suspend fun createPost(@Body request: ApiCreatePostRequest): ApiCarpoolPost

    /** Fetch a single post by its ID. */
    @GET("posts/{id}")
    suspend fun getPost(@Path("id") postId: String): ApiCarpoolPost

    /**
     * Update the status of a post.
     *
     * Valid status values: "active" | "in_progress" | "completed" | "cancelled"
     */
    @PATCH("posts/{id}/status")
    suspend fun updateStatus(
        @Path("id") postId: String,
        @Body request: ApiUpdateStatusRequest
    ): ApiCarpoolPost

    /** Start active ride and mark non-boarded passengers as left behind. */
    @PATCH("posts/{id}/start")
    suspend fun startRide(@Path("id") postId: String): ApiCarpoolPost
}
