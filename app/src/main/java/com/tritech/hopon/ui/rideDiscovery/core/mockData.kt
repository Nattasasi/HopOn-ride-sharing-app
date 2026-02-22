package com.tritech.hopon.ui.rideDiscovery.core

import com.google.android.gms.maps.model.LatLng

data class PlaceholderPlace(val name: String, val latLng: LatLng)

data class MockUser(
    val id: String,
    val name: String,
    val rating: Float,
    val vehicleType: String,
    val contactInfo: String
)

data class MockRide(
    val meetupLabel: String,
    val meetupLatLng: LatLng,
    val destinationLabel: String,
    val destinationLatLng: LatLng,
    val meetupDateTimeLabel: String,
    val meetupDateLabel: String,
    val meetupTimeLabel: String,
    val waitTimeMinutes: Int,
    val host: MockUser,
    val passengers: List<MockUser>,
    val maxPeopleCount: Int,
    val vehicleInfo: String,
    val contactInfo: String,
    val additionalNotes: String,
    val isCompleted: Boolean,
    val rideTimeMinutes: Int?
) {
    init {
        require(!isCompleted || rideTimeMinutes != null)
    }

    val peopleCount: Int
        get() = 1 + passengers.size
}

data class MockUserRideRelation(
    val user: MockUser,
    val rideHosted: List<MockRide>,
    val rideJoined: List<MockRide>
)

data class CreateRideSubmission(
    val meetupLocation: String,
    val meetupLatLng: LatLng?,
    val destination: String,
    val destinationLatLng: LatLng?,
    val meetupDate: String,
    val meetupTime: String,
    val waitTimeMinutes: Int,
    val maxPeopleCount: Int,
    val vehicleInfo: String,
    val contactInfo: String,
    val additionalNotes: String
)

object MockData {
    val placeholderPlaces = listOf(
        PlaceholderPlace("Siam Paragon", LatLng(13.7466, 100.5347)),
        PlaceholderPlace("CentralWorld", LatLng(13.7460, 100.5395)),
        PlaceholderPlace("Terminal 21", LatLng(13.7373, 100.5607))
    )

    private val mockUsers = listOf(
        MockUser("u001", "Narin P.", 4.9f, "Toyota Yaris", "080-111-1111"),
        MockUser("u002", "Mali S.", 4.8f, "Honda City", "080-222-2222"),
        MockUser("u003", "Krit T.", 4.7f, "Mazda 2", "080-333-3333"),
        MockUser("u004", "Pimchanok R.", 4.9f, "Tesla Model 3", "080-444-4444"),
        MockUser("u005", "Thanawat K.", 4.6f, "Nissan Almera", "080-555-5555"),
        MockUser("u006", "Suda C.", 4.8f, "Mitsubishi Attrage", "080-666-6666")
    )

    private fun ride(
        meetupLabel: String,
        meetupLatLng: LatLng,
        destinationLabel: String,
        destinationLatLng: LatLng,
        meetupDateTimeLabel: String,
        waitTimeMinutes: Int,
        hostIndex: Int,
        passengerIndexes: List<Int>,
        maxPeopleCount: Int = 4,
        vehicleInfo: String? = null,
        contactInfo: String? = null,
        additionalNotes: String = "",
        isCompleted: Boolean = false,
        rideTimeMinutes: Int? = null
    ): MockRide {
        val host = mockUsers[hostIndex]
        val passengers = passengerIndexes
            .map { mockUsers[it] }
            .filterNot { it.id == host.id }
        val meetupDateLabel = meetupDateTimeLabel.substringBefore(",").trim()
        val meetupTimeLabel = meetupDateTimeLabel.substringAfter(",", "").trim()
        return MockRide(
            meetupLabel = meetupLabel,
            meetupLatLng = meetupLatLng,
            destinationLabel = destinationLabel,
            destinationLatLng = destinationLatLng,
            meetupDateTimeLabel = meetupDateTimeLabel,
            meetupDateLabel = meetupDateLabel,
            meetupTimeLabel = meetupTimeLabel,
            waitTimeMinutes = waitTimeMinutes,
            host = host,
            passengers = passengers,
            maxPeopleCount = maxPeopleCount,
            vehicleInfo = vehicleInfo ?: host.vehicleType,
            contactInfo = contactInfo ?: host.contactInfo,
            additionalNotes = additionalNotes,
            isCompleted = isCompleted,
            rideTimeMinutes = if (isCompleted) rideTimeMinutes else null
        )
    }

