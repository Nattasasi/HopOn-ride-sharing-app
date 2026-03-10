package com.tritech.hopon.ui.rideDiscovery.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
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
    hostVerificationStatus: String? = null,
    hostRating: Float,
    hostVehicleType: String,
    vehiclePlate: String? = null,
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
    onDeclineRequest: (bookingId: String) -> Unit = {},
    cancelWindowInfo: String? = null,
    isCancelWindowExpired: Boolean = false,
    showReportDriverAction: Boolean = false,
    onReportDriverClick: () -> Unit = {},
    showHostRideActions: Boolean = false,
    onGroupChatClick: () -> Unit = {},
    onCancelRideClick: () -> Unit = {}
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

        cancelWindowInfo?.takeIf { it.isNotBlank() }?.let { text ->
            Text(
                text = text,
                style = MaterialTheme.typography.bodySmall,
                color = if (isCancelWindowExpired) {
                    MaterialTheme.colorScheme.error
                } else {
                    Color.DarkGray
                },
                modifier = Modifier.fillMaxWidth()
            )
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
            value = meetup
        )

        rideDetailCard(
            label = stringResource(id = R.string.destination_label),
            value = destination
        )

        Text(
            text = stringResource(id = R.string.pickup_distance_format, pickupDistanceKm),
            style = MaterialTheme.typography.bodyMedium,
            color = Color.DarkGray,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp)
        )

        Text(
            text = stringResource(id = R.string.meetup_datetime_format, meetupDateTime),
            style = MaterialTheme.typography.bodyMedium,
            color = Color.DarkGray,
            modifier = Modifier.fillMaxWidth()
        )

        Text(
            text = stringResource(id = R.string.wait_time_format, waitTimeMinutes),
            style = MaterialTheme.typography.bodyMedium,
            color = Color.DarkGray,
            modifier = Modifier.fillMaxWidth()
        )

        Text(
            text = stringResource(
                id = R.string.host_detail_format,
                displayedHostName,
                String.format(Locale.US, "%.1f", hostRating),
                hostVehicleType
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = Color.DarkGray,
            modifier = Modifier.fillMaxWidth()
        )

        if (!vehiclePlate.isNullOrBlank()) {
            Text(
                text = stringResource(id = R.string.vehicle_plate_format, vehiclePlate),
                style = MaterialTheme.typography.bodyMedium,
                color = Color.DarkGray,
                modifier = Modifier.fillMaxWidth()
            )
        }

        hostVerificationBadge(hostVerificationStatus)

        Text(
            text = stringResource(id = R.string.people_count_format, peopleCount),
            style = MaterialTheme.typography.bodyMedium,
            color = Color.DarkGray,
            modifier = Modifier.fillMaxWidth()
        )

        // Price per seat (only when available from API)
        if (pricePerSeat > 0.0) {
            Text(
                text = stringResource(id = R.string.price_per_seat_format, pricePerSeat),
                style = MaterialTheme.typography.bodyMedium,
                color = Color.DarkGray,
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Seats available (only when we have data)
        if (seatsAvailable > 0) {
            Text(
                text = stringResource(id = R.string.seats_available_format, seatsAvailable),
                style = MaterialTheme.typography.bodyMedium,
                color = Color.DarkGray,
                modifier = Modifier.fillMaxWidth()
            )
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

        if (showReportDriverAction) {
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedButton(
                onClick = onReportDriverClick,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = stringResource(R.string.report_driver))
            }
        }

        if (showHostRideActions) {
            Spacer(modifier = Modifier.height(6.dp))
            Button(
                onClick = onGroupChatClick,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = stringResource(R.string.group_chat))
            }
            OutlinedButton(
                onClick = onCancelRideClick,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = stringResource(R.string.cancel_ride))
            }
        }
    }
}

@Composable
private fun hostVerificationBadge(status: String?) {
    val normalized = status?.trim()?.lowercase(Locale.US) ?: "unverified"
    val (label, tone) = when (normalized) {
        "verified" -> stringResource(R.string.verification_status_verified) to HopOnBadgeTone.GREEN
        "pending" -> stringResource(R.string.verification_status_pending) to HopOnBadgeTone.YELLOW
        "rejected" -> stringResource(R.string.verification_status_rejected) to HopOnBadgeTone.BLUE
        else -> stringResource(R.string.verification_status_unverified) to HopOnBadgeTone.BLUE
    }
    val colors = hopOnBadgeColors(tone)
    Row(modifier = Modifier.fillMaxWidth()) {
        statusBadge(
            text = label,
            backgroundColor = colors.backgroundColor,
            textColor = colors.textColor
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
    value: String
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
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = Color.Gray
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge,
                color = Color.Black
            )
        }
    }
}

