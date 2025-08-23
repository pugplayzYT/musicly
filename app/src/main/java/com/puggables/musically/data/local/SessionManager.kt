package com.puggables.musically.data.local

import android.content.Context
import android.content.SharedPreferences

class SessionManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("MusicallyApp", Context.MODE_PRIVATE)

    companion object {
        const val USER_ID = "user_id"
        const val USERNAME = "username"
        const val TOKEN = "token"
        const val BASE_URL = "base_url"
        const val DEFAULT_SPEED = "default_speed"
        const val IS_PRO = "is_pro" // New key for Pro status
    }

    // Update saveAuth to include isPro
    fun saveAuth(userId: Int, username: String, token: String, isPro: Boolean) {
        prefs.edit()
            .putInt(USER_ID, userId)
            .putString(USERNAME, username)
            .putString(TOKEN, token)
            .putBoolean(IS_PRO, isPro) // Save the pro status
            .apply()
    }

    fun saveBaseUrl(url: String) {
        prefs.edit().putString(BASE_URL, url).apply()
    }

    fun saveDefaultSpeed(speed: Float) {
        prefs.edit().putFloat(DEFAULT_SPEED, speed).apply()
    }

    // New function to update the user's status to Pro
    fun setUserAsPro() {
        prefs.edit().putBoolean(IS_PRO, true).apply()
    }

    fun getBaseUrl(): String? = prefs.getString(BASE_URL, null)
    fun getDefaultSpeed(): Float = prefs.getFloat(DEFAULT_SPEED, 1.0f)
    fun getUserId(): Int = prefs.getInt(USER_ID, -1)
    fun getUsername(): String? = prefs.getString(USERNAME, null)
    fun getToken(): String? = prefs.getString(TOKEN, null)
    fun isLoggedIn(): Boolean = getUserId() != -1
    fun isPro(): Boolean = prefs.getBoolean(IS_PRO, false) // New function to check Pro status

    fun logout() {
        val customUrl = getBaseUrl()
        val defaultSpeed = getDefaultSpeed()
        prefs.edit().clear().apply()
        if (customUrl != null) {
            saveBaseUrl(customUrl)
        }
        saveDefaultSpeed(defaultSpeed)
    }
}