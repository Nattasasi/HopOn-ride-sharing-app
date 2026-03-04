package com.tritech.hopon.ui.rides.core

import android.content.Context
import com.google.android.gms.maps.model.LatLng
import com.tritech.hopon.R
import com.tritech.hopon.ui.rideDiscovery.core.MockData
import com.tritech.hopon.ui.rideDiscovery.core.MockRide
import com.tritech.hopon.ui.rideDiscovery.core.MockRideRepository
import com.tritech.hopon.ui.rideDiscovery.core.RideDateTimeFormatter
import com.tritech.hopon.ui.rideDiscovery.core.RideLifecycleStatus
import com.tritech.hopon.ui.rideDiscovery.core.RideParticipationRole
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
            .filter { ride ->
                rideHistoryKey(ride) in relatedRideKeys
            }
            .sortedWith(compareByDescending<MockRide> { !it.isCompleted }
                .thenByDescending { ride ->
                    RideDateTimeFormatter.parseMeetupDateTimeToEpochMillis(ride.meetupDateTimeLabel)
                        ?: Long.MIN_VALUE
                }
                .thenByDescending { it.rideTimeMinutes ?: 0 }
            )
            .map { ride ->
                val isHostedByCurrentUser = ride.host.id == currentUserId
                val lifecycleStatus = when {
                    ride.isCompleted -> RideLifecycleStatus.COMPLETED
                    MockData.isRideOngoingForUser(currentUserId, ride) -> RideLifecycleStatus.ONGOING
                    else -> RideLifecycleStatus.UPCOMING
                }
                RideListItem(
                    meetupLabel = ride.meetupLabel,
                    meetupLatLng = ride.meetupLatLng,
                    destinationLabel = ride.destinationLabel,
                    destinationLatLng = ride.destinationLatLng,
                    pickupDistanceMeters = pickupDistanceMetersForMeetup(ride.meetupLatLng),
                    meetupDateTimeLabel = ride.meetupDateTimeLabel,
                    waitTimeMinutes = ride.waitTimeMinutes,
                    hostName = if (isHostedByCurrentUser) context.getString(R.string.me_label) else ride.host.name,
                    hostRating = ride.host.rating,
                    hostVehicleType = ride.host.vehicleType,
                    peopleCount = ride.peopleCount,
                    maxPeopleCount = ride.maxPeopleCount,
                    participationRole = if (isHostedByCurrentUser) {
                        RideParticipationRole.HOSTED
                    } else {
                        RideParticipationRole.JOINED
                    },
                    isCompleted = ride.isCompleted,
                    rideTimeMinutes = ride.rideTimeMinutes,
                    lifecycleStatus = lifecycleStatus
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
