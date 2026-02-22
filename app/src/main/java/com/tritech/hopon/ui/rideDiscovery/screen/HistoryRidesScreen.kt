package com.tritech.hopon.ui.rideDiscovery.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import com.tritech.hopon.ui.components.rideResultCard
import com.tritech.hopon.ui.rideDiscovery.core.RideLifecycleStatus
import com.tritech.hopon.ui.rideDiscovery.core.RideListItem

private val HistoryRideCardHeight = 130.dp
private val HeaderLeadingInset = 58.dp
private val SectionTitleInset = 4.dp

@Composable
fun historyRidesScreen(
    rides: List<RideListItem>,
    currentUserName: String,
    onRideClick: (RideListItem) -> Unit
) {
    val ongoingRides = rides.filter { it.lifecycleStatus == RideLifecycleStatus.ONGOING }
    val upcomingRides = rides.filter { it.lifecycleStatus == RideLifecycleStatus.UPCOMING }
    val completedRides = rides.filter { it.lifecycleStatus == RideLifecycleStatus.COMPLETED }
    val hasAnyRide = ongoingRides.isNotEmpty() || upcomingRides.isNotEmpty() || completedRides.isNotEmpty()
    val ongoingTitle = stringResource(id = R.string.history_section_ongoing)
    val upcomingTitle = stringResource(id = R.string.history_section_upcoming)
    val completedTitle = stringResource(id = R.string.history_section_completed)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        Text(
            text = stringResource(id = R.string.nav_history),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .statusBarsPadding()
                .padding(start = HeaderLeadingInset, end = 16.dp, top = 16.dp, bottom = 8.dp)
        )

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            contentPadding = PaddingValues(bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (!hasAnyRide) {
                item {
                    Text(
                        text = stringResource(id = R.string.history_empty_try_share),
                        color = Color.DarkGray,
                        modifier = Modifier.padding(vertical = 20.dp)
                    )
                }
            } else {
                historyRideSection(
                    title = ongoingTitle,
                    status = RideLifecycleStatus.ONGOING,
                    rides = ongoingRides,
                    currentUserName = currentUserName,
                    onRideClick = onRideClick
                )
                historyRideSection(
                    title = upcomingTitle,
                    status = RideLifecycleStatus.UPCOMING,
                    rides = upcomingRides,
                    currentUserName = currentUserName,
                    onRideClick = onRideClick
                )
                historyRideSection(
                    title = completedTitle,
                    status = RideLifecycleStatus.COMPLETED,
                    rides = completedRides,
                    currentUserName = currentUserName,
                    onRideClick = onRideClick
                )
            }
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.historyRideSection(
    title: String,
    status: RideLifecycleStatus,
    rides: List<RideListItem>,
    currentUserName: String,
    onRideClick: (RideListItem) -> Unit
) {
    if (rides.isEmpty()) {
        return
    }

    item {
        val (icon, tint) = sectionIconSpec(status)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = SectionTitleInset, top = 4.dp, bottom = 2.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint
            )
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
    }

    items(rides) { ride ->
        rideResultCard(
            meetupLabel = stringResource(
                id = R.string.history_route_format,
                ride.meetupLabel,
                ride.destinationLabel
            ),
            participationRole = ride.participationRole,
            showHistoryRideMetrics = true,
            rideTimeMinutes = ride.rideTimeMinutes,
            meetupDateTimeLabel = ride.meetupDateTimeLabel,
            pickupDistanceMeters = ride.pickupDistanceMeters,
            hostName = ride.hostName,
            currentUserName = currentUserName,
            waitTimeMinutes = ride.waitTimeMinutes,
            peopleCount = ride.peopleCount,
            maxPeopleCount = ride.maxPeopleCount,
            onClick = { onRideClick(ride) },
            modifier = Modifier
                .fillMaxWidth()
                .height(HistoryRideCardHeight)
        )
    }
}

@Composable
private fun sectionIconSpec(status: RideLifecycleStatus): Pair<ImageVector, Color> {
    return when (status) {
        RideLifecycleStatus.ONGOING -> Icons.Filled.DirectionsCar to MaterialTheme.colorScheme.primary
        RideLifecycleStatus.UPCOMING -> Icons.Filled.AccessTime to MaterialTheme.colorScheme.secondary
        RideLifecycleStatus.COMPLETED -> Icons.Filled.CheckCircle to MaterialTheme.colorScheme.tertiary
    }
}