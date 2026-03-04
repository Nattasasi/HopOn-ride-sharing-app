package com.tritech.hopon.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.DirectionsCarFilled
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.tritech.hopon.R
import com.tritech.hopon.ui.rideDiscovery.core.RideDateTimeFormatter
import com.tritech.hopon.ui.rideDiscovery.core.RideParticipationRole
import java.util.Locale

private const val RideCardIconScale = 1.8f

@Composable
fun rideResultCard(
    meetupLabel: String,
    meetupDateTimeLabel: String,
    pickupDistanceMeters: Float,
    hostName: String,
    hostUserId: String? = null,
    currentUserId: String? = null,
    currentUserName: String? = null,
    hostVerificationStatus: String? = null,
    waitTimeMinutes: Int,
    peopleCount: Int,
    maxPeopleCount: Int = 4,
    participationRole: RideParticipationRole? = null,
    showHistoryRideMetrics: Boolean = false,
    rideTimeMinutes: Int? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val pinIconTint = colorResource(id = R.color.colorPrimary)
    val iconTint = colorResource(id = R.color.colorPrimaryDark)
    val (meetupDate, meetupTime) = splitMeetupDateTime(meetupDateTimeLabel)
    val distanceText = formatDistanceLabel(pickupDistanceMeters)
    val rideTimeText = formatRideTimeLabel(rideTimeMinutes)
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

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        val edgeVerticalMargin = 10.dp
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = edgeVerticalMargin)
        ) {
            val thirdRowHorizontalMargin = maxWidth * 0.05f

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Place,
                            contentDescription = null,
                            tint = pinIconTint,
                            modifier = Modifier.size((18f * RideCardIconScale).dp)
                        )
                        Text(
                            text = meetupLabel,
                            style = MaterialTheme.typography.titleMedium,
                            lineHeight = MaterialTheme.typography.titleMedium.lineHeight,
                            color = Color.Black,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    if (participationRole != null) {
                        rideRoleBadge(role = participationRole)
                    }
                }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                infoCell(
                    icon = {
                        Icon(
                            imageVector = Icons.Filled.CalendarToday,
                            contentDescription = null,
                            tint = iconTint,
                            modifier = Modifier.size((14f * RideCardIconScale * 0.9025f).dp)
                        )
                    },
                    text = meetupDate,
                    modifier = Modifier.weight(0.9f)
                )
                infoCell(
                    icon = {
                        Icon(
                            imageVector = Icons.Filled.AccessTime,
                            contentDescription = null,
                            tint = iconTint,
                            modifier = Modifier.size((14f * RideCardIconScale * 0.9025f).dp)
                        )
                    },
                    text = if (showHistoryRideMetrics) meetupTime else "$meetupTime | $waitTimeMinutes mins",
                    modifier = Modifier.weight(1.3f)
                )
                infoCell(
                    icon = {
                        Icon(
                            imageVector = if (showHistoryRideMetrics) {
                                Icons.Filled.DirectionsCarFilled
                            } else {
                                Icons.AutoMirrored.Filled.DirectionsWalk
                            },
                            contentDescription = null,
                            tint = iconTint,
                            modifier = Modifier.size((14f * RideCardIconScale * 0.9025f).dp)
                        )
                    },
                    text = if (showHistoryRideMetrics) rideTimeText else distanceText,
                    modifier = Modifier.weight(0.8f)
                )
            }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = thirdRowHorizontalMargin),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    compactInfoCell(
                        icon = {
                            Icon(
                                imageVector = Icons.Filled.AccountCircle,
                                contentDescription = null,
                                tint = iconTint,
                                modifier = Modifier.size((15f * RideCardIconScale).dp)
                            )
                        },
                        text = displayedHostName
                    )
                    compactInfoCell(
                        icon = {
                            Icon(
                                imageVector = Icons.Filled.People,
                                contentDescription = null,
                                tint = iconTint,
                                modifier = Modifier.size((15f * RideCardIconScale).dp)
                            )
                        },
                        text = stringResource(id = R.string.people_ratio_format, peopleCount, maxPeopleCount)
                    )
                }
                hostVerificationStatusBadge(hostVerificationStatus)
            }
        }
    }
}

private fun formatRideTimeLabel(rideTimeMinutes: Int?): String {
    return if (rideTimeMinutes != null && rideTimeMinutes > 0) {
        "$rideTimeMinutes mins"
    } else {
        "--"
    }
}

@Composable
private fun rideRoleBadge(role: RideParticipationRole) {
    val roleBadgeColors = when (role) {
        RideParticipationRole.JOINED -> hopOnBadgeColors(HopOnBadgeTone.BLUE)
        RideParticipationRole.HOSTED -> hopOnBadgeColors(HopOnBadgeTone.YELLOW)
    }
    val roleText = when (role) {
        RideParticipationRole.JOINED -> stringResource(id = R.string.ride_role_joined)
        RideParticipationRole.HOSTED -> stringResource(id = R.string.ride_role_hosted)
    }

    statusBadge(
        text = roleText,
        backgroundColor = roleBadgeColors.backgroundColor,
        textColor = roleBadgeColors.textColor
    )
}

@Composable
private fun hostVerificationStatusBadge(status: String?) {
    val normalized = status?.trim()?.lowercase(Locale.US) ?: "unverified"
    val (label, tone) = when (normalized) {
        "verified" -> stringResource(R.string.verification_status_verified) to HopOnBadgeTone.GREEN
        "pending" -> stringResource(R.string.verification_status_pending) to HopOnBadgeTone.YELLOW
        "rejected" -> stringResource(R.string.verification_status_rejected) to HopOnBadgeTone.BLUE
        else -> stringResource(R.string.verification_status_unverified) to HopOnBadgeTone.BLUE
    }
    val colors = hopOnBadgeColors(tone)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        horizontalArrangement = Arrangement.Start
    ) {
        statusBadge(
            text = label,
            backgroundColor = colors.backgroundColor,
            textColor = colors.textColor
        )
    }
}

@Composable
private fun compactInfoCell(
    icon: @Composable () -> Unit,
    text: String
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp * 1.05f),
        verticalAlignment = Alignment.CenterVertically
    ) {
        icon()
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium,
            color = Color.DarkGray,
            maxLines = 1
        )
    }
}

@Composable
private fun infoCell(
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
            style = MaterialTheme.typography.titleMedium,
            color = Color.DarkGray,
            maxLines = 1,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(start = 4.dp)
        )
    }
}

private fun splitMeetupDateTime(label: String): Pair<String, String> {
    val (datePart, timePart) = RideDateTimeFormatter.splitMeetupDateTimeLabel(label)
    val formattedDate = RideDateTimeFormatter.formatDateLabelForDisplay(datePart)
    return formattedDate to timePart.ifEmpty { "--" }
}

private fun formatDistanceLabel(distanceMeters: Float): String {
    val safeDistance = distanceMeters.coerceAtLeast(0f)
    return if (safeDistance < 1000f) {
        "${safeDistance.toInt()} m"
    } else {
        String.format(Locale.US, "%.1f km", safeDistance / 1000f)
    }
}
