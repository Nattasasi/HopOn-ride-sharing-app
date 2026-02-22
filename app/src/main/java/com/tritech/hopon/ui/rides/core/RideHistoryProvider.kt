package com.tritech.hopon.ui.rides.core

import android.content.Context
import com.google.android.gms.maps.model.LatLng
import com.tritech.hopon.R
import com.tritech.hopon.ui.rideDiscovery.core.MockData
import com.tritech.hopon.ui.rideDiscovery.core.MockRide
import com.tritech.hopon.ui.rideDiscovery.core.MockRideRepository
import com.tritech.hopon.ui.rideDiscovery.core.RideParticipationRole
import com.tritech.hopon.ui.rideDiscovery.core.RideListItem
import com.tritech.hopon.utils.SessionManager
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

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
                ride.isCompleted && rideHistoryKey(ride) in relatedRideKeys
            }
            .sortedByDescending { ride ->
                parseMeetupDateTimeToEpochMillis(ride.meetupDateTimeLabel)
            }
            .map { ride ->
                val isHostedByCurrentUser = ride.host.id == currentUserId
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
                    rideTimeMinutes = ride.rideTimeMinutes
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

    private fun parseMeetupDateTimeToEpochMillis(label: String): Long {
        val parts = label.split(",", limit = 2)
        if (parts.size != 2) return Long.MIN_VALUE

        val datePart = parts[0].trim()
        val timePart = parts[1].trim()
        val timeTokens = timePart.split(":")
        if (timeTokens.size != 2) return Long.MIN_VALUE

        val hour = timeTokens[0].toIntOrNull() ?: return Long.MIN_VALUE
        val minute = timeTokens[1].toIntOrNull() ?: return Long.MIN_VALUE

        val baseCalendar = when {
            datePart.equals("Today", ignoreCase = true) -> Calendar.getInstance()
            datePart.equals("Tomorrow", ignoreCase = true) -> Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, 1)
            }
            else -> parseMonthDayToCalendar(datePart)
        } ?: return Long.MIN_VALUE

        baseCalendar.set(Calendar.HOUR_OF_DAY, hour)
        baseCalendar.set(Calendar.MINUTE, minute)
        baseCalendar.set(Calendar.SECOND, 0)
        baseCalendar.set(Calendar.MILLISECOND, 0)

        return baseCalendar.timeInMillis
    }

    private fun parseMonthDayToCalendar(datePart: String): Calendar? {
        val now = Calendar.getInstance()
        val year = now.get(Calendar.YEAR)
        val parserWithYear = SimpleDateFormat("MMM dd yyyy", Locale.US)
        val parserWithExplicitYear = SimpleDateFormat("MMM dd, yyyy", Locale.US)

        val parsedDate = parserWithExplicitYear.parse(datePart)
            ?: parserWithYear.parse("$datePart $year")
            ?: return null

        return Calendar.getInstance().apply {
            time = parsedDate
        }
    }
}
