package com.puggables.musically

import android.app.Application
import com.puggables.musically.data.local.SessionManager

class MusicallyApplication : Application() {
    companion object {
        lateinit var sessionManager: SessionManager
    }

    override fun onCreate() {
        super.onCreate()
        sessionManager = SessionManager(applicationContext)
    }
}
