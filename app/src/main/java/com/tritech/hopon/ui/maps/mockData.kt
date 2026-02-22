package com.tritech.hopon.ui.maps

import com.google.android.gms.maps.model.LatLng

data class PlaceholderPlace(val name: String, val latLng: LatLng)

data class MockUser(
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
    val peopleCount: Int,
    val maxPeopleCount: Int
)

object MockData {
    val placeholderPlaces = listOf(
        PlaceholderPlace("Siam Paragon", LatLng(13.7466, 100.5347)),
        PlaceholderPlace("CentralWorld", LatLng(13.7460, 100.5395)),
        PlaceholderPlace("Terminal 21", LatLng(13.7373, 100.5607))
    )

    private val mockUsers = listOf(
        MockUser("Narin P.", 4.9f, "Toyota Yaris"),
        MockUser("Mali S.", 4.8f, "Honda City"),
        MockUser("Krit T.", 4.7f, "Mazda 2"),
        MockUser("Pimchanok R.", 4.9f, "Tesla Model 3"),
        MockUser("Thanawat K.", 4.6f, "Nissan Almera"),
        MockUser("Suda C.", 4.8f, "Mitsubishi Attrage")
    )

    val mockRides = listOf(
        MockRide("Asok BTS", LatLng(13.7370, 100.5603), "Siam Paragon", LatLng(13.7466, 100.5347), "Today, 18:15", 5, mockUsers[0], 3, 4),
        MockRide("Phrom Phong", LatLng(13.7306, 100.5696), "Siam Paragon", LatLng(13.7466, 100.5347), "Today, 18:30", 8, mockUsers[1], 2, 4),
        MockRide("Chit Lom", LatLng(13.7449, 100.5431), "Siam Paragon", LatLng(13.7466, 100.5347), "Today, 18:45", 3, mockUsers[2], 4, 4),
        MockRide("Nana", LatLng(13.7405, 100.5550), "CentralWorld", LatLng(13.7460, 100.5395), "Today, 19:00", 6, mockUsers[3], 3, 4),
        MockRide("Ratchathewi", LatLng(13.7514, 100.5310), "CentralWorld", LatLng(13.7460, 100.5395), "Today, 19:10", 4, mockUsers[4], 2, 4),
        MockRide("Victory Monument", LatLng(13.7628, 100.5372), "CentralWorld", LatLng(13.7460, 100.5395), "Today, 19:20", 7, mockUsers[5], 4, 4),
        MockRide("Sukhumvit Soi 11", LatLng(13.7429, 100.5559), "Terminal 21", LatLng(13.7373, 100.5607), "Today, 20:00", 2, mockUsers[0], 1, 4),
        MockRide("Benjasiri Park", LatLng(13.7308, 100.5680), "Terminal 21", LatLng(13.7373, 100.5607), "Today, 20:15", 6, mockUsers[1], 3, 4),
        MockRide("Ekkamai", LatLng(13.7197, 100.5850), "Terminal 21", LatLng(13.7373, 100.5607), "Today, 20:30", 9, mockUsers[2], 4, 4),
        MockRide("Silom Complex", LatLng(13.7296, 100.5349), "Siam Paragon", LatLng(13.7466, 100.5347), "Tomorrow, 07:45", 12, mockUsers[3], 2, 4),
        MockRide("Samyan Mitrtown", LatLng(13.7327, 100.5291), "CentralWorld", LatLng(13.7460, 100.5395), "Tomorrow, 08:00", 10, mockUsers[4], 3, 4),
        MockRide("Thong Lo", LatLng(13.7241, 100.5783), "Terminal 21", LatLng(13.7373, 100.5607), "Feb 23, 08:30", 11, mockUsers[5], 2, 4),
        MockRide("Ari", LatLng(13.7794, 100.5381), "Siam Paragon", LatLng(13.7466, 100.5347), "Feb 25, 09:00", 15, mockUsers[0], 1, 4),
        MockRide("On Nut", LatLng(13.7053, 100.5997), "Terminal 21", LatLng(13.7373, 100.5607), "Mar 01, 10:15", 20, mockUsers[1], 3, 4)
    )
}