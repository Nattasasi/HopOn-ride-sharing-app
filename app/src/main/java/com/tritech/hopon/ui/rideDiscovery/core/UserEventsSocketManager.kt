package com.tritech.hopon.ui.rideDiscovery.core

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.tritech.hopon.utils.SessionManager
import io.socket.client.IO
import io.socket.client.Socket

class UserEventsSocketManager(private val context: Context) {

    companion object {
        private const val TAG = "UserEventsSocketMgr"
        private const val EVENT_JOIN_USER = "join_user"
        private const val EVENT_RIDE_CANCELLED = "ride_cancelled"
        private const val EVENT_VERIFICATION_UPDATED = "verification_updated"
        private const val EVENT_BOOKING_REQUESTED = "booking_requested"
        private const val EVENT_PASSENGER_ARRIVED = "passenger_arrived"
        private const val EVENT_PASSENGER_BOARDED = "passenger_boarded"
        private const val EVENT_RIDE_STATUS_CHANGED = "ride_status_changed"
        private const val EVENT_BOOKING_STATUS_CHANGED = "booking_status_changed"
        private const val EVENT_BOOKING_CANCELLED = "booking_cancelled"
        private const val EVENT_RIDE_STARTED = "ride_started"
    }

    data class RideCancelledEvent(
        val postId: String?,
        val postUuid: String?,
        val cancelledBy: String?
    )

    data class VerificationUpdatedEvent(
        val status: String?,
        val isVerified: Boolean?
    )

    data class BookingRequestedEvent(
        val bookingId: String?,
        val postId: String?,
        val postUuid: String?
    )

    data class PassengerArrivedEvent(
        val bookingId: String?,
        val postId: String?,
        val postUuid: String?
    )

    data class PassengerBoardedEvent(
        val bookingId: String?,
        val postId: String?,
        val postUuid: String?
    )

    data class RideStatusChangedEvent(
        val postId: String?,
        val postUuid: String?,
        val status: String?
    )

    data class BookingStatusChangedEvent(
        val bookingId: String?,
        val postId: String?,
        val postUuid: String?,
        val status: String?
    )

    data class BookingCancelledEvent(
        val bookingId: String?,
        val postId: String?,
        val postUuid: String?,
        val status: String?
    )

    data class RideStartedEvent(
        val postId: String?,
        val postUuid: String?
    )

