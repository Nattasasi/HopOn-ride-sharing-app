package com.tritech.hopon.ui.rideDiscovery.core

import com.google.android.gms.maps.model.LatLng

data class PlaceholderPlace(val name: String, val latLng: LatLng)

data class MockUser(
    val id: String,
    val name: String,
    val rating: Float,
    val vehicleType: String
)

data class MockRide(
    val meetupLabel: String,
    val meetupLatLng: LatLng,
    val destinationLabel: String,
    val destinationLatLng: LatLng,
    val meetupDateTimeLabel: String,
    val waitTimeMinutes: Int,
    val host: MockUser,
    val passengers: List<MockUser>,
    val maxPeopleCount: Int,
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

object MockData {
    val placeholderPlaces = listOf(
        PlaceholderPlace("Siam Paragon", LatLng(13.7466, 100.5347)),
        PlaceholderPlace("CentralWorld", LatLng(13.7460, 100.5395)),
        PlaceholderPlace("Terminal 21", LatLng(13.7373, 100.5607))
    )

    private val mockUsers = listOf(
        MockUser("u001", "Narin P.", 4.9f, "Toyota Yaris"),
        MockUser("u002", "Mali S.", 4.8f, "Honda City"),
        MockUser("u003", "Krit T.", 4.7f, "Mazda 2"),
        MockUser("u004", "Pimchanok R.", 4.9f, "Tesla Model 3"),
        MockUser("u005", "Thanawat K.", 4.6f, "Nissan Almera"),
        MockUser("u006", "Suda C.", 4.8f, "Mitsubishi Attrage")
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
        isCompleted: Boolean = false,
        rideTimeMinutes: Int? = null
    ): MockRide {
        val host = mockUsers[hostIndex]
        val passengers = passengerIndexes
            .map { mockUsers[it] }
            .filterNot { it.id == host.id }
        return MockRide(
            meetupLabel = meetupLabel,
            meetupLatLng = meetupLatLng,
            destinationLabel = destinationLabel,
            destinationLatLng = destinationLatLng,
            meetupDateTimeLabel = meetupDateTimeLabel,
            waitTimeMinutes = waitTimeMinutes,
            host = host,
            passengers = passengers,
            maxPeopleCount = maxPeopleCount,
            isCompleted = isCompleted,
            rideTimeMinutes = if (isCompleted) rideTimeMinutes else null
        )
    }

    val mockRides = listOf(
        ride("Asok BTS", LatLng(13.7370, 100.5603), "Siam Paragon", LatLng(13.7466, 100.5347), "Today, 18:15", 5, 0, listOf(1, 2), isCompleted = true, rideTimeMinutes = 26),
        ride("Phrom Phong", LatLng(13.7306, 100.5696), "Siam Paragon", LatLng(13.7466, 100.5347), "Today, 18:30", 8, 1, listOf(0), isCompleted = true, rideTimeMinutes = 22),
        ride("Chit Lom", LatLng(13.7449, 100.5431), "Siam Paragon", LatLng(13.7466, 100.5347), "Today, 18:45", 3, 2, listOf(3, 4, 5), isCompleted = true, rideTimeMinutes = 18),
        ride("Nana", LatLng(13.7405, 100.5550), "CentralWorld", LatLng(13.7460, 100.5395), "Today, 19:00", 6, 3, listOf(0, 1), isCompleted = true, rideTimeMinutes = 24),
        ride("Ratchathewi", LatLng(13.7514, 100.5310), "CentralWorld", LatLng(13.7460, 100.5395), "Today, 19:10", 4, 4, listOf(2), isCompleted = true, rideTimeMinutes = 16),
        ride("Victory Monument", LatLng(13.7628, 100.5372), "CentralWorld", LatLng(13.7460, 100.5395), "Today, 19:20", 7, 5, listOf(0, 3, 4), isCompleted = true, rideTimeMinutes = 29),
        ride("Sukhumvit Soi 11", LatLng(13.7429, 100.5559), "Terminal 21", LatLng(13.7373, 100.5607), "Today, 20:00", 2, 0, emptyList()),
        ride("Benjasiri Park", LatLng(13.7308, 100.5680), "Terminal 21", LatLng(13.7373, 100.5607), "Today, 20:15", 6, 1, listOf(2, 4)),
        ride("Ekkamai", LatLng(13.7197, 100.5850), "Terminal 21", LatLng(13.7373, 100.5607), "Today, 20:30", 9, 2, listOf(0, 1, 5)),
        ride("Silom Complex", LatLng(13.7296, 100.5349), "Siam Paragon", LatLng(13.7466, 100.5347), "Tomorrow, 07:45", 12, 3, listOf(4)),
        ride("Samyan Mitrtown", LatLng(13.7327, 100.5291), "CentralWorld", LatLng(13.7460, 100.5395), "Tomorrow, 08:00", 10, 4, listOf(1, 5)),
        ride("Thong Lo", LatLng(13.7241, 100.5783), "Terminal 21", LatLng(13.7373, 100.5607), "Feb 23, 08:30", 11, 5, listOf(2)),
        ride("Ari", LatLng(13.7794, 100.5381), "Siam Paragon", LatLng(13.7466, 100.5347), "Feb 25, 09:00", 15, 0, emptyList()),
        ride("On Nut", LatLng(13.7053, 100.5997), "Terminal 21", LatLng(13.7373, 100.5607), "Mar 01, 10:15", 20, 1, listOf(3, 5))
    )

    val mockUserRideRelations = mockUsers.map { user ->
        MockUserRideRelation(
            user = user,
            rideHosted = mockRides.filter { it.host.id == user.id },
            rideJoined = mockRides.filter { ride -> ride.passengers.any { it.id == user.id } }
        )
    }
}