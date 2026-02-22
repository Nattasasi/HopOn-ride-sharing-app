package com.tritech.hopon

import android.app.Application
import com.google.android.libraries.places.api.Places
import com.tritech.hopon.simulator.Simulator

class RideSharingApp : Application() {

    override fun onCreate() {
        super.onCreate()

        // Initialize Google Places SDK once at app startup using key from resources.
        Places.initialize(applicationContext, getString(R.string.google_maps_key))

        // Provide Routes API key to simulator components that build route requests.
        Simulator.routesApiKey = getString(R.string.routes_api_key)
    }

}