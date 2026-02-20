package com.tritech.hopon.simulator

import android.os.Handler
import android.os.Looper
import android.util.Log
import com.google.maps.internal.PolylineEncoding
import com.google.maps.model.LatLng
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.Timer
import java.util.TimerTask

object Simulator {

    private const val TAG = "Simulator"
    private const val ROUTES_ENDPOINT = "https://routes.googleapis.com/directions/v2:computeRoutes"
    private const val ROUTES_FIELD_MASK = "routes.polyline.encodedPolyline"
    private var timer: Timer? = null
    private var timerTask: TimerTask? = null
    var routesApiKey: String = ""
    private lateinit var currentLocation: LatLng
    private lateinit var pickUpLocation: LatLng
    private lateinit var dropLocation: LatLng
    private var nearbyCabLocations = arrayListOf<LatLng>()
    private var pickUpPath = arrayListOf<LatLng>()
    private var tripPath = arrayListOf<LatLng>()
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

    fun requestCab(
        pickUpLocation: LatLng,
        dropLocation: LatLng,
        webSocketListener: WebSocketListener
    ) {
        this.pickUpLocation = pickUpLocation
        this.dropLocation = dropLocation

        val randomOperatorForLat = (0..1).random()
        val randomOperatorForLng = (0..1).random()

        var randomDeltaForLat = (5..30).random() / 10000.00
        var randomDeltaForLng = (5..30).random() / 10000.00

        if (randomOperatorForLat == 1) {
            randomDeltaForLat *= -1
        }
        if (randomOperatorForLng == 1) {
            randomDeltaForLng *= -1
        }
        val latFakeNearby = (pickUpLocation.lat + randomDeltaForLat).coerceAtMost(90.00)
        val lngFakeNearby = (pickUpLocation.lng + randomDeltaForLng).coerceAtMost(180.00)

        val bookedCabCurrentLocation = LatLng(latFakeNearby, lngFakeNearby)
        requestRoute(bookedCabCurrentLocation, this.pickUpLocation, { path ->
            val jsonObjectCabBooked = JSONObject()
            jsonObjectCabBooked.put("type", "cabBooked")
            mainThread.post {
                webSocketListener.onMessage(jsonObjectCabBooked.toString())
            }
            pickUpPath.clear()
            pickUpPath.addAll(path)
            val jsonObject = JSONObject()
            jsonObject.put("type", "pickUpPath")
            val jsonArray = JSONArray()
            for (pickUp in pickUpPath) {
                val jsonObjectLatLng = JSONObject()
                jsonObjectLatLng.put("lat", pickUp.lat)
                jsonObjectLatLng.put("lng", pickUp.lng)
                jsonArray.put(jsonObjectLatLng)
            }
            jsonObject.put("path", jsonArray)
            mainThread.post {
                webSocketListener.onMessage(jsonObject.toString())
            }
            startTimerForPickUp(webSocketListener)
        }, { error ->
            notifyDirectionApiFailed(webSocketListener, error)
        })
    }

    fun startTimerForPickUp(webSocketListener: WebSocketListener) {
        val delay = 2000L
        val period = 3000L
        val size = pickUpPath.size
        var index = 0
        timer = Timer()
        timerTask = object : TimerTask() {
            override fun run() {
                val jsonObject = JSONObject()
                jsonObject.put("type", "location")
                jsonObject.put("lat", pickUpPath[index].lat)
                jsonObject.put("lng", pickUpPath[index].lng)
                mainThread.post {
                    webSocketListener.onMessage(jsonObject.toString())
                }

                if (index == size - 1) {
                    stopTimer()
                    val jsonObjectCabIsArriving = JSONObject()
                    jsonObjectCabIsArriving.put("type", "cabIsArriving")
                    mainThread.post {
                        webSocketListener.onMessage(jsonObjectCabIsArriving.toString())
                    }
                    startTimerForWaitDuringPickUp(webSocketListener)
                }

                index++
            }
        }

        timer?.schedule(timerTask, delay, period)
    }

    fun startTimerForWaitDuringPickUp(webSocketListener: WebSocketListener) {
        val delay = 3000L
        val period = 3000L
        timer = Timer()
        timerTask = object : TimerTask() {
            override fun run() {
                stopTimer()
                val jsonObjectCabArrived = JSONObject()
                jsonObjectCabArrived.put("type", "cabArrived")
                mainThread.post {
                    webSocketListener.onMessage(jsonObjectCabArrived.toString())
                }
                requestRoute(pickUpLocation, dropLocation, { path ->
                    tripPath.clear()
                    tripPath.addAll(path)
                    startTimerForTrip(webSocketListener)
                }, { error ->
                    notifyDirectionApiFailed(webSocketListener, error)
                })

            }
        }
        timer?.schedule(timerTask, delay, period)
    }

