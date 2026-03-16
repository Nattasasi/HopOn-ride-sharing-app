package com.tritech.hopon.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.tritech.hopon.R
import com.tritech.hopon.ui.rideDiscovery.screen.RootHostActivity
import com.tritech.hopon.ui.auth.LoginActivity
import com.tritech.hopon.utils.SessionManager

object NotificationHelper {

    private const val CHANNEL_ID_RIDE = "ride_updates"
    private const val CHANNEL_ID_CHAT = "ride_chat"
    private const val CHANNEL_ID_PAYMENT = "ride_payment"
    private const val CHANNEL_ID_SAFETY = "ride_safety"

    fun showNotification(
        context: Context,
        title: String,
        body: String,
        data: Map<String, String> = emptyMap()
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return
        }

        ensureChannels(context)

        val type = data["type"]
        val target = data[NotificationRouting.EXTRA_NOTIFICATION_TARGET]
            ?: NotificationRouting.targetFromType(type)

        val destinationClass = if (SessionManager.isLoggedIn(context)) {
            RootHostActivity::class.java
        } else {
            LoginActivity::class.java
        }

        val intent = Intent(context, destinationClass).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(NotificationRouting.EXTRA_NOTIFICATION_TARGET, target)
            data.forEach { (key, value) ->
                putExtra(key, value)
            }
        }

        val postId = data["post_id"]
        if (!postId.isNullOrBlank()) {
            intent.putExtra(NotificationRouting.EXTRA_NOTIFICATION_POST_ID, postId)
        }

        val postUuid = data["post_uuid"]
        if (!postUuid.isNullOrBlank()) {
            intent.putExtra(NotificationRouting.EXTRA_NOTIFICATION_POST_UUID, postUuid)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            System.currentTimeMillis().toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val channelId = resolveChannelId(type)

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        NotificationManagerCompat.from(context)
            .notify(System.currentTimeMillis().toInt(), notification)
    }

    private fun resolveChannelId(type: String?): String {
        return when (type?.trim()?.lowercase()) {
            "new_message", "chat_message" -> CHANNEL_ID_CHAT
            "payment_due", "payment_received", "payment_failed" -> CHANNEL_ID_PAYMENT
            "emergency_alert", "safety_alert" -> CHANNEL_ID_SAFETY
            else -> CHANNEL_ID_RIDE
        }
    }

    private fun ensureChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val manager = context.getSystemService(NotificationManager::class.java)
        val channels = listOf(
            NotificationChannel(
                CHANNEL_ID_RIDE,
                context.getString(R.string.notification_channel_ride_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = context.getString(R.string.notification_channel_ride_description)
            },
            NotificationChannel(
                CHANNEL_ID_CHAT,
                context.getString(R.string.notification_channel_chat_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = context.getString(R.string.notification_channel_chat_description)
            },
            NotificationChannel(
                CHANNEL_ID_PAYMENT,
                context.getString(R.string.notification_channel_payment_name),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = context.getString(R.string.notification_channel_payment_description)
            },
            NotificationChannel(
                CHANNEL_ID_SAFETY,
                context.getString(R.string.notification_channel_safety_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = context.getString(R.string.notification_channel_safety_description)
            }
        )
        channels.forEach(manager::createNotificationChannel)
    }
}
