package com.tritech.hopon.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.AccountCircle
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tritech.hopon.R
import com.tritech.hopon.ui.rideDiscovery.core.RideDateTimeFormatter

@Composable
fun rideInProcessCard(
    meetupLabel: String,
    meetupDateTimeLabel: String,
    waitTimeMinutes: Int,
    peopleCount: Int,
    maxPeopleCount: Int,
    hostName: String,
    modifier: Modifier = Modifier
) {
    val iconTint = colorResource(id = R.color.colorPrimaryDark)
    val (meetupDate, meetupTimeRaw) = RideDateTimeFormatter.splitMeetupDateTimeLabel(meetupDateTimeLabel)
    val meetupTime = meetupTimeRaw.ifBlank { "--" }
    val safeWaitTimeMinutes = waitTimeMinutes.coerceAtLeast(0)

    val waitTimeLabel = stringResource(id = R.string.wait_time_format, safeWaitTimeMinutes)
    val timeAndWaitTime = if (meetupTime == "--") {
        waitTimeLabel
    } else {
        "$meetupTime | $waitTimeLabel"
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            rideInProcessInfoRow(
                icon = {
                    Icon(
                        imageVector = Icons.Filled.Place,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(20.dp)
                    )
                },
                value = meetupLabel
            )

            rideInProcessInfoRow(
                icon = {
                    Icon(
                        imageVector = Icons.Filled.AccessTime,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(20.dp)
                    )
                },
                value = if (meetupDate.isBlank()) {
                    timeAndWaitTime
                } else {
                    "$meetupDate • $timeAndWaitTime"
                }
            )

            rideInProcessInfoRow(
                icon = {
                    Icon(
                        imageVector = Icons.Filled.People,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(20.dp)
                    )
                },
                value = stringResource(id = R.string.people_ratio_format, peopleCount, maxPeopleCount)
            )

            rideInProcessInfoRow(
                icon = {
                    Icon(
                        imageVector = Icons.Filled.AccountCircle,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(20.dp)
                    )
                },
                value = hostName
            )
        }
    }
}

@Composable
private fun rideInProcessInfoRow(
    icon: @Composable () -> Unit,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        icon()
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            color = Color.DarkGray
        )
    }
}