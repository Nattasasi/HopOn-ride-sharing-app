package com.tritech.hopon.utils

import android.content.Context

object SessionManager {

    private const val PREF_NAME = "hopon_session"
    private const val KEY_IS_LOGGED_IN = "is_logged_in"
    private const val KEY_CURRENT_USER_ID = "current_user_id"
    private const val KEY_TOKEN = "auth_token"
    private const val KEY_REFRESH_TOKEN = "refresh_token"
    private const val KEY_DISPLAY_NAME = "display_name"

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
                    remove(KEY_TOKEN)
                    remove(KEY_REFRESH_TOKEN)
                    remove(KEY_DISPLAY_NAME)
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

    fun getToken(context: Context): String? {
        val preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return preferences.getString(KEY_TOKEN, null)
    }

    fun setToken(context: Context, token: String) {
        val preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        preferences.edit().putString(KEY_TOKEN, token).apply()
    }

    fun getRefreshToken(context: Context): String? {
        val preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return preferences.getString(KEY_REFRESH_TOKEN, null)
    }

    fun setRefreshToken(context: Context, refreshToken: String) {
        val preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        preferences.edit().putString(KEY_REFRESH_TOKEN, refreshToken).apply()
    }

    fun getDisplayName(context: Context): String? {
        val preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return preferences.getString(KEY_DISPLAY_NAME, null)
    }

    fun setDisplayName(context: Context, name: String) {
        val preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        preferences.edit().putString(KEY_DISPLAY_NAME, name).apply()
    }
}
