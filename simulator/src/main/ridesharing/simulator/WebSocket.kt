package com.tritech.hopon.simulator

import org.json.JSONObject

class WebSocket(private var webSocketListener: WebSocketListener) {

    fun connect() {
        webSocketListener.onConnect()
    }

    fun sendMessage(data: String) {
        val jsonObject = JSONObject(data)
        when (jsonObject.getString("type")) {
            "nearByCabs" -> {
                Simulator.getFakeNearbyCabLocations(
                    jsonObject.getDouble("lat"),
                    jsonObject.getDouble("lng"),
                    webSocketListener
                )
            }
        }
    }

    fun disconnect() {
        this.webSocketListener.onDisconnect()
    }

}