package com.tritech.hopon.ui.rideDiscovery.core

import com.google.android.gms.maps.model.LatLng
import com.google.maps.internal.PolylineEncoding
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class RoutesApiClient {

    fun computeRoute(
        origin: LatLng,
        destination: LatLng,
        routesApiKey: String,
        onResult: (List<LatLng>?) -> Unit
    ) {
        Thread {
            var connection: HttpURLConnection? = null
            try {
                val requestBody = JSONObject().apply {
                    put("origin", buildRouteLocationJson(origin))
                    put("destination", buildRouteLocationJson(destination))
                    put("travelMode", "DRIVE")
                    put("routingPreference", "TRAFFIC_AWARE")
                }

                val url = URL("https://routes.googleapis.com/directions/v2:computeRoutes")
                connection = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    connectTimeout = 15000
                    readTimeout = 15000
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json")
                    setRequestProperty("X-Goog-Api-Key", routesApiKey)
                    setRequestProperty("X-Goog-FieldMask", "routes.polyline.encodedPolyline")
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
                    onResult(null)
                    return@Thread
                }

                val routes = JSONObject(responseText).optJSONArray("routes")
                if (routes == null || routes.length() == 0) {
                    onResult(null)
                    return@Thread
                }

                val encodedPolyline = routes.getJSONObject(0)
                    .getJSONObject("polyline")
                    .getString("encodedPolyline")

                val latLngList = PolylineEncoding.decode(encodedPolyline)
                    .map { LatLng(it.lat, it.lng) }

                onResult(latLngList)
            } catch (_: Exception) {
                onResult(null)
            } finally {
                connection?.disconnect()
            }
        }.start()
    }

    private fun buildRouteLocationJson(latLng: LatLng): JSONObject {
        val latLngJson = JSONObject().apply {
            put("latitude", latLng.latitude)
            put("longitude", latLng.longitude)
        }
        val locationJson = JSONObject().apply {
            put("latLng", latLngJson)
        }
        return JSONObject().apply {
            put("location", locationJson)
        }
    }
}
