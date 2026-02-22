package com.tritech.hopon.ui.rides.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tritech.hopon.R
import com.tritech.hopon.ui.components.rideResultCard
import com.tritech.hopon.ui.rideDiscovery.core.RideListItem

private val HistoryRideCardHeight = 130.dp

@Composable
fun historyRidesScreen(
    rides: List<RideListItem>,
    onRideClick: (RideListItem) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        Text(
            text = stringResource(id = R.string.nav_history),
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp,
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .statusBarsPadding()
                .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp)
        )

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            contentPadding = PaddingValues(bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (rides.isEmpty()) {
                item {
                    Text(
                        text = stringResource(id = R.string.no_ride_history),
                        color = Color.DarkGray,
                        modifier = Modifier.padding(vertical = 20.dp)
                    )
                }
            } else {
                items(rides) { ride ->
                    rideResultCard(
                        meetupLabel = ride.meetupLabel,
                        meetupDateTimeLabel = ride.meetupDateTimeLabel,
                        pickupDistanceMeters = ride.pickupDistanceMeters,
                        hostName = ride.hostName,
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
