package com.puggables.musically.ui.downloads

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.asLiveData
import com.puggables.musically.data.local.db.AppDatabase

class DownloadsViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getDatabase(application)
    val downloadedSongs = db.downloadedSongDao().getAll().asLiveData()
}