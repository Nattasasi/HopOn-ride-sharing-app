package com.tritech.hopon.ui.rides

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tritech.hopon.R
import java.util.Locale

@Composable
fun rideDetailScreen(
    meetup: String,
    destination: String,
    pickupDistanceKm: String,
    meetupDateTime: String,
    waitTimeMinutes: Int,
    hostName: String,
    hostRating: Float,
    hostVehicleType: String,
    peopleCount: Int
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = stringResource(id = R.string.ride_detail_title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = Color.Black,
            modifier = Modifier.fillMaxWidth()
        )

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
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
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
                hostName,
                String.format(Locale.US, "%.1f", hostRating),
                hostVehicleType
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = Color.DarkGray,
            modifier = Modifier.fillMaxWidth()
        )

        Text(
            text = stringResource(id = R.string.people_count_format, peopleCount),
            style = MaterialTheme.typography.bodyMedium,
            color = Color.DarkGray,
            modifier = Modifier.fillMaxWidth()
        )
    }
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
