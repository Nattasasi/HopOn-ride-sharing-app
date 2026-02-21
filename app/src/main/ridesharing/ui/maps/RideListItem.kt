package com.tritech.hopon.ui.maps

import com.google.android.gms.maps.model.LatLng

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
    val peopleCount: Int
)
