package com.tritech.hopon.ui.rideDiscovery.core

/**
 * Repository interface for carpool ride (post) data.
 * Both [ApiRideRepository] (live) and [MockRideRepository]-based adapters implement this.
 */
interface RideRepository {

    /**
     * Returns rides whose destination is near [lat]/[lng] within [radiusKm] kilometres.
     * The list is ordered by ascending pickup distance from the caller's location.
     */
    suspend fun getNearbyRides(
        lat: Double,
        lng: Double,
        radiusKm: Double? = null
    ): List<RideListItem>

    /** Returns all rides created or joined by the current user. */
    suspend fun getMyRides(): List<RideListItem>

    /**
     * Creates a new carpool post from a UI-layer [CreateRideSubmission].
     * Returns [Result.success] with [RideListItem] on success.
     */
    suspend fun createRide(submission: CreateRideSubmission): Result<RideListItem>

    /**
     * Fetches a single ride by its API post ID.
     * Returns null when the post is not found or the request fails.
     */
    suspend fun getRideDetail(postId: String): RideListItem?
}
