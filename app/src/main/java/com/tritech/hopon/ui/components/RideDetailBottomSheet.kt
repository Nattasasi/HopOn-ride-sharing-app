package com.tritech.hopon.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tritech.hopon.R
import com.tritech.hopon.ui.rideDiscovery.core.RideDateTimeFormatter
import java.util.Locale

@Composable
fun rideDetailBottomSheet(
    meetupLabel: String,
    destinationLabel: String,
    meetupDateTimeLabel: String,
    pickupDistanceMeters: Float,
    waitTimeMinutes: Int,
    hostName: String,
    currentUserName: String? = null,
    hostRating: Float,
    hostVehicleType: String,
    peopleCount: Int,
    maxPeopleCount: Int,
    isExpanded: Boolean,
    modifier: Modifier = Modifier
) {
    val iconTint = colorResource(id = R.color.colorPrimaryDark)
    val primaryTint = colorResource(id = R.color.colorPrimary)
    val accentTint = colorResource(id = R.color.colorAccent)
    val (meetupDate, meetupTime) = splitDetailMeetupDateTime(meetupDateTimeLabel)
    val distanceText = formatDetailDistanceLabel(pickupDistanceMeters)
    val displayedHostName = if (
        !currentUserName.isNullOrBlank() &&
        hostName.trim().equals(currentUserName.trim(), ignoreCase = true)
    ) {
        stringResource(id = R.string.me_label)
    } else {
        hostName
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 8.dp)
    ) {
        // Meetup and destination block
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp, bottom = 8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                Box(
                    modifier = Modifier
                        .width(24.dp)
                        .padding(top = 2.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .background(color = accentTint, shape = CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.NearMe,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 10.dp, end = 8.dp)
                ) {
                    Text(
                        text = stringResource(id = R.string.meetup_location_label),
                        fontSize = 14.sp,
                        color = Color.Gray
                    )
                    Text(
                        text = meetupLabel,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Normal,
                        color = Color.Black,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .width(24.dp)
                        .height(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(
                        modifier = Modifier
                            .width(2.dp)
                            .height(8.dp)
                    ) {
                        drawLine(
                            color = Color.LightGray,
                            start = Offset(x = size.width / 2f, y = 0f),
                            end = Offset(x = size.width / 2f, y = size.height),
                            strokeWidth = 2.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
                        )
                    }
                }
                HorizontalDivider(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 10.dp, end = 8.dp),
                    thickness = 1.dp,
                    color = Color.LightGray
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                Icon(
                    imageVector = Icons.Filled.Place,
                    contentDescription = null,
                    tint = primaryTint,
                    modifier = Modifier
                        .padding(top = 2.dp)
                        .size(28.dp)
                )
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 10.dp, end = 8.dp)
                ) {
                    Text(
                        text = stringResource(id = R.string.destination_label),
                        fontSize = 14.sp,
                        color = Color.Gray
                    )
                    Text(
                        text = destinationLabel,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Normal,
                        color = Color.Black,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        // Expanded details
        if (isExpanded) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    rideDetailInfoCell(
                        icon = {
                            Icon(
                                imageVector = Icons.Filled.CalendarToday,
                                contentDescription = null,
                                tint = iconTint,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        text = meetupDate,
                        modifier = Modifier.weight(1f)
                    )
                    rideDetailInfoCell(
                        icon = {
                            Icon(
                                imageVector = Icons.Filled.AccessTime,
                                contentDescription = null,
                                tint = iconTint,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        text = stringResource(
                            id = R.string.ride_in_process_time_countdown_format,
                            meetupTime,
                            waitTimeMinutes
                        ),
                        modifier = Modifier.weight(1.25f)
                    )
                    rideDetailInfoCell(
                        icon = {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.DirectionsWalk,
                                contentDescription = null,
                                tint = iconTint,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        text = distanceText,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = stringResource(id = R.string.host_label),
                            fontSize = 14.sp,
                            color = Color.Gray
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.AccountCircle,
                                contentDescription = null,
                                tint = iconTint,
                                modifier = Modifier.size(40.dp)
                            )
                            Column(
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = displayedHostName,
                                    fontSize = 18.sp,
                                    color = colorResource(id = R.color.colorPrimaryDark),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Star,
                                        contentDescription = null,
                                        tint = accentTint,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = stringResource(
                                            id = R.string.rating_out_of_five,
                                            hostRating
                                        ),
                                        fontSize = 16.sp,
                                        color = Color.Gray
                                    )
                                }
                            }
                        }
                    }

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = stringResource(id = R.string.vehicle_label),
                            fontSize = 14.sp,
                            color = Color.Gray
                        )
                        Text(
                            text = hostVehicleType,
                            fontSize = 18.sp,
                            color = colorResource(id = R.color.colorPrimaryDark),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.People,
                                contentDescription = null,
                                tint = iconTint,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = stringResource(
                                    id = R.string.people_ratio_format,
                                    peopleCount,
                                    maxPeopleCount
                                ),
                                fontSize = 16.sp,
                                color = Color.Gray
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun rideDetailInfoCell(
    icon: @Composable () -> Unit,
    text: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        icon()
        Text(
            text = text,
            fontSize = 15.sp,
            color = Color.DarkGray,
            maxLines = 1,
            modifier = Modifier.padding(start = 6.dp)
        )
    }
}

private fun splitDetailMeetupDateTime(label: String): Pair<String, String> {
    val (datePart, timePart) = RideDateTimeFormatter.splitMeetupDateTimeLabel(label)
    val formattedDate = RideDateTimeFormatter.formatDateLabelForDisplay(datePart)
    return formattedDate to timePart.ifEmpty { "--" }
}

private fun formatDetailDistanceLabel(distanceMeters: Float): String {
    val safeDistance = distanceMeters.coerceAtLeast(0f)
    return if (safeDistance < 1000f) {
        "${safeDistance.toInt()} m"
    } else {
        String.format(Locale.US, "%.1f km", safeDistance / 1000f)
    }
}
