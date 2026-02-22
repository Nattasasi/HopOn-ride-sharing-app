package com.tritech.hopon.ui.rides

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity

class RideDetailActivity : AppCompatActivity() {

    companion object {
        private const val EXTRA_MEETUP = "extra_meetup"
        private const val EXTRA_DESTINATION = "extra_destination"
        private const val EXTRA_DISTANCE_KM = "extra_distance_km"
        private const val EXTRA_MEETUP_DATETIME = "extra_meetup_datetime"
        private const val EXTRA_WAIT_TIME_MINUTES = "extra_wait_time_minutes"
        private const val EXTRA_HOST_NAME = "extra_host_name"
        private const val EXTRA_HOST_RATING = "extra_host_rating"
        private const val EXTRA_HOST_VEHICLE_TYPE = "extra_host_vehicle_type"
        private const val EXTRA_PEOPLE_COUNT = "extra_people_count"

        fun createIntent(
            context: Context,
            meetupLocation: String,
            destinationLocation: String,
            pickupDistanceKm: String,
            meetupDateTime: String,
            waitTimeMinutes: Int,
            hostName: String,
            hostRating: Float,
            hostVehicleType: String,
            peopleCount: Int
        ): Intent {
            return Intent(context, RideDetailActivity::class.java).apply {
                putExtra(EXTRA_MEETUP, meetupLocation)
                putExtra(EXTRA_DESTINATION, destinationLocation)
                putExtra(EXTRA_DISTANCE_KM, pickupDistanceKm)
                putExtra(EXTRA_MEETUP_DATETIME, meetupDateTime)
                putExtra(EXTRA_WAIT_TIME_MINUTES, waitTimeMinutes)
                putExtra(EXTRA_HOST_NAME, hostName)
                putExtra(EXTRA_HOST_RATING, hostRating)
                putExtra(EXTRA_HOST_VEHICLE_TYPE, hostVehicleType)
                putExtra(EXTRA_PEOPLE_COUNT, peopleCount)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val meetup = intent.getStringExtra(EXTRA_MEETUP).orEmpty()
        val destination = intent.getStringExtra(EXTRA_DESTINATION).orEmpty()
        val distanceKm = intent.getStringExtra(EXTRA_DISTANCE_KM).orEmpty()
        val meetupDateTime = intent.getStringExtra(EXTRA_MEETUP_DATETIME).orEmpty()
        val waitTimeMinutes = intent.getIntExtra(EXTRA_WAIT_TIME_MINUTES, 0)
        val hostName = intent.getStringExtra(EXTRA_HOST_NAME).orEmpty()
        val hostRating = intent.getFloatExtra(EXTRA_HOST_RATING, 0f)
        val hostVehicleType = intent.getStringExtra(EXTRA_HOST_VEHICLE_TYPE).orEmpty()
        val peopleCount = intent.getIntExtra(EXTRA_PEOPLE_COUNT, 0)

        setContent {
            rideDetailScreen(
                meetup = meetup,
                destination = destination,
                pickupDistanceKm = distanceKm,
                meetupDateTime = meetupDateTime,
                waitTimeMinutes = waitTimeMinutes,
                hostName = hostName,
                hostRating = hostRating,
                hostVehicleType = hostVehicleType,
                peopleCount = peopleCount
            )
        }
    }
}
