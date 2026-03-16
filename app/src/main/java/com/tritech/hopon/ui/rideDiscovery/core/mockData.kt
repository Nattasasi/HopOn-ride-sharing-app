package com.tritech.hopon.ui.rideDiscovery.core

import com.google.android.gms.maps.model.LatLng
import java.util.Calendar

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
    val pricePerSeat: Double = 0.0,
    val vehiclePlate: String = "",
    val vehicleInfo: String,
    val contactInfo: String,
    val additionalNotes: String
)

data class MockChatMessage(
    val localId: String = "",
    val senderUserId: String,
    val senderDisplayName: String,
    val message: String,
    val sentAtLabel: String,
    val deliveryStatus: ChatDeliveryStatus = ChatDeliveryStatus.SENT
)

enum class ChatDeliveryStatus {
    SENDING,
    SENT,
    FAILED
}

object MockData {
    val placeholderPlaces: List<PlaceholderPlace> = listOf(
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
        val (meetupDateLabel, meetupTimeLabel) = RideDateTimeFormatter.splitMeetupDateTimeLabel(meetupDateTimeLabel)
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
        ride("Asok BTS", LatLng(13.7370, 100.5603), "Siam Paragon", LatLng(13.7466, 100.5347), RideDateTimeFormatter.seedMeetupDateTimeLabel(0, "18:15"), 5, 0, listOf(1, 2), isCompleted = true, rideTimeMinutes = 26),
        ride("Phrom Phong", LatLng(13.7306, 100.5696), "Siam Paragon", LatLng(13.7466, 100.5347), RideDateTimeFormatter.seedMeetupDateTimeLabel(0, "18:30"), 8, 1, listOf(0), isCompleted = true, rideTimeMinutes = 22),
        ride("Chit Lom", LatLng(13.7449, 100.5431), "Siam Paragon", LatLng(13.7466, 100.5347), RideDateTimeFormatter.seedMeetupDateTimeLabel(0, "18:45"), 3, 2, listOf(3, 4, 5), isCompleted = true, rideTimeMinutes = 18),
        ride("Nana", LatLng(13.7405, 100.5550), "CentralWorld", LatLng(13.7460, 100.5395), RideDateTimeFormatter.seedMeetupDateTimeLabel(0, "19:00"), 6, 3, listOf(0, 1), isCompleted = true, rideTimeMinutes = 24),
        ride("Ratchathewi", LatLng(13.7514, 100.5310), "CentralWorld", LatLng(13.7460, 100.5395), RideDateTimeFormatter.seedMeetupDateTimeLabel(0, "19:10"), 4, 4, listOf(2), isCompleted = true, rideTimeMinutes = 16),
        ride("Victory Monument", LatLng(13.7628, 100.5372), "CentralWorld", LatLng(13.7460, 100.5395), RideDateTimeFormatter.seedMeetupDateTimeLabel(0, "19:20"), 7, 5, listOf(0, 3, 4), isCompleted = true, rideTimeMinutes = 29),
        ride("Sukhumvit Soi 11", LatLng(13.7429, 100.5559), "Terminal 21", LatLng(13.7373, 100.5607), RideDateTimeFormatter.seedMeetupDateTimeLabel(0, "20:00"), 2, 0, emptyList(), additionalNotes = "Please wait near the BTS exit."),
        ride("Benjasiri Park", LatLng(13.7308, 100.5680), "Terminal 21", LatLng(13.7373, 100.5607), RideDateTimeFormatter.seedMeetupDateTimeLabel(0, "20:15"), 6, 1, listOf(2, 4), additionalNotes = "White sedan, plate ends with 19."),
        ride("Ekkamai", LatLng(13.7197, 100.5850), "Terminal 21", LatLng(13.7373, 100.5607), RideDateTimeFormatter.seedMeetupDateTimeLabel(0, "20:30"), 9, 2, listOf(0, 1, 5), additionalNotes = "Running 5 minutes late is okay."),
        ride("Silom Complex", LatLng(13.7296, 100.5349), "Siam Paragon", LatLng(13.7466, 100.5347), RideDateTimeFormatter.seedMeetupDateTimeLabel(1, "07:45"), 12, 3, listOf(4), additionalNotes = "Morning commute route."),
        ride("Samyan Mitrtown", LatLng(13.7327, 100.5291), "CentralWorld", LatLng(13.7460, 100.5395), RideDateTimeFormatter.seedMeetupDateTimeLabel(1, "08:00"), 10, 4, listOf(1, 5), additionalNotes = "Please be on time."),
        ride("Thong Lo", LatLng(13.7241, 100.5783), "Terminal 21", LatLng(13.7373, 100.5607), RideDateTimeFormatter.seedMeetupDateTimeLabel(1, "08:30"), 11, 5, listOf(2), additionalNotes = "Pickup near main gate."),
        ride("Ari", LatLng(13.7794, 100.5381), "Siam Paragon", LatLng(13.7466, 100.5347), RideDateTimeFormatter.seedMeetupDateTimeLabel(3, "09:00"), 15, 0, emptyList(), additionalNotes = "Space for one small luggage."),
        ride("On Nut", LatLng(13.7053, 100.5997), "Terminal 21", LatLng(13.7373, 100.5607), RideDateTimeFormatter.seedMeetupDateTimeLabel(7, "10:15"), 20, 1, listOf(3, 5), additionalNotes = "Meet at the taxi stand.")
    )

    private val mutableMockRides = seedMockRides.toMutableList()
    private val ongoingRideKeyByUserId = buildInitialOngoingRideKeys().toMutableMap()
    private val rideChatMessagesByRideKey = mutableMapOf<String, MutableList<MockChatMessage>>()

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

