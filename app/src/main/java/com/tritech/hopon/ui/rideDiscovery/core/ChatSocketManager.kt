package com.tritech.hopon.ui.rideDiscovery.core

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.google.gson.Gson
import com.tritech.hopon.utils.SessionManager
import io.socket.client.IO
import io.socket.client.Socket
import org.json.JSONObject
import java.util.ArrayDeque

/**
 * Manages a Socket.IO connection for real-time group chat in a carpool post.
 *
 * Usage:
 * 1. Call [connect] with the MongoDB `_id` of the post.
 * 2. Provide an [onMessageReceived] callback that is invoked **on the main thread**.
 * 3. Call [sendMessage] to send a new chat message.
 * 4. Call [disconnect] when leaving the chat screen.
 */
class ChatSocketManager(private val context: Context) {

    private data class OutboundChatMessage(
        val localId: String,
        val body: String
    )

    enum class ConnectionState {
        CONNECTING,
        CONNECTED,
        DISCONNECTED,
        ERROR
    }

    companion object {
        private const val TAG = "ChatSocketManager"
        private const val EVENT_JOIN_POST = "join_post"
        private const val EVENT_SEND_MESSAGE = "send_message"
        private const val EVENT_NEW_MESSAGE = "new_message"
        private const val MAX_PENDING_MESSAGES = 100
    }

    private var socket: Socket? = null
    private var currentPostId: String? = null
    private var hasConnectedOnce = false
    private val mainHandler = Handler(Looper.getMainLooper())
    private val gson = Gson()
    private val pendingOutboundMessages = ArrayDeque<OutboundChatMessage>()

    private fun enqueueMessage(localId: String, body: String) {
        pendingOutboundMessages.removeAll { it.localId == localId }
        pendingOutboundMessages.addLast(OutboundChatMessage(localId = localId, body = body))
        while (pendingOutboundMessages.size > MAX_PENDING_MESSAGES) {
            pendingOutboundMessages.removeFirst()
        }
    }

    private fun emitMessage(body: String): Boolean {
        val postId = currentPostId ?: return false
        val s = socket ?: return false
        if (!s.connected()) return false

        val data = JSONObject().apply {
            put("post_id", postId)
            put("body", body)
        }
        s.emit(EVENT_SEND_MESSAGE, data)
        return true
    }

    private fun flushPendingMessages() {
        if (pendingOutboundMessages.isEmpty()) return

        val iterator = pendingOutboundMessages.iterator()
        while (iterator.hasNext()) {
            val pending = iterator.next()
            val sent = emitMessage(pending.body)
            if (sent) {
                iterator.remove()
            } else {
                break
            }
        }
    }

    /**
     * Connects to the Socket.IO server and joins the room for [postId].
     *
     * @param postId             MongoDB `_id` of the carpool post.
     * @param onMessageReceived  Callback fired on the main thread when a new
     *                           message arrives from the server.
     */
    fun connect(
        postId: String,
        onMessageReceived: (MockChatMessage) -> Unit,
        onConnectionStateChanged: (ConnectionState) -> Unit = {},
        onReconnected: () -> Unit = {}
    ) {
        disconnect()
        currentPostId = postId
        hasConnectedOnce = false

        val socketUrl = ApiBaseUrlResolver.resolve()
            .replace("/api/v1/", "")
            .replace("/api/v1", "")
            .trimEnd('/')

        try {
            val options = IO.Options().apply {
                forceNew = true
                reconnection = true
            }
            socket = IO.socket(socketUrl, options)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create socket", e)
            return
        }

        val s = socket ?: return
        mainHandler.post { onConnectionStateChanged(ConnectionState.CONNECTING) }

        s.on(Socket.EVENT_CONNECT) {
            Log.d(TAG, "Socket connected, joining post $postId")
            val token = SessionManager.getToken(context) ?: ""
            val joinData = JSONObject().apply {
                put("token", token)
                put("post_id", postId)
            }
            s.emit(EVENT_JOIN_POST, joinData)
            // Give the server a brief moment to process room join before sending queued messages.
            mainHandler.postDelayed({ flushPendingMessages() }, 250)
            mainHandler.post {
                onConnectionStateChanged(ConnectionState.CONNECTED)
                if (hasConnectedOnce) {
                    onReconnected()
                } else {
                    hasConnectedOnce = true
                }
            }
        }

        s.on(EVENT_NEW_MESSAGE) { args ->
            if (args.isNotEmpty()) {
                try {
                    val json = args[0].toString()
                    val apiMessage = gson.fromJson(json, ApiMessage::class.java)
                    val chatMessage = apiMessage.toMockChatMessage()
                    mainHandler.post { onMessageReceived(chatMessage) }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to parse new_message", e)
                }
            }
        }

        s.on(Socket.EVENT_CONNECT_ERROR) { args ->
            Log.e(TAG, "Socket connect error: ${args.firstOrNull()}")
            mainHandler.post { onConnectionStateChanged(ConnectionState.ERROR) }
        }

        s.on(Socket.EVENT_DISCONNECT) { args ->
            Log.d(TAG, "Socket disconnected: ${args.firstOrNull()}")
            mainHandler.post { onConnectionStateChanged(ConnectionState.DISCONNECTED) }
        }

        s.connect()
    }

    /**
     * Sends a chat message to the currently joined post room.
     *
     * @param body  The message text.
     */
    fun sendMessage(localId: String, body: String): Boolean {
        if (currentPostId == null) return false

        val sentNow = emitMessage(body)
        if (sentNow) {
            pendingOutboundMessages.removeAll { it.localId == localId }
            return true
        }

        enqueueMessage(localId = localId, body = body)
        Log.w(TAG, "Socket not connected, queued message for resend")
        return true
    }

    /** Disconnects from the Socket.IO server and cleans up. */
    fun disconnect() {
        socket?.off()
        socket?.disconnect()
        socket = null
        currentPostId = null
        pendingOutboundMessages.clear()
    }

    /** Returns true if currently connected. */
    val isConnected: Boolean
        get() = socket?.connected() == true
}
