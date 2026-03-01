package com.tritech.hopon.ui.rideDiscovery.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tritech.hopon.R
import com.tritech.hopon.ui.components.HopOnBadgeTone
import com.tritech.hopon.ui.components.hopOnBadgeColors
import com.tritech.hopon.ui.components.statusBadge
import com.tritech.hopon.ui.rideDiscovery.core.ApiBooking
import java.util.Locale

private val HeaderLeadingIconSpace = 36.dp
private val HeaderTitleStartGap = 6.dp
private data class RideParticipant(val name: String, val roleLabel: String, val isHost: Boolean)

/** Booking statuses from the backend and how to render them. */
private data class BookingBadgeStyle(val label: String, val tone: HopOnBadgeTone)

@Composable
private fun bookingStatusBadgeStyle(status: String?): BookingBadgeStyle? = when (status) {
    "pending"   -> BookingBadgeStyle(
        stringResource(R.string.booking_status_pending),
        HopOnBadgeTone.YELLOW
    )
    "confirmed" -> BookingBadgeStyle(
        stringResource(R.string.booking_status_confirmed),
        HopOnBadgeTone.GREEN
    )
    "rejected"  -> BookingBadgeStyle(
        stringResource(R.string.booking_status_rejected),
        HopOnBadgeTone.BLUE
    )
    "cancelled" -> BookingBadgeStyle(
        stringResource(R.string.booking_status_cancelled),
        HopOnBadgeTone.BLUE
    )
    else -> null
}

@Composable
fun rideDetailScreen(
    onBackClick: () -> Unit,
    meetup: String,
    destination: String,
    pickupDistanceKm: String,
    meetupDateTime: String,
    waitTimeMinutes: Int,
    hostName: String,
    hostUserId: String? = null,
    currentUserId: String? = null,
    currentUserName: String? = null,
    hostRating: Float,
    hostVehicleType: String,
    peopleCount: Int,
    /** Price per seat in Thai Baht (0 if unknown). */
    pricePerSeat: Double = 0.0,
    /** Seats still available on this ride (ignored if 0). */
    seatsAvailable: Int = 0,
    /** The current user's booking status for this ride, or null if not booked. */
    bookingStatus: String? = null,
    /** For host view: number of pending booking requests for this ride. */
    pendingRequestCount: Int = 0,
    /** All booking requests for this ride (host view). */
    bookingRequests: List<ApiBooking> = emptyList(),
    onApproveRequest: (bookingId: String) -> Unit = {},
    onDeclineRequest: (bookingId: String) -> Unit = {}
) {
    val isCurrentUserHost = when {
        !currentUserId.isNullOrBlank() && !hostUserId.isNullOrBlank() -> hostUserId == currentUserId
        !currentUserName.isNullOrBlank() -> hostName.trim().equals(currentUserName.trim(), ignoreCase = true)
        else -> false
    }
    val displayedHostName = if (isCurrentUserHost) {
        stringResource(id = R.string.me_label)
    } else {
        hostName
    }
    val confirmedPassengerNames = bookingRequests
        .filter { it.status == "confirmed" || it.status == "accepted" }
        .mapNotNull { it.passenger_id?.fullName?.takeIf(String::isNotBlank) }
        .distinct()
    val participants = buildList {
        add(RideParticipant(displayedHostName, stringResource(id = R.string.host_label), true))
        confirmedPassengerNames.forEach {
            add(RideParticipant(it, stringResource(id = R.string.booking_status_confirmed), false))
        }
    }
    val pendingPassengerNames = bookingRequests
        .filter { it.status == "pending" }
        .mapNotNull { it.passenger_id?.fullName?.takeIf(String::isNotBlank) }
        .distinct()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .statusBarsPadding()
                .fillMaxWidth()
                .padding(bottom = 8.dp)
        ) {
            IconButton(
                onClick = onBackClick,
                modifier = Modifier.size(HeaderLeadingIconSpace)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    contentDescription = stringResource(id = R.string.ride_detail_back),
                    modifier = Modifier.size(36.dp)
                )
            }
            Spacer(modifier = Modifier.size(HeaderTitleStartGap))
            Text(
                text = stringResource(id = R.string.ride_detail_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = Color.Black,
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Booking status badge (shown when the user has a booking for this ride)
        bookingStatusBadgeStyle(bookingStatus)?.let { style ->
            val badgeColors = hopOnBadgeColors(style.tone)
            Row(modifier = Modifier.fillMaxWidth()) {
                statusBadge(
                    text = style.label,
                    backgroundColor = badgeColors.backgroundColor,
                    textColor = badgeColors.textColor
                )
            }
        }

        if (isCurrentUserHost && pendingRequestCount > 0) {
            bookingStatusBadgeStyle("pending")?.let { style ->
                val badgeColors = hopOnBadgeColors(style.tone)
                Row(modifier = Modifier.fillMaxWidth()) {
                    statusBadge(
                        text = stringResource(
                            R.string.booking_requests_pending_count,
                            pendingRequestCount
                        ),
                        backgroundColor = badgeColors.backgroundColor,
                        textColor = badgeColors.textColor
                    )
                }
            }
        }

        rideDetailCard(
            label = stringResource(id = R.string.meetup_location_label),
            value = meetup,
            leadingIcon = Icons.Filled.LocationOn
        )

        rideDetailCard(
            label = stringResource(id = R.string.destination_label),
            value = destination,
            leadingIcon = Icons.Filled.Flag
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                rideDetailInfoRow(
                    icon = Icons.Filled.AccessTime,
                    text = stringResource(id = R.string.meetup_datetime_format, meetupDateTime)
                )
                rideDetailInfoRow(
                    icon = Icons.Filled.AccessTime,
                    text = stringResource(id = R.string.wait_time_format, waitTimeMinutes)
                )
                rideDetailInfoRow(
                    icon = Icons.Filled.Star,
                    text = stringResource(
                        id = R.string.host_detail_format,
                        displayedHostName,
                        String.format(Locale.US, "%.1f", hostRating),
                        hostVehicleType
                    )
                )
                rideDetailInfoRow(
                    icon = Icons.Filled.Group,
                    text = stringResource(id = R.string.people_count_format, peopleCount)
                )
                rideDetailInfoRow(
                    icon = Icons.Filled.LocationOn,
                    text = stringResource(id = R.string.pickup_distance_format, pickupDistanceKm)
                )
                if (pricePerSeat > 0.0) {
                    rideDetailInfoRow(
                        icon = Icons.Filled.DirectionsCar,
                        text = stringResource(id = R.string.price_per_seat_format, pricePerSeat)
                    )
                }
                if (seatsAvailable > 0) {
                    rideDetailInfoRow(
                        icon = Icons.Filled.Group,
                        text = stringResource(id = R.string.seats_available_format, seatsAvailable)
                    )
                }
            }
        }

        Text(
            text = stringResource(id = R.string.people_count_format, participants.size),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = Color.Black,
            modifier = Modifier.fillMaxWidth()
        )

        participants.forEach { participant ->
            rideParticipantCard(participant)
        }

        if (isCurrentUserHost && pendingPassengerNames.isNotEmpty()) {
            Text(
                text = stringResource(id = R.string.booking_status_pending),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = Color.Black,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp)
            )

            pendingPassengerNames.forEach { pendingName ->
                rideParticipantCard(
                    participant = RideParticipant(
                        name = pendingName,
                        roleLabel = stringResource(id = R.string.booking_status_pending),
                        isHost = false
                    )
                )
            }
        }

        if (isCurrentUserHost && pendingRequestCount > 0) {
            Text(
                text = stringResource(R.string.booking_requests_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = Color.Black,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp)
            )

            val pendingBookings = bookingRequests.filter { it.status == "pending" }
            pendingBookings.forEach { booking ->
                rideDetailBookingRequestCard(
                    booking = booking,
                    onApprove = { onApproveRequest(booking.id) },
                    onDecline = { onDeclineRequest(booking.id) }
                )
            }
        }
    }
}

