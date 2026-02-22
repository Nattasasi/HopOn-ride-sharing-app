package com.tritech.hopon.ui.rideDiscovery.core

import com.google.android.gms.maps.model.LatLng

interface MapsView {

    // Notify user when booking request is accepted.
    fun informCabBooked()

    // Draw path polyline for pickup/trip route.
    fun showPath(latLngList: List<LatLng>)

    // Animate moving cab marker to latest server location.
    fun updateCabLocation(latLng: LatLng)

    // Notify user that cab is close to pickup point.
    fun informCabIsArriving()

    // Notify user that cab reached pickup point.
    fun informCabArrived()

    // Notify user when trip officially starts.
    fun informTripStart()

    // Notify user when trip ends.
    fun informTripEnd()

    // Show route availability error and reset state.
    fun showRoutesNotAvailableError()

    // Show external directions API error details.
    fun showDirectionApiFailedError(error: String)

}