package com.puggables.musically

import android.app.Application
import com.puggables.musically.data.local.SessionManager
import com.puggables.musically.data.local.db.AppDatabase

class MusicallyApplication : Application() {
    companion object {
        lateinit var sessionManager: SessionManager
    }

    // Lazy initialization of the database
    val database: AppDatabase by lazy { AppDatabase.getDatabase(this) }

    override fun onCreate() {
        super.onCreate()
        sessionManager = SessionManager(applicationContext)
    }
}