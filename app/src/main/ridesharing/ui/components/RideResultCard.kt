package com.tritech.hopon.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tritech.hopon.R

@Composable
fun rideResultCard(
    meetupLabel: String,
    destinationLabel: String,
    pickupDistanceKm: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accent = colorResource(id = R.color.colorPrimary)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Text(
                text = stringResource(id = R.string.meetup_format, meetupLabel),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = stringResource(id = R.string.destination_format, destinationLabel),
                style = MaterialTheme.typography.bodySmall,
                color = Color.DarkGray,
                modifier = Modifier.padding(top = 2.dp)
            )
            Text(
                text = stringResource(id = R.string.pickup_distance_format, pickupDistanceKm),
                style = MaterialTheme.typography.bodySmall,
                color = accent,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}
