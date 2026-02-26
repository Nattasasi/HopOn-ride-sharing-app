package com.tritech.hopon.ui.rideDiscovery.core

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

/**
 * Retrofit service for payment endpoints.
 *
 * Base path: /api/v1/payments/
 */
interface PaymentsService {

    /** Create a new payment record. */
    @POST("payments")
    suspend fun createPayment(@Body request: ApiCreatePaymentRequest): ApiPayment

    /**
     * Fetch the payment for a booking.
     *
     * @param bookingId  The Booking MongoDB `_id`.
     */
    @GET("payments/{bookingId}")
    suspend fun getPayment(@Path("bookingId") bookingId: String): ApiPayment?
}
