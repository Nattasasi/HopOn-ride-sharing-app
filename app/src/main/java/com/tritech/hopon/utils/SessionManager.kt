package com.tritech.hopon.utils

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

object SessionManager {

    private const val PREF_NAME = "hopon_session"
    private const val SECURE_PREF_NAME = "hopon_session_secure"
    private const val KEY_IS_LOGGED_IN = "is_logged_in"
    private const val KEY_CURRENT_USER_ID = "current_user_id"
    private const val KEY_TOKEN = "auth_token"
    private const val KEY_REFRESH_TOKEN = "refresh_token"
    private const val KEY_DISPLAY_NAME = "display_name"
    private const val KEY_PUSH_TOKEN = "push_token"
    private const val KEY_PENDING_PUSH_TOKEN = "pending_push_token"
    private const val KEY_SECURE_MIGRATED = "secure_migrated"
    private const val TAG = "SessionManager"

    @Volatile
    private var cachedSecurePreferences: SharedPreferences? = null

    private fun preferences(context: Context): SharedPreferences {
        return try {
            securePreferences(context.applicationContext)
        } catch (error: Exception) {
            Log.w(TAG, "Falling back to legacy SharedPreferences", error)
            legacyPreferences(context.applicationContext)
        }
    }

    private fun legacyPreferences(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    private fun securePreferences(context: Context): SharedPreferences {
        cachedSecurePreferences?.let { return it }

        synchronized(this) {
            cachedSecurePreferences?.let { return it }

            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            val securePrefs = EncryptedSharedPreferences.create(
                context,
                SECURE_PREF_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )

            migrateLegacyValuesIfNeeded(context, securePrefs)
            cachedSecurePreferences = securePrefs
            return securePrefs
        }
    }

    private fun migrateLegacyValuesIfNeeded(
        context: Context,
        securePrefs: SharedPreferences
    ) {
        if (securePrefs.getBoolean(KEY_SECURE_MIGRATED, false)) return

        val legacy = legacyPreferences(context)
        val secureEditor = securePrefs.edit()

        if (legacy.contains(KEY_IS_LOGGED_IN)) {
            secureEditor.putBoolean(KEY_IS_LOGGED_IN, legacy.getBoolean(KEY_IS_LOGGED_IN, false))
        }
        if (legacy.contains(KEY_CURRENT_USER_ID)) {
            secureEditor.putString(KEY_CURRENT_USER_ID, legacy.getString(KEY_CURRENT_USER_ID, null))
        }
        if (legacy.contains(KEY_TOKEN)) {
            secureEditor.putString(KEY_TOKEN, legacy.getString(KEY_TOKEN, null))
        }
        if (legacy.contains(KEY_REFRESH_TOKEN)) {
            secureEditor.putString(KEY_REFRESH_TOKEN, legacy.getString(KEY_REFRESH_TOKEN, null))
        }
        if (legacy.contains(KEY_DISPLAY_NAME)) {
            secureEditor.putString(KEY_DISPLAY_NAME, legacy.getString(KEY_DISPLAY_NAME, null))
        }
        if (legacy.contains(KEY_PUSH_TOKEN)) {
            secureEditor.putString(KEY_PUSH_TOKEN, legacy.getString(KEY_PUSH_TOKEN, null))
        }
        if (legacy.contains(KEY_PENDING_PUSH_TOKEN)) {
            secureEditor.putString(KEY_PENDING_PUSH_TOKEN, legacy.getString(KEY_PENDING_PUSH_TOKEN, null))
        }

        secureEditor.putBoolean(KEY_SECURE_MIGRATED, true).apply()

        // Best effort cleanup to reduce plaintext persistence after migration.
        legacy.edit().clear().apply()
    }

    fun isLoggedIn(context: Context): Boolean {
        val preferences = preferences(context)
        return preferences.getBoolean(KEY_IS_LOGGED_IN, false)
    }

    fun setLoggedIn(context: Context, isLoggedIn: Boolean) {
        val preferences = preferences(context)
        preferences.edit()
            .putBoolean(KEY_IS_LOGGED_IN, isLoggedIn)
            .apply {
                if (!isLoggedIn) {
                    remove(KEY_CURRENT_USER_ID)
                    remove(KEY_TOKEN)
                    remove(KEY_REFRESH_TOKEN)
                    remove(KEY_DISPLAY_NAME)
                    remove(KEY_PENDING_PUSH_TOKEN)
                }
            }
            .apply()
    }

    fun getCurrentUserId(context: Context): String? {
        val preferences = preferences(context)
        return preferences.getString(KEY_CURRENT_USER_ID, null)
    }

    fun setCurrentUserId(context: Context, userId: String) {
        val preferences = preferences(context)
        preferences.edit().putString(KEY_CURRENT_USER_ID, userId).apply()
    }

    fun getToken(context: Context): String? {
        val preferences = preferences(context)
        return preferences.getString(KEY_TOKEN, null)
    }

    fun setToken(context: Context, token: String) {
        val preferences = preferences(context)
        preferences.edit().putString(KEY_TOKEN, token).apply()
    }

    fun getRefreshToken(context: Context): String? {
        val preferences = preferences(context)
        return preferences.getString(KEY_REFRESH_TOKEN, null)
    }

    fun setRefreshToken(context: Context, refreshToken: String) {
        val preferences = preferences(context)
        preferences.edit().putString(KEY_REFRESH_TOKEN, refreshToken).apply()
    }

    fun getDisplayName(context: Context): String? {
        val preferences = preferences(context)
        return preferences.getString(KEY_DISPLAY_NAME, null)
    }

    fun setDisplayName(context: Context, name: String) {
        val preferences = preferences(context)
        preferences.edit().putString(KEY_DISPLAY_NAME, name).apply()
    }

    fun getPushToken(context: Context): String? {
        val preferences = preferences(context)
        return preferences.getString(KEY_PUSH_TOKEN, null)
    }

    fun setPushToken(context: Context, token: String) {
        val preferences = preferences(context)
        preferences.edit().putString(KEY_PUSH_TOKEN, token).apply()
    }

    fun getPendingPushToken(context: Context): String? {
        val preferences = preferences(context)
        return preferences.getString(KEY_PENDING_PUSH_TOKEN, null)
    }

    fun setPendingPushToken(context: Context, token: String) {
        val preferences = preferences(context)
        preferences.edit().putString(KEY_PENDING_PUSH_TOKEN, token).apply()
    }

    fun clearPendingPushToken(context: Context) {
        val preferences = preferences(context)
        preferences.edit().remove(KEY_PENDING_PUSH_TOKEN).apply()
    }
}
