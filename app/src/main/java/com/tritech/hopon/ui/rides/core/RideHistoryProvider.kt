package com.tritech.hopon.ui.rides.core

import android.content.Context
import com.google.android.gms.maps.model.LatLng
import com.tritech.hopon.ui.rideDiscovery.core.MockData
import com.tritech.hopon.ui.rideDiscovery.core.MockRide
import com.tritech.hopon.ui.rideDiscovery.core.MockRideRepository
import com.tritech.hopon.ui.rideDiscovery.core.RideListItem
import com.tritech.hopon.utils.SessionManager

object RideHistoryProvider {
    fun loadCurrentUserHistoryRides(
        context: Context,
        pickupDistanceMetersForMeetup: (LatLng) -> Float
    ): List<RideListItem> {
        val currentUserId = SessionManager.getCurrentUserId(context) ?: "u001"
        val relation = MockRideRepository.getUserRideRelation(currentUserId) ?: return emptyList()
        val relatedRideKeys = (relation.rideHosted + relation.rideJoined)
            .map { rideHistoryKey(it) }
            .toSet()

        return MockData.mockRides
            .filter { rideHistoryKey(it) in relatedRideKeys }
            .map { ride ->
                RideListItem(
                    meetupLabel = ride.meetupLabel,
                    meetupLatLng = ride.meetupLatLng,
                    destinationLabel = ride.destinationLabel,
                    destinationLatLng = ride.destinationLatLng,
                    pickupDistanceMeters = pickupDistanceMetersForMeetup(ride.meetupLatLng),
                    meetupDateTimeLabel = ride.meetupDateTimeLabel,
                    waitTimeMinutes = ride.waitTimeMinutes,
                    hostName = ride.host.name,
                    hostRating = ride.host.rating,
                    hostVehicleType = ride.host.vehicleType,
                    peopleCount = ride.peopleCount,
                    maxPeopleCount = ride.maxPeopleCount
                )
            }
    }

    private fun rideHistoryKey(ride: MockRide): String {
        return listOf(
            ride.meetupLabel,
            ride.destinationLabel,
            ride.meetupDateTimeLabel,
            ride.host.id
        ).joinToString("|")
    }
}
