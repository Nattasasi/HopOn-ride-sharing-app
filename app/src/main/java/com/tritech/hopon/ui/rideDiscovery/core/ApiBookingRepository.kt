package com.tritech.hopon.ui.rideDiscovery.core

import android.content.Context
import android.util.Log

/**
 * Real [BookingRepository] implementation backed by the car_pool REST API.
 *
 * All calls are wrapped in [runCatching] so failures return a failed [Result]
 * rather than throwing into the caller.
 */
class ApiBookingRepository(private val context: Context) : BookingRepository {

    private val service: BookingsService by lazy {
        ApiClient.create(context)
    }

    override suspend fun createBooking(postUuid: String): Result<ApiBooking> =
        runCatching {
            service.createBooking(ApiCreateBookingRequest(post_id = postUuid))
        }.onFailure { Log.e(TAG, "createBooking failed", it) }

    override suspend fun getMyBookings(): Result<List<ApiBooking>> =
        runCatching {
            service.getMyBookings()
        }.onFailure { Log.e(TAG, "getMyBookings failed", it) }

    /**
     * Finds the current user's booking for [postMongoId] by scanning
     * [getMyBookings] and matching the populated `post_id._id` field.
     */
    override suspend fun getBookingForPost(postMongoId: String): Result<ApiBooking?> =
        runCatching {
            val all = service.getMyBookings()
            all.firstOrNull { booking ->
                // post_id is populated in /me response; compare by MongoDB _id
                booking.post_id?.id == postMongoId
            }
        }.onFailure { Log.e(TAG, "getBookingForPost($postMongoId) failed", it) }

    override suspend fun getBookingsForPost(postUuid: String): Result<List<ApiBooking>> =
        runCatching {
            service.getBookingsForPost(postUuid)
        }.onFailure { Log.e(TAG, "getBookingsForPost($postUuid) failed", it) }

    override suspend fun respondToBooking(
        bookingId: String,
        accept: Boolean
    ): Result<ApiBooking> =
        runCatching {
            val status = if (accept) "accepted" else "rejected"
            service.respondToBooking(bookingId, ApiRespondBookingRequest(status))
        }.onFailure { Log.e(TAG, "respondToBooking($bookingId, accept=$accept) failed", it) }

    override suspend fun markArrived(bookingId: String): Result<ApiBooking> =
        runCatching {
            service.markArrived(bookingId)
        }.onFailure { Log.e(TAG, "markArrived($bookingId) failed", it) }

    override suspend fun confirmBoarded(bookingId: String): Result<ApiBooking> =
        runCatching {
            service.confirmBoarded(bookingId)
        }.onFailure { Log.e(TAG, "confirmBoarded($bookingId) failed", it) }

    override suspend fun cancelBooking(bookingId: String): Result<ApiBooking?> =
        runCatching {
            val response = service.cancelBooking(bookingId)
            response.booking
        }.onFailure { Log.e(TAG, "cancelBooking($bookingId) failed", it) }

    companion object {
        private const val TAG = "ApiBookingRepository"
    }
}
