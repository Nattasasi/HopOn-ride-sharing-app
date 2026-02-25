package com.tritech.hopon.ui.rideDiscovery.core

interface MapsView {

    // Notify user when trip ends.
    fun informTripEnd()

    // Show route availability error and reset state.
    fun showRoutesNotAvailableError()

    // Show external directions API error details.
    fun showDirectionApiFailedError(error: String)

}