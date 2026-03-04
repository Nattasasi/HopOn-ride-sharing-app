package com.tritech.hopon.ui.rideDiscovery.core

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

/**
 * Retrofit service for feedback endpoints.
 *
 * Base path: /api/v1/feedback/
 */
interface FeedbackService {

    /** Submit feedback after a completed ride. */
    @POST("feedback")
    suspend fun createFeedback(@Body request: ApiCreateFeedbackRequest): ApiFeedback

    /**
     * Fetch all feedback received by a user.
     *
     * @param userId  The User MongoDB `_id`.
     */
    @GET("feedback/users/{userId}/feedback")
    suspend fun getFeedback(@Path("userId") userId: String): List<ApiFeedback>
}
