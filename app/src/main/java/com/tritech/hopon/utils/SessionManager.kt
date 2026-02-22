package com.tritech.hopon.utils

import android.content.Context

object SessionManager {

    private const val PREF_NAME = "hopon_session"
    private const val KEY_IS_LOGGED_IN = "is_logged_in"
    private const val KEY_CURRENT_USER_ID = "current_user_id"

    fun isLoggedIn(context: Context): Boolean {
        val preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return preferences.getBoolean(KEY_IS_LOGGED_IN, false)
    }

    fun setLoggedIn(context: Context, isLoggedIn: Boolean) {
        val preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        preferences.edit()
            .putBoolean(KEY_IS_LOGGED_IN, isLoggedIn)
            .apply {
                if (!isLoggedIn) {
                    remove(KEY_CURRENT_USER_ID)
                }
            }
            .apply()
    }

    fun getCurrentUserId(context: Context): String? {
        val preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return preferences.getString(KEY_CURRENT_USER_ID, null)
    }

    fun setCurrentUserId(context: Context, userId: String) {
        val preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        preferences.edit().putString(KEY_CURRENT_USER_ID, userId).apply()
    }
}
