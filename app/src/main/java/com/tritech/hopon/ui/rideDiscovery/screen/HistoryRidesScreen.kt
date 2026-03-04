package com.tritech.hopon.ui.rideDiscovery.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tritech.hopon.R
import com.tritech.hopon.ui.components.rideResultCard
import com.tritech.hopon.ui.rideDiscovery.core.RideLifecycleStatus
import com.tritech.hopon.ui.rideDiscovery.core.RideListItem

private val HistoryRideCardHeight = 130.dp
private val HeaderLeadingInset = 20.dp

@Composable
fun historyRidesScreen(
    rides: List<RideListItem>,
    currentUserId: String?,
    currentUserName: String,
    onRideClick: (RideListItem) -> Unit
) {
    val tabs = listOf(
        stringResource(id = R.string.history_section_ongoing) to rides.filter { it.lifecycleStatus == RideLifecycleStatus.ONGOING },
        stringResource(id = R.string.history_section_upcoming) to rides.filter { it.lifecycleStatus == RideLifecycleStatus.UPCOMING },
        stringResource(id = R.string.history_section_completed) to rides.filter { it.lifecycleStatus == RideLifecycleStatus.COMPLETED },
        stringResource(id = R.string.history_section_cancelled) to rides.filter { it.lifecycleStatus == RideLifecycleStatus.CANCELLED }
    )
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    if (selectedTabIndex >= tabs.size) selectedTabIndex = 0
    val selectedTabRides = tabs[selectedTabIndex].second
    val hasAnyRide = tabs.any { it.second.isNotEmpty() }

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

        TabRow(
            selectedTabIndex = selectedTabIndex,
            containerColor = Color.White,
            contentColor = MaterialTheme.colorScheme.primary
        ) {
            tabs.forEachIndexed { index, (title, stateRides) ->
                val hasItems = stateRides.isNotEmpty()
                val tabLabel = if (hasItems) "$title (${stateRides.size})" else title
                Tab(
                    selected = selectedTabIndex == index,
                    onClick = { selectedTabIndex = index },
                    text = {
                        Text(
                            text = tabLabel,
                            fontWeight = if (selectedTabIndex == index) FontWeight.SemiBold else FontWeight.Normal
                        )
                    }
                )
            }
        }

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
                if (selectedTabRides.isEmpty()) {
                    item {
                        Text(
                            text = stringResource(id = R.string.history_empty_state),
                            color = Color.DarkGray,
                            modifier = Modifier.padding(vertical = 20.dp)
                        )
                    }
                } else {
                    items(selectedTabRides) { ride ->
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
                            hostUserId = ride.hostUserId,
                            currentUserId = currentUserId,
                            currentUserName = currentUserName,
                            hostVerificationStatus = ride.hostVerificationStatus,
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
            }
        }
    }
}
