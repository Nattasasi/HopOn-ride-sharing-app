package com.tritech.hopon.ui.rideDiscovery.core

import com.google.android.gms.maps.model.LatLng

enum class RideParticipationRole {
    JOINED,
    HOSTED
}

enum class RideLifecycleStatus {
    ONGOING,
    UPCOMING,
    COMPLETED
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
    val peopleCount: Int,
    val maxPeopleCount: Int,
    val participationRole: RideParticipationRole? = null,
    val isCompleted: Boolean = false,
    val rideTimeMinutes: Int? = null,
    val lifecycleStatus: RideLifecycleStatus = RideLifecycleStatus.UPCOMING
)