    private val seedMockRides = listOf(
        ride("Asok BTS", LatLng(13.7370, 100.5603), "Siam Paragon", LatLng(13.7466, 100.5347), "Today, 18:15", 5, 0, listOf(1, 2), isCompleted = true, rideTimeMinutes = 26),
        ride("Phrom Phong", LatLng(13.7306, 100.5696), "Siam Paragon", LatLng(13.7466, 100.5347), "Today, 18:30", 8, 1, listOf(0), isCompleted = true, rideTimeMinutes = 22),
        ride("Chit Lom", LatLng(13.7449, 100.5431), "Siam Paragon", LatLng(13.7466, 100.5347), "Today, 18:45", 3, 2, listOf(3, 4, 5), isCompleted = true, rideTimeMinutes = 18),
        ride("Nana", LatLng(13.7405, 100.5550), "CentralWorld", LatLng(13.7460, 100.5395), "Today, 19:00", 6, 3, listOf(0, 1), isCompleted = true, rideTimeMinutes = 24),
        ride("Ratchathewi", LatLng(13.7514, 100.5310), "CentralWorld", LatLng(13.7460, 100.5395), "Today, 19:10", 4, 4, listOf(2), isCompleted = true, rideTimeMinutes = 16),
        ride("Victory Monument", LatLng(13.7628, 100.5372), "CentralWorld", LatLng(13.7460, 100.5395), "Today, 19:20", 7, 5, listOf(0, 3, 4), isCompleted = true, rideTimeMinutes = 29),
        ride("Sukhumvit Soi 11", LatLng(13.7429, 100.5559), "Terminal 21", LatLng(13.7373, 100.5607), "Today, 20:00", 2, 0, emptyList(), additionalNotes = "Please wait near the BTS exit."),
        ride("Benjasiri Park", LatLng(13.7308, 100.5680), "Terminal 21", LatLng(13.7373, 100.5607), "Today, 20:15", 6, 1, listOf(2, 4), additionalNotes = "White sedan, plate ends with 19."),
        ride("Ekkamai", LatLng(13.7197, 100.5850), "Terminal 21", LatLng(13.7373, 100.5607), "Today, 20:30", 9, 2, listOf(0, 1, 5), additionalNotes = "Running 5 minutes late is okay."),
        ride("Silom Complex", LatLng(13.7296, 100.5349), "Siam Paragon", LatLng(13.7466, 100.5347), "Tomorrow, 07:45", 12, 3, listOf(4), additionalNotes = "Morning commute route."),
        ride("Samyan Mitrtown", LatLng(13.7327, 100.5291), "CentralWorld", LatLng(13.7460, 100.5395), "Tomorrow, 08:00", 10, 4, listOf(1, 5), additionalNotes = "Please be on time."),
        ride("Thong Lo", LatLng(13.7241, 100.5783), "Terminal 21", LatLng(13.7373, 100.5607), "Feb 23, 08:30", 11, 5, listOf(2), additionalNotes = "Pickup near main gate."),
        ride("Ari", LatLng(13.7794, 100.5381), "Siam Paragon", LatLng(13.7466, 100.5347), "Feb 25, 09:00", 15, 0, emptyList(), additionalNotes = "Space for one small luggage."),
        ride("On Nut", LatLng(13.7053, 100.5997), "Terminal 21", LatLng(13.7373, 100.5607), "Mar 01, 10:15", 20, 1, listOf(3, 5), additionalNotes = "Meet at the taxi stand.")
    )

    private val mutableMockRides = seedMockRides.toMutableList()
    private val ongoingRideKeyByUserId = mutableMapOf<String, String>()

    val mockRides: List<MockRide>
        get() = mutableMockRides.toList()

    val mockUserRideRelations: List<MockUserRideRelation>
        get() = mockUsers.map { user ->
            MockUserRideRelation(
                user = user,
                rideHosted = mockRides.filter { it.host.id == user.id },
                rideJoined = mockRides.filter { ride -> ride.passengers.any { it.id == user.id } }
            )
        }

    private fun resolveHost(userId: String?): MockUser {
        return mockUsers.find { it.id == userId } ?: mockUsers.first()
    }

