package com.tritech.hopon.ui.rideDiscovery.core

/**
 * Repository interface for all booking-related operations (Phase 6).
 *
 * All methods wrap results in [Result] so call-sites can handle network failures
 * without propagating exceptions.
 */
interface BookingRepository {

    /**
     * Request to join a carpool post.
     *
     * @param postUuid  The CarpoolPost `post_id` UUID (NOT the MongoDB `_id`).
     */
    suspend fun createBooking(postUuid: String): Result<ApiBooking>

    /**
     * Fetch all bookings for the current authenticated passenger.
     * Each booking's [ApiBooking.post_id] field is populated (full post object).
     */
    suspend fun getMyBookings(): Result<List<ApiBooking>>

    /**
     * Find the current user's active booking for a specific post, if any.
     *
     * @param postMongoId  The CarpoolPost MongoDB `_id` (used to match bookings
     *                     from [getMyBookings] where `post_id` is populated).
     */
    suspend fun getBookingForPost(postMongoId: String): Result<ApiBooking?>

    /**
     * Fetch all booking requests for a specific post (driver view).
     *
     * @param postUuid  The CarpoolPost UUID `post_id`.
     */
    suspend fun getBookingsForPost(postUuid: String): Result<List<ApiBooking>>

    /**
     * Driver accepts or rejects a booking.
     *
     * @param bookingId  MongoDB `_id` of the booking.
     * @param accept     True to accept, false to reject.
     */
    suspend fun respondToBooking(bookingId: String, accept: Boolean): Result<ApiBooking>

    /**
     * Cancel an existing booking (passenger or driver).
     *
     * @param bookingId  MongoDB `_id` of the booking.
     */
    suspend fun cancelBooking(bookingId: String): Result<ApiBooking?>
}
