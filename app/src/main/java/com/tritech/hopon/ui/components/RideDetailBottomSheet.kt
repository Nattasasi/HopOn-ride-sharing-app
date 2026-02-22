package com.tritech.hopon.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tritech.hopon.R
import java.util.Locale

@Composable
fun rideDetailBottomSheet(
    meetupLabel: String,
    destinationLabel: String,
    meetupDateTimeLabel: String,
    pickupDistanceMeters: Float,
    waitTimeMinutes: Int,
    hostName: String,
    hostRating: Float,
    hostVehicleType: String,
    peopleCount: Int,
    maxPeopleCount: Int,
    isExpanded: Boolean,
    modifier: Modifier = Modifier
) {
    val iconTint = colorResource(id = R.color.colorPrimaryDark)
    val distanceKm = String.format(Locale.US, "%.2f", pickupDistanceMeters / 1000f)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // Meetup → Destination (always visible)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.Place,
                contentDescription = null,
                tint = colorResource(id = R.color.colorPrimary),
                modifier = Modifier.size(24.dp)
            )
            Text(
                text = meetupLabel,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = Color.Black
            )
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = Color.Gray,
                modifier = Modifier.size(20.dp)
            )
            Icon(
                imageVector = Icons.Filled.Place,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(24.dp)
            )
            Text(
                text = destinationLabel,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = Color.Black
            )
        }

        // Expanded details
        if (isExpanded) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Date & Time
                rideDetailRow(
                    icon = {
                        Icon(
                            imageVector = Icons.Filled.CalendarToday,
                            contentDescription = null,
                            tint = iconTint,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    label = stringResource(id = R.string.meetup_datetime_format, meetupDateTimeLabel)
                )

                // Wait Time
                rideDetailRow(
                    icon = {
                        Icon(
                            imageVector = Icons.Filled.AccessTime,
                            contentDescription = null,
                            tint = iconTint,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    label = stringResource(id = R.string.wait_time_format, waitTimeMinutes)
                )

                // Pickup Distance
                rideDetailRow(
                    icon = {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.DirectionsWalk,
                            contentDescription = null,
                            tint = iconTint,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    label = stringResource(id = R.string.pickup_distance_format, distanceKm)
                )

                // Host Info
                rideDetailRow(
                    icon = {
                        Icon(
                            imageVector = Icons.Filled.AccountCircle,
                            contentDescription = null,
                            tint = iconTint,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    label = "$hostName • $hostVehicleType"
                )

                // Host Rating
                rideDetailRow(
                    icon = {
                        Icon(
                            imageVector = Icons.Filled.Star,
                            contentDescription = null,
                            tint = colorResource(id = R.color.colorAccent),
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    label = String.format(Locale.US, "%.1f", hostRating)
                )

                // People Count
                rideDetailRow(
                    icon = {
                        Icon(
                            imageVector = Icons.Filled.People,
                            contentDescription = null,
                            tint = iconTint,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    label = stringResource(id = R.string.people_ratio_format, peopleCount, maxPeopleCount)
                )
            }
        }
    }
}

@Composable
private fun rideDetailRow(
    icon: @Composable () -> Unit,
    label: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        icon()
        Text(
            text = label,
            fontSize = 15.sp,
            color = Color.DarkGray
        )
    }
}
