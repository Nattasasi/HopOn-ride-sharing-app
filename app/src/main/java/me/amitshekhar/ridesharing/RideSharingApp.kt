package com.tritech.hopon

import android.app.Application
import com.google.android.libraries.places.api.Places
import com.tritech.hopon.simulator.Simulator

class RideSharingApp : Application() {

    override fun onCreate() {
        super.onCreate()
        Places.initialize(applicationContext, getString(R.string.google_maps_key))
        Simulator.routesApiKey = getString(R.string.routes_api_key)
    }

}