@Composable
private fun rideParticipantCard(participant: RideParticipant) {
    Card(
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Card(
                shape = RoundedCornerShape(50),
                colors = CardDefaults.cardColors(
                    containerColor = if (participant.isHost) {
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    }
                ),
                modifier = Modifier.size(36.dp)
            ) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Filled.Person,
                        contentDescription = null,
                        tint = if (participant.isHost) MaterialTheme.colorScheme.primary else Color.DarkGray
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = participant.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.Black,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = participant.roleLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
        }
    }
}

@Composable
private fun rideDetailInfoRow(icon: ImageVector, text: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(18.dp)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.DarkGray,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun rideDetailBookingRequestCard(
    booking: ApiBooking,
    onApprove: () -> Unit,
    onDecline: () -> Unit
) {
    val passengerName = booking.passenger_id?.fullName
        ?.takeIf { it.isNotBlank() }
        ?: stringResource(R.string.booking_unknown_passenger)

    Card(
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.booking_passenger_label, passengerName),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                bookingStatusBadgeStyle("pending")?.let { style ->
                    val badgeColors = hopOnBadgeColors(style.tone)
                    statusBadge(
                        text = style.label,
                        backgroundColor = badgeColors.backgroundColor,
                        textColor = badgeColors.textColor
                    )
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(
                    onClick = onApprove,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = stringResource(R.string.booking_request_accept),
                        style = MaterialTheme.typography.labelLarge
                    )
                }
                OutlinedButton(
                    onClick = onDecline,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = stringResource(R.string.booking_request_reject),
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }
    }

    Spacer(modifier = Modifier.height(2.dp))
}

@Composable
private fun rideDetailCard(
    label: String,
    value: String,
    leadingIcon: ImageVector? = null
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                leadingIcon?.let {
                    Icon(
                        imageVector = it,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray
                )
            }
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge,
                color = Color.Black
            )
        }
    }
}