    private var socket: Socket? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    fun connect(
        onRideCancelled: (RideCancelledEvent) -> Unit,
        onVerificationUpdated: (VerificationUpdatedEvent) -> Unit = {},
        onBookingRequested: (BookingRequestedEvent) -> Unit = {},
        onPassengerArrived: (PassengerArrivedEvent) -> Unit = {},
        onPassengerBoarded: (PassengerBoardedEvent) -> Unit = {},
        onRideStatusChanged: (RideStatusChangedEvent) -> Unit = {},
        onBookingStatusChanged: (BookingStatusChangedEvent) -> Unit = {},
        onBookingCancelled: (BookingCancelledEvent) -> Unit = {},
        onRideStarted: (RideStartedEvent) -> Unit = {}
    ) {
        disconnect()

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
        s.on(Socket.EVENT_CONNECT) {
            val token = SessionManager.getToken(context) ?: ""
            s.emit(EVENT_JOIN_USER, token)
        }

        s.on(EVENT_RIDE_CANCELLED) { args ->
            val payload = args.firstOrNull()?.toString().orEmpty()
            val json = runCatching { org.json.JSONObject(payload) }.getOrNull() ?: return@on
            val postId = json.optString("post_id").takeIf { it.isNotBlank() }
            val postUuid = json.optString("post_uuid").takeIf { it.isNotBlank() }
            val cancelledBy = json.optString("cancelled_by").takeIf { it.isNotBlank() }
            val event = RideCancelledEvent(
                postId = postId,
                postUuid = postUuid,
                cancelledBy = cancelledBy
            )
            mainHandler.post { onRideCancelled(event) }
        }

        s.on(EVENT_VERIFICATION_UPDATED) { args ->
            val payload = args.firstOrNull()?.toString().orEmpty()
            val json = runCatching { org.json.JSONObject(payload) }.getOrNull() ?: return@on
            val userObj = json.optJSONObject("user_id")
            val status = userObj?.optString("verification_status")?.takeIf { it.isNotBlank() }
            val isVerified = when {
                userObj == null -> null
                userObj.has("is_verified") -> userObj.optBoolean("is_verified")
                status.equals("verified", ignoreCase = true) -> true
                else -> null
            }
            mainHandler.post {
                onVerificationUpdated(VerificationUpdatedEvent(status = status, isVerified = isVerified))
            }
        }

        s.on(EVENT_BOOKING_REQUESTED) { args ->
            val payload = args.firstOrNull()?.toString().orEmpty()
            val json = runCatching { org.json.JSONObject(payload) }.getOrNull() ?: return@on
            val event = BookingRequestedEvent(
                bookingId = json.optString("booking_id").takeIf { it.isNotBlank() },
                postId = json.optString("post_id").takeIf { it.isNotBlank() },
                postUuid = json.optString("post_uuid").takeIf { it.isNotBlank() }
            )
            mainHandler.post { onBookingRequested(event) }
        }

        s.on(EVENT_PASSENGER_ARRIVED) { args ->
            val payload = args.firstOrNull()?.toString().orEmpty()
            val json = runCatching { org.json.JSONObject(payload) }.getOrNull() ?: return@on
            val event = PassengerArrivedEvent(
                bookingId = json.optString("booking_id").takeIf { it.isNotBlank() },
                postId = json.optString("post_id").takeIf { it.isNotBlank() },
                postUuid = json.optString("post_uuid").takeIf { it.isNotBlank() }
            )
            mainHandler.post { onPassengerArrived(event) }
        }

        s.on(EVENT_PASSENGER_BOARDED) { args ->
            val payload = args.firstOrNull()?.toString().orEmpty()
            val json = runCatching { org.json.JSONObject(payload) }.getOrNull() ?: return@on
            val event = PassengerBoardedEvent(
                bookingId = json.optString("booking_id").takeIf { it.isNotBlank() },
                postId = json.optString("post_id").takeIf { it.isNotBlank() },
                postUuid = json.optString("post_uuid").takeIf { it.isNotBlank() }
            )
            mainHandler.post { onPassengerBoarded(event) }
        }

        s.on(EVENT_RIDE_STATUS_CHANGED) { args ->
            val payload = args.firstOrNull()?.toString().orEmpty()
            val json = runCatching { org.json.JSONObject(payload) }.getOrNull() ?: return@on
            val event = RideStatusChangedEvent(
                postId = json.optString("post_id").takeIf { it.isNotBlank() },
                postUuid = json.optString("post_uuid").takeIf { it.isNotBlank() },
                status = json.optString("status").takeIf { it.isNotBlank() }
            )
            mainHandler.post { onRideStatusChanged(event) }
        }

        s.on(EVENT_BOOKING_STATUS_CHANGED) { args ->
            val payload = args.firstOrNull()?.toString().orEmpty()
            val json = runCatching { org.json.JSONObject(payload) }.getOrNull() ?: return@on
            val event = BookingStatusChangedEvent(
                bookingId = json.optString("booking_id").takeIf { it.isNotBlank() },
                postId = json.optString("post_id").takeIf { it.isNotBlank() },
                postUuid = json.optString("post_uuid").takeIf { it.isNotBlank() },
                status = json.optString("status").takeIf { it.isNotBlank() }
            )
            mainHandler.post { onBookingStatusChanged(event) }
        }

        s.on(EVENT_BOOKING_CANCELLED) { args ->
            val payload = args.firstOrNull()?.toString().orEmpty()
            val json = runCatching { org.json.JSONObject(payload) }.getOrNull() ?: return@on
            val event = BookingCancelledEvent(
                bookingId = json.optString("booking_id").takeIf { it.isNotBlank() },
                postId = json.optString("post_id").takeIf { it.isNotBlank() },
                postUuid = json.optString("post_uuid").takeIf { it.isNotBlank() },
                status = json.optString("status").takeIf { it.isNotBlank() }
            )
            mainHandler.post { onBookingCancelled(event) }
        }

        s.on(EVENT_RIDE_STARTED) { args ->
            val payload = args.firstOrNull()?.toString().orEmpty()
            val json = runCatching { org.json.JSONObject(payload) }.getOrNull()
            val event = RideStartedEvent(
                postId = json?.optString("post_id")?.takeIf { it.isNotBlank() },
                postUuid = json?.optString("post_uuid")?.takeIf { it.isNotBlank() }
            )
            mainHandler.post { onRideStarted(event) }
        }

        s.on(Socket.EVENT_CONNECT_ERROR) { args ->
            Log.e(TAG, "Socket connect error: ${args.firstOrNull()}")
        }

        s.connect()
    }

    fun disconnect() {
        socket?.off()
        socket?.disconnect()
        socket = null
    }
}