    private fun findUser(userId: String?): MockUser? {
        return mockUsers.find { it.id == userId }
    }

    private fun rideKey(ride: MockRide): String {
        return listOf(
            ride.meetupLabel,
            ride.destinationLabel,
            ride.meetupDateTimeLabel,
            ride.host.id
        ).joinToString("|")
    }

    private fun findRideIndex(
        meetupLabel: String,
        destinationLabel: String,
        meetupDateTimeLabel: String,
        hostName: String
    ): Int {
        return mutableMockRides.indexOfFirst { ride ->
            ride.meetupLabel == meetupLabel &&
                ride.destinationLabel == destinationLabel &&
                ride.meetupDateTimeLabel == meetupDateTimeLabel &&
                ride.host.name == hostName
        }
    }

    fun hasOngoingRide(userId: String?): Boolean {
        if (userId.isNullOrBlank()) return false
        return ongoingRideKeyByUserId[userId] != null
    }

    fun isRideOngoingForUser(userId: String?, ride: MockRide): Boolean {
        if (userId.isNullOrBlank()) return false
        val ongoingKey = ongoingRideKeyByUserId[userId] ?: return false
        return ongoingKey == rideKey(ride)
    }

    fun startOngoingRide(
        userId: String?,
        meetupLabel: String,
        destinationLabel: String,
        meetupDateTimeLabel: String,
        hostName: String
    ): Boolean {
        if (userId.isNullOrBlank()) return false
        if (hasOngoingRide(userId)) return false

        val index = findRideIndex(meetupLabel, destinationLabel, meetupDateTimeLabel, hostName)
        if (index == -1) return false

        ongoingRideKeyByUserId[userId] = rideKey(mutableMockRides[index])
        return true
    }

    fun joinRide(
        userId: String?,
        meetupLabel: String,
        destinationLabel: String,
        meetupDateTimeLabel: String,
        hostName: String
    ): MockRide? {
        val rider = findUser(userId) ?: return null
        val index = findRideIndex(meetupLabel, destinationLabel, meetupDateTimeLabel, hostName)
        if (index == -1) return null

        val ride = mutableMockRides[index]
        if (ride.host.id == rider.id) {
            return ride
        }

        if (ride.passengers.any { it.id == rider.id }) {
            return ride
        }

        if (ride.peopleCount >= ride.maxPeopleCount) {
            return null
        }

        val updatedRide = ride.copy(passengers = ride.passengers + rider)
        mutableMockRides[index] = updatedRide
        return updatedRide
    }

    fun addCreatedRide(submission: CreateRideSubmission, hostUserId: String?): MockRide {
        val host = resolveHost(hostUserId)
        val normalizedDate = submission.meetupDate.trim().ifEmpty { "Today" }
        val normalizedTime = submission.meetupTime.trim().ifEmpty { "18:00" }
        val normalizedMeetup = submission.meetupLocation.trim().ifEmpty { "Current Location" }
        val normalizedDestination = submission.destination.trim().ifEmpty { "Selected place" }

        val createdRide = MockRide(
            meetupLabel = normalizedMeetup,
            meetupLatLng = submission.meetupLatLng ?: placeholderPlaces.first().latLng,
            destinationLabel = normalizedDestination,
            destinationLatLng = submission.destinationLatLng ?: placeholderPlaces.first().latLng,
            meetupDateTimeLabel = "$normalizedDate, $normalizedTime",
            meetupDateLabel = normalizedDate,
            meetupTimeLabel = normalizedTime,
            waitTimeMinutes = submission.waitTimeMinutes.coerceAtLeast(0),
            host = host,
            passengers = emptyList(),
            maxPeopleCount = submission.maxPeopleCount.coerceAtLeast(1),
            vehicleInfo = submission.vehicleInfo.trim().takeIf { it.isNotEmpty() }?.let {
                if (it.equals("Default", ignoreCase = true)) host.vehicleType else it
            } ?: host.vehicleType,
            contactInfo = submission.contactInfo.trim().takeIf { it.isNotEmpty() }?.let {
                if (it.equals("Default", ignoreCase = true)) host.contactInfo else it
            } ?: host.contactInfo,
            additionalNotes = submission.additionalNotes.trim(),
            isCompleted = false,
            rideTimeMinutes = null
        )

        mutableMockRides.add(0, createdRide)
        return createdRide
    }
}