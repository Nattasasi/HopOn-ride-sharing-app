package com.tritech.hopon.ui.rideDiscovery.core

import android.location.Location
import com.google.android.gms.maps.model.LatLng
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

// ─── Date helpers ─────────────────────────────────────────────────────────────

private val iso8601Formats = listOf(
    "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
    "yyyy-MM-dd'T'HH:mm:ss'Z'",
    "yyyy-MM-dd'T'HH:mm:ss.SSSZ",
    "yyyy-MM-dd'T'HH:mm:ssZ"
)

/**
 * Parses an ISO 8601 departure_time string and returns a
 * `RideDateTimeFormatter`-compatible label such as "Feb 26, 2026, 15:30".
 * Returns the raw string unchanged on parse failure so the UI always has
 * something to display.
 */
fun isoToDateTimeLabel(isoString: String): String {
    for (pattern in iso8601Formats) {
        try {
            val sdf = SimpleDateFormat(pattern, Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
            val date = sdf.parse(isoString) ?: continue
            val dateLabel = SimpleDateFormat("MMM dd, yyyy", Locale.US).format(date)
            val timeLabel = SimpleDateFormat("HH:mm", Locale.US).format(date)
            return RideDateTimeFormatter.formatMeetupDateTimeLabel(dateLabel, timeLabel)
        } catch (_: Exception) { /* try next pattern */ }
    }
    return isoString
}

/**
 * Parses ISO 8601 to a date-only label ("Feb 26, 2026").
 * Used when the date and time labels are needed independently.
 */
fun isoToDateLabel(isoString: String): String {
    for (pattern in iso8601Formats) {
        try {
            val sdf = SimpleDateFormat(pattern, Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
            val date = sdf.parse(isoString) ?: continue
            return SimpleDateFormat("MMM dd, yyyy", Locale.US).format(date)
        } catch (_: Exception) { /* try next pattern */ }
    }
    return isoString
}

/**
 * Parses ISO 8601 to epoch millis.
 * Returns null on parse failure.
 */
fun isoToEpochMillis(isoString: String): Long? {
    for (pattern in iso8601Formats) {
        try {
            val sdf = SimpleDateFormat(pattern, Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
            val date = sdf.parse(isoString) ?: continue
            return date.time
        } catch (_: Exception) { /* try next pattern */ }
    }
    return null
}

// ─── Distance helper ──────────────────────────────────────────────────────────

/**
 * Returns the straight-line distance in metres between two [LatLng] points
 * using Android's [Location.distanceBetween].  Used to populate
 * [RideListItem.pickupDistanceMeters] when the API does not provide it.
 */
fun distanceBetween(from: LatLng, to: LatLng): Float {
    val result = FloatArray(1)
    Location.distanceBetween(from.latitude, from.longitude, to.latitude, to.longitude, result)
    return result[0]
}

// ─── Post (Ride) mappers ──────────────────────────────────────────────────────

/**
 * Maps an [ApiCarpoolPost] to a [RideListItem] for display in ride-discovery
 * and history screens.
 *
 * @param currentUserLatLng  Used to calculate [RideListItem.pickupDistanceMeters];
 *                           pass `null` to default to 0 when location is unavailable.
 * @param currentUserId      Used to derive [RideParticipationRole] for the history view.
 */
fun ApiCarpoolPost.toRideListItem(
    currentUserLatLng: LatLng? = null,
    currentUserId: String? = null
): RideListItem {
    val meetupLatLng = LatLng(start_lat, start_lng)
    val destinationLatLng = LatLng(end_lat, end_lng)

    val pickupDistance = if (currentUserLatLng != null) {
        distanceBetween(currentUserLatLng, meetupLatLng)
    } else {
        0f
    }

    val lifecycleStatus = when (status) {
        "in_progress" -> RideLifecycleStatus.ONGOING
        "completed"   -> RideLifecycleStatus.COMPLETED
        "cancelled"   -> RideLifecycleStatus.CANCELLED
        else          -> RideLifecycleStatus.UPCOMING
    }

    val participationRole = when {
        currentUserId != null && driver_id.id == currentUserId -> RideParticipationRole.HOSTED
        currentUserId != null -> RideParticipationRole.JOINED
        else -> null
    }

    val resolvedHostName = driver_id.fullName.ifBlank { "Unknown host" }
    val resolvedHostVerificationStatus = when (
        driver_id.verification_status?.takeIf { it.isNotBlank() }?.lowercase(Locale.US)
    ) {
        "pending", "verified", "rejected", "unverified" -> driver_id.verification_status?.lowercase(Locale.US)
        else -> if (driver_id.is_verified == true) "verified" else "unverified"
    }

    return RideListItem(
        meetupLabel          = start_location_name,
        meetupLatLng         = meetupLatLng,
        destinationLabel     = end_location_name,
        destinationLatLng    = destinationLatLng,
        pickupDistanceMeters = pickupDistance,
        meetupDateTimeLabel  = isoToDateTimeLabel(departure_time),
        waitTimeMinutes      = wait_time_minutes ?: 0,
        hostName             = resolvedHostName,
        hostRating           = driver_id.rating?.toFloat() ?: 0f,
        hostVehicleType      = vehicle_info.orEmpty(),
        vehiclePlate         = vehicle_plate,
        hostUserId           = driver_id.id,
        hostVerificationStatus = resolvedHostVerificationStatus,
        peopleCount          = total_seats - available_seats,
        maxPeopleCount       = total_seats,
        participationRole    = participationRole,
        isCompleted          = status == "completed",
        rideTimeMinutes      = null,     // not provided by API; set after ride ends
        lifecycleStatus      = lifecycleStatus,
        postId               = id,
        postUuid             = post_id.orEmpty(),
        pricePerSeat         = price_per_seat,
        departureEpochMillis = isoToEpochMillis(departure_time)
    )
}

/** Convenience extension for mapping a list of posts. */
fun List<ApiCarpoolPost>.toRideListItems(
    currentUserLatLng: LatLng? = null,
    currentUserId: String? = null
): List<RideListItem> = map { it.toRideListItem(currentUserLatLng, currentUserId) }

// ─── CreateRideSubmission → ApiCreatePostRequest ──────────────────────────────

/**
 * Converts a UI-layer [CreateRideSubmission] to the API request body for
 * `POST /api/v1/posts`.
 *
 * [departureTimeIso] should be the ISO 8601 string built from the user's
 * chosen date + time (see [meetupSubmissionToIso]).
 */
fun CreateRideSubmission.toApiCreatePostRequest(departureTimeIso: String): ApiCreatePostRequest =
    ApiCreatePostRequest(
        start_location_name = meetupLocation,
        start_lat           = meetupLatLng?.latitude ?: 0.0,
        start_lng           = meetupLatLng?.longitude ?: 0.0,
        end_location_name   = destination,
        end_lat             = destinationLatLng?.latitude ?: 0.0,
        end_lng             = destinationLatLng?.longitude ?: 0.0,
        departure_time      = departureTimeIso,
        total_seats         = maxPeopleCount,
        price_per_seat      = pricePerSeat,
        vehicle_info        = vehicleInfo.takeIf { it.isNotBlank() },
        vehicle_plate       = vehiclePlate.takeIf { it.isNotBlank() },
        contact_info        = contactInfo.takeIf { it.isNotBlank() },
        additional_notes    = additionalNotes.takeIf { it.isNotBlank() },
        wait_time_minutes   = waitTimeMinutes
    )

/**
 * Converts a UI-layer date + time submission Strings to an ISO 8601 string
 * for the API.  Input format matches what [RideDateTimeFormatter] produces:
 * date = "MMM dd, yyyy", time = "HH:mm".
 * Falls back to current time on parse failure.
 */
fun meetupSubmissionToIso(dateLabel: String, timeLabel: String): String {
    val epochMillis = RideDateTimeFormatter.parseSubmissionMeetupToEpochMillis(
        dateLabel = dateLabel,
        timeLabel = timeLabel
    ) ?: System.currentTimeMillis()

    val isoSdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }
    return isoSdf.format(epochMillis)
}

// ─── Chat message mapper ──────────────────────────────────────────────────────

fun ApiMessage.toMockChatMessage(): MockChatMessage {
    val senderName = sender_id?.fullName ?: "Unknown"
    val timeLabel = runCatching {
        isoToDateTimeLabel(sent_at ?: "")
    }.getOrDefault(sent_at ?: "")
    return MockChatMessage(
        localId           = id ?: "remote-${sender_id?.id ?: "unknown"}-${sent_at ?: body.hashCode()}",
        senderUserId      = sender_id?.id ?: "",
        senderDisplayName = senderName,
        message           = body,
        sentAtLabel       = timeLabel
    )
}

fun List<ApiMessage>.toMockChatMessages(): List<MockChatMessage> = map { it.toMockChatMessage() }

// ─── Booking helpers ──────────────────────────────────────────────────────────

/**
 * Normalises the booking status from the backend.
 * The backend stores "confirmed" (not "accepted") when the driver accepts.
 * Frontend treats "confirmed" the same as "accepted".
 */
fun normaliseBookingStatus(raw: String?): String? = when (raw) {
    "confirmed" -> "confirmed"   // keep as-is; UI maps this to "Accepted"
    "accepted"  -> "confirmed"   // backward compatibility for legacy rows/states
    null        -> null
    else        -> raw
}

/**
 * Returns true if this booking status represents an active (non-final) state
 * that prevents the user from booking again.
 */
fun String?.isActiveBooking(): Boolean = this == "pending" || this == "confirmed"
