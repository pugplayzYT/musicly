package com.puggables.musically.data.local

import android.content.Context
import android.content.SharedPreferences

class SessionManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("MusicallyApp", Context.MODE_PRIVATE)

    companion object {
        const val USER_ID = "user_id"
        const val USERNAME = "username"
        const val TOKEN = "token"
        const val BASE_URL = "base_url" // New key for the server URL
    }

    fun saveAuth(userId: Int, username: String, token: String) {
        prefs.edit()
            .putInt(USER_ID, userId)
            .putString(USERNAME, username)
            .putString(TOKEN, token)
            .apply()
    }

    fun saveBaseUrl(url: String) {
        prefs.edit().putString(BASE_URL, url).apply()
    }

    fun getBaseUrl(): String? = prefs.getString(BASE_URL, null)

    fun getUserId(): Int = prefs.getInt(USER_ID, -1)
    fun getUsername(): String? = prefs.getString(USERNAME, null)
    fun getToken(): String? = prefs.getString(TOKEN, null)
    fun isLoggedIn(): Boolean = getUserId() != -1

    fun logout() {
        // Keep the custom URL when logging out
        val customUrl = getBaseUrl()
        prefs.edit().clear().apply()
        if (customUrl != null) {
            saveBaseUrl(customUrl)
        }
    }
}
