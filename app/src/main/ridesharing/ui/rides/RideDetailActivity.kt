package com.tritech.hopon.ui.rides

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.tritech.hopon.R

class RideDetailActivity : AppCompatActivity() {

    companion object {
        private const val EXTRA_MEETUP = "extra_meetup"
        private const val EXTRA_DESTINATION = "extra_destination"
        private const val EXTRA_DISTANCE_KM = "extra_distance_km"

        fun createIntent(
            context: Context,
            meetupLocation: String,
            destinationLocation: String,
            pickupDistanceKm: String
        ): Intent {
            return Intent(context, RideDetailActivity::class.java).apply {
                putExtra(EXTRA_MEETUP, meetupLocation)
                putExtra(EXTRA_DESTINATION, destinationLocation)
                putExtra(EXTRA_DISTANCE_KM, pickupDistanceKm)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ride_detail)

        val meetupValueTextView = findViewById<TextView>(R.id.meetupValueTextView)
        val destinationValueTextView = findViewById<TextView>(R.id.destinationValueTextView)
        val distanceValueTextView = findViewById<TextView>(R.id.distanceValueTextView)

        val meetup = intent.getStringExtra(EXTRA_MEETUP).orEmpty()
        val destination = intent.getStringExtra(EXTRA_DESTINATION).orEmpty()
        val distanceKm = intent.getStringExtra(EXTRA_DISTANCE_KM).orEmpty()

        meetupValueTextView.text = meetup
        destinationValueTextView.text = destination
        distanceValueTextView.text = getString(R.string.pickup_distance_format, distanceKm)
    }
}
