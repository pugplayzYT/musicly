package com.puggables.musically

import android.app.Application
import com.puggables.musically.data.local.SessionManager
import com.puggables.musically.data.local.db.AppDatabase
import com.puggables.musically.downloading.DownloadTracker

class MusicallyApplication : Application() {
    companion object {
        lateinit var sessionManager: SessionManager
    }

    // Lazy initialization of the database
    val database: AppDatabase by lazy { AppDatabase.getDatabase(this) }

    override fun onCreate() {
        super.onCreate()
        sessionManager = SessionManager(applicationContext)
        // Initialize the DownloadTracker with the database DAO
        DownloadTracker.initialize(database.downloadedSongDao())
    }
}
