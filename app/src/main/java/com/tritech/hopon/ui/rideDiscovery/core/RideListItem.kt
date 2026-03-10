package com.tritech.hopon.ui.rideDiscovery.core

import com.google.android.gms.maps.model.LatLng

enum class RideParticipationRole {
    JOINED,
    HOSTED
}

enum class RideLifecycleStatus {
    ONGOING,
    UPCOMING,
    COMPLETED,
    CANCELLED
}

data class RideListItem(
    val meetupLabel: String,
    val meetupLatLng: LatLng,
    val destinationLabel: String,
    val destinationLatLng: LatLng,
    val pickupDistanceMeters: Float,
    val meetupDateTimeLabel: String,
    val waitTimeMinutes: Int,
    val hostName: String,
    val hostRating: Float,
    val hostVehicleType: String,
    val vehiclePlate: String? = null,
    /** MongoDB `_id` of the driver / host user — used for feedback submission. */
    val hostUserId: String = "",
    /** Verification status of host account: unverified | pending | verified | rejected. */
    val hostVerificationStatus: String? = null,
    val peopleCount: Int,
    val maxPeopleCount: Int,
    val participationRole: RideParticipationRole? = null,
    val isCompleted: Boolean = false,
    val rideTimeMinutes: Int? = null,
    val lifecycleStatus: RideLifecycleStatus = RideLifecycleStatus.UPCOMING,
    /** API post `_id` (MongoDB ObjectId) — empty string for mock/offline items. */
    val postId: String = "",
    /**
     * API post `post_id` UUID — used for booking creation and fetching booking
     * requests.  Empty string for mock/offline items.
     */
    val postUuid: String = "",
    /** Price per seat in Thai Baht as returned by the API. */
    val pricePerSeat: Double = 0.0,
    /** Epoch millis of departure time when available from API. */
    val departureEpochMillis: Long? = null,
    /**
     * MongoDB `_id` of the current user's active booking for this ride, if any.
     * Null when the user has not booked or when the ride is a mock item.
     */
    val bookingId: String? = null,
    /**
     * Status of the current user's booking: "pending" | "confirmed" | "rejected"
     * | "cancelled", or null when not booked.
     * Note: the backend stores "confirmed" for an accepted booking.
     */
    val bookingStatus: String? = null
)
