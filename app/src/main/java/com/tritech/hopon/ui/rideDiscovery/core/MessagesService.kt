package com.tritech.hopon.ui.rideDiscovery.core

import retrofit2.http.GET
import retrofit2.http.Path

/**
 * Retrofit service for message (chat) endpoints.
 *
 * Base path: /api/v1/messages/
 */
interface MessagesService {

    /**
     * Fetch all messages for a carpool post, ordered by [sent_at] ascending.
     *
     * @param postId  The CarpoolPost MongoDB `_id`.
     */
    @GET("messages/{postId}")
    suspend fun getMessages(@Path("postId") postId: String): List<ApiMessage>
}
