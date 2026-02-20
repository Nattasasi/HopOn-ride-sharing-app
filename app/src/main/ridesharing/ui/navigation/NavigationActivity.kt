package com.tritech.hopon.ui.navigation

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.maps.model.LatLng
import com.google.android.libraries.navigation.NavigationApi
import com.google.android.libraries.navigation.NavigationView
import com.google.android.libraries.navigation.Navigator
import com.google.android.libraries.navigation.Waypoint
import com.tritech.hopon.R
import com.tritech.hopon.ui.auth.LoginActivity
import com.tritech.hopon.utils.SessionManager

class NavigationActivity : AppCompatActivity() {

    companion object {
        private const val EXTRA_ORIGIN_LAT = "extra_origin_lat"
        private const val EXTRA_ORIGIN_LNG = "extra_origin_lng"
        private const val EXTRA_DEST_LAT = "extra_dest_lat"
        private const val EXTRA_DEST_LNG = "extra_dest_lng"

        // Factory intent carrying origin/destination coordinates for turn-by-turn guidance.
        fun createIntent(context: Context, origin: LatLng, destination: LatLng): Intent {
            return Intent(context, NavigationActivity::class.java).apply {
                putExtra(EXTRA_ORIGIN_LAT, origin.latitude)
                putExtra(EXTRA_ORIGIN_LNG, origin.longitude)
                putExtra(EXTRA_DEST_LAT, destination.latitude)
                putExtra(EXTRA_DEST_LNG, destination.longitude)
            }
        }
    }

    private lateinit var navigationView: NavigationView
    private var navigator: Navigator? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Guard screen behind login session.
        if (!SessionManager.isLoggedIn(this)) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        setContentView(R.layout.activity_navigation)
        navigationView = findViewById(R.id.navigation_view)
        navigationView.onCreate(savedInstanceState)

        // Acquire navigator instance asynchronously, then start guidance.
        NavigationApi.getNavigator(this, object : NavigationApi.NavigatorListener {
            override fun onNavigatorReady(navigator: Navigator) {
                this@NavigationActivity.navigator = navigator
                startGuidance()
            }

            override fun onError(errorCode: Int) {
                Toast.makeText(
                    this@NavigationActivity,
                    getString(R.string.navigation_not_available),
                    Toast.LENGTH_LONG
                ).show()
                finish()
            }
        })
    }

    override fun onStart() {
        super.onStart()
        navigationView.onStart()
    }

    override fun onResume() {
        super.onResume()
        navigationView.onResume()
    }

    override fun onPause() {
        navigationView.onPause()
        super.onPause()
    }

    override fun onStop() {
        navigationView.onStop()
        super.onStop()
    }

    override fun onDestroy() {
        // Release navigation resources with activity lifecycle.
        navigator?.stopGuidance()
        navigator?.cleanup()
        navigationView.onDestroy()
        super.onDestroy()
    }

    private fun startGuidance() {
        // Read waypoints passed from previous screen.
        val originLat = intent.getDoubleExtra(EXTRA_ORIGIN_LAT, Double.NaN)
        val originLng = intent.getDoubleExtra(EXTRA_ORIGIN_LNG, Double.NaN)
        val destLat = intent.getDoubleExtra(EXTRA_DEST_LAT, Double.NaN)
        val destLng = intent.getDoubleExtra(EXTRA_DEST_LNG, Double.NaN)

        // Abort when required coordinates are missing.
        if (originLat.isNaN() || originLng.isNaN() || destLat.isNaN() || destLng.isNaN()) {
            Toast.makeText(this, getString(R.string.navigation_missing_waypoints), Toast.LENGTH_LONG)
                .show()
            finish()
            return
        }

        val origin = LatLng(originLat, originLng)
        val destination = LatLng(destLat, destLng)
        val destinationWaypoint = Waypoint.builder()
            .setLatLng(destination.latitude, destination.longitude)
            .build()

        // Start turn-by-turn guidance toward destination.
        navigator?.setDestinations(listOf(destinationWaypoint))
        navigator?.startGuidance()
    }
}
