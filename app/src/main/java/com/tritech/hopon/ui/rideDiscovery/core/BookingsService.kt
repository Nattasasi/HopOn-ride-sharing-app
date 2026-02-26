package com.tritech.hopon.ui.rideDiscovery.core

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path

/**
 * Retrofit service for Booking endpoints.
 *
 * Base path: /api/v1/bookings/
 *
 * Notes on response shapes (from the car_pool API):
 *  - createBooking  → raw ApiBooking (post_id is a UUID string, not populated)
 *  - getMyBookings  → raw JSON array (not wrapped); post_id IS populated
 *  - getBookingsForPost → raw JSON array (not wrapped); passenger_id IS populated
 *  - respondToBooking → raw ApiBooking
 *  - cancelBooking  → { message, booking } envelope
 */
interface BookingsService {

    /** Request to join a carpool post. Requires auth JWT. */
    @POST("bookings")
    suspend fun createBooking(@Body request: ApiCreateBookingRequest): ApiBooking

    /**
     * Fetch all bookings for the current user (as passenger).
     * Backend returns a raw JSON array — NOT wrapped in a `bookings` key.
     */
    @GET("bookings/me")
    suspend fun getMyBookings(): List<ApiBooking>

    /**
     * Fetch all bookings for a specific post (driver view).
     * Backend returns a raw JSON array — NOT wrapped.
     * [postId] must be the CarpoolPost UUID `post_id`, NOT the MongoDB `_id`.
     */
    @GET("bookings/posts/{postId}")
    suspend fun getBookingsForPost(@Path("postId") postId: String): List<ApiBooking>

    /**
     * Accept or reject a booking (driver action).
     *
     * @param status  "accepted" or "rejected"
     * Backend stores "confirmed" internally for "accepted".
     */
    @PATCH("bookings/{id}/respond")
    suspend fun respondToBooking(
        @Path("id") bookingId: String,
        @Body request: ApiRespondBookingRequest
    ): ApiBooking

    /**
     * Cancel an existing booking.
     * Backend returns { message, booking } envelope.
     */
    @PATCH("bookings/{id}/cancel")
    suspend fun cancelBooking(@Path("id") bookingId: String): ApiCancelBookingResponse
}