    private fun requestRoute(
        origin: LatLng,
        destination: LatLng,
        onSuccess: (List<LatLng>) -> Unit,
        onFailure: (String) -> Unit
    ) {
        if (routesApiKey.isBlank()) {
            onFailure("Routes API key missing")
            return
        }
        Thread {
            var connection: HttpURLConnection? = null
            try {
                val requestBody = JSONObject()
                requestBody.put("origin", buildLocationJson(origin))
                requestBody.put("destination", buildLocationJson(destination))
                requestBody.put("travelMode", "DRIVE")
                requestBody.put("routingPreference", "TRAFFIC_AWARE")

                val url = URL(ROUTES_ENDPOINT)
                connection = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    connectTimeout = 15000
                    readTimeout = 15000
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json")
                    setRequestProperty("X-Goog-Api-Key", routesApiKey)
                    setRequestProperty("X-Goog-FieldMask", ROUTES_FIELD_MASK)
                }

                connection.outputStream.use { outputStream ->
                    outputStream.write(requestBody.toString().toByteArray(Charsets.UTF_8))
                }

                val responseCode = connection.responseCode
                val responseStream = if (responseCode in 200..299) {
                    connection.inputStream
                } else {
                    connection.errorStream ?: connection.inputStream
                }
                val responseText = responseStream.bufferedReader().use { it.readText() }

                if (responseCode !in 200..299) {
                    onFailure("Routes API failed: $responseCode $responseText")
                    return@Thread
                }

                val responseJson = JSONObject(responseText)
                val routes = responseJson.optJSONArray("routes")
                if (routes == null || routes.length() == 0) {
                    onFailure("No routes returned")
                    return@Thread
                }

                val polyline = routes.getJSONObject(0)
                    .getJSONObject("polyline")
                    .getString("encodedPolyline")
                val decoded = PolylineEncoding.decode(polyline)
                onSuccess(decoded)
            } catch (e: Exception) {
                onFailure(e.message ?: "Routes API error")
            } finally {
                connection?.disconnect()
            }
        }.start()
    }

    private fun buildLocationJson(latLng: LatLng): JSONObject {
        val latLngJson = JSONObject()
        latLngJson.put("latitude", latLng.lat)
        latLngJson.put("longitude", latLng.lng)
        val locationJson = JSONObject()
        locationJson.put("latLng", latLngJson)
        val container = JSONObject()
        container.put("location", locationJson)
        return container
    }

    private fun notifyDirectionApiFailed(webSocketListener: WebSocketListener, error: String?) {
        Log.d(TAG, "onFailure : $error")
        val jsonObjectFailure = JSONObject()
        jsonObjectFailure.put("type", "directionApiFailed")
        jsonObjectFailure.put("error", error)
        mainThread.post {
            webSocketListener.onError(jsonObjectFailure.toString())
        }
    }

    fun startTimerForTrip(webSocketListener: WebSocketListener) {
        val delay = 5000L
        val period = 3000L
        val size = tripPath.size
        var index = 0
        timer = Timer()
        timerTask = object : TimerTask() {
            override fun run() {

                if (index == 0) {
                    val jsonObjectTripStart = JSONObject()
                    jsonObjectTripStart.put("type", "tripStart")
                    mainThread.post {
                        webSocketListener.onMessage(jsonObjectTripStart.toString())
                    }

                    val jsonObject = JSONObject()
                    jsonObject.put("type", "tripPath")
                    val jsonArray = JSONArray()
                    for (trip in tripPath) {
                        val jsonObjectLatLng = JSONObject()
                        jsonObjectLatLng.put("lat", trip.lat)
                        jsonObjectLatLng.put("lng", trip.lng)
                        jsonArray.put(jsonObjectLatLng)
                    }
                    jsonObject.put("path", jsonArray)
                    mainThread.post {
                        webSocketListener.onMessage(jsonObject.toString())
                    }
                }

                val jsonObject = JSONObject()
                jsonObject.put("type", "location")
                jsonObject.put("lat", tripPath[index].lat)
                jsonObject.put("lng", tripPath[index].lng)
                mainThread.post {
                    webSocketListener.onMessage(jsonObject.toString())
                }

                if (index == size - 1) {
                    stopTimer()
                    startTimerForTripEndEvent(webSocketListener)
                }

                index++
            }
        }
        timer?.schedule(timerTask, delay, period)
    }

    fun startTimerForTripEndEvent(webSocketListener: WebSocketListener) {
        val delay = 3000L
        val period = 3000L
        timer = Timer()
        timerTask = object : TimerTask() {
            override fun run() {
                stopTimer()
                val jsonObjectTripEnd = JSONObject()
                jsonObjectTripEnd.put("type", "tripEnd")
                mainThread.post {
                    webSocketListener.onMessage(jsonObjectTripEnd.toString())
                }
            }
        }
        timer?.schedule(timerTask, delay, period)
    }

    fun stopTimer() {
        if (timer != null) {
            timer?.cancel()
            timer = null
        }
    }

}