package com.tritech.hopon.notifications

object NotificationRouting {
    const val EXTRA_NOTIFICATION_TARGET = "extra_notification_target"
    const val EXTRA_NOTIFICATION_POST_ID = "extra_notification_post_id"
    const val EXTRA_NOTIFICATION_POST_UUID = "extra_notification_post_uuid"

    const val TARGET_HISTORY = "history"
    const val TARGET_CHAT = "chat"
    const val TARGET_PAYMENT = "payment"
    const val TARGET_IN_PROCESS = "in_process"

    fun targetFromType(type: String?): String {
        return when (type?.trim()?.lowercase()) {
            "new_message", "chat_message" -> TARGET_CHAT
            "payment_due", "payment_received", "payment_failed" -> TARGET_PAYMENT
            "ride_started" -> TARGET_IN_PROCESS
            else -> TARGET_HISTORY
        }
    }
}