    fun userNameForId(userId: String?): String? {
        return findUser(userId)?.name
    }

    private fun rideKey(ride: MockRide): String {
        return listOf(
            ride.meetupLabel,
            ride.destinationLabel,
            ride.meetupDateTimeLabel,
            ride.host.id
        ).joinToString("|")
    }

    private fun buildInitialOngoingRideKeys(): Map<String, String> {
        return mockUsers.mapNotNull { user ->
            val initialRide = mutableMockRides.firstOrNull { ride ->
                !ride.isCompleted &&
                    (ride.host.id == user.id || ride.passengers.any { passenger -> passenger.id == user.id })
            }
            if (initialRide != null) user.id to rideKey(initialRide) else null
        }.toMap()
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

    fun ongoingRideForUser(userId: String?): MockRide? {
        if (userId.isNullOrBlank()) return null
        val ongoingKey = ongoingRideKeyByUserId[userId] ?: return null
        return mutableMockRides.firstOrNull { ride -> rideKey(ride) == ongoingKey }
    }

    fun cancelOngoingRide(userId: String?): Boolean {
        if (userId.isNullOrBlank()) return false
        val removedRideKey = ongoingRideKeyByUserId.remove(userId) ?: return false
        rideChatMessagesByRideKey.remove(removedRideKey)
        return true
    }

    fun completeOngoingRide(userId: String?, rideTimeMinutes: Int = 20): Boolean {
        if (userId.isNullOrBlank()) return false
        val ongoingKey = ongoingRideKeyByUserId[userId] ?: return false
        val index = mutableMockRides.indexOfFirst { ride -> rideKey(ride) == ongoingKey }
        if (index == -1) {
            ongoingRideKeyByUserId.remove(userId)
            return false
        }

        val ride = mutableMockRides[index]
        mutableMockRides[index] = ride.copy(
            isCompleted = true,
            rideTimeMinutes = ride.rideTimeMinutes ?: rideTimeMinutes
        )
        ongoingRideKeyByUserId.remove(userId)
        rideChatMessagesByRideKey.remove(ongoingKey)
        return true
    }

    fun groupChatParticipantsForUser(userId: String?): List<MockUser> {
        val ride = ongoingRideForUser(userId) ?: return emptyList()
        return listOf(ride.host) + ride.passengers
    }

    fun groupChatMessagesForUser(userId: String?): List<MockChatMessage> {
        if (userId.isNullOrBlank()) return emptyList()
        val ride = ongoingRideForUser(userId) ?: return emptyList()
        val key = rideKey(ride)
        val existing = rideChatMessagesByRideKey[key]
        if (existing != null) {
            return existing.toList()
        }

        val initialMessages = mutableListOf<MockChatMessage>()
        initialMessages += MockChatMessage(
            senderUserId = "system",
            senderDisplayName = "System",
            message = "Group chat created for this ride.",
            sentAtLabel = "Now"
        )
        initialMessages += MockChatMessage(
            senderUserId = ride.host.id,
            senderDisplayName = ride.host.name,
            message = "Hi everyone, please be ready at ${ride.meetupLabel}.",
            sentAtLabel = "Now"
        )
        val firstPassenger = ride.passengers.firstOrNull()
        if (firstPassenger != null) {
            initialMessages += MockChatMessage(
                senderUserId = firstPassenger.id,
                senderDisplayName = firstPassenger.name,
                message = "Got it, I’m on the way!",
                sentAtLabel = "Now"
            )
        }

        rideChatMessagesByRideKey[key] = initialMessages
        return initialMessages.toList()
    }

    fun sendGroupChatMessage(userId: String?, message: String): List<MockChatMessage> {
        if (userId.isNullOrBlank()) return emptyList()
        val ride = ongoingRideForUser(userId) ?: return emptyList()
        val sender = findUser(userId) ?: return emptyList()
        val normalizedMessage = message.trim()
        if (normalizedMessage.isEmpty()) {
            return groupChatMessagesForUser(userId)
        }

        val key = rideKey(ride)
        val messages = rideChatMessagesByRideKey.getOrPut(key) {
            groupChatMessagesForUser(userId).toMutableList()
        }
        messages += MockChatMessage(
            senderUserId = sender.id,
            senderDisplayName = sender.name,
            message = normalizedMessage,
            sentAtLabel = "Now"
        )

        val otherParticipants = (listOf(ride.host) + ride.passengers)
            .filterNot { it.id == sender.id }
        val responder = otherParticipants.firstOrNull()
        if (responder != null) {
            messages += MockChatMessage(
                senderUserId = responder.id,
                senderDisplayName = responder.name,
                message = "Noted 👍",
                sentAtLabel = "Now"
            )
        }

        return messages.toList()
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
        val now = Calendar.getInstance()
        val normalizedDate = submission.meetupDate.trim().ifEmpty {
            RideDateTimeFormatter.canonicalDateLabelForNow(now)
        }
        val normalizedTime = submission.meetupTime.trim().ifEmpty {
            RideDateTimeFormatter.canonicalTimeLabelForNow(now)
        }
        val normalizedMeetup = submission.meetupLocation.trim().ifEmpty { "Current Location" }
        val normalizedDestination = submission.destination.trim().ifEmpty { "Selected place" }

        val createdRide = MockRide(
            meetupLabel = normalizedMeetup,
            meetupLatLng = submission.meetupLatLng ?: placeholderPlaces.first().latLng,
            destinationLabel = normalizedDestination,
            destinationLatLng = submission.destinationLatLng ?: placeholderPlaces.first().latLng,
            meetupDateTimeLabel = RideDateTimeFormatter.formatMeetupDateTimeLabel(normalizedDate, normalizedTime),
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
