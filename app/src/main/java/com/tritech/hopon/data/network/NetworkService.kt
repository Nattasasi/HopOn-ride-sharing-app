package com.tritech.hopon.data.network

import com.tritech.hopon.simulator.WebSocket
import com.tritech.hopon.simulator.WebSocketListener

class NetworkService {

    fun createWebSocket(webSocketListener: WebSocketListener): WebSocket {
        return WebSocket(webSocketListener)
    }

}