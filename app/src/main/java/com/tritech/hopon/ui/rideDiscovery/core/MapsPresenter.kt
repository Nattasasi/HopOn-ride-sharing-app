package com.tritech.hopon.ui.rideDiscovery.core

import android.util.Log
import com.google.android.gms.maps.model.LatLng
import com.tritech.hopon.data.network.NetworkService
import com.tritech.hopon.simulator.WebSocket
import com.tritech.hopon.simulator.WebSocketListener
import com.tritech.hopon.utils.Constants
import org.json.JSONObject

class MapsPresenter(private val networkService: NetworkService) : WebSocketListener {

    companion object {
        private const val TAG = "MapsPresenter"
    }

    private var view: MapsView? = null
    private lateinit var webSocket: WebSocket

    fun onAttach(view: MapsView?) {
        // Bind view and open websocket channel for live cab events.
        this.view = view
        webSocket = networkService.createWebSocket(this)
        webSocket.connect()
    }

    fun onDetach() {
        // Release socket and view references to avoid leaks.
        webSocket.disconnect()
        view = null
    }

    fun requestNearbyCabs(latLng: LatLng) {
        // Ask backend for nearby cabs around current pickup location.
        val jsonObject = JSONObject()
        jsonObject.put(Constants.TYPE, Constants.NEAR_BY_CABS)
        jsonObject.put(Constants.LAT, latLng.latitude)
        jsonObject.put(Constants.LNG, latLng.longitude)
        webSocket.sendMessage(jsonObject.toString())
    }

    fun requestCab(pickUpLatLng: LatLng, dropLatLng: LatLng) {
        // Send a booking request with pickup and destination coordinates.
        val jsonObject = JSONObject()
        jsonObject.put("type", "requestCab")
        jsonObject.put("pickUpLat", pickUpLatLng.latitude)
        jsonObject.put("pickUpLng", pickUpLatLng.longitude)
        jsonObject.put("dropLat", dropLatLng.latitude)
        jsonObject.put("dropLng", dropLatLng.longitude)
        webSocket.sendMessage(jsonObject.toString())
    }

    private fun handleOnMessageNearbyCabs(jsonObject: JSONObject) {
        // Nearby cabs message received but not displayed in current implementation.
    }

    override fun onConnect() {
        Log.d(TAG, "onConnect")
    }

    override fun onMessage(data: String) {
        Log.d(TAG, "onMessage data : $data")
        val jsonObject = JSONObject(data)
        // Route each socket event type to the appropriate view action.
        when (jsonObject.getString(Constants.TYPE)) {
            Constants.NEAR_BY_CABS -> {
                handleOnMessageNearbyCabs(jsonObject)
            }
            Constants.CAB_BOOKED -> {
                view?.informCabBooked()
            }
            Constants.PICKUP_PATH, Constants.TRIP_PATH -> {
                val jsonArray = jsonObject.getJSONArray("path")
                val pickUpPath = arrayListOf<LatLng>()
                for (i in 0 until jsonArray.length()) {
                    val lat = (jsonArray.get(i) as JSONObject).getDouble("lat")
                    val lng = (jsonArray.get(i) as JSONObject).getDouble("lng")
                    val latLng = LatLng(lat, lng)
                    pickUpPath.add(latLng)
                }
                view?.showPath(pickUpPath)
            }
            Constants.LOCATION -> {
                val latCurrent = jsonObject.getDouble("lat")
                val lngCurrent = jsonObject.getDouble("lng")
                view?.updateCabLocation(LatLng(latCurrent, lngCurrent))
            }
            Constants.CAB_IS_ARRIVING -> {
                view?.informCabIsArriving()
            }
            Constants.CAB_ARRIVED -> {
                view?.informCabArrived()
            }
            Constants.TRIP_START -> {
                view?.informTripStart()
            }
            Constants.TRIP_END -> {
                view?.informTripEnd()
            }
        }
    }

    override fun onDisconnect() {
        Log.d(TAG, "onDisconnect")
    }

    override fun onError(error: String) {
        Log.d(TAG, "onError : $error")
        val jsonObject = JSONObject(error)
        // Surface known backend error types as user-facing view messages.
        when (jsonObject.getString(Constants.TYPE)) {
            Constants.ROUTES_NOT_AVAILABLE -> {
                view?.showRoutesNotAvailableError()
            }
            Constants.DIRECTION_API_FAILED -> {
                view?.showDirectionApiFailedError(
                    "Direction API Failed : " + jsonObject.getString(
                        Constants.ERROR
                    )
                )
            }
        }
    }

}