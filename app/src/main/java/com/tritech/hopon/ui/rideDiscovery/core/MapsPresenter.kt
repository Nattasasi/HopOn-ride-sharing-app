package com.tritech.hopon.ui.rideDiscovery.core

import android.util.Log
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

    override fun onConnect() {
        Log.d(TAG, "onConnect")
    }

    override fun onMessage(data: String) {
        Log.d(TAG, "onMessage data : $data")
        val jsonObject = JSONObject(data)
        // Route each socket event type to the appropriate view action.
        when (jsonObject.getString(Constants.TYPE)) {
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