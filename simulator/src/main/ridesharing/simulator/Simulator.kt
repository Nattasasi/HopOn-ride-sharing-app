package com.tritech.hopon.simulator

import android.os.Handler
import android.os.Looper
import com.google.maps.internal.PolylineEncoding
import com.google.maps.model.LatLng
import org.json.JSONArray
import org.json.JSONObject

object Simulator {

    var routesApiKey: String = ""
    private lateinit var currentLocation: LatLng
    private var nearbyCabLocations = arrayListOf<LatLng>()
    private val mainThread = Handler(Looper.getMainLooper())

    fun getFakeNearbyCabLocations(
        latitude: Double,
        longitude: Double,
        webSocketListener: WebSocketListener
    ) {
        nearbyCabLocations.clear()
        currentLocation = LatLng(latitude, longitude)
        val size = (4..6).random()

        for (i in 1..size) {
            val randomOperatorForLat = (0..1).random()
            val randomOperatorForLng = (0..1).random()
            var randomDeltaForLat = (10..50).random() / 10000.00
            var randomDeltaForLng = (10..50).random() / 10000.00
            if (randomOperatorForLat == 1) {
                randomDeltaForLat *= -1
            }
            if (randomOperatorForLng == 1) {
                randomDeltaForLng *= -1
            }
            val randomLatitude = (latitude + randomDeltaForLat).coerceAtMost(90.00)
            val randomLongitude = (longitude + randomDeltaForLng).coerceAtMost(180.00)
            nearbyCabLocations.add(LatLng(randomLatitude, randomLongitude))
        }

        val jsonObjectToPush = JSONObject()
        jsonObjectToPush.put("type", "nearByCabs")
        val jsonArray = JSONArray()
        for (location in nearbyCabLocations) {
            val jsonObjectLatLng = JSONObject()
            jsonObjectLatLng.put("lat", location.lat)
            jsonObjectLatLng.put("lng", location.lng)
            jsonArray.put(jsonObjectLatLng)
        }
        jsonObjectToPush.put("locations", jsonArray)
        mainThread.post {
            webSocketListener.onMessage(jsonObjectToPush.toString())
        }
    }

